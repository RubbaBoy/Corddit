package com.uddernetworks.reddicord.discord.command;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public interface Command {
    boolean commandMatches(String base);
    default void onCommand(Member author, TextChannel channel, String rawMessage) {}
    default void onCommand(Member author, TextChannel channel, GuildMessageReceivedEvent event) {}
}
