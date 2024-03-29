package com.uddernetworks.corddit.discord;

import com.uddernetworks.corddit.discord.corddit.SubredditLink;
import net.dean.jraw.models.Subreddit;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.annotation.Nonnull;
import java.awt.Color;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class EmbedUtils extends ListenerAdapter {

    private static Map<Message, Long> messageRequesters = Collections.synchronizedMap(new HashMap<>());

    public static void error(TextChannel channel, Member author, String message) {
        EmbedBuilder eb = new EmbedBuilder();

        eb.setTitle("Error", null);
        eb.setColor(new Color(0xFF0000));

        eb.setDescription(message);

        eb.setFooter("In response to " + author.getEffectiveName(), author.getUser().getAvatarUrl());
        channel.sendMessage(eb.build()).submit().thenAccept(sent -> {
            sent.addReaction("U+1F5D1").queue();
            messageRequesters.put(sent, author.getIdLong());
        });
    }

    public static Message sendEmbed(TextChannel channel, Member author, String title, String description) {
        return sendEmbed(channel, author, title, embed -> embed.setDescription(description));
    }

    public static Message sendEmbed(TextChannel channel, Member author, String title, Consumer<EmbedBuilder> embedBuilderConsumer) {
        var message = channel.sendMessage(createEmbed(author, title, embedBuilderConsumer)).complete();
        if (author != null) {
            message.addReaction("U+1F5D1").queue();
            messageRequesters.put(message, author.getIdLong());
        }
        return message;
    }

    public static MessageEmbed createEmbed(String title, Consumer<EmbedBuilder> embedBuilderConsumer) {
        return createEmbed(null, title, embedBuilderConsumer);
    }

    public static MessageEmbed createEmbed(Member author, String title, Consumer<EmbedBuilder> embedBuilderConsumer) {
        EmbedBuilder eb = new EmbedBuilder();

        eb.setTitle(title, null);
        eb.setColor(new Color(0x424BE9));

        if (author != null) {
            eb.setFooter("Requested by " + author.getEffectiveName(), author.getUser().getAvatarUrl());
        }

        embedBuilderConsumer.accept(eb);
        return eb.build();
    }

    public static void setColor(EmbedBuilder embed, SubredditLink subredditLink) {
        setColor(embed, subredditLink.getSubreddit());
    }

    public static void setColor(EmbedBuilder embed, Subreddit subreddit) {
        if (subreddit == null || subreddit.getKeyColor() == null) return;
        embed.setColor(hex2Rgb(subreddit.getKeyColor()));
    }

    /**
     * Thanks SO <3
     * https://stackoverflow.com/a/4129692/3929546
     */
    public static Color hex2Rgb(String colorStr) {
        if (colorStr == null || colorStr.length() != 7) return Color.BLACK;
        return new Color(
                Integer.valueOf(colorStr.substring(1, 3), 16),
                Integer.valueOf(colorStr.substring(3, 5), 16),
                Integer.valueOf(colorStr.substring(5, 7), 16));
    }

    @Override
    public void onGuildMessageReactionAdd(@Nonnull GuildMessageReactionAddEvent event) {
        if (event.getUser().isBot()) return;
        if (!event.getReactionEmote().isEmoji()) return;
        if (!event.getReactionEmote().getAsCodepoints().equalsIgnoreCase("U+1F5D1")) return;
        event.getChannel().retrieveMessageById(event.getMessageIdLong()).submit().thenAccept(message -> {
            var userId = event.getUser().getIdLong();
            var member = event.getGuild().getMember(event.getUser());

            if (messageRequesters.containsKey(message) &&
                    messageRequesters.get(message) != userId &&
                    !member.hasPermission(Permission.MESSAGE_MANAGE)) {
                return;
            }

            if (message.getAuthor().getIdLong() == event.getJDA().getSelfUser().getIdLong()) {
                message.delete().queue();
            }
        });
    }
}
