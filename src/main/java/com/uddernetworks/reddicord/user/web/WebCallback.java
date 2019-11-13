package com.uddernetworks.reddicord.user.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class WebCallback {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebCallback.class);

    public static CompletableFuture<String> listenFor() {
        var future = new CompletableFuture<String>();
        try {
            var server = HttpServer.create(new InetSocketAddress(8000), 0);
            server.createContext("/reddicord", new MyHandler(server, future::complete));
            server.setExecutor(null);
            server.start();
        } catch (IOException e) {
            LOGGER.error("Error during server creation", e);
            future.completeExceptionally(e);
        }
        return future;
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
                httpServer.stop(1000);
            } catch (IOException e) {
                LOGGER.error("Error during response", e);
            }
        }
    }

}
