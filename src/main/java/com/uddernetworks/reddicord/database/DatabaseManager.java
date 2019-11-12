package com.uddernetworks.reddicord.database;

import com.uddernetworks.reddicord.Reddicord;
import com.uddernetworks.reddicord.discord.DiscordManager;
import com.uddernetworks.reddicord.reddit.RedditManager;
import com.uddernetworks.reddicord.reddit.user.LinkedUser;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.intellij.lang.annotations.Language;
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
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DatabaseManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseManager.class);

    private Reddicord reddicord;
    private RedditManager redditManager;
    private DiscordManager discordManager;
    private DataSource dataSource;

    @Language("MySQL")
    private String getAllLinkedAccounts = "SELECT * FROM `users`;";

    @Language("MySQL")
    private String addLinkedAccount = "INSERT INTO `users` VALUES(?, ?)";

    public DatabaseManager(Reddicord reddicord) {
        this.reddicord = reddicord;
        this.redditManager = reddicord.getRedditManager();
        this.discordManager = reddicord.getDiscordManager();
    }

    public void init(String databasePath) {
        long start = System.currentTimeMillis();
        var config = new HikariConfig();

        try {
            Class.forName("org.hsqldb.jdbcDriver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        var filePath = new File(databasePath + File.separator + "reddicord");
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

        Stream.of("users.sql").forEach(table -> {
            try (var reader = new BufferedReader(new InputStreamReader(DatabaseManager.class.getResourceAsStream("/" + table)));
                 var connection = dataSource.getConnection();
                 var statement = connection.prepareStatement(reader.lines().collect(Collectors.joining("\n")))) {
                statement.executeUpdate();
            } catch (IOException | SQLException e) {
                e.printStackTrace();
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

    private Optional<LinkedUser> linkedUserFrom(GenericMap genericMap) {
        var reddit = redditManager.getAccount(genericMap.get("reddit"));
        var discord = discordManager.getUser(genericMap.get("discord"));
        if (discord.isEmpty() || reddit.isEmpty()) return Optional.empty();
        return Optional.of(new LinkedUser(discord.get(), reddit.get()));
    }

    public CompletableFuture<List<LinkedUser>> getAllLinkedAccounts() {
        return CompletableFuture.supplyAsync(() -> {
            try (var connection = dataSource.getConnection();
                var statement = connection.prepareStatement(getAllLinkedAccounts)) {
                return iterResultSet(statement.executeQuery())
                        .stream()
                        .map(this::linkedUserFrom)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(Collectors.toUnmodifiableList());
            } catch (SQLException e) {
                LOGGER.error("Error during #getAllLinkedAccounts()", e);
                return Collections.emptyList();
            }
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
                LOGGER.error("Error during #getAllLinkedAccounts()", e);
            }
        });
    }
}
