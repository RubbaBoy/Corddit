package com.uddernetworks.reddicord.reddit.user;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.uddernetworks.reddicord.Reddicord;
import net.dean.jraw.RedditClient;
import net.dv8tion.jda.api.entities.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class UserManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserManager.class);
    private static final Type LIST_TYPE = new TypeToken<List<LinkedUser>>() {}.getType();

    private final Reddicord reddicord;
    private final Path userStore;
    private final Gson gson;
    private final List<LinkedUser> users = Collections.synchronizedList(new ArrayList<>());
    private boolean autoSave = true;

    public UserManager(Reddicord reddicord, File userStore) {
        this.reddicord = reddicord;
        this.userStore = userStore.toPath().toAbsolutePath();
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeHierarchyAdapter(User.class, new DiscordUserAdapter(reddicord.getDiscordManager()))
                .registerTypeHierarchyAdapter(RedditClient.class, new RedditClientAdapter(reddicord.getRedditManager()))
                .create();
    }

    public void addUser(LinkedUser linkedUser) {
        users.add(linkedUser);
        if (autoSave) save();
    }

    public Optional<LinkedUser> getUser(User discordUser) {
        return users.stream().filter(linkedUser -> linkedUser.getDiscordUser().equals(discordUser)).findFirst();
    }

    public void save() {
        CompletableFuture.runAsync(() -> {
            try {
                LOGGER.debug("Saving users to {}", userStore);
                synchronized (users) {
                    Files.writeString(userStore, gson.toJson(users));
                }
            } catch (Exception e) {
                LOGGER.error("An error occurred while writing to " + userStore, e);
            }
        }).exceptionally(t -> {
            LOGGER.error("An error occurred while saving users", t);
            return null;
        });
    }

    public void load() {
        CompletableFuture.runAsync(() -> {
            try {
                LOGGER.debug("Loading users from {}", userStore);
                if (!Files.exists(userStore)) return;
                synchronized (users) {
                    users.clear();
                    users.addAll(gson.fromJson(Files.readString(userStore), LIST_TYPE));
                }
            } catch (IOException e) {
                LOGGER.error("An error occurred while writing to " + userStore, e);
            }
        }).exceptionally(t -> {
            LOGGER.error("An error occurred while loading users", t);
            return null;
        });
    }

    public List<LinkedUser> getUsers() {
        return users;
    }

    public boolean isAutoSaving() {
        return autoSave;
    }

    public void setAutoSave(boolean autoSave) {
        this.autoSave = autoSave;
    }
}
