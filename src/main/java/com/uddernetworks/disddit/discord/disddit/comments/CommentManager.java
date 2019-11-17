package com.uddernetworks.disddit.discord.disddit.comments;

import com.uddernetworks.disddit.Cooldown;
import com.uddernetworks.disddit.Disddit;
import com.uddernetworks.disddit.discord.DiscordManager;
import com.uddernetworks.disddit.discord.DiscordStateManager;
import com.uddernetworks.disddit.discord.disddit.SubredditDataManager;
import com.uddernetworks.disddit.discord.disddit.SubredditManager;
import com.uddernetworks.disddit.discord.reaction.ReactManager;
import com.uddernetworks.disddit.user.LinkedUser;
import com.uddernetworks.disddit.user.UserManager;
import net.dean.jraw.models.Submission;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class CommentManager {

    private final Disddit disddit;
    private final DiscordManager discordManager;
    private final DiscordStateManager discordStateManager;
    private final ReactManager reactManager;
    private final SubredditManager subredditManager;
    private final SubredditDataManager subredditDataManager;
    private final UserManager userManager;
    private final Cooldown<LinkedUser> userCooldown = new Cooldown<>(5, TimeUnit.SECONDS);
    private final Map<LinkedUser, CommentChannel> commentChannels = new ConcurrentHashMap<>();

    public CommentManager(Disddit disddit) {
        this.disddit = disddit;
        this.discordManager = disddit.getDiscordManager();
        this.discordStateManager = disddit.getDiscordStateManager();
        this.reactManager = discordManager.getReactManager();
        this.subredditManager = disddit.getSubredditManager();
        this.subredditDataManager = subredditManager.getSubredditDataManager();
        this.userManager = disddit.getUserManager();
    }

    public CompletableFuture<Optional<CommentChannel>> openCommentThread(Submission submission, TextChannel channel, LinkedUser user) {
        var client = user.getRedditAccount();
        var submissionReference = submission.toReference(client);
        return CompletableFuture.supplyAsync(() -> Optional.ofNullable(userCooldown.ifDone(user, () -> {
            closeCommentThread(user);
            var category = discordStateManager.getOrCreateCategory(channel.getGuild()).join();
            var threadChannel = category.createTextChannel("thread-" + submissionReference.getFullName() + ThreadLocalRandom.current().nextInt(0, 9999999)).complete();
            var commentChannel = new CommentChannel(disddit, threadChannel, submissionReference, user);
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
