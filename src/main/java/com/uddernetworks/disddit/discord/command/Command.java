package com.uddernetworks.disddit.discord.command;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Command {

    private static final Logger LOGGER = LoggerFactory.getLogger(Command.class);

    private String base;

    public Command() {}

    Command(String base) {
        this.base = base;
    }

    public boolean commandMatches(String base) {
        if (this.base == null) {
            LOGGER.error("Error with implementation of Command: #commandMatches must be implemented if no command base is set in the constructor.");
            return false;
        }

        return this.base.equalsIgnoreCase(base);
    }

    public void onCommand(Member author, TextChannel channel, String[] args) {}
    public void onCommand(Member author, TextChannel channel, String rawMessage) {}
    public void onCommand(Member author, TextChannel channel, GuildMessageReceivedEvent event) {}
}
