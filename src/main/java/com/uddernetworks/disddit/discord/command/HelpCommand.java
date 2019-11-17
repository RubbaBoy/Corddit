package com.uddernetworks.disddit.discord.command;

import com.uddernetworks.disddit.Disddit;
import com.uddernetworks.disddit.discord.HelpUtility;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;

public class HelpCommand extends Command {

    private Disddit disddit;

    public HelpCommand(Disddit disddit) {
        super("help");
        this.disddit = disddit;
    }

    @Override
    public void onCommand(Member author, TextChannel channel, String rawMessage) {
        HelpUtility.send(author, channel);
    }
}
