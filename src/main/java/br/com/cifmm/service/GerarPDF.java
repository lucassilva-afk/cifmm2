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
import java.util.List;

public class GerarPDF {
    
    // Enum para as opções de impressão
    public enum OpcoesImpressao {
        FRENTE,
        VERSO,
        TODOS
    }
    
    // Classe para representar um item selecionado para impressão
    public static class ItemImpressao {
        private String caminhoArquivo;
        private OpcoesImpressao tipo;
        private String nomeArquivo;
        
        public ItemImpressao(String caminhoArquivo, OpcoesImpressao tipo, String nomeArquivo) {
            this.caminhoArquivo = caminhoArquivo;
            this.tipo = tipo;
            this.nomeArquivo = nomeArquivo;
        }
        
        // Getters
        public String getCaminhoArquivo() { return caminhoArquivo; }
        public OpcoesImpressao getTipo() { return tipo; }
        public String getNomeArquivo() { return nomeArquivo; }
        
        public boolean isFrente() {
            return tipo == OpcoesImpressao.FRENTE;
        }
        
        public boolean isVerso() {
            return tipo == OpcoesImpressao.VERSO;
        }
    }

    /**
     * Gera PDF com múltiplos crachás baseado nas seleções do usuário
     * @param itensParaImprimir Lista de itens selecionados pelo usuário
     */
    public void gerarPDFMultiplo(List<ItemImpressao> itensParaImprimir) {
        if (itensParaImprimir == null || itensParaImprimir.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Nenhum item selecionado para impressão!");
            return;
        }
        
        try {
            PDDocument document = new PDDocument();
            
            for (ItemImpressao item : itensParaImprimir) {
                System.out.println("Processando: " + item.getNomeArquivo() + " - Tipo: " + item.getTipo());
                
                if (!new File(item.getCaminhoArquivo()).exists()) {
                    System.err.println("Arquivo não encontrado: " + item.getCaminhoArquivo());
                    continue;
                }
                
                // Adicionar página para cada item selecionado
                adicionarPagina(document, item.getCaminhoArquivo(), 
                    item.getTipo().name() + " - " + item.getNomeArquivo());
            }
            
            if (document.getNumberOfPages() == 0) {
                document.close();
                JOptionPane.showMessageDialog(null, "Nenhuma página foi gerada. Verifique as seleções.");
                return;
            }
            
            // Salvar o documento
            salvarDocumento(document);
            
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Erro ao gerar PDF: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Adiciona uma página individual ao documento
     */
    private void adicionarPagina(PDDocument document, String caminhoImagem, String descricao) throws IOException {
        // Criar página A4 em orientação paisagem (horizontal/deitada)
        PDPage page = new PDPage(new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth()));
        document.addPage(page);
        
        PDImageXObject image = PDImageXObject.createFromFile(caminhoImagem, document);
        
        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
            // Dimensões da página A4 em paisagem
            float pageWidth = PDRectangle.A4.getHeight();  // 842 pontos
            float pageHeight = PDRectangle.A4.getWidth();  // 595 pontos
            
            // Calcular escala para preencher melhor a página
            float scaleX = pageWidth / image.getWidth();
            float scaleY = pageHeight / image.getHeight();
            
            // Usar escala de 140% (1.4f) como você mencionou, mas limitando para não exceder a página
            float scale = Math.min(scaleX, scaleY) * 1.4f;
            
            // Se a escala ficar muito grande, limitar para não ultrapassar a página
            float maxScale = Math.min(scaleX, scaleY) * 0.95f; // 95% da página como limite máximo
            if (scale > maxScale) {
                scale = maxScale;
            }
            
            float scaledWidth = image.getWidth() * scale;
            float scaledHeight = image.getHeight() * scale;
            
            // Centralizar na página
            float x = (pageWidth - scaledWidth) / 2;
            float y = (pageHeight - scaledHeight) / 2;
            
            contentStream.drawImage(image, x, y, scaledWidth, scaledHeight);
            
            System.out.println("Página adicionada (paisagem): " + descricao + 
                              " - Escala aplicada: " + String.format("%.1f%%", scale * 100));
        }
    }
    
    /**
     * Salva o documento PDF
     */
    private void salvarDocumento(PDDocument document) throws IOException {
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setDialogTitle("Salvar PDF dos Crachás");
    fileChooser.setSelectedFile(new File("crachas_selecionados.pdf"));
    fileChooser.setFileFilter(new FileNameExtensionFilter("Arquivos PDF (*.pdf)", "pdf"));
    
    int result = fileChooser.showSaveDialog(null);
    
    if (result == JFileChooser.APPROVE_OPTION) {
        File arquivoDestino = fileChooser.getSelectedFile();
        
        // Garantir extensão .pdf
        if (!arquivoDestino.getName().toLowerCase().endsWith(".pdf")) {
            arquivoDestino = new File(arquivoDestino.getAbsolutePath() + ".pdf");
        }
        
        document.save(arquivoDestino);
        document.close();
        
        JOptionPane.showMessageDialog(null, 
            "PDF gerado com sucesso!\n" + 
            "Páginas: " + document.getNumberOfPages() + " (orientação paisagem)\n" +
            "Local: " + arquivoDestino.getAbsolutePath(), 
            "Sucesso", 
            JOptionPane.INFORMATION_MESSAGE);
            
        System.out.println("PDF salvo em orientação paisagem: " + arquivoDestino.getAbsolutePath());
    } else {
        document.close();
        System.out.println("Operação cancelada pelo usuário");
    }
}
    
    /**
     * Método legado - mantido para compatibilidade
     */
    public void generateBadgePDF(String frentePath, String versoPath, OpcoesImpressao opcao) {
        List<ItemImpressao> itens = new java.util.ArrayList<>();
        
        if (opcao == OpcoesImpressao.FRENTE || opcao == OpcoesImpressao.TODOS) {
            itens.add(new ItemImpressao(frentePath, OpcoesImpressao.FRENTE, "Crachá - Frente"));
        }
        
        if (opcao == OpcoesImpressao.VERSO || opcao == OpcoesImpressao.TODOS) {
            itens.add(new ItemImpressao(versoPath, OpcoesImpressao.VERSO, "Crachá - Verso"));
        }
        
        gerarPDFMultiplo(itens);
    }

	public void setFrentePath(String frentePath) {
		// TODO Auto-generated method stub
		
	}

	public void setVersoPath(String versoPath) {
		// TODO Auto-generated method stub
		
	}
}