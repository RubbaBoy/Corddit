package com.uddernetworks.corddit.discord.corddit;

import net.dean.jraw.models.Subreddit;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;

public class SubredditLink {

    private final Guild guild;
    private final TextChannel textChannel;
    private final Subreddit subreddit;

    public SubredditLink(TextChannel textChannel, Subreddit subreddit) {
        this.guild = textChannel.getGuild();
        this.textChannel = textChannel;
        this.subreddit = subreddit;
    }

    public Guild getGuild() {
        return guild;
    }

    public TextChannel getTextChannel() {
        return textChannel;
    }

    public String getName() {
        return subreddit.getName();
    }

    public Subreddit getSubreddit() {
        return subreddit;
    }
}
