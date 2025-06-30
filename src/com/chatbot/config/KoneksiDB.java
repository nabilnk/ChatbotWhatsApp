package com.chatbot.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import javax.swing.JOptionPane;

public class KoneksiDB {

    // Hapus variabel statis 'koneksi'
    // private static Connection koneksi;

    // Ubah method getKoneksi agar selalu membuat koneksi baru
    public static Connection getKoneksi() {
        Connection koneksiBaru = null; // Buat variabel lokal
        try {
            String url = "jdbc:mysql://localhost:3306/db_chatbot_wa";
            String user = "root";
            String password = "";

            // Tidak perlu lagi DriverManager.registerDriver() di versi JDBC modern
            // DriverManager.registerDriver(new com.mysql.cj.jdbc.Driver());
            
            koneksiBaru = DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error Koneksi Database: " + e.getMessage());
            // Keluar dari aplikasi jika koneksi database gagal total
            System.exit(0); 
        }
        return koneksiBaru; // Kembalikan koneksi yang baru dibuat
    }
}