package com.uddernetworks.corddit.discord.input;

import com.uddernetworks.corddit.discord.corddit.InputStatus;
import com.uddernetworks.corddit.user.LinkedUser;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public abstract class Input<T> {

    final T onReceive;
    private final TextChannel channel;
    private final Function<String, String> confirmationMessage;
    private InputStatus inputStatus = InputStatus.WAITING;

    public Input(TextChannel channel, T onReceive, @Nullable Function<String, String> confirmationMessage) {
        this.channel = channel;
        this.onReceive = onReceive;
        this.confirmationMessage = confirmationMessage;
    }

    public abstract void onReceive(GuildMessageReceivedEvent event, LinkedUser user);

    public TextChannel getChannel() {
        return channel;
    }

    public T getOnReceive() {
        return onReceive;
    }

    public Function<String, String> getConfirmationMessage() {
        return confirmationMessage;
    }

    public InputStatus getInputStatus() {
        return inputStatus;
    }

    public void setInputStatus(InputStatus inputStatus) {
        this.inputStatus = inputStatus;
    }

}
