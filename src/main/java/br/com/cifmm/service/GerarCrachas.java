package br.com.cifmm.service;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import java.util.Objects;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import br.com.cifmm.model.FuncionarioModel;
import br.com.cifmm.repository.FuncionarioRepository;

@Service
public class GerarCrachas {
	
	private static final String getResourcePath(String resourcePath) {
		return Objects.requireNonNull(GerarCrachas.class.getClassLoader().getResource(resourcePath)).getPath();
	}

    // DependÃªncias
    @Autowired private FuncionarioRepository funcionarioRepository;
    @Autowired private GerarQRCode gerarQRCodeService;
    
    private static final String PROJECT_ROOT = System.getProperty("user.dir");

    // ConfiguraÃ§Ãµes de caminhos
    private static final String IMAGES_PATH = System.getProperty("user.dir") + 
            File.separator + "src" + 
            File.separator + "main" + 
            File.separator + "resources" + 
            File.separator + "images" + 
            File.separator;
    private static final String OUTPUT_PATH = PROJECT_ROOT + "/output/";

    // Posicionamento dos elementos no crachÃ¡
    private static final Point POSICAO_FOTO = new Point(41, 71);
    private static final Dimension TAMANHO_FOTO = new Dimension(129, 179);
    private static final Point POSICAO_QR = new Point(410, 305);
    private static final Dimension TAMANHO_QR = new Dimension(50, 50);
    private static final Point POSICAO_NOME = new Point(274, 210);
    private static final Point POSICAO_MATRICULA = new Point(274, 245);
    private static final Point POSICAO_DADOS_VERSO = new Point(23, 45);

