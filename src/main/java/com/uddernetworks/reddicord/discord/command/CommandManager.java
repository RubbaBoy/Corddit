package com.uddernetworks.reddicord.discord.command;

import com.uddernetworks.reddicord.Reddicord;
import net.dv8tion.jda.api.JDA;

import java.util.ArrayList;
import java.util.List;

import static com.uddernetworks.reddicord.config.Config.PREFIX;

public class CommandManager {

    private final Reddicord reddicord;
    private final CommandHandler commandHandler;
    private final JDA jda;

    private List<Command> commands = new ArrayList<>();
    private String prefix;

    public CommandManager(Reddicord reddicord) {
        this.reddicord = reddicord;
        this.jda = reddicord.getDiscordManager().getJDA();
        this.prefix = reddicord.getConfigManager().get(PREFIX);

        jda.addEventListener(this.commandHandler = new CommandHandler(reddicord, this));
    }

    public CommandManager registerCommand(Command command) {
        commands.add(command);
        return this;
    }

    public List<Command> getCommands() {
        return commands;
    }

    public String getPrefix() {
        return prefix;
    }
}
