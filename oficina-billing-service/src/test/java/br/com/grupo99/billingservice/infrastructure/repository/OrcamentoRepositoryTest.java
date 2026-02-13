package br.com.grupo99.billingservice.infrastructure.repository;

import br.com.grupo99.billingservice.domain.model.*;
import br.com.grupo99.billingservice.domain.repository.OrcamentoRepository;
import br.com.grupo99.billingservice.infrastructure.persistence.adapter.OrcamentoEntityMapper;
import br.com.grupo99.billingservice.infrastructure.persistence.adapter.OrcamentoRepositoryAdapter;
import br.com.grupo99.billingservice.infrastructure.persistence.repository.DynamoDbOrcamentoRepository;
import br.com.grupo99.billingservice.infrastructure.config.DynamoDbConfig;
import br.com.grupo99.billingservice.testconfig.DynamoDbTestContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(initializers = DynamoDbTestContainer.Initializer.class)
@DisplayName("OrcamentoRepository - Testes de Integração (DynamoDB)")
class OrcamentoRepositoryTest {

    @Autowired
    private OrcamentoRepository repository;

    @Autowired
    private DynamoDbOrcamentoRepository dynamoDbRepository;

    @BeforeEach
    void setUp() {
        // Limpar dados antes de cada teste para garantir isolamento
        dynamoDbRepository.deleteAll();
    }

    @Test
    @DisplayName("Deve salvar orçamento com sucesso")
    void deveSalvarOrcamentoComSucesso() {
        // Arrange
        UUID osId = UUID.randomUUID();
        Orcamento orcamento = Orcamento.criar(osId, "Orçamento de teste");
        orcamento.adicionarItem(new ItemOrcamento(TipoItem.SERVICO, "Serviço 1", 1, new BigDecimal("100.00")));

        // Act
        Orcamento saved = repository.save(orcamento);

        // Assert
        assertNotNull(saved);
        assertNotNull(saved.getId());
        assertEquals(osId, saved.getOsId());
        assertEquals(1, saved.getItens().size());
    }

    @Test
    @DisplayName("Deve buscar orçamento por osId")
    void deveBuscarOrcamentoPorOsId() {
        // Arrange
        UUID osId = UUID.randomUUID();
        Orcamento orcamento = Orcamento.criar(osId, "Teste");
        repository.save(orcamento);

        // Act
        Optional<Orcamento> found = repository.findByOsId(osId);

        // Assert
        assertTrue(found.isPresent());
        assertEquals(osId, found.get().getOsId());
    }

    @Test
    @DisplayName("Deve buscar orçamentos por status")
    void deveBuscarOrcamentosPorStatus() {
        // Arrange
        Orcamento orc1 = Orcamento.criar(UUID.randomUUID(), "Orc 1");
        Orcamento orc2 = Orcamento.criar(UUID.randomUUID(), "Orc 2");
        Orcamento orc3 = Orcamento.criar(UUID.randomUUID(), "Orc 3");

        orc2.aprovar();
        orc3.aprovar();

        repository.save(orc1);
        repository.save(orc2);
        repository.save(orc3);

        // Act
        List<Orcamento> pendentes = repository.findByStatus(StatusOrcamento.PENDENTE);
        List<Orcamento> aprovados = repository.findByStatus(StatusOrcamento.APROVADO);

        // Assert
        assertEquals(1, pendentes.size());
        assertEquals(2, aprovados.size());
    }

    @Test
    @DisplayName("Deve verificar existência por osId")
    void deveVerificarExistenciaPorOsId() {
        // Arrange
        UUID osId = UUID.randomUUID();
        Orcamento orcamento = Orcamento.criar(osId, "Teste");
        repository.save(orcamento);

        // Act
        boolean existe = repository.existsByOsId(osId);
        boolean naoExiste = repository.existsByOsId(UUID.randomUUID());

        // Assert
        assertTrue(existe);
        assertFalse(naoExiste);
    }

    @Test
    @DisplayName("Deve atualizar orçamento mantendo itens e histórico")
    void deveAtualizarOrcamentoMantendoItensEHistorico() {
        // Arrange
        Orcamento orcamento = Orcamento.criar(UUID.randomUUID(), "Teste");
        orcamento.adicionarItem(new ItemOrcamento(TipoItem.PECA, "Peça 1", 2, new BigDecimal("50.00")));
        Orcamento saved = repository.save(orcamento);

        // Act
        saved.aprovar();
        Orcamento updated = repository.save(saved);

        // Assert
        assertEquals(StatusOrcamento.APROVADO, updated.getStatus());
        assertEquals(1, updated.getItens().size());
        assertTrue(updated.getHistorico().size() >= 1, "Deve ter pelo menos 1 registro no histórico");
    }
}
