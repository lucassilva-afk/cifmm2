package br.com.cifmm.view.components;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class LoadingScreen extends JPanel {
    private static final long serialVersionUID = 1L;
    
    private JProgressBar progressBar;
    private JLabel lblStatus;
    private JLabel lblTempo; // Adicionado para acesso externo
    private Timer timer;
    private int progress = 0;
    private final int TOTAL_TIME = 11000; // 30 segundos em milissegundos
    private final int UPDATE_INTERVAL = 100; // Atualizar a cada 100ms
    private final int MAX_PROGRESS = TOTAL_TIME / UPDATE_INTERVAL; // 300 updates
    
    private Runnable onComplete; // Callback para quando terminar

    public LoadingScreen() {
        initComponents();
    }
    
    private void initComponents() {
        setLayout(null);
        setPreferredSize(new Dimension(450, 200));
        
        lblStatus = new JLabel("Gerando Crachás...");
        lblStatus.setFont(new Font("Tahoma", Font.PLAIN, 20));
        lblStatus.setHorizontalAlignment(SwingConstants.CENTER);
        lblStatus.setBounds(50, 50, 350, 30);
        add(lblStatus);
        
        progressBar = new JProgressBar(0, 100);
        progressBar.setBounds(75, 100, 300, 25);
        progressBar.setStringPainted(true); // Mostra a porcentagem
        progressBar.setString("0%");
        add(progressBar);
        
        // Label para mostrar tempo restante
        lblTempo = new JLabel("Tempo restante: 30s");
        lblTempo.setFont(new Font("Tahoma", Font.PLAIN, 12));
        lblTempo.setHorizontalAlignment(SwingConstants.CENTER);
        lblTempo.setBounds(50, 135, 350, 20);
        add(lblTempo);
        
        // Timer para atualizar a progress bar
        timer = new Timer(UPDATE_INTERVAL, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                progress++;
                
                // Calcula a porcentagem (0-100)
                int percentage = (progress * 100) / MAX_PROGRESS;
                progressBar.setValue(percentage);
                progressBar.setString(percentage + "%");
                
                // Calcula tempo restante
                int tempoRestante = (MAX_PROGRESS - progress) * UPDATE_INTERVAL / 1000;
                lblTempo.setText("Tempo restante: " + tempoRestante + "s");
                
                // Quando chegar a 100%
                if (progress >= MAX_PROGRESS) {
                    timer.stop();
                    lblStatus.setText("Concluído!");
                    lblTempo.setText("Processo finalizado!");
                    
                    // Executa o callback se definido
                    if (onComplete != null) {
                        onComplete.run();
                    }
                }
            }
        });
    }
    
    /**
     * Inicia o carregamento
     */
    public void startLoading() {
        progress = 0;
        progressBar.setValue(0);
        progressBar.setString("0%");
        lblStatus.setText("Gerando Crachás...");
        lblTempo.setText("Tempo restante: 30s");
        timer.start();
    }
    
    /**
     * Para o carregamento
     */
    public void stopLoading() {
        if (timer != null && timer.isRunning()) {
            timer.stop();
        }
    }
    
    /**
     * Força a conclusão do carregamento, atualizando a UI e acionando o callback
     */
    public void completeLoading() {
        if (timer != null && timer.isRunning()) {
            timer.stop();
        }
        progress = MAX_PROGRESS;
        progressBar.setValue(100);
        progressBar.setString("100%");
        lblStatus.setText("Concluído!");
        lblTempo.setText("Processo finalizado!");
        
        // Executa o callback se definido
        if (onComplete != null) {
            onComplete.run();
        }
    }
    
    /**
     * Define uma ação para ser executada quando o carregamento terminar
     */
    public void setOnComplete(Runnable onComplete) {
        this.onComplete = onComplete;
    }
    
    /**
     * Verifica se o carregamento está ativo
     */
    public boolean isLoading() {
        return timer != null && timer.isRunning();
    }
}