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

import org.springframework.stereotype.Component;

import br.com.cifmm.control.FuncionarioControl;
import br.com.cifmm.service.GerarCrachas;
import br.com.cifmm.service.GerarPDF;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

@Component
public class AppSwingMain extends JFrame {

    private static final long serialVersionUID = 1L;
    private JPanel contentPane;
    private JTextField textField;
    private final FuncionarioControl funcionarioControl;
    
    private JTable table_2;
    private JCheckBox chckbxNewCheckBox; // Adicionar como variÃ¡vel de instÃ¢ncia

    /**
     * Create the frame.
     */
    public AppSwingMain(FuncionarioControl funcionarioControl) {
        this.funcionarioControl = funcionarioControl;
        initUI();
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

                if (value instanceof ImageIcon) {
                    ImageIcon icon = (ImageIcon) value;
                    Image img = icon.getImage().getScaledInstance(500, 370, Image.SCALE_SMOOTH);
                    label.setIcon(new ImageIcon(img));
                } else {
                    label.setIcon(null);
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
    
    public void processSelectedRows() {
        List<Integer> selectedRows = getSelectedRows();
        
        if (selectedRows.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nenhuma linha selecionada!"); // 'this' se refere ao JFrame
            return;
        }
        
        // Processar as linhas selecionadas
        for (int row : selectedRows) {
            System.out.println("Processando linha: " + row);
        }
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
        List<File> imageFiles = new ArrayList<>();
        File folder = new File(folderPath);

        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles((dir, name) ->
                    name.toLowerCase().endsWith(".png") || 
                    name.toLowerCase().endsWith(".jpg") || 
                    name.toLowerCase().endsWith(".jpeg"));
            if (files != null) {
                for (File file : files) {
                    imageFiles.add(file);
                }
            }
        } else {
            System.err.println("DiretÃ³rio nÃ£o encontrado: " + folderPath);
        }
        return imageFiles;
    }

     

    public void updateMasterCheckBox() {
        if (areAllSelected()) {
            chckbxNewCheckBox.setSelected(true);           // Usar chckbxNewCheckBox
            chckbxNewCheckBox.setText("Deselecionar Todos");
        } else if (areNoneSelected()) {
            chckbxNewCheckBox.setSelected(false);          // Usar chckbxNewCheckBox
            chckbxNewCheckBox.setText("Selecionar Todos");
        } else {
            chckbxNewCheckBox.setSelected(false);          // Usar chckbxNewCheckBox
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

        JLabel lblNewLabel_1 = new JLabel("Digite o RE:");
        lblNewLabel_1.setFont(new Font("Tahoma", Font.PLAIN, 20));

        JButton btnNewButton_1 = new JButton("Buscar");
        btnNewButton_1.addActionListener(e -> onBuscar());
        
        chckbxNewCheckBox = new JCheckBox("Selecionar Todos"); // Sem JCheckBox na frente!
        chckbxNewCheckBox.setFont(new Font("Tahoma", Font.PLAIN, 15));
        chckbxNewCheckBox.addActionListener(e -> {
            toggleSelectAll(chckbxNewCheckBox.isSelected(), chckbxNewCheckBox); // Usar chckbxNewCheckBox nos dois lugares
        });
        
        chckbxNewCheckBox.addActionListener(e -> {
            toggleSelectAll(chckbxNewCheckBox.isSelected(), chckbxNewCheckBox);
        });        
        
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
        GroupLayout gl_Main = new GroupLayout(Main);
        gl_Main.setHorizontalGroup(
        	gl_Main.createParallelGroup(Alignment.LEADING)
        		.addGroup(gl_Main.createSequentialGroup()
        			.addContainerGap()
        			.addGroup(gl_Main.createParallelGroup(Alignment.LEADING)
        				.addGroup(gl_Main.createSequentialGroup()
        					.addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 1192, Short.MAX_VALUE)
        					.addContainerGap())
        				.addGroup(gl_Main.createParallelGroup(Alignment.LEADING)
        					.addGroup(Alignment.TRAILING, gl_Main.createSequentialGroup()
        						.addComponent(chckbxNewCheckBox)
        						.addPreferredGap(ComponentPlacement.RELATED, 951, Short.MAX_VALUE)
        						.addComponent(btnNewButton_1, GroupLayout.PREFERRED_SIZE, 106, GroupLayout.PREFERRED_SIZE)
        						.addContainerGap())
        					.addGroup(Alignment.TRAILING, gl_Main.createSequentialGroup()
        						.addComponent(textField, GroupLayout.DEFAULT_SIZE, 1192, Short.MAX_VALUE)
        						.addContainerGap())
        					.addGroup(Alignment.TRAILING, gl_Main.createSequentialGroup()
        						.addComponent(lblNewLabel_1)
        						.addGap(623)))
        				.addGroup(Alignment.TRAILING, gl_Main.createSequentialGroup()
        					.addComponent(btnNewButton_2, GroupLayout.PREFERRED_SIZE, 100, GroupLayout.PREFERRED_SIZE)
        					.addContainerGap())))
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
		String outputPath = "C:\\Users\\lucas.santos\\eclipse-workspace\\cifmm-master\\output";
		List<File> imageFiles = getImageFilesFromFolder(outputPath);
		
		// Create data for the table
		Object[][] data = new Object[imageFiles.size()][3];
		for (int i = 0; i < imageFiles.size(); i++) {
		    File imageFile = imageFiles.get(i);                        
		    data[i][0] = false; // Inicialmente desmarcado (Boolean em vez de String)
		    data[i][1] = loadImage(imageFile.getAbsolutePath());
		    data[i][2] = "Editar";
		}
        
		table_2 = new JTable();
        table_2.setModel(new DefaultTableModel(data, new String[] {
        	    "Selecionar", "Fotos", "EdiÃ§Ã£o"
        	}) {
        	    @Override
        	    public Class<?> getColumnClass(int columnIndex) {
        	        if (columnIndex == 0) {
        	            return Boolean.class; // Coluna 0 contÃ©m valores Boolean
        	        }
        	        return super.getColumnClass(columnIndex);
        	    }
        	});
        
		// ConfiguraÃ§Ã£o do renderizador e editor
        table_2.getColumnModel().getColumn(0).setCellRenderer(new CheckBoxRenderer());
        table_2.getColumnModel().getColumn(0).setCellEditor(new CheckBoxEditor(this));		
		table_2.getColumnModel().getColumn(1).setCellRenderer(new ImageRenderer());				
		table_2.getColumnModel().getColumn(2).setCellRenderer(new ButtonRenderer());
		table_2.getColumnModel().getColumn(2).setCellEditor(new ButtonEditor(table_2, "", "EDIT"));
		
		
		// Configura a largura da coluna 2 (Ã­ndice 1) para 500px
        table_2.getColumnModel().getColumn(1).setPreferredWidth(500);
        table_2.getColumnModel().getColumn(1).setMinWidth(500);
        table_2.getColumnModel().getColumn(1).setMaxWidth(500);
        
		// Configura a largura das colunas 1 e 3 para botÃµes
		table_2.getColumnModel().getColumn(0).setPreferredWidth(50);		
		table_2.getColumnModel().getColumn(2).setPreferredWidth(50);
		table_2.setRowHeight(370); // Ajuste a altura conforme necessÃ¡rio
		
		
        
        scrollPane.setViewportView(table_2);
        Main.setLayout(gl_Main);

        
    }

    private void toggleSelectAll(boolean selected, JCheckBox chckbxNewCheckBox) {
    	DefaultTableModel model = (DefaultTableModel) table_2.getModel();
        
        // Atualizar todas as linhas da tabela
        for (int row = 0; row < model.getRowCount(); row++) {
            model.setValueAt(selected, row, 0); // Coluna 0 = checkboxes
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
        
        for (int row = 0; row < model.getRowCount(); row++) {
            Object value = model.getValueAt(row, 0);
            if (!(value instanceof Boolean) || !(Boolean) value) {
                return false;
            }
        }
        return true;
    }
    
    
    private boolean areNoneSelected() {
        DefaultTableModel model = (DefaultTableModel) table_2.getModel();
        
        for (int row = 0; row < model.getRowCount(); row++) {
            Object value = model.getValueAt(row, 0);
            if (value instanceof Boolean && (Boolean) value) {
                return false;
            }
        }
        return true;
    }
    

	private void onBuscar() {
        String re = textField.getText().trim();
        if (re.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Digite o RE");
            return;
        }

        try {
            funcionarioControl.salvarFuncionario(re);
            JOptionPane.showMessageDialog(this, "Processado.");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao processar: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private ImageIcon loadImage(String path) {
        try {
            if (path != null && !path.isEmpty()) {
                return new ImageIcon(path);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}