package com.uddernetworks.reddicord.reddit;

import com.uddernetworks.reddicord.Reddicord;
import com.uddernetworks.reddicord.config.ConfigManager;
import com.uddernetworks.reddicord.discord.EmbedUtils;
import com.uddernetworks.reddicord.user.LinkedUser;
import com.uddernetworks.reddicord.user.web.WebCallback;
import net.dean.jraw.RedditClient;
import net.dean.jraw.http.NetworkAdapter;
import net.dean.jraw.http.OkHttpNetworkAdapter;
import net.dean.jraw.http.UserAgent;
import net.dean.jraw.oauth.Credentials;
import net.dean.jraw.oauth.JsonFileTokenStore;
import net.dean.jraw.oauth.OAuthHelper;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
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
    private final WebCallback webCallback;

    private final List<RedditClient> clientCache = Collections.synchronizedList(new ArrayList<>());
    private final List<User> waitingUsers = Collections.synchronizedList(new ArrayList<>());

    private Credentials credentials;
    private NetworkAdapter networkAdapter;
    private JsonFileTokenStore store;

    public RedditManager(Reddicord reddicord, ConfigManager configManager, WebCallback webCallback) {
        this.reddicord = reddicord;
        this.configManager = configManager;
        this.webCallback = webCallback;
    }

    public void init(File tokenStore) {
        credentials = Credentials.webapp(configManager.get(CLIENTID), configManager.get(CLIENTSECRET), configManager.get(REDIRECTURL));

        var userAgent = new UserAgent("bot", "com.uddernetworks.reddicord", "1.0.0", "Reddicord");

        networkAdapter = new OkHttpNetworkAdapter(userAgent);
        store = new JsonFileTokenStore(tokenStore);
        if (tokenStore.exists()) {
            LOGGER.info("Exists! loading");
            store.load();
        } else {
            LOGGER.info("BAD!!!!");
        }
        store.setAutoPersist(true);

        LOGGER.info("usernames: {}", store.getUsernames());
        store.getUsernames().forEach(this::createAndAddAccount);
    }

    private Optional<RedditClient> createAndAddAccount(String username) {
        var tokenStoreUser = store.fetchLatest(username);

        var inspected = store.inspect(username);
        LOGGER.info("INspected: {}", inspected);
        LOGGER.info("Token store for {} is {}", username, tokenStoreUser);

//        var statefulAuthHelper = OAuthHelper.interactive(networkAdapter, credentials, store);
//        LOGGER.info(statefulAuthHelper.getAuthStatus().name());

        if (tokenStoreUser == null) return Optional.empty();
        var client = new RedditClient(networkAdapter, tokenStoreUser, credentials, store, username);
        client.setAutoRenew(true);
        clientCache.add(client);
        return Optional.of(client);
    }

    public Optional<RedditClient> getAccount(String username) {
        return Optional.ofNullable(clientCache.stream().filter(client -> client.getAuthManager().currentUsername().equalsIgnoreCase(username)).findFirst().orElseGet(() ->
                createAndAddAccount(username).orElse(null)));
    }

    public boolean isWaiting(Member member) {
        return waitingUsers.contains(member.getUser());
    }

    public CompletableFuture<Optional<LinkedUser>> linkClient(Member member) {
        var user = member.getUser();
        var completableFuture = new CompletableFuture<Optional<LinkedUser>>();
        user.openPrivateChannel().queue(dm -> {
            waitingUsers.add(user);
            var statefulAuthHelper = OAuthHelper.interactive(networkAdapter, credentials, store);
            var authUrl = statefulAuthHelper.getAuthorizationUrl(true, false, "read", "vote", "identity", "account", "save", "history");
            var query = WebCallback.getQuery(authUrl);
            var state = query.get("state");

            dm.sendMessage(EmbedUtils.createEmbed(member, "Reddit Link", embed -> embed.setDescription("To link your Reddit account with Reddicord, please click [here](" + authUrl + ").\n\nIf you don't complete this within 10 minutes, the verification will be ignored and you will need to redo the /link command."))).queue();

            webCallback.listenForState(state).thenApply(body -> {
                var reddit = statefulAuthHelper.onUserChallenge(configManager.get(REDIRECTURL) + body);
                var linkedUser = new LinkedUser(user, reddit);
                clientCache.add(reddit);
                reddicord.getUserManager().addUser(linkedUser);
                dm.sendMessage("Account **/u/" + linkedUser.getRedditName() + "** successfully verified.").queue();
                return linkedUser;
            }).orTimeout(10, TimeUnit.MINUTES).exceptionally(t -> null).thenAccept(linkedUser -> {
                webCallback.clearStateListen(state);
                waitingUsers.remove(user);
                completableFuture.complete(Optional.ofNullable(linkedUser));
            });
        }, completableFuture::completeExceptionally);
        return completableFuture;
    }

}
