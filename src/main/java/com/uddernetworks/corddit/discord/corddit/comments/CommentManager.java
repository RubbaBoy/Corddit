package com.uddernetworks.corddit.discord.corddit.comments;

import com.uddernetworks.corddit.Cooldown;
import com.uddernetworks.corddit.Corddit;
import com.uddernetworks.corddit.discord.DiscordStateManager;
import com.uddernetworks.corddit.user.LinkedUser;
import net.dean.jraw.models.Submission;
import net.dv8tion.jda.api.entities.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class CommentManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommentManager.class);

    private final Corddit corddit;
    private final DiscordStateManager discordStateManager;
    private final Cooldown<LinkedUser> userCooldown = new Cooldown<>(5, TimeUnit.SECONDS);
    private final Map<LinkedUser, CommentChannel> commentChannels = new ConcurrentHashMap<>();

    public CommentManager(Corddit corddit) {
        this.corddit = corddit;
        this.discordStateManager = corddit.getDiscordStateManager();
    }

    public CompletableFuture<Optional<CommentChannel>> openCommentThread(Submission submission, TextChannel channel, LinkedUser user) {
        var userInputListener = corddit.getDiscordManager().getUserInputListener();
        var client = user.getRedditAccount();
        var submissionReference = submission.toReference(client);
        return CompletableFuture.supplyAsync(() -> Optional.ofNullable(userCooldown.ifDone(user, () -> {
            try {
                closeCommentThread(user);
                var category = discordStateManager.getOrCreateCategory(channel.getGuild()).join();
                var threadChannel = category.createTextChannel("thread-" + submissionReference.getFullName() + ThreadLocalRandom.current().nextInt(0, 9999999)).complete();
                var commentChannel = new CommentChannel(corddit, threadChannel, submissionReference, user);
                commentChannel.sendMessages().join();

                userInputListener.addChannelListener(threadChannel, (linkedUser, input) -> {
                    submission.toReference(linkedUser.getRedditAccount()).reply(input);
                }, confirmInput -> "Please react with the :white_check_mark: or :x: to confirm or deny the post response with the following text:\n\n**" + confirmInput + "**");
                return commentChannel;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }

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
