package br.com.cifmm.service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;

import io.github.bonigarcia.wdm.WebDriverManager;

@Service
public class GerarQRCode {

    private static final String IMAGES_PATH = "C:/Users/Relogio.ponto/eclipse-workspace/CIFMM2/resources/images/";
    private static final String DOWNLOAD_PATH = "C:\\Users\\Relogio.ponto\\Downloads\\qrcode\\";
    private static final String QR_URL = "https://validar.mogimirim.sp.gov.br/admin/qr/fR8V0M839kpVXT1gXv8SSf5M0wsXEcAC0fyCEnMC6FOa9XC57F1X0qU0K5RM2Lpk";
    private static final int TIMEOUT_SEGUNDOS = 60; // Aumentado para 60 segundos

    public void baixarQRCode(String re) {
        System.out.println("=== INICIANDO DOWNLOAD QR CODE ===");
        System.out.println("RE: " + re);
        System.out.println("URL: " + QR_URL);
        System.out.println("Diretório download: " + DOWNLOAD_PATH);
        System.out.println("Diretório destino: " + IMAGES_PATH);

        validarDiretorioDownload();
        limparDownloadsAnteriores();
        
        WebDriver driver = null;
        try {
            driver = configurarDriver();
            realizarDownloadQR(driver, re);
        } catch (Exception e) {
            registrarErro(driver, re, e);
            throw new RuntimeException("Falha ao baixar QR Code para RE: " + re, e);
        } finally {
            if (driver != null) {
                try {
                    driver.quit();
                } catch (Exception e) {
                    System.err.println("Erro ao fechar driver: " + e.getMessage());
                }
            }
        }
    }

    private void validarDiretorioDownload() {
        File downloadDir = new File(DOWNLOAD_PATH);
        if (!downloadDir.exists()) {
            System.out.println("Criando diretório de download: " + DOWNLOAD_PATH);
            if (!downloadDir.mkdirs()) {
                throw new RuntimeException("Falha ao criar diretório de download: " + DOWNLOAD_PATH);
            }
        }
        
        if (!downloadDir.canWrite()) {
            throw new RuntimeException("Sem permissão de escrita no diretório: " + DOWNLOAD_PATH);
        }
        
        System.out.println("Diretório de download validado: " + downloadDir.getAbsolutePath());
    }

    private void limparDownloadsAnteriores() {
        try {
            File downloadDir = new File(DOWNLOAD_PATH);
            File[] arquivos = downloadDir.listFiles((dir, name) -> 
                name.toLowerCase().endsWith(".png") || 
                name.toLowerCase().endsWith(".jpg") ||
                name.toLowerCase().endsWith(".crdownload"));
            
            if (arquivos != null && arquivos.length > 0) {
                System.out.println("Limpando " + arquivos.length + " arquivo(s) antigo(s)...");
                for (File arquivo : arquivos) {
                    if (arquivo.delete()) {
                        System.out.println("Removido: " + arquivo.getName());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Erro ao limpar downloads anteriores: " + e.getMessage());
        }
    }

    private WebDriver configurarDriver() {
        System.out.println("Configurando WebDriver...");
        
        WebDriverManager.chromedriver().setup();
        
        ChromeOptions options = new ChromeOptions();
        
        // Configurações básicas
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--start-maximized");
        
        // Para debug - remova em produção
        // options.addArguments("--headless=new");
        
        // Configurações de download
        HashMap<String, Object> chromePrefs = new HashMap<>();
        chromePrefs.put("download.default_directory", DOWNLOAD_PATH);
        chromePrefs.put("download.prompt_for_download", false);
        chromePrefs.put("download.directory_upgrade", true);
        chromePrefs.put("safebrowsing.enabled", false);
        chromePrefs.put("safebrowsing.disable_download_protection", true);
        chromePrefs.put("profile.default_content_settings.popups", 0);
        chromePrefs.put("profile.default_content_setting_values.automatic_downloads", 1);
        
        options.setExperimentalOption("prefs", chromePrefs);

        WebDriver driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(TIMEOUT_SEGUNDOS));
        
        System.out.println("WebDriver configurado com sucesso");
        return driver;
    }

    private void realizarDownloadQR(WebDriver driver, String re) throws Exception {
        System.out.println("=== ACESSANDO PÁGINA ===");
        driver.get(QR_URL);
        
        // Aguarda página carregar
        Thread.sleep(3000);
        
        // Debug: imprime título da página
        System.out.println("Título da página: " + driver.getTitle());
        System.out.println("URL atual: " + driver.getCurrentUrl());
        
        preencherFormulario(driver, re);
        submeterFormulario(driver);
        
        File arquivoBaixado = monitorarDownload();
        if (arquivoBaixado != null) {
            moverArquivoParaDestino(arquivoBaixado, re);
        } else {
            throw new RuntimeException("Nenhum arquivo foi baixado dentro do tempo limite de " + TIMEOUT_SEGUNDOS + " segundos");
        }
    }

    private void preencherFormulario(WebDriver driver, String re) {
        try {
            System.out.println("Preenchendo formulário para RE: " + re);
            
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(TIMEOUT_SEGUNDOS));
            
            // Aguarda o textarea estar presente e clicável
            WebElement textarea = wait.until(ExpectedConditions.elementToBeClickable(By.id("Re")));
            
            // Limpa e preenche o textarea
            textarea.clear();
            textarea.sendKeys(re);
            
            System.out.println("✓ Textarea preenchido com RE: " + re);
            
            // Pequena pausa para garantir que o valor foi inserido
            Thread.sleep(500);
            
        } catch (Exception e) {
            System.err.println("Erro ao preencher formulário: " + e.getMessage());
            throw new RuntimeException("Falha ao preencher formulário para RE: " + re, e);
        }
    }

    

   

    private void submeterFormulario(WebDriver driver) {
        try {
            System.out.println("Submetendo formulário...");
            
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(TIMEOUT_SEGUNDOS));
            
            // Encontra o botão de submit
            WebElement botaoSubmit = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("button[type='submit']")));
            
