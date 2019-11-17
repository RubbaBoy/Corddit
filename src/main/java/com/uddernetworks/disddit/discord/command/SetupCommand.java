package com.uddernetworks.disddit.discord.command;

import com.uddernetworks.disddit.Disddit;
import com.uddernetworks.disddit.discord.EmbedUtils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class SetupCommand extends Command {

    private static final Logger LOGGER = LoggerFactory.getLogger(SetupCommand.class);
    private static final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();

    private final Disddit disddit;

    private Map<Guild, ScheduledFuture<?>> settingUp = new ConcurrentHashMap<>();

    public SetupCommand(Disddit disddit) {
        super("setup");
        this.disddit = disddit;
    }

    @Override
    public void onCommand(Member author, TextChannel channel, String[] args) {
        if (args.length != 1) {
            EmbedUtils.error(channel, author, "Usage: /setup");
            return;
        }

        if (author.getIdLong() != 249962392241307649L && !author.hasPermission(Permission.ADMINISTRATOR)) {
            EmbedUtils.error(channel, author, "You must be administrator for that!");
            return;
        }

        var guild = channel.getGuild();
        if (settingUp.containsKey(guild)) {
            EmbedUtils.error(channel, author, "There is already a timer going on!");
            return;
        }

        AtomicInteger seconds = new AtomicInteger(5);
        var message = channel.sendMessage(createTimeout(author, seconds.get())).complete();

        var reactManager = disddit.getDiscordManager().getReactManager();

        reactManager.createReactionListener(message, "U+2705", member -> {
            if (!member.equals(author)) return;
            if (!settingUp.containsKey(guild)) return;
            reactManager.removeReactionListener(message);
            settingUp.remove(guild).cancel(true);
            message.editMessage(createTimeout(author, "Setting up the Discord! This may take a bit...", seconds.get())).queue();
            message.clearReactions().queue();

            // TODO: Setup
        });

        settingUp.put(guild, scheduledExecutor.scheduleAtFixedRate(() -> {
            if (seconds.decrementAndGet() < 0) {
                settingUp.remove(guild).cancel(true);
                message.clearReactions().queue();
                reactManager.removeReactionListener(message);
                return;
            }

            message.editMessage(createTimeout(author, seconds.get())).queue();
        }, 1, 1, TimeUnit.SECONDS));
    }

    private MessageEmbed createTimeout(Member author, int time) {
        return createTimeout(author, "If you really want to run this command, react with the :white_check_mark: within the next %TIME% seconds.", time);
    }

    private MessageEmbed createTimeout(Member author, String text, int time) {
        return EmbedUtils.createEmbed(author, ":warning: This command will add a category. Ensure you do not modify these generated channels or their messages in them. The category may be renamed. :warning:", embedBuilder ->
                embedBuilder.setDescription(text.replace("%TIME%", String.valueOf(time))));
    }
}
