package com.uddernetworks.corddit.discord.corddit.comments;

import com.uddernetworks.corddit.Corddit;
import com.uddernetworks.corddit.discord.DiscordManager;
import com.uddernetworks.corddit.discord.reaction.ReactManager;
import com.uddernetworks.corddit.discord.reaction.VoteListener;
import com.uddernetworks.corddit.user.LinkedUser;
import net.dean.jraw.models.Comment;
import net.dean.jraw.references.SubmissionReference;
import net.dean.jraw.tree.CommentNode;
import net.dean.jraw.tree.RootCommentNode;
import net.dv8tion.jda.api.entities.TextChannel;
import org.ocpsoft.prettytime.PrettyTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

import static com.uddernetworks.corddit.discord.HelpUtility.ZWS;

public class CommentChannel {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommentChannel.class);
    private static final int COMMENT_COUNT = 5;
    private static final int INDENTATION_SEVERITY = 3;

    private static final PrettyTime prettyTime = new PrettyTime();

    private final Corddit corddit;
    private final DiscordManager discordManager;
    private final ReactManager reactManager;
    private final TextChannel channel;
    private final RootCommentNode comments;
    private final LinkedUser user;

    public CommentChannel(Corddit corddit, TextChannel channel, SubmissionReference submissionReference, LinkedUser user) {
        this.corddit = corddit;
        this.discordManager = corddit.getDiscordManager();
        this.reactManager = discordManager.getReactManager();
        this.channel = channel;
        this.comments = submissionReference.comments();
        this.user = user;
    }

    public CompletableFuture<Void> sendMessages() {
        return CompletableFuture.runAsync(() -> {
            comments.visualize();

            var iterator = comments.getReplies().iterator();
            for (int i = 0; i < COMMENT_COUNT && iterator.hasNext(); i++) {
                var comment = iterator.next();
                if (comment.getDepth() == 0) continue;
                processComment(comment);
            }
        }).exceptionally(t -> {
            LOGGER.error("An error occurred while sending comment messages", t);
            return null;
        });
    }

    private void processComment(CommentNode<Comment> comment) {
        sendSubject(comment.getSubject(), comment.getDepth() - 1);
        comment.getReplies().forEach(this::processComment);
    }

    private void sendSubject(Comment subject, int depth) {
        sendComment(channel, subject, subject.getAuthor(), subject.getScore(), prettyTime.format(subject.getCreated()), depth, subject.getBody());
    }

    private void sendComment(TextChannel channel, Comment comment, String username, int points, String time, int indentation, String text) {
        channel.sendMessage(topIndentation(indentation) + "**" + username + "**    " + points + " points \u00B7 " + time + "\n```" +
                indentation(indentation) + text +
                "```").queue(message -> {
            new VoteListener<>(corddit, message, comment, user -> {
                channel.sendMessage("Commenting will come soon!").queue();
            }).startListener();
        });
    }

    private String topIndentation(int amount) {
        return (ZWS + " ").repeat(4 + (amount * (INDENTATION_SEVERITY * 2 + 1)));
    }

    private String indentation(int amount) {
        return ("|" + (ZWS + " ").repeat(INDENTATION_SEVERITY)).repeat(amount);
    }

    public TextChannel getChannel() {
        return channel;
    }

    public LinkedUser getUser() {
        return user;
    }
}
