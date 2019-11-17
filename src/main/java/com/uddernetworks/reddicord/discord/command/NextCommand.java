package com.uddernetworks.reddicord.discord.command;

import com.uddernetworks.reddicord.Reddicord;
import com.uddernetworks.reddicord.discord.DiscordManager;
import com.uddernetworks.reddicord.discord.EmbedUtils;
import com.uddernetworks.reddicord.discord.reaction.ReactManager;
import com.uddernetworks.reddicord.discord.reddicord.SubredditDataManager;
import com.uddernetworks.reddicord.discord.reddicord.SubredditLink;
import com.uddernetworks.reddicord.discord.reddicord.SubredditManager;
import com.uddernetworks.reddicord.user.LinkedUser;
import com.uddernetworks.reddicord.user.UserManager;
import net.dean.jraw.RedditClient;
import net.dean.jraw.models.Submission;
import net.dean.jraw.models.SubredditSort;
import net.dean.jraw.models.VoteDirection;
import net.dean.jraw.pagination.DefaultPaginator;
import net.dean.jraw.references.SubmissionReference;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class NextCommand extends Command {

    private static final Logger LOGGER = LoggerFactory.getLogger(NextCommand.class);

    private static final Pattern PREVIEW_PATTERN = Pattern.compile(".*\\.(png|jpg|jpeg|bmp|gif|gifv)$");

    private final Reddicord reddicord;
    private final DiscordManager discordManager;
    private final ReactManager reactManager;
    private final SubredditManager subredditManager;
    private final SubredditDataManager subredditDataManager;
    private final UserManager userManager;
    private final Map<SubredditLink, Long> timeouts = new HashMap<>();
    private final Map<SubredditLink, DefaultPaginator<Submission>> paginators = new ConcurrentHashMap<>();

    public NextCommand(Reddicord reddicord) {
        super("next");
        this.reddicord = reddicord;
        this.discordManager = reddicord.getDiscordManager();
        this.reactManager = discordManager.getReactManager();
        this.subredditManager = reddicord.getSubredditManager();
        this.subredditDataManager = subredditManager.getSubredditDataManager();
        this.userManager = reddicord.getUserManager();
    }

    @Override
    public void onCommand(Member author, TextChannel channel, GuildMessageReceivedEvent event) {
        var receivedMessage = event.getMessage();

        receivedMessage.delete().queue();
        subredditManager.getLinkFromChannel(channel).ifPresentOrElse(link -> {
            if (timeouts.containsKey(link) && timeouts.get(link) > System.currentTimeMillis()) {
                EmbedUtils.error(channel, author, "You can only run this command once every 5 seconds per subreddit");
                return;
            }
            timeouts.put(link, System.currentTimeMillis() + 5000);

            var color = EmbedUtils.hex2Rgb(link.getSubreddit().getKeyColor());

            var upvote = discordManager.getUpvote();
            var comment = discordManager.getComment();
            var downvote = discordManager.getDownvote();
            clearOrContinue(channel, paginators.containsKey(link)).thenRunAsync(() -> {
                var hot = paginators.computeIfAbsent(link, $ -> link.getSubreddit().toReference(subredditDataManager.getClient()).posts().sorting(SubredditSort.HOT).limit(1).build());
                for (int i = 0; i < 5; i++) {
                    var listing = hot.next();
                    if (listing.isEmpty()) break;
                    var submission = listing.get(0);

                    var embed = new EmbedBuilder();

                    embed.setTitle(submission.getTitle(), "https://reddit.com" + submission.getPermalink());
                    embed.setColor(color);

                    var descriptionSet = false;
                    if (submission.hasThumbnail()) {
                        var url = submission.getUrl();
                        if (PREVIEW_PATTERN.matcher(url.toLowerCase()).matches()) {
                            embed.setImage(url);
                        } else {
                            var imageUrl = url;
                            var preview = submission.getPreview();
                            if (preview != null && preview.isEnabled()) {
                                imageUrl = preview.getImages().get(0).getSource().getUrl();
                                descriptionSet = true;
                                embed.setImage(imageUrl);
                            }

                            embed.setDescription("[" + url + "](" + url + ")");
                        }
                    }

                    var media = submission.getEmbeddedMedia();
                    if (!descriptionSet && media != null) {
                        var oembed = media.getOEmbed();
                        if (oembed != null) {
                            embed.setImage(oembed.getUrl());
                            descriptionSet = true;
                        }
                    }

                    if (!descriptionSet) {
                        var self = submission.getSelfText();
                        embed.setDescription(self);
                        descriptionSet = self != null;
                    }

                    if (!descriptionSet) {
                        embed.setDescription("[" + submission.getUrl() + "](" + submission.getUrl() + ")");
                        descriptionSet = true;
                    }

                    var voting = new ConcurrentHashMap<Member, VoteDirection>();

                    embed.setFooter(submission.getAuthor());
                    embed.setTimestamp(submission.getCreated().toInstant());

                    channel.sendMessage(embed.build()).queue(message -> {
                        reactManager.createReactionListener(message, upvote,
                                member -> { // Upvote
                                    var prev = voting.getOrDefault(member, VoteDirection.NONE);
                                    if (prev == VoteDirection.DOWN) {
                                        clearDownvote(message, author);
                                    }
                                    getSubredditRef(member, submission, SubmissionReference::upvote);
                                    voting.put(author, VoteDirection.UP);
                                },
                                member -> { // Remove upvote
                                    getSubredditRef(member, submission, SubmissionReference::unvote);
                                    voting.put(author, VoteDirection.NONE);
                                });

                        reactManager.createReactionListener(message, comment,
                                member -> {
                                    userManager.getUser(member.getUser()).ifPresent(user -> {
                                        reddicord.getCommentManager().openCommentThread(submission, channel, user).thenAccept(channelOptional -> {
                                            channelOptional.ifPresentOrElse(commentChannel -> {
                                                channel.sendMessage(author.getAsMention() + " Go to " + commentChannel.getChannel().getAsMention()).queue();
                                            }, () -> {
                                                EmbedUtils.error(channel, author, "Couldn't open the channel. Make sure to wait a few seconds between opening comment threads.");
                                            });
                                        });
                                    });
                                }, null, true, true);

                        reactManager.createReactionListener(message, downvote,
                                member -> { // Downvote
                                    var prev = voting.getOrDefault(member, VoteDirection.NONE);
                                    if (prev == VoteDirection.UP) {
                                        clearDownvote(message, author);
                                    }
                                    getSubredditRef(member, submission, SubmissionReference::downvote);
                                    voting.put(author, VoteDirection.DOWN);
                                },
                                member -> { // Remove downvote
                                    getSubredditRef(member, submission, SubmissionReference::unvote);
                                    voting.put(author, VoteDirection.NONE);
                                });
                    });
                }
            }).exceptionally(t -> {
                LOGGER.error("An error occurred", t);
                return null;
            });
        }, () -> EmbedUtils.error(channel, author, "The /next command must be ran in a subreddit channel"));
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
        message.removeReaction(emote, member.getUser()).queue();
        callback.run();
    }

    private Optional<SubmissionReference> getSubredditRef(Member member, Submission submission) {
        return getAccount(member).map(submission::toReference);
    }

    private void getSubredditRef(Member member, Submission submission, Consumer<SubmissionReference> consumer) {
        getAccount(member).map(submission::toReference).ifPresent(consumer);
    }

    private Optional<RedditClient> getAccount(Member member) {
        return userManager.getUser(member.getUser()).map(LinkedUser::getRedditAccount);
    }

    public void reset(SubredditLink subredditLink) {
        paginators.remove(subredditLink);
    }

    private CompletableFuture<Void> clearOrContinue(TextChannel channel, boolean clear) {
        if (clear) return CompletableFuture.completedFuture(null);
        return clearMessages(channel);
    }

    private CompletableFuture<Void> clearMessages(TextChannel channel) {
        var repeating = new AtomicBoolean(true);
        var iters = new AtomicInteger(0);
        return CompletableFuture.runAsync(() -> {
            while (repeating.get() && iters.getAndIncrement() < 500) {
                var messages = channel.getHistory().retrievePast(100).complete();
                var retrieved = channel.getHistoryFromBeginning(1).complete();
                var removingMessages = Collections.synchronizedList(messages);
                try {
                    var history = retrieved.getRetrievedHistory();
                    if (history.isEmpty()) return;
                    var first = history.get(history.size() - 1);
                    if (first.getEmbeds().isEmpty()) return;
                    var title = first.getEmbeds().get(0).getTitle();
                    if (title != null && title.equalsIgnoreCase("Subreddit Info")) {
                        removingMessages.removeIf(message -> message.getIdLong() == first.getIdLong());
                    }
                } finally {
                    iters.incrementAndGet();
                    if (removingMessages.size() > 2) {
                        channel.deleteMessages(removingMessages).queue();
                    } else {
                        repeating.set(false);
                        if (removingMessages.size() == 1) {
                            channel.deleteMessageById(removingMessages.get(0).getIdLong()).queue();
                        }
                    }
                }
            }
        });
    }
}
