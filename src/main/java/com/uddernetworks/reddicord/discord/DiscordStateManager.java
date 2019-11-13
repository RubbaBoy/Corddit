package com.uddernetworks.reddicord.discord;

import com.uddernetworks.reddicord.Reddicord;
import com.uddernetworks.reddicord.database.DatabaseManager;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static net.dv8tion.jda.api.Permission.MANAGE_CHANNEL;
import static net.dv8tion.jda.api.Permission.MANAGE_PERMISSIONS;
import static net.dv8tion.jda.api.Permission.MESSAGE_WRITE;

public class DiscordStateManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiscordStateManager.class);

    private final Reddicord reddicord;
    private final DiscordManager discordManager;
    private final DatabaseManager databaseManager;
    private final Map<Guild, Category> categories = new ConcurrentHashMap<>();

    public DiscordStateManager(Reddicord reddicord, DiscordManager discordManager, DatabaseManager databaseManager) {
        this.reddicord = reddicord;
        this.discordManager = discordManager;
        this.databaseManager = databaseManager;
    }

    public CompletableFuture<Void> init() {
        return databaseManager.getAllGuildCategories().thenAccept(guildCategoryMap -> {
            synchronized (categories) {
                categories.clear();
                categories.putAll(guildCategoryMap);
            }
            LOGGER.info("Registered {} unique subreddit categories", categories.size());
        }).exceptionally(t -> {
            LOGGER.error("An error occurred while loading database", t);
            return null;
        });
    }

    public CompletableFuture<Category> createCategory(Guild guild) {
        var everyone = guild.getPublicRole();
        var subredditsCategory = guild.createCategory("subreddits")
                .addPermissionOverride(everyone, Collections.emptyList(), Arrays.asList(MANAGE_CHANNEL, MESSAGE_WRITE, MANAGE_PERMISSIONS)).complete();
        return databaseManager.addGuild(guild, subredditsCategory).thenApply($ -> subredditsCategory);
    }

    public CompletableFuture<Category> getOrCreateCategory(Guild guild) {
        return databaseManager.getGuildCategory(guild).thenApplyAsync(optionalCategory ->
                optionalCategory.orElseGet(() -> createCategory(guild).join()));
    }

    public CompletableFuture<TextChannel> addSubredditChannel(Guild guild, String subreddit) {
        return CompletableFuture.supplyAsync(() ->
                getOrCreateCategory(guild).thenApplyAsync(category ->
                    category.createTextChannel(subreddit).setTopic("Reddicord-managed channel for the r/" + subreddit + " subreddit").complete()).join());
    }
}
