package com.uddernetworks.reddicord;

import com.uddernetworks.reddicord.config.ConfigManager;
import com.uddernetworks.reddicord.database.DatabaseManager;
import com.uddernetworks.reddicord.discord.DiscordManager;
import com.uddernetworks.reddicord.reddit.RedditManager;
import com.uddernetworks.reddicord.reddit.user.UserManager;

import javax.security.auth.login.LoginException;
import java.io.File;

import static com.uddernetworks.reddicord.config.Config.DATABASE_PATH;
import static com.uddernetworks.reddicord.config.Config.TOKENSTORE;

public class Reddicord {

    private final ConfigManager configManager;
    private final DatabaseManager databaseManager;
    private final RedditManager redditManager;
    private final DiscordManager discordManager;
    private final UserManager userManager;

    public static void main(String[] args) throws LoginException {
        new Reddicord().main();
    }

    public Reddicord() {
        (this.configManager = new ConfigManager("src/main/resources/" + (new File("src/main/resources/secret.conf").exists() ? "secret.conf" : "config.conf"))).init();
        this.discordManager = new DiscordManager(this);
        this.redditManager = new RedditManager(this);
        (this.databaseManager = new DatabaseManager(this)).init(configManager.get(DATABASE_PATH));
        this.userManager = new UserManager(this);
    }

    private void main() throws LoginException {
        discordManager.init();
        redditManager.init(new File(configManager.<String>get(TOKENSTORE)));
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
}
