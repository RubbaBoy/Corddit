package com.uddernetworks.reddicord.discord.reddicord;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;

public class SubredditLink {

    private final Guild guild;
    private final TextChannel textChannel;
    private final String subreddit;

    public SubredditLink(TextChannel textChannel, String subreddit) {
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

    public String getSubreddit() {
        return subreddit;
    }
}
