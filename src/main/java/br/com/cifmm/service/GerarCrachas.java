package br.com.cifmm.service;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import br.com.cifmm.model.FuncionarioModel;
import br.com.cifmm.repository.FuncionarioRepository;

@Service
public class GerarCrachas {

    // Dependências
    @Autowired private FuncionarioRepository funcionarioRepository;
    @Autowired private GerarQRCode gerarQRCodeService;

    // Configurações de caminhos
    private static final String IMAGES_PATH = "C:/Users/Relogio.ponto/eclipse-workspace/CIFMM2/resources/images/";
    private static final String OUTPUT_PATH = "output/";

    // Posicionamento dos elementos no crachá
    private static final Point POSICAO_FOTO = new Point(41, 71);
    private static final Dimension TAMANHO_FOTO = new Dimension(129, 179);
    private static final Point POSICAO_QR = new Point(410, 305);
    private static final Dimension TAMANHO_QR = new Dimension(50, 50);
    private static final Point POSICAO_NOME = new Point(274, 210);
    private static final Point POSICAO_MATRICULA = new Point(274, 245);
    private static final Point POSICAO_DADOS_VERSO = new Point(23, 45);

    // Fontes
    private static final String FONTE_PRINCIPAL = "Arial";
    private static final String FONTE_CUSTOM_1 = "C:\\Users\\Relogio.ponto\\eclipse-workspace\\CIFMM2\\resources\\fonts\\Museo500-Regular.otf";
    private static final String FONTE_CUSTOM_2 = "C:\\Users\\Relogio.ponto\\eclipse-workspace\\CIFMM2\\resources\\fonts\\Museo300-Regular.otf";

    public void gerarTodosCrachas() {
        criarDiretorioSaida();
        List<FuncionarioModel> funcionarios = funcionarioRepository.findAll();

        funcionarios.forEach(func -> {
            if (dadosValidos(func)) {
                gerarCrachaComQR(func.getNome(), func.getRe(), func.getCargo(), func.getSecretaria());
            } else {
                System.out.println("Dados incompletos para RE: " + func.getRe());
            }
        });
    }

    public void gerarCracha(FuncionarioModel func) {
        if (dadosValidos(func)) {
            gerarCrachaComQR(func.getNome(), func.getRe(), func.getCargo(), func.getSecretaria());
        }
    }

    private boolean dadosValidos(FuncionarioModel func) {
        return func.getNome() != null && func.getRe() != null && 
               func.getCargo() != null && func.getSecretaria() != null;
    }

    private void gerarCrachaComQR(String nome, String matricula, String cargo, String secretaria) {
        criarDiretorioSaida();
        garantirQRCodeExistente(matricula);

        try {
            BufferedImage frente = processarFrente(nome, matricula);
            BufferedImage verso = processarVerso(nome, cargo, secretaria, matricula);
            
            salvarCracha(frente, verso, matricula);
            System.out.println("Crachá gerado para " + nome + " (RE: " + matricula + ")");
        } catch (Exception e) {
            System.err.println("Erro ao gerar crachá para RE: " + matricula);
            e.printStackTrace();
        }
    }

    private void criarDiretorioSaida() {
        new File(OUTPUT_PATH).mkdirs();
    }

    private void garantirQRCodeExistente(String matricula) {
        File qrFile = new File(IMAGES_PATH + matricula + ".png");
        
        if (!qrFile.exists()) {
            System.out.println("Baixando QR Code para RE: " + matricula);
            gerarQRCodeService.baixarQRCode(matricula);
            aguardarDownloadQR(matricula);
        }
    }

