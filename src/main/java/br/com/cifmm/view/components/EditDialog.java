package br.com.cifmm.view.components;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;

import br.com.cifmm.model.FuncionarioModel;
import br.com.cifmm.repository.FuncionarioRepository;

public class EditDialog extends JDialog {

    private static final long serialVersionUID = 1L;
    private final JPanel contentPanel = new JPanel();
    private JTextField txtNome;
    private JTextField txtCargo;
    private JTextField txtSecretaria;
    private JTextField txtApelido;
    
    private JPanel southPanel;
    private CardLayout cardLayout;

    // Componentes de carregamento (loading)
    private JPanel loadingPanel;
    private JProgressBar progressBar;
    private JLabel loadingLabel;

    // Botões
    private JButton btnSalvar;
    private JButton btnCancelar;

    // Dependência injetada
    private FuncionarioRepository funcionarioRepository;

    private FuncionarioModel funcionarioAtual;
    private boolean edicaoSalva = false;

    /**
     * Construtor que recebe as dependências.
     * @param funcionarioRepository O repositório para acesso aos dados.
     */
    public EditDialog(FuncionarioRepository funcionarioRepository) {
        this.funcionarioRepository = funcionarioRepository;
        initComponents();
    }

    /**
     * Construtor padrão (para compatibilidade com designers de UI).
     */
    public EditDialog() {
        initComponents();
    }

    /**
     * Inicializa os componentes da interface gráfica.
     */
    private void initComponents() {
        setTitle("Editar Funcionário");
        setModal(true);
        setResizable(false);
        setBounds(100, 100, 900, 723);
        getContentPane().setLayout(new BorderLayout());
        contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        getContentPane().add(contentPanel, BorderLayout.CENTER);

        setupMainPanel();
        setupButtonAndLoadingPanel(); // Combina a criação dos painéis inferiores
    }

    /**
     * Configura o painel principal com os campos de texto.
     */
    private void setupMainPanel() {
        GridBagLayout gbl_contentPanel = new GridBagLayout();
        gbl_contentPanel.columnWidths = new int[]{0, 0};
        gbl_contentPanel.rowHeights = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0};
        gbl_contentPanel.columnWeights = new double[]{1.0, Double.MIN_VALUE};
        gbl_contentPanel.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
        contentPanel.setLayout(gbl_contentPanel);