            // Clica no botão
            botaoSubmit.click();
            
            System.out.println("✓ Formulário submetido com sucesso");
            
            // Aguarda o processamento
            Thread.sleep(3000);
            
        } catch (Exception e) {
            System.err.println("Erro ao submeter formulário: " + e.getMessage());
            throw new RuntimeException("Falha ao submeter formulário", e);
        }
    }

    private File monitorarDownload() throws InterruptedException {
        System.out.println("=== MONITORANDO DOWNLOADS ===");
        File downloadDir = new File(DOWNLOAD_PATH);
        
        for (int i = 0; i < TIMEOUT_SEGUNDOS; i++) {
            File[] arquivos = downloadDir.listFiles((dir, name) -> {
                String nameLower = name.toLowerCase();
                return (nameLower.endsWith(".png") || nameLower.endsWith(".jpg")) && 
                       !nameLower.endsWith(".crdownload");
            });
            
            if (arquivos != null && arquivos.length > 0) {
                Arrays.sort(arquivos, Comparator.comparingLong(File::lastModified).reversed());
                File arquivoMaisRecente = arquivos[0];
                
                if (arquivoCompletamenteBaixado(arquivoMaisRecente)) {
                    System.out.println("✓ Arquivo baixado com sucesso: " + arquivoMaisRecente.getName());
                    System.out.println("Tamanho: " + arquivoMaisRecente.length() + " bytes");
                    return arquivoMaisRecente;
                }
            }
            
            if (i % 5 == 0) { // Log a cada 5 segundos
                System.out.println("Aguardando download... (" + (i+1) + "/" + TIMEOUT_SEGUNDOS + "s)");
            }
            Thread.sleep(1000);
        }
        
        System.err.println("✗ Timeout no download após " + TIMEOUT_SEGUNDOS + " segundos");
        return null;
    }

    private boolean arquivoCompletamenteBaixado(File arquivo) throws InterruptedException {
        if (!arquivo.exists() || arquivo.length() == 0) {
            return false;
        }
        
        long tamanhoInicial = arquivo.length();
        Thread.sleep(2000); // Aguarda mais tempo
        
        return arquivo.exists() && arquivo.length() > 0 && tamanhoInicial == arquivo.length();
    }

    private void moverArquivoParaDestino(File arquivoBaixado, String re) throws Exception {
        System.out.println("=== MOVENDO ARQUIVO ===");
        
        Path destino = Paths.get(IMAGES_PATH + re + ".png");
        System.out.println("Origem: " + arquivoBaixado.getAbsolutePath());
        System.out.println("Destino: " + destino.toString());

        // Cria diretório se não existir
        Files.createDirectories(Paths.get(IMAGES_PATH));

        // Remove arquivo existente
        if (Files.exists(destino)) {
            Files.delete(destino);
            System.out.println("Arquivo anterior removido");
        }

        // Move o arquivo
        Files.move(arquivoBaixado.toPath(), destino, StandardCopyOption.REPLACE_EXISTING);
        
        // Verifica se foi movido corretamente
        if (Files.exists(destino) && Files.size(destino) > 0) {
            System.out.println("✓ QR Code movido com sucesso!");
            System.out.println("Tamanho final: " + Files.size(destino) + " bytes");
        } else {
            throw new RuntimeException("Falha na verificação do arquivo movido");
        }
    }

    private void capturarScreenshot(WebDriver driver, String sufixo) {
        try {
            if (driver != null) {
                File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
                String nomeArquivo = "screenshot_" + sufixo + "_" + System.currentTimeMillis() + ".png";
                Path destino = Paths.get(IMAGES_PATH + nomeArquivo);
                Files.copy(screenshot.toPath(), destino, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Screenshot salvo: " + destino);
            }
        } catch (Exception e) {
            System.err.println("Erro ao capturar screenshot: " + e.getMessage());
        }
    }

    private void registrarErro(WebDriver driver, String re, Exception e) {
        System.err.println("=== ERRO NO PROCESSAMENTO ===");
        System.err.println("RE: " + re);
        System.err.println("Erro: " + e.getMessage());
        e.printStackTrace();

        capturarScreenshot(driver, "erro_" + re);
        
        // Log adicional do estado do driver
        try {
            if (driver != null) {
                System.err.println("URL atual: " + driver.getCurrentUrl());
                System.err.println("Título: " + driver.getTitle());
            }
        } catch (Exception ex) {
            System.err.println("Erro ao obter informações do driver: " + ex.getMessage());
        }
    }
}