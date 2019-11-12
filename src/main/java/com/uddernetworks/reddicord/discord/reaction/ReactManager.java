package com.uddernetworks.reddicord.discord.reaction;

import com.uddernetworks.reddicord.discord.DiscordManager;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.guild.GuildMessageDeleteEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class ReactManager extends ListenerAdapter {

    private final List<ReactionListener> listeners = Collections.synchronizedList(new ArrayList<>());
    private final DiscordManager discordManager;
    private final JDA jda;

    public ReactManager(DiscordManager discordManager) {
        this.discordManager = discordManager;
        this.jda = discordManager.getJDA();
    }

    public ReactionListener addReactionListener(ReactionListener listener) {
        listeners.add(listener);
        return listener;
    }

    public ReactionListener createReactionListener(Message message, String codepoints, Consumer<Member> callback) {
        return createReactionListener(message, codepoints, callback);
    }

    public ReactionListener createReactionListener(Message message, String codepoints, Consumer<Member> callback, boolean selfReact) {
        if (selfReact) message.addReaction(codepoints).queue();
        var listener = new ReactionListener(message, codepoints, callback);
        listeners.add(listener);
        return listener;
    }

    public void removeReactionListener(ReactionListener listener) {
        listeners.remove(listener);
    }

    public void removeReactionListener(Message message) {
        findReactionListener(message).ifPresent(listeners::remove);
    }

    public Optional<ReactionListener> findReactionListener(Message message) {
        return findReactionListener(message.getIdLong());
    }

    public Optional<ReactionListener> findReactionListener(long messageId) {
        return listeners.stream()
                .filter(reactionListener -> reactionListener.getMessageId() == messageId)
                .findFirst();
    }

    @Override
    public void onGuildMessageReactionAdd(@Nonnull GuildMessageReactionAddEvent event) {
        var user = event.getUser();
        if (user.isBot()) return;
        var codepoints = event.getReactionEmote().getAsCodepoints();
        findReactionListener(event.getMessageIdLong())
                .filter(reactionListener -> codepoints.equalsIgnoreCase(reactionListener.getCodepoints()))
                .ifPresent(reactionListener -> reactionListener.onReact(event.getGuild().getMember(user)));
    }

    @Override
    public void onGuildMessageDelete(@Nonnull GuildMessageDeleteEvent event) {
        findReactionListener(event.getMessageIdLong())
                .ifPresent(listeners::remove);
    }
}
