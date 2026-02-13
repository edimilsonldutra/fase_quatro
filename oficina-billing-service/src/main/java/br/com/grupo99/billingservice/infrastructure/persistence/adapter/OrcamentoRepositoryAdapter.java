package br.com.grupo99.billingservice.infrastructure.persistence.adapter;

import br.com.grupo99.billingservice.domain.model.Orcamento;
import br.com.grupo99.billingservice.domain.model.StatusOrcamento;
import br.com.grupo99.billingservice.domain.repository.OrcamentoRepository;
import br.com.grupo99.billingservice.infrastructure.persistence.repository.DynamoDbOrcamentoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Adapter Repository implementando OrcamentoRepository (domain interface)
 * 
 * ✅ CLEAN ARCHITECTURE - ADAPTER PATTERN:
 * - Implementa a interface de domínio (OrcamentoRepository)
 * - Adapta DynamoDB (DynamoDbOrcamentoRepository) para domínio
 * - Domain não conhece detalhes de persistência
 * - Migrado de MongoDB para DynamoDB
 */
@Slf4j
@Component
public class OrcamentoRepositoryAdapter implements OrcamentoRepository {

    private final DynamoDbOrcamentoRepository dynamoDbRepository;
    private final OrcamentoEntityMapper mapper;

    public OrcamentoRepositoryAdapter(
            DynamoDbOrcamentoRepository dynamoDbRepository,
            OrcamentoEntityMapper mapper) {
        this.dynamoDbRepository = dynamoDbRepository;
        this.mapper = mapper;
    }

    /**
     * Salva um Orcamento
     * 
     * Fluxo: Domain Model → Entity → DynamoDB
     */
    @Override
    public Orcamento save(Orcamento orcamento) {
        log.debug("Salvando orcamento: {}", orcamento.getId());

        // 1. Domain → Entity
        var entity = mapper.toEntity(orcamento);

        // 2. Entity → DynamoDB
        var saved = dynamoDbRepository.save(entity);

        // 3. Entity → Domain (retornar domínio)
        return mapper.toDomain(saved);
    }

    /**
     * Busca um Orcamento por ID
     */
    @Override
    public Optional<Orcamento> findById(UUID id) {
        log.debug("Buscando orcamento por id: {}", id);

        // 1. Buscar no DynamoDB
        var entity = dynamoDbRepository.findById(id.toString());

        // 2. Entity → Domain
        return entity.map(mapper::toDomain);
    }

    /**
     * Busca um Orcamento por OS ID
     */
    @Override
    public Optional<Orcamento> findByOsId(UUID osId) {
        log.debug("Buscando orcamento por osId: {}", osId);

        // 1. Buscar no DynamoDB
        var entity = dynamoDbRepository.findByOsId(osId.toString());

        // 2. Entity → Domain
        return entity.map(mapper::toDomain);
    }

    /**
     * Busca Orcamentos por Status
     */
    @Override
    public List<Orcamento> findByStatus(StatusOrcamento status) {
        log.debug("Buscando orcamentos por status: {}", status);

        // 1. Buscar no DynamoDB
        var entities = dynamoDbRepository.findByStatus(status.name());

        // 2. Entity → Domain (stream)
        return entities.stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    /**
     * Verifica se existe Orcamento para uma OS
     */
    @Override
    public boolean existsByOsId(UUID osId) {
        log.debug("Verificando existência de orcamento para osId: {}", osId);
        return dynamoDbRepository.existsByOsId(osId.toString());
    }

    /**
     * Deleta um Orcamento
     */
    @Override
    public void deleteById(UUID id) {
        log.debug("Deletando orcamento: {}", id);
        dynamoDbRepository.deleteById(id.toString());
    }
}
