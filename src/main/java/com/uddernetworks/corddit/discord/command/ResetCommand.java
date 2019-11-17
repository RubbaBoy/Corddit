package com.uddernetworks.corddit.discord.command;

import com.uddernetworks.corddit.Corddit;
import com.uddernetworks.corddit.discord.DiscordManager;
import com.uddernetworks.corddit.discord.EmbedUtils;
import com.uddernetworks.corddit.discord.corddit.SubredditLink;
import com.uddernetworks.corddit.discord.corddit.SubredditManager;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class ResetCommand extends Command {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResetCommand.class);

    private final Corddit corddit;
    private final DiscordManager discordManager;
    private final SubredditManager subredditManager;
    private final Map<SubredditLink, Long> timeouts = new HashMap<>();

    public ResetCommand(Corddit corddit) {
        super("reset");
        this.corddit = corddit;
        this.discordManager = corddit.getDiscordManager();
        this.subredditManager = corddit.getSubredditManager();
    }

    @Override
    public void onCommand(Member author, TextChannel channel, String[] args) {
        if (args.length != 0) {
            EmbedUtils.error(channel, author, "Usage: /reset");
            return;
        }

        subredditManager.getLinkFromChannel(channel).ifPresentOrElse(link -> {
            if (timeouts.containsKey(link) && timeouts.get(link) > System.currentTimeMillis()) {
                EmbedUtils.error(channel, author, "You can only run this command once every 30 seconds per subreddit");
                return;
            }

            timeouts.put(link, System.currentTimeMillis() + 30_000);

            discordManager.getCommandManager().getCommand(NextCommand.class).ifPresent(nextCommand -> nextCommand.reset(link));

            EmbedUtils.sendEmbed(channel, author, "Reset posts", "Reset the post sorting. The next /next command will start over from a refreshed list.");
        }, () -> EmbedUtils.error(channel, author, "The /reset command must be ran in a subreddit channel"));
    }
}
