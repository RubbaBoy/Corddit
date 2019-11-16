package com.uddernetworks.reddicord.discord.reddicord;

import com.uddernetworks.reddicord.Reddicord;
import com.uddernetworks.reddicord.config.ConfigManager;
import com.uddernetworks.reddicord.reddit.RedditManager;
import net.dean.jraw.RedditClient;
import net.dean.jraw.models.Subreddit;
import net.dean.jraw.oauth.Credentials;
import net.dean.jraw.oauth.OAuthHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static com.uddernetworks.reddicord.config.Config.CLIENTID;
import static com.uddernetworks.reddicord.config.Config.CLIENTSECRET;

public class SubredditDataManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubredditDataManager.class);

    // TODO: This should probably be device-specific as the name implies, but whatever
    private static final UUID DEVICE_ID = UUID.fromString("54f24a5f-4522-4c34-b907-861c5af2dfc6");

    private final Reddicord reddicord;
    private final RedditManager redditManager;
    private final ConfigManager configManager;

    private RedditClient userlessClient;

    public SubredditDataManager(Reddicord reddicord) {
        this.reddicord = reddicord;
        this.redditManager = reddicord.getRedditManager();
        this.configManager = reddicord.getConfigManager();
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
