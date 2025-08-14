package br.com.cifmm.control;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import br.com.cifmm.service.BuscarDados;
import br.com.cifmm.service.FuncionarioService;
import br.com.cifmm.service.GerarCrachas; // Importar a classe GerarCrachas

@Component
public class FuncionarioControl {
   
    @Autowired
    private BuscarDados buscarDados;    
    
    private final FuncionarioService funcionarioService;
    
    private final GerarCrachas gerarCrachas; // Adicionar o serviço de geração de crachás

    public void salvarFuncionario(String re) {
        try {
            var funcionario = buscarDados.buscarPorRe(re); // Retorna entidade preenchida
            funcionarioService.salvar(funcionario);
            System.out.println("[OK] Funcionário salvo: " + funcionario.getNome());
            
            // Após salvar, gerar o crachá com os dados do funcionário
            gerarCrachas.gerarCracha(funcionario);
            System.out.println("[OK] Crachá gerado para o funcionário: " + funcionario.getNome());
            
        } catch (Exception e) {
            System.err.println("[ERRO] Falha ao salvar funcionário ou gerar crachá: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public FuncionarioControl(BuscarDados buscarDados, FuncionarioService funcionarioService, GerarCrachas gerarCrachas) {
        this.buscarDados = buscarDados;
        this.funcionarioService = funcionarioService;
        this.gerarCrachas = gerarCrachas; // Incluir no construtor para injeção
    }

}