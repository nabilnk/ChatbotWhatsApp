package com.chatbot.service;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.json.JSONObject;

public class WebhookHandler implements HttpHandler {

    private final BotService botService;

    // Daftar nomor WA bot sendiri yang harus diabaikan (bisa lebih dari satu)
    private final Set<String> ignoredSenders = Set.of("6282328591635");

    public WebhookHandler(BotService botService) {
        this.botService = botService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String response;
        int statusCode;

        System.out.println(">> Webhook dipanggil dengan metode: " + method);

        if ("GET".equalsIgnoreCase(method)) {
            // GET digunakan hanya untuk cek koneksi webhook
            response = "Webhook Aktif (GET OK)";
            statusCode = 200;
        } else if ("POST".equalsIgnoreCase(method)) {
            try {
                InputStream is = exchange.getRequestBody();
                String requestBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                System.out.println("--- [WEBHOOK DITERIMA] ---");
                System.out.println("Body: " + requestBody);

                JSONObject json = new JSONObject(requestBody);
                String sender = json.optString("sender", null);
                String message = json.optString("message", null);
                boolean isQuick = json.optBoolean("quick", false);

                // Abaikan jika quick (balasan dari bot) atau sender adalah nomor bot sendiri
                if (isQuick) {
                    System.out.println(">> Abaikan pesan quick (dari bot sendiri).");
                    response = "Ignored quick message";
                    statusCode = 200;
                } else if (ignoredSenders.contains(sender)) {
                    System.out.println(">> Abaikan pesan dari nomor bot.");
                    response = "Ignored sender";
                    statusCode = 200;
                } else if (sender != null && message != null) {
                    System.out.println("Pengirim: " + sender);
                    System.out.println("Pesan: " + message);

                    // Proses dalam thread terpisah
                    new Thread(() -> botService.processAndReplyMessage(sender, message)).start();

                    response = "OK";
                    statusCode = 200;
                } else {
                    response = "Bad Request";
                    statusCode = 400;
                }

            } catch (Exception e) {
                System.err.println("Error di Webhook Handler: " + e.getMessage());
                e.printStackTrace();
                response = "Internal Server Error";
                statusCode = 500;
            }

        } else {
            // Method lain ditolak
            response = "Method Not Allowed";
            statusCode = 405;
        }

        // Kirim response ke Fonnte
        exchange.sendResponseHeaders(statusCode, response.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }
}
