package com.uddernetworks.reddicord.reddit.user;

import com.uddernetworks.reddicord.Reddicord;
import com.uddernetworks.reddicord.discord.DiscordManager;
import net.dv8tion.jda.api.entities.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserManager.class);

    private final Reddicord reddicord;
    private final DiscordManager discordManager;
    private final List<RedditUser> users = new ArrayList<>();

    public UserManager(Reddicord reddicord, DiscordManager discordManager) {
        this.reddicord = reddicord;
        this.discordManager = discordManager;
    }

    public void addUser(RedditUser redditUser) {
        users.add(redditUser);
    }

    public Optional<RedditUser> getUser(User discordUser) {
        return users.stream().filter(redditUser -> redditUser.getDiscordUser().equals(discordUser)).findFirst();
    }
}
