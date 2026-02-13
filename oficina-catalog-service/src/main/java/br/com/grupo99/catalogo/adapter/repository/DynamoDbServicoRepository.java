package br.com.grupo99.catalogo.adapter.repository;

import br.com.grupo99.catalogo.domain.model.Servico;
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

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * DynamoDB Repository para Servico.
 *
 * ✅ CLEAN ARCHITECTURE: Repository específico do DynamoDB fica no adapter
 * Substitui ServicoMongoRepository (Spring Data MongoDB)
 */
@Repository
public class DynamoDbServicoRepository {

    private static final Logger log = LoggerFactory.getLogger(DynamoDbServicoRepository.class);
    private final DynamoDbTable<Servico> table;

    public DynamoDbServicoRepository(
            DynamoDbEnhancedClient enhancedClient,
            @Value("${aws.dynamodb.table-prefix:}") String tablePrefix) {
        String tableName = (tablePrefix != null && !tablePrefix.isBlank())
                ? tablePrefix + "servicos"
                : "servicos";
        this.table = enhancedClient.table(tableName, TableSchema.fromBean(Servico.class));
    }

    public Servico save(Servico servico) {
        if (servico.getId() == null || servico.getId().isBlank()) {
            servico.setId(UUID.randomUUID().toString());
        }
        if (servico.getCreatedAt() == null) {
            servico.setCreatedAt(LocalDateTime.now());
        }
        servico.setUpdatedAt(LocalDateTime.now());

        table.putItem(servico);
        log.debug("Servico salvo: {}", servico.getId());
        return servico;
    }

    public Optional<Servico> findById(String id) {
        Servico entity = table.getItem(Key.builder().partitionValue(id).build());
        return Optional.ofNullable(entity);
    }

    public List<Servico> findAll() {
        return table.scan().items().stream().collect(Collectors.toList());
    }

    public List<Servico> findByAtivoTrue() {
        ScanEnhancedRequest request = ScanEnhancedRequest.builder()
                .filterExpression(Expression.builder()
                        .expression("ativo = :ativo")
                        .putExpressionValue(":ativo", AttributeValue.builder().bool(true).build())
                        .build())
                .build();

        return table.scan(request).items().stream().collect(Collectors.toList());
    }

    public List<Servico> findByAtivoTrueOrderByNomeAsc() {
        return findByAtivoTrue().stream()
                .sorted(Comparator.comparing(Servico::getNome, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    public List<Servico> findByCategoriasContainingAndAtivoTrue(String categoria) {
        ScanEnhancedRequest request = ScanEnhancedRequest.builder()
                .filterExpression(Expression.builder()
                        .expression("contains(categorias, :cat) AND ativo = :ativo")
                        .putExpressionValue(":cat", AttributeValue.builder().s(categoria).build())
                        .putExpressionValue(":ativo", AttributeValue.builder().bool(true).build())
                        .build())
                .build();

        return table.scan(request).items().stream().collect(Collectors.toList());
    }

    public List<Servico> findByTempoEstimadoMinutosLessThanEqualAndAtivoTrue(Integer tempoMaximo) {
        ScanEnhancedRequest request = ScanEnhancedRequest.builder()
                .filterExpression(Expression.builder()
                        .expression("tempoEstimadoMinutos <= :tempo AND ativo = :ativo")
                        .putExpressionValue(":tempo", AttributeValue.builder().n(String.valueOf(tempoMaximo)).build())
                        .putExpressionValue(":ativo", AttributeValue.builder().bool(true).build())
                        .build())
                .build();

        return table.scan(request).items().stream().collect(Collectors.toList());
    }

    public void deleteById(String id) {
        table.deleteItem(Key.builder().partitionValue(id).build());
        log.debug("Servico deletado: {}", id);
    }

    public boolean existsById(String id) {
        return findById(id).isPresent();
    }
}
