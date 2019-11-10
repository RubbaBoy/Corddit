package com.uddernetworks.reddicord.reddit;

import com.uddernetworks.reddicord.Reddicord;
import com.uddernetworks.reddicord.config.ConfigManager;
import com.uddernetworks.reddicord.discord.EmbedUtils;
import com.uddernetworks.reddicord.reddit.user.LinkedUser;
import com.uddernetworks.reddicord.reddit.web.WebCallback;
import net.dean.jraw.RedditClient;
import net.dean.jraw.http.NetworkAdapter;
import net.dean.jraw.http.OkHttpNetworkAdapter;
import net.dean.jraw.http.UserAgent;
import net.dean.jraw.oauth.Credentials;
import net.dean.jraw.oauth.JsonFileTokenStore;
import net.dean.jraw.oauth.OAuthHelper;
import net.dean.jraw.oauth.StatefulAuthHelper;
import net.dv8tion.jda.api.entities.Member;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.uddernetworks.reddicord.config.Config.CLIENTID;
import static com.uddernetworks.reddicord.config.Config.CLIENTSECRET;
import static com.uddernetworks.reddicord.config.Config.REDIRECTURL;

public class RedditManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedditManager.class);

    private final Reddicord reddicord;
    private final ConfigManager configManager;

    private final List<RedditClient> clientCache = Collections.synchronizedList(new ArrayList<>());

    private Credentials credentials;
    private NetworkAdapter networkAdapter;
    private JsonFileTokenStore store;
    private StatefulAuthHelper statefulAuthHelper;

    public RedditManager(Reddicord reddicord) {
        this.reddicord = reddicord;
        this.configManager = reddicord.getConfigManager();
    }

    public void init(File tokenStore) {
        credentials = Credentials.webapp(configManager.get(CLIENTID), configManager.get(CLIENTSECRET), configManager.get(REDIRECTURL));

        var userAgent = new UserAgent("bot", "com.uddernetworks.reddicord", "1.0.0", "Reddicord");

        networkAdapter = new OkHttpNetworkAdapter(userAgent);
        store = new JsonFileTokenStore(tokenStore);
        if (tokenStore.exists()) store.load();
        store.setAutoPersist(true);
        statefulAuthHelper = OAuthHelper.interactive(networkAdapter, credentials, store);
    }

    public Optional<RedditClient> getAccount(String username) {
        return Optional.ofNullable(clientCache.stream().filter(client -> client.me().getUsername().equalsIgnoreCase(username)).findFirst().orElseGet(() -> {
            var tokenStoreUser = store.fetchLatest(username);
            if (tokenStoreUser == null) return null;
            var client = new RedditClient(networkAdapter, tokenStoreUser, credentials, store, username);
            clientCache.add(client);
            return client;
        }));
    }

    public CompletableFuture<Optional<LinkedUser>> linkClient(Member member) {
        var dm = member.getUser().openPrivateChannel().complete();
        if (dm == null) {
            LOGGER.error("DM is null for {}", member.getNickname());
            return CompletableFuture.completedFuture(Optional.empty());
        }

        var authUrl = statefulAuthHelper.getAuthorizationUrl(true, false, "read", "vote", "identity", "account", "save", "history");
        dm.sendMessage(EmbedUtils.createEmbed(member, "Reddit Link", embed -> embed.setDescription("To link your Reddit account with Reddicord, please click [here](" + authUrl + ").\n\nIf you don't complete this within 10 minutes, the verification will be ignored and you will need to redo the /link command."))).queue();

        return WebCallback.listenFor().thenApply(body -> {
            var reddit = statefulAuthHelper.onUserChallenge(configManager.get(REDIRECTURL) + body);
            var linkedUser = new LinkedUser(member.getUser(), reddit);
            clientCache.add(reddit);
            reddicord.getUserManager().addUser(linkedUser);
            dm.sendMessage("Account **/u/" + reddit.me().getUsername() + "** successfully verified.").queue();
            return linkedUser;
        }).orTimeout(10, TimeUnit.MINUTES).exceptionally(t -> null).thenApply(Optional::ofNullable);
    }

}
