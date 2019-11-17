package com.uddernetworks.disddit.user;

import net.dean.jraw.RedditClient;
import net.dv8tion.jda.api.entities.User;

public class LinkedUser {

    private final User discordUser;
    private final RedditClient redditAccount;
    private final String redditName;

    public LinkedUser(User discordUser, RedditClient redditAccount) {
        this.discordUser = discordUser;
        this.redditAccount = redditAccount;
        this.redditName = redditAccount.getAuthManager().currentUsername();
    }

    public User getDiscordUser() {
        return discordUser;
    }

    public RedditClient getRedditAccount() {
        return redditAccount;
    }

    public String getRedditName() {
        return redditName;
    }
}
