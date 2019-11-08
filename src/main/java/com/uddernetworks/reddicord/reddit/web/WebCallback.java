package com.uddernetworks.reddicord.reddit.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.Consumer;

public class WebCallback {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebCallback.class);

    public static void listenFor(Consumer<String> callback) throws IOException {
        var server = HttpServer.create(new InetSocketAddress(8000), 0);
        server.createContext("/reddicord", new MyHandler(server, callback));
        server.setExecutor(null);
        server.start();
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
//                var body = IOUtils.toString(exchange.getRequestBody(), StandardCharsets.UTF_8);
                os.write(response.getBytes());
                os.close();

                callback.accept("http://73.218.245.138:8000/reddicord" + exchange.getRequestURI().toString());
                httpServer.stop(1000);
            } catch (Exception e) {
                LOGGER.error("Error during response!", e);
            }
        }
    }

}
