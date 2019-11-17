package com.uddernetworks.disddit.discord.disddit;

import com.uddernetworks.disddit.Disddit;
import com.uddernetworks.disddit.database.DatabaseManager;
import com.uddernetworks.disddit.discord.DiscordStateManager;
import com.uddernetworks.disddit.discord.EmbedUtils;
import net.dean.jraw.models.Subreddit;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class SubredditManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubredditManager.class);

    private final Disddit disddit;
    private final DatabaseManager databaseManager;
    private final DiscordStateManager discordStateManager;
    private final SubredditDataManager subredditDataManager;
    private final Map<Guild, List<SubredditLink>> subredditLinks = new ConcurrentHashMap<>();

    public SubredditManager(Disddit disddit, DatabaseManager databaseManager, DiscordStateManager discordStateManager) {
        this.disddit = disddit;
        this.databaseManager = databaseManager;
        this.discordStateManager = discordStateManager;
        this.subredditDataManager = new SubredditDataManager(disddit);
    }

    public CompletableFuture<Void> init() {
        subredditDataManager.init();
        return databaseManager.getAllSubreddits().thenAccept(subreddits -> {
            subredditLinks.clear();
            subreddits.forEach(subreddit ->
                    subredditLinks.computeIfAbsent(subreddit.getGuild(), $ -> Collections.synchronizedList(new ArrayList<>())).add(subreddit));
        });
    }

    public Optional<SubredditLink> getLinkFromChannel(TextChannel channel) {
        var subreddits = subredditLinks.get(channel.getGuild());
        if (subreddits == null) return Optional.empty();
        return subreddits.stream().filter(link -> link.getTextChannel().equals(channel)).findFirst();
    }

    public Map<Guild, List<SubredditLink>> getSubreddits() {
        return Collections.unmodifiableMap(subredditLinks);
    }

    public List<SubredditLink> getSubreddits(Guild guild) {
        var subreddits = subredditLinks.get(guild);
        if (subreddits == null) return Collections.emptyList();
        return Collections.unmodifiableList(subreddits);
    }

    public boolean hasSubreddit(Guild guild, String subreddit) {
        var subreddits = subredditLinks.get(guild);
        if (subreddits == null) return false;
        return subreddits.stream().anyMatch(subredditLink -> subredditLink.getName().equalsIgnoreCase(subreddit));
    }

    public CompletableFuture<Void> removeSubreddit(Guild guild, String subreddit) {
        return databaseManager.removeSubreddit(guild, subreddit)
                .thenRun(() -> removeCachedSubreddit(guild, subreddit));
    }

    private void removeCachedSubreddit(Guild guild, String subreddit) {
        subredditLinks.computeIfPresent(guild, (k, subreddits) -> {
            subreddits.stream().filter(subredditLink -> subredditLink.getName().equalsIgnoreCase(subreddit)).findFirst().ifPresent(subredditLink -> {
                subredditLink.getTextChannel().delete().queue();
                subreddits.remove(subredditLink);
            });
            return subreddits;
        });
    }

    public CompletableFuture<Optional<Subreddit>> addSubreddit(Guild guild, String subredditName) {
        if (hasSubreddit(guild, subredditName)) return CompletableFuture.completedFuture(null);
        return subredditDataManager.getSubreddit(subredditName).thenApply(subredditOptional -> {
            if (subredditOptional.isEmpty()) return Optional.empty();
            var subreddit = subredditOptional.get();
            return discordStateManager.addSubredditChannel(guild, subreddit)
                    .thenApply(textChannel -> {
                        var link = new SubredditLink(textChannel, subreddit);
                        addSubreddit(link).join();
                        return Optional.of(subreddit);
                    }).exceptionally(t -> {
                        LOGGER.error("Error while creating subreddit!", t);
                        return Optional.empty();
                    }).join();
        });
    }

    public CompletableFuture<Void> addSubreddit(SubredditLink subredditLink) {
        sendInitialMessage(subredditLink);
        return databaseManager.addSubreddit(subredditLink.getTextChannel(), subredditLink.getName()).thenRun(() ->
                subredditLinks.computeIfAbsent(subredditLink.getGuild(), $ -> Collections.synchronizedList(new ArrayList<>())).add(subredditLink));
    }

    public void sendInitialMessage(SubredditLink subredditLink) {
        subredditLink.getTextChannel().sendMessage(EmbedUtils.createEmbed("Subreddit Info", embed -> {
            embed.setDescription("This is the Discord channel for r/" + subredditLink.getName() + "\nTo view the next set of posts, do **/next**\nSorting is not available at the time, however in the future it will be.");
            embed.addField("**" + subredditLink.getName() + "**", subredditLink.getSubreddit().getPublicDescription(), false);
            EmbedUtils.setColor(embed, subredditLink);
        })).queue();
    }

    public static CompletableFuture<Void> clearMessages(TextChannel channel) {
        var repeating = new AtomicBoolean(true);
        var iters = new AtomicInteger(0);
        return CompletableFuture.runAsync(() -> {
            while (repeating.get() && iters.getAndIncrement() < 500) {
                var messages = channel.getHistory().retrievePast(100).complete();
                var retrieved = channel.getHistoryFromBeginning(1).complete();
                var removingMessages = Collections.synchronizedList(messages);
                try {
                    var history = retrieved.getRetrievedHistory();
                    if (history.isEmpty()) break;
                    var first = history.get(history.size() - 1);
                    if (first.getEmbeds().isEmpty()) break;
                    var title = first.getEmbeds().get(0).getTitle();
                    if (title != null && title.equalsIgnoreCase("Subreddit Info")) {
                        removingMessages.removeIf(message -> message.getIdLong() == first.getIdLong());
                    }
                } finally {
                    iters.incrementAndGet();
                    if (removingMessages.size() > 2) {
                        channel.deleteMessages(removingMessages).queue(null, SubredditManager::handleError);
                    } else {
                        repeating.set(false);
                        if (removingMessages.size() == 1) {
                            channel.deleteMessageById(removingMessages.get(0).getIdLong()).queue(null, SubredditManager::handleError);
                        }
                    }
                }
            }
        });
    }

    private static void handleError(Throwable t) {
        if (t instanceof ErrorResponseException) {
            var ere = (ErrorResponseException) t;
            if (ere.getErrorCode() == 10008) return; // Unknown Message; don't be concerned
        }
        LOGGER.error("An error occurred while deleting message(s)", t);
    }

    public SubredditDataManager getSubredditDataManager() {
        return subredditDataManager;
    }
}
