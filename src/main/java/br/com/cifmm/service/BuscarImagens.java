package br.com.cifmm.service;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class BuscarImagens {

    private static final String PASTA_ID = "1b2WhRQA-oxBmqlpqZ1YB5twVzzTZKYIS"; // ID real da pasta

    public BufferedImage buscarImagemPorRE(String re) {
        try {
            // Carrega página HTML da pasta pública
            String url = "https://drive.google.com/drive/folders/" + PASTA_ID;
            Document doc = Jsoup.connect(url).get();

            // Extrai os links de cada arquivo
            Elements scripts = doc.select("script");
            Pattern pattern = Pattern.compile("\"([a-zA-Z0-9_-]{25,})\""); // IDs de arquivos

            for (Element script : scripts) {
                Matcher matcher = pattern.matcher(script.html());
                while (matcher.find()) {
                    String fileId = matcher.group(1);

                    // Aqui você poderia pegar o nome do arquivo e comparar com RE
                    String fileName = getFileNameFromDrive(fileId);
                    if (fileName != null && fileName.contains(re)) {
                        return baixarImagem(fileId);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null; // Não encontrou
    }

    private String getFileNameFromDrive(String fileId) {
        try {
            // URL pública de visualização
            String fileUrl = "https://drive.google.com/file/d/" + fileId + "/view";
            Document doc = Jsoup.connect(fileUrl).get();
            return doc.title(); // Nome do arquivo no título
        } catch (Exception e) {
            return null;
        }
    }

    private BufferedImage baixarImagem(String fileId) {
        try {
            String downloadUrl = "https://drive.google.com/uc?export=download&id=" + fileId;
            try (InputStream in = new URL(downloadUrl).openStream()) {
                return ImageIO.read(in);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}

