package br.com.grupo99.billingservice.infrastructure.messaging;

import br.com.grupo99.billingservice.domain.events.OrcamentoAprovadoEvent;
import br.com.grupo99.billingservice.domain.events.OrcamentoProntoEvent;
import br.com.grupo99.billingservice.domain.events.OrcamentoRejeitadoEvent;
import br.com.grupo99.billingservice.domain.events.PagamentoFalhouEvent;
import br.com.grupo99.billingservice.infrastructure.config.KafkaConfig;
import br.com.grupo99.billingservice.testconfig.DynamoDbTestContainer;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Testes de integração para o KafkaBillingEventPublisher
 * Utiliza Embedded Kafka para testes isolados
 */
@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(initializers = DynamoDbTestContainer.Initializer.class)
@EmbeddedKafka(partitions = 1, brokerProperties = { "listeners=PLAINTEXT://localhost:9092", "port=9092" }, topics = {
                KafkaConfig.TOPIC_BILLING_EVENTS, KafkaConfig.TOPIC_OS_EVENTS, KafkaConfig.TOPIC_EXECUTION_EVENTS })
@TestPropertySource(properties = {
                "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
                "spring.kafka.consumer.auto-offset-reset=earliest"
})
class KafkaBillingEventPublisherTest {

        @Autowired
        private KafkaBillingEventPublisher kafkaPublisher;

        @Autowired
        private EmbeddedKafkaBroker embeddedKafkaBroker;

        private Consumer<String, Map<String, Object>> consumer;

        @BeforeEach
        void setUp() {
                Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
                                "test-group-" + UUID.randomUUID(), "true", embeddedKafkaBroker);
                consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
                consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
                consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
                consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
                consumerProps.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
                consumerProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, Map.class);

                DefaultKafkaConsumerFactory<String, Map<String, Object>> cf = new DefaultKafkaConsumerFactory<>(
                                consumerProps);
                consumer = cf.createConsumer();
                consumer.subscribe(Collections.singletonList(KafkaConfig.TOPIC_BILLING_EVENTS));

                // Poll inicial para garantir que o consumer está pronto
                consumer.poll(Duration.ofSeconds(1));
        }

        @AfterEach
        void tearDown() {
                if (consumer != null) {
                        consumer.close();
                }
        }

        @Test
        @DisplayName("Deve publicar evento ORCAMENTO_PRONTO no Kafka")
        void devePublicarOrcamentoPronto() {
                // Arrange
                UUID osId = UUID.randomUUID();
                UUID orcamentoId = UUID.randomUUID();

                OrcamentoProntoEvent event = OrcamentoProntoEvent.builder()
                                .osId(osId)
                                .orcamentoId(orcamentoId)
                                .valorTotal(new BigDecimal("1500.00"))
                                .prazoValidadeDias(7)
                                .timestamp(LocalDateTime.now())
                                .build();

                // Act
                kafkaPublisher.publishOrcamentoPronto(event);

                // Assert
                await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
                        ConsumerRecords<String, Map<String, Object>> records = KafkaTestUtils.getRecords(consumer,
                                        Duration.ofSeconds(5));

                        assertThat(records.count()).isGreaterThan(0);

                        var record = records.iterator().next();
                        assertThat(record.key()).isEqualTo(osId.toString());

                        // Verifica header
                        String eventType = new String(record.headers().lastHeader("eventType").value());
                        assertThat(eventType).isEqualTo("ORCAMENTO_PRONTO");
                });
        }

        @Test
        @DisplayName("Deve publicar evento ORCAMENTO_APROVADO no Kafka")
        void devePublicarOrcamentoAprovado() {
                // Arrange
                UUID osId = UUID.randomUUID();
                UUID orcamentoId = UUID.randomUUID();

                OrcamentoAprovadoEvent event = OrcamentoAprovadoEvent.builder()
                                .osId(osId)
                                .orcamentoId(orcamentoId)
                                .valorTotal(new BigDecimal("2000.00"))
                                .aprovadoPor("cliente@example.com")
                                .timestamp(LocalDateTime.now())
                                .build();

                // Act
                kafkaPublisher.publishOrcamentoAprovado(event);

                // Assert
                await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
                        ConsumerRecords<String, Map<String, Object>> records = KafkaTestUtils.getRecords(consumer,
                                        Duration.ofSeconds(5));

                        assertThat(records.count()).isGreaterThan(0);

                        var record = records.iterator().next();
                        assertThat(record.key()).isEqualTo(osId.toString());

                        String eventType = new String(record.headers().lastHeader("eventType").value());
                        assertThat(eventType).isEqualTo("ORCAMENTO_APROVADO");
                });
        }

        @Test
        @DisplayName("Deve publicar evento ORCAMENTO_REJEITADO no Kafka")
        void devePublicarOrcamentoRejeitado() throws InterruptedException {
                // Arrange
                UUID osId = UUID.randomUUID();
                UUID orcamentoId = UUID.randomUUID();

                OrcamentoRejeitadoEvent event = OrcamentoRejeitadoEvent.builder()
                                .osId(osId)
                                .orcamentoId(orcamentoId)
                                .motivo("Valor acima do esperado")
                                .rejeitadoPor("cliente@example.com")
                                .timestamp(LocalDateTime.now())
                                .build();

                // Act
                kafkaPublisher.publishOrcamentoRejeitado(event);

                // Aguardar um pouco para garantir que a mensagem foi processada
                Thread.sleep(500);

                // Assert
                ConsumerRecords<String, Map<String, Object>> records = KafkaTestUtils.getRecords(consumer,
                                Duration.ofSeconds(10));

                assertThat(records.count()).isGreaterThan(0);

                var record = records.iterator().next();
                assertThat(record.key()).isEqualTo(osId.toString());

                String eventType = new String(record.headers().lastHeader("eventType").value());
                assertThat(eventType).isEqualTo("ORCAMENTO_REJEITADO");
        }

        @Test
        @DisplayName("Deve publicar evento PAGAMENTO_FALHOU no Kafka")
        void devePublicarPagamentoFalhou() throws InterruptedException {
                // Arrange
                UUID osId = UUID.randomUUID();
                UUID pagamentoId = UUID.randomUUID();

                PagamentoFalhouEvent event = PagamentoFalhouEvent.builder()
                                .osId(osId)
                                .pagamentoId(pagamentoId)
                                .motivo("Cartão recusado")
                                .codigoErro("CARD_DECLINED")
                                .valorTentado(new BigDecimal("1500.00"))
                                .timestamp(LocalDateTime.now())
                                .build();

                // Act
                kafkaPublisher.publishPagamentoFalhou(event);

                // Aguardar um pouco para garantir que a mensagem foi processada
                Thread.sleep(500);

                // Assert
                ConsumerRecords<String, Map<String, Object>> records = KafkaTestUtils.getRecords(consumer,
                                Duration.ofSeconds(10));

                assertThat(records.count()).isGreaterThan(0);

                var record = records.iterator().next();
                assertThat(record.key()).isEqualTo(osId.toString());

                String eventType = new String(record.headers().lastHeader("eventType").value());
                assertThat(eventType).isEqualTo("PAGAMENTO_FALHOU");
        }
}
