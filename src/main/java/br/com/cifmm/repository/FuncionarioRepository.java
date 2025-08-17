package br.com.cifmm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

import br.com.cifmm.model.FuncionarioModel;

@Repository
//In your FuncionarioRepository interface
public interface FuncionarioRepository extends JpaRepository<FuncionarioModel, Long> {
 
 // Change this method to return Optional to handle multiple results
 Optional<FuncionarioModel> findFirstByRe(String re);
 
 // Or if you want to get all records with the same RE
 List<FuncionarioModel> findAllByRe(String re);
 
 // Keep the original method but handle the exception in the calling code
 FuncionarioModel findByRe(String re);
}