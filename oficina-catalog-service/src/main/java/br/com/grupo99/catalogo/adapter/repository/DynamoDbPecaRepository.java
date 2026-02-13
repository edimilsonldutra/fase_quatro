package br.com.grupo99.catalogo.adapter.repository;

import br.com.grupo99.catalogo.domain.model.Peca;
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
 * DynamoDB Repository para Peca.
 *
 * ✅ CLEAN ARCHITECTURE: Repository específico do DynamoDB fica no adapter
 * Substitui PecaMongoRepository (Spring Data MongoDB)
 */
@Repository
public class DynamoDbPecaRepository {

    private static final Logger log = LoggerFactory.getLogger(DynamoDbPecaRepository.class);
    private final DynamoDbTable<Peca> table;

    public DynamoDbPecaRepository(
            DynamoDbEnhancedClient enhancedClient,
            @Value("${aws.dynamodb.table-prefix:}") String tablePrefix) {
        String tableName = (tablePrefix != null && !tablePrefix.isBlank())
                ? tablePrefix + "pecas"
                : "pecas";
        this.table = enhancedClient.table(tableName, TableSchema.fromBean(Peca.class));
    }

    public Peca save(Peca peca) {
        if (peca.getId() == null || peca.getId().isBlank()) {
            peca.setId(UUID.randomUUID().toString());
        }
        if (peca.getCreatedAt() == null) {
            peca.setCreatedAt(LocalDateTime.now());
        }
        peca.setUpdatedAt(LocalDateTime.now());

        table.putItem(peca);
        log.debug("Peca salva: {}", peca.getId());
        return peca;
    }

    public Optional<Peca> findById(String id) {
        Peca entity = table.getItem(Key.builder().partitionValue(id).build());
        return Optional.ofNullable(entity);
    }

    public List<Peca> findAll() {
        return table.scan().items().stream().collect(Collectors.toList());
    }

    public List<Peca> findByAtivoTrue() {
        ScanEnhancedRequest request = ScanEnhancedRequest.builder()
                .filterExpression(Expression.builder()
                        .expression("ativo = :ativo")
                        .putExpressionValue(":ativo", AttributeValue.builder().bool(true).build())
                        .build())
                .build();

        return table.scan(request).items().stream().collect(Collectors.toList());
    }

    public List<Peca> findByAtivoTrueOrderByNomeAsc() {
        return findByAtivoTrue().stream()
                .sorted(Comparator.comparing(Peca::getNome, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    public Optional<Peca> findByCodigoFabricante(String codigoFabricante) {
        ScanEnhancedRequest request = ScanEnhancedRequest.builder()
                .filterExpression(Expression.builder()
                        .expression("codigoFabricante = :codigo")
                        .putExpressionValue(":codigo", AttributeValue.builder().s(codigoFabricante).build())
                        .build())
                .build();

        return table.scan(request).items().stream().findFirst();
    }

    public List<Peca> findByCategoriasContainingAndAtivoTrue(String categoria) {
        ScanEnhancedRequest request = ScanEnhancedRequest.builder()
                .filterExpression(Expression.builder()
                        .expression("contains(categorias, :cat) AND ativo = :ativo")
                        .putExpressionValue(":cat", AttributeValue.builder().s(categoria).build())
                        .putExpressionValue(":ativo", AttributeValue.builder().bool(true).build())
                        .build())
                .build();

        return table.scan(request).items().stream().collect(Collectors.toList());
    }

    public List<Peca> findByMarcaAndAtivoTrue(String marca) {
        ScanEnhancedRequest request = ScanEnhancedRequest.builder()
                .filterExpression(Expression.builder()
                        .expression("marca = :marca AND ativo = :ativo")
                        .putExpressionValue(":marca", AttributeValue.builder().s(marca).build())
                        .putExpressionValue(":ativo", AttributeValue.builder().bool(true).build())
                        .build())
                .build();

        return table.scan(request).items().stream().collect(Collectors.toList());
    }

    public List<Peca> findByQuantidadeLessThanEqualAndAtivoTrue(Integer quantidade) {
        ScanEnhancedRequest request = ScanEnhancedRequest.builder()
                .filterExpression(Expression.builder()
                        .expression("quantidade <= :qtd AND ativo = :ativo")
                        .putExpressionValue(":qtd", AttributeValue.builder().n(String.valueOf(quantidade)).build())
                        .putExpressionValue(":ativo", AttributeValue.builder().bool(true).build())
                        .build())
                .build();

        return table.scan(request).items().stream().collect(Collectors.toList());
    }

    public void deleteById(String id) {
        table.deleteItem(Key.builder().partitionValue(id).build());
        log.debug("Peca deletada: {}", id);
    }

    public boolean existsById(String id) {
        return findById(id).isPresent();
    }
}
