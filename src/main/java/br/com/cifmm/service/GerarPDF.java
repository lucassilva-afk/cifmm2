package br.com.cifmm.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.io.IOException;

public class GerarPDF {

    public void generateBadgePDF(String frentePath, String versoPath) {  	
    	
        
        try {
            // Create a new PDF document
            PDDocument document = new PDDocument();
            
            System.out.println("Frente: " + frentePath);
            System.out.println("Verso: " + versoPath);

            // Create page 1 (front of badge)
            PDPage page1 = new PDPage(PDRectangle.A4);
            document.addPage(page1);

            // Load front image
            PDImageXObject frontImage = PDImageXObject.createFromFile(frentePath, document);

            // Create content stream for page 1
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page1)) {
                // Scale and position the image to fit A4 page (595 x 842 points)
                float scale = Math.min(595f / frontImage.getWidth(), 842f / frontImage.getHeight());
                float scaledWidth = frontImage.getWidth() * scale;
                float scaledHeight = frontImage.getHeight() * scale;
                float x = (595f - scaledWidth) / 2; // Center horizontally
                float y = (842f - scaledHeight) / 2; // Center vertically
                contentStream.drawImage(frontImage, x, y, scaledWidth, scaledHeight);
            }

            // Create page 2 (back of badge)
            PDPage page2 = new PDPage(PDRectangle.A4);
            document.addPage(page2);

            // Load back image
            PDImageXObject backImage = PDImageXObject.createFromFile(versoPath, document);

            // Create content stream for page 2
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page2)) {
                // Scale and position the image to fit A4 page
                float scale = Math.min(595f / backImage.getWidth(), 842f / backImage.getHeight());
                float scaledWidth = backImage.getWidth() * scale;
                float scaledHeight = backImage.getHeight() * scale;
                float x = (595f - scaledWidth) / 2; // Center horizontally
                float y = (842f - scaledHeight) / 2; // Center vertically
                contentStream.drawImage(backImage, x, y, scaledWidth, scaledHeight);
            }

            // Create a file chooser dialog
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Salvar PDF do CrachÃ¡");
            fileChooser.setFileFilter(new FileNameExtensionFilter("Arquivos PDF", "pdf"));
            fileChooser.setSelectedFile(new File("cracha.pdf"));

            // Show save dialog and get the selected file
            int userSelection = fileChooser.showSaveDialog(null);
            if (userSelection == JFileChooser.APPROVE_OPTION) {
                File fileToSave = fileChooser.getSelectedFile();
                String filePath = fileToSave.getAbsolutePath();
                // Ensure the file has .pdf extension
                if (!filePath.toLowerCase().endsWith(".pdf")) {
                    filePath += ".pdf";
                }
                // Save the PDF document
                document.save(filePath);
            }

            // Close the document
            document.close();

        } catch (IOException e) {
            e.printStackTrace();
            // Handle the exception (e.g., show an error message to the user)
        }
    }
}