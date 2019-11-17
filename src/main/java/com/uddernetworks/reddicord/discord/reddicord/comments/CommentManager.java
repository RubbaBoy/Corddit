package com.uddernetworks.reddicord.discord.reddicord.comments;

import com.uddernetworks.reddicord.Cooldown;
import com.uddernetworks.reddicord.Reddicord;
import com.uddernetworks.reddicord.discord.DiscordManager;
import com.uddernetworks.reddicord.discord.DiscordStateManager;
import com.uddernetworks.reddicord.discord.reaction.ReactManager;
import com.uddernetworks.reddicord.discord.reddicord.SubredditDataManager;
import com.uddernetworks.reddicord.discord.reddicord.SubredditManager;
import com.uddernetworks.reddicord.user.LinkedUser;
import com.uddernetworks.reddicord.user.UserManager;
import net.dean.jraw.models.Submission;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class CommentManager {

    private final Reddicord reddicord;
    private final DiscordManager discordManager;
    private final DiscordStateManager discordStateManager;
    private final ReactManager reactManager;
    private final SubredditManager subredditManager;
    private final SubredditDataManager subredditDataManager;
    private final UserManager userManager;
    private final Cooldown<LinkedUser> userCooldown = new Cooldown<>(5, TimeUnit.SECONDS);
    private final Map<LinkedUser, CommentChannel> commentChannels = new ConcurrentHashMap<>();

    public CommentManager(Reddicord reddicord) {
        this.reddicord = reddicord;
        this.discordManager = reddicord.getDiscordManager();
        this.discordStateManager = reddicord.getDiscordStateManager();
        this.reactManager = discordManager.getReactManager();
        this.subredditManager = reddicord.getSubredditManager();
        this.subredditDataManager = subredditManager.getSubredditDataManager();
        this.userManager = reddicord.getUserManager();
    }

    public CompletableFuture<Optional<CommentChannel>> openCommentThread(Submission submission, TextChannel channel, LinkedUser user) {
        var client = user.getRedditAccount();
        var submissionReference = submission.toReference(client);
        return CompletableFuture.supplyAsync(() -> Optional.ofNullable(userCooldown.ifDone(user, () -> {
            closeCommentThread(user);
            var category = discordStateManager.getOrCreateCategory(channel.getGuild()).join();
            var threadChannel = category.createTextChannel("thread-" + submissionReference.getFullName() + ThreadLocalRandom.current().nextInt(0, 9999999)).complete();
            var commentChannel = new CommentChannel(reddicord, threadChannel, client, submissionReference, user);
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
