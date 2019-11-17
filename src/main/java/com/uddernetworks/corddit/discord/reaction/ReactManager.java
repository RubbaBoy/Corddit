package com.uddernetworks.corddit.discord.reaction;

import com.uddernetworks.corddit.discord.DiscordManager;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.events.channel.text.TextChannelDeleteEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageDeleteEvent;
import net.dv8tion.jda.api.events.message.guild.react.GenericGuildMessageReactionEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionRemoveEvent;
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
        return createReactionListener(message, codepoints, callback, null, true, false);
    }

    public ReactionListener createReactionListener(Message message, String codepoints, Consumer<Member> onReact, Consumer<Member> onUnreact) {
        return createReactionListener(message, codepoints, onReact, onUnreact, true, false);
    }

    public ReactionListener createReactionListener(Message message, String codepoints, Consumer<Member> onReact, Consumer<Member> onUnreact, boolean selfReact, boolean keepSingle) {
        if (selfReact) message.addReaction(codepoints).queue();
        Consumer<Member> override = onReact;
        if (keepSingle) {
            override = member -> {
                message.removeReaction(codepoints, member.getUser()).queue();
                onReact.accept(member);
            };
        }
        var listener = new ReactionListener(message, codepoints, override, onUnreact);
        listeners.add(listener);
        return listener;
    }

    public ReactionListener createReactionListener(Message message, Emote emote, Consumer<Member> callback) {
        return createReactionListener(message, emote, callback, null, true, false);
    }

    public ReactionListener createReactionListener(Message message, Emote emote, Consumer<Member> onReact, Consumer<Member> onUnreact) {
        return createReactionListener(message, emote, onReact, onUnreact, true, false);
    }

    public ReactionListener createReactionListener(Message message, Emote emote, Consumer<Member> onReact, Consumer<Member> onUnreact, boolean selfReact, boolean keepSingle) {
        if (selfReact) message.addReaction(emote).queue();
        Consumer<Member> override = onReact;
        if (keepSingle) {
            override = member -> {
                message.removeReaction(emote, member.getUser()).queue();
                onReact.accept(member);
            };
        }
        var listener = new ReactionListener(message, emote, override, onUnreact);
        listeners.add(listener);
        return listener;
    }

    public void removeReactionListener(ReactionListener listener) {
        listeners.remove(listener);
    }

    public void removeReactionListener(Message message) {
        removeReactionListener(message.getIdLong());
    }

    public void removeReactionListener(long messageId) {
        listeners.stream()
                .filter(reactionListener -> reactionListener.getMessageId() == messageId)
                .forEach(listeners::remove);
    }

    public void removeReactionListener(Message message, MessageReaction.ReactionEmote reaction) {
        findReactionListener(message, reaction).ifPresent(listeners::remove);
    }

    public Optional<ReactionListener> findReactionListener(Message message, MessageReaction.ReactionEmote reaction) {
        return findReactionListener(message.getIdLong(), reaction);
    }

    public Optional<ReactionListener> findReactionListener(long messageId, MessageReaction.ReactionEmote reaction) {
        return listeners.stream()
                .filter(reactionListener -> reactionListener.getMessageId() == messageId)
                .filter(reactionListener -> {
                    if (reaction.isEmoji()) {
                        return reaction.getAsCodepoints().equalsIgnoreCase(reactionListener.getCodepoints());
                    } else {
                        return reaction.getIdLong() == reactionListener.getEmote().getIdLong();
                    }
                })
                .findFirst();
    }

    private void reactionEvent(GenericGuildMessageReactionEvent event, Consumer<ReactionListener> reactionEvent) {
        var user = event.getUser();
        if (user.isBot()) return;
        var reaction = event.getReactionEmote();
        var emote = reaction.getEmote().getIdLong();
        var listener = findReactionListener(event.getMessageIdLong(), reaction);
        listener.filter(reactionListener -> emote == reactionListener.getEmote().getIdLong())
                .ifPresentOrElse(reactionEvent, () -> {
                    if (!reaction.isEmoji()) return;
                    var codepoints = reaction.getAsCodepoints();
                    listener.filter(reactionListener -> codepoints.equalsIgnoreCase(reactionListener.getCodepoints()))
                            .ifPresent(reactionEvent);
                });
    }

    @Override
    public void onGuildMessageReactionRemove(@Nonnull GuildMessageReactionRemoveEvent event) {
        reactionEvent(event, reactionListener -> reactionListener.onUnreact(event.getGuild().getMember(event.getUser())));
    }

    @Override
    public void onGuildMessageReactionAdd(@Nonnull GuildMessageReactionAddEvent event) {
        reactionEvent(event, reactionListener -> reactionListener.onReact(event.getGuild().getMember(event.getUser())));
    }

    @Override
    public void onGuildMessageDelete(@Nonnull GuildMessageDeleteEvent event) {
        removeReactionListener(event.getMessageIdLong());
    }

    @Override
    public void onTextChannelDelete(@Nonnull TextChannelDeleteEvent event) {
        listeners.removeIf(reactionListener -> reactionListener.getMessage().getChannel().equals(event.getChannel()));
    }

    public static class Codepoint {
        public static String YELLOW_CIRCLE = "\uD83D\uDFE1";
        public static String GREEN_CHECK = "\u2705";
    }
}
