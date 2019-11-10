package com.uddernetworks.reddicord.reddit.user;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.uddernetworks.reddicord.reddit.RedditManager;
import net.dean.jraw.RedditClient;

import java.io.IOException;

public class RedditClientAdapter extends TypeAdapter<RedditClient> {

    private RedditManager redditManager;

    public RedditClientAdapter(RedditManager redditManager) {
        this.redditManager = redditManager;
    }

    @Override
    public void write(JsonWriter writer, RedditClient value) throws IOException {
        writer.beginObject()
                .name("name").value(value.me().getUsername())
                .endObject();
    }

    @Override
    public RedditClient read(JsonReader reader) throws IOException {
        reader.beginObject();

        if (!reader.hasNext()) return null;

        RedditClient client = null;
        if ("name".equals(reader.nextName())) {
            var name = reader.nextString();
            client = redditManager.getAccount(name).orElse(null);
        }

        reader.endObject();
        return client;
    }

}
