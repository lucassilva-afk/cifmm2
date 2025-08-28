package br.com.cifmm.view.components;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JTable;

import br.com.cifmm.model.FuncionarioModel;
import br.com.cifmm.repository.FuncionarioRepository;
import br.com.cifmm.service.GerarCrachas;
import br.com.cifmm.service.BuscarDados;

public class ButtonEditor extends DefaultCellEditor {
    
    private static final long serialVersionUID = 1L;
    private JButton button;
    private String label;
    private boolean isPushed;
    private JTable table;
    private int currentRow;
    private String buttonType;
    
    // Dependências passadas via construtor
    private FuncionarioRepository funcionarioRepository;
    private GerarCrachas gerarCrachas;
    private BuscarDados buscarDados;
    private TabelaCallback tabelaCallback;

    public ButtonEditor(JTable table, String label, String buttonType, 
                       FuncionarioRepository funcionarioRepository, 
                       GerarCrachas gerarCrachas,
                       BuscarDados buscarDados,
                       TabelaCallback tabelaCallback) {
        super(new JCheckBox());
        this.table = table;
        this.label = label;
        this.buttonType = buttonType;
        this.funcionarioRepository = funcionarioRepository;
        this.gerarCrachas = gerarCrachas;
        this.buscarDados = buscarDados;
        this.tabelaCallback = tabelaCallback;
        
        button = new JButton();
        button.setOpaque(true);
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fireEditingStopped();
                handleButtonClick();
            }
        });
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value,
            boolean isSelected, int row, int column) {
        
        this.currentRow = row;
        button.setText(label);
        isPushed = true;
        return button;
    }

    @Override
    public Object getCellEditorValue() {
        // Remove a lógica de handleButtonClick daqui
        isPushed = false;
        return label;
    }

    @Override
    public boolean stopCellEditing() {
        isPushed = false;
        return super.stopCellEditing();
    }

    private void handleButtonClick() {
        if ("EDIT".equals(buttonType)) {
            handleEditButton();
        }
    }

    private void handleEditButton() {
        try {
            String reDoFuncionario = extrairREDaImagem(currentRow);
            
            if (reDoFuncionario != null) {
                System.out.println("🔍 Editando funcionário com RE: " + reDoFuncionario + " (linha " + currentRow + ")");
                
                // Para a edição da célula ANTES de abrir o diálogo
                fireEditingStopped();
                
                // Executa a abertura do diálogo em uma nova thread da EDT
                javax.swing.SwingUtilities.invokeLater(() -> {
                    abrirEditDialog(reDoFuncionario, currentRow);
                });
                
            } else {
                System.err.println("❌ Não foi possível extrair o RE da linha " + currentRow);
            }
            
        } catch (Exception e) {
            System.err.println("❌ Erro ao processar botão editar: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String extrairREDaImagem(int row) {
        try {
            Object imageValue = table.getValueAt(row, 1);
            
            if (imageValue instanceof javax.swing.ImageIcon) {
                javax.swing.ImageIcon icon = (javax.swing.ImageIcon) imageValue;
                String imagePath = icon.getDescription();
                
                String fileName = new java.io.File(imagePath).getName();
                
                if (fileName.contains("cracha_frente_")) {
                    String re = fileName.replace("cracha_frente_", "").replace(".png", "");
                    return re;
                } else if (fileName.contains("cracha_verso_")) {
                    String re = fileName.replace("cracha_verso_", "").replace(".png", "");
                    return re;
                }
            }
            
        } catch (Exception e) {
            System.err.println("Erro ao extrair RE da imagem: " + e.getMessage());
        }
        
        return null;
    }

    private void abrirEditDialog(String re, int linha) {
        try {
            EditDialog editDialog = new EditDialog(funcionarioRepository);
            editDialog.carregarFuncionario(re);
            editDialog.setLocationRelativeTo(table);
            editDialog.setModal(true);
            
            // Mostra o diálogo e aguarda o fechamento
            editDialog.setVisible(true);
            
            // Só executa após o diálogo ser fechado
            if (editDialog.isEdicaoSalva()) {
                System.out.println("✅ Apelido editado com sucesso para RE: " + re);
                
                // Ocultar a tabela primeiro
                ocultarTabelaEMostrarCarregamento();
                
                // Executar regeneração em background
                javax.swing.SwingWorker<Void, Void> worker = new javax.swing.SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        regenerarCrachaComDadosAtualizados(re);
                        return null;
                    }
                    
                    @Override
                    protected void done() {
                        try {
                            get(); // Verifica se houve exceções
                            // Reexibir tabela e atualizar
                            reexibirTabelaEAtualizar();
                            System.out.println("🔄 Tabela totalmente atualizada");
                        } catch (Exception e) {
                            System.err.println("❌ Erro na regeneração: " + e.getMessage());
                            e.printStackTrace();
                            reexibirTabelaEAtualizar(); // Reexibe mesmo com erro
                        }
                    }

					
                };
                
                worker.execute();
            }
            
        } catch (Exception e) {
            System.err.println("❌ Erro ao abrir EditDialog: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void reexibirTabelaEAtualizar() {
    if (tabelaCallback != null) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            try {
                // Atualiza tabela e reexibe
                tabelaCallback.atualizarTabela();
                
                // Chama método específico para ocultar carregamento
                if (tabelaCallback instanceof br.com.cifmm.view.AppSwingMain) {
                    ((br.com.cifmm.view.AppSwingMain) tabelaCallback).ocultarCarregamentoTabela();
                }
            } catch (Exception e) {
                System.err.println("❌ Erro ao atualizar tabela: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
}

    private void ocultarTabelaEMostrarCarregamento() {
        if (tabelaCallback != null) {
            javax.swing.SwingUtilities.invokeLater(() -> {
                try {
                    if (tabelaCallback instanceof br.com.cifmm.view.AppSwingMain) {
                        br.com.cifmm.view.AppSwingMain app = (br.com.cifmm.view.AppSwingMain) tabelaCallback;
                        app.mostrarCarregamentoTabela();
                    }
                } catch (Exception e) {
                    System.err.println("Erro ao mostrar carregamento: " + e.getMessage());
                    // Não interrompe o fluxo se der erro no carregamento
                }
            });
        }
    }

	private void regenerarCrachaComDadosAtualizados(String re) {
        try {
            System.out.println("🎨 Regenerando crachá com dados atualizados do site para RE: " + re);
            
            // 1. Remove o arquivo antigo primeiro
            removerCrachaAntigo(re);
            
            // 2. Busca dados atualizados do SITE (não do banco)
            FuncionarioModel funcionarioAtualizado = buscarDados.buscarPorRe(re);
            
            // 3. Pega o apelido do banco (que foi salvo recentemente) - FIXED: Handle multiple results
            FuncionarioModel funcionarioBanco = buscarFuncionarioComTratamentoDeErro(re);
            if (funcionarioBanco != null && funcionarioBanco.getApelido() != null) {
                funcionarioAtualizado.setApelido(funcionarioBanco.getApelido());
            }
            
            // 4. Regenera APENAS a frente com os dados corretos
            gerarCrachas.regenerarFrenteCracha(funcionarioAtualizado);
            
            Thread.sleep(1000); // Aguarda arquivo ser criado
            
        } catch (Exception e) {
            System.err.println("❌ Erro ao regenerar crachá: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Busca funcionário tratando o caso de múltiplos resultados
     */
    private FuncionarioModel buscarFuncionarioComTratamentoDeErro(String re) {
        try {
            // Tenta buscar com o método original
            return funcionarioRepository.findByRe(re);
            
        } catch (org.springframework.dao.IncorrectResultSizeDataAccessException e) {
            // Se houver múltiplos resultados, busca todos e pega o primeiro
            System.out.println("⚠️ ATENÇÃO: Múltiplos registros encontrados para RE: " + re + ". Usando o primeiro encontrado.");
            
            try {
                List<FuncionarioModel> funcionarios = funcionarioRepository.findAllByRe(re);
                if (!funcionarios.isEmpty()) {
                    return funcionarios.get(0); // Retorna o primeiro
                }
            } catch (Exception ex) {
                System.err.println("❌ Erro ao buscar funcionários por RE: " + ex.getMessage());
            }
            
            return null;
            
        } catch (Exception e) {
            System.err.println("❌ Erro geral ao buscar funcionário: " + e.getMessage());
            return null;
        }
    }
    
    private void removerCrachaAntigo(String re) {
        try {
            String caminhoAntigo = System.getProperty("user.dir") + "/output/cracha_frente_" + re + ".png";
            java.io.File arquivoAntigo = new java.io.File(caminhoAntigo);
            
            if (arquivoAntigo.exists()) {
                if (arquivoAntigo.delete()) {
                    System.out.println("🗑️ Crachá antigo removido: " + caminhoAntigo);
                } else {
                    System.err.println("⚠️ Não foi possível remover o crachá antigo: " + caminhoAntigo);
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Erro ao remover crachá antigo: " + e.getMessage());
        }
    }

    // Método para chamar atualizarTabela do App.java via callback
    private void chamarAtualizarTabela() {
        try {
            if (tabelaCallback != null) {
                System.out.println("🔄 Chamando atualização completa da tabela via callback...");
                tabelaCallback.atualizarTabela();
            } else {
                System.err.println("⚠️ TabelaCallback é null, fazendo atualização simples");
                table.revalidate();
                table.repaint();
            }
        } catch (Exception e) {
            System.err.println("❌ Erro ao atualizar tabela via callback: " + e.getMessage());
            e.printStackTrace();
        }
    }
}