package br.com.cifmm.view.components;

/**
 * Interface de callback para permitir que componentes chamem
 * métodos de atualização da tabela no App principal.
 */
public interface TabelaCallback {
    
    /**
     * Atualiza a tabela completamente, recarregando todos os dados.
     */
    void atualizarTabela();
    
    /**
     * Atualiza apenas uma linha específica da tabela.
     * @param linha Índice da linha a ser atualizada
     * @param re RE do funcionário
     */
    default void atualizarLinha(int linha, String re) {
        // Implementação padrão que chama atualização completa
        atualizarTabela();
    }
}