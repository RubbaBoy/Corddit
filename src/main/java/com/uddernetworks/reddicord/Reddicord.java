package com.uddernetworks.reddicord;

import com.uddernetworks.reddicord.reddit.RedditManager;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

public class Reddicord {

    private ConfigManager configManager;
    private RedditManager redditManager;

    public static void main(String[] args) throws IOException, URISyntaxException {
        new Reddicord().main();
    }

    private void main() throws IOException, URISyntaxException {
        (configManager = new ConfigManager("src/main/resources/" + (new File("src/main/resources/secret.conf").exists() ? "secret.conf" : "config.conf"))).init();
        (redditManager = new RedditManager(this)).init();
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }
}
