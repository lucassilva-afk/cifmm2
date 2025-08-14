package br.com.cifmm.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.io.IOException;

public class GerarPDF {
	
	private String frentePath;
	private String versoPath;
	
	public void setFrentePath(String frentePath) {
		this.frentePath = frentePath;
	}
	
	public void setVersoPath(String versoPath) {
        this.versoPath = versoPath;
    }
    
    // Enum para as opções de impressão
    public enum OpcoesImpressao {
        FRENTE,
        VERSO,
        TODOS
    }

    // O método foi modificado para aceitar o enum OpcoesImpressao
    public void generateBadgePDF(String frentePath, String versoPath, OpcoesImpressao opcao) {  	
    	
        try {
            // Crie um novo documento PDF
            PDDocument document = new PDDocument();
            
            System.out.println("Frente: " + frentePath);
            System.out.println("Verso: " + versoPath);
            System.out.println("Opção de impressão: " + opcao);
            
            if (opcao == OpcoesImpressao.FRENTE || opcao == OpcoesImpressao.TODOS) {
                // Lógica para adicionar a página da FRENTE
                PDPage page1 = new PDPage(PDRectangle.A4);
                document.addPage(page1);
                PDImageXObject frontImage = PDImageXObject.createFromFile(frentePath, document);
                try (PDPageContentStream contentStream = new PDPageContentStream(document, page1)) {
                    // Escala e posição da imagem para a página A4
                    float scale = Math.min(595f / frontImage.getWidth(), 842f / frontImage.getHeight());
                    float scaledWidth = frontImage.getWidth() * scale;
                    float scaledHeight = frontImage.getHeight() * scale;
                    float x = (595f - scaledWidth) / 2;
                    float y = (842f - scaledHeight) / 2;
                    contentStream.drawImage(frontImage, x, y, scaledWidth, scaledHeight);
                }
            }

            if (opcao == OpcoesImpressao.VERSO || opcao == OpcoesImpressao.TODOS) {
                // Lógica para adicionar a página do VERSO
                PDPage page2 = new PDPage(PDRectangle.A4);
                document.addPage(page2);
                PDImageXObject backImage = PDImageXObject.createFromFile(versoPath, document);
                try (PDPageContentStream contentStream = new PDPageContentStream(document, page2)) {
                    // Escala e posição da imagem para a página A4
                    float scale = Math.min(595f / backImage.getWidth(), 842f / backImage.getHeight());
                    float scaledWidth = backImage.getWidth() * scale;
                    float scaledHeight = backImage.getHeight() * scale;
                    float x = (595f - scaledWidth) / 2;
                    float y = (842f - scaledHeight) / 2;
                    contentStream.drawImage(backImage, x, y, scaledWidth, scaledHeight);
                }
            }

            // ... (restante da sua lógica para salvar o arquivo) ...
            
            // A lógica de salvamento e seleção do arquivo é a mesma do seu código
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Salvar PDF");
            fileChooser.setSelectedFile(new File("crachas.pdf"));
            fileChooser.setFileFilter(new FileNameExtensionFilter("Arquivos PDF (*.pdf)", "pdf"));

            if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                File arquivoDestino = fileChooser.getSelectedFile();
                if (!arquivoDestino.getName().toLowerCase().endsWith(".pdf")) {
                    arquivoDestino = new File(arquivoDestino.getAbsolutePath() + ".pdf");
                }
                document.save(arquivoDestino);
                JOptionPane.showMessageDialog(null, "PDF gerado com sucesso em: " + arquivoDestino.getAbsolutePath());
            }

            document.close();

        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Erro ao gerar PDF: " + e.getMessage());
        }
    }
}