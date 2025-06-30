package com.chatbot.gui;

import com.chatbot.config.KoneksiDB;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;

public class KeywordFrame extends JFrame {

    // Komponen GUI
    private JTable tblKeywords;
    private DefaultTableModel modelKeywords;
    private JTextField txtKeyword;
    private JTextArea txtJawaban;
    private JButton btnTambah, btnUpdate, btnHapus, btnBersihkan;
    private int selectedKeywordId = -1;

    public KeywordFrame() {
        setTitle("Manajemen Keyword & Jawaban");
        setSize(700, 500);
        setLocationRelativeTo(null); // Muncul di tengah
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); // Hanya menutup jendela ini, bukan seluruh aplikasi
        
        initComponents();
        loadKeywordData();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        JPanel mainPanel = (JPanel) getContentPane();
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Panel Form (Kiri)
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(new TitledBorder(" Form Keyword "));
        formPanel.setPreferredSize(new java.awt.Dimension(250, 0));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST;
        formPanel.add(new JLabel("Keyword:"), gbc);
        
        gbc.gridy++;
        txtKeyword = new JTextField();
        formPanel.add(txtKeyword, gbc);
        
        gbc.gridy++;
        formPanel.add(new JLabel("Jawaban Otomatis:"), gbc);
        
        gbc.gridy++;
        txtJawaban = new JTextArea(8, 20);
        txtJawaban.setLineWrap(true);
        txtJawaban.setWrapStyleWord(true);
        JScrollPane scrollJawaban = new JScrollPane(txtJawaban);
        formPanel.add(scrollJawaban, gbc);

        JPanel buttonFormPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        btnTambah = new JButton("Tambah");
        btnUpdate = new JButton("Update");
        btnHapus = new JButton("Hapus");
        btnBersihkan = new JButton("Bersihkan");
        buttonFormPanel.add(btnTambah);
        buttonFormPanel.add(btnUpdate);
        buttonFormPanel.add(btnHapus);
        buttonFormPanel.add(btnBersihkan);
        
        gbc.gridy++;
        formPanel.add(buttonFormPanel, gbc);
        
        gbc.gridy++; gbc.weighty = 1.0; // Spacer
        formPanel.add(new JLabel(), gbc);

        // Panel Tabel (Kanan)
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBorder(new TitledBorder(" Daftar Keyword Tersimpan "));
        modelKeywords = new DefaultTableModel(new String[]{"ID", "Keyword", "Jawaban"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tblKeywords = new JTable(modelKeywords);
        tblKeywords.getColumnModel().getColumn(0).setMaxWidth(40);
        tblKeywords.getColumnModel().getColumn(1).setPreferredWidth(100);
        tblKeywords.getColumnModel().getColumn(2).setPreferredWidth(250);
        tablePanel.add(new JScrollPane(tblKeywords), BorderLayout.CENTER);

        // Tambahkan Aksi Listener
        addActions();
        
        add(formPanel, BorderLayout.WEST);
        add(tablePanel, BorderLayout.CENTER);
    }
    
    private void addActions() {
    btnTambah.addActionListener(e -> addKeyword());
    btnUpdate.addActionListener(e -> updateKeyword());
    btnHapus.addActionListener(e -> deleteKeyword());
    btnBersihkan.addActionListener(e -> cleanFields());

    // PASTIKAN BLOK INI ADA DAN BENAR
    tblKeywords.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
            int selectedRow = tblKeywords.getSelectedRow();
            if (selectedRow != -1) {
                selectedKeywordId = (int) modelKeywords.getValueAt(selectedRow, 0);
                txtKeyword.setText((String) modelKeywords.getValueAt(selectedRow, 1));
                
                // Ambil jawaban lengkap dari database
                try (PreparedStatement ps = KoneksiDB.getKoneksi().prepareStatement("SELECT jawaban FROM keywords WHERE id_keyword = ?")) {
                    ps.setInt(1, selectedKeywordId);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        txtJawaban.setText(rs.getString("jawaban"));
                    }
                    rs.close(); // Selalu tutup ResultSet
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(KeywordFrame.this, "Error mengambil detail jawaban: " + ex.getMessage());
                }
            }
        }
    });
}

    private void loadKeywordData() {
        modelKeywords.setRowCount(0);
        try (Statement stmt = KoneksiDB.getKoneksi().createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id_keyword, keyword, jawaban FROM keywords ORDER BY id_keyword DESC")) {
            while (rs.next()) {
                modelKeywords.addRow(new Object[]{rs.getInt("id_keyword"), rs.getString("keyword"), rs.getString("jawaban")});
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error memuat data keyword: " + e.getMessage());
        }
    }

    private void cleanFields() {
        txtKeyword.setText("");
        txtJawaban.setText("");
        tblKeywords.clearSelection();
        selectedKeywordId = -1;
    }

    private void addKeyword() {
        String keyword = txtKeyword.getText().trim().toLowerCase();
        String jawaban = txtJawaban.getText().trim();
        if (keyword.isEmpty() || jawaban.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Keyword dan Jawaban tidak boleh kosong.");
            return;
        }
        try (PreparedStatement ps = KoneksiDB.getKoneksi().prepareStatement("INSERT INTO keywords (keyword, jawaban) VALUES (?, ?)")) {
            ps.setString(1, keyword);
            ps.setString(2, jawaban);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Keyword baru berhasil ditambahkan.");
            loadKeywordData();
            cleanFields();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Gagal menambah keyword: " + e.getMessage());
        }
    }

    private void updateKeyword() {
        if (selectedKeywordId == -1) {
            JOptionPane.showMessageDialog(this, "Pilih keyword dari tabel untuk di-update.");
            return;
        }
        String keyword = txtKeyword.getText().trim().toLowerCase();
        String jawaban = txtJawaban.getText().trim();
        if (keyword.isEmpty() || jawaban.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Keyword dan Jawaban tidak boleh kosong.");
            return;
        }
        try (PreparedStatement ps = KoneksiDB.getKoneksi().prepareStatement("UPDATE keywords SET keyword = ?, jawaban = ? WHERE id_keyword = ?")) {
            ps.setString(1, keyword);
            ps.setString(2, jawaban);
            ps.setInt(3, selectedKeywordId);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Keyword berhasil di-update.");
            loadKeywordData();
            cleanFields();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Gagal meng-update keyword: " + e.getMessage());
        }
    }
    
    private void deleteKeyword() {
        if (selectedKeywordId == -1) {
            JOptionPane.showMessageDialog(this, "Pilih keyword dari tabel untuk dihapus.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "Yakin ingin menghapus keyword ini?", "Konfirmasi", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try (PreparedStatement ps = KoneksiDB.getKoneksi().prepareStatement("DELETE FROM keywords WHERE id_keyword = ?")) {
                ps.setInt(1, selectedKeywordId);
                ps.executeUpdate();
                JOptionPane.showMessageDialog(this, "Keyword berhasil dihapus.");
                loadKeywordData();
                cleanFields();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Gagal menghapus keyword: " + e.getMessage());
            }
        }
    }
}