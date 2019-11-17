package com.uddernetworks.disddit.discord.reaction;

import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;

import java.util.function.Consumer;

public class ReactionListener {
    private final Message message;
    private String codepoints;
    private Emote emote;
    private final Consumer<Member> onReact;
    private final Consumer<Member> onUnreact;

    public ReactionListener(Message message, Emote emote, Consumer<Member> onReact) {
        this(message, emote, onReact, null);
    }

    public ReactionListener(Message message, Emote emote, Consumer<Member> onReact, Consumer<Member> onUnreact) {
        this.message = message;
        this.emote = emote;
        this.onReact = onReact;
        this.onUnreact = onUnreact;
    }

    public ReactionListener(Message message, String codepoints, Consumer<Member> onReact) {
        this(message, codepoints, onReact, null);
    }

    public ReactionListener(Message message, String codepoints, Consumer<Member> onReact, Consumer<Member> onUnreact) {
        this.message = message;
        this.codepoints = codepoints;
        this.onReact = onReact;
        this.onUnreact = onUnreact;
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

    public Emote getEmote() {
        return emote;
    }

    public void onReact(Member member) {
        if (onReact != null) onReact.accept(member);
    }

    public void onUnreact(Member member) {
        if (onUnreact != null) onUnreact.accept(member);
    }
}