package com.uddernetworks.reddicord.discord.command;

import com.uddernetworks.reddicord.Reddicord;
import com.uddernetworks.reddicord.discord.HelpUtility;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;

public class HelpCommand implements Command {

    private Reddicord reddicord;

    public HelpCommand(Reddicord reddicord) {
        this.reddicord = reddicord;
    }

    @Override
    public boolean commandMatches(String base) {
        return base.equalsIgnoreCase("help");
    }

    @Override
    public void onCommand(Member author, TextChannel channel, String rawMessage) {
        HelpUtility.send(author, channel);
    }
}
