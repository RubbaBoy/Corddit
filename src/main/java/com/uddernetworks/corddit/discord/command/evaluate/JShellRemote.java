package com.uddernetworks.corddit.discord.command.evaluate;

import com.uddernetworks.corddit.Corddit;
import com.uddernetworks.corddit.config.ConfigManager;
import com.uddernetworks.corddit.database.DatabaseManager;
import com.uddernetworks.corddit.discord.DiscordManager;
import com.uddernetworks.corddit.discord.DiscordStateManager;
import com.uddernetworks.corddit.discord.EmbedUtils;
import com.uddernetworks.corddit.discord.corddit.SubredditManager;
import com.uddernetworks.corddit.reddit.RedditManager;
import com.uddernetworks.corddit.user.UserManager;
import com.uddernetworks.corddit.user.web.WebCallback;
import jdk.jshell.JShell;
import jdk.jshell.Snippet;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public class JShellRemote {

    private static final Logger LOGGER = LoggerFactory.getLogger(JShellRemote.class);

    private JShell shell;

    // These are ONLY to be used by JShell, they should not be accessed anywhere in the project
    // they allow to interactively call the managers from the /evaluate command
    public static Corddit corddit;
    public static JDA jda;
    public static ConfigManager configManager;
    public static DatabaseManager databaseManager;
    public static RedditManager redditManager;
    public static DiscordManager discordManager;
    public static UserManager userManager;
    public static SubredditManager subredditManager;
    public static DiscordStateManager discordStateManager;
    public static WebCallback webCallback;

    public static Guild guild;
    public static TextChannel channel;

    private Field key;
    private Method keyName;

    public JShellRemote(Corddit corddit) {
        try {
            key = Snippet.class.getDeclaredField("key");
            key.setAccessible(true);

            keyName = Class.forName("jdk.jshell.Key$PersistentKey").getDeclaredMethod("name");
            keyName.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            LOGGER.error("Error while getting key and name()", e);
        }

        JShellRemote.corddit = corddit;
        configManager = corddit.getConfigManager();
        databaseManager = corddit.getDatabaseManager();
        redditManager = corddit.getRedditManager();
        discordManager = corddit.getDiscordManager();
        userManager = corddit.getUserManager();
        subredditManager = corddit.getSubredditManager();
        discordStateManager  = corddit.getDiscordStateManager();
        webCallback = corddit.getWebCallback();
        jda = corddit.getDiscordManager().getJDA();

        shell = JShell.builder().executionEngine("local").build();
        shell.eval("import java.lang.*;");
        shell.eval("import java.util.*;");
        shell.eval("import net.dv8tion.jda.api.entities.*;");
        shell.eval("import com.uddernetworks.corddit.*;");
        shell.eval("import com.uddernetworks.corddit.discord.*;");
        shell.eval("import com.uddernetworks.corddit.reddit.*;");
        shell.eval("import com.uddernetworks.corddit.database.*;");
        shell.eval("import com.uddernetworks.corddit.config.*;");
        shell.eval("import com.uddernetworks.corddit.discord.command.evaluate.JShellRemote;");
        shell.eval("import static com.uddernetworks.corddit.discord.command.evaluate.JShellRemote.corddit;");
        shell.eval("import static com.uddernetworks.corddit.discord.command.evaluate.JShellRemote.jda;");
        shell.eval("import static com.uddernetworks.corddit.discord.command.evaluate.JShellRemote.guild;");
        shell.eval("import static com.uddernetworks.corddit.discord.command.evaluate.JShellRemote.channel;");
        shell.eval("import static com.uddernetworks.corddit.discord.command.evaluate.JShellRemote.respond;");
        shell.eval("import static com.uddernetworks.corddit.discord.command.evaluate.JShellRemote.configManager;");
        shell.eval("import static com.uddernetworks.corddit.discord.command.evaluate.JShellRemote.databaseManager;");
        shell.eval("import static com.uddernetworks.corddit.discord.command.evaluate.JShellRemote.redditManager;");
        shell.eval("import static com.uddernetworks.corddit.discord.command.evaluate.JShellRemote.discordManager;");
        shell.eval("import static com.uddernetworks.corddit.discord.command.evaluate.JShellRemote.userManager;");
        shell.eval("import static com.uddernetworks.corddit.discord.command.evaluate.JShellRemote.subredditManager;");
        shell.eval("import static com.uddernetworks.corddit.discord.command.evaluate.JShellRemote.discordStateManager;");
        shell.eval("import static com.uddernetworks.corddit.discord.command.evaluate.JShellRemote.webCallback;");
        shell.eval("import static com.uddernetworks.corddit.discord.command.evaluate.JShellRemote.purge;");
    }

    public static CompletableFuture<Optional<Message>> respond(Object response) {
        var string = String.valueOf(response);
        if (string.isBlank()) return CompletableFuture.completedFuture(Optional.empty());
        var completableFuture = new CompletableFuture<Optional<Message>>();
        channel.sendMessage(string).queue(message -> completableFuture.complete(Optional.of(message)), completableFuture::completeExceptionally);
        return completableFuture;
    }

    public static void purge() {
        purge(50);
    }

    public static void purge(int amount) {
        var messages = channel.getHistory().retrievePast(amount).complete();
        channel.deleteMessages(messages).queue();
    }

    public void runCode(TextChannel channel, Member author, String code) {
        JShellRemote.guild = channel.getGuild();
        JShellRemote.channel = channel;

        shell.eval(code).forEach(sne -> {
            System.out.println("=] \t\t " + sne.toString());
            var exception = sne.exception();
            var value = sne.value();
            if (exception != null) {
                EmbedUtils.error(channel, author, ExceptionUtils.getStackTrace(exception));
            } else if (sne.status() == Snippet.Status.REJECTED) {
                EmbedUtils.error(channel, author, "Rejected");
            } else if (value != null && !value.isBlank()) {
                var snippet = sne.snippet();
                if (snippet.kind() == Snippet.Kind.VAR) {
                    var title = new AtomicReference<>("<unknown>");
                    getKey(snippet).ifPresent(name -> title.set("**" + name + "** ="));
                    EmbedUtils.sendEmbed(channel, author, "Evaluated expression", embed -> embed.addField(title.get(), value, false));
                }
            }
        });
    }

    private Optional<String> getKey(Snippet snippet) {
        try {
            var key = this.key.get(snippet);
            if (key == null) return Optional.empty();
            return Optional.ofNullable((String) keyName.invoke(key));
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

}
