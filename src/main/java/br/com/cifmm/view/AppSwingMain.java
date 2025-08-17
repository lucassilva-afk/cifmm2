package br.com.cifmm.view;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.nio.file.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import br.com.cifmm.service.GerarPDF.ItemImpressao;
import br.com.cifmm.service.GerarPDF.OpcoesImpressao;
import br.com.cifmm.util.REParser;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import br.com.cifmm.control.FuncionarioControl;
import br.com.cifmm.repository.FuncionarioRepository;
import br.com.cifmm.service.BuscarDados;
import br.com.cifmm.service.GerarCrachas;
import br.com.cifmm.service.GerarPDF;
import br.com.cifmm.view.components.ButtonEditor;
import br.com.cifmm.view.components.EditDialog;
import br.com.cifmm.view.components.LoadingScreen;
import br.com.cifmm.view.components.TabelaCallback;
import jakarta.annotation.PostConstruct;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EventObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import javax.swing.SwingUtilities;

@Component
public class AppSwingMain extends JFrame implements TabelaCallback {

    private static final long serialVersionUID = 1L;
    private JPanel contentPane;
    private JTextField textField;
    private final FuncionarioControl funcionarioControl;
    
    @Autowired
    private FuncionarioRepository funcionarioRepository;

    @Autowired 
    private GerarCrachas gerarCrachas;
    
    @Autowired 
    private BuscarDados buscarDados;
  
    
    private WatchService watchService;
    private ExecutorService executorService;
    private Timer timerAtualizacao; // Alternativa mais simples
    private long ultimaModificacaoPasta = 0;
    
    private static final String PROJECT_ROOT = System.getProperty("user.dir");
    
    private static final String OUTPUT_PATH = PROJECT_ROOT + "/output/";

    
    private JTable table_2;
    private JCheckBox chckbxNewCheckBox;
    private JScrollPane scrollPane;        
    private JButton btnNewButton_2;
	private JPanel Main; 

    /**
     * Create the frame.
     */
    public AppSwingMain(FuncionarioControl funcionarioControl) {
    	setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/images/Logo CIFMM.jpg")));
    	setTitle("CIFMM 2.0");
        this.funcionarioControl = funcionarioControl;
        
        
        SwingUtilities.invokeLater(() -> {
            garantirPastaOutput();
            iniciarMonitoramentoArquivos();
            
        });
    }
    
    @PostConstruct // Ou você pode chamar manualmente após obter o bean do Spring
    public void initializeUI() {
        SwingUtilities.invokeLater(() -> {
            initUI();
            setVisible(true);
        });
    }

