package com.uddernetworks.corddit;

import com.uddernetworks.corddit.config.ConfigManager;
import com.uddernetworks.corddit.database.DatabaseManager;
import com.uddernetworks.corddit.discord.DiscordManager;
import com.uddernetworks.corddit.discord.DiscordStateManager;
import com.uddernetworks.corddit.discord.HelpUtility;
import com.uddernetworks.corddit.discord.corddit.SubredditManager;
import com.uddernetworks.corddit.discord.corddit.comments.CommentManager;
import com.uddernetworks.corddit.reddit.RedditManager;
import com.uddernetworks.corddit.user.UserManager;
import com.uddernetworks.corddit.user.web.WebCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;
import java.io.File;

import static com.uddernetworks.corddit.config.Config.DATABASE_PATH;
import static com.uddernetworks.corddit.config.Config.PREFIX;
import static com.uddernetworks.corddit.config.Config.TOKENSTORE;
import static com.uddernetworks.corddit.discord.corddit.SubredditManager.clearMessages;

public class Corddit {

    private static final Logger LOGGER = LoggerFactory.getLogger(Corddit.class);

    private final ConfigManager configManager;
    private final DatabaseManager databaseManager;
    private final RedditManager redditManager;
    private final DiscordManager discordManager;
    private final UserManager userManager;
    private final SubredditManager subredditManager;
    private final CommentManager commentManager;
    private final DiscordStateManager discordStateManager;
    private final WebCallback webCallback;

    public static void main(String[] args) throws LoginException {
        new Corddit().main();
    }

    public Corddit() {
        Thread.setDefaultUncaughtExceptionHandler((thread, exception) -> LOGGER.error("Error on thread {}", thread.getName(), exception));

        (this.configManager = new ConfigManager("src/main/resources/" + (new File("src/main/resources/secret.conf").exists() ? "secret.conf" : "config.conf"))).init();
        this.webCallback = new WebCallback(this, configManager);
        this.discordManager = new DiscordManager(this, configManager);
        this.redditManager = new RedditManager(this, configManager, webCallback);

        this.databaseManager = new DatabaseManager(this);
        this.userManager = new UserManager(this, databaseManager);

        this.discordStateManager = new DiscordStateManager(this, discordManager, databaseManager);
        this.subredditManager = new SubredditManager(this, databaseManager, discordStateManager);
        this.commentManager = new CommentManager(this);

        HelpUtility.setCommandPrefix(configManager.get(PREFIX));
    }

    public void main() throws LoginException {
        LOGGER.info("Initializing database manager...");
        databaseManager.init(configManager.get(DATABASE_PATH));

        LOGGER.info("Initializing discord manager...");
        discordManager.init().join();

        LOGGER.info("Initializing reddit manager...");
        redditManager.init(new File(configManager.<String>get(TOKENSTORE)));

        LOGGER.info("Initializing discord state manager...");
        discordStateManager.init().join();

        LOGGER.info("Initializing user link manager...");
        userManager.load().join();

        LOGGER.info("Initializing subreddit manager...");
        subredditManager.init().join();

        LOGGER.info("Starting web callback server...");
        webCallback.start();

        LOGGER.info("Clearing comment threads from discord...");
        databaseManager.getAllGuildCategories().join().forEach((guild, category) -> {
            category.getTextChannels().forEach(channel -> {
                if (channel.getName().startsWith("thread-")) {
                    channel.delete().queue();
                } else {
                    clearMessages(channel);
                }
            });
        });

        LOGGER.info("Everything is ready!");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public RedditManager getRedditManager() {
        return redditManager;
    }

    public DiscordManager getDiscordManager() {
        return discordManager;
    }

    public UserManager getUserManager() {
        return userManager;
    }

    public SubredditManager getSubredditManager() {
        return subredditManager;
    }

    public DiscordStateManager getDiscordStateManager() {
        return discordStateManager;
    }

    public WebCallback getWebCallback() {
        return webCallback;
    }

    public CommentManager getCommentManager() {
        return commentManager;
    }
}
