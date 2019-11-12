package com.uddernetworks.reddicord.reddit.user;

import com.google.gson.reflect.TypeToken;
import com.uddernetworks.reddicord.Reddicord;
import com.uddernetworks.reddicord.database.DatabaseManager;
import net.dv8tion.jda.api.entities.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class UserManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserManager.class);
    private static final Type LIST_TYPE = new TypeToken<List<LinkedUser>>() {}.getType();

    private final Reddicord reddicord;
    private final DatabaseManager databaseManager;
    private final List<LinkedUser> users = Collections.synchronizedList(new ArrayList<>());

    public UserManager(Reddicord reddicord) {
        this.reddicord = reddicord;
        this.databaseManager = reddicord.getDatabaseManager();
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

    public void load() {
        databaseManager.getAllLinkedAccounts().thenAccept(linkedUsers -> {
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
