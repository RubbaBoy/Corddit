package com.uddernetworks.corddit.discord.corddit.comments;

import com.uddernetworks.corddit.Cooldown;
import com.uddernetworks.corddit.Corddit;
import com.uddernetworks.corddit.discord.DiscordManager;
import com.uddernetworks.corddit.discord.DiscordStateManager;
import com.uddernetworks.corddit.discord.corddit.SubredditDataManager;
import com.uddernetworks.corddit.discord.corddit.SubredditManager;
import com.uddernetworks.corddit.discord.reaction.ReactManager;
import com.uddernetworks.corddit.user.LinkedUser;
import com.uddernetworks.corddit.user.UserManager;
import net.dean.jraw.models.Submission;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class CommentManager {

    private final Corddit corddit;
    private final DiscordManager discordManager;
    private final DiscordStateManager discordStateManager;
    private final ReactManager reactManager;
    private final SubredditManager subredditManager;
    private final SubredditDataManager subredditDataManager;
    private final UserManager userManager;
    private final Cooldown<LinkedUser> userCooldown = new Cooldown<>(5, TimeUnit.SECONDS);
    private final Map<LinkedUser, CommentChannel> commentChannels = new ConcurrentHashMap<>();

    public CommentManager(Corddit corddit) {
        this.corddit = corddit;
        this.discordManager = corddit.getDiscordManager();
        this.discordStateManager = corddit.getDiscordStateManager();
        this.reactManager = discordManager.getReactManager();
        this.subredditManager = corddit.getSubredditManager();
        this.subredditDataManager = subredditManager.getSubredditDataManager();
        this.userManager = corddit.getUserManager();
    }

    public CompletableFuture<Optional<CommentChannel>> openCommentThread(Submission submission, TextChannel channel, LinkedUser user) {
        var client = user.getRedditAccount();
        var submissionReference = submission.toReference(client);
        return CompletableFuture.supplyAsync(() -> Optional.ofNullable(userCooldown.ifDone(user, () -> {
            closeCommentThread(user);
            var category = discordStateManager.getOrCreateCategory(channel.getGuild()).join();
            var threadChannel = category.createTextChannel("thread-" + submissionReference.getFullName() + ThreadLocalRandom.current().nextInt(0, 9999999)).complete();
            var commentChannel = new CommentChannel(corddit, threadChannel, submissionReference, user);
            commentChannel.sendMessages().join();
            return commentChannel;
        }, (CommentChannel) null)));
    }

    public void closeCommentThread(LinkedUser user) {
        if (!commentChannels.containsKey(user)) return;
        commentChannels.get(user).getChannel().delete().queue();
        commentChannels.remove(user);
    }

    public CompletableFuture<Void> closeCommentThread(CommentChannel commentChannel) {
        return CompletableFuture.runAsync(() -> {
            commentChannel.getChannel().delete().queue();
            commentChannels.remove(commentChannel.getUser());
        });
    }
}
