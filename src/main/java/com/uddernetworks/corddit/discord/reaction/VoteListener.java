package com.uddernetworks.corddit.discord.reaction;

import com.uddernetworks.corddit.Corddit;
import com.uddernetworks.corddit.discord.DiscordManager;
import com.uddernetworks.corddit.user.LinkedUser;
import com.uddernetworks.corddit.user.UserManager;
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

    private final Corddit corddit;
    private final DiscordManager discordManager;
    private final UserManager userManager;
    private final Consumer<LinkedUser> onUncomment;
    private final ReactManager reactManager;
    private final Consumer<LinkedUser> onComment;

    private final Map<Member, VoteDirection> voting = new ConcurrentHashMap<>();
    private final Message message;
    private final TextChannel channel;
    private final PublicContribution<T> contribution;

    private ReactionListener upvoteListener;
    private ReactionListener commentListener;
    private ReactionListener downvoteListener;

    public VoteListener(Corddit corddit, Message message, PublicContribution<T> contribution) {
        this(corddit, message, contribution, $ -> {});
    }

    public VoteListener(Corddit corddit, Message message, PublicContribution<T> contribution, Consumer<LinkedUser> onComment) {
        this(corddit, message, contribution, onComment, $ -> {});
    }

    public VoteListener(Corddit corddit, Message message, PublicContribution<T> contribution, Consumer<LinkedUser> onComment, Consumer<LinkedUser> onUncomment) {
        this.corddit = corddit;
        this.discordManager = corddit.getDiscordManager();
        this.userManager = corddit.getUserManager();
        this.reactManager = discordManager.getReactManager();
        this.onComment = onComment;
        this.onUncomment = onUncomment;
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
                member -> userManager.getUser(member.getUser()).ifPresent(onComment),
                member -> userManager.getUser(member.getUser()).ifPresent(onUncomment), true, true);

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
