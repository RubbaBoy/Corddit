package com.uddernetworks.reddicord.reddit.user;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.uddernetworks.reddicord.discord.DiscordManager;
import net.dv8tion.jda.api.entities.User;

import java.io.IOException;

public class DiscordUserAdapter extends TypeAdapter<User> {

    private DiscordManager discordManager;

    public DiscordUserAdapter(DiscordManager discordManager) {
        this.discordManager = discordManager;
    }

    @Override
    public void write(JsonWriter writer, User value) throws IOException {
        writer.beginObject()
                .name("id").value(value.getIdLong())
                .endObject();
    }

    @Override
    public User read(JsonReader reader) throws IOException {
        reader.beginObject();

        if (!reader.hasNext()) return null;

        User user = null;
        if ("id".equals(reader.nextName())) {
            user = discordManager.getJDA().getUserById(reader.nextLong());
        }

        reader.endObject();
        return user;
    }

}
