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

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

@Component
public class AppSwingMain extends JFrame {

    private static final long serialVersionUID = 1L;
    private JPanel contentPane;
    private JTextField textField;
    private final FuncionarioControl funcionarioControl;
    private JTable table_1;
    private JTable table_2;

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
    private static class ButtonRenderer extends JButton implements TableCellRenderer {
        private static final long serialVersionUID = 1L;

        public ButtonRenderer(String text) {
            setText(text);
            setOpaque(true);
        }

        @Override
        public java.awt.Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            if (isSelected) {
                setForeground(table.getSelectionForeground());
                setBackground(table.getSelectionBackground());
            } else {
                setForeground(table.getForeground());
                setBackground(UIManager.getColor("Button.background"));
            }
            setText(value == null ? "" : value.toString());
            return this;
        }
    }

    // Editor for buttons in the table
    private static class ButtonEditor extends AbstractCellEditor implements TableCellEditor {
        private static final long serialVersionUID = 1L;
        private final JButton button;
        private String label;
        private int row;
        private final JTable table;
        private final String actionType;

        public ButtonEditor(JTable table, String buttonText, String actionType) {
            this.table = table;
            this.actionType = actionType;
            button = new JButton(buttonText);
            button.addActionListener(e -> fireEditingStopped());
        }

        @Override
        public java.awt.Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            this.label = (value == null) ? "" : value.toString();
            this.row = row;
            button.setText(label);
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            if (actionType.equals("SELECT")) {
                table.setRowSelectionInterval(row, row);
            } else if (actionType.equals("EDIT")) {
                String re = (String) table.getModel().getValueAt(row, 0);
                showEditDialog(re);
            }
            return label;
        }

        private void showEditDialog(String re) {
            JDialog dialog = new JDialog();
            dialog.setTitle("Editar Informações - RE: " + re);
            dialog.setModal(true);
            dialog.setSize(400, 300);
            dialog.getContentPane().setLayout(new BorderLayout());

            JLabel label = new JLabel("Editar informações para RE: " + re);
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
            System.err.println("Diretório não encontrado: " + folderPath);
        }
        return imageFiles;
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
        lblNewLabel.setIcon(new ImageIcon("C:\\Users\\Relogio.ponto\\eclipse-workspace\\CIFMM2\\resources\\images\\logo.png"));
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
        
        JCheckBox chckbxNewCheckBox = new JCheckBox("Selecionar Todos");
        chckbxNewCheckBox.setFont(new Font("Tahoma", Font.PLAIN, 15));
        
        // Tabela
        
        JScrollPane scrollPane = new JScrollPane();
        GroupLayout gl_Main = new GroupLayout(Main);
        gl_Main.setHorizontalGroup(
        	gl_Main.createParallelGroup(Alignment.LEADING)
        		.addGroup(gl_Main.createSequentialGroup()
        			.addContainerGap()
        			.addGroup(gl_Main.createParallelGroup(Alignment.LEADING)
        				.addGroup(gl_Main.createSequentialGroup()
        					.addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 1192, Short.MAX_VALUE)
        					.addContainerGap())
        				.addGroup(Alignment.TRAILING, gl_Main.createSequentialGroup()
        					.addComponent(chckbxNewCheckBox)
        					.addPreferredGap(ComponentPlacement.RELATED, 951, Short.MAX_VALUE)
        					.addComponent(btnNewButton_1, GroupLayout.PREFERRED_SIZE, 106, GroupLayout.PREFERRED_SIZE)
        					.addContainerGap())
        				.addGroup(gl_Main.createSequentialGroup()
        					.addComponent(textField, GroupLayout.DEFAULT_SIZE, 1192, Short.MAX_VALUE)
        					.addContainerGap())
        				.addGroup(gl_Main.createSequentialGroup()
        					.addComponent(lblNewLabel_1)
        					.addGap(623))))
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
        			.addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 473, Short.MAX_VALUE)
        			.addContainerGap())
        );
        
		// Dynamic table population
		String outputPath = "C:\\Users\\Relogio.ponto\\eclipse-workspace\\CIFMM2\\output";
		List<File> imageFiles = getImageFilesFromFolder(outputPath);
		
		// Create data for the table
        Object[][] data = new Object[imageFiles.size()][3];
        for (int i = 0; i < imageFiles.size(); i++) {
            File imageFile = imageFiles.get(i);                        
            data[i][0] = "Selecionar"; // Column 1: Button text for selection
            data[i][1] = loadImage(imageFile.getAbsolutePath()); // Column 2: Image
            data[i][2] = "Editar"; // Column 3: Button text for edit dialog
        }
        
        table_2 = new JTable();
        table_2.setModel(new DefaultTableModel(
        	new Object[][] {
        		{null, null, null},
        		{null, null, null},
        		{null, null, null},
        	},
        	new String[] {
        		"New column", "New column", "New column"
        	}
        ));
        
		// Configuração do renderizador e editor
		table_2.getColumnModel().getColumn(0).setCellRenderer(new ButtonRenderer("Selecionar"));
		table_2.getColumnModel().getColumn(0).setCellEditor(new ButtonEditor(table_2, "Selecionar", "SELECT"));
		table_2.getColumnModel().getColumn(1).setCellRenderer(new ImageRenderer());
		table_2.getColumnModel().getColumn(2).setCellRenderer(new ButtonRenderer("Editar"));
		table_2.getColumnModel().getColumn(2).setCellEditor(new ButtonEditor(table_2, "Editar", "EDIT"));
		
		// Configura a largura da coluna 2 (índice 1) para 500px
        table_2.getColumnModel().getColumn(1).setPreferredWidth(500);
        table_2.getColumnModel().getColumn(1).setMinWidth(500);
        table_2.getColumnModel().getColumn(1).setMaxWidth(500);
        
		// Configura a largura das colunas 1 e 3 para botões
		table_2.getColumnModel().getColumn(0).setPreferredWidth(150);
		table_2.getColumnModel().getColumn(2).setPreferredWidth(150);
		table_2.setRowHeight(370); // Ajuste a altura conforme necessário
        
        scrollPane.setViewportView(table_2);
        Main.setLayout(gl_Main);

        
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