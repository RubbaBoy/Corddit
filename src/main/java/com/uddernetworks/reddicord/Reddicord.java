package com.uddernetworks.reddicord;

import com.uddernetworks.reddicord.config.ConfigManager;
import com.uddernetworks.reddicord.discord.DiscordManager;
import com.uddernetworks.reddicord.reddit.RedditManager;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import static com.uddernetworks.reddicord.config.Config.TOKENSTORE;

public class Reddicord {

    private ConfigManager configManager;
    private RedditManager redditManager;
    private DiscordManager discordManager;

    public static void main(String[] args) throws IOException, URISyntaxException, LoginException {
        new Reddicord().main();
    }

    private void main() throws IOException, URISyntaxException, LoginException {
        (configManager = new ConfigManager("src/main/resources/" + (new File("src/main/resources/secret.conf").exists() ? "secret.conf" : "config.conf"))).init();
        (discordManager = new DiscordManager(this))
                .init();
        (redditManager = new RedditManager(this))
                .init(new File(configManager.<String>get(TOKENSTORE)));
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public RedditManager getRedditManager() {
        return redditManager;
    }

    public DiscordManager getDiscordManager() {
        return discordManager;
    }
}
