package com.uddernetworks.reddicord.discord.command;

import com.uddernetworks.reddicord.Reddicord;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.annotation.Nonnull;

public class CommandHandler extends ListenerAdapter {

    private final Reddicord reddicord;
    private final CommandManager commandManager;

    public CommandHandler(Reddicord reddicord, CommandManager commandManager) {
        this.reddicord = reddicord;
        this.commandManager = commandManager;
    }

    @Override
    public void onGuildMessageReceived(@Nonnull GuildMessageReceivedEvent event) {
        var author = event.getAuthor();
        var member = event.getGuild().getMember(author);
        var channel = event.getChannel();
        if (author.isBot() || author.isFake()) return;
        var message = event.getMessage();
        var raw = message.getContentRaw();
        var base = raw.split("\\s+")[0];
        var prefix = commandManager.getPrefix();
        if (!base.startsWith(prefix)) return;
        var strippedBase = base.substring(prefix.length());
        commandManager.getCommands().stream().filter(command -> command.commandMatches(strippedBase)).findFirst().ifPresent(command ->  {
            command.onCommand(member, channel, raw.split("\\s+"));
            command.onCommand(member, channel, raw);
            command.onCommand(member, channel, event);
        });
    }
}
