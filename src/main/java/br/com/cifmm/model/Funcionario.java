package br.com.cifmm.model;

// IMPORTS

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "funcionario")
public class Funcionario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;

    public Funcionario() {
    }

    public Funcionario(String nome) {
        this.nome = nome;
    }

    // Getters e setters
    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

	public void setRe(String string) {
		// TODO Auto-generated method stub
		
	}

	public void setMatricula(String text) {
		// TODO Auto-generated method stub
		
	}

	public void setCargo(String trim) {
		// TODO Auto-generated method stub
		
	}

	public void setSecretaria(String trim) {
		// TODO Auto-generated method stub
		
	}	

	public String getCargo() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getSecretaria() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getRe() {
		// TODO Auto-generated method stub
		return null;
	}
}

