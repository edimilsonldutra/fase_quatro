package br.com.grupo99.billingservice.infrastructure.persistence.repository;

import br.com.grupo99.billingservice.infrastructure.persistence.entity.PagamentoEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * DynamoDB Repository para PagamentoEntity.
 *
 * ✅ CLEAN ARCHITECTURE: Repository específico do DynamoDB fica na
 * infrastructure
 * Substitui MongoPagamentoRepository (Spring Data MongoDB)
 */
@Repository
public class DynamoDbPagamentoRepository {

    private static final Logger log = LoggerFactory.getLogger(DynamoDbPagamentoRepository.class);
    private final DynamoDbTable<PagamentoEntity> table;

    public DynamoDbPagamentoRepository(
            DynamoDbEnhancedClient enhancedClient,
            @Value("${aws.dynamodb.table-prefix:}") String tablePrefix) {
        String tableName = (tablePrefix != null && !tablePrefix.isBlank())
                ? tablePrefix + "pagamentos"
                : "pagamentos";
        this.table = enhancedClient.table(tableName, TableSchema.fromBean(PagamentoEntity.class));
    }

    /**
     * Salva ou atualiza um PagamentoEntity.
     * Gera ID e timestamps automaticamente.
     */
    public PagamentoEntity save(PagamentoEntity entity) {
        if (entity.getId() == null || entity.getId().isBlank()) {
            entity.setId(UUID.randomUUID().toString());
        }
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(Instant.now());
        }
        entity.setUpdatedAt(Instant.now());

        table.putItem(entity);
        log.debug("PagamentoEntity salvo: {}", entity.getId());
        return entity;
    }

    /**
     * Busca PagamentoEntity por ID (partition key).
     */
    public Optional<PagamentoEntity> findById(String id) {
        PagamentoEntity entity = table.getItem(Key.builder().partitionValue(id).build());
        return Optional.ofNullable(entity);
    }

    /**
     * Busca PagamentoEntity por orcamentoId (scan com filtro).
     */
    public Optional<PagamentoEntity> findByOrcamentoId(String orcamentoId) {
        ScanEnhancedRequest request = ScanEnhancedRequest.builder()
                .filterExpression(Expression.builder()
                        .expression("orcamentoId = :orcamentoId")
                        .putExpressionValue(":orcamentoId", AttributeValue.builder().s(orcamentoId).build())
                        .build())
                .build();

        return table.scan(request).items().stream().findFirst();
    }

    /**
     * Busca PagamentoEntities por osId (scan com filtro).
     */
    public List<PagamentoEntity> findByOsId(String osId) {
        ScanEnhancedRequest request = ScanEnhancedRequest.builder()
                .filterExpression(Expression.builder()
                        .expression("osId = :osId")
                        .putExpressionValue(":osId", AttributeValue.builder().s(osId).build())
                        .build())
                .build();

        return table.scan(request).items().stream().collect(Collectors.toList());
    }

    /**
     * Busca PagamentoEntities por status (scan com filtro).
     */
    public List<PagamentoEntity> findByStatus(String status) {
        ScanEnhancedRequest request = ScanEnhancedRequest.builder()
                .filterExpression(Expression.builder()
                        .expression("#st = :status")
                        .putExpressionName("#st", "status")
                        .putExpressionValue(":status", AttributeValue.builder().s(status).build())
                        .build())
                .build();

        return table.scan(request).items().stream().collect(Collectors.toList());
    }

    /**
     * Verifica se existe PagamentoEntity com orcamentoId e status específicos.
     */
    public boolean existsByOrcamentoIdAndStatus(String orcamentoId, String status) {
        ScanEnhancedRequest request = ScanEnhancedRequest.builder()
                .filterExpression(Expression.builder()
                        .expression("orcamentoId = :orcamentoId AND #st = :status")
                        .putExpressionName("#st", "status")
                        .putExpressionValue(":orcamentoId", AttributeValue.builder().s(orcamentoId).build())
                        .putExpressionValue(":status", AttributeValue.builder().s(status).build())
                        .build())
                .build();

        return table.scan(request).items().stream().findFirst().isPresent();
    }

    /**
     * Deleta PagamentoEntity por ID.
     */
    public void deleteById(String id) {
        table.deleteItem(Key.builder().partitionValue(id).build());
        log.debug("PagamentoEntity deletado: {}", id);
    }
}
