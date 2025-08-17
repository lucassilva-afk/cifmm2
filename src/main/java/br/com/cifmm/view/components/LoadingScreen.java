package br.com.cifmm.view.components;

import javax.swing.*;
import java.awt.*;
import javax.swing.border.LineBorder;
import javax.swing.border.EmptyBorder;

public class LoadingScreen extends JDialog {
    private static final long serialVersionUID = 1L;
    
    private JProgressBar progressBar;
    private JLabel lblStatus;
    private JLabel lblPercentage; // 📊 Novo label para mostrar porcentagem
    
    public LoadingScreen(JFrame parent) {
        super(parent, "Processando...", false);
        initComponents();
        setSize(450, 250); // 📏 Aumentar altura para acomodar novo label
        setLocationRelativeTo(parent);
        setResizable(false);
        setUndecorated(true);
    }
    
    private void initComponents() {
        getContentPane().setLayout(new BorderLayout());
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        panel.setBorder(new LineBorder(Color.GRAY, 2)); // 🎨 Adicionar borda
        
        lblStatus = new JLabel("Iniciando processamento...");
        lblStatus.setFont(new Font("Tahoma", Font.PLAIN, 16));
        lblStatus.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(lblStatus, BorderLayout.NORTH);
        
        // 📊 Painel central com barra de progresso e porcentagem
        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));
        
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(false); // 🔧 Remover texto da barra (usaremos label separado)
        progressBar.setPreferredSize(new Dimension(350, 25));
        centerPanel.add(progressBar, BorderLayout.CENTER);
        
        lblPercentage = new JLabel("0%");
        lblPercentage.setFont(new Font("Tahoma", Font.BOLD, 18));
        lblPercentage.setHorizontalAlignment(SwingConstants.CENTER);
        lblPercentage.setForeground(new Color(0, 120, 215)); // 🎨 Cor azul
        centerPanel.add(lblPercentage, BorderLayout.SOUTH);
        
        panel.add(centerPanel, BorderLayout.CENTER);
        
        getContentPane().add(panel, BorderLayout.CENTER);
    }
    
    public void setProgress(int value, String message) {
        // 🔧 GARANTIR que value esteja no range correto
        final int clampedValue = Math.max(0, Math.min(100, value));
        final String safeMessage = message != null ? message : "Processando...";
        
        // Garantir que a atualização da UI ocorra na EDT
        SwingUtilities.invokeLater(() -> {
            progressBar.setValue(clampedValue);
            lblStatus.setText(safeMessage);
            lblPercentage.setText(clampedValue + "%");
            
            // 🎨 MUDANÇA DE COR baseada no progresso
            if (clampedValue < 30) {
                lblPercentage.setForeground(Color.RED);
            } else if (clampedValue < 70) {
                lblPercentage.setForeground(Color.ORANGE);
            } else {
                lblPercentage.setForeground(new Color(0, 150, 0)); // Verde
            }
            
            // 🔄 Força repaint para garantir atualização visual
            progressBar.repaint();
            lblStatus.repaint();
            lblPercentage.repaint();
        });
    }
    
    public void setComplete(String message) {
        SwingUtilities.invokeLater(() -> {
            progressBar.setValue(100);
            lblStatus.setText(message != null ? message : "Concluído!");
            lblPercentage.setText("100%");
            lblPercentage.setForeground(new Color(0, 150, 0)); // ✅ Verde para sucesso
            
            // 🔄 Força repaint
            progressBar.repaint();
            lblStatus.repaint();
            lblPercentage.repaint();
        });
    }
    
    public void close() {
        SwingUtilities.invokeLater(this::dispose);
    }
}