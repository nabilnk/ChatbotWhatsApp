package com.chatbot.gui;

import com.chatbot.config.KoneksiDB;
import com.chatbot.service.BotService;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import com.chatbot.service.WebhookServer;

public class MainFrame extends JFrame {

    private final BotService botService;

    // Komponen GUI
    private JTextField txtNomorHp, txtUsername, txtBroadcast, txtSimSender, txtSimMessage;
    private JTable tblDataMember;
    private JTextArea areaHistory;
    private JTextArea txtBroadcastArea;
    private DefaultTableModel modelMember;
    private int selectedMemberId = -1;

    public MainFrame() {
        this.botService = new BotService();

        setTitle("WhatsApp Bot Control Panel");
        setSize(920, 740); // Ukuran disesuaikan untuk layout baru
        setMinimumSize(new Dimension(850, 650));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) { e.printStackTrace(); }
        
        initComponents();
        loadMemberData();
        loadHistoryData();
    }

    private void initComponents() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.BOTH;
    
        // --- Kolom Kiri ---
        // Panel Kelola Member (Baris 0)
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.weightx = 0.4; gbc.weighty = 0.1;
        gbc.anchor = GridBagConstraints.NORTH;
        add(createKelolaMemberPanel(), gbc);
    
        // Panel Broadcast (Baris 1)
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.weighty = 0.3; // Beri ruang lebih untuk area teks broadcast
        gbc.anchor = GridBagConstraints.CENTER;
        add(createBroadcastPanel(), gbc);
    
        // --- [FIX] Panel Simulasi Ditambahkan di sini (Baris 2) ---
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.weighty = 0.0; // Tidak perlu membesar secara vertikal
        gbc.anchor = GridBagConstraints.SOUTH; // Letakkan di bagian bawah selnya
        add(createSimulasiPanel(), gbc);
        
        // --- Kolom Kanan ---
        // Panel Tabel Member (Membentang 3 baris di kanan)
        gbc.gridx = 1; gbc.gridy = 0;
        gbc.gridheight = 3; // --- [FIX] Dibuat membentang 3 baris agar sejajar dengan 3 panel di kiri
        gbc.weightx = 0.6; gbc.weighty = 0.4;
        gbc.anchor = GridBagConstraints.CENTER;
        add(createDataMemberPanel(), gbc);
    
        // --- Panel Bawah (Lebar) ---
        // Panel History
        gbc.gridx = 0; gbc.gridy = 3; // --- [FIX] Pindah ke baris 3
        gbc.gridheight = 1; // Reset gridheight
        gbc.gridwidth = 2;  // Membentang 2 kolom
        gbc.weightx = 1.0; gbc.weighty = 0.6; // Panel history mengambil sisa ruang vertikal
        add(createHistoryPanel(), gbc);
    
        // Panel Tombol Aksi
        gbc.gridx = 0; gbc.gridy = 4; // --- [FIX] Pindah ke baris 4
        gbc.weighty = 0.0; // Tinggi tetap
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(createBottomActionPanel(), gbc);
    }
    
    // --- METHOD UNTUK MEMBUAT SETIAP PANEL ---

    private JPanel createKelolaMemberPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new TitledBorder(" Kelola Member "));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 8, 5, 8);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0; panel.add(new JLabel("Nomor HP:"), gbc);
        gbc.gridy = 1; panel.add(new JLabel("Username:"), gbc);
        
        gbc.gridx = 1; gbc.gridy = 0; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        txtNomorHp = new JTextField(); panel.add(txtNomorHp, gbc);
        gbc.gridy = 1; txtUsername = new JTextField(); panel.add(txtUsername, gbc);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        JButton btnTambah = new JButton("Tambah");
        JButton btnUpdate = new JButton("Update");
        JButton btnHapus = new JButton("Hapus");
        JButton btnClean = new JButton("Bersihkan");
        buttonPanel.add(btnTambah);
        buttonPanel.add(btnUpdate);
        buttonPanel.add(btnHapus);
        buttonPanel.add(btnClean);
        
        btnTambah.addActionListener(e -> addMemberAction());
        btnUpdate.addActionListener(e -> editMemberAction());
        btnHapus.addActionListener(e -> deleteMemberAction());
        btnClean.addActionListener(e -> cleanFieldsAction());

        gbc.gridy = 2; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(10, 8, 5, 8);
        panel.add(buttonPanel, gbc);
        
         gbc.gridy = 3; gbc.weighty = 1.0;
         panel.add(new JLabel(), gbc);
    
        return panel;
    }

    private JPanel createDataMemberPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(new TitledBorder(" Data Member Terdaftar "));
        modelMember = new DefaultTableModel(new String[]{"ID", "Nomor HP", "Username"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tblDataMember = new JTable(modelMember);
        tblDataMember.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { tableRowClickedAction(); }
        });
        panel.add(new JScrollPane(tblDataMember), BorderLayout.CENTER);
        return panel;
    }
    
    private JPanel createBroadcastPanel() {
    JPanel panel = new JPanel(new BorderLayout(5, 5));
    panel.setBorder(new TitledBorder(" Broadcast Pesan "));

    // Gunakan JTextArea untuk input pesan yang bisa multi-baris
    txtBroadcastArea = new JTextArea();
    txtBroadcastArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
    txtBroadcastArea.setLineWrap(true);
    txtBroadcastArea.setWrapStyleWord(true);
    
    // Tombol Kirim
    JButton btnBroadcast = new JButton("Kirim ke Semua Member");
    btnBroadcast.setFont(new Font("Segoe UI", Font.BOLD, 12));
    btnBroadcast.addActionListener(e -> broadcastAction());

    // Panel untuk meletakkan tombol di tengah
    JPanel buttonContainer = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 5));
    buttonContainer.add(btnBroadcast);

    // Tambahkan komponen ke panel utama
    panel.add(new JScrollPane(txtBroadcastArea), BorderLayout.CENTER); // Area teks di tengah (mengisi ruang)
    panel.add(buttonContainer, BorderLayout.SOUTH); // Tombol di bawah

    return panel;
}

    
    private JPanel createSimulasiPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(new TitledBorder(" Simulasi Balasan Otomatis "));
        JPanel innerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        txtSimSender = new JTextField("628...", 12);
        txtSimMessage = new JTextField("halo", 10);
        JButton btnSimulate = new JButton("Kirim & Balas");
        btnSimulate.addActionListener(e -> simulateAction());
        innerPanel.add(new JLabel("Dari:")); innerPanel.add(txtSimSender);
        innerPanel.add(new JLabel("Pesan:")); innerPanel.add(txtSimMessage);
        innerPanel.add(btnSimulate);
        panel.add(innerPanel, BorderLayout.CENTER);
        return panel;
    }
    
    private JPanel createHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new TitledBorder(" Riwayat Pesan "));
        areaHistory = new JTextArea();
        areaHistory.setEditable(false);
        areaHistory.setFont(new Font("Monospaced", Font.PLAIN, 12));
        panel.add(new JScrollPane(areaHistory), BorderLayout.CENTER);
        return panel;
    }
    
    private JPanel createBottomActionPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        panel.setBorder(new EmptyBorder(0, 5, 0, 5));
        JButton btnHapusHistory = new JButton("Hapus History");
        JButton btnKelolaKeyword = new JButton("Kelola Keyword");
        JButton btnRefresh = new JButton("Refresh Data");
        JButton btnKeluar = new JButton("Keluar");
        btnHapusHistory.addActionListener(e -> deleteAllHistoryAction());
        btnKelolaKeyword.addActionListener(e -> new KeywordFrame().setVisible(true));
        btnRefresh.addActionListener(e -> { loadMemberData(); loadHistoryData(); });
        btnKeluar.addActionListener(e -> System.exit(0));
        panel.add(btnHapusHistory);
        panel.add(btnKelolaKeyword);
        panel.add(btnRefresh);
        panel.add(btnKeluar);
        return panel;
    }

    // --- ACTIONS & DATA LOADERS ---
    
    private void loadMemberData() {
        modelMember.setRowCount(0);
        try (Statement stmt = KoneksiDB.getKoneksi().createStatement(); ResultSet rs = stmt.executeQuery("SELECT id_member, nomor_wa, nama FROM members ORDER BY nama ASC")) {
            while (rs.next()) modelMember.addRow(new Object[]{rs.getInt("id_member"), rs.getString("nomor_wa"), rs.getString("nama")});
        } catch (SQLException e) { JOptionPane.showMessageDialog(this, "Error memuat member: " + e.getMessage()); }
    }

    private void loadHistoryData() {
        areaHistory.setText("");
        StringBuilder historyText = new StringBuilder();
        try (Statement stmt = KoneksiDB.getKoneksi().createStatement(); ResultSet rs = stmt.executeQuery("SELECT nomor_wa, pesan, tipe, timestamp FROM message_history ORDER BY id_history DESC LIMIT 100")) {
            while (rs.next()) {
                String prefix = "masuk".equals(rs.getString("tipe")) ? ">> " : "<< ";
                historyText.append(rs.getTimestamp("timestamp")).append(" | ").append(prefix)
                           .append(rs.getString("nomor_wa")).append(" : ").append(rs.getString("pesan")).append("\n");
            }
            areaHistory.setText(historyText.toString());
        } catch (SQLException e) { JOptionPane.showMessageDialog(this, "Error memuat history: " + e.getMessage()); }
    }
    
    private void cleanFieldsAction() {
        txtNomorHp.setText("");
        txtUsername.setText("");
        tblDataMember.clearSelection();
        selectedMemberId = -1;
    }
    
    private void tableRowClickedAction() {
        int selectedRow = tblDataMember.getSelectedRow();
        if (selectedRow != -1) {
            selectedMemberId = (int) modelMember.getValueAt(selectedRow, 0);
            String nomor = (String) modelMember.getValueAt(selectedRow, 1);
            String nama = (String) modelMember.getValueAt(selectedRow, 2);
            txtNomorHp.setText(nomor);
            txtUsername.setText(nama);
            txtSimSender.setText(nomor); // Auto-fill nomor simulasi
        }
    }
    
    private void addMemberAction() {
        String nomor = txtNomorHp.getText().trim(); String nama = txtUsername.getText().trim();
        if (nomor.isEmpty() || nama.isEmpty()) { JOptionPane.showMessageDialog(this, "Nomor HP dan Username tidak boleh kosong!"); return; }
        try (PreparedStatement ps = KoneksiDB.getKoneksi().prepareStatement("INSERT INTO members (nomor_wa, nama) VALUES (?,?)")) {
            ps.setString(1, nomor); ps.setString(2, nama); ps.executeUpdate();
            loadMemberData(); cleanFieldsAction();
        } catch (SQLException e) { JOptionPane.showMessageDialog(this, "Gagal mendaftar: " + e.getMessage()); }
    }

    private void editMemberAction() {
        if (selectedMemberId == -1) { JOptionPane.showMessageDialog(this, "Pilih member dari tabel untuk diedit."); return; }
        String nomor = txtNomorHp.getText().trim(); String nama = txtUsername.getText().trim();
        if (nomor.isEmpty() || nama.isEmpty()) { JOptionPane.showMessageDialog(this, "Nomor HP dan Username tidak boleh kosong!"); return; }
        try (PreparedStatement ps = KoneksiDB.getKoneksi().prepareStatement("UPDATE members SET nomor_wa = ?, nama = ? WHERE id_member = ?")) {
            ps.setString(1, nomor); ps.setString(2, nama); ps.setInt(3, selectedMemberId); ps.executeUpdate();
            loadMemberData(); cleanFieldsAction();
        } catch (SQLException e) { JOptionPane.showMessageDialog(this, "Gagal mengedit: " + e.getMessage()); }
    }

    private void deleteMemberAction() {
        if (selectedMemberId == -1) { JOptionPane.showMessageDialog(this, "Pilih member dari tabel untuk dihapus."); return; }
        int confirm = JOptionPane.showConfirmDialog(this, "Yakin ingin menghapus member ini?", "Konfirmasi", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try (PreparedStatement ps = KoneksiDB.getKoneksi().prepareStatement("DELETE FROM members WHERE id_member = ?")) {
                ps.setInt(1, selectedMemberId); ps.executeUpdate();
                loadMemberData(); cleanFieldsAction();
            } catch (SQLException e) { JOptionPane.showMessageDialog(this, "Gagal menghapus: " + e.getMessage()); }
        }
    }
    
    private void simulateAction() {
        String sender = txtSimSender.getText().trim(); String message = txtSimMessage.getText().trim();
        if (sender.isEmpty() || message.isEmpty()) { JOptionPane.showMessageDialog(this, "Isi semua field simulasi."); return; }
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() {
                botService.processAndReplyMessage(sender, message);
                return null;
            }
            @Override protected void done() {
                loadHistoryData(); // Refresh history setelah simulasi selesai
            }
        }.execute();
    }
    
