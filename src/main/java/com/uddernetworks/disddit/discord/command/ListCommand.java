package com.uddernetworks.disddit.discord.command;

import com.uddernetworks.disddit.Disddit;
import com.uddernetworks.disddit.discord.EmbedUtils;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: Temporary command
public class ListCommand extends Command {

    private static final Logger LOGGER = LoggerFactory.getLogger(ListCommand.class);

    private final Disddit disddit;

    public ListCommand(Disddit disddit) {
        super("list");
        this.disddit = disddit;
    }

    @Override
    public void onCommand(Member author, TextChannel channel, String[] args) {
        if (args.length != 0) {
            EmbedUtils.error(channel, author, "Usage: /list");
            return;
        }

        var builder = new StringBuilder("**Users**\n");
        disddit.getUserManager().getUsers().forEach(linkedUser -> {
            var discord = linkedUser.getDiscordUser();
            var reddit = linkedUser.getRedditAccount().me().query().getAccount();
            var username = linkedUser.getRedditName();
            String karma;
            if (reddit == null) {
                karma = " Reddit null :(";
            } else {
                karma = " Karma: " + reddit.getCommentKarma() + " comment, " + reddit.getLinkKarma() + " post";
            }
            builder.append(discord.getName()).append(discord.getDiscriminator()).append(" **>** /u/").append(username).append(karma).append("\n");
        });

        builder.append("\n**Subreddits**\n");
        disddit.getSubredditManager().getSubreddits().forEach((guild, subredditLinks) -> {
            builder.append("__").append(guild.getName()).append("__\n");
            subredditLinks.forEach(subredditLink -> {
                builder.append("\t").append(subredditLink.getTextChannel().getAsMention()).append(" **>** ").append(subredditLink.getName()).append("\n");
            });
        });

        var text = builder.toString().trim();
        if (text.isEmpty()) text = "No data";
        channel.sendMessage(text).queue();
    }
}
