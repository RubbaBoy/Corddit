package com.uddernetworks.corddit.discord.corddit;

import com.uddernetworks.corddit.Corddit;
import com.uddernetworks.corddit.config.ConfigManager;
import com.uddernetworks.corddit.reddit.RedditManager;
import net.dean.jraw.RedditClient;
import net.dean.jraw.models.Subreddit;
import net.dean.jraw.oauth.Credentials;
import net.dean.jraw.oauth.OAuthHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static com.uddernetworks.corddit.config.Config.CLIENTID;
import static com.uddernetworks.corddit.config.Config.CLIENTSECRET;

public class SubredditDataManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubredditDataManager.class);

    // TODO: This should probably be device-specific as the name implies, but whatever
    private static final UUID DEVICE_ID = UUID.fromString("54f24a5f-4522-4c34-b907-861c5af2dfc6");

    private final Corddit corddit;
    private final RedditManager redditManager;
    private final ConfigManager configManager;

    private RedditClient userlessClient;

    public SubredditDataManager(Corddit corddit) {
        this.corddit = corddit;
        this.redditManager = corddit.getRedditManager();
        this.configManager = corddit.getConfigManager();
    }

    public void init() {
        var userless = Credentials.userless(configManager.get(CLIENTID), configManager.get(CLIENTSECRET), DEVICE_ID);
        userlessClient = OAuthHelper.automatic(redditManager.getNetworkAdapter(), userless);
    }

    public CompletableFuture<Optional<Subreddit>> getSubreddit(String name) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return Optional.of(userlessClient.subreddit(name).about());
            } catch (Exception ignored) {
                return Optional.empty();
            }
        });
    }

//    public CompletableFuture<Void> upvote(Subreddit subredd)

    public RedditClient getClient() {
        return userlessClient;
    }
}
