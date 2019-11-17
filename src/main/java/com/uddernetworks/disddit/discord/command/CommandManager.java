package com.uddernetworks.disddit.discord.command;

import com.uddernetworks.disddit.Disddit;
import net.dv8tion.jda.api.JDA;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.uddernetworks.disddit.config.Config.PREFIX;

public class CommandManager {

    private final Disddit disddit;
    private final CommandHandler commandHandler;
    private final JDA jda;

    private List<Command> commands = new ArrayList<>();
    private String prefix;

    public CommandManager(Disddit disddit) {
        this.disddit = disddit;
        this.jda = disddit.getDiscordManager().getJDA();
        this.prefix = disddit.getConfigManager().get(PREFIX);

        jda.addEventListener(this.commandHandler = new CommandHandler(disddit, this));
    }

    public CommandManager registerCommand(Command command) {
        commands.add(command);
        return this;
    }

    public <T extends Command> Optional<T> getCommand(Class<T> clazz) {
        return commands.stream().filter(command -> command.getClass().equals(clazz)).findFirst().map(command -> (T) command);
    }

    public List<Command> getCommands() {
        return commands;
    }

    public String getPrefix() {
        return prefix;
    }
}