    // Fontes
    private static final String FONTE_PRINCIPAL = "Arial";
    private static final String FONTE_CUSTOM_1 = getResourcePath("fonts/Museo500-Regular.otf");
    private static final String FONTE_CUSTOM_2 = getResourcePath("fonts/Museo300-Regular.otf");

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
            System.out.println("CrachÃ¡ gerado para " + nome + " (RE: " + matricula + ")");
        } catch (Exception e) {
            System.err.println("Erro ao gerar crachÃ¡ para RE: " + matricula);
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
            throw new RuntimeException("QR Code nÃ£o disponÃ­vel para RE: " + matricula);
        }
    }

    private BufferedImage processarFrente(String nome, String matricula) throws Exception {
        BufferedImage template = ImageIO.read(new File(IMAGES_PATH + "Cracha_Frente.jpg"));
        System.out.println("Template dimensions: " + template.getWidth() + "x" + template.getHeight());

        // Carrega foto e QR Code antes
        BufferedImage foto = carregarFotoFuncionario(matricula);
        BufferedImage qrCode = carregarImagem(matricula + ".png");

        // Busca o funcionário no banco para verificar se tem apelido
        // CORREÇÃO: Usando Optional para evitar erro de múltiplos resultados
        String apelido = null;
        try {
            Optional<FuncionarioModel> funcionarioOpt = funcionarioRepository.findFirstByRe(matricula);
            if (funcionarioOpt.isPresent()) {
                FuncionarioModel funcionario = funcionarioOpt.get();
                apelido = funcionario.getApelido();
                System.out.println("Funcionário encontrado: " + funcionario.getNome() + ", Apelido: " + apelido);
            } else {
                System.out.println("Nenhum funcionário encontrado com RE: " + matricula);
            }
        } catch (Exception e) {
            System.err.println("Erro ao buscar funcionário com RE " + matricula + ": " + e.getMessage());
            // Continua sem apelido em caso de erro
            apelido = null;
        }

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

        // Adiciona textos por último (agora inclui apelido)
        configurarTextosFrente(g, nome, matricula, apelido);

        // Fecha o Graphics
        g.dispose();

        return template;
    }
    
    /**
     * Versão sobrecarregada que aceita um FuncionarioModel com todos os dados
     * Esta versão usa os dados passados diretamente, não busca no banco
     */
    private BufferedImage processarFrente(FuncionarioModel funcionario) throws Exception {
        BufferedImage template = ImageIO.read(new File(IMAGES_PATH + "Cracha_Frente.jpg"));
        System.out.println("Template dimensions: " + template.getWidth() + "x" + template.getHeight());

        // Carrega foto e QR Code antes
        BufferedImage foto = carregarFotoFuncionario(funcionario.getRe());
        BufferedImage qrCode = carregarImagem(funcionario.getRe() + ".png");

        // USA OS DADOS DO OBJETO PASSADO, NÃO DO BANCO
        String apelido = funcionario.getApelido();

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

        // Adiciona textos por último usando os dados corretos do funcionário
        configurarTextosFrente(g, funcionario.getNome(), funcionario.getRe(), apelido);

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

    private void configurarTextosFrente(Graphics2D g, String nome, String matricula, String apelido) {
        // Ativa suavização de texto
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Define a cor do texto como preto
        g.setColor(Color.BLACK);

        // Log para verificar os valores de entrada
        System.out.println("Desenhando nome: " + nome + ", matricula: " + matricula + ", apelido: " + apelido);

        String primeiroNome = getPrimeiroNome(nome);
        String nomeExibir;

        // Se o apelido existir, formata como "PrimeiroNome (Apelido)"
        if (apelido != null && !apelido.trim().isEmpty()) {
            nomeExibir = primeiroNome + " (" + apelido.trim() + ")";
            System.out.println("Usando nome com apelido: " + nomeExibir);
        } else {
            nomeExibir = primeiroNome;
            System.out.println("Usando apenas primeiro nome: " + nomeExibir);
        }

        // Nome (com ou sem apelido)
        g.setFont(new Font(FONTE_PRINCIPAL, Font.BOLD, 24));
        System.out.println("Nome/Apelido a ser exibido: " + nomeExibir + ", posição: (" + POSICAO_NOME.x + ", " + POSICAO_NOME.y + ")");
        // O método drawStringFit já ajusta o tamanho da fonte se o texto for muito longo
        drawStringFit(g, nomeExibir, POSICAO_NOME.x, POSICAO_NOME.y, 220); 

        // Matrícula (código existente)
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
    
    /**
     * Regenera APENAS a frente do crachá de um funcionário.
     * Ideal para quando apenas o apelido é alterado.
     * @param func O funcionário com os dados atualizados.
     */
    public void regenerarFrenteCracha(FuncionarioModel func) {
        if (!dadosValidos(func)) {
            System.out.println("Dados inválidos para regenerar crachá do RE: " + func.getRe());
            return;
        }

        try {
            System.out.println("🎨 Regenerando frente do crachá para: " + func.getNome() + " (RE: " + func.getRe() + ")");
            System.out.println("📝 Dados utilizados - Nome: " + func.getNome() + ", RE: " + func.getRe() + ", Apelido: " + func.getApelido());
            
            // Remove o arquivo antigo primeiro
            String frentePath = OUTPUT_PATH + "cracha_frente_" + func.getRe() + ".png";
            File arquivoAntigo = new File(frentePath);
            if (arquivoAntigo.exists()) {
                if (arquivoAntigo.delete()) {
                    System.out.println("🗑️ Arquivo antigo removido: " + frentePath);
                } else {
                    System.err.println("⚠️ Não foi possível remover arquivo antigo: " + frentePath);
                }
            }
            
            // Processa apenas a frente usando os dados corretos do funcionário passado
            BufferedImage frente = processarFrente(func); // Usa a versão que aceita FuncionarioModel
            
            // Salva apenas a frente
            ImageIO.write(frente, "png", new File(frentePath));
            
            System.out.println("✅ Frente do crachá REGENERADA para " + func.getNome() + " (RE: " + func.getRe() + ")");
        } catch (Exception e) {
            System.err.println("❌ Erro ao regenerar a frente do crachá para RE: " + func.getRe());
            e.printStackTrace();
        }
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
    
    public void gerarCrachasEmPDF(String matricula) {
        try {
            
            String frentePath = GerarCrachas.OUTPUT_PATH + "cracha_frente_" + matricula + ".png";
            String versoPath  = GerarCrachas.OUTPUT_PATH + "cracha_verso_" + matricula + ".png";
            
            GerarPDF gerarPDF = new GerarPDF();
            gerarPDF.setFrentePath(frentePath);
            gerarPDF.setVersoPath(versoPath);                        


            JOptionPane.showMessageDialog(null, "PDF gerado com sucesso!");
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "Erro ao gerar PDF: " + ex.getMessage());
        }
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