package com.uddernetworks.corddit.discord.command;

import com.uddernetworks.corddit.Corddit;
import com.uddernetworks.corddit.discord.HelpUtility;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;

public class HelpCommand extends Command {

    private Corddit corddit;

    public HelpCommand(Corddit corddit) {
        super("help");
        this.corddit = corddit;
    }

    @Override
    public void onCommand(Member author, TextChannel channel, String rawMessage) {
        HelpUtility.send(author, channel);
    }
}
