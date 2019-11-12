package com.uddernetworks.reddicord.discord.reaction;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;

import java.util.function.Consumer;

public class ReactionListener {
    private final Message message;
    private final String codepoints;
    private final Consumer<Member> callback;

    public ReactionListener(Message message, String codepoints, Consumer<Member> callback) {
        this.message = message;
        this.codepoints = codepoints;
        this.callback = callback;
    }

    public Message getMessage() {
        return message;
    }

    public long getMessageId() {
        return message.getIdLong();
    }

    public String getCodepoints() {
        return codepoints;
    }

    public void onReact(Member member) {
        callback.accept(member);
    }
}