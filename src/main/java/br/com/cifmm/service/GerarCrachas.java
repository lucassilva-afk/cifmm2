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
    private static final String IMAGES_PATH = "resources/images/";
    private static final String OUTPUT_PATH = PROJECT_ROOT + "/output/";

    // Posicionamento dos elementos no crachÃ¡
    private static final Point POSICAO_FOTO = new Point(32, 76);
    private static final Dimension TAMANHO_FOTO = new Dimension(170, 185);
    private static final Point POSICAO_QR = new Point(410, 232);
    private static final Dimension TAMANHO_QR = new Dimension(70, 64);
    private static final Point POSICAO_NOME = new Point(267, 170);
    private static final Point POSICAO_MATRICULA = new Point(267, 215);
    private static final Point POSICAO_DADOS_VERSO = new Point(23, 45);

    // Fontes
    private static final String FONTE_PRINCIPAL = "Arial";
    private static final String FONTE_CUSTOM_1 = "resources/fonts/Museo500-Regular.otf";
    private static final String FONTE_CUSTOM_2 = "resources/fonts/Museo300-Regular.otf";
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

        // Busca o funcionário no banco para pegar os dados completos
        FuncionarioModel funcionario = null;
        try {
            Optional<FuncionarioModel> funcionarioOpt = funcionarioRepository.findFirstByRe(matricula);
            if (funcionarioOpt.isPresent()) {
                funcionario = funcionarioOpt.get();
                System.out.println("Funcionário encontrado: " + funcionario.getNome() + 
                    ", Apelido: " + funcionario.getApelido() + 
                    ", Nome Crachá: " + funcionario.getNomeCracha());
            } else {
                System.out.println("Nenhum funcionário encontrado com RE: " + matricula);
            }
        } catch (Exception e) {
            System.err.println("Erro ao buscar funcionário com RE " + matricula + ": " + e.getMessage());
            funcionario = null;
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

        // Adiciona textos por último
        if (funcionario != null) {
            // Se encontrou o funcionário no banco, usa a versão que aceita FuncionarioModel
            configurarTextosFrente(g, funcionario);
        } else {
            // Se não encontrou, usa a versão tradicional (fallback)
        	configurarTextosFrente(g, funcionario);
        }

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
        configurarTextosFrente(g, funcionario);

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
        linha += 55;
        g.drawString(cargo, POSICAO_DADOS_VERSO.x, linha);
        linha += 55;
        g.drawString(secretaria, POSICAO_DADOS_VERSO.x, linha);

        g.dispose();
        return template;
    }

    private void configurarTextosFrente(Graphics2D g, FuncionarioModel funcionario) {
        // Ativa suavização de texto
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Define a cor do texto como preto
        g.setColor(Color.BLACK);

        String nome = funcionario.getNome();
        String matricula = funcionario.getRe();
        String apelido = funcionario.getApelido();

        // Log para verificar os valores de entrada
        System.out.println("Desenhando nome: " + nome + ", matricula: " + matricula + ", apelido: " + apelido);

        // Usa o nome do crachá personalizado se existir, senão usa o primeiro nome
        String nomeParaCracha;
        String nomeCracha = funcionario.getNomeCracha();
        if (nomeCracha != null && !nomeCracha.trim().isEmpty()) {
            nomeParaCracha = nomeCracha;
            System.out.println("Usando nome do crachá personalizado: " + nomeParaCracha);
        } else {
            nomeParaCracha = getPrimeiroNome(nome);
            System.out.println("Usando primeiro nome como padrão: " + nomeParaCracha);
        }
        
        // Configuração da fonte principal para o nome
        g.setFont(new Font(FONTE_PRINCIPAL, Font.BOLD, 24));
        
        // 1. Desenha o nome do crachá
        System.out.println("Desenhando nome no crachá: " + nomeParaCracha + ", posição: (" + POSICAO_NOME.x + ", " + POSICAO_NOME.y + ")");
        drawStringFit(g, nomeParaCracha, POSICAO_NOME.x, POSICAO_NOME.y, 220);

        // 2. Desenha o apelido (se existir) - uma linha abaixo do nome
        if (apelido != null && !apelido.trim().isEmpty()) {
            int posicaoApelidoY = POSICAO_NOME.y + 30;
            String apelidoFormatado = "\"" + apelido.trim() + "\"";
            
            System.out.println("Desenhando apelido: " + apelidoFormatado + ", posição: (" + POSICAO_NOME.x + ", " + posicaoApelidoY + ")");
            drawStringFit(g, apelidoFormatado, POSICAO_NOME.x, posicaoApelidoY, 220);
        }

        // 3. Matrícula/RE - configuração da fonte customizada
        try {
            Font fonteCustom = Font.createFont(Font.TRUETYPE_FONT, new File(FONTE_CUSTOM_1)).deriveFont(18f);
            g.setFont(fonteCustom);
        } catch (Exception e) {
            System.err.println("Fonte customizada não encontrada, usando Arial padrão: " + e.getMessage());
            g.setFont(new Font("Arial", Font.PLAIN, 18));
        }
        
        // Formato atualizado para "Matricula/RE"
        String matriculaFormatada = "RE: " + matricula;
        System.out.println("Desenhando matrícula: " + matriculaFormatada + ", posição: (" + POSICAO_MATRICULA.x + ", " + POSICAO_MATRICULA.y + ")");
        g.drawString(matriculaFormatada, POSICAO_MATRICULA.x, POSICAO_MATRICULA.y + 30);
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
        
        String[] partes = nome.trim().split("\\s+");
        
        // Se tem apenas uma palavra, retorna ela
        if (partes.length == 1) {
            return partes[0];
        }
        
        // Lista de nomes compostos comuns no Brasil
        String[] nomesCompostos = {
                "ABIGAIL CELEGATTI CLARO", "ADEMIR NOVAES", "ADMIR ALVES LEITE", "ADRIAN NALIATO", "ADRIANA ANDRE", "ADRIANA ANUNCIATO CASSEMIRO", "ADRIANA APARECIDA PEREIRA", "ADRIANA AYUMI MATSUMORI",
                "ADRIANA BRONZATTO MORGON", "ADRIANA CASSIA SIMOES", "ADRIANA CRISTINA SABINO", "ADRIANA MARIA GUARNIERI", "ADRIANA MAURA GUARNIERI", "ADRIANA ROBERTA PRIMO", "ADRIANA VALERIA JARNIAC", "ADRIANA ZINGRA GUERREIRO",
                "ADRIANO APARECIDO DELALANA", "ADRIANO COLICI", "ADRIANO PEREIRA SOARES", "AGNALDO ANTONIO PORCINI", "AGUINALDO APARECIDO FLORIANO", "AIRTON ANTONIO TUROLLA", "ALANNA WURZLER SILVEIRA", "ALBA VALERIA NOGUEIRA",
                "ALCEU CAMPAGNOLI", "ALESSANDRA CRISTINA LAURINDO", "ALESSANDRA CRISTINA NIERI", "ALESSANDRA SPITTI CECATO", "ALESSANDRA VERGANI SILVA", "ALESSANDRA ZAGANIN FALASCHI", "ALESSANDRO CARLOS", "ALESSANDRO JOSE",
                "ALEX ANDERSON MARTINS", "ALEXANDER LIMA BALECH", "ALEXANDER TORRES LUCON", "ALEXANDER VITORIO", "ALEXANDRA INES LEME", "ALEXANDRE ALEM LOPES", "ALEXANDRE ANTONIO FELIX", "ALEXANDRE BIGHETI",
                "ALEXANDRE CIACO NUNES", "ALEXANDRE FANTINATO CRUZ", "ALEXANDRE PEREIRA", "ALEXANDRE RAMOS MOUSINHO", "ALICE ANDRADE SILVA", "ALICE SOBOTTKA EUGENIO", "ALINE CRISTINA CREDITO", "ALINE LEE NUNES",
                "ALINE MARA VILLELA", "ALIPIO MARIO ROMANINI", "ALMIR ROGERIO SUARES", "AMANDA CHAGAS BUENO", "AMANDA ZANI COSTA", "AMILTON DURVALINO MOUTINHO", "ANA BEATRIZ", "ANA CAROLINA",
                "ANA CAROLINA FERREIRA", "ANA CAROLINA PINTO", "ANA CEZARINA VAZ", "ANA CLARA", "ANA CLAUDIA QUAGLIO", "ANA CRISTINA BERTAZZOLI", "ANA CRISTINA RODRIGUES", "ANA JULIA",
                "ANA LUCIMARA PERCEGO", "ANA MARIA CAPRETZ", "ANA MARIA MOREIRA", "ANA OLIVIA FERNANDES", "ANA PAULA", "ANA PAULA PEROCCI", "ANA PAULA RODRIGUES", "ANA PAULA VITOR",
                "ANA RITA PEREIRA", "ANA SIMONE MOREIRA", "ANANDA LETICIA FRANCISCO", "ANDRE LUIS PERCEBON", "ANDRE LUIS ROVIGATTI", "ANDRE SANTOS SILVA", "ANDRE WOHLERS", "ANDREA LAMI NATIVIDADE",
                "ANDREIA CRISTIANA FERRAZ", "ANDREIA CRISTINA ARANTES", "ANDREIA MANCINI BRAZ", "ANDREIA PAULA AUGUSTO", "ANDREIA REGINA CLAUDIO", "ANDREIA SILVEIRA", "ANDREIA TEREZINHA MARQUES", "ANDREIA TETZLAF MANSANO",
                "ANDRESSA RIBEIRO ALVES", "ANGELA APARECIDA NOSARI", "ANGELA IBARRA CARDOSO", "ANGELA MARIA BAZZUCCO", "ANGELA MARIA BORGES", "ANGELA MARIA PALHARES", "ANGELA PRONI DURAN", "ANGELA TEIXEIRA",
                "ANNALY PRETO", "ANNE CAROLINE", "ANNE CLAIRE", "ANTONIO ALEXANDRE BENTO", "ANTONIO CARLOS", "ANTONIO CARLOS GRACINI", "ANTONIO CARLOS MARCOLINO", "ANTONIO CARLOS MIADAIRA",
                "ANTONIO CARLOS ZENI", "ANTONIO CLARET PINTO", "ANTONIO CLAUDIO GALLO", "ANTONIO CREMASCO", "ANTONIO DAMASIO", "ANTONIO FERREIRA SACRAMENTO", "ANTONIO GOMES FERREIRA", "ANTONIO JOSE",
                "ANTONIO ROBERTO NARDON", "APARECIDA ANTOGNOLLI FERMINO", "APARECIDA BATISTA BELCHIOR", "APARECIDA CASSIA ALVES", "APARECIDA MARA BONATTI", "APARECIDA NAKIBAR MODESTO", "APARECIDO AUGUSTO", "ARGENTINO PEREIRA",
                "ARI VELOZO", "AUDREY ROBERTA PETERTIN", "AURACI LOPES BARBOSA", "AVELINO ALEX MORAES", "BEATRIS GODOI RODRIGUES", "BEATRIZ CAMARGO", "BEATRIZ MARTINI ANDRADE", "BENEDITA APARECIDA ALVES",
                "BENEDITO AGRIPINO", "BENEDITO ANTONIO DUARTI", "BENEDITO GUMERCINDO CARDOZO", "BENEDITO JOCIVAL MARIANO", "BERTOLINA BERTOCCI FRANCO", "BRENO BATISTA ANDRADE", "BRUNA HEITZMANN TORRES", "CAIO MACHADO FRANCATTO",
                "CAMILA MARCHIORI", "CAMILA STIVALLE POLLETTINI", "CARINA ROBERTA COELHO", "CARLA APARECIDA RODRIGUES", "CARLA RIZELLI GUARNIERI", "CARLOS ALBERTO", "CARLOS ANTONIO BORGES", "CARLOS EDUARDO",
                "CARLOS EDUARDO ANSELMO", "CARLOS EDUARDO FERREIRA", "CARLOS HENRIQUE", "CARLOS HENRIQUE FERREIRA", "CARLOS PEREIRA", "CARLOS ROBERTO FERREIRA", "CARLOS ROBERTO GILENO", "CARLOS ROBERTO GRUEL",
                "CARLOS ROGERIO PINHEIRO", "CARMEN GUARNIERI MALVEZZI", "CAROLINA RODRIGUES CANDIDO", "CASSIO RICARDO FALCAO", "CATARINA APARECIDA PINOTTI", "CELIA ATAIDE", "CELIA RAQUEL MONTEIRO", "CELIA TOZZINI",
                "CELINA AGRIPINO", "CELSO ANTONIO FREIRIA", "CELSO AUGUSTO BAIARDO", "CELSO CAMARGOS FERNANDES", "CELSO RICARDO FERREIRA", "CESAR FRANCISCO PEREIRA", "CESAR HENRIQUE SELEGATTI", "CESAR LUIZ MOLINA",
                "CESAR TADASHI ASAKURA", "CHRISTIANE GRANZIERA ZANCO", "CHRISTIANO GREGIO GUIMARAES", "CIBELE CRISTINA LEITE", "CICERO DIMARTINI", "CINTHIA CADONI GONCALVES", "CIRINEU WALTER LUCAS", "CLAITON DONIZETE DIAS",
                "CLAREANA FALCONI MAZOLINI", "CLARICE RODRIGUES CHABREGAS", "CLAUDEMIR MENEZES CORATO", "CLAUDENIR DONIZETI ZAVARIZZE", "CLAUDETE GLACIRA WOLLMANN", "CLAUDETE MIRANDA", "CLAUDIA CRISTIANE NERVA", "CLAUDIA GAZITO FURLANETTO",
                "CLAUDIA ROGERIA SILVA", "CLAUDIO QUINTANA RIBEIRO", "CLAUDIO ROBERTO VIEIRA", "CLAUDIZETI JOSE CHAVES", "CLELIA SILVANA XAVIER", "CLEUNICE OLIVEIRA SOARES", "CLODOALDO CARRILHO", "CONCEICAO APARECIDA GASPARIN",
                "CONSTANTINO BOUHORIS", "CRISTIANE RIBEIRO AMARAL", "CRISTIANE ZANI", "CRISTIANO LUIZ FURIGO", "CRISTINA AMANCIO SILVA", "CRISTINA PULS", "CYNTHIA MARIA TENEDINI", "CYNTIA TALITA LANATOWITZ",
                "DAIANA RAMOS RIBEIRO", "DAIANE CAETANO FRANCISCO", "DAIANE CASAROTTO MOSCA", "DAIANE MARTIM BIANCO", "DAIANY CRISTINA BARBOSA", "DALVA GOMES LUIZ", "DANIEL PUERTA RODRIGUES", "DANIELA AUGUSTA FERREIRA",
                "DANIELA CRISTINA BERG", "DANIELA CRISTINA LOPES", "DANIELA DALBEN MOTA", "DANIELA FERNANDES SILVA", "DANIELA MESTRINER", "DANIELA MIGUEL CRIVELLI", "DANIELA OSELIERO", "DANIELE BATISTA BASSI",
                "DANIELE CRISTINA GOMES", "DANIELE TONIETTI MIGUEL", "DANIELLE ALESSANDRA ALVES", "DAVID BARONE", "DAYANE AMARO COSTA", "DEBORA MARIA DURANTE", "DEJAIR PAIXAO", "DENER LUIZ CATINI",
                "DENILSON ADORNO SCARPITI", "DENILSON CATINI", "DENISE ALESSANDRA RODRIGUES", "DENISE CAMPAGNOLI", "DENISE HELENA COPPO", "DENISE MARIA CAVENAGHI", "DENISE MORAIS FONTENLA", "DEVALDO MARCOLINO DIMARTINI",
                "DIEGO AUGUSTO", "DIEGO HENRIQUE", "DILETA DIAS", "DIRCE FIRMIANO", "DIRCE MARTINEZ CEREGATTI", "DONIZETE MUNIZ BARRETO", "DONIZETE UVEDA MARTINS", "DOUGLAS DIONIZIO FERNANDES",
                "DOUGLAS LUIZ HYPPOLITO", "EDA PIMENTA MOREIRA", "EDINEIA FELIPE APOLINARIO", "EDMARA ALINE PISSINATTI", "EDMARA CHOQUETA SABINO", "EDNA APARECIDA BRUNO", "EDNA APARECIDA PINAFI", "EDNA GUARNIERI RIBEIRO",
                "EDNALVA MARCOS PINTO", "EDNEUSA MIRANDA ATAIDE", "EDSON AUGUSTO JONAS", "EDSON JOSE GANDOLFO", "EDSON VANDERLEI RIBEIRO", "EDUARDO AUGUSTO", "EDUARDO CHABREGAS GUERREIRO", "EDUARDO FERNANDO PIRES",
                "EDUARDO GABRELON", "EDUARDO GIMENEZ MAIA", "EDUARDO GONCALVES BRANDAO", "EDUARDO HENRIQUE", "EDVALDO DONISETI ALIPIO", "ELAINE APARECIDA PORTA", "ELAINE CHOQUETA COURA", "ELAINE CRISTINA CAMPAGNOLI",
                "ELAINE CRISTINA FRANCO", "ELAINE CRISTINA NAVARRO", "ELAINE CRISTINA SECO", "ELAINE MARTINS", "ELAINE SANAVIO MARQUES", "ELDA BARONE", "ELIANA BELLINI", "ELIANA CRISTINA MARTINS",
                "ELIANA RAMIRO MANIEZZO", "ELIANA ZORZETTO LEYN", "ELIANE ALVES BATISTA", "ELIANE VIEIRA BRISENO", "ELIAS BOUHORIS", "ELIENAI FRANCO FERNANDES", "ELISABETE GALDINO ALVES", "ELISABETE MARIA SOARES",
                "ELISABETH CURCIO CATINI", "ELISANGELA CORATO VITOR", "ELISANGELA LANZA MANARA", "ELIZA ELODIA FERREIRA", "ELIZABETH MARIA BUENO", "ELIZABETH REGINA CASTILHO", "ELTON MARCHIORI", "EMERSON ALDEIR DANTAS",
                "EMERSON GOMES BARBOSA", "EMILIO VILELA BERGAMIN", "ENYMAI TELES LUIZ", "ERIKA APARECIDA CIRILO", "ERIKA FORMENTI MINUZZO", "ERIKSON RICARDO ZACARIOTTO", "ESTELA ROSA", "ESTER BARONE",
                "EVA MARIA", "EVALDO RIOS MOREIRA", "EVANILCE APARECIDA FERREIRA", "EVELIN LAIANE VASCONCELOS", "EVERTON BOMBARDA", "EWERTON CESAR DONEGA", "FABIANA BURGOS RIBEIRO", "FABIANA MENDES GONCALVES",
                "FABINA SCARELLI MARANGONI", "FABIO BEZERRA", "FABIO EDUARDO CLEMENTE", "FABIO LUIS CRIVELLI", "FABIO LUIZ VENANCIO", "FABIO SECOLIN", "FABRICIO MOTA MASCARENHAS", "FATIMA APARECIDA CELEGATTI",
                "FATIMA APARECIDA COELHO", "FATIMA APARECIDA RICCI", "FATIMA APARECIDA VIEIRA", "FELIPE MATARAZZO", "FELIPE RAFAEL MATTIOLI", "FERNANDA APARECIDA NEGRI", "FERNANDA GAZITO FURLANETTO", "FERNANDA GUIMARAES JUNQUEIRA",
                "FERNANDA LACERDA", "FERNANDA LIMA PINHEIRO", "FERNANDO ANTOGNOLI BARBOSA", "FERNANDO AUGUSTO", "FERNANDO HENRIQUE", "FLAVIA CRISTHIANE BIELSA", "FLAVIA ELAINE MARINHO", "FLAVIA ROSSI",
                "FLAVIO BARIZON", "FLAVIO LUIZ MANERA", "FLORISA DIONYSIO FERREIRA", "FRANCI INES DIAZ", "FRANCI MARA DAVOLI", "FRANCIELI BARROS MENDES", "FRANCISCO CARLOS", "FRANCISCO JOSE",
                "FRANCISCO MARIANO", "FRANCISCO ROBERTO MARANGONI", "FRANÇOISE SCANAVACHI", "GABRIEL ANASTACIO GOMES", "GABRIEL ANDRADE PINHEIRO", "GABRIEL AUGUSTO", "GABRIEL HENRIQUE", "GABRIEL HENRIQUE PEREIRA",
                "GELMO BATTILANA", "GENI GOMES", "GEORGIA PAULA GONCALVES", "GERTRUDES ISETE TAGLIAFERRO", "GERTRUDES MARQUES RODRIGUES", "GIANE ROBERTO CAETANO", "GILBERTO BORTOLOCI FERREIRA", "GIOVANA FONTES SIQUEIRA",
                "GIOVANNA SILVERIO ALMEIDA", "GISELE PERES BERTHOLDO", "GISLEIDE APARECIDA NUNES", "GISLENE MARINI ANDRADE", "GIULIANO VANUCCI", "GRACIELA ROSSETTO MAGNAN", "GUARACIABA MARIA FERRAZ", "GUERINO FRANCISCO PAVANELLO",
                "GUILHERME AUGUSTO", "GUILHERME HENRIQUE", "GUSTAVO PAVANELLO", "GUSTAVO ROBERTO PIOVESAN", "HELENA CAMPOS LEITE", "HELENICE SARTI RODRIGUES", "HELI MARCOS CORREA", "HELIO FERREIRA",
                "HIRLA MARIA ASSUNCAO", "HRISSA CHRISTINA BOUHORIS", "IARA LUANA GUIDO", "IARA ROSATTO", "IGOR PEREIRA CERRUTI", "INDALECIO DIMARTINI", "IOLANDA LEMOS", "IRENE FELIX",
                "ISABEL ALEXANDRA SERIANI", "ISABEL CRISTINA MANARA", "ISABEL CRISTINA TABERTI", "ISABELA GUARDIA", "ISETE CRISTINA GONÇALVES", "IVAIR LUIZ BIAZOTTO", "IVAN JOSE MARCELINO", "IVETE APARECIDA SECCO",
                "IVONETE REBEQUI", "IZETE APARECIDA FIORITTI", "JADE FRANCOSO LABIGALINI", "JAIR FRANCISCO DIAS", "JANE SILVA BUENO", "JANETE APARECIDA BLEFARI", "JAQUELINE CRISTINA ZANI", "JEAN CARLOS",
                "JEAN PIERRE", "JESSE RIBEIRO CAMARGO", "JESSICA FERNANDA GOMES", "JESSICA MALU ALEXANDRE", "JOAO ANTONIO GALLO", "JOAO BATISTA MARIA", "JOAO BATISTA VICENTE", "JOAO CHAVES JOAQUIM",
                "JOAO LUIZ PEREIRA", "JOAO PAULO GRECCO", "JOAO ROBERTO BRINKER", "JOAQUIM DIAS", "JOCELINA POLETTINE VANZO", "JOEL FERNANDES", "JOICE APARECIDA NEGRO", "JORGE EMILIO GONCALVES",
                "JOSE APARECIDO GOMES", "JOSE CARLOS MOREIRA", "JOSE CARLOS MOZELA", "JOSE CARLOS SIQUEIRA", "JOSE DIMAS PEDRINI", "JOSE DUBAHI PEREIRA", "JOSE EDUARDO FAVERO", "JOSE EDUARDO RODRIGUES",
                "JOSE LUIZ BRIDA", "JOSE LUIZ LAURINDO", "JOSE MARANHO RODRIGUES", "JOSE RAFAEL MAGNAN", "JOSE RINALDI", "JOSE ROBERTO FIRMIANO", "JOSE ROBERTO GOMES", "JOSE VITOR CREMASCO",
                "JOSEMAR MOREIRA", "JOSENILDA LIMA SANTANA", "JOSIANE ALISKI ALVES", "JOSIANE BELLAFONTE", "JOSIANE REGINA CARDOSO", "JOSISLEI ONOFRE TOMAS", "JOSÉ ANTONIO", "JOSÉ CARLOS",
                "JOSÉ MARIA", "JOSÉ ROBERTO", "JOVELINA SOARES CHAVES", "JOVINA BARBOSA CARREIRO", "JOÃO GABRIEL", "JOÃO LUCAS", "JOÃO PEDRO", "JOÃO VICTOR",
                "JOÃO VITOR", "JUCIANY BERTOLINI SILVA", "JULIANA APARECIDA MARTINS", "JULIANA CRISTINA BATAGLIA", "JULIANA GRACINI", "JULIANA MARQUES MODENA", "JULIANA MARTINHO BARBOSA", "JULIANA MONTANARI GIROLDO",
                "JULIANA RIGO TAROSSI", "JULIANE OLBERG GENTIL", "JULIO CESAR BAZZUCCO", "JULIO CESAR LIMA", "JUSSARA APARECIDA BONALDO", "KARINA RONDINI PEREIRA", "KARINE PEREIRA VEDAN", "KATIA BRAZ SERER",
                "KATIA CAROLINO CAMARGO", "KATIA LUCIA FRANCO", "KATIA REGINA CARDOSO", "KATIA WANIA PEDRO", "KELI CRISTINA MODESTO", "KELITA SOUSA LEOCADIO", "KLEBER EDUARDO ESCANAVAQUI", "LARISSA RODRIGUES VICENTE",
                "LAUDEMIRA PEREIRA TAVARES", "LAURA CUTRI ALVES", "LAZARA CRISTIANE RAMOS", "LAZARA LOPES THEODORO", "LEANDRA CIBELE BRIOSO", "LEANDRA TURBIANI FERNANDES", "LEANDRO ANDRE DAVOLI", "LEANDRO HENRIQUE GASPARIN",
                "LEANDRO MARANGON", "LEANDRO RODRIGUES CORREA", "LEILA APARECIDA VALERIO", "LELIA PITON", "LEONARA MARIANO FERREIRA", "LEONARDO AUGUSTO", "LEONARDO HENRIQUE", "LEONILDA FOGACA",
                "LETICIA HERNANDEZ", "LETICIA TEIXEIRA NOGUEIRA", "LIA MARTA VIEIRA", "LIDIA BARONE MARANGONI", "LIDIANE CRISTINA HENRIQUE", "LIGIA BAGIO ESPITTI", "LIGIA MARIA DIAS", "LILIAN KROHLING MARTINHO",
                "LILIAN RENATA GOMES", "LILIANA APARECIDA SANTAMBROSIO", "LILIANE ASSUNCAO", "LIVIA MARIA ANDRADE", "LOURDES LEONELLO BOLDIM", "LUANA CAMILA RAMOS", "LUCAS GABRIEL", "LUCAS GUILHERME BRESCHILIARO",
                "LUCAS HENRIQUE", "LUCAS LUIS VALERIO", "LUCAS PILLA ZORZETTO", "LUCAS THIAGO ZIBORDI", "LUCELENA COCCO", "LUCELENA FURTADO", "LUCIA HELENA LEAL", "LUCIA TEIXEIRA PROCOPIO",
                "LUCIANA APARECIDA GASPARINI", "LUCIANA BOLOGNA", "LUCIANA CAMPOS BARROS", "LUCIANA CRISTINA RODRIGUES", "LUCIANA MARIA ERNESTO", "LUCIANA MORALES GRANADIER", "LUCIANA PEDRETTI SILVA", "LUCIANA SALVADOR",
                "LUCIANE BELETATI", "LUCIANE DOVIGO MOREIRA", "LUCIDALVA REIS MESCENAS", "LUCIENE APARECIDA MARIANO", "LUCIENE CARVALHO TREBESQUI", "LUCIENE MARA MATHIAS", "LUCILENE ZEFERINO TEIXEIRA", "LUCIMARA TUROLA",
                "LUCIMARI GOMES HENRIQUE", "LUDMILLA OLIVEIRA BARBOSA", "LUIS ALBERTO", "LUIS ANTONIO PADOVANI", "LUIS APARECIDO FINOTI", "LUIS CARLOS", "LUIS FELIPE", "LUIS FERNANDO",
                "LUIS GUSTAVO", "LUISA HELENA LEITE", "LUIZ ANTONIO FAEZ", "LUIZ ANTONIO MEIRA", "LUIZ ANTONIO PIVATTO", "LUIZ CARLOS GUARNIERI", "LUIZ CARLOS NUNES", "LUIZ CARLOS PIMENTA",
                "LUIZ CARLOS PINTO", "LUIZ CARLOS VICENTE", "LUIZ CARLOS ZEFERINO", "LUIZ FERNANDO GUARNIERI", "LUIZ FERNANDO SAMBINELLI", "LUIZ HENRIQUE DALBO", "LUIZ HENRIQUE DAMASIO", "LUIZ HENRIQUE GUARDIA",
                "LUIZ ROCHA", "LUIZ RODRIGO SERNAGLIA", "LUZIA BATISTA PONTES", "LUZIA LOPES VENANCIO", "MAGALI MIZAEL", "MAGDA ADORNO BUENO", "MAIARA MAGDA PERCEBOM", "MANOEL ANTONIO SCOPARO",
                "MARA HELOISA GUARNIERI", "MARA LUCIA MENOCCI", "MARCEL HENRIQUE SCATURCHIO", "MARCELA BARBOSA CALEFFI", "MARCELA BERNADETE BUTRICO", "MARCELO ALESSANDRO CAMILO", "MARCELO DONIZETI ROSA", "MARCELO GUERREIRO FERNANDES",
                "MARCELO SOUTO DANTE", "MARCELO TORRICELLI ALVES", "MARCELO WAGNER PILLA", "MARCIA ALESSANDRA MANDAIO", "MARCIA APARECIDA BELTRAMI", "MARCIA APARECIDA ZERBINATTI", "MARCIA BERNARDO GUIMARAES", "MARCIA CUSTODIO CREDITO",
                "MARCIA DAMASCENO PENA", "MARCIA LONGO MANTOVANI", "MARCIA REGINA BUENO", "MARCIO ALEXANDRE CARDOSO", "MARCIO ANDRE PEDRONI", "MARCIO CARVALHO PEREIRA", "MARCIO CORDEIRO", "MARCIO LUIZ LOPES",
                "MARCO ALEXANDRE CASTELLANI", "MARCO ANTONIO GONCALVES", "MARCO ANTONIO XAVIER", "MARCO MIRANDA FERREIRA", "MARCOS ALEX MORINIGO", "MARCOS ANTONIO", "MARCOS ANTONIO BARBOSA", "MARCOS ANTONIO CERRUTI",
                "MARCOS ANTONIO FRANCO", "MARCOS ANTONIO MARQUES", "MARCOS AURELIO ABREU", "MARCOS DONIZETE GOMES", "MARCOS LUIZ COMARIM", "MARCOS ROBERTO DIAS", "MARCOS ROBERTO SOARES", "MARCOS VINICIUS",
                "MARCOS VINICIUS LOPES", "MARGARETE CHOQUETTA", "MARGARETH ASSAD BENEDITO", "MARIA ALVES GOMES", "MARIA AMELIA DOMINGUES", "MARIA AMELIA RODRIGUES", "MARIA APARECIDA ERNESTO", "MARIA APARECIDA MOTTA",
                "MARIA APARECIDA NIERI", "MARIA APARECIDA ROSSI", "MARIA BEATRIZ FRANCISCHINI", "MARIA CECILIA MANCANI", "MARIA CECILIA ROBERTO", "MARIA CLARA", "MARIA CRISTINA DOMINGUES", "MARIA CRISTINA LELLIS",
                "MARIA CRISTINA TAMACIA", "MARIA EDUARDA", "MARIA ELIZA GOULART", "MARIA FERNANDA", "MARIA FERNANDA BRIDI", "MARIA GABRIELA CAZONATO", "MARIA HELENA GOES", "MARIA HELOISA FERREIRA",
                "MARIA JOSE FORTINI", "MARIA JOSÉ", "MARIA JULIA", "MARIA JULIA DAMAS", "MARIA LUISA CAVALCANTE", "MARIA LUIZA", "MARIA LUIZA NIERI", "MARIA LUIZA SBEGHEN",
                "MARIA MARTA SALVADOR", "MARIA PAULA NUNES", "MARIA REGINA MIRANDA", "MARIA SIRLENE BERNARDINELI", "MARIA TERESA ARCHAF", "MARIA TERESINHA GUARNIERI", "MARIANA BREDA TIBANA", "MARIANA CAPITONI REIS",
                "MARILENA GUILHERME DIAS", "MARILENE APARECIDA ROSA", "MARILIA PISSATO FERREIRA", "MARINA BENEDITO", "MARINA ZINETTI NARESSI", "MARINA ZORZETTO BATTAGLIA", "MARINALVA GONCALVES RODRIGUES", "MARINEIDE BOLDORINI BRANDINO",
                "MARIO LUIZ TORATI", "MARISA GONCALVES TORRES", "MARISA MARQUES CAMILO", "MARISTELA SCUDELER", "MARLENE COSETIN", "MARLENE GERLACH", "MARLENE GOMES BIAZOTTO", "MARLENE TOMAS BERALDO",
                "MARLI PICOLI ROCHA", "MARLY TORRES BRIDI", "MARTA MARIA LEAL", "MARTA RIBEIRO ALVES", "MARY LIMA BALECH", "MATHEUS AUGUSTO", "MATHEUS HENRIQUE", "MAURA REGINA PEREIRA",
                "MAURICIO APARECIDO VICENTE", "MAURICIO CALLEGARI CORREIA", "MAURICIO TOMAS", "MAURO ANTONIO CARDOSO", "MAURO ZEURI", "MAYARA APARECIDA SACCINI", "MAYARA THAIS ALVES", "MEIRE TEREZINHA BARREIRO",
                "MELQUIZEDEQUE ROSSI ALVES", "MESSIAS ASSUNCAO", "MICHELE CRISTINA NESPINI", "MICHELE GUARNIERI SILVIDIO", "MIRIA ONOFRE", "MIRIAM JUANITA VIEIRA", "MIRIAN ELISABETE PATRICIO", "MISLENE CRISTINA COLOMBO",
                "MONICA RONCATO", "MORGANA MARIA TERUEL", "MYLENE CAIRO CENKO", "MYRIAN HEYDEN", "NADIA MANTOVANI CELEGATTI", "NADIR APARECIDO GALBI", "NADJA ALMEIDA AYRES", "NAIR DOVIGO",
                "NAJARA CARDOSO ALVES", "NANCI APARECIDA REZENDE", "NANCY FIDALMA DOVIGO", "NATALIA GALVIM CAFÉ", "NATALINO APARECIDO CATINI", "NAZARE APARECIDA RAIMUNDO", "NEIDE CAMPOS", "NEIROBERTO SILVA",
                "NELI ALVES", "NELSON FRANCISCO ZULIANI", "NELSON OLIVEIRA PELICHE", "NEUSA GUERINO REIS", "NEUZA AGRELLA GRANDINI", "NEUZA RAIMUNDA BARBOSA", "NICOLAS SEIJI AOKI", "NIDIA MANTOVANI MARSURA",
                "NILMA APARECIDA TEODORO", "NILTON CEZAR PROCOPIO", "NILZA MARIA CAMPELO", "NIVALDO MARCOLINO DIMARTINI", "NORIVAL DONIZETE PAZZETO", "NORMA ELAINE NEGRETTO", "ODAIR HYPOLITO", "ODINEI NARCISO PANINI",
                "OSMAR JOSE FRANCISCO", "OSVALDO CLODOALDO DOVIGO", "PATRICIA APARECIDA RODRIGUES", "PATRICIA AVANCINI MAESTRE", "PATRICIA BIANCHI INACIO", "PATRICIA RAMOS GARCIA", "PATRICIA REGINA COGUI", "PATRICIA RODRIGUES PEREIRA",
                "PATRICIA SALES DIAS", "PATRICIA SIMAO VIEIRA", "PATRICIA TREVELIN", "PAULO ALEXANDRE MORAES", "PAULO CESAR DELFINO", "PAULO CESAR FERREIRA", "PAULO CESAR MOREIRA", "PAULO CEZAR",
                "PAULO DONIZETI DUARTE", "PAULO HENRIQUE", "PAULO HENRIQUE NARDON", "PAULO HENRIQUE PINTO", "PAULO JOAO", "PAULO OLIVEIRA VENANCIO", "PAULO RENATO LILLI", "PAULO ROBERTO",
                "PAULO ROBERTO TRISTAO", "PAULO SERGIO", "PEDRO GABRIEL", "PEDRO HENRIQUE", "PEDRO JOSE GONCALVES", "PEDRO LUCAS", "PEDRO LUIZ CUNHA", "PLINIO PACOLLA",
                "POLYANNA FERRARI BARSOTINI", "PRISCILA ALVES PINHEIRO", "PRISCILA DUARTE CARDOSO", "PRISCILA FERNANDA BERTO", "PRISCILA LEITE VALLIM", "PRISCILA SILVA ROSA", "RACHEL RONCATO", "RAFAEL AUGUSTO",
                "RAFAEL CAVALHERI", "RAFAEL CRISPIM", "RAFAEL HENRIQUE", "RAFAELA CRISTIANE BENATTI", "RAFAELA PANGALDI LOPES", "RAISSA MARIA RIZOLA", "RAMON ALONÇO", "RAMON REZENDE",
                "RAQUEL GASPAR", "RAQUEL HELEN RISSO", "RAQUEL NORONHA ABREU", "RAYANE RAFAELA BORBA", "REBECA HELENA JANUARIO", "REBECA MINIACI COCCHEVIA", "REGIANE APARECIDA DONEGA", "REGIANE MIGUELASSE NASCIMENTO",
                "REGIANE VIEIRA MACHADO", "REGILAINE RICCI", "REGINA CELIA PAULSEN", "REGINA CELIA PIRES", "REGINA HELENA RIVABEN", "REGINA JULIANI", "REGINA SCHEILA VALEIRO", "REGINALDO ELIAS FERREIRA",
                "REINALDO LUIZ MALTA", "RENAN BENATTI DIAS", "RENATA APARECIDA ANANIAS", "RENATA BAIOCHI", "RENATA CAMPANA CADAN", "RENATA LUIZA MANTOVANI", "RENATA MANTOVANI DUZZI", "RENATA SANTOS LESSA",
                "RINALDO APARECIDO ZANCO", "ROBERTA HELENA SILVEIRA", "ROBERTO ANTONIO FACCIO", "ROBERTO LEMES SILVEIRA", "ROBINSON APARECIDO BETIOL", "ROBSON RICHTER", "RODINEI APARECIDO MANOEL", "RODINEY RICARDO TRESSOLDI",
                "RODRIGO ALVES", "RODRIGO AUGUSTO", "RODRIGO HENRIQUE", "RODRIGO MARTINS ROSA", "RODRIGO PALHA MADUREIRA", "ROGERIO MARCOS GARROS", "ROGERIO ZORZETTO LOPES", "RONALDO ALVES FONSECA",
                "ROSA APARECIDA NOLLI", "ROSA APARECIDA VALERIO", "ROSA ELIANA ZULIANI", "ROSALINA APARECIDA CAVENAGHI", "ROSALINA FERNANDES SERNAGLIA", "ROSANA APARECIDA BAPTISTA", "ROSANA BELOSINI PIERI", "ROSANA CORREA RALHA",
                "ROSANA DIAS GRACINI", "ROSANA ELISABETE VILLANOVA", "ROSANA HELENA NIERI", "ROSANA LOPES SIMOES", "ROSANA MARIA GUARNIERI", "ROSANE PELLEGRINA IMPROTA", "ROSANGELA APARECIDA PEREIRA", "ROSANGELA MARIA BENATTI",
                "ROSANGELA ZINETTI LOPES", "ROSEANE MANCA", "ROSELI ANDRADE", "ROSEMARI RIBEIRO PALMA", "ROSEMARY CEZARONE", "ROSEMARY CRISTINA PINTO", "ROSEMEIRE DONEGA", "ROSEMEIRE ELIZABETH OLIVEIRA",
                "ROSENIS CANDIDA ROSA", "ROSIENE CRISTINA RALHA", "ROSIVAL ANTONIO FRANCATTO", "ROZIANA FATIMA GALIS", "RUI EVERSON GOULART", "SAMANTHA BUENO MARTINI", "SAMARA ALEGRE", "SAMIRA MARIA RAVAGNANI",
                "SAMUEL ANDRE", "SAMUEL ARAUJO SILVA", "SAMUEL MARTINS COELHO", "SANDRA APARECIDA PORPHIRIO", "SANDRA CRISTINA ERNESTO", "SANDRA HELENA PINTO", "SANDRA HELENA PULCINELLI", "SANDRA HELENA SOARES",
                "SANDRA MARA ANDRE", "SANDRA REGINA MORETTO", "SARA REGINA FRANCISCO", "SEOMARA PINTO GUEDES", "SERGIO CESAR PENTEADO", "SERGIO FERNANDO ZAVARIZE", "SERGIO MARQUES", "SERGIO PARENTI",
                "SIDNEI ANTONIO FERREIRA", "SILMARA MORAES PEREIRA", "SILVANA APARECIDA BIANCHINI", "SILVANA BUENO", "SILVANA DONIZETE PINAFFI", "SILVANA HOLANDA MANTELATTO", "SILVANA MARTA BORDIGNON", "SILVANA PIERINA PISSINATTI",
                "SILVANA VIANNA", "SILVESTRE BERALDO", "SILVIA APARECIDA POLYDORO", "SILVIA FERNANDES SANTOS", "SILVIA HELENA MARICCUTI", "SILVIA HELENA RODRIGUES", "SILVIA MARIA SOZZA", "SILVIA REGINA SABINO",
                "SILVIA RODRIGUES GOMES", "SILVIA SAMARA ROQUE", "SIMERIA MACHADO PEREIRA", "SIMONE APARECIDA HILARIO", "SIMONE CAMPOS ZACCARIOTTO", "SIMONE GUERREIRO SILVA", "SIRLENE LOPES CONGENTINO", "SOLANGE BRAGA",
                "SOLANGE FAVALLI INACIO", "SOLANGE MARIA GOULART", "SONIA GARCIA", "SONIA HELENA MELO", "SONIA HELENA SIGNORELLI", "SONIA REGINA MATILE", "SONIA ZEFERINO VISCHI", "SORAIA APOLINARIO",
                "SORAIA COSTA DAVOLI", "SORAYA CRISTINA COSER", "STEFANY FERNANDA PEREIRA", "SUELI APARECIDA GONCALVES", "SUELI CRISTINA SALVATO", "SUZANA PEREIRA PINHEIRO", "SUZANA SEVERINO", "TAISE RIBEIRO VIEIRA",
                "TALITA ANDRESSA RICCI", "TALITA FABIANA CARPANELLI", "TAMIRES FRANCATTO BORSARINI", "TANIA MARA CASTILHO", "TANIA PIMENTA", "TATIANA APARECIDA VICENTE", "TATIANA MARIA LONGO", "TATIANA MOREIRA NOGUEIRA",
                "TATIANE CRISTINA PINHEIRO", "TATIANE LINO", "TATIANE RINCO", "TATIELE MARANHA", "TERESA MILIATI LACERDA", "THAINARA ANTUNES TOLENTINO", "THAIS BRAGA RUBIM", "THAIS DEVITO TRIGO",
                "THAYANE DOMINGOS ROSA", "THAYS HELENA MANGILLI", "TIAGO ROSA SCALON", "VALDECIR FERREIRA", "VALDECIR MARQUES", "VALDEMAR FERREIRA", "VALDIR LUIZ BIAZOTTO", "VALDIR MANTOVANI",
                "VALDIRENE APARECIDA RIBEIRO", "VALDIRENE MARA CORAINI", "VALERIA APARECIDA SIQUEIRA", "VALERIA MUSIGNATO GRANZIERA", "VALERIA ORSI", "VALERIA PALHARES", "VALERIA TORRES APPARECIDO", "VALMIR ROBERTO LADISLAU",
                "VALQUIRIA BENEDITO", "VALQUIRIA LEOPOLDINO", "VALTER EUGENIO MOTTA", "VANDERLANE MACHADO CORREIA", "VANDERLEI DONIZETI VELOZO", "VANDERLEIA MACHADO CORREIA", "VANESSA APARECIDA POLETTINI", "VANESSA MENDES SILLES",
                "VANESSA VICENTE RODRIGUES", "VANIA CRISTINA CORREA", "VANIA LUCIA FERREIRA", "VANIA MARIA SILVEIRA", "VANUZA BUENO BOVELONI", "VERA LUCIA LINO", "VERA LUCIA PACHECO", "VERA LUCIA PEREIRA",
                "VERA LUCIA RESENDE", "VERA LUCIA SILVA", "VERA LUCIA TEIXEIRA", "VICTOR HUGO", "VIRGINIA CERRUTI ALVES", "VITOR HUGO", "VIVIAN CRISTINA GASPARINI", "VIVIAN RAQUEL MODESTO",
                "VIVIANE MARIA BOVELLONI", "VIVIANE NEGRETTO BANDIERA", "VIVIANE SOUZA ALVARENGA", "WAGNER AUGUSTO NEGRI", "WAGNER RIBEIRO VIANA", "WALDEMAR SILVEIRA", "WANDERLEY RIBEIRO CAETANO", "WELLINGTON RODRIGO FRANCO",
                "WILLIAM GALVAO GOMES", "WILLIAN MEIRA NERI", "ZENAIDE COSTA", "ZILPA FERNANDES PEREIRA", "ZORAIDE COSTA MACHADO", "ZORAIDE LEITE MANZAN", "ZULMIRA MARIA ANTONIO",
            };
        
        // Verifica se as duas primeiras palavras formam um nome composto
        if (partes.length >= 2) {
            String possivelNomeComposto = partes[0] + " " + partes[1];
            
            for (String nomeComposto : nomesCompostos) {
                if (possivelNomeComposto.equalsIgnoreCase(nomeComposto)) {
                    return possivelNomeComposto;
                }
            }
        }
        
        // Verifica se as três primeiras palavras formam um nome composto (casos raros)
        if (partes.length >= 3) {
            String possivelNomeTriple = partes[0] + " " + partes[1] + " " + partes[2];
            
            // Alguns casos especiais de nomes triplos
            String[] nomesTriple = {
                "ANA CLARA MARIA", "MARIA ANA CLARA", "JOSÉ MARIA ANTONIO"
            };
            
            for (String nomeTriple : nomesTriple) {
                if (possivelNomeTriple.equalsIgnoreCase(nomeTriple)) {
                    return possivelNomeTriple;
                }
            }
        }
        
        // Se não é um nome composto conhecido, retorna apenas o primeiro nome
        return partes[0];
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