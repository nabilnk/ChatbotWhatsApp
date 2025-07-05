package com.chatbot.service;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class WebhookServer {
    private final int port;
    private final BotService botService;

    public WebhookServer(int port, BotService botService) {
        this.port = port;
        this.botService = botService;
    }

    public void start() throws IOException {
        // Buat server HTTP di port yang kamu tentukan (misal 8090)
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        // Pasang handler untuk endpoint "/webhook"
        server.createContext("/webhook", new WebhookHandler(botService));

        // Gunakan thread pool agar server responsif terhadap banyak request
        server.setExecutor(Executors.newCachedThreadPool());

        // Jalankan server
        server.start();

        System.out.println(">>> Webhook Server berjalan di http://localhost:" + port + "/webhook");
    }
}
