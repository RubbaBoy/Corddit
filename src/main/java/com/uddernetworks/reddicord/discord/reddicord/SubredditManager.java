package com.uddernetworks.reddicord.discord.reddicord;

import com.uddernetworks.reddicord.Reddicord;
import com.uddernetworks.reddicord.database.DatabaseManager;
import com.uddernetworks.reddicord.discord.DiscordStateManager;
import net.dv8tion.jda.api.entities.Guild;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class SubredditManager {

    private final Reddicord reddicord;
    private final DatabaseManager databaseManager;
    private final DiscordStateManager discordStateManager;
    private final Map<Guild, List<SubredditLink>> subredditLinks = new ConcurrentHashMap<>();

    public SubredditManager(Reddicord reddicord, DatabaseManager databaseManager, DiscordStateManager discordStateManager) {
        this.reddicord = reddicord;
        this.databaseManager = databaseManager;
        this.discordStateManager = discordStateManager;
    }

    public CompletableFuture<Void> init() {
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
        return subreddits.stream().anyMatch(subredditLink -> subredditLink.getSubreddit().equalsIgnoreCase(subreddit));
    }

    public CompletableFuture<Void> removeSubreddit(Guild guild, String subreddit) {
        return databaseManager.removeSubreddit(guild, subreddit)
                .thenRun(() -> removeCachedSubreddit(guild, subreddit));
    }

    private void removeCachedSubreddit(Guild guild, String subreddit) {
        subredditLinks.computeIfPresent(guild, (k, subreddits) -> {
            subreddits.stream().filter(subredditLink -> subredditLink.getSubreddit().equalsIgnoreCase(subreddit)).findFirst().ifPresent(subredditLink -> {
                subredditLink.getTextChannel().delete().queue();
                subreddits.remove(subredditLink);
            });
            return subreddits;
        });
    }

    public CompletableFuture<Void> addSubreddit(Guild guild, String subreddit) {
        if (hasSubreddit(guild, subreddit)) return CompletableFuture.completedFuture(null);
        return discordStateManager.addSubredditChannel(guild, subreddit)
                .thenAccept(textChannel -> addSubreddit(new SubredditLink(textChannel, subreddit)).join());
    }

    public CompletableFuture<Void> addSubreddit(SubredditLink subredditLink) {
        if (hasSubreddit(subredditLink.getGuild(), subredditLink.getSubreddit())) return CompletableFuture.completedFuture(null);
        return databaseManager.addSubreddit(subredditLink.getTextChannel(), subredditLink.getSubreddit()).thenRun(() ->
                subredditLinks.computeIfAbsent(subredditLink.getGuild(), $ -> Collections.synchronizedList(new ArrayList<>())).add(subredditLink));
    }
}
