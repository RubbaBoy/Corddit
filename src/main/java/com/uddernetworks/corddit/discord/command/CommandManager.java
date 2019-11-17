package com.uddernetworks.corddit.discord.command;

import com.uddernetworks.corddit.Corddit;
import net.dv8tion.jda.api.JDA;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.uddernetworks.corddit.config.Config.PREFIX;

public class CommandManager {

    private final Corddit corddit;
    private final CommandHandler commandHandler;
    private final JDA jda;

    private List<Command> commands = new ArrayList<>();
    private String prefix;

    public CommandManager(Corddit corddit) {
        this.corddit = corddit;
        this.jda = corddit.getDiscordManager().getJDA();
        this.prefix = corddit.getConfigManager().get(PREFIX);

        jda.addEventListener(this.commandHandler = new CommandHandler(corddit, this));
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
