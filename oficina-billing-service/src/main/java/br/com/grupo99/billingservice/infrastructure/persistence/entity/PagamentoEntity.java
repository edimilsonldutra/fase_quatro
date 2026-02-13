package br.com.grupo99.billingservice.infrastructure.persistence.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Entity DynamoDB para Pagamento
 * 
 * ✅ CLEAN ARCHITECTURE: Entity fica na infrastructure layer
 * Migrado de MongoDB (@Document) para DynamoDB (@DynamoDbBean)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class PagamentoEntity {

    private String id;

    private String orcamentoId;

    private String osId;

    @Builder.Default
    private String status = "PENDENTE";

    private BigDecimal valor;

    private String formaPagamento;

    private String comprovante;

    private Instant dataPagamento;

    private Instant dataEstorno;

    private String motivoEstorno;

    private Instant createdAt;

    private Instant updatedAt;

    @DynamoDbPartitionKey
    public String getId() {
        return this.id;
    }
}
