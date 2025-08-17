package br.com.cifmm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

import br.com.cifmm.model.FuncionarioModel;

@Repository
public interface FuncionarioRepository extends JpaRepository<FuncionarioModel, Long> {
    
    /**
     * Busca funcionário pelo RE (retorna o primeiro encontrado se houver duplicatas)
     * @param re Número do RE do funcionário
     * @return FuncionarioModel ou null se não encontrado
     */
    Optional<FuncionarioModel> findFirstByRe(String re);
    FuncionarioModel findByRe(String re);
    
    /**
     * Busca todos os funcionários pelo RE
     * @param re Número do RE do funcionário
     * @return Lista de FuncionarioModel
     */
    List<FuncionarioModel> findAllByRe(String re);
    
    /**
     * Verifica se existe funcionário com o RE informado
     * @param re Número do RE
     * @return true se existe, false caso contrário
     */
    boolean existsByRe(String re);
}