package com.uddernetworks.reddicord.discord.command;

import com.uddernetworks.reddicord.Reddicord;
import com.uddernetworks.reddicord.discord.DiscordManager;
import com.uddernetworks.reddicord.discord.EmbedUtils;
import com.uddernetworks.reddicord.discord.reddicord.SubredditDataFetcher;
import com.uddernetworks.reddicord.discord.reddicord.SubredditLink;
import com.uddernetworks.reddicord.discord.reddicord.SubredditManager;
import net.dean.jraw.models.Submission;
import net.dean.jraw.models.SubredditSort;
import net.dean.jraw.pagination.DefaultPaginator;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class NextCommand extends Command {

    private static final Logger LOGGER = LoggerFactory.getLogger(NextCommand.class);

    private final Reddicord reddicord;
    private final DiscordManager discordManager;
    private final SubredditManager subredditManager;
    private final SubredditDataFetcher subredditDataFetcher;
    private final Map<SubredditLink, Long> timeouts = new HashMap<>();
    private final Map<SubredditLink, DefaultPaginator<Submission>> paginators = new ConcurrentHashMap<>();

    public NextCommand(Reddicord reddicord) {
        super("next");
        this.reddicord = reddicord;
        this.discordManager = reddicord.getDiscordManager();
        this.subredditManager = reddicord.getSubredditManager();
        this.subredditDataFetcher = subredditManager.getSubredditDataFetcher();
    }

    @Override
    public void onCommand(Member author, TextChannel channel, String[] args) {
        if (args.length != 0) {
            EmbedUtils.error(channel, author, "Usage: /next");
            return;
        }

        subredditManager.getLinkFromChannel(channel).ifPresentOrElse(link -> {
            if (timeouts.containsKey(link) && timeouts.get(link) > System.currentTimeMillis()) {
                EmbedUtils.error(channel, author, "You can only run this command once every 5 seconds per subreddit");
                return;
            }
            timeouts.put(link, System.currentTimeMillis() + 5000);

            var color = EmbedUtils.hex2Rgb(link.getSubreddit().getKeyColor());

            channel.getHistory().retrievePast(100).queue(messages -> {
                channel.getHistoryFromBeginning(1).queue(retrieved -> {
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
                        channel.deleteMessages(removingMessages).queue();
                    }
                });

                CompletableFuture.runAsync(() -> {
                    var hot = paginators.computeIfAbsent(link, $ -> link.getSubreddit().toReference(subredditDataFetcher.getClient()).posts().sorting(SubredditSort.HOT).limit(1).build());
                    for (int i = 0; i < 10; i++) {
                        var listing = hot.next();
                        var submission = listing.get(0);

                        var embed = new EmbedBuilder();

                        embed.setTitle(submission.getTitle(), "https://reddit.com" + submission.getPermalink());
                        embed.setColor(color);

                        var descriptionSet = false;
                        if (submission.hasThumbnail()) {
                            embed.setImage(submission.getUrl());
                            descriptionSet = true;
                        }

                        var media = submission.getEmbeddedMedia();
                        if (media != null) {
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

                        embed.setFooter(submission.getAuthor());
                        embed.setTimestamp(submission.getCreated().toInstant());

                        channel.sendMessage(embed.build()).queue();
                    }
                }).exceptionally(t -> {
                    LOGGER.error("An error occurred", t);
                    return null;
                });
            });

        }, () -> EmbedUtils.error(channel, author, "The /next command must be ran in a subreddit channel"));
    }

    public void reset(SubredditLink subredditLink) {
        paginators.remove(subredditLink);
    }
}
