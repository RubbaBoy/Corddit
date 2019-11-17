package com.uddernetworks.corddit.user.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.uddernetworks.corddit.Corddit;
import com.uddernetworks.corddit.config.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.uddernetworks.corddit.config.Config.REDIRECTURL;

public class WebCallback {

    private final Corddit corddit;
    private final ConfigManager configManager;
    private static final Logger LOGGER = LoggerFactory.getLogger(WebCallback.class);

    private String base;
    private Map<String, Consumer<String>> callbackMap = new ConcurrentHashMap<>();

    public WebCallback(Corddit corddit, ConfigManager configManager) {
        this.corddit = corddit;
        this.configManager = configManager;

        base = configManager.get(REDIRECTURL);
        base = base.substring(0, base.length() - "/corddit".length());
    }

    public void start() {
        try {
            var server = HttpServer.create(new InetSocketAddress(8000), 0);
            server.createContext("/corddit", new MyHandler(server, requested -> {
                var query = getQuery(base, requested);
                if (!query.containsKey("state")) return;
                var state = query.get("state");
                if (!callbackMap.containsKey(state)) return;
                callbackMap.get(state).accept(requested);
            }));
            server.setExecutor(null);
            server.start();
        } catch (IOException e) {
            LOGGER.error("Error during server creation. Account linking will NOT work.", e);
        }
    }

    public CompletableFuture<String> listenForState(String state) {
        var completableFuture = new CompletableFuture<String>();
        callbackMap.put(state, completableFuture::complete);
        return completableFuture;
    }

    public void listenForState(String state, Consumer<String> callback) {
        callbackMap.put(state, callback);
    }

    public void clearStateListen(String state) {
        callbackMap.remove(state);
    }

    public static Map<String, String> getQuery(String requested) {
        return getQuery("", requested);
    }

    public static Map<String, String> getQuery(String base, String requested) {
        try {
            var url = new URL(base + requested);
            return Arrays.stream(url.getQuery().split("&")).map(kv -> kv.split("=", 2)).collect(Collectors.toMap(arr -> arr[0], arr -> arr[1]));
        } catch (MalformedURLException e) {
            LOGGER.error("Invalid URL", e);
            return Collections.emptyMap();
        }
    }

    static class MyHandler implements HttpHandler {

        private static final Logger LOGGER = LoggerFactory.getLogger(MyHandler.class);

        private HttpServer httpServer;
        private final Consumer<String> callback;

        public MyHandler(HttpServer httpServer, Consumer<String> callback) {
            this.httpServer = httpServer;
            this.callback = callback;
        }

        @Override
        public void handle(HttpExchange exchange) {
            try {
                String response = "Completed";
                exchange.sendResponseHeaders(200, response.length());
                var os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();

                callback.accept(exchange.getRequestURI().toString());
            } catch (IOException e) {
                LOGGER.error("Error during response", e);
            }
        }
    }

}
