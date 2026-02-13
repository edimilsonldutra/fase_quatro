package br.com.grupo99.catalogo.domain.model;

import br.com.grupo99.catalogo.adapter.config.JsonMapConverter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbConvertedBy;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Model DynamoDB para Peça.
 * 
 * Migrado de MongoDB/DocumentDB para DynamoDB:
 * - Schema flexível (especificações via Map armazenado como JSON)
 * - Alta performance e escalabilidade
 * - Free Tier da AWS (25GB, 25 RCU, 25 WCU)
 */
@DynamoDbBean
public class Peca {

    private String id;

    private String nome;

    private String descricao;

    private String codigoFabricante;

    private BigDecimal preco;

    private Integer quantidade;

    private Integer quantidadeMinima = 5;

    private Boolean ativo = true;

    // Campos flexíveis - armazenados nativamente no DynamoDB
    private List<String> categorias;

    private Map<String, Object> especificacoes;

    private List<String> compatibilidade;

    private String marca;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public Peca() {
        this.id = UUID.randomUUID().toString();
    }

    public Peca(String nome, String descricao, String codigoFabricante, BigDecimal preco, Integer quantidade) {
        this.id = UUID.randomUUID().toString();
        this.nome = nome;
        this.descricao = descricao;
        this.codigoFabricante = codigoFabricante;
        this.preco = preco;
        this.quantidade = quantidade;
        this.ativo = true;
    }

    public void decrementarQuantidade(Integer quantidade) {
        if (this.quantidade < quantidade) {
            throw new IllegalArgumentException("Quantidade insuficiente em estoque");
        }
        this.quantidade -= quantidade;
    }

    public void incrementarQuantidade(Integer quantidade) {
        this.quantidade += quantidade;
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

    public String getCodigoFabricante() {
        return codigoFabricante;
    }

    public void setCodigoFabricante(String codigoFabricante) {
        this.codigoFabricante = codigoFabricante;
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public void setPreco(BigDecimal preco) {
        this.preco = preco;
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(Integer quantidade) {
        this.quantidade = quantidade;
    }

    public Integer getQuantidadeMinima() {
        return quantidadeMinima;
    }

    public void setQuantidadeMinima(Integer quantidadeMinima) {
        this.quantidadeMinima = quantidadeMinima;
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

    @DynamoDbConvertedBy(JsonMapConverter.class)
    public Map<String, Object> getEspecificacoes() {
        return especificacoes;
    }

    public void setEspecificacoes(Map<String, Object> especificacoes) {
        this.especificacoes = especificacoes;
    }

    public List<String> getCompatibilidade() {
        return compatibilidade;
    }

    public void setCompatibilidade(List<String> compatibilidade) {
        this.compatibilidade = compatibilidade;
    }

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
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
        Peca peca = (Peca) o;
        return id != null && id.equals(peca.id);
    }

    @Override
    public int hashCode() {
        return 31;
    }
}
