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


import org.springframework.stereotype.Component;

import br.com.cifmm.control.FuncionarioControl;
import br.com.cifmm.service.GerarCrachas;
import br.com.cifmm.service.GerarPDF;
import br.com.cifmm.view.components.LoadingScreen;

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
public class AppSwingMain extends JFrame {

    private static final long serialVersionUID = 1L;
    private JPanel contentPane;
    private JTextField textField;
    private final FuncionarioControl funcionarioControl;  
  
    
    private WatchService watchService;
    private ExecutorService executorService;
    private Timer timerAtualizacao; // Alternativa mais simples
    private long ultimaModificacaoPasta = 0;
    
    private static final String OUTPUT_PATH = "C:\\Users\\lucas.santos\\eclipse-workspace\\cifmm-master\\output";

    
    private JTable table_2;
    private JCheckBox chckbxNewCheckBox; // Adicionar como variÃ¡vel de instÃ¢ncia

    /**
     * Create the frame.
     */
    public AppSwingMain(FuncionarioControl funcionarioControl) {
        this.funcionarioControl = funcionarioControl;
        initUI();
        
        SwingUtilities.invokeLater(() -> {
            garantirPastaOutput();
            iniciarMonitoramentoArquivos();
            
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
                ImageIcon icon = new ImageIcon("C:/Users/lucas.santos/eclipse-workspace/cifmm-master/resources/images/pencil-square.png");
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
    private static class ButtonEditor extends AbstractCellEditor implements TableCellEditor {
        private static final long serialVersionUID = 1L;
        private final JButton button;
        private final JPanel panel;
        private String label;
        private int row;
        private final JTable table;
        private final String actionType;

        public ButtonEditor(JTable table, String buttonText, String actionType) {
            this.table = table;
            this.actionType = actionType;
            button = new JButton(buttonText);
            
            panel = new JPanel(new GridBagLayout());
            panel.setOpaque(true);
            
            
            // Configurar aparÃªncia do botÃ£o no editor
            button.setOpaque(true);
            button.setHorizontalAlignment(JLabel.CENTER);
            button.setVerticalAlignment(JLabel.CENTER);
            
            panel.add(button, new GridBagConstraints());
            
            // Carregar imagem
            try {
                ImageIcon icon = new ImageIcon("C:/Users/lucas.santos/eclipse-workspace/cifmm-master/resources/images/pencil-square.png");
                Image img = icon.getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH);
                button.setIcon(new ImageIcon(img));
            } catch (Exception e) {
                System.err.println("Erro ao carregar imagem: " + e.getMessage());
            }
            
            // Configurar tamanho
            button.setPreferredSize(new Dimension(120, 30));

            
            button.addActionListener(e -> fireEditingStopped());
        }

        @Override
        public java.awt.Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            this.label = (value == null) ? "" : value.toString();
            this.row = row;
            
            // Manter configuraÃ§Ãµes visuais
            button.setText("Editar");
            
            return panel;
        }

        @Override
        public Object getCellEditorValue() {
            if (actionType.equals("SELECT")) {
                table.setRowSelectionInterval(row, row);
            } else if (actionType.equals("EDIT")) {
                // Pegar o valor correto da primeira coluna (assumindo que Ã© onde estÃ¡ o RE)
                
                showEditDialog();
            }
            return label;
        }

        private void showEditDialog() {
            JDialog dialog = new JDialog();
            dialog.setTitle("Editar InformaÃ§Ãµes");
            dialog.setModal(true);
            dialog.setSize(400, 300);
            dialog.getContentPane().setLayout(new BorderLayout());

            JLabel label = new JLabel("Editar informaÃ§Ãµes");
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setFont(new Font("Tahoma", Font.PLAIN, 16));
            dialog.getContentPane().add(label, BorderLayout.CENTER);

            JButton closeButton = new JButton("Fechar");
            closeButton.addActionListener(e -> dialog.dispose());
            dialog.getContentPane().add(closeButton, BorderLayout.SOUTH);

            dialog.setLocationRelativeTo(null);
            dialog.setVisible(true);
        }

        @Override
        public boolean stopCellEditing() {
            return super.stopCellEditing();
        }

        @Override
        public boolean isCellEditable(EventObject e) {
            return true;
        }
    }
    
    
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
    
    public void atualizarTabela() {
        try {
            System.out.println("🔄 Atualizando tabela...");
            List<File> imageFiles = getImageFilesFromFolder(OUTPUT_PATH);
            DefaultTableModel model = (DefaultTableModel) table_2.getModel();
            Map<String, Boolean> estadoCheckboxes = new HashMap<>();

            // Salva o estado atual dos checkboxes
            // ... (o código aqui está correto, mas pode ser otimizado para as colunas 0 e 1)
            for (int i = 0; i < model.getRowCount(); i++) {
                // A coluna 1 é para o checkbox 'verso', não para a imagem
                Object valor = model.getValueAt(i, 2); // Corrigido para a coluna da imagem
                if (valor instanceof ImageIcon) {
                    ImageIcon icon = (ImageIcon) valor;
                    String caminho = icon.getDescription();
                    Boolean selecionado = (Boolean) model.getValueAt(i, 0);
                    if (caminho != null) {
                        estadoCheckboxes.put(new File(caminho).getName(), selecionado);
                    }
                }
            }

            // Limpa e recarrega a tabela
            model.setRowCount(0);

            for (File imageFile : imageFiles) {
                // O modelo da tabela tem 4 colunas: Frente, Verso, Fotos, Editar
                Object[] rowData = new Object[3]; // Corrigido para 4 colunas
                String nomeArquivo = imageFile.getName();
                Boolean estadoAnterior = estadoCheckboxes.get(nomeArquivo);

                rowData[0] = estadoAnterior != null ? estadoAnterior : false; // Checkbox 'Frente'
                rowData[0] = estadoAnterior != null ? estadoAnterior : false; // Checkbox 'Verso'
                
                // Carrega a imagem e a coloca na coluna correta (índice 2)
                ImageIcon icon = new ImageIcon(imageFile.getAbsolutePath());
                icon.setDescription(imageFile.getAbsolutePath());
                rowData[1] = icon; // Coluna 2 para a imagem
                
                rowData[2] = "Editar"; // Coluna 3 para o botão

                model.addRow(rowData);
            }

            updateMasterCheckBox();
            table_2.revalidate();
            table_2.repaint();
            System.out.println("✅ Tabela atualizada - " + imageFiles.size() + " arquivos encontrados");
        } catch (Exception e) {
            System.err.println("❌ Erro ao atualizar tabela: " + e.getMessage());
            e.printStackTrace();
        }
    }
	
    public void updateMasterCheckBox() {
        if (areAllSelected()) {
            chckbxNewCheckBox.setSelected(true);
            chckbxNewCheckBox.setText("Deselecionar Todos");
        } else if (areNoneSelected()) {
            chckbxNewCheckBox.setSelected(false);
            chckbxNewCheckBox.setText("Selecionar Todos");
        } else {
            chckbxNewCheckBox.setSelected(false);
            chckbxNewCheckBox.setText("Selecionar Todos");
        }
    }
	
	private void initUI() {			
		
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 1391, 796);
        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(contentPane);
        contentPane.setLayout(new BorderLayout(0, 0));

        JPanel Header = new JPanel();
        Header.setBorder(new LineBorder(new Color(192, 192, 192)));
        FlowLayout flowLayout = (FlowLayout) Header.getLayout();
        flowLayout.setAlignment(FlowLayout.LEFT);
        contentPane.add(Header, BorderLayout.NORTH);

        JLabel lblNewLabel = new JLabel("");
        lblNewLabel.setIcon(new ImageIcon("C:\\Users\\lucas.santos\\eclipse-workspace\\cifmm-master\\resources\\images\\logo.png"));
        Header.add(lblNewLabel);

        JPanel SideBar = new JPanel();
        SideBar.setBorder(new MatteBorder(0, 1, 0, 1, new Color(192, 192, 192)));
        FlowLayout flowLayout_1 = (FlowLayout) SideBar.getLayout();
        flowLayout_1.setHgap(25);
        contentPane.add(SideBar, BorderLayout.WEST);

        JButton btnNewButton = new JButton("Gerar Crachas");
        
        btnNewButton.setIcon(null);
        SideBar.add(btnNewButton);

        JPanel Main = new JPanel();
        contentPane.add(Main, BorderLayout.CENTER);
        
        textField = new JTextField();
        textField.setColumns(10);
        textField.setVisible(false);

        JLabel lblNewLabel_1 = new JLabel("Digite o RE:");
        lblNewLabel_1.setFont(new Font("Tahoma", Font.PLAIN, 20));
        lblNewLabel_1.setVisible(false);

        JButton btnNewButton_1 = new JButton("Buscar");
        btnNewButton_1.setVisible(false);
        
        chckbxNewCheckBox = new JCheckBox("Selecionar Todos"); // Sem JCheckBox na frente!
        chckbxNewCheckBox.setFont(new Font("Tahoma", Font.PLAIN, 15));
        chckbxNewCheckBox.addActionListener(e -> {
            toggleSelectAll(chckbxNewCheckBox.isSelected(), chckbxNewCheckBox); // Usar chckbxNewCheckBox nos dois lugares
        });
        
              
        
        chckbxNewCheckBox.setVisible(false);
        // Tabela
        
        JScrollPane scrollPane = new JScrollPane();
        
        JButton btnNewButton_2 = new JButton("Gerar PDF");
        btnNewButton_2.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                GerarCrachas gerarCrachas = new GerarCrachas();
                String re = textField.getText().trim();
                gerarCrachas.gerarCrachasEmPDF(re);
            }
        });
        
        btnNewButton_2.setVisible(false);
        
        GroupLayout gl_Main = new GroupLayout(Main);
        gl_Main.setHorizontalGroup(
        	gl_Main.createParallelGroup(Alignment.LEADING)
        		.addGroup(gl_Main.createSequentialGroup()
        			.addContainerGap()
        			.addGroup(gl_Main.createParallelGroup(Alignment.LEADING)
        				.addGroup(Alignment.TRAILING, gl_Main.createSequentialGroup()
        					.addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 1192, Short.MAX_VALUE)
        					.addContainerGap())
        				.addGroup(Alignment.TRAILING, gl_Main.createSequentialGroup()
        					.addComponent(chckbxNewCheckBox)
        					.addPreferredGap(ComponentPlacement.RELATED, 951, Short.MAX_VALUE)
        					.addComponent(btnNewButton_1, GroupLayout.PREFERRED_SIZE, 106, GroupLayout.PREFERRED_SIZE)
        					.addContainerGap())
        				.addGroup(Alignment.TRAILING, gl_Main.createSequentialGroup()
        					.addComponent(textField, GroupLayout.DEFAULT_SIZE, 1192, Short.MAX_VALUE)
        					.addContainerGap())
        				.addGroup(Alignment.TRAILING, gl_Main.createSequentialGroup()
        					.addComponent(btnNewButton_2, GroupLayout.PREFERRED_SIZE, 100, GroupLayout.PREFERRED_SIZE)
        					.addContainerGap())
        				.addGroup(gl_Main.createSequentialGroup()
        					.addComponent(lblNewLabel_1)
        					.addContainerGap(1096, Short.MAX_VALUE))))
        );
        gl_Main.setVerticalGroup(
        	gl_Main.createParallelGroup(Alignment.LEADING)
        		.addGroup(gl_Main.createSequentialGroup()
        			.addContainerGap()
        			.addComponent(lblNewLabel_1)
        			.addPreferredGap(ComponentPlacement.RELATED)
        			.addComponent(textField, GroupLayout.PREFERRED_SIZE, 48, GroupLayout.PREFERRED_SIZE)
        			.addPreferredGap(ComponentPlacement.UNRELATED)
        			.addGroup(gl_Main.createParallelGroup(Alignment.LEADING)
        				.addComponent(btnNewButton_1, GroupLayout.PREFERRED_SIZE, 42, GroupLayout.PREFERRED_SIZE)
        				.addComponent(chckbxNewCheckBox))
        			.addGap(18)
        			.addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 429, Short.MAX_VALUE)
        			.addPreferredGap(ComponentPlacement.RELATED)
        			.addComponent(btnNewButton_2, GroupLayout.PREFERRED_SIZE, 38, GroupLayout.PREFERRED_SIZE)
        			.addContainerGap())
        );
        
        
        
     // Dynamic table population
        List<File> imageFiles = getAllImageFiles();

        // Create data for the table
        Object[][] data = new Object[imageFiles.size()][3];
        for (int i = 0; i < imageFiles.size(); i++) {
            File imageFile = imageFiles.get(i);

            data[i][0] = true;  // Frente
            data[i][0] = true;  // Verso
            // --- ALTERAÇÃO AQUI ---
            // Cria um ImageIcon a partir do caminho do arquivo
            ImageIcon icon = new ImageIcon(imageFile.getAbsolutePath());
            // Armazena o caminho na descrição do ícone, o que pode ser útil mais tarde
            icon.setDescription(imageFile.getAbsolutePath());
            // Adiciona o ImageIcon diretamente no array de dados
            data[i][1] = icon;
            // --- FIM DA ALTERAÇÃO ---
            data[i][2] = "Editar"; // Botão ou texto
        }

        table_2 = new JTable();
        // Atualize o modelo da tabela para incluir a nova coluna
     // O array de colunas agora é: [Selecionar, Fotos, Editar]
        table_2.setModel(new DefaultTableModel(data, new String[] {
            "Selecionar", "Fotos", "Editar"
        }) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                // A primeira coluna (índice 0) é a de checkbox
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
		table_2.getColumnModel().getColumn(2).setCellEditor(new ButtonEditor(table_2, "", "EDIT"));

		
		// Configura a largura das colunas
		table_2.getColumnModel().getColumn(0).setPreferredWidth(50);
		table_2.getColumnModel().getColumn(1).setPreferredWidth(50);
		table_2.getColumnModel().getColumn(2).setPreferredWidth(500);
		table_2.getColumnModel().getColumn(2).setPreferredWidth(50);
		table_2.setRowHeight(370);
		
		chckbxNewCheckBox.addActionListener(e -> {
		    boolean isSelected = chckbxNewCheckBox.isSelected();
		    
		    DefaultTableModel model = (DefaultTableModel) table_2.getModel();
		    
		    for (int i = 0; i < model.getRowCount(); i++) {
		        // Agora você define o valor apenas para a coluna de checkbox (índice 0)
		        model.setValueAt(isSelected, i, 0); 
		    }
		});


		
		
        
        scrollPane.setViewportView(table_2);
        scrollPane.setVisible(false);
        
        btnNewButton.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent e) {
        		lblNewLabel_1.setVisible(true);
        		textField.setVisible(true);
        		btnNewButton_1.setVisible(true);
        	}
        });
        
        btnNewButton_1.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // Criar o JDialog para a tela de carregamento
                JDialog loadingDialog = new JDialog(AppSwingMain.this, "Carregando...", true);
                LoadingScreen loadingScreen = new LoadingScreen();
                loadingDialog.add(loadingScreen);
                loadingDialog.setSize(450, 200);
                loadingDialog.setLocationRelativeTo(AppSwingMain.this);
                loadingDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

                // Configurar o callback para quando o carregamento terminar
                loadingScreen.setOnComplete(() -> {
                    SwingUtilities.invokeLater(() -> {
                        loadingDialog.dispose();
                        // Mostrar os componentes após o carregamento
                        scrollPane.setVisible(true);
                        chckbxNewCheckBox.setVisible(true);
                        btnNewButton_2.setVisible(true);
                    });
                });

                // Executar onBuscar em uma thread separada
                SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        try {
                            onBuscar();
                        } catch (Exception ex) {
                            // Tratar erro na EDT
                            SwingUtilities.invokeLater(() -> {
                                loadingScreen.stopLoading();
                                loadingDialog.dispose();
                                JOptionPane.showMessageDialog(AppSwingMain.this, 
                                    "Erro ao processar: " + ex.getMessage(), 
                                    "Erro", JOptionPane.ERROR_MESSAGE);
                                ex.printStackTrace();
                            });
                            throw ex; // Relançar para o método done detectar o erro
                        }
                        return null;
                    }

                    @Override
                    protected void done() {
                        SwingUtilities.invokeLater(() -> {
                            try {
                                // Verificar se houve erro (get() lança exceção se houver)
                                get(); // Força a verificação de exceções do doInBackground
                                // Aguardar para garantir que os arquivos foram criados
                                Thread.sleep(2000);
                                atualizarTabela();
                                // Forçar a conclusão da LoadingScreen
                                loadingScreen.completeLoading();
                            } catch (InterruptedException ex) {
                                Thread.currentThread().interrupt();
                            } catch (Exception ex) {
                                // Exceção já tratada no doInBackground, evitar duplicação
                            }
                        });
                    }
                };

                // Iniciar a tela de carregamento e a tarefa
                loadingScreen.startLoading();
                worker.execute();
                loadingDialog.setVisible(true);
            }
        });
        
        Main.setLayout(gl_Main);
        
        iniciarMonitoramentoArquivos();

        
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
    
	private boolean areNoneSelected() {
	    DefaultTableModel model = (DefaultTableModel) table_2.getModel();
	    for (int i = 0; i < model.getRowCount(); i++) {
	        // Verifique apenas a coluna 0
	        if ((Boolean) model.getValueAt(i, 0)) {
	            return false;
	        }
	    }
	    return true;
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
        String re = textField.getText().trim();
        if (re.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Digite o RE");
            return;
        }

        try {
            // Limpar a pasta antes de gerar novos crachás
            limparPastaOutput();

            funcionarioControl.salvarFuncionario(re);            
            
            // Força uma atualização da tabela após processar
            SwingUtilities.invokeLater(() -> {
                try {
                    Thread.sleep(2000); // Aguarda um pouco para garantir que os arquivos foram criados
                    atualizarTabela();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao processar: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
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
    @Override
    public void dispose() {
        pararMonitoramento();
        pararTimer();
        super.dispose();
    }
    
    @Override
    protected void finalize() throws Throwable {
        pararMonitoramento();
        pararTimer();
        super.finalize();
    }
}