    private void aguardarDownloadQR(String matricula) {
        File qrFile = new File(IMAGES_PATH + matricula + ".png");
        int tentativas = 0;
        int maxTentativas = 5;

        while (!qrFile.exists() && tentativas < maxTentativas) {
            try {
                Thread.sleep(1000);
                tentativas++;
                System.out.println("Aguardando QR Code... (" + tentativas + "/" + maxTentativas + ")");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        if (!qrFile.exists()) {
            throw new RuntimeException("QR Code não disponível para RE: " + matricula);
        }
    }

    private BufferedImage processarFrente(String nome, String matricula) throws Exception {
        BufferedImage template = ImageIO.read(new File(IMAGES_PATH + "Cracha_Frente.jpg"));
        System.out.println("Template dimensions: " + template.getWidth() + "x" + template.getHeight());

        // Carrega foto e QR Code antes
        BufferedImage foto = carregarFotoFuncionario(matricula);
        BufferedImage qrCode = carregarImagem(matricula + ".png");

        // Cria o Graphics2D
        Graphics2D g = template.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Desenha foto
        if (foto != null) {
            g.drawImage(foto, POSICAO_FOTO.x, POSICAO_FOTO.y, TAMANHO_FOTO.width, TAMANHO_FOTO.height, null);
        }

        // Desenha QR Code
        if (qrCode != null) {
            g.drawImage(qrCode, POSICAO_QR.x, POSICAO_QR.y, TAMANHO_QR.width, TAMANHO_QR.height, null);
        }

        // Adiciona textos por último
        configurarTextosFrente(g, nome, matricula);

        // Fecha o Graphics
        g.dispose();

        return template;
    }


    private BufferedImage processarVerso(String nome, String cargo, String secretaria, String matricula) throws Exception {
        BufferedImage template = ImageIO.read(new File(IMAGES_PATH + "Cracha_Verso.jpg"));
        Graphics2D g = template.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Configura fonte e cor
        Font fonte = Font.createFont(Font.TRUETYPE_FONT, new File(FONTE_CUSTOM_2)).deriveFont(12f);
        g.setFont(fonte);
        g.setColor(Color.BLACK);

        // Adiciona textos
        int linha = POSICAO_DADOS_VERSO.y;
        g.drawString(nome, POSICAO_DADOS_VERSO.x, linha);
        linha += 50;
        g.drawString(cargo, POSICAO_DADOS_VERSO.x, linha);
        linha += 50;
        g.drawString(secretaria, POSICAO_DADOS_VERSO.x, linha);

        g.dispose();
        return template;
    }

    private void configurarTextosFrente(Graphics2D g, String nome, String matricula) {
        // Ativa suavização de texto
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Define a cor do texto como preto (ou outra cor visível)
        g.setColor(Color.BLACK);

        // Log para verificar os valores de entrada
        System.out.println("Desenhando nome: " + nome + ", matricula: " + matricula);

        // Nome
        g.setFont(new Font(FONTE_PRINCIPAL, Font.BOLD, 24));
        String primeiroNome = getPrimeiroNome(nome);
        System.out.println("Primeiro nome: " + primeiroNome + ", posição: (" + POSICAO_NOME.x + ", " + POSICAO_NOME.y + ")");
        drawStringFit(g, primeiroNome, POSICAO_NOME.x, POSICAO_NOME.y, 220);

        // Matrícula (com fallback se fonte custom falhar)
        try {
            Font fonteCustom = Font.createFont(Font.TRUETYPE_FONT, new File(FONTE_CUSTOM_1)).deriveFont(18f);
            g.setFont(fonteCustom);
        } catch (Exception e) {
            System.err.println("Fonte customizada não encontrada, usando Arial padrão: " + e.getMessage());
            g.setFont(new Font("Arial", Font.PLAIN, 18));
        }
        System.out.println("Desenhando RE: " + matricula + ", posição: (" + POSICAO_MATRICULA.x + ", " + POSICAO_MATRICULA.y + ")");
        g.drawString("RE: " + matricula, POSICAO_MATRICULA.x, POSICAO_MATRICULA.y);
    }


    private BufferedImage carregarFotoFuncionario(String matricula) {
        BufferedImage foto = carregarImagem(matricula + ".jpg");
        return foto != null ? foto : carregarImagem(matricula + ".png");
    }

    private BufferedImage carregarImagem(String nomeArquivo) {
        try {
            File arquivo = new File(IMAGES_PATH + nomeArquivo);
            if (arquivo.exists()) {
                return ImageIO.read(arquivo);
            }
        } catch (Exception e) {
            System.err.println("Erro ao carregar imagem: " + nomeArquivo + " - " + e.getMessage());
        }
        return null;
    }

    private void salvarCracha(BufferedImage frente, BufferedImage verso, String matricula) throws Exception {
        String frentePath = OUTPUT_PATH + "cracha_frente_" + matricula + ".png";
        String versoPath = OUTPUT_PATH + "cracha_verso_" + matricula + ".png";

        ImageIO.write(frente, "png", new File(frentePath));
        ImageIO.write(verso, "png", new File(versoPath));
    }

    private String getPrimeiroNome(String nome) {
        if (nome == null || nome.trim().isEmpty()) return "";
        return nome.trim().split("\\s+")[0];
    }

    private void drawStringFit(Graphics2D g, String text, int x, int y, int maxWidth) {
        if (text == null) text = "";
        Font font = g.getFont();
        
        while (g.getFontMetrics().stringWidth(text) > maxWidth && font.getSize() > 8) {
            font = font.deriveFont((float) font.getSize() - 1);
            g.setFont(font);
        }
        
        g.drawString(text, x, y);
    }
}