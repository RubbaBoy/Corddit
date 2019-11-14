package com.uddernetworks.reddicord.discord.command.evaluate;

import com.uddernetworks.reddicord.Reddicord;
import com.uddernetworks.reddicord.discord.EmbedUtils;
import com.uddernetworks.reddicord.discord.command.Command;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EvaluateCommand extends Command {

    private static final Logger LOGGER = LoggerFactory.getLogger(EvaluateCommand.class);

    private final Reddicord reddicord;
    private final JShellRemote shell;

    public EvaluateCommand(Reddicord reddicord) {
        this.reddicord = reddicord;
        this.shell = new JShellRemote(reddicord);
    }

    @Override
    public boolean commandMatches(String base) {
        return base.equalsIgnoreCase("e") || base.equalsIgnoreCase("eval") || base.equalsIgnoreCase("evaluate");
    }

    @Override
    public void onCommand(Member author, TextChannel channel, GuildMessageReceivedEvent event) {
        var message = event.getMessage();
        var rawMessage = message.getContentRaw();
        if (author.getIdLong() != 249962392241307649L) {
            EmbedUtils.error(channel, author, "Only RubbaBoy can do this!");
            return;
        }

        var code = rawMessage.substring(rawMessage.indexOf(" ") + 1);
        if (code.startsWith("```")) {
            if (code.length() >= 6) code = code.substring(3, code.length() - 3);
        } else if (code.startsWith("`")) {
            if (code.length() >= 2) code = code.substring(1, code.length() - 1);
        }
        if (code.startsWith("Java") || code.startsWith("java")) {
            code = code.substring(4);
        }
        code = code.trim();

        LOGGER.info("Code: \n{}\n", code);

        var yellowCircle = "\uD83D\uDFE1";
        var checkMark = "\u2705";

        message.addReaction(yellowCircle).queue();
        shell.runCode(channel, author, code);
        message.clearReactions().queue($ -> message.addReaction(checkMark).queue());
    }
}
