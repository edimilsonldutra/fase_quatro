package br.com.grupo99.catalogo.domain.model;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Model DynamoDB para Serviço.
 * 
 * Migrado de MongoDB/DocumentDB para DynamoDB:
 * - Schema flexível via listas
 * - Alta performance e escalabilidade
 * - Free Tier da AWS
 */
@DynamoDbBean
public class Servico {

    private String id;

    private String nome;

    private String descricao;

    private BigDecimal preco;

    private Integer tempoEstimadoMinutos;

    private Boolean ativo = true;

    // Campos flexíveis - armazenados nativamente no DynamoDB
    private List<String> categorias;

    private List<String> pecasNecessarias;

    private List<String> requisitos;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public Servico() {
        this.id = UUID.randomUUID().toString();
    }

    public Servico(String nome, String descricao, BigDecimal preco, Integer tempoEstimadoMinutos) {
        this.id = UUID.randomUUID().toString();
        this.nome = nome;
        this.descricao = descricao;
        this.preco = preco;
        this.tempoEstimadoMinutos = tempoEstimadoMinutos;
        this.ativo = true;
    }

    // Getters e Setters
    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public void setPreco(BigDecimal preco) {
        this.preco = preco;
    }

    public Integer getTempoEstimadoMinutos() {
        return tempoEstimadoMinutos;
    }

    public void setTempoEstimadoMinutos(Integer tempoEstimadoMinutos) {
        this.tempoEstimadoMinutos = tempoEstimadoMinutos;
    }

    public Boolean getAtivo() {
        return ativo;
    }

    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }

    public List<String> getCategorias() {
        return categorias;
    }

    public void setCategorias(List<String> categorias) {
        this.categorias = categorias;
    }

    public List<String> getPecasNecessarias() {
        return pecasNecessarias;
    }

    public void setPecasNecessarias(List<String> pecasNecessarias) {
        this.pecasNecessarias = pecasNecessarias;
    }

    public List<String> getRequisitos() {
        return requisitos;
    }

    public void setRequisitos(List<String> requisitos) {
        this.requisitos = requisitos;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Servico servico = (Servico) o;
        return id != null && id.equals(servico.id);
    }

    @Override
    public int hashCode() {
        return 31;
    }
}
