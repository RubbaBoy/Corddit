package com.uddernetworks.reddicord.discord;

import com.uddernetworks.reddicord.Reddicord;
import com.uddernetworks.reddicord.config.ConfigManager;
import com.uddernetworks.reddicord.discord.command.CommandManager;
import com.uddernetworks.reddicord.discord.command.HelpCommand;
import com.uddernetworks.reddicord.discord.command.LinkCommand;
import com.uddernetworks.reddicord.discord.command.ListCommand;
import com.uddernetworks.reddicord.discord.command.SetupCommand;
import com.uddernetworks.reddicord.discord.reaction.ReactManager;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.security.auth.login.LoginException;
import java.util.Optional;

import static com.uddernetworks.reddicord.config.Config.TOKEN;

public class DiscordManager extends ListenerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiscordManager.class);

    private final Reddicord reddicord;
    private final ConfigManager configManager;
    private CommandManager commandManager;
    private ReactManager reactManager;
    private JDA jda;

    public DiscordManager(Reddicord reddicord) {
        this.reddicord = reddicord;
        this.configManager = reddicord.getConfigManager();
    }

    public void init() throws LoginException {
        this.jda = new JDABuilder()
                .setToken(configManager.get(TOKEN))
                .setStatus(OnlineStatus.ONLINE)
                .addEventListeners(this)
                .addEventListeners(new EmbedUtils())
                .addEventListeners(this.reactManager = new ReactManager(this))
                .build();

        (this.commandManager = new CommandManager(reddicord))
                .registerCommand(new HelpCommand(reddicord))
                .registerCommand(new LinkCommand(reddicord))
                .registerCommand(new ListCommand(reddicord))
                .registerCommand(new SetupCommand(reddicord));
    }

    @Override
    public void onReady(@Nonnull ReadyEvent event) {
        LOGGER.info("Bot is ready!");
        reddicord.getUserManager().load();
    }

    public Optional<User> getUser(long id) {
        return Optional.ofNullable(jda.getUserById(id));
    }

    public Reddicord getReddicord() {
        return reddicord;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }

    public ReactManager getReactManager() {
        return reactManager;
    }

    public JDA getJDA() {
        return jda;
    }
}