private void broadcastAction() {
    // Ambil teks dari txtBroadcastArea
    String message = txtBroadcastArea.getText().trim(); 
    if (message.isEmpty()) { JOptionPane.showMessageDialog(this, "Pesan broadcast tidak boleh kosong."); return; }
    int confirm = JOptionPane.showConfirmDialog(this, "Kirim broadcast ke semua member?", "Konfirmasi", JOptionPane.YES_NO_OPTION);
    if (confirm == JOptionPane.YES_OPTION) {
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() { botService.sendBroadcast(message); return null; }
            @Override protected void done() {
                JOptionPane.showMessageDialog(MainFrame.this, "Proses broadcast selesai.");
                txtBroadcastArea.setText(""); // Kosongkan JTextArea
                loadHistoryData();
            }
        }.execute();
    }
}
    
    private void deleteAllHistoryAction() {
        int confirm = JOptionPane.showConfirmDialog(this, "Yakin hapus semua history pesan?", "Konfirmasi", JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            try (Statement stmt = KoneksiDB.getKoneksi().createStatement()) {
                stmt.execute("TRUNCATE TABLE message_history");
                JOptionPane.showMessageDialog(this, "History berhasil dihapus.");
                loadHistoryData();
            } catch (SQLException e) { JOptionPane.showMessageDialog(this, "Gagal: " + e.getMessage()); }
        }
    }

    public static void main(String[] args) {
               try {
            // Kita butuh instance BotService untuk diberikan ke server
            BotService service = new BotService(); 
            // Jalankan server di port 8080 (bisa diganti)
            WebhookServer server = new WebhookServer(8090, service); 
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Gagal menjalankan Webhook Server: " + e.getMessage(), "Error Kritis", JOptionPane.ERROR_MESSAGE);
            return; // Hentikan aplikasi jika server gagal start
        }
               
        EventQueue.invokeLater(() -> new MainFrame().setVisible(true));
    }
}

