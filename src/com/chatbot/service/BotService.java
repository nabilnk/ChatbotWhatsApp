package com.chatbot.service;

import com.chatbot.config.KoneksiDB;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BotService {

    private static final String FONNTE_TOKEN = "KtFK4KH6GiNXKwjMBiB8";

    // --- METHOD UNTUK INTERAKSI NYATA DENGAN FONNTE ---

    /**
     * Mengirim satu pesan ke target melalui Fonnte.
     * Hanya digunakan untuk broadcast nyata.
     */
    // Di dalam class BotService.java

private void sendRealMessage(String target, String message) {
    System.out.println("\n--- [sendRealMessage] DIPANGGIL ---");
    System.out.println("    TARGET: " + target);
    System.out.println("    MESSAGE: " + message);
    
    if (FONNTE_TOKEN == null || FONNTE_TOKEN.isEmpty()) {
        System.err.println("    GAGAL: FONNTE_TOKEN kosong.");
        System.out.println("---------------------------------");
        return;
    }
    
    // Periksa apakah target atau message null/kosong
    if (target == null || target.trim().isEmpty() || message == null || message.trim().isEmpty()) {
        System.err.println("    GAGAL: Target atau Message kosong/null.");
        System.out.println("---------------------------------");
        return;
    }

    saveMessageToHistory(target, message, "keluar");
    System.out.println("    Berhasil simpan ke history.");
    
    try {
        URL url = new URL("https://api.fonnte.com/send");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", FONNTE_TOKEN);
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setDoOutput(true);
        
        String safeMessage = message.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
        String jsonPayload = String.format("{\"target\":\"%s\", \"message\":\"%s\"}", target, safeMessage);
        
        System.out.println("    PAYLOAD JSON DIKIRIM: " + jsonPayload);
        
        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonPayload.getBytes("utf-8"));
        }
        
        int responseCode = conn.getResponseCode();
        System.out.println("    Fonnte Response Code: " + responseCode);
        
        // Baca response body
        StringBuilder responseBody = new StringBuilder();
        // Pilih stream yang benar berdasarkan response code
        java.io.InputStream stream = (responseCode == 200) ? conn.getInputStream() : conn.getErrorStream();
        if (stream != null) {
            try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(stream))) {
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    responseBody.append(responseLine.trim());
                }
            }
            System.out.println("    Fonnte Response Body: " + responseBody.toString());
        }
        
        conn.disconnect();
    } catch (Exception e) {
        System.err.println("    EXCEPTION TERJADI: " + e.getMessage());
        e.printStackTrace();
    }
    System.out.println("--- [sendRealMessage] SELESAI ---");
}

    /**
     * Menjalankan proses broadcast nyata ke semua member.
     */
    public void sendBroadcast(String message) {
        System.out.println("--- [BROADCAST] Memulai proses ---");
        List<String> members = getAllMemberNumbers();
        if (members.isEmpty()) {
            System.out.println("--- [BROADCAST] Tidak ada member ditemukan. ---");
            return;
        }
        for (String memberNumber : members) {
            System.out.println("    Mengirim broadcast ke: " + memberNumber);
            sendRealMessage(memberNumber, message);
            try { Thread.sleep(1500); } catch (InterruptedException e) {}
        }
        System.out.println("--- [BROADCAST] Proses selesai ---");
    }

    // --- METHOD UNTUK LOGIKA SIMULASI (100% LOKAL) ---

    /**
     * Memproses pesan masuk secara lokal dan mengembalikan balasan dalam bentuk String.
     * TIDAK mengirim pesan apa pun ke Fonnte.
     * @return String berisi balasan dari bot.
     */
    // Di dalam class BotService.java

public String processAndReplyMessage(String sender, String message) {
    System.out.println("--- [PROSES PESAN] Memulai ---");
    saveMessageToHistory(sender, message, "masuk");

    if (!isMemberRegistered(sender)) {
        String reply = "Maaf, nomor Anda " + sender + " belum terdaftar.";
        System.out.println("    Nomor tidak terdaftar. Mengirim balasan penolakan.");
        sendRealMessage(sender, reply);
        return reply;
    }

    String keyword = message.toLowerCase().trim();
    String replyMessageFromDB = getReplyForKeyword(keyword); // Kita tetap ambil dari DB untuk log
    System.out.println("    Jawaban dari DB: '" + replyMessageFromDB + "'");
    
    // ==========================================================
    // === BAGIAN DEBUGGING: KIRIM PESAN STATIS YANG PASTI BISA ===
    // ==========================================================
    String hardcodedReply = "tes balasan dari simulasi"; // Pesan sederhana tanpa karakter aneh
    System.out.println("    MENGIRIM PESAN HARDCODED: '" + hardcodedReply + "'");
    sendRealMessage(sender, hardcodedReply); // KIRIM PESAN INI, BUKAN DARI DB
    // ==========================================================

    return replyMessageFromDB; // Kita tetap kembalikan pesan asli untuk ditampilkan di log GUI
}

    // --- HELPER METHOD (TETAP SAMA) ---

    private List<String> getAllMemberNumbers() {
        List<String> numbers = new ArrayList<>();
        String sql = "SELECT nomor_wa FROM members";
        try (Connection conn = KoneksiDB.getKoneksi(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) { numbers.add(rs.getString("nomor_wa")); }
        } catch (SQLException e) { e.printStackTrace(); }
        return numbers;
    }

    private boolean isMemberRegistered(String number) {
        String sql = "SELECT COUNT(*) FROM members WHERE nomor_wa = ?";
        try (Connection conn = KoneksiDB.getKoneksi(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, number);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getInt(1) > 0; }
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    private String getReplyForKeyword(String keyword) {
        String sql = "SELECT jawaban FROM keywords WHERE keyword = ?";
        String defaultReply = "Maaf, keyword tidak ditemukan.";
        try (Connection conn = KoneksiDB.getKoneksi(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, keyword);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getString("jawaban"); }
        } catch (SQLException e) { e.printStackTrace(); }
        return defaultReply;
    }

    private void saveMessageToHistory(String number, String message, String type) {
        String sql = "INSERT INTO message_history (nomor_wa, pesan, tipe) VALUES (?, ?, ?)";
        try (Connection conn = KoneksiDB.getKoneksi(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, number); ps.setString(2, message); ps.setString(3, type); ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }
}