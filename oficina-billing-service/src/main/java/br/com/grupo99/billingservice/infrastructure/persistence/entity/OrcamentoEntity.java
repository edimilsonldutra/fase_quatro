package br.com.grupo99.billingservice.infrastructure.persistence.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity DynamoDB para Orcamento
 * 
 * ✅ CLEAN ARCHITECTURE: Entity fica na infrastructure layer
 * Domain models não conhecem detalhes de persistência
 * 
 * Migrado de MongoDB (@Document) para DynamoDB (@DynamoDbBean)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class OrcamentoEntity {

    private String id;

    private String osId;

    @Builder.Default
    private String status = "PENDENTE";

    @Builder.Default
    private List<ItemOrcamentoEntity> itens = new ArrayList<>();

    private BigDecimal valorTotal;

    private Instant dataGeracao;

    private Instant dataAprovacao;

    private Instant dataRejeicao;

    private String observacao;

    private String motivoRejeicao;

    @Builder.Default
    private List<HistoricoStatusEntity> historico = new ArrayList<>();

    private Instant createdAt;

    private Instant updatedAt;

    @DynamoDbPartitionKey
    public String getId() {
        return this.id;
    }

    /**
     * Item do Orcamento (nested DynamoDbBean)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @DynamoDbBean
    public static class ItemOrcamentoEntity {
        private String tipo;
        private String descricao;
        private BigDecimal valorUnitario;
        private BigDecimal valorTotal;
        private Integer quantidade;
    }

    /**
     * Histórico de status (nested DynamoDbBean)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @DynamoDbBean
    public static class HistoricoStatusEntity {
        private String statusAnterior;
        private String novoStatus;
        private String usuario;
        private String observacao;
        private Instant data;
    }
}