    private void iniciarMonitoramentoArquivos() {
        try {
            watchService = FileSystems.getDefault().newWatchService();
            Path outputDir = Paths.get(OUTPUT_PATH);
            
            if (!Files.exists(outputDir)) {
                Files.createDirectories(outputDir);
            }
            
            outputDir.register(watchService, 
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_DELETE,
                StandardWatchEventKinds.ENTRY_MODIFY);
            
            executorService = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "FileMonitor");
                t.setDaemon(true); // Thread daemon para não impedir o fechamento da aplicação
                return t;
            });
            
            executorService.submit(this::monitorarMudancasArquivos);
            
            System.out.println("✅ Monitoramento de arquivos iniciado para: " + OUTPUT_PATH);
            
        } catch (Exception e) {
            System.err.println("❌ Erro ao inicializar monitoramento: " + e.getMessage());
            // Fallback para Timer se WatchService falhar
            iniciarTimerAtualizacao();
        }
    }
    
    private void toggleSelectAll(boolean selected, JCheckBox chckbxNewCheckBox) {
		DefaultTableModel model = (DefaultTableModel) table_2.getModel();
        
		for (int row = 0; row < model.getRowCount(); row++) {
	        model.setValueAt(selected, row, 0); // Coluna 0 é a única de checkbox
	    }
        
        // Atualizar o texto do checkbox principal
        if (selected) {
        	chckbxNewCheckBox.setText("Deselecionar Todos");
        } else {
        	chckbxNewCheckBox.setText("Selecionar Todos");
        }
        
        // Atualizar a visualizaÃ§Ã£o da tabela
        table_2.repaint();
		
	}
    
    private List<File> getAllImageFiles() {
        List<File> images = new ArrayList<>();
        try {
            File folder = new File(OUTPUT_PATH);
            if (folder.exists() && folder.isDirectory()) {
                File[] files = folder.listFiles((dir, name) ->
                    name.toLowerCase().endsWith(".png") ||
                    name.toLowerCase().endsWith(".jpg") ||
                    name.toLowerCase().endsWith(".jpeg")
                );

                if (files != null) {
                    Arrays.sort(files); // opcional: ordenar por nome
                    images.addAll(Arrays.asList(files));
                }
            } else {
                System.err.println("Pasta não encontrada: " + OUTPUT_PATH);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return images;
    }
    
    private void monitorarMudancasArquivos() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                WatchKey key = watchService.take();
                
                boolean mudancaDetectada = false;
                
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    
                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }
                    
                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    Path fileName = ev.context();
                    String nomeArquivo = fileName.toString().toLowerCase();
                    
                    // Verifica se é um arquivo de imagem
                    if (nomeArquivo.endsWith(".png") || 
                        nomeArquivo.endsWith(".jpg") || 
                        nomeArquivo.endsWith(".jpeg")) {
                        
                        System.out.println("🔍 Mudança detectada: " + kind.name() + " - " + fileName);
                        mudancaDetectada = true;
                    }
                }
                
                if (mudancaDetectada) {
                    // Aguarda um pouco para garantir que o arquivo foi completamente escrito
                    Thread.sleep(1500);
                    
                    // Atualiza a tabela na EDT
                    SwingUtilities.invokeLater(this::atualizarTabela);
                }
                
                boolean valid = key.reset();
                if (!valid) {
                    break;
                }
            }
        } catch (InterruptedException e) {
            System.out.println("⏹️ Monitoramento de arquivos interrompido");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println("❌ Erro no monitoramento: " + e.getMessage());
        }
    }
    
    
    
    public void atualizarTabelaTempoReal() {
        try {
            System.out.println("Atualizando tabela...");

            // Recarrega os arquivos da pasta
            List<File> imageFiles = getImageFilesFromFolder(OUTPUT_PATH);

            // Obtém o modelo da tabela
            DefaultTableModel model = (DefaultTableModel) table_2.getModel();

            // Limpa a tabela atual
            model.setRowCount(0);

            // Adiciona os novos arquivos
            for (File imageFile : imageFiles) {
                // O modelo da tabela deve ter 4 colunas: Frente, Verso, Fotos, Editar
                Object[] rowData = new Object[3]; // Corrigido para 4 colunas

                rowData[0] = false; // Checkbox 'Frente' desmarcado
                rowData[0] = false; // Checkbox 'Verso' desmarcado

                // Cria um ImageIcon a partir do caminho do arquivo
                ImageIcon icon = new ImageIcon(imageFile.getAbsolutePath());
                // Armazena o caminho na descrição do ícone, o que pode ser útil mais tarde
                icon.setDescription(imageFile.getAbsolutePath());
                // Adiciona o ImageIcon na coluna correta, que é a de índice 2
                rowData[1] = icon; // Imagem na coluna 2

                rowData[2] = "Editar"; // Botão na coluna 3

                model.addRow(rowData);
            }

            // Atualiza o estado do checkbox master
            updateMasterCheckBox();

            // Força a repintagem da tabela
            table_2.revalidate();
            table_2.repaint();

            System.out.println("Tabela atualizada com " + imageFiles.size() + " arquivos");

        } catch (Exception e) {
            System.err.println("Erro ao atualizar tabela: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private boolean arquivoExisteEstaCompleto(String caminho) {
        try {
            File arquivo = new File(caminho);
            if (!arquivo.exists()) {
                return false;
            }
            
            // Verifica se o arquivo não está sendo escrito
            long tamanho1 = arquivo.length();
            Thread.sleep(100);
            long tamanho2 = arquivo.length();
            
            return tamanho1 == tamanho2 && tamanho1 > 0;
        } catch (Exception e) {
            return false;
        }
    }
    
    private void garantirPastaOutput() {
        File pastaOutput = new File(OUTPUT_PATH);
        if (!pastaOutput.exists()) {
            if (pastaOutput.mkdirs()) {
                System.out.println("📁 Pasta output criada: " + OUTPUT_PATH);
            } else {
                System.err.println("❌ Erro ao criar pasta output: " + OUTPUT_PATH);
            }
        }
    }

	// Renderer for images in the table
    private static class ImageRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        public java.awt.Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            java.awt.Component c = super.getTableCellRendererComponent(table, "",
                    isSelected, hasFocus, row, column);

            if (c instanceof JLabel) {
                JLabel label = (JLabel) c;
                label.setHorizontalAlignment(JLabel.CENTER);

                if (value instanceof ImageIcon) { // <-- The code checks if the value is an ImageIcon
                    ImageIcon icon = (ImageIcon) value;
                    Image img = icon.getImage().getScaledInstance(500, 370, Image.SCALE_SMOOTH);
                    label.setIcon(new ImageIcon(img));
                } else {
                    label.setIcon(null);
                    System.err.println("Invalid value for ImageRenderer at row " + row + ": " + (value != null ? value.getClass() : "null")); // <-- If it's not, it prints the error
                }
            }
            return c;
        }
    }

    // Renderer for buttons in the table
    private static class CheckBoxRenderer extends JCheckBox implements TableCellRenderer {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public CheckBoxRenderer() {
            setHorizontalAlignment(JLabel.CENTER);
            setOpaque(true);
        }

        @Override
        public java.awt.Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {

            if (value instanceof Boolean) {
                setSelected((Boolean) value);
            } else {
                setSelected(false);
            }

            if (isSelected) {
                setBackground(table.getSelectionBackground());
                setForeground(table.getSelectionForeground());
            } else {
                setBackground(table.getBackground());
                setForeground(table.getForeground());
            }
            return this;
        }
    }
    
 // O seu método para processar a impressão (exemplo)
    public void processSelectedRows() {
        DefaultTableModel model = (DefaultTableModel) table_2.getModel();

        for (int i = 0; i < model.getRowCount(); i++) {
            boolean imprimirFrente = (boolean) model.getValueAt(i, 0); // Coluna "Frente"
            boolean imprimirVerso = (boolean) model.getValueAt(i, 1);  // Coluna "Verso"

            if (imprimirFrente || imprimirVerso) {
                // Obtenha os caminhos dos arquivos da frente e do verso para esta linha
                // ... sua lógica para obter o caminho do arquivo com base na linha ...
                
                // Aqui você deve adaptar para obter o caminho do arquivo correto.
                String nomeArquivoFrente = "caminho_para_o_arquivo_da_frente_da_linha_" + i + ".png";
                String nomeArquivoVerso = "caminho_para_o_arquivo_do_verso_da_linha_" + i + ".png";

                GerarPDF.OpcoesImpressao opcaoSelecionada;
                if (imprimirFrente && imprimirVerso) {
                    opcaoSelecionada = GerarPDF.OpcoesImpressao.TODOS;
                } else if (imprimirFrente) {
                    opcaoSelecionada = GerarPDF.OpcoesImpressao.FRENTE;
                } else {
                    opcaoSelecionada = GerarPDF.OpcoesImpressao.VERSO;
                }

                GerarPDF gerador = new GerarPDF();
                gerador.generateBadgePDF(nomeArquivoFrente, nomeArquivoVerso, opcaoSelecionada);
            }
        }
        JOptionPane.showMessageDialog(this, "Processo de impressão concluído.");
    }

    
    public List<Integer> getSelectedRows() {
        List<Integer> selectedRows = new ArrayList<>();
        DefaultTableModel model = (DefaultTableModel) table_2.getModel(); // Funciona porque table_2 Ã© variÃ¡vel de instÃ¢ncia
        
        for (int row = 0; row < model.getRowCount(); row++) {
            Object value = model.getValueAt(row, 0);
            if (value instanceof Boolean && (Boolean) value) {
                selectedRows.add(row);
            }
        }
        
        return selectedRows;
    }

	private static class CheckBoxEditor extends DefaultCellEditor {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private AppSwingMain parent;
        
        public CheckBoxEditor(AppSwingMain appSwingMain) {
            super(new JCheckBox());
            this.parent = parent;
            JCheckBox checkBox = (JCheckBox) getComponent();
            checkBox.setHorizontalAlignment(JLabel.CENTER);
            
            // Adicionar listener para detectar mudanÃ§as
            checkBox.addActionListener(e -> {
                // Usar SwingUtilities para executar apÃ³s a mudanÃ§a ser aplicada
                SwingUtilities.invokeLater(() -> {
                    if (parent != null) {
                        parent.updateMasterCheckBox();
                    }
                });
            });
        }
    }
	
    // Editor for buttons in the table
    private static class ButtonRenderer extends JButton implements TableCellRenderer {
        private static final long serialVersionUID = 1L;
        
        private JButton button;
        private JPanel panel;

        public ButtonRenderer() {
        	button = new JButton();
        	button.setOpaque(true);
        	button.setHorizontalAlignment(JLabel.CENTER);
            button.setVerticalAlignment(JLabel.CENTER); 
        	
            panel = new JPanel(new GridBagLayout());
			panel.setOpaque(true);	
            
			panel.add(button, new GridBagConstraints());
			
        }

        @Override
        public java.awt.Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            
            button.setText("Editar");
            
            // Carregar e definir a imagem
            try {
                ImageIcon icon = new ImageIcon(getClass().getResource("/images/pencil-square.png"));
                // Redimensionar a imagem para ficar pequena
                Image img = icon.getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH);
                button.setIcon(new ImageIcon(img));
            } catch (Exception e) {
                System.err.println("Erro ao carregar imagem: " + e.getMessage());
            }
            
            // Configurar tamanho pequeno do botÃ£o
            button.setPreferredSize(new Dimension(120, 30));

            
            // Configurar cores baseado na seleÃ§Ã£o
            if (isSelected) {
                panel.setBackground(table.getSelectionBackground());
                button.setBackground(table.getSelectionBackground());
                button.setForeground(table.getSelectionForeground());
            } else {
                panel.setBackground(table.getBackground());
                button.setBackground(UIManager.getColor("Button.background"));
                button.setForeground(UIManager.getColor("Button.foreground"));
            }
            
            return panel; // Retorna o painel em vez do botÃ£o diretamente
        }
    }
    
 // EDITOR - para quando clicar no botÃ£o
   
    
    
    // Method to get all image files from the output folder
    private List<File> getImageFilesFromFolder(String folderPath) {
        File folder = new File(folderPath);

        if (!folder.exists() || !folder.isDirectory()) {
            System.err.println("📁 Diretório não encontrado: " + folderPath);
            return new ArrayList<>();
        }

        File[] files = folder.listFiles((dir, name) -> {
            String nameLower = name.toLowerCase();
            return nameLower.endsWith(".png") ||
                   nameLower.endsWith(".jpg") ||
                   nameLower.endsWith(".jpeg");
        });

        if (files == null || files.length == 0) {
            return new ArrayList<>();
        }

        // Usar um mapa para agrupar arquivos por RE
        Map<String, List<File>> crachasMap = new HashMap<>();

        for (File file : files) {
            String fileName = file.getName();
            // Assume que o nome do arquivo tem o formato "RE_..._frente.png" ou "RE_..._verso.png"
            // Ou seja, o RE é a primeira parte do nome
            String[] parts = fileName.split("_");
            if (parts.length > 1) {
                String re = parts[0];
                crachasMap.computeIfAbsent(re, k -> new ArrayList<>()).add(file);
            }
        }

        // Criar a lista final ordenada
        List<File> imageFiles = new ArrayList<>();
        
        // Pegar as chaves (REs) e ordenar
        List<String> sortedRes = new ArrayList<>(crachasMap.keySet());
        sortedRes.sort(Comparator.naturalOrder()); // Ordenar os REs para garantir consistência

        for (String re : sortedRes) {
            List<File> filesForRe = crachasMap.get(re);
            // Ordenar os arquivos de cada crachá para que "frente" venha antes de "verso"
            filesForRe.sort(Comparator.comparing(File::getName));
            imageFiles.addAll(filesForRe);
        }

        return imageFiles;
    }
    
    private void iniciarTimerAtualizacao() {
        // Inicializa a última modificação
        File pastaOutput = new File(OUTPUT_PATH);
        if (pastaOutput.exists()) {
            ultimaModificacaoPasta = obterUltimaModificacaoPasta();
        }
        
        // Cria timer que verifica a cada 3 segundos
        timerAtualizacao = new Timer(3000, e -> verificarMudancasPasta());
        timerAtualizacao.start();
        System.out.println("⏰ Timer de atualização iniciado (verificando a cada 3s)");
    }
    
    
    
    private long obterUltimaModificacaoPasta() {
        File pastaOutput = new File(OUTPUT_PATH);
        long ultimaModificacao = 0;
        
        if (pastaOutput.exists()) {
            ultimaModificacao = pastaOutput.lastModified();
            
            File[] arquivos = pastaOutput.listFiles((dir, name) -> {
                String nameLower = name.toLowerCase();
                return nameLower.endsWith(".png") || 
                       nameLower.endsWith(".jpg") || 
                       nameLower.endsWith(".jpeg");
            });
            
            if (arquivos != null) {
                for (File arquivo : arquivos) {
                    if (arquivo.lastModified() > ultimaModificacao) {
                        ultimaModificacao = arquivo.lastModified();
                    }
                }
            }
        }
        
        return ultimaModificacao;
    }

    private void verificarMudancasPasta() {
        try {
            long modificacaoAtual = obterUltimaModificacaoPasta();
            
            if (modificacaoAtual > ultimaModificacaoPasta) {
                ultimaModificacaoPasta = modificacaoAtual;
                System.out.println("🔄 Mudança detectada via Timer, atualizando tabela...");
                atualizarTabela();
            }
        } catch (Exception e) {
            System.err.println("Erro ao verificar mudanças: " + e.getMessage());
        }
    }
    
    private void pararTimer() {
        if (timerAtualizacao != null && timerAtualizacao.isRunning()) {
            timerAtualizacao.stop();
            System.out.println("⏹️ Timer parado");
        }
    }
    
    @Override
    public void atualizarTabela() {
        // Este método já existe no seu App.java, só precisa implementar a interface
        try {
            System.out.println("🔄 Atualizando tabela via callback...");
            List<File> imageFiles = getImageFilesFromFolder(OUTPUT_PATH);
            DefaultTableModel model = (DefaultTableModel) table_2.getModel();
            
            // Salvar estado dos checkboxes por caminho do arquivo
            Map<String, Boolean> estadoCheckboxes = new HashMap<>();
            
            for (int i = 0; i < model.getRowCount(); i++) {
                Object valor = model.getValueAt(i, 1); // Coluna da imagem
                if (valor instanceof ImageIcon) {
                    ImageIcon icon = (ImageIcon) valor;
                    String caminho = icon.getDescription();
                    Boolean selecionado = (Boolean) model.getValueAt(i, 0);
                    if (caminho != null) {
                        estadoCheckboxes.put(new File(caminho).getName(), selecionado);
                    }
                }
            }

            // Limpar e recarregar a tabela
            model.setRowCount(0);

            // Para cada arquivo de imagem, criar uma linha na tabela
            for (File imageFile : imageFiles) {
                Object[] rowData = new Object[3];
                String nomeArquivo = imageFile.getName();
                
                // Restaurar estado anterior ou usar false como padrão
                Boolean estadoAnterior = estadoCheckboxes.get(nomeArquivo);
                rowData[0] = estadoAnterior != null ? estadoAnterior : false; // Checkbox
                
                // Carregar a imagem
                ImageIcon icon = new ImageIcon(imageFile.getAbsolutePath());
                icon.setDescription(imageFile.getAbsolutePath()); // Armazenar caminho completo
                rowData[1] = icon; // Coluna da imagem
                
                rowData[2] = "Editar"; // Botão

                model.addRow(rowData);
            }

            updateMasterCheckBox();
            table_2.revalidate();
            table_2.repaint();
            System.out.println("✅ Tabela atualizada via callback - " + imageFiles.size() + " arquivos encontrados");
            
        } catch (Exception e) {
            System.err.println("❌ Erro ao atualizar tabela via callback: " + e.getMessage());
            e.printStackTrace();
        }
    }
	
    public void updateMasterCheckBox() {
        if (areAllSelected()) {
            chckbxNewCheckBox.setSelected(true);
            chckbxNewCheckBox.setText("Deselecionar Todos");
        } else if (areAllSelected()) {
            chckbxNewCheckBox.setSelected(false);
            chckbxNewCheckBox.setText("Selecionar Todos");
        } else {
            chckbxNewCheckBox.setSelected(false);
            chckbxNewCheckBox.setText("Selecionar Todos");
        }
    }
    
    private boolean areAllSelected() {
    	DefaultTableModel model = (DefaultTableModel) table_2.getModel();
	    for (int i = 0; i < model.getRowCount(); i++) {
	        // Verifique apenas a coluna 0
	        if (!(Boolean) model.getValueAt(i, 0)) {
	            return false;
	        }
	    }
	    return true;
	}

	private List<ItemImpressao> obterItensParaImpressao() {
        List<ItemImpressao> itens = new ArrayList<>();
        DefaultTableModel model = (DefaultTableModel) table_2.getModel();
        
        for (int row = 0; row < model.getRowCount(); row++) {
            boolean selecionado = (Boolean) model.getValueAt(row, 0);
            
            if (selecionado) {
                // Obter o caminho do arquivo da descrição do ícone
                Object iconObj = model.getValueAt(row, 1);
                if (iconObj instanceof ImageIcon) {
                    ImageIcon icon = (ImageIcon) iconObj;
                    String caminhoArquivo = icon.getDescription();
                    
                    if (caminhoArquivo != null && new File(caminhoArquivo).exists()) {
                        // Determinar se é frente ou verso baseado no nome do arquivo
                        String nomeArquivo = new File(caminhoArquivo).getName();
                        OpcoesImpressao tipo = determinarTipoArquivo(nomeArquivo);
                        
                        ItemImpressao item = new ItemImpressao(
                            caminhoArquivo,
                            tipo,
                            nomeArquivo
                        );
                        
                        itens.add(item);
                    }
                }
            }
        }
        
        return itens;
    }
	
	private OpcoesImpressao determinarTipoArquivo(String nomeArquivo) {
	    String nomeMinusculo = nomeArquivo.toLowerCase();
	    
	    if (nomeMinusculo.contains("frente")) {
	        return OpcoesImpressao.FRENTE;
	    } else if (nomeMinusculo.contains("verso")) {
	        return OpcoesImpressao.VERSO;
	    } else {
	        // Se não conseguir determinar, assume frente como padrão
	        return OpcoesImpressao.FRENTE;
	    }
	}


	private void initUI() {
	    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	    setBounds(100, 100, 1391, 796);
	    contentPane = new JPanel();
	    contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
	    setContentPane(contentPane);
	    contentPane.setLayout(new BorderLayout(0, 0));

	    // ===========================================
	    // HEADER PANEL
	    // ===========================================
	    JPanel Header = new JPanel();
	    Header.setBorder(new LineBorder(new Color(192, 192, 192)));
	    contentPane.add(Header, BorderLayout.NORTH);

	    JLabel lblNewLabel = new JLabel("");
	    lblNewLabel.setIcon(new ImageIcon(getClass().getResource("/images/logo.png")));
	    
	    JLabel lblNewLabel_2 = new JLabel("");
	    lblNewLabel_2.setHorizontalAlignment(SwingConstants.TRAILING);
	    lblNewLabel_2.setIcon(new ImageIcon(getClass().getResource("/images/Logo_Header.png")));
	    
	    GroupLayout gl_Header = new GroupLayout(Header);
	    gl_Header.setHorizontalGroup(
	        gl_Header.createParallelGroup(GroupLayout.Alignment.LEADING)
	            .addGroup(GroupLayout.Alignment.TRAILING, gl_Header.createSequentialGroup()
	                .addContainerGap()
	                .addComponent(lblNewLabel_2, GroupLayout.PREFERRED_SIZE, 185, GroupLayout.PREFERRED_SIZE)
	                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 908, Short.MAX_VALUE)
	                .addComponent(lblNewLabel)
	                .addContainerGap())
	    );
	    gl_Header.setVerticalGroup(
	        gl_Header.createParallelGroup(GroupLayout.Alignment.LEADING)
	            .addGroup(gl_Header.createSequentialGroup()
	                .addGap(5)
	                .addGroup(gl_Header.createParallelGroup(GroupLayout.Alignment.LEADING)
	                    .addComponent(lblNewLabel_2)
	                    .addComponent(lblNewLabel))
	                .addContainerGap())
	    );
	    Header.setLayout(gl_Header);

	    // ===========================================
	    // SIDEBAR PANEL
	    // ===========================================
	    JPanel SideBar = new JPanel();
	    SideBar.setBorder(new MatteBorder(0, 1, 0, 1, new Color(192, 192, 192)));
	    FlowLayout flowLayout_1 = (FlowLayout) SideBar.getLayout();
	    flowLayout_1.setHgap(25);
	    contentPane.add(SideBar, BorderLayout.WEST);

	    JButton btnNewButton = new JButton("Gerar Crachas");
	    btnNewButton.setIcon(null);
	    SideBar.add(btnNewButton);

	    // ===========================================
	    // MAIN PANEL COM GROUPLAYOUT
	    // ===========================================
	    JPanel Main = new JPanel();
	    contentPane.add(Main, BorderLayout.CENTER);
	    
	    // Criação dos componentes do Main
	    textField = new JTextField();
	    textField.setColumns(10);
	    textField.setVisible(false);

	    JLabel lblNewLabel_1 = new JLabel("Digite o RE:");
	    lblNewLabel_1.setFont(new Font("Tahoma", Font.PLAIN, 20));
	    lblNewLabel_1.setVisible(false);

	    JButton btnNewButton_1 = new JButton("Buscar");
	    btnNewButton_1.setVisible(false);
	    
	    chckbxNewCheckBox = new JCheckBox("Selecionar Todos");
	    chckbxNewCheckBox.setFont(new Font("Tahoma", Font.PLAIN, 15));
	    chckbxNewCheckBox.setVisible(false);
	    
	    // ScrollPane da tabela
	    scrollPane = new JScrollPane();
	    
	    btnNewButton_2 = new JButton("Gerar PDF");
	    btnNewButton_2.setVisible(false);

	    // ===========================================
	    // CONFIGURAÇÃO DA TABELA
	    // ===========================================
	    List<File> imageFiles = getAllImageFiles();

	    // Create data for the table
	    Object[][] data = new Object[imageFiles.size()][3];
	    for (int i = 0; i < imageFiles.size(); i++) {
	        File imageFile = imageFiles.get(i);
	        data[i][0] = true;  // Checkbox
	        
	        // Criar ImageIcon para a imagem
	        ImageIcon icon = new ImageIcon(imageFile.getAbsolutePath());
	        icon.setDescription(imageFile.getAbsolutePath());
	        data[i][1] = icon;
	        data[i][2] = "Editar"; // Botão
	    }

	    table_2 = new JTable();
	    table_2.setModel(new DefaultTableModel(data, new String[] {
	        "Selecionar", "Fotos", "Editar"
	    }) {
	        @Override
	        public Class<?> getColumnClass(int columnIndex) {
	            if (columnIndex == 0) {
	                return Boolean.class;
	            }
	            return super.getColumnClass(columnIndex);
	        }
	    });
	    
	    // Configuração do renderizador e editor
	    table_2.getColumnModel().getColumn(0).setCellRenderer(new CheckBoxRenderer());
	    table_2.getColumnModel().getColumn(0).setCellEditor(new CheckBoxEditor(this));	
	    table_2.getColumnModel().getColumn(1).setCellRenderer(new ImageRenderer());
	    table_2.getColumnModel().getColumn(2).setCellRenderer(new ButtonRenderer());
	    
	    ButtonEditor buttonEditor = new ButtonEditor(
	            table_2, 
	            "Editar", 
	            "EDIT",
	            funcionarioRepository,
	            gerarCrachas,
	            buscarDados,
	            this  // <- Passa esta instância como callback
	        );
	    
	    table_2.getColumnModel().getColumn(2).setCellEditor(buttonEditor);	    

	    // Configura a largura das colunas
	    table_2.getColumnModel().getColumn(0).setPreferredWidth(100);
	    table_2.getColumnModel().getColumn(1).setPreferredWidth(400);
	    table_2.getColumnModel().getColumn(2).setPreferredWidth(100);
	    table_2.setRowHeight(370);
	    
	    scrollPane.setViewportView(table_2);
	    scrollPane.setVisible(false);

	    // ===========================================
	    // GROUPLAYOUT PARA O PAINEL MAIN
	    // ===========================================
	    GroupLayout gl_Main = new GroupLayout(Main);
	    Main.setLayout(gl_Main);

	    // Layout Horizontal (esquerda para direita)
	    gl_Main.setHorizontalGroup(
	        gl_Main.createParallelGroup(GroupLayout.Alignment.LEADING)
	            .addGroup(gl_Main.createSequentialGroup()
	                .addContainerGap()
	                .addGroup(gl_Main.createParallelGroup(GroupLayout.Alignment.LEADING)
	                    // Label "Digite o RE:"
	                    .addComponent(lblNewLabel_1)
	                    // Campo de texto (ocupa toda largura disponível)
	                    .addComponent(textField, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
	                    // Linha com checkbox e botão buscar
	                    .addGroup(gl_Main.createSequentialGroup()
	                        .addComponent(chckbxNewCheckBox)
	                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
	                        .addComponent(btnNewButton_1))
	                    // ScrollPane da tabela
	                    .addGroup(gl_Main.createSequentialGroup()
	                        .addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 1000, Short.MAX_VALUE)
	                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
	                        .addComponent(btnNewButton_2)))
	                .addContainerGap())
	    );

	    // Layout Vertical (cima para baixo)
	    gl_Main.setVerticalGroup(
	        gl_Main.createParallelGroup(GroupLayout.Alignment.LEADING)
	            .addGroup(gl_Main.createSequentialGroup()
	                .addContainerGap()
	                // Label "Digite o RE:"
	                .addComponent(lblNewLabel_1)
	                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
	                // Campo de texto
	                .addComponent(textField, GroupLayout.PREFERRED_SIZE, 41, GroupLayout.PREFERRED_SIZE)
	                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
	                // Linha com checkbox e botão buscar
	                .addGroup(gl_Main.createParallelGroup(GroupLayout.Alignment.BASELINE)
	                    .addComponent(chckbxNewCheckBox)
	                    .addComponent(btnNewButton_1))
	                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
	                // ScrollPane (ocupa o espaço restante) e botão PDF
	                .addGroup(gl_Main.createParallelGroup(GroupLayout.Alignment.LEADING)
	                    .addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 500, Short.MAX_VALUE)
	                    .addGroup(GroupLayout.Alignment.TRAILING, gl_Main.createSequentialGroup()
	                        .addGap(0, 0, Short.MAX_VALUE)
	                        .addComponent(btnNewButton_2)))
	                .addContainerGap())
	    );

	    // ===========================================
	    // EVENT LISTENERS
	    // ===========================================
	    
	    // Listener do checkbox "Selecionar Todos"
	    chckbxNewCheckBox.addActionListener(e -> {
	        boolean isSelected = chckbxNewCheckBox.isSelected();
	        DefaultTableModel model = (DefaultTableModel) table_2.getModel();
	        
	        for (int i = 0; i < model.getRowCount(); i++) {
	            model.setValueAt(isSelected, i, 0); 
	        }
	    });
	    
	    // Listener do botão "Gerar Crachas"
	    btnNewButton.addActionListener(new ActionListener() {
	        public void actionPerformed(ActionEvent e) {
	            lblNewLabel_1.setVisible(true);
	            textField.setVisible(true);
	            btnNewButton_1.setVisible(true);
	        }
	    });
	    
	    // Listener do botão "Buscar"
	    btnNewButton_1.addActionListener(new ActionListener() {
	        public void actionPerformed(ActionEvent e) {        
	            onBuscar();
	            iniciarMonitoramentoArquivos();	            
	        }
	    });

	    // Listener do botão "Gerar PDF"
	    btnNewButton_2.addActionListener(new ActionListener() {
	        public void actionPerformed(ActionEvent e) {
	            List<ItemImpressao> itensParaImprimir = obterItensParaImpressao();
	            
	            if (itensParaImprimir.isEmpty()) {
	                JOptionPane.showMessageDialog(AppSwingMain.this, 
	                    "Por favor, selecione pelo menos um crachá para gerar o PDF!", 
	                    "Nenhuma Seleção", 
	                    JOptionPane.WARNING_MESSAGE);
	                return;
	            }
	            
	            // Mostrar resumo do que será impresso
	            StringBuilder resumo = new StringBuilder();
	            resumo.append("Itens selecionados para impressão:\n\n");
	            
	            // Contar frentes e versos
	            int totalFrente = 0;
	            int totalVerso = 0;
	            
	            for (ItemImpressao item : itensParaImprimir) {
	                String tipo = item.isFrente() ? "Frente" : "Verso";
	                resumo.append("• ").append(item.getNomeArquivo()).append(" (").append(tipo).append(")\n");
	                
	                if (item.isFrente()) totalFrente++;
	                else totalVerso++;
	            }
	            
	            resumo.append("\nResumo: ").append(totalFrente).append(" frente(s) + ")
	                   .append(totalVerso).append(" verso(s) = ").append(itensParaImprimir.size()).append(" página(s)");
	            resumo.append("\n\nDeseja continuar com a geração do PDF?");
	            
	            int opcao = JOptionPane.showConfirmDialog(
	                AppSwingMain.this, 
	                resumo.toString(), 
	                "Confirmar Geração de PDF", 
	                JOptionPane.YES_NO_OPTION,
	                JOptionPane.QUESTION_MESSAGE
	            );
	            
	            if (opcao == JOptionPane.YES_OPTION) {
	                // Executar geração em thread separada
	                SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
	                    @Override
	                    protected Void doInBackground() throws Exception {
	                        GerarPDF gerador = new GerarPDF();
	                        gerador.gerarPDFMultiplo(itensParaImprimir);
	                        return null;
	                    }
	                    
	                    @Override
	                    protected void done() {
	                        try {
	                            get(); // Verificar se houve exceção
	                            System.out.println("PDF gerado com sucesso!");
	                        } catch (Exception ex) {
	                            ex.printStackTrace();
	                            JOptionPane.showMessageDialog(AppSwingMain.this,
	                                "Erro ao gerar PDF: " + ex.getMessage(),
	                                "Erro",
	                                JOptionPane.ERROR_MESSAGE);
	                        }
	                    }
	                };
	                
	                worker.execute();
	            }
	        }
	    });
	}
    
    private void limparPastaOutput() {
        File outputFolder = new File(OUTPUT_PATH);
        if (outputFolder.exists() && outputFolder.isDirectory()) {
            File[] files = outputFolder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        file.delete();
                    }
                }
                System.out.println("📁 Pasta 'output' limpa com sucesso.");
            }
        }
    }
    
    private void onBuscar() {
        String input = textField.getText().trim();
        if (input.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Digite o(s) RE(s)");
            return;
        }

        try {
            List<String> res = REParser.parseREs(input);
            if (res.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "Nenhum RE válido encontrado!\n\nFormato aceito:\n" +
                    "• Um RE: 12790\n" +
                    "• Múltiplos REs: 12790,14359,14237",
                    "Entrada inválida", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (res.size() > 1) {
                String mensagem = String.format(
                    "Foram encontrados %d REs válidos:\n\n%s\n\n" +
                    "Deseja processar todos?",
                    res.size(),
                    String.join(", ", res.subList(0, Math.min(10, res.size()))) +
                    (res.size() > 10 ? "..." : "")
                );
                int opcao = JOptionPane.showConfirmDialog(this, mensagem,
                    "Confirmar Processamento", JOptionPane.YES_NO_OPTION);
                if (opcao != JOptionPane.YES_OPTION) {
                    return;
                }
            }

            // Limpar pasta antes de processar
            limparPastaOutput();

            // Criar a tela de carregamento (agora não-modal)
            LoadingScreen loadingScreen = new LoadingScreen(this);
            
            // Desabilitar a janela principal para simular o comportamento modal
            this.setEnabled(false);

            // Usar SwingWorker para executar a tarefa em segundo plano
            SwingWorker<Void, String[]> worker = new SwingWorker<Void, String[]>() {
                @Override
                protected Void doInBackground() throws Exception {
                    funcionarioControl.processarEntrada(input, new FuncionarioControl.ProgressCallback() {
                        @Override
                        public void onProgress(int atual, int total, String mensagem) {
                            // 📊 Calcular progresso baseado nos valores recebidos
                            int progress = (int) ((double) atual / total * 100);
                            
                            // 🐛 DEBUG: Imprimir valores recebidos
                            System.out.println("📊 Callback recebido - atual: " + atual + ", total: " + total + ", progress: " + progress + "% - " + mensagem);
                            
                            // Publica o progresso e a mensagem como um array de String
                            publish(new String[]{String.valueOf(progress), mensagem});
                        }

                        @Override
                        public void onComplete(int sucessos, int erros, String mensagemFinal) {
                            System.out.println("✅ Callback onComplete: " + sucessos + " sucessos, " + erros + " erros");
                            // A lógica final é tratada no done()
                        }
                    });
                    return null;
                }

                @Override
                protected void process(List<String[]> chunks) {
                    // 📈 Processar todas as atualizações recebidas
                    for (String[] update : chunks) {
                        try {
                            int progress = Integer.parseInt(update[0]);
                            String message = update[1];
                            
                            System.out.println("📈 Atualizando UI: " + progress + "% - " + message);
                            loadingScreen.setProgress(progress, message);
                            
                        } catch (NumberFormatException e) {
                            System.err.println("⚠️ Erro ao parsear progresso: " + update[0]);
                        }
                    }
                }

                @Override
                protected void done() {
                    try {
                        get(); // Verifica se houve exceções no doInBackground()
                        loadingScreen.setComplete("Processamento concluído!");
                        
                        // ⏱️ Pequeno delay para mostrar mensagem final
                        try {
                            Thread.sleep(1500);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        
                        // ✅ SÓ MOSTRA OS ELEMENTOS SE TUDO DEU CERTO
                        atualizarTabela();
                        chckbxNewCheckBox.setVisible(true);
                        scrollPane.setVisible(true); // ✅ Agora vai funcionar!
                        btnNewButton_2.setVisible(true);
                        
                        System.out.println("✅ Elementos da UI exibidos com sucesso!");
                        
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(AppSwingMain.this, 
                            "Erro ao processar: " + e.getMessage(), 
                            "Erro", 
                            JOptionPane.ERROR_MESSAGE);
                        e.printStackTrace();
                        System.err.println("❌ Erro no processamento, elementos não serão exibidos");
                    } finally {
                        // LIMPEZA SEMPRE EXECUTADA
                        loadingScreen.close();
                        AppSwingMain.this.setEnabled(true);
                        AppSwingMain.this.toFront();
                        System.out.println("🧹 Limpeza do SwingWorker concluída");
                    }
                }
            };
            
            worker.execute(); // Inicia o worker
            loadingScreen.setVisible(true); // Mostra a tela (não vai bloquear a EDT)

        } catch (Exception e) {
            // Garante que a janela principal seja reabilitada em caso de erro inicial
            this.setEnabled(true);
            JOptionPane.showMessageDialog(this, "Erro ao iniciar o processamento: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
    
    
    public void pararMonitoramento() {
        try {
            if (watchService != null) {
                watchService.close();
                System.out.println("🛑 WatchService fechado");
            }
            if (executorService != null) {
                executorService.shutdown();
                System.out.println("🛑 ExecutorService finalizado");
            }
        } catch (Exception e) {
            System.err.println("Erro ao finalizar monitoramento: " + e.getMessage());
        }
    }
    
    // Sobrescreva o método de fechamento da janela
    //@Override
    //public void dispose() {
        //pararMonitoramento();
        //pararTimer();
        //super.dispose();
    //}
    
    @Override
    protected void finalize() throws Throwable {
        pararMonitoramento();
        pararTimer();
        super.finalize();
    }
}