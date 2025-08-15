package br.com.cifmm.service;

import br.com.cifmm.model.FuncionarioModel;
import br.com.cifmm.util.REParser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class BuscarDados {

    // Método original mantido
    public FuncionarioModel buscarPorRe(String reDigitado) throws IOException {
        // ... código atual mantido igual ...
        // Monta a URL
        String url = "https://validar.mogimirim.sp.gov.br/Funcionarios?RE=" + reDigitado;
        System.out.println("Acessando URL: " + url);

        // Faz a requisição
        Document doc;
        try {
            doc = Jsoup.connect(url).userAgent("Mozilla/5.0").timeout(10000).get();
            System.out.println("HTML retornado (primeiros 500 caracteres): " + 
                doc.body().html().substring(0, Math.min(500, doc.body().html().length())));
        } catch (IOException e) {
            System.err.println("Erro ao acessar a URL: " + e.getMessage());
            throw new IOException("Falha ao conectar ao site: " + e.getMessage());
        }

        // Cria o objeto que será salvo
        FuncionarioModel funcionario = new FuncionarioModel();

        // Extrai o nome
        String nome = doc.select("p:contains(Nome:)").text().replace("Nome:", "").trim();
        if (nome.isEmpty()) {
            System.err.println("Nome não encontrado para RE: " + reDigitado);
        } else {
            System.out.println("Nome extraído: " + nome);
        }
        funcionario.setNome(nome.isEmpty() ? null : nome);

        // Define o RE
        String re = doc.select("p:contains(RE:)").text().replace("RE:", "").trim();
        if (re.isEmpty()) {
            System.err.println("RE não encontrado, usando RE digitado: " + reDigitado);
            re = reDigitado; // Fallback para o RE digitado
        } else {
            System.out.println("RE extraído: " + re);
        }
        funcionario.setRe(re);

        // Extrai o cargo
        String cargo = doc.select("p:contains(Cargo:)").text().replace("Cargo:", "").trim();
        if (cargo.isEmpty()) {
            System.err.println("Cargo não encontrado para RE: " + reDigitado);
        } else {
            System.out.println("Cargo extraído: " + cargo);
        }
        funcionario.setCargo(cargo.isEmpty() ? null : cargo);

        // Extrai a secretaria
        String secretaria = doc.select("p:contains(Locação de Trabalho:)").text().replace("Locação de Trabalho:", "").trim();
        if (secretaria.isEmpty()) {
            System.err.println("Locação de Trabalho/Secretaria não encontrada para RE: " + reDigitado);
        } else {
            System.out.println("Secretaria extraída: " + secretaria);
        }
        funcionario.setSecretaria(secretaria.isEmpty() ? null : secretaria);

        // Foto, qrcode e apelido deixamos null por enquanto
        funcionario.setFoto(null);
        funcionario.setQrcode(null);
        funcionario.setApelido(null);

        // Verifica se todos os campos obrigatórios estão vazios
        if (funcionario.getNome() == null && funcionario.getCargo() == null && funcionario.getSecretaria() == null) {
            throw new IOException("Nenhum dado válido encontrado para o RE: " + reDigitado);
        }

        return funcionario;
    }
    

    /**
     * NOVO MÉTODO: Busca múltiplos funcionários com controle de erro individual
     * @param res Lista de REs para buscar
     * @param callback Callback para progresso (opcional)
     * @return Mapa com resultados (RE -> Funcionário ou Erro)
     */
    public Map<String, BuscaResult> buscarMultiplos(List<String> res, ProgressCallback callback) {
        Map<String, BuscaResult> resultados = new HashMap<>();
        int total = res.size();
        
        System.out.println("=== INICIANDO BUSCA EM LOTE ===");
        System.out.println("Total de REs para processar: " + total);
        
        for (int i = 0; i < res.size(); i++) {
            String re = res.get(i);
            
            try {
                // Callback de progresso
                if (callback != null) {
                    callback.onProgress(i, total, "Buscando RE: " + re);
                }
                
                System.out.println("Processando " + (i + 1) + "/" + total + ": RE " + re);
                
                // Usa o método original
                FuncionarioModel funcionario = buscarPorRe(re);
                resultados.put(re, new BuscaResult(funcionario, null));
                
                System.out.println("✅ Sucesso para RE " + re + ": " + funcionario.getNome());
                
                // Pausa entre requests para não sobrecarregar o servidor
                if (i < res.size() - 1) { // Não pausar no último
                    Thread.sleep(800); // 800ms entre requests
                }
                
            } catch (Exception e) {
                String mensagemErro = "Erro para RE " + re + ": " + e.getMessage();
                System.err.println("❌ " + mensagemErro);
                resultados.put(re, new BuscaResult(null, e));
            }
        }
        
        // Estatísticas finais
        long sucessos = resultados.values().stream().mapToLong(r -> r.getSucesso() ? 1 : 0).sum();
        long erros = resultados.values().stream().mapToLong(r -> r.getSucesso() ? 0 : 1).sum();
        
        System.out.println("\n=== RELATÓRIO DE BUSCA ===");
        System.out.println("Total processados: " + total);
        System.out.println("Sucessos: " + sucessos);
        System.out.println("Erros: " + erros);
        System.out.println("Taxa de sucesso: " + String.format("%.1f%%", (double) sucessos / total * 100));
        
        return resultados;
    }
    
    /**
     * Classe para encapsular resultado da busca (sucesso ou erro)
     */
    public static class BuscaResult {
        private final FuncionarioModel funcionario;
        private final Exception erro;
        
        public BuscaResult(FuncionarioModel funcionario, Exception erro) {
            this.funcionario = funcionario;
            this.erro = erro;
        }
        
        public boolean getSucesso() {
            return funcionario != null && erro == null;
        }
        
        public FuncionarioModel getFuncionario() {
            return funcionario;
        }
        
        public Exception getErro() {
            return erro;
        }
        
        public String getMensagemErro() {
            return erro != null ? erro.getMessage() : null;
        }
    }
    
    /**
     * Interface para callback de progresso
     */
    public interface ProgressCallback {
        void onProgress(int atual, int total, String mensagem);
    }
}