package com.uddernetworks.disddit.discord.command;

import com.uddernetworks.disddit.Disddit;
import com.uddernetworks.disddit.discord.EmbedUtils;
import com.uddernetworks.disddit.reddit.RedditManager;
import com.uddernetworks.disddit.user.UserManager;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LinkCommand extends Command {

    private static final Logger LOGGER = LoggerFactory.getLogger(LinkCommand.class);

    private final Disddit disddit;
    private final RedditManager redditManager;
    private final UserManager userManager;

    public LinkCommand(Disddit disddit) {
        super("link");
        this.disddit = disddit;
        this.redditManager = disddit.getRedditManager();
        this.userManager = disddit.getUserManager();
    }

    @Override
    public void onCommand(Member author, TextChannel channel, String[] args) {
        if (args.length != 0) {
            EmbedUtils.error(channel, author, "Usage: /link");
            return;
        }

        if (redditManager.isWaiting(author)) {
            channel.sendMessage(author.getAsMention() + " I'm already waiting on your verification, check your DMs.").queue();
            return;
        }

        var existingLink = userManager.getUser(author.getUser());
        if (existingLink.isPresent()) {
            channel.sendMessage(author.getAsMention() + " You are already linked with **/u/" + existingLink.get().getRedditName() + "**. In the future there will be an /unlink command, so check back later.").queue();
            return;
        }

        channel.sendMessage(author.getAsMention() + " Check your DMs for a verification link").queue();

        redditManager.linkClient(author).thenAccept(clientOptional ->
                clientOptional.ifPresentOrElse(client -> {
                    LOGGER.info("Verified {} as {}", author.getUser().getName(), client.getRedditName());
                }, () -> {
                    LOGGER.info("Couldn't find user! Timeout?");
                })).exceptionally(t -> {
            channel.sendMessage("An error occurred while opening the DM, are your DMs closed?").queue();
            return null;
        });
    }
}
