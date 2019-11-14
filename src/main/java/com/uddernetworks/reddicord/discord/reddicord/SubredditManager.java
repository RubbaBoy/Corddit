package com.uddernetworks.reddicord.discord.reddicord;

import com.uddernetworks.reddicord.Reddicord;
import com.uddernetworks.reddicord.database.DatabaseManager;
import com.uddernetworks.reddicord.discord.DiscordStateManager;
import com.uddernetworks.reddicord.discord.EmbedUtils;
import net.dean.jraw.models.Subreddit;
import net.dv8tion.jda.api.entities.Guild;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class SubredditManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubredditManager.class);

    private final Reddicord reddicord;
    private final DatabaseManager databaseManager;
    private final DiscordStateManager discordStateManager;
    private final SubredditDataFetcher subredditDataFetcher;
    private final Map<Guild, List<SubredditLink>> subredditLinks = new ConcurrentHashMap<>();

    public SubredditManager(Reddicord reddicord, DatabaseManager databaseManager, DiscordStateManager discordStateManager) {
        this.reddicord = reddicord;
        this.databaseManager = databaseManager;
        this.discordStateManager = discordStateManager;
        this.subredditDataFetcher = new SubredditDataFetcher(reddicord);
    }

    public CompletableFuture<Void> init() {
        subredditDataFetcher.init();
        return databaseManager.getAllSubreddits().thenAccept(subreddits -> {
            subredditLinks.clear();
            subreddits.forEach(subreddit ->
                    subredditLinks.computeIfAbsent(subreddit.getGuild(), $ -> Collections.synchronizedList(new ArrayList<>())).add(subreddit));
        });
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
        if (hasSubreddit(guild, subredditName)) {
            LOGGER.info("Has already!");
            return CompletableFuture.completedFuture(null);
        }
        return subredditDataFetcher.getSubreddit(subredditName).thenApply(subredditOptional -> {
            if (subredditOptional.isEmpty()) {
                LOGGER.info("Subreddit empty!");
                return Optional.empty();
            }

            var subreddit = subredditOptional.get();
            LOGGER.info("Pre subreddit: {}", subreddit);
            return discordStateManager.addSubredditChannel(guild, subreddit.getName())
                    .thenApply(textChannel -> {
                        var link = new SubredditLink(textChannel, subreddit);
                        addSubreddit(link).join();
                        LOGGER.info("Subreddit: {}", subreddit);
                        return Optional.of(subreddit);
                    }).join();
        });
    }

    public CompletableFuture<Void> addSubreddit(SubredditLink subredditLink) {
//        if (hasSubreddit(subredditLink.getGuild(), subredditLink.getName())) return CompletableFuture.completedFuture(null);
        sendInitialMessage(subredditLink);
        return databaseManager.addSubreddit(subredditLink.getTextChannel(), subredditLink.getName()).thenRun(() ->
                subredditLinks.computeIfAbsent(subredditLink.getGuild(), $ -> Collections.synchronizedList(new ArrayList<>())).add(subredditLink));
    }

    public void sendInitialMessage(SubredditLink subredditLink) {
        subredditLink.getTextChannel().sendMessage(EmbedUtils.createEmbed("Subreddit Info", embed -> {
            embed.setDescription("This is the Discord channel for r/" + subredditLink.getName() + "\nTo view the next set of posts, do **/next** Sorting is not available at the time, however in the future it will be.");
            embed.addField("**" + subredditLink.getName() + "**", subredditLink.getSubreddit().getPublicDescription(), false);
            EmbedUtils.setColor(embed, subredditLink);
        })).queue();
    }

    public SubredditDataFetcher getSubredditDataFetcher() {
        return subredditDataFetcher;
    }
}
