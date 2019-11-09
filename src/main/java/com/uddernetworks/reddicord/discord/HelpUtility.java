package com.uddernetworks.reddicord.discord;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;

public class HelpUtility {

    public static final char ZWS = '\u200b';

    public static void send(Member member, TextChannel channel) {
        EmbedUtils.sendEmbed(channel, member, "Reddicord Command Help", embed -> embed.setDescription("Help for the Reddicord commands")
                .addField("**!help**", "Show this help menu", false));
    }

    private static String commandRow(String name, String description) {
        return ("    **" + name + "**" + " ".repeat(7)).replace(" ", ZWS + " ") + " - " + description + "\n";
    }

    public static String space(int amount) {
        return (ZWS + " ").repeat(amount);
    }

}
