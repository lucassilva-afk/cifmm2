package br.com.cifmm.model;

import java.time.LocalDateTime;
import java.util.Objects;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "funcionarios")
public class FuncionarioModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String re;          // Campo para armazenar o RE
    private String nome;
    private String cargo;
    private String secretaria;

    private String foto;        // Pode ser null
    private String qrcode;      // Pode ser null
    private String apelido;     // Pode ser null

    // NOVO CAMPO PARA CONTROLE DE CACHE
    private LocalDateTime dataUltimaAtualizacao;

    // Getters e setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRe() {
        return re;
    }

    public void setRe(String re) {
        this.re = re;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCargo() {
        return cargo;
    }

    public void setCargo(String cargo) {
        this.cargo = cargo;
    }

    public String getSecretaria() {
        return secretaria;
    }

    public void setSecretaria(String secretaria) {
        this.secretaria = secretaria;
    }

    public String getFoto() {
        return foto;
    }

    public void setFoto(String foto) {
        this.foto = foto;
    }

    public String getQrcode() {
        return qrcode;
    }

    public void setQrcode(String qrcode) {
        this.qrcode = qrcode;
    }

    public String getApelido() {
        return apelido;
    }

    public void setApelido(String apelido) {
        this.apelido = apelido;
    }

    // NOVO GETTER E SETTER PARA CONTROLE DE CACHE
    public LocalDateTime getDataUltimaAtualizacao() {
        return dataUltimaAtualizacao;
    }

    public void setDataUltimaAtualizacao(LocalDateTime dataUltimaAtualizacao) {
        this.dataUltimaAtualizacao = dataUltimaAtualizacao;
    }

    /**
     * Retorna o caminho dinâmico para a foto do crachá da frente.
     */
    public String getCaminhoFotoFrente() {
        if (this.re != null && !this.re.trim().isEmpty()) {
            return System.getProperty("user.dir") + "/output/cracha_frente_" + this.re + ".png";
        }
        return null;
    }

    @Override
    public String toString() {
        return "FuncionarioModel [re=" + re + ", nome=" + nome + ", cargo=" + cargo + 
               ", secretaria=" + secretaria + ", apelido=" + apelido + 
               ", dataUltimaAtualizacao=" + dataUltimaAtualizacao + "]";
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FuncionarioModel that = (FuncionarioModel) o;
        return Objects.equals(re, that.re);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(re);
    }
}