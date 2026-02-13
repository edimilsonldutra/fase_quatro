package br.com.grupo99.catalogo.adapter.repository;

import br.com.grupo99.catalogo.domain.model.Servico;
import br.com.grupo99.catalogo.domain.repository.ServicoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Adapter que implementa ServicoRepository (domínio) usando DynamoDB.
 * 
 * ✅ CLEAN ARCHITECTURE:
 * - Implementa interface de domínio: ServicoRepository
 * - Delega para DynamoDB: DynamoDbServicoRepository
 * - Isolamento de framework em adapter layer
 * 
 * Migrado de MongoDB/DocumentDB para DynamoDB
 */
@Repository
public class ServicoRepositoryAdapter implements ServicoRepository {

    private final DynamoDbServicoRepository dynamoDbRepository;

    public ServicoRepositoryAdapter(DynamoDbServicoRepository dynamoDbRepository) {
        this.dynamoDbRepository = dynamoDbRepository;
    }

    @Override
    public Servico save(Servico servico) {
        return dynamoDbRepository.save(servico);
    }

    @Override
    public Optional<Servico> findById(String id) {
        return dynamoDbRepository.findById(id);
    }

    @Override
    public List<Servico> findByAtivoTrue() {
        return dynamoDbRepository.findByAtivoTrue();
    }

    @Override
    public List<Servico> findByAtivoTrueOrderByNomeAsc() {
        return dynamoDbRepository.findByAtivoTrueOrderByNomeAsc();
    }

    @Override
    public List<Servico> findAll() {
        return dynamoDbRepository.findAll();
    }

    @Override
    public void deleteById(String id) {
        dynamoDbRepository.deleteById(id);
    }

    @Override
    public boolean existsById(String id) {
        return dynamoDbRepository.existsById(id);
    }

    @Override
    public List<Servico> findByCategoria(String categoria) {
        return dynamoDbRepository.findByCategoriasContainingAndAtivoTrue(categoria);
    }
}
