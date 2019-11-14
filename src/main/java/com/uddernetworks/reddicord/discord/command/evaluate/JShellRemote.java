package com.uddernetworks.reddicord.discord.command.evaluate;

import com.uddernetworks.reddicord.Reddicord;
import com.uddernetworks.reddicord.discord.EmbedUtils;
import jdk.jshell.JShell;
import jdk.jshell.Snippet;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class JShellRemote {

    private JShell shell;

    public static Reddicord reddicord;
    public static JDA jda;

    public static Guild guild;
    public static TextChannel channel;

    public JShellRemote(Reddicord reddicord) {

        JShellRemote.reddicord = reddicord;
        jda = reddicord.getDiscordManager().getJDA();

        shell = JShell.builder().executionEngine("local").build();
        shell.eval("import java.lang.*;");
        shell.eval("import java.util.*;");
        shell.eval("import net.dv8tion.jda.api.entities.*;");
        shell.eval("import com.uddernetworks.reddicord.*;");
        shell.eval("import com.uddernetworks.reddicord.discord.*;");
        shell.eval("import com.uddernetworks.reddicord.reddit.*;");
        shell.eval("import com.uddernetworks.reddicord.database.*;");
        shell.eval("import com.uddernetworks.reddicord.config.*;");
        shell.eval("import com.uddernetworks.reddicord.discord.command.evaluate.JShellRemote;");
        shell.eval("import static com.uddernetworks.reddicord.discord.command.evaluate.JShellRemote.reddicord;");
        shell.eval("import static com.uddernetworks.reddicord.discord.command.evaluate.JShellRemote.jda;");
        shell.eval("import static com.uddernetworks.reddicord.discord.command.evaluate.JShellRemote.guild;");
        shell.eval("import static com.uddernetworks.reddicord.discord.command.evaluate.JShellRemote.channel;");
        shell.eval("import static com.uddernetworks.reddicord.discord.command.evaluate.JShellRemote.respond;");
    }

    public static CompletableFuture<Optional<Message>> respond(Object response) {
        var string = String.valueOf(response);
        if (string.isBlank()) return CompletableFuture.completedFuture(Optional.empty());
        var completableFuture = new CompletableFuture<Optional<Message>>();
        channel.sendMessage(string).queue(message -> completableFuture.complete(Optional.of(message)), completableFuture::completeExceptionally);
        return completableFuture;
    }

    public void runCode(TextChannel channel, Member author, String code) {
        JShellRemote.guild = channel.getGuild();
        JShellRemote.channel = channel;

        shell.eval(code).forEach(sne -> {
            System.out.println("=] \t\t " + sne.toString());
            var exception = sne.exception();
            if (exception != null) {
                EmbedUtils.error(channel, author, ExceptionUtils.getStackTrace(exception));
            } else if (sne.status() == Snippet.Status.REJECTED) {
                EmbedUtils.error(channel, author, "Rejected");
            }
        });

    }

}
