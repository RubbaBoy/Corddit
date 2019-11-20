package com.uddernetworks.corddit.discord.input;

import com.uddernetworks.corddit.Corddit;
import com.uddernetworks.corddit.discord.DiscordManager;
import com.uddernetworks.corddit.discord.EmbedUtils;
import com.uddernetworks.corddit.discord.reaction.ReactManager;
import com.uddernetworks.corddit.user.LinkedUser;
import com.uddernetworks.corddit.user.UserManager;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.uddernetworks.corddit.discord.corddit.InputStatus.CONFIRMATION;
import static com.uddernetworks.corddit.discord.corddit.InputStatus.WAITING;
import static com.uddernetworks.corddit.discord.reaction.ReactManager.Codepoint.GREEN_CHECK;
import static com.uddernetworks.corddit.discord.reaction.ReactManager.Codepoint.RED_X;

public class UserInputListener extends ListenerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserInputListener.class);

    private final Corddit corddit;
    private final DiscordManager discordManager;
    private final UserManager userManager;
    private final ReactManager reactManager;
    private final List<UserInput> userInputs = Collections.synchronizedList(new ArrayList<>());
    private final List<ChannelInput> channelInputs = Collections.synchronizedList(new ArrayList<>());

    public UserInputListener(Corddit corddit) {
        this.corddit = corddit;
        this.discordManager = corddit.getDiscordManager();
        this.userManager = corddit.getUserManager();
        this.reactManager = discordManager.getReactManager();
    }

    @Override
    public void onGuildMessageReceived(@Nonnull GuildMessageReceivedEvent event) {
        var author = event.getAuthor();
        var text = event.getMessage().getContentRaw();
        var channel = event.getChannel();

        if (author.isBot()) return;

        try {
            getInputFor(author, channel).ifPresent(input -> {
                try {
                    userManager.getUser(author).ifPresent(user -> {
                        var inputList = input instanceof UserInput ? userInputs : channelInputs;
                        var confirmation = input.getConfirmationMessage();

                        if (input.getInputStatus() == WAITING && confirmation != null) {
                            var confirmMessage = EmbedUtils.sendEmbed(channel, null, "Input verification", confirmation.apply(text));
                            input.setInputStatus(CONFIRMATION);

                            reactManager.createReactionListener(confirmMessage, GREEN_CHECK, reacted -> {
                                if (!reacted.getUser().equals(author)) return;
                                confirmMessage.delete().queue();
                                event.getMessage().delete().queue();
                                input.onReceive(event, user);
                            });

                            reactManager.createReactionListener(confirmMessage, RED_X, reacted -> {
                                if (!reacted.getUser().equals(author)) return;
                                confirmMessage.delete().queue();
                                event.getMessage().delete().queue();
                                input.setInputStatus(WAITING);
                                if (input instanceof UserInput) inputList.remove(input);
                            });
                            return;
                        }

                        input.onReceive(event, user);
                        if (input instanceof UserInput) inputList.remove(input);
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Optional<Input<?>> getInputFor(User user, TextChannel channel) {
        var channelId = channel.getIdLong();
        return Optional.ofNullable(userInputs.parallelStream().filter(userInput -> userInput.getChannel().getIdLong() == channelId && userInput.getMember().getUser().equals(user))
                .findFirst().map(Input.class::cast)
                .orElseGet(() -> channelInputs.parallelStream().filter(userInput -> userInput.getChannel().getIdLong() == channelId)
                        .findFirst().orElse(null)));
    }

    private Optional<UserInput> getUserInputFor(Member member, TextChannel channel) {
        return getUserInputFor(member.getUser(), channel);
    }

    private Optional<UserInput> getUserInputFor(User user, TextChannel channel) {
        return userInputs.stream().filter(userInput -> userInput.getChannel().equals(channel) && userInput.getMember().getUser().equals(user)).findFirst();
    }

    private Optional<ChannelInput> getChannelInputFor(TextChannel channel) {
        return channelInputs.parallelStream().filter(userInput -> userInput.getChannel().equals(channel)).findFirst();
    }

    public UserInput addListener(Member member, TextChannel channel, Consumer<String> onReceive, @Nullable Function<String, String> confirmationMessage) {
        var userInput = new UserInput(member, channel, onReceive, confirmationMessage);
        addListener(userInput);
        return userInput;
    }

    public void addListener(UserInput userInput) {
        getUserInputFor(userInput.getMember(), userInput.getChannel()).ifPresent(userInputs::remove);
        userInputs.add(userInput);
    }

    public ChannelInput addChannelListener(TextChannel channel, BiConsumer<LinkedUser, String> onReceive, @Nullable Function<String, String> confirmationMessage) {
        var channelInput = new ChannelInput(channel, onReceive, confirmationMessage);
        addChannelListener(channelInput);
        return channelInput;
    }

    public void addChannelListener(ChannelInput channelInput) {
        getChannelInputFor(channelInput.getChannel()).ifPresent(channelInputs::remove);
        channelInputs.add(channelInput);
    }

    public void removeListener(Member member) {
        userInputs.removeIf(userInput -> userInput.getMember().equals(member));
    }

    public void removeAllListeners(TextChannel channel) {
        userInputs.removeIf(userInput -> userInput.getChannel().equals(channel));
    }
}
