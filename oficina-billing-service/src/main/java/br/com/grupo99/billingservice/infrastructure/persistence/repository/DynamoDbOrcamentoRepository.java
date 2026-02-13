package br.com.grupo99.billingservice.infrastructure.persistence.repository;

import br.com.grupo99.billingservice.infrastructure.persistence.entity.OrcamentoEntity;
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
 * DynamoDB Repository para OrcamentoEntity.
 *
 * ✅ CLEAN ARCHITECTURE: Repository específico do DynamoDB fica na
 * infrastructure
 * Substitui MongoOrcamentoRepository (Spring Data MongoDB)
 */
@Repository
public class DynamoDbOrcamentoRepository {

    private static final Logger log = LoggerFactory.getLogger(DynamoDbOrcamentoRepository.class);
    private final DynamoDbTable<OrcamentoEntity> table;

    public DynamoDbOrcamentoRepository(
            DynamoDbEnhancedClient enhancedClient,
            @Value("${aws.dynamodb.table-prefix:}") String tablePrefix) {
        String tableName = (tablePrefix != null && !tablePrefix.isBlank())
                ? tablePrefix + "orcamentos"
                : "orcamentos";
        this.table = enhancedClient.table(tableName, TableSchema.fromBean(OrcamentoEntity.class));
    }

    /**
     * Salva ou atualiza um OrcamentoEntity.
     * Gera ID e timestamps automaticamente.
     */
    public OrcamentoEntity save(OrcamentoEntity entity) {
        if (entity.getId() == null || entity.getId().isBlank()) {
            entity.setId(UUID.randomUUID().toString());
        }
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(Instant.now());
        }
        entity.setUpdatedAt(Instant.now());

        table.putItem(entity);
        log.debug("OrcamentoEntity salvo: {}", entity.getId());
        return entity;
    }

    /**
     * Busca OrcamentoEntity por ID (partition key).
     */
    public Optional<OrcamentoEntity> findById(String id) {
        OrcamentoEntity entity = table.getItem(Key.builder().partitionValue(id).build());
        return Optional.ofNullable(entity);
    }

    /**
     * Busca OrcamentoEntity por osId (scan com filtro).
     */
    public Optional<OrcamentoEntity> findByOsId(String osId) {
        ScanEnhancedRequest request = ScanEnhancedRequest.builder()
                .filterExpression(Expression.builder()
                        .expression("osId = :osId")
                        .putExpressionValue(":osId", AttributeValue.builder().s(osId).build())
                        .build())
                .build();

        return table.scan(request).items().stream().findFirst();
    }

    /**
     * Busca OrcamentoEntities por status (scan com filtro).
     */
    public List<OrcamentoEntity> findByStatus(String status) {
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
     * Verifica se existe um OrcamentoEntity para a OS.
     */
    public boolean existsByOsId(String osId) {
        return findByOsId(osId).isPresent();
    }

    /**
     * Busca todos os OrcamentoEntities (scan sem filtro).
     */
    public List<OrcamentoEntity> findAll() {
        return table.scan().items().stream().collect(Collectors.toList());
    }

    /**
     * Deleta OrcamentoEntity por ID.
     */
    public void deleteById(String id) {
        table.deleteItem(Key.builder().partitionValue(id).build());
        log.debug("OrcamentoEntity deletado: {}", id);
    }

    /**
     * Deleta todos os OrcamentoEntities (para uso em testes).
     */
    public void deleteAll() {
        findAll().forEach(entity -> deleteById(entity.getId()));
        log.debug("Todos os OrcamentoEntities deletados");
    }
}
