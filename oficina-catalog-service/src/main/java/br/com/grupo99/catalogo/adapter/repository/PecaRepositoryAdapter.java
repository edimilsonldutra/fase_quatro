package br.com.grupo99.catalogo.adapter.repository;

import br.com.grupo99.catalogo.domain.model.Peca;
import br.com.grupo99.catalogo.domain.repository.PecaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Adapter que implementa PecaRepository (domínio) usando DynamoDB.
 * 
 * ✅ CLEAN ARCHITECTURE:
 * - Implementa interface de domínio: PecaRepository
 * - Delega para DynamoDB: DynamoDbPecaRepository
 * - Isolamento de framework em adapter layer
 * 
 * Migrado de MongoDB/DocumentDB para DynamoDB
 */
@Repository
public class PecaRepositoryAdapter implements PecaRepository {

    private final DynamoDbPecaRepository dynamoDbRepository;

    public PecaRepositoryAdapter(DynamoDbPecaRepository dynamoDbRepository) {
        this.dynamoDbRepository = dynamoDbRepository;
    }

    @Override
    public Peca save(Peca peca) {
        return dynamoDbRepository.save(peca);
    }

    @Override
    public Optional<Peca> findById(String id) {
        return dynamoDbRepository.findById(id);
    }

    @Override
    public List<Peca> findByAtivoTrue() {
        return dynamoDbRepository.findByAtivoTrue();
    }

    @Override
    public List<Peca> findByAtivoTrueOrderByNomeAsc() {
        return dynamoDbRepository.findByAtivoTrueOrderByNomeAsc();
    }

    @Override
    public Optional<Peca> findByCodigoFabricante(String codigoFabricante) {
        return dynamoDbRepository.findByCodigoFabricante(codigoFabricante);
    }

    @Override
    public List<Peca> findAll() {
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
    public List<Peca> findByCategoria(String categoria) {
        return dynamoDbRepository.findByCategoriasContainingAndAtivoTrue(categoria);
    }

    @Override
    public List<Peca> findByMarca(String marca) {
        return dynamoDbRepository.findByMarcaAndAtivoTrue(marca);
    }
}
