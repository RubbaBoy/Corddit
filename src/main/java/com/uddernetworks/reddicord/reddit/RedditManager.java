package com.uddernetworks.reddicord.reddit;

import com.uddernetworks.reddicord.Reddicord;
import com.uddernetworks.reddicord.config.ConfigManager;
import net.dean.jraw.RedditClient;
import net.dean.jraw.http.OkHttpNetworkAdapter;
import net.dean.jraw.http.UserAgent;
import net.dean.jraw.oauth.Credentials;
import net.dean.jraw.oauth.JsonFileTokenStore;
import net.dean.jraw.oauth.OAuthHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import static com.uddernetworks.reddicord.config.Config.CLIENTID;
import static com.uddernetworks.reddicord.config.Config.CLIENTSECRET;
import static com.uddernetworks.reddicord.config.Config.REDIRECTURL;

public class RedditManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedditManager.class);

    private final Reddicord reddicord;
    private final ConfigManager configManager;

    public RedditManager(Reddicord reddicord) {
        this.reddicord = reddicord;
        this.configManager = reddicord.getConfigManager();
    }

    // TODO: This will be refactored completely to allow for dynamic everything. This is a proof-of-concept currently
    public void init(File tokenStore) throws URISyntaxException, IOException {
        var oauthCreds = Credentials.webapp(configManager.get(CLIENTID), configManager.get(CLIENTSECRET), configManager.get(REDIRECTURL));

        var userAgent = new UserAgent("bot", "com.uddernetworks.reddicord", "1.0.0", "Reddicord");

        var networkAdapter = new OkHttpNetworkAdapter(userAgent);
        var store = new JsonFileTokenStore(tokenStore);
        store.load();
        store.setAutoPersist(true);
        var helper = OAuthHelper.interactive(networkAdapter, oauthCreds, store);
        System.out.println("USERS:");
        System.out.println(store.getUsernames());
        var reddit = new RedditClient(networkAdapter, store.fetchLatest("OnlyTwo_jpg"), oauthCreds, store, "OnlyTwo_jpg");
//        var helper = OAuthHelper.automatic(networkAdapter, oauthCreds, store);

//        String authUrl = helper.getAuthorizationUrl(true, false, "read", "vote", "identity", "account", "save", "history");

//        WebCallback.listenFor(body -> {
//            var reddit = helper.onUserChallenge(body);
//
//            var me = reddit.me().query().getAccount();
//            if (me == null) {
//                LOGGER.error("Me is null!");
//                return;
//            }
//
//            LOGGER.info("Hello {}!", me.getName());
//            var jda = reddicord.getDiscordManager().getJDA();
//            jda.getGuildById(642549950361632778L).getTextChannelById(642549950361632781L)
//                    .sendMessage("Hello " + me.getName()).queue();
//        });
//
//        var desktop = Desktop.getDesktop();
//        desktop.browse(new URI(authUrl));


        var me = reddit.me().query().getAccount();
        System.out.println(me);
        System.out.println(store.getUsernames());
        var jda = reddicord.getDiscordManager().getJDA();
        jda.getGuildById(642549950361632778L).getTextChannelById(642549950361632781L)
                .sendMessage("Hello " + me.getName() + "\nYour karma is:\n\t" + me.getCommentKarma() + " Comment\n\t" + me.getLinkKarma() + " Post").queue();
    }

}
