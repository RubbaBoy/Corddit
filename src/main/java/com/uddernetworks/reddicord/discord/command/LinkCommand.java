package com.uddernetworks.reddicord.discord.command;

import com.uddernetworks.reddicord.Reddicord;
import com.uddernetworks.reddicord.discord.EmbedUtils;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LinkCommand extends Command {

    private static final Logger LOGGER = LoggerFactory.getLogger(LinkCommand.class);

    private final Reddicord reddicord;

    public LinkCommand(Reddicord reddicord) {
        super("link");
        this.reddicord = reddicord;
    }

    @Override
    public void onCommand(Member author, TextChannel channel, String[] args) {
        if (args.length != 1) {
            EmbedUtils.error(channel, author, "Usage: /link");
            return;
        }

        channel.sendMessage(author.getAsMention() + " Check your DMs for a verification link").queue();

        reddicord.getRedditManager().linkClient(author).thenAccept(clientOptional -> {
            clientOptional.ifPresentOrElse(client -> {
                var query = client.me().query();

                LOGGER.info("Hello {}!", query.getName());
                var jda = reddicord.getDiscordManager().getJDA();
                jda.getGuildById(642549950361632778L).getTextChannelById(642549950361632781L)
                        .sendMessage(author.getAsMention() + " verified as " + query.getName()).queue();
            }, () -> {
                LOGGER.info("Couldn't find user!");
            });
        });
    }
}
