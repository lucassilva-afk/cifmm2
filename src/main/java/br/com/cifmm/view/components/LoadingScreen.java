package br.com.cifmm.view.components;

import javax.swing.*;
import java.awt.*;

public class LoadingScreen extends JDialog {
    private static final long serialVersionUID = 1L;
    
    private JProgressBar progressBar;
    private JLabel lblStatus;
    
    public LoadingScreen(JFrame parent) {
        super(parent, "Processando...", true); // 'true' para modal
        initComponents();
        setSize(450, 200);
        setLocationRelativeTo(parent);
        setResizable(false);
        setUndecorated(true); // Oculta a barra de título da janela
    }
    
    private void initComponents() {
        setLayout(new BorderLayout());
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        lblStatus = new JLabel("Iniciando...");
        lblStatus.setFont(new Font("Tahoma", Font.PLAIN, 16));
        lblStatus.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(lblStatus, BorderLayout.NORTH);
        
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        panel.add(progressBar, BorderLayout.CENTER);

        add(panel, BorderLayout.CENTER);
    }
    
    public void setProgress(int value, String message) {
        // Garantir que a atualização da UI ocorra na EDT
        SwingUtilities.invokeLater(() -> {
            progressBar.setValue(value);
            lblStatus.setText(message);
        });
    }
    
    public void setComplete(String message) {
        SwingUtilities.invokeLater(() -> {
            progressBar.setValue(100);
            lblStatus.setText(message);
            // Poderia adicionar um pequeno delay para o usuário ver a mensagem final,
            // mas o ideal é fechar logo em seguida.
        });
    }
    
    public void close() {
        SwingUtilities.invokeLater(this::dispose);
    }
}