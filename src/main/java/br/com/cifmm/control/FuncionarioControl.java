package br.com.cifmm.control;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import br.com.cifmm.service.BuscarDados;
import br.com.cifmm.service.FuncionarioService;
import br.com.cifmm.service.GerarCrachas;
import br.com.cifmm.util.REParser;

import java.util.List;
import java.util.Map;

@Component
public class FuncionarioControl {
   
    @Autowired
    private BuscarDados buscarDados;    
    
    private final FuncionarioService funcionarioService;
    private final GerarCrachas gerarCrachas;

    public FuncionarioControl(BuscarDados buscarDados, FuncionarioService funcionarioService, GerarCrachas gerarCrachas) {
        this.buscarDados = buscarDados;
        this.funcionarioService = funcionarioService;
        this.gerarCrachas = gerarCrachas;
    }

    // Método original mantido
    public void salvarFuncionario(String re) {
        try {
            var funcionario = buscarDados.buscarPorRe(re);
            funcionarioService.salvar(funcionario);
            System.out.println("[OK] Funcionário salvo: " + funcionario.getNome());
            
            gerarCrachas.gerarCracha(funcionario);
            System.out.println("[OK] Crachá gerado para o funcionário: " + funcionario.getNome());
            
        } catch (Exception e) {
            System.err.println("[ERRO] Falha ao salvar funcionário ou gerar crachá: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * NOVO MÉTODO: Processa entrada do JTextField (única ou múltipla)
     * @param input String do JTextField (ex: "12790" ou "12790,14359,14237")
     * @param callback Callback para progresso (opcional)
     * @return Relatório do processamento
     */
    public RelatorioProcessamento processarEntrada(String input, ProgressCallback callback) {
        System.out.println("=== PROCESSANDO ENTRADA ===");
        System.out.println("Input recebido: " + input);
        
        // Parse da entrada
        List<String> res = REParser.parseREs(input);
        
        if (res.isEmpty()) {
            throw new IllegalArgumentException("Nenhum RE válido encontrado na entrada: " + input);
        }
        
        System.out.println("REs encontrados: " + res.size());
        System.out.println("REs: " + res);
        
        // Determinar se é processamento único ou múltiplo
        if (res.size() == 1) {
            return processarUnico(res.get(0), callback);
        } else {
            return processarMultiplos(res, callback);
        }
    }
    
    /**
     * Processa um único RE (reutiliza método atual)
     */
    /**
     * Processa um único RE com progresso mais detalhado
     */
    private RelatorioProcessamento processarUnico(String re, ProgressCallback callback) {
        try {
            // 🔄 Etapa 1: Iniciando
            if (callback != null) {
                callback.onProgress(0, 100, "Iniciando processamento do RE: " + re);
                Thread.sleep(200); // Pequena pausa para visualizar
            }
            
            // 🔄 Etapa 2: Buscando dados
            if (callback != null) {
                callback.onProgress(20, 100, "Buscando dados do funcionário: " + re);
                Thread.sleep(300);
            }
            
            // Buscar dados do funcionário
            var funcionario = buscarDados.buscarPorRe(re);
            
            // 🔄 Etapa 3: Dados encontrados
            if (callback != null) {
                callback.onProgress(50, 100, "Dados encontrados: " + funcionario.getNome());
                Thread.sleep(200);
            }
            
            // 🔄 Etapa 4: Salvando no banco
            if (callback != null) {
                callback.onProgress(70, 100, "Salvando funcionário no banco de dados...");
                Thread.sleep(300);
            }
            
            funcionarioService.salvar(funcionario);
            System.out.println("[OK] Funcionário salvo: " + funcionario.getNome());
            
            // 🔄 Etapa 5: Gerando crachá
            if (callback != null) {
                callback.onProgress(85, 100, "Gerando crachá para " + funcionario.getNome());
                Thread.sleep(400);
            }
            
            gerarCrachas.gerarCracha(funcionario);
            System.out.println("[OK] Crachá gerado para o funcionário: " + funcionario.getNome());
            
            // 🔄 Etapa 6: Finalizando
            if (callback != null) {
                callback.onProgress(100, 100, "Processamento concluído!");
                Thread.sleep(200);
            }
            
            if (callback != null) {
                callback.onComplete(1, 0, "Processamento concluído com sucesso!");
            }
            
            return new RelatorioProcessamento(1, 1, 0);
            
        } catch (Exception e) {
            if (callback != null) {
                callback.onProgress(100, 100, "Erro: " + e.getMessage());
                callback.onComplete(0, 1, "Erro: " + e.getMessage());
            }
            return new RelatorioProcessamento(1, 0, 1);
        }
    }
    
    /**
     * Processa múltiplos REs
     */
    private RelatorioProcessamento processarMultiplos(List<String> res, ProgressCallback callback) {
        System.out.println("=== PROCESSAMENTO EM LOTE ===");
        
        // 1. Buscar dados de todos os REs
        Map<String, BuscarDados.BuscaResult> resultadosBusca = buscarDados.buscarMultiplos(res, 
            (atual, total, mensagem) -> {
                if (callback != null) {
                    callback.onProgress(atual, total * 2, "Fase 1/2 - " + mensagem);
                }
            });
        
        // 2. Salvar e gerar crachás para os que deram certo
        int sucessos = 0;
        int erros = 0;
        int currentIndex = res.size(); // Continua a partir de onde parou
        
        for (String re : res) {
            BuscarDados.BuscaResult resultado = resultadosBusca.get(re);
            
            if (callback != null) {
                callback.onProgress(currentIndex, res.size() * 2, "Fase 2/2 - Gerando crachá: " + re);
            }
            
            if (resultado.getSucesso()) {
                try {
                    // Salvar no banco
                    funcionarioService.salvar(resultado.getFuncionario());
                    System.out.println("💾 Funcionário salvo: " + resultado.getFuncionario().getNome());
                    
                    // Gerar crachá
                    gerarCrachas.gerarCracha(resultado.getFuncionario());
                    System.out.println("🏷️ Crachá gerado para: " + resultado.getFuncionario().getNome());
                    
                    sucessos++;
                    
                } catch (Exception e) {
                    System.err.println("❌ Erro ao processar RE " + re + ": " + e.getMessage());
                    erros++;
                }
            } else {
                System.err.println("⏭️ Pulando RE " + re + " devido a erro na busca");
                erros++;
            }
            
            currentIndex++;
        }
        
        if (callback != null) {
            callback.onComplete(sucessos, erros, String.format(
                "Processamento concluído! Sucessos: %d, Erros: %d", sucessos, erros));
        }
        
        return new RelatorioProcessamento(res.size(), sucessos, erros);
    }
    
    /**
     * Interface para callback de progresso
     */
    public interface ProgressCallback {
        void onProgress(int atual, int total, String mensagem);
        void onComplete(int sucessos, int erros, String mensagemFinal);
    }
    
    /**
     * Classe para relatório de processamento
     */
    public static class RelatorioProcessamento {
        private final int total;
        private final int sucessos;
        private final int erros;
        
        public RelatorioProcessamento(int total, int sucessos, int erros) {
            this.total = total;
            this.sucessos = sucessos;
            this.erros = erros;
        }
        
        // Getters
        public int getTotal() { return total; }
        public int getSucessos() { return sucessos; }
        public int getErros() { return erros; }
        
        public double getTaxaSucesso() {
            return total > 0 ? (double) sucessos / total * 100 : 0;
        }
        
        @Override
        public String toString() {
            return String.format("Total: %d, Sucessos: %d, Erros: %d (%.1f%% sucesso)", 
                total, sucessos, erros, getTaxaSucesso());
        }
    }
}