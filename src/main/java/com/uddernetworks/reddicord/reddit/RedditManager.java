package com.uddernetworks.reddicord.reddit;

import com.electronwill.nightconfig.core.file.FileConfig;
import com.uddernetworks.reddicord.Reddicord;
import com.uddernetworks.reddicord.reddit.web.WebCallback;
import net.dean.jraw.http.OkHttpNetworkAdapter;
import net.dean.jraw.http.UserAgent;
import net.dean.jraw.oauth.Credentials;
import net.dean.jraw.oauth.NoopTokenStore;
import net.dean.jraw.oauth.OAuthHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class RedditManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedditManager.class);

    private Reddicord reddicord;
    private FileConfig fileConfig;

    public RedditManager(Reddicord reddicord) {
        this.reddicord = reddicord;
        this.fileConfig = reddicord.getConfigManager().getConfig();
    }

    public void init() throws URISyntaxException, IOException {
        var oauthCreds = Credentials.webapp(fileConfig.get("reddit.clientId"), fileConfig.get("reddit.clientSecret"), fileConfig.get("reddit.redirectURL"));

        var userAgent = new UserAgent("bot", "com.uddernetworks.reddicord", "1.0.0", "OnlyTwo_jpg");

        var networkAdapter = new OkHttpNetworkAdapter(userAgent);
        var store = new NoopTokenStore();
        var helper = OAuthHelper.interactive(networkAdapter, oauthCreds, store);

        String authUrl = helper.getAuthorizationUrl(true, false, "read", "vote", "identity", "account", "save", "history");

        WebCallback.listenFor(body -> {
            var reddit = helper.onUserChallenge(body);

            var me = reddit.me().query().getAccount();
            if (me == null) {
                LOGGER.error("Me is null!");
                return;
            }

            LOGGER.info("Hello {}!", me.getName());
        });

        var desktop = Desktop.getDesktop();
        desktop.browse(new URI(authUrl));
    }

}
