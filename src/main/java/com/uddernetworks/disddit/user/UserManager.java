package com.uddernetworks.disddit.user;

import com.uddernetworks.disddit.Disddit;
import com.uddernetworks.disddit.database.DatabaseManager;
import net.dv8tion.jda.api.entities.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class UserManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserManager.class);

    private final Disddit disddit;
    private final DatabaseManager databaseManager;
    private final List<LinkedUser> users = Collections.synchronizedList(new ArrayList<>());

    public UserManager(Disddit disddit, DatabaseManager databaseManager) {
        this.disddit = disddit;
        this.databaseManager = databaseManager;
    }

    public void addUser(LinkedUser linkedUser) {
        users.add(linkedUser);
        databaseManager.addLinkedAccount(linkedUser);
    }

    public Optional<LinkedUser> getUser(User discordUser) {
        return users.stream().filter(linkedUser -> linkedUser.getDiscordUser().equals(discordUser)).findFirst();
    }

    public List<LinkedUser> getUsers() {
        return Collections.unmodifiableList(users);
    }

    public CompletableFuture<Void> load() {
        return databaseManager.getAllLinkedAccounts().thenAccept(linkedUsers -> {
            synchronized (users) {
                users.clear();
                users.addAll(linkedUsers);
            }
            LOGGER.info("Loaded {} users", users.size());
        }).exceptionally(t -> {
            LOGGER.error("An error occurred while loading database", t);
            return null;
        });
    }
}
