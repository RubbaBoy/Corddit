package com.uddernetworks.disddit.discord;

import com.uddernetworks.disddit.Disddit;
import com.uddernetworks.disddit.config.ConfigManager;
import com.uddernetworks.disddit.discord.command.CommandManager;
import com.uddernetworks.disddit.discord.command.HelpCommand;
import com.uddernetworks.disddit.discord.command.LinkCommand;
import com.uddernetworks.disddit.discord.command.ListCommand;
import com.uddernetworks.disddit.discord.command.NextCommand;
import com.uddernetworks.disddit.discord.command.ResetCommand;
import com.uddernetworks.disddit.discord.command.SetupCommand;
import com.uddernetworks.disddit.discord.command.SubredditCommand;
import com.uddernetworks.disddit.discord.command.evaluate.EvaluateCommand;
import com.uddernetworks.disddit.discord.reaction.ReactManager;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.security.auth.login.LoginException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.uddernetworks.disddit.config.Config.TOKEN;

public class DiscordManager extends ListenerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiscordManager.class);

    private final Disddit disddit;
    private final ConfigManager configManager;
    private final CompletableFuture<Void> initFuture = new CompletableFuture<>();
    private CommandManager commandManager;
    private ReactManager reactManager;
    private JDA jda;

    private Emote upvote;
    private Emote comment;
    private Emote downvote;

    public DiscordManager(Disddit disddit, ConfigManager configManager) {
        this.disddit = disddit;
        this.configManager = configManager;
    }

    public CompletableFuture<Void> init() throws LoginException {
        this.jda = new JDABuilder()
                .setToken(configManager.get(TOKEN))
                .setStatus(OnlineStatus.ONLINE)
                .addEventListeners(this)
                .addEventListeners(new EmbedUtils())
                .addEventListeners(this.reactManager = new ReactManager(this))
                .build();
        return initFuture;
    }

    @Override
    public void onReady(@Nonnull ReadyEvent event) {
        (this.commandManager = new CommandManager(disddit))
                .registerCommand(new HelpCommand(disddit))
                .registerCommand(new LinkCommand(disddit))
                .registerCommand(new ListCommand(disddit))
                .registerCommand(new SetupCommand(disddit))
                .registerCommand(new SubredditCommand(disddit))
                .registerCommand(new NextCommand(disddit))
                .registerCommand(new ResetCommand(disddit))
                .registerCommand(new EvaluateCommand(disddit));

        emoteFromName("upvote").ifPresent(upvote -> this.upvote = upvote);
        emoteFromName("comment").ifPresent(comment -> this.comment = comment);
        emoteFromName("downvote").ifPresent(downvote -> this.downvote = downvote);

        initFuture.complete(null);
    }

    private Optional<Emote> emoteFromName(String name) {
        var emotes = jda.getEmotesByName(name, true);
        if (emotes.isEmpty()) return Optional.empty();
        return Optional.of(emotes.get(0));
    }

    public Optional<User> getUser(long id) {
        return Optional.ofNullable(jda.getUserById(id));
    }

    public Disddit getDisddit() {
        return disddit;
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

    public Emote getUpvote() {
        return upvote;
    }

    public Emote getComment() {
        return comment;
    }

    public Emote getDownvote() {
        return downvote;
    }
}
