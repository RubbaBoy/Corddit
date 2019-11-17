package com.uddernetworks.disddit.discord.reaction;

import com.uddernetworks.disddit.Disddit;
import com.uddernetworks.disddit.discord.DiscordManager;
import com.uddernetworks.disddit.user.LinkedUser;
import com.uddernetworks.disddit.user.UserManager;
import net.dean.jraw.RedditClient;
import net.dean.jraw.models.PublicContribution;
import net.dean.jraw.models.VoteDirection;
import net.dean.jraw.references.PublicContributionReference;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class VoteListener<T extends PublicContributionReference> {

    private final Disddit disddit;
    private final DiscordManager discordManager;
    private final UserManager userManager;
    private final ReactManager reactManager;
    private final Consumer<LinkedUser> onComment;

    private final Map<Member, VoteDirection> voting = new ConcurrentHashMap<>();
    private final Message message;
    private final TextChannel channel;
    private final PublicContribution<T> contribution;

    private ReactionListener upvoteListener;
    private ReactionListener commentListener;
    private ReactionListener downvoteListener;

    public VoteListener(Disddit disddit, Message message, PublicContribution<T> contribution, Consumer<LinkedUser> onComment) {
        this.disddit = disddit;
        this.discordManager = disddit.getDiscordManager();
        this.userManager = disddit.getUserManager();
        this.reactManager = discordManager.getReactManager();
        this.onComment = onComment;
        this.contribution = contribution;
        this.message = message;
        this.channel = message.getTextChannel();
    }

    public void startListener() {
        upvoteListener = reactManager.createReactionListener(message, discordManager.getUpvote(),
                member -> { // Upvote
                    var prev = voting.getOrDefault(member, VoteDirection.NONE);
                    if (prev == VoteDirection.DOWN) {
                        clearDownvote(message, member);
                    }
                    getSubredditRef(member, T::upvote);
                    voting.put(member, VoteDirection.UP);
                },
                member -> { // Remove upvote
                    var prev = voting.getOrDefault(member, VoteDirection.NONE);
                    if (prev == VoteDirection.UP) getSubredditRef(member, T::unvote);
                    voting.put(member, VoteDirection.NONE);
                });

        commentListener = reactManager.createReactionListener(message, discordManager.getComment(),
                member -> {
                    userManager.getUser(member.getUser()).ifPresent(onComment);
                }, null, true, true);

        downvoteListener = reactManager.createReactionListener(message, discordManager.getDownvote(),
                member -> { // Downvote
                    var prev = voting.getOrDefault(member, VoteDirection.NONE);
                    if (prev == VoteDirection.UP) {
                        clearDownvote(message, member);
                    }
                    getSubredditRef(member, T::downvote);
                    voting.put(member, VoteDirection.DOWN);
                },
                member -> { // Remove downvote
                    var prev = voting.getOrDefault(member, VoteDirection.NONE);
                    if (prev == VoteDirection.DOWN) getSubredditRef(member, T::unvote);
                    voting.put(member, VoteDirection.NONE);
                });
    }

    private void clearUpvote(Message message, Member member, Runnable callback) {
        clearEmote(discordManager.getUpvote(), message, member, callback);
    }

    private void clearDownvote(Message message, Member member, Runnable callback) {
        clearEmote(discordManager.getDownvote(), message, member, callback);
    }

    private void clearDownvote(Message message, Member member) {
        clearEmote(discordManager.getDownvote(), message, member, () -> {});
    }

    private void clearEmote(Emote emote, Message message, Member member, Runnable callback) {
        message.removeReaction(emote, member.getUser()).complete();
        callback.run();
    }

    private Optional<T> getSubredditRef(Member member) {
        return getAccount(member).map(contribution::toReference);
    }

    private void getSubredditRef(Member member, Consumer<T> consumer) {
        getAccount(member).map(contribution::toReference).ifPresent(consumer);
    }

    private Optional<RedditClient> getAccount(Member member) {
        return userManager.getUser(member.getUser()).map(LinkedUser::getRedditAccount);
    }
}
