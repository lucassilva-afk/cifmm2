package br.com.cifmm;

import java.awt.GraphicsEnvironment;
import javax.swing.SwingUtilities;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import br.com.cifmm.view.AppSwingMain;

@SpringBootApplication
@EntityScan(basePackages = "br.com.cifmm.model")
@EnableJpaRepositories(basePackages = "br.com.cifmm.repository")
public class Main {

    private static ConfigurableApplicationContext context;

    public static void main(String[] args) {
        // Configurar para não ser headless
        System.setProperty("java.awt.headless", "false");

        // Verificar se está em ambiente headless
        if (GraphicsEnvironment.isHeadless()) {
            System.out.println("Ambiente headless detectado. Iniciando apenas backend.");
            context = SpringApplication.run(Main.class, args);
            return;
        }

        try {
            // Inicializar Spring Boot
            System.out.println("Iniciando Spring Boot...");
            context = SpringApplication.run(Main.class, args);
            System.out.println("Contexto do Spring Boot inicializado com sucesso.");

            // Abrir interface gráfica na Event Dispatch Thread (EDT)
            SwingUtilities.invokeLater(() -> {
                try {
                    System.out.println("Abrindo a interface gráfica...");
                    AppSwingMain app = context.getBean(AppSwingMain.class);
                    app.setVisible(true);
                    System.out.println("Interface gráfica aberta com sucesso.");
                } catch (Exception e) {
                    System.err.println("Erro ao abrir interface gráfica: " + e.getMessage());
                    e.printStackTrace();
                    javax.swing.JOptionPane.showMessageDialog(null,
                            "Erro ao abrir interface gráfica:\n" + e.getMessage(),
                            "Erro",
                            javax.swing.JOptionPane.ERROR_MESSAGE);
                }
            });

        } catch (Exception e) {
            System.err.println("Erro ao inicializar Spring Boot: " + e.getMessage());
            e.printStackTrace();

            // Mostrar mensagem de erro se não estiver em ambiente headless
            if (!GraphicsEnvironment.isHeadless()) {
                SwingUtilities.invokeLater(() -> {
                    javax.swing.JOptionPane.showMessageDialog(null,
                            "Erro ao inicializar a aplicação:\n" + e.getMessage(),
                            "Erro de Inicialização",
                            javax.swing.JOptionPane.ERROR_MESSAGE);
                });
            }
            System.exit(1);
        }
    }

    public static ConfigurableApplicationContext getContext() {
        return context;
    }
}