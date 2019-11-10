package com.uddernetworks.reddicord.reddit.user;

import net.dean.jraw.RedditClient;
import net.dv8tion.jda.api.entities.User;

public class LinkedUser {

    private final User discordUser;
    private final RedditClient redditAccount;

    public LinkedUser(User discordUser, RedditClient redditAccount) {
        this.discordUser = discordUser;
        this.redditAccount = redditAccount;
    }

    public User getDiscordUser() {
        return discordUser;
    }

    public RedditClient getRedditAccount() {
        return redditAccount;
    }
}
