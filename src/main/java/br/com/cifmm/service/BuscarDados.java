package br.com.cifmm.service;

import br.com.cifmm.model.FuncionarioModel;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class BuscarDados {

    public FuncionarioModel buscarPorRe(String reDigitado) throws IOException {
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

        // Extrai a secretaria (Locação de Trabalho)
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
}