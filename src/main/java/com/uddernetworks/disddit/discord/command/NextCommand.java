package com.uddernetworks.disddit.discord.command;

import com.uddernetworks.disddit.Disddit;
import com.uddernetworks.disddit.discord.DiscordManager;
import com.uddernetworks.disddit.discord.EmbedUtils;
import com.uddernetworks.disddit.discord.disddit.SubredditDataManager;
import com.uddernetworks.disddit.discord.disddit.SubredditLink;
import com.uddernetworks.disddit.discord.disddit.SubredditManager;
import com.uddernetworks.disddit.discord.reaction.ReactManager;
import com.uddernetworks.disddit.discord.reaction.VoteListener;
import com.uddernetworks.disddit.user.UserManager;
import net.dean.jraw.models.Submission;
import net.dean.jraw.models.SubredditSort;
import net.dean.jraw.pagination.DefaultPaginator;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import static com.uddernetworks.disddit.discord.disddit.SubredditManager.clearMessages;

public class NextCommand extends Command {

    private static final Logger LOGGER = LoggerFactory.getLogger(NextCommand.class);

    private static final Pattern PREVIEW_PATTERN = Pattern.compile(".*\\.(png|jpg|jpeg|bmp|gif|gifv)$");

    private final Disddit disddit;
    private final DiscordManager discordManager;
    private final ReactManager reactManager;
    private final SubredditManager subredditManager;
    private final SubredditDataManager subredditDataManager;
    private final UserManager userManager;
    private final Map<SubredditLink, Long> timeouts = new HashMap<>();
    private final Map<SubredditLink, DefaultPaginator<Submission>> paginators = new ConcurrentHashMap<>();

    public NextCommand(Disddit disddit) {
        super("next");
        this.disddit = disddit;
        this.discordManager = disddit.getDiscordManager();
        this.reactManager = discordManager.getReactManager();
        this.subredditManager = disddit.getSubredditManager();
        this.subredditDataManager = subredditManager.getSubredditDataManager();
        this.userManager = disddit.getUserManager();
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

                    embed.setFooter(submission.getAuthor());
                    embed.setTimestamp(submission.getCreated().toInstant());

                    channel.sendMessage(embed.build()).queue(message -> {
                        new VoteListener<>(disddit, message, submission, user -> {
                            var member = Objects.requireNonNull(channel.getGuild().getMember(user.getDiscordUser()));
                            disddit.getCommentManager().openCommentThread(submission, channel, user).thenAccept(channelOptional -> {
                                channelOptional.ifPresentOrElse(commentChannel -> {
                                    channel.sendMessage(member.getAsMention() + " Go to " + commentChannel.getChannel().getAsMention()).queue();
                                }, () -> {
                                    EmbedUtils.error(channel, member, "Couldn't open the channel. Make sure to wait a few seconds between opening comment threads.");
                                });
                            });
                        }).startListener();
                    });
                }
            }).exceptionally(t -> {
                LOGGER.error("An error occurred", t);
                return null;
            });
        }, () -> EmbedUtils.error(channel, author, "The /next command must be ran in a subreddit channel"));
    }

    public void reset(SubredditLink subredditLink) {
        paginators.remove(subredditLink);
    }

    private CompletableFuture<Void> clearOrContinue(TextChannel channel, boolean clear) {
        if (clear) return CompletableFuture.completedFuture(null);
        return clearMessages(channel);
    }
}
