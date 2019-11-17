package com.uddernetworks.disddit.database;

import com.uddernetworks.disddit.Disddit;
import com.uddernetworks.disddit.discord.DiscordManager;
import com.uddernetworks.disddit.discord.disddit.SubredditLink;
import com.uddernetworks.disddit.reddit.RedditManager;
import com.uddernetworks.disddit.user.LinkedUser;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DatabaseManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseManager.class);

    private final Disddit disddit;
    private RedditManager redditManager;
    private DiscordManager discordManager;
    private DataSource dataSource;

    private final String getAllLinkedAccounts = "SELECT * FROM `users`";

    private final String addLinkedAccount = "INSERT INTO `users` VALUES(?, ?) ON DUPLICATE KEY UPDATE reddit = reddit";

    private final String getAllSubreddits = "SELECT * FROM `subreddits`";

    private final String addSubreddit = "INSERT INTO `subreddits` VALUES(?, ?, ?)";

    private final String removeSubreddit = "DELETE FROM `subreddits` WHERE guild = ? AND subreddit = ?";

    private final String addGuild = "INSERT INTO `guilds` VALUES(?, ?) ON DUPLICATE KEY UPDATE category = category";

    private final String getGuildCategory = "SELECT category FROM `guilds` WHERE guild = ?";

    private final String getAllGuildCategories = "SELECT * FROM `guilds`";

    public DatabaseManager(Disddit disddit) {
        this.disddit = disddit;
    }

    public void init(String databasePath) {
        this.redditManager = disddit.getRedditManager();
        this.discordManager = disddit.getDiscordManager();

        long start = System.currentTimeMillis();
        var config = new HikariConfig();

        try {
            Class.forName("org.hsqldb.jdbcDriver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        var filePath = new File(databasePath + File.separator + "disddit");
        filePath.getParentFile().mkdirs();
        config.setJdbcUrl("jdbc:hsqldb:file:" + filePath);
        config.setUsername("SA");
        config.setPassword("");

        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "1000");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "8192");

        dataSource = new HikariDataSource(config);

        try (var connection = this.dataSource.getConnection();
             var useMySQL = connection.prepareStatement("SET DATABASE SQL SYNTAX MYS TRUE")) {
            useMySQL.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("Error while trying to set MySQL dialect", e);
        }

        Stream.of("users.sql", "subreddits.sql", "guilds.sql").forEach(table -> {
            try (var reader = new BufferedReader(new InputStreamReader(DatabaseManager.class.getResourceAsStream("/" + table)));
                 var connection = dataSource.getConnection();
                 var statement = connection.prepareStatement(reader.lines().collect(Collectors.joining("\n")))) {
                statement.executeUpdate();
            } catch (IOException | SQLException e) {
                LOGGER.error("Error creating table from file " + table, e);
            }
        });

        LOGGER.info("Initialized DatabaseManager in {}ms", System.currentTimeMillis() - start);
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    private List<GenericMap> iterResultSet(ResultSet resultSet) throws SQLException {
        var meta = resultSet.getMetaData();
        int columns = meta.getColumnCount();

        var rows = new ArrayList<GenericMap>();
        while (resultSet.next()) {
            var row = new GenericMap();
            for(int i = 1; i <= columns; ++i){
                row.put(meta.getColumnName(i).toLowerCase(), resultSet.getObject(i));
            }
            rows.add(row);
        }

        return List.copyOf(rows);
    }

    private <T> CompletableFuture<List<T>> mapResultOf(String query, Function<GenericMap, Optional<T>> mappingFunction) {
        return CompletableFuture.supplyAsync(() -> {
            try (var connection = dataSource.getConnection();
                 var statement = connection.prepareStatement(query)) {
                return iterResultSet(statement.executeQuery())
                        .stream()
                        .map(mappingFunction)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(Collectors.toUnmodifiableList());
            } catch (SQLException e) {
                LOGGER.error("Error during #mapResultOf for '" + query + "'", e);
                return Collections.emptyList();
            }
        });
    }

    public CompletableFuture<List<LinkedUser>> getAllLinkedAccounts() {
        return mapResultOf(getAllLinkedAccounts, genericMap -> {
            var reddit = redditManager.getAccount(genericMap.get("reddit"));
            var discord = discordManager.getUser(genericMap.get("discord"));
            if (discord.isEmpty() || reddit.isEmpty()) return Optional.empty();
            return Optional.of(new LinkedUser(discord.get(), reddit.get()));
        });
    }

    public CompletableFuture<Void> addLinkedAccount(LinkedUser linkedUser) {
        return addLinkedAccount(linkedUser.getRedditName(), linkedUser.getDiscordUser().getIdLong());
    }

    public CompletableFuture<Void> addLinkedAccount(String redditName, long discordId) {
        return CompletableFuture.runAsync(() -> {
            try (var connection = dataSource.getConnection();
                 var statement = connection.prepareStatement(addLinkedAccount)) {
                statement.setString(1, redditName);
                statement.setLong(2, discordId);
                statement.executeUpdate();
            } catch (SQLException e) {
                LOGGER.error("Error during #addLinkedAccount(String, long)", e);
            }
        });
    }

    public CompletableFuture<List<SubredditLink>> getAllSubreddits() {
        return mapResultOf(getAllSubreddits, genericMap -> {
            var channel = discordManager.getJDA().getTextChannelById(genericMap.<Long>get("channel"));
            if (channel == null) return Optional.empty();
            return disddit.getSubredditManager().getSubredditDataManager().getSubreddit(genericMap.get("subreddit")).join().map(subreddit -> new SubredditLink(channel, subreddit));
        });
    }

    public CompletableFuture<Void> addSubreddit(TextChannel textChannel, String subreddit) {
        return CompletableFuture.runAsync(() -> {
            try (var connection = dataSource.getConnection();
                 var statement = connection.prepareStatement(addSubreddit)) {
                statement.setLong(1, textChannel.getGuild().getIdLong());
                statement.setLong(2, textChannel.getIdLong());
                statement.setString(3, subreddit);
                statement.executeUpdate();
            } catch (SQLException e) {
                LOGGER.error("Error during #addSubreddit(TextChannel, String)", e);
            }
        });
    }

    public CompletableFuture<Void> removeSubreddit(Guild guild, String subreddit) {
        return CompletableFuture.runAsync(() -> {
            try (var connection = dataSource.getConnection();
                 var statement = connection.prepareStatement(removeSubreddit)) {
                statement.setLong(1, guild.getIdLong());
                statement.setString(2, subreddit);
                statement.executeUpdate();
            } catch (SQLException e) {
                LOGGER.error("Error during #removeSubreddit(Guild, String)", e);
            }
        });
    }

    public CompletableFuture<Void> addGuild(Guild guild, Category category) {
        return CompletableFuture.runAsync(() -> {
            try (var connection = dataSource.getConnection();
                 var statement = connection.prepareStatement(addGuild)) {
                statement.setLong(1, guild.getIdLong());
                statement.setLong(2, category.getIdLong());
                statement.executeUpdate();
            } catch (SQLException e) {
                LOGGER.error("Error during #addSubreddit(TextChannel, String)", e);
            }
        });
    }

    public CompletableFuture<Optional<Category>> getGuildCategory(Guild guild) {
        return CompletableFuture.supplyAsync(() -> {
            try (var connection = dataSource.getConnection();
                 var statement = connection.prepareStatement(getGuildCategory)) {
                statement.setLong(1, guild.getIdLong());
                var rs = statement.executeQuery();
                if (rs.next()) {
                    return Optional.ofNullable(guild.getCategoryById(rs.getLong("category")));
                }
            } catch (SQLException e) {
                LOGGER.error("Error during #addSubreddit(TextChannel, String)", e);
            }
            return Optional.empty();
        });
    }

    public CompletableFuture<Map<Guild, Category>> getAllGuildCategories() {
        return CompletableFuture.supplyAsync(() -> {
            try (var connection = dataSource.getConnection();
                 var statement = connection.prepareStatement(getAllGuildCategories)) {
                return iterResultSet(statement.executeQuery())
                        .stream()
                        .map(map -> discordManager.getJDA().getCategoryById(map.<Long>get("category")))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toMap(GuildChannel::getGuild, category -> category));
            } catch (Exception e) {
                LOGGER.error("Error during #getAllGuildCategories()", e);
                return Collections.emptyMap();
            }
        });
    }
}
