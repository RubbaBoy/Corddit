package com.uddernetworks.disddit.discord.command.evaluate;

import com.uddernetworks.disddit.Disddit;
import com.uddernetworks.disddit.discord.EmbedUtils;
import com.uddernetworks.disddit.discord.command.Command;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.uddernetworks.disddit.discord.reaction.ReactManager.Codepoint.GREEN_CHECK;
import static com.uddernetworks.disddit.discord.reaction.ReactManager.Codepoint.YELLOW_CIRCLE;

public class EvaluateCommand extends Command {

    private static final Logger LOGGER = LoggerFactory.getLogger(EvaluateCommand.class);

    private final Disddit disddit;
    private final JShellRemote shell;

    public EvaluateCommand(Disddit disddit) {
        this.disddit = disddit;
        this.shell = new JShellRemote(disddit);
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

        message.addReaction(YELLOW_CIRCLE).queue();
        shell.runCode(channel, author, code);
        message.clearReactions().queue($ -> message.addReaction(GREEN_CHECK).queue());
    }
}