        // Campos de texto (Nome, Cargo, Secretaria, Apelido)
        addFormField("Nome:", 0, false);
        addFormField("Cargo:", 2, false);
        addFormField("Secretaria:", 4, false);
        addFormField("Apelido (Editável):", 6, true);
    }

    /**
     * Método auxiliar para adicionar um rótulo e um campo de texto ao painel principal.
     */
    private void addFormField(String labelText, int gridy, boolean isEditable) {
        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Tahoma", Font.BOLD, 20));
        GridBagConstraints gbc_label = new GridBagConstraints();
        gbc_label.anchor = GridBagConstraints.WEST;
        gbc_label.insets = new Insets(0, 0, 5, 0);
        gbc_label.gridx = 0;
        gbc_label.gridy = gridy;
        contentPanel.add(label, gbc_label);

        JTextField textField = new JTextField();
        textField.setFont(new Font("Tahoma", Font.PLAIN, 20));
        textField.setEditable(isEditable);
        GridBagConstraints gbc_textField = new GridBagConstraints();
        gbc_textField.ipady = 15;
        gbc_textField.insets = new Insets(0, 0, 15, 0); // Espaçamento maior
        gbc_textField.fill = GridBagConstraints.HORIZONTAL;
        gbc_textField.gridx = 0;
        gbc_textField.gridy = gridy + 1;
        contentPanel.add(textField, gbc_textField);
        textField.setColumns(10);

        // Associa o campo de texto à variável de instância correta
        if (labelText.startsWith("Nome")) txtNome = textField;
        else if (labelText.startsWith("Cargo")) txtCargo = textField;
        else if (labelText.startsWith("Secretaria")) txtSecretaria = textField;
        else if (labelText.startsWith("Apelido")) txtApelido = textField;
    }

    /**
     * Configura o painel inferior que contém os botões e a área de carregamento.
     */
    private void setupButtonAndLoadingPanel() {
        cardLayout = new CardLayout();
        southPanel = new JPanel(cardLayout);
        getContentPane().add(southPanel, BorderLayout.SOUTH);

        // Card 1: Painel com os botões
        JPanel buttonPane = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPane.setBorder(new EmptyBorder(15, 15, 15, 15));

        btnSalvar = new JButton("Salvar Alterações");
        btnSalvar.setFont(new Font("Tahoma", Font.PLAIN, 20));
        btnSalvar.addActionListener(e -> salvarAlteracoes());
        buttonPane.add(btnSalvar);
        getRootPane().setDefaultButton(btnSalvar);

        btnCancelar = new JButton("Cancelar");
        btnCancelar.setFont(new Font("Tahoma", Font.PLAIN, 20));
        btnCancelar.addActionListener(e -> dispose());
        buttonPane.add(btnCancelar);

        // Card 2: Painel de carregamento
        loadingPanel = new JPanel(new BorderLayout(0, 10));
        loadingPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        loadingPanel.add(progressBar, BorderLayout.CENTER);

        loadingLabel = new JLabel("Processando...");
        loadingLabel.setFont(new Font("Tahoma", Font.PLAIN, 16));
        loadingLabel.setHorizontalAlignment(SwingConstants.CENTER);
        loadingPanel.add(loadingLabel, BorderLayout.SOUTH);

        // Adiciona os painéis (cards) ao painel principal do sul
        southPanel.add(buttonPane, "BUTTONS");
        southPanel.add(loadingPanel, "LOADING");
    }
    
    /**
     * Carrega os dados de um funcionário com base no RE.
     * @param re A matrícula do funcionário.
     */
    /**
     * Método para carregar os dados do funcionário nos campos
     * @param re RE do funcionário para buscar no banco de dados
     */
    public void carregarFuncionario(String re) {
        try {
            // CORREÇÃO: Busca o funcionário usando Optional para evitar erro de múltiplos resultados
            Optional<FuncionarioModel> funcionarioOpt = funcionarioRepository.findFirstByRe(re);
            
            if (funcionarioOpt.isPresent()) {
                funcionarioAtual = funcionarioOpt.get();
                
                // Preenche os campos com os dados do funcionário
                txtNome.setText(funcionarioAtual.getNome() != null ? funcionarioAtual.getNome() : "");
                txtCargo.setText(funcionarioAtual.getCargo() != null ? funcionarioAtual.getCargo() : "");
                txtSecretaria.setText(funcionarioAtual.getSecretaria() != null ? funcionarioAtual.getSecretaria() : "");
                txtApelido.setText(funcionarioAtual.getApelido() != null ? funcionarioAtual.getApelido() : "");
                
                System.out.println("Funcionário carregado: " + funcionarioAtual.getNome() + " (RE: " + funcionarioAtual.getRe() + ")");
                
                // Verificar se existem duplicatas e avisar
                List<FuncionarioModel> todosFuncionarios = funcionarioRepository.findAllByRe(re);
                if (todosFuncionarios.size() > 1) {
                    System.out.println("⚠️  ATENÇÃO: Encontrados " + todosFuncionarios.size() + " registros com RE " + re + 
                        ". Carregando o primeiro encontrado: " + funcionarioAtual.getNome());
                }
                
            } else {
                JOptionPane.showMessageDialog(this, 
                    "Funcionário com RE " + re + " não encontrado no banco de dados!", 
                    "Erro", 
                    JOptionPane.ERROR_MESSAGE);
                dispose();
            }
        } catch (Exception e) {
            System.err.println("Erro ao carregar funcionário: " + e.getMessage());
            JOptionPane.showMessageDialog(this, 
                "Erro ao carregar dados do funcionário: " + e.getMessage(), 
                "Erro", 
                JOptionPane.ERROR_MESSAGE);
            dispose();
        }
    }

    /**
     * Preenche os campos da tela com os dados de um objeto FuncionarioModel.
     * @param funcionario O funcionário cujos dados serão exibidos.
     */
    public void carregarFuncionario(FuncionarioModel funcionario) {
        this.funcionarioAtual = funcionario;
        if (funcionarioAtual != null) {
            txtNome.setText(Objects.toString(funcionarioAtual.getNome(), ""));
            txtCargo.setText(Objects.toString(funcionarioAtual.getCargo(), ""));
            txtSecretaria.setText(Objects.toString(funcionarioAtual.getSecretaria(), ""));
            txtApelido.setText(Objects.toString(funcionarioAtual.getApelido(), ""));
            System.out.println("✅ Funcionário carregado: " + funcionarioAtual.getNome() + " (RE: " + funcionarioAtual.getRe() + ")");
        }
    }

    /**
     * Valida as alterações e inicia o processo de salvamento em background.
     */
    /**
     * Valida as alterações e inicia o processo de salvamento em background.
     */
    private void salvarAlteracoes() {
        if (funcionarioAtual == null) {
            JOptionPane.showMessageDialog(this, "Nenhum funcionário carregado para edição!", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String novoApelido = txtApelido.getText().trim();
        // Converte string vazia para null para consistência com o banco de dados.
        final String apelidoFinal = novoApelido.isEmpty() ? null : novoApelido;
        
        String apelidoAtual = funcionarioAtual.getApelido();

        // Se não houve mudança, informa o usuário e fecha o diálogo.
        if (Objects.equals(apelidoAtual, apelidoFinal)) {
            JOptionPane.showMessageDialog(this, "Nenhuma alteração foi feita.", "Informação", JOptionPane.INFORMATION_MESSAGE);
            dispose();
            return;
        }

        // Mostra a tela de carregamento e inicia o SwingWorker.
        mostrarLoading(true);

        SwingWorker<Void, String> worker = new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() throws Exception {
                publish("Salvando no banco de dados...");
                Thread.sleep(500); // Simula latência para feedback visual.
                
                funcionarioAtual.setApelido(apelidoFinal); // Usa a variável final.
                funcionarioRepository.save(funcionarioAtual);
                
                publish("Alterações salvas com sucesso!");
                Thread.sleep(500);
                
                return null;
            }
            
            @Override
            protected void process(java.util.List<String> chunks) {
                // Atualiza a mensagem de loading na UI thread.
                if (!chunks.isEmpty()) {
                    loadingLabel.setText(chunks.get(chunks.size() - 1));
                }
            }
            
            @Override
            protected void done() {
                try {
                    get(); // Verifica se ocorreram exceções no doInBackground().
                    
                    // MARCA COMO SALVO ANTES DE MOSTRAR QUALQUER DIALOG
                    edicaoSalva = true;
                    
                    String mensagem = apelidoFinal != null ? 
                        "Apelido '" + apelidoFinal + "' salvo com sucesso!" : 
                        "Apelido removido com sucesso!";
                    
                    System.out.println("✅ Apelido atualizado para " + funcionarioAtual.getNome() + 
                        " (RE: " + funcionarioAtual.getRe() + "): " + apelidoFinal);
                    
                    // Mostra mensagem de sucesso
                    JOptionPane.showMessageDialog(EditDialog.this, mensagem, "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                    
                    // FECHA IMEDIATAMENTE após mostrar a mensagem
                    dispose();
                    
                } catch (Exception e) {
                    System.err.println("❌ Erro ao salvar apelido: " + e.getMessage());
                    e.printStackTrace();
                    
                    // Em caso de erro, não marca como salvo
                    edicaoSalva = false;
                    
                    JOptionPane.showMessageDialog(EditDialog.this, 
                        "Erro ao salvar alterações: " + e.getCause().getMessage(), 
                        "Erro", 
                        JOptionPane.ERROR_MESSAGE);
                        
                    // Volta para o estado normal (não fecha em caso de erro)
                    mostrarLoading(false);
                }
            }
        };
        
        worker.execute();
    }

    /**
     * Alterna a visibilidade do painel de carregamento e o estado dos botões.
     * @param mostrar true para exibir o loading, false para ocultar.
     */
    private void mostrarLoading(boolean mostrar) {
        if (mostrar) {
            cardLayout.show(southPanel, "LOADING");
            loadingLabel.setText("Aplicando alterações..."); // Mensagem mais descritiva
        } else {
            cardLayout.show(southPanel, "BUTTONS");
        }
        
        // Desabilita os botões para evitar cliques duplos
        btnSalvar.setEnabled(!mostrar);
        btnCancelar.setEnabled(!mostrar);
    }

    /**
     * Retorna true se a edição foi salva com sucesso.
     */
    public boolean isEdicaoSalva() {
        return edicaoSalva;
    }

    /**
     * Retorna a instância do funcionário sendo editado.
     */
    public FuncionarioModel getFuncionarioAtual() {
        return funcionarioAtual;
    }
}