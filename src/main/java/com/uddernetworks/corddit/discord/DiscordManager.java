package com.uddernetworks.corddit.discord;

import com.uddernetworks.corddit.Corddit;
import com.uddernetworks.corddit.config.ConfigManager;
import com.uddernetworks.corddit.discord.command.CommandManager;
import com.uddernetworks.corddit.discord.command.HelpCommand;
import com.uddernetworks.corddit.discord.command.LinkCommand;
import com.uddernetworks.corddit.discord.command.ListCommand;
import com.uddernetworks.corddit.discord.command.NextCommand;
import com.uddernetworks.corddit.discord.command.ResetCommand;
import com.uddernetworks.corddit.discord.command.SetupCommand;
import com.uddernetworks.corddit.discord.command.SubredditCommand;
import com.uddernetworks.corddit.discord.command.evaluate.EvaluateCommand;
import com.uddernetworks.corddit.discord.input.UserInputListener;
import com.uddernetworks.corddit.discord.reaction.ReactManager;
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

import static com.uddernetworks.corddit.config.Config.TOKEN;

public class DiscordManager extends ListenerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiscordManager.class);

    private final Corddit corddit;
    private final ConfigManager configManager;
    private final CompletableFuture<Void> initFuture = new CompletableFuture<>();
    private CommandManager commandManager;
    private ReactManager reactManager;
    private UserInputListener userInputListener;
    private JDA jda;

    private Emote upvote;
    private Emote comment;
    private Emote downvote;

    public DiscordManager(Corddit corddit, ConfigManager configManager) {
        this.corddit = corddit;
        this.configManager = configManager;
    }

    public CompletableFuture<Void> init() throws LoginException {
        this.jda = new JDABuilder()
                .setToken(configManager.get(TOKEN))
                .setStatus(OnlineStatus.ONLINE)
                .addEventListeners(this)
                .addEventListeners(new EmbedUtils())
                .addEventListeners(this.reactManager = new ReactManager(this))
                .addEventListeners(this.userInputListener = new UserInputListener(corddit))
                .build();
        return initFuture;
    }

    @Override
    public void onReady(@Nonnull ReadyEvent event) {
        (this.commandManager = new CommandManager(corddit))
                .registerCommand(new HelpCommand(corddit))
                .registerCommand(new LinkCommand(corddit))
                .registerCommand(new ListCommand(corddit))
                .registerCommand(new SetupCommand(corddit))
                .registerCommand(new SubredditCommand(corddit))
                .registerCommand(new NextCommand(corddit))
                .registerCommand(new ResetCommand(corddit))
                .registerCommand(new EvaluateCommand(corddit));

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

    public Corddit getCorddit() {
        return corddit;
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

    public UserInputListener getUserInputListener() {
        return userInputListener;
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
