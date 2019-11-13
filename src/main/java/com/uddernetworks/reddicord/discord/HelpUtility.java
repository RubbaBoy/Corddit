package com.uddernetworks.reddicord.discord;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;

public class HelpUtility {

    public static final char ZWS = '\u200b';
    private static String commandPrefix = "/";

    public static void send(Member member, TextChannel channel) {
        EmbedUtils.sendEmbed(channel, member, "Reddicord Command Help", embed -> {
                    embed.setDescription("Help for the Reddicord commands (**base** is just the base command, and no arguments)");
                    addCommand(embed, "help",
                            commandRow("base", "Shows this help menu"));
                    addCommand(embed, "subreddit",
                            commandRow("base", "Shows this help menu") +
                            commandRow("help", "Shows the command's help") +
                            commandRow("list", "Lists the subreddits for the guild") +
                            commandRow("add [subreddit]", "Adds the given subreddit to the guild") +
                            commandRow("remove [subreddit]", "Removes the given subreddit from the guild")
                    );
                    addCommand(embed, "link",
                            commandRow("base", "DMs you a link to link your reddit and Discord account")
                    );
                    addCommand(embed, "list",
                            commandRow("base", "Lists all linked accounts and subreddits. This command may be removed in the future")
                    );
                }
        );
    }

    private static void addCommand(EmbedBuilder embed, String command, String description) {
        embed.addField(commandPrefix + command, description, false);
    }

    private static String commandRow(String name, String description) {
        return ("    **" + name + "**" + " ".repeat(7)).replace(" ", ZWS + " ") + " - " + description + "\n";
    }

    public static String space(int amount) {
        return (ZWS + " ").repeat(amount);
    }

    public static void setCommandPrefix(String commandPrefix) {
        HelpUtility.commandPrefix = commandPrefix;
    }

}
