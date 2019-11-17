package com.uddernetworks.reddicord.discord.reddicord.comments;

import com.uddernetworks.reddicord.Reddicord;
import com.uddernetworks.reddicord.discord.DiscordManager;
import com.uddernetworks.reddicord.user.LinkedUser;
import net.dean.jraw.RedditClient;
import net.dean.jraw.models.Comment;
import net.dean.jraw.models.PublicContribution;
import net.dean.jraw.references.SubmissionReference;
import net.dean.jraw.tree.CommentNode;
import net.dean.jraw.tree.RootCommentNode;
import net.dv8tion.jda.api.entities.TextChannel;
import org.ocpsoft.prettytime.PrettyTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

import static com.uddernetworks.reddicord.discord.HelpUtility.ZWS;

public class CommentChannel {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommentChannel.class);
    private static final int COMMENT_COUNT = 5;
    private static final int INDENTATION_SEVERITY = 3;

    private static final PrettyTime prettyTime = new PrettyTime();

    private final Reddicord reddicord;
    private final DiscordManager discordManager;
    private final TextChannel channel;
    private final RedditClient client;
    private final SubmissionReference submissionReference;
    private final RootCommentNode comments;
    private final LinkedUser user;

    public CommentChannel(Reddicord reddicord, TextChannel channel, RedditClient client, SubmissionReference submissionReference, LinkedUser user) {
        this.reddicord = reddicord;
        this.discordManager = reddicord.getDiscordManager();
        this.channel = channel;
        this.client = client;
        this.submissionReference = submissionReference;
        this.comments = submissionReference.comments();
        this.user = user;
    }

    public CompletableFuture<Void> sendMessages() {
        return CompletableFuture.runAsync(() -> {
            comments.visualize();

            var iterator = comments.getReplies().iterator();
            for (int i = 0; i < COMMENT_COUNT && iterator.hasNext(); i++) {
                var comment = iterator.next();
                LOGGER.info("First: {} Class: {} DEPTH: {}", comment.getSubject(), comment.getSubject().getClass().getCanonicalName(), comment.getDepth());
                if (comment.getDepth() == 0) continue;
                processComment(comment);
            }
        }).exceptionally(t -> {
            LOGGER.error("An error occurred while sending comment messages", t);
            return null;
        });
    }

    private void processComment(PublicContribution<?> subject, CommentNode<PublicContribution<?>> comment) {
        sendSubject(subject, 0);
        comment.getReplies().forEach(this::processComment);
    }

    private void processComment(CommentNode<Comment> comment) {
        sendSubject(comment.getSubject(), comment.getDepth() - 1);
        comment.getReplies().forEach(this::processComment);
    }

    private void sendSubject(PublicContribution<?> subject, int depth) {
        sendComment(channel, subject.getAuthor(), subject.getScore(), prettyTime.format(subject.getCreated()), depth, subject.getBody());
    }

    private void sendComment(TextChannel channel, String username, int points, String time, int indentation, String text) {
        channel.sendMessage(topIndentation(indentation) + "**" + username + "**    " + points + " points \u00B7 " + time + "\n```" +
                indentation(indentation) + text +
                "```").queue(message -> {
            message.addReaction(discordManager.getUpvote()).queue();
            message.addReaction(discordManager.getComment()).queue();
            message.addReaction(discordManager.getDownvote()).queue();
        });
    }

    private <T> T submissionToComment(CommentNode<PublicContribution<?>> commentNode) {
        try {
            return (T) commentNode;
        } catch(ClassCastException e) {
            return null;
        }
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
