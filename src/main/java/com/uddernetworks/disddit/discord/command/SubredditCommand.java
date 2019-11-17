package com.uddernetworks.disddit.discord.command;

import com.uddernetworks.disddit.Disddit;
import com.uddernetworks.disddit.discord.EmbedUtils;
import com.uddernetworks.disddit.discord.disddit.SubredditLink;
import com.uddernetworks.disddit.discord.disddit.SubredditManager;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Collectors;

public class SubredditCommand extends Command {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubredditCommand.class);

    private final Disddit disddit;
    private SubredditManager subredditManager;

    public SubredditCommand(Disddit disddit) {
        super("subreddit");
        this.disddit = disddit;
        this.subredditManager = disddit.getSubredditManager();
    }

    @Override
    public void onCommand(Member author, TextChannel channel, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            LOGGER.info("Help!");
            subredditHelp(author, channel);
            return;
        }

        if (author.getIdLong() != 249962392241307649L && !author.hasPermission(Permission.ADMINISTRATOR)) {
            EmbedUtils.error(channel, author, "You must be administrator for that!");
            return;
        }

        var guild = channel.getGuild();

        switch (args[0].toLowerCase()) {
            case "list":
                EmbedUtils.sendEmbed(channel, author, "Subreddits", embedBuilder -> {
                    embedBuilder.setDescription("The following are all the subreddits in the Discord:\n\n**" +
                            subredditManager.getSubreddits(channel.getGuild()).stream()
                                    .map(SubredditLink::getName)
                                    .map("#"::concat)
                                    .collect(Collectors.joining("\n"))
                            + "**");
                });
                break;
            case "add":
                if (args.length != 2) {
                    subredditHelp(author, channel);
                    return;
                }

                var subredditName = args[1];
                if (!StringUtils.isAlphanumeric(subredditName)) {
                    EmbedUtils.error(channel, author, "That's not a valid subreddit name!");
                    return;
                }

                subredditManager.addSubreddit(guild, args[1])
                        .thenAccept(subredditOptional -> subredditOptional.ifPresentOrElse(subreddit -> {
                            channel.sendMessage("Added subreddit '" + subreddit.getName() + "'").queue();
                        }, () -> {
                            EmbedUtils.error(channel, author, "Couldn't find the given subreddit!");
                        }));
                break;
            case "remove":
                if (args.length != 2) {
                    subredditHelp(author, channel);
                    return;
                }

                subredditName = args[1];
                if (!StringUtils.isAlphanumeric(subredditName)) {
                    EmbedUtils.error(channel, author, "That's not a valid subreddit name!");
                    return;
                }

                subredditManager.removeSubreddit(guild, args[1])
                        .thenRun(() -> channel.sendMessage("Removed subreddit '" + args[1] + "'").queue());
                break;
        }
    }

    private void subredditHelp(Member author, TextChannel textChannel) {
        EmbedUtils.sendEmbed(textChannel, author, "Subreddit help", embed ->
                embed.setDescription("Help for the /subreddit command")
                        .addField("**/subreddit help**", "Show this help menu", false)
                        .addField("**/subreddit list**", "Lists the active subreddits", false)
                        .addField("**/subreddit add [subreddit]**", "Adds the given subreddit", false)
                        .addField("**/subreddit remove [subreddit]**", "Removes the given subreddit", false)
        );
    }
}
