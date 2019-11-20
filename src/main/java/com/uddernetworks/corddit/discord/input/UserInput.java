package com.uddernetworks.corddit.discord.input;

import com.uddernetworks.corddit.user.LinkedUser;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Function;

public class UserInput extends Input<Consumer<String>> {

    private final Member member;

    public UserInput(Member member, TextChannel channel, Consumer<String> onReceive, @Nullable Function<String, String> confirmationMessage) {
        super(channel, onReceive, confirmationMessage);
        this.member = member;
    }

    @Override
    public void onReceive(GuildMessageReceivedEvent event, LinkedUser user) {
        onReceive.accept(event.getMessage().getContentRaw());
    }

    public Member getMember() {
        return member;
    }
}
