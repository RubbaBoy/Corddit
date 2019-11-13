package com.uddernetworks.reddicord.discord.command;

import com.uddernetworks.reddicord.Reddicord;
import com.uddernetworks.reddicord.discord.EmbedUtils;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: Temporary command
public class ListCommand extends Command {

    private static final Logger LOGGER = LoggerFactory.getLogger(ListCommand.class);

    private final Reddicord reddicord;

    public ListCommand(Reddicord reddicord) {
        super("list");
        this.reddicord = reddicord;
    }

    @Override
    public void onCommand(Member author, TextChannel channel, String[] args) {
        if (args.length != 0) {
            EmbedUtils.error(channel, author, "Usage: /list");
            return;
        }

        var builder = new StringBuilder("**Users**\n");
        reddicord.getUserManager().getUsers().forEach(linkedUser -> {
            var discord = linkedUser.getDiscordUser();
            var username = linkedUser.getRedditName();
            builder.append(discord.getName()).append(discord.getDiscriminator()).append(" **>** /u/").append(username).append("\n");
        });

        builder.append("\n**Subreddits**\n");
        reddicord.getSubredditManager().getSubreddits().forEach((guild, subredditLinks) -> {
            builder.append("__").append(guild.getName()).append("__\n");
            subredditLinks.forEach(subredditLink -> {
                builder.append("\t").append(subredditLink.getTextChannel().getAsMention()).append(" **>** ").append(subredditLink.getSubreddit()).append("\n");
            });
        });

        var text = builder.toString().trim();
        if (text.isEmpty()) text = "No data";
        channel.sendMessage(text).queue();
    }
}
