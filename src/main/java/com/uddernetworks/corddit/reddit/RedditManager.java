package com.uddernetworks.corddit.reddit;

import com.uddernetworks.corddit.Corddit;
import com.uddernetworks.corddit.config.ConfigManager;
import com.uddernetworks.corddit.discord.EmbedUtils;
import com.uddernetworks.corddit.user.LinkedUser;
import com.uddernetworks.corddit.user.web.WebCallback;
import net.dean.jraw.RedditClient;
import net.dean.jraw.http.NetworkAdapter;
import net.dean.jraw.http.OkHttpNetworkAdapter;
import net.dean.jraw.http.UserAgent;
import net.dean.jraw.models.OAuthData;
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
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.uddernetworks.corddit.config.Config.CLIENTID;
import static com.uddernetworks.corddit.config.Config.CLIENTSECRET;
import static com.uddernetworks.corddit.config.Config.REDIRECTURL;

public class RedditManager {

    public static final UserAgent USER_AGENT = new UserAgent("bot", "com.uddernetworks.corddit", "1.0.0", "Corddit");
    private static final Logger LOGGER = LoggerFactory.getLogger(RedditManager.class);

    private final Corddit corddit;
    private final ConfigManager configManager;
    private final WebCallback webCallback;

    private final List<RedditClient> clientCache = Collections.synchronizedList(new ArrayList<>());
    private final List<User> waitingUsers = Collections.synchronizedList(new ArrayList<>());

    private Credentials credentials;
    private NetworkAdapter networkAdapter;
    private JsonFileTokenStore store;

    public RedditManager(Corddit corddit, ConfigManager configManager, WebCallback webCallback) {
        this.corddit = corddit;
        this.configManager = configManager;
        this.webCallback = webCallback;
    }

    public void init(File tokenStore) {
        credentials = Credentials.webapp(configManager.get(CLIENTID), configManager.get(CLIENTSECRET), configManager.get(REDIRECTURL));

        networkAdapter = new OkHttpNetworkAdapter(USER_AGENT);
        store = new JsonFileTokenStore(tokenStore);
        if (tokenStore.exists()) store.load();
        store.setAutoPersist(true);

        LOGGER.info("Usernames: {}", store.getUsernames());
        store.getUsernames().forEach(this::createAndAddAccount);
    }

    private Optional<RedditClient> createAndAddAccount(String username) {
        var tokenStoreUser = store.fetchLatest(username);

        var inspected = store.inspect(username);
        LOGGER.info("Inspected: {}", inspected);
        LOGGER.info("Token store for {} is {}", username, tokenStoreUser);

        RedditClient client;
        if (tokenStoreUser == null) {
            var refresh = store.fetchRefreshToken(username);
            LOGGER.info("Couldn't find tokenStoreUSer, it's probably expired. However refresh = {}", refresh);

            // See AccountHelper.kt#101
            var emptyData = OAuthData.create("", Collections.emptyList(), refresh, new Date(0L));
            client = new RedditClient(networkAdapter, emptyData, credentials, store, username);
        } else {
            client = new RedditClient(networkAdapter, tokenStoreUser, credentials, store, username);
        }

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

            dm.sendMessage(EmbedUtils.createEmbed(member, "Reddit Link", embed -> embed.setDescription("To link your Reddit account with Corddit, please click [here](" + authUrl + ").\n\nIf you don't complete this within 10 minutes, the verification will be ignored and you will need to redo the /link command."))).queue();

            webCallback.listenForState(state).thenApply(body -> {
                var reddit = statefulAuthHelper.onUserChallenge(configManager.get(REDIRECTURL) + body);
                var linkedUser = new LinkedUser(user, reddit);
                clientCache.add(reddit);
                corddit.getUserManager().addUser(linkedUser);
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

    public Credentials getCredentials() {
        return credentials;
    }

    public NetworkAdapter getNetworkAdapter() {
        return networkAdapter;
    }
}
