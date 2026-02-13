package br.com.grupo99.catalogo.application.service;

import br.com.grupo99.catalogo.application.dto.PecaRequestDTO;
import br.com.grupo99.catalogo.application.dto.PecaResponseDTO;
import br.com.grupo99.catalogo.domain.model.Peca;
import br.com.grupo99.catalogo.domain.repository.PecaRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Application Service para Peça.
 * Migrado para DynamoDB - ID é String (UUID).
 */
@Service
public class PecaApplicationService {
    private final PecaRepository pecaRepository;

    public PecaApplicationService(PecaRepository pecaRepository) {
        this.pecaRepository = pecaRepository;
    }

    public PecaResponseDTO criarPeca(PecaRequestDTO request) {
        Peca peca = new Peca(
                request.getNome(),
                request.getDescricao(),
                request.getCodigoFabricante(),
                request.getPreco(),
                request.getQuantidade());
        if (request.getQuantidadeMinima() != null) {
            peca.setQuantidadeMinima(request.getQuantidadeMinima());
        }
        peca = pecaRepository.save(peca);
        return PecaResponseDTO.fromDomain(peca);
    }

    public PecaResponseDTO buscarPorId(String id) {
        Peca peca = pecaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Peça não encontrada com ID: " + id));
        return PecaResponseDTO.fromDomain(peca);
    }

    public List<PecaResponseDTO> listarTodas() {
        return pecaRepository.findAll()
                .stream()
                .map(PecaResponseDTO::fromDomain)
                .collect(Collectors.toList());
    }

    public List<PecaResponseDTO> listarAtivas() {
        return pecaRepository.findByAtivoTrueOrderByNomeAsc()
                .stream()
                .map(PecaResponseDTO::fromDomain)
                .collect(Collectors.toList());
    }

    public PecaResponseDTO atualizar(String id, PecaRequestDTO request) {
        Peca peca = pecaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Peça não encontrada com ID: " + id));

        peca.setNome(request.getNome());
        peca.setDescricao(request.getDescricao());
        peca.setCodigoFabricante(request.getCodigoFabricante());
        peca.setPreco(request.getPreco());
        peca.setQuantidade(request.getQuantidade());
        if (request.getQuantidadeMinima() != null) {
            peca.setQuantidadeMinima(request.getQuantidadeMinima());
        }

        peca = pecaRepository.save(peca);
        return PecaResponseDTO.fromDomain(peca);
    }

    public void desativar(String id) {
        Peca peca = pecaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Peça não encontrada com ID: " + id));
        peca.setAtivo(false);
        pecaRepository.save(peca);
    }

    public void deletar(String id) {
        if (!pecaRepository.existsById(id)) {
            throw new RuntimeException("Peça não encontrada com ID: " + id);
        }
        pecaRepository.deleteById(id);
    }

    public void decrementarEstoque(String id, Integer quantidade) {
        Peca peca = pecaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Peça não encontrada com ID: " + id));
        peca.decrementarQuantidade(quantidade);
        pecaRepository.save(peca);
    }

    public void incrementarEstoque(String id, Integer quantidade) {
        Peca peca = pecaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Peça não encontrada com ID: " + id));
        peca.incrementarQuantidade(quantidade);
        pecaRepository.save(peca);
    }

    public List<PecaResponseDTO> buscarPorCategoria(String categoria) {
        return pecaRepository.findByCategoria(categoria)
                .stream()
                .map(PecaResponseDTO::fromDomain)
                .collect(Collectors.toList());
    }

    public List<PecaResponseDTO> buscarPorMarca(String marca) {
        return pecaRepository.findByMarca(marca)
                .stream()
                .map(PecaResponseDTO::fromDomain)
                .collect(Collectors.toList());
    }
}
