package com.uddernetworks.reddicord;

import com.uddernetworks.reddicord.config.ConfigManager;
import com.uddernetworks.reddicord.database.DatabaseManager;
import com.uddernetworks.reddicord.discord.DiscordManager;
import com.uddernetworks.reddicord.discord.DiscordStateManager;
import com.uddernetworks.reddicord.discord.reddicord.SubredditManager;
import com.uddernetworks.reddicord.reddit.RedditManager;
import com.uddernetworks.reddicord.user.UserManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;
import java.io.File;

import static com.uddernetworks.reddicord.config.Config.DATABASE_PATH;
import static com.uddernetworks.reddicord.config.Config.TOKENSTORE;

public class Reddicord {

    private static final Logger LOGGER = LoggerFactory.getLogger(Reddicord.class);

    private final ConfigManager configManager;
    private final DatabaseManager databaseManager;
    private final RedditManager redditManager;
    private final DiscordManager discordManager;
    private final UserManager userManager;
    private final SubredditManager subredditManager;
    private final DiscordStateManager discordStateManager;

    public static void main(String[] args) throws LoginException {
        new Reddicord().main();
    }

    public Reddicord() {
        Thread.setDefaultUncaughtExceptionHandler((thread, exception) -> LOGGER.error("Error on thread {}", thread.getName(), exception));

        (this.configManager = new ConfigManager("src/main/resources/" + (new File("src/main/resources/secret.conf").exists() ? "secret.conf" : "config.conf"))).init();
        this.discordManager = new DiscordManager(this, configManager);
        this.redditManager = new RedditManager(this, configManager);

        this.databaseManager = new DatabaseManager(this);
        this.userManager = new UserManager(this, databaseManager);

        this.discordStateManager = new DiscordStateManager(this, discordManager, databaseManager);
        this.subredditManager = new SubredditManager(this, databaseManager, discordStateManager);
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
}
