package com.uddernetworks.corddit.discord.input;

import com.uddernetworks.corddit.discord.corddit.InputStatus;
import com.uddernetworks.corddit.user.LinkedUser;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class ChannelInput extends Input<BiConsumer<LinkedUser, String>> {

    public ChannelInput(TextChannel channel, BiConsumer<LinkedUser, String> onReceive, @Nullable Function<String, String> confirmationMessage) {
        super(channel, onReceive, confirmationMessage);
    }

    @Override
    public void onReceive(GuildMessageReceivedEvent event, LinkedUser user) {
        onReceive.accept(user, event.getMessage().getContentRaw());
    }

    @Override
    public void setInputStatus(InputStatus inputStatus) {
    }
}
