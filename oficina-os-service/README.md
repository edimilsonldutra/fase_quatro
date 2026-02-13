# 🚗 OS Service - Ordem de Serviço

[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3.13-6DB33F?logo=springboot)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk)](https://openjdk.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql)](https://www.postgresql.org/)
[![Apache Kafka](https://img.shields.io/badge/Apache_Kafka-3.7.2-231F20?logo=apachekafka)](https://kafka.apache.org/)

Microsserviço responsável por gerenciar o ciclo de vida das ordens de serviço em uma oficina mecânica.

## 📋 Índice

- [Visão Geral](#visão-geral)
- [Responsabilidades](#responsabilidades)
- [Arquitetura](#arquitetura)
- [Tecnologias](#tecnologias)
- [APIs REST](#apis-rest)
- [Eventos (Kafka)](#eventos-kafka)
- [Banco de Dados](#banco-de-dados)
- [Configuração](#configuração)
- [Deploy](#deploy)
- [Testes](#testes)
- [Monitoramento](#monitoramento)

---

## 🎯 Visão Geral

O **OS Service** é o microsserviço central que gerencia todas as ordens de serviço (OS) da oficina. Ele controla o ciclo de vida completo de uma OS, desde sua criação até a entrega final ao cliente.

### Bounded Context

Este serviço representa o **bounded context "Gestão de Ordens de Serviço"** no modelo Domain-Driven Design (DDD).

---

## 🔷 Responsabilidades

- ✅ **Criar ordem de serviço** - Registrar nova OS para um cliente e veículo
- ✅ **Atualizar status** - Gerenciar transições de estado da OS
- ✅ **Consultar status e histórico** - Fornecer informações sobre OS específica ou listagem
- ✅ **Rastreamento de estados** - Manter histórico de todas as mudanças de status
- ✅ **Publicar eventos** - Notificar outros serviços sobre mudanças no ciclo de vida da OS
- ✅ **Consumir eventos** - Reagir a eventos de outros serviços (ex: execução finalizada)

---

## 🏗️ Arquitetura

### Clean Architecture (Hexagonal)

```
┌─────────────────────────────────────────┐
│        Infrastructure Layer             │
│  (REST Controllers, Kafka Listeners,     │
│   JPA Repositories, Configs)            │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│         Adapter Layer                   │
│  (Controllers, Presenters, Gateways)    │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│      Application Layer                  │
│  (Use Cases, DTOs, Services)            │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│         Domain Layer                    │
│  (Entities, Value Objects,              │
│   Domain Services, Repositories)        │
└─────────────────────────────────────────┘
```

### Estrutura de Pastas

```
oficina-os-service/
├── src/
│   ├── main/
│   │   ├── java/br/com/grupo99/osservice/
│   │   │   ├── domain/                    # Camada de Domínio
│   │   │   │   ├── model/
│   │   │   │   │   ├── OrdemServico.java
│   │   │   │   │   ├── StatusOS.java
│   │   │   │   │   └── HistoricoStatus.java
│   │   │   │   ├── repository/
│   │   │   │   │   └── OrdemServicoRepository.java
│   │   │   │   └── service/
│   │   │   │       └── OrdemServicoDomainService.java
│   │   │   ├── application/               # Camada de Aplicação
│   │   │   │   ├── usecase/
│   │   │   │   │   ├── CriarOrdemServicoUseCase.java
│   │   │   │   │   ├── AtualizarStatusUseCase.java
│   │   │   │   │   └── ConsultarOrdemServicoUseCase.java
│   │   │   │   ├── dto/
│   │   │   │   │   ├── OrdemServicoRequestDTO.java
│   │   │   │   │   └── OrdemServicoResponseDTO.java
│   │   │   │   └── service/
│   │   │   │       └── OrdemServicoApplicationService.java
│   │   │   ├── adapter/                   # Camada de Adapter
│   │   │   │   ├── controller/
│   │   │   │   │   └── OrdemServicoController.java
│   │   │   │   └── gateway/
│   │   │   │       ├── EventPublisherGateway.java
│   │   │   │       └── impl/
│   │   │   │           └── KafkaEventPublisherGateway.java
│   │   │   └── infrastructure/            # Camada de Infraestrutura
│   │   │       ├── rest/
│   │   │       │   └── OrdemServicoRestController.java
│   │   │       ├── messaging/
│   │   │       │   ├── publisher/
│   │   │       │   │   └── OsEventPublisher.java
│   │   │       │   └── consumer/
│   │   │       │       ├── ExecucaoEventConsumer.java
│   │   │       │       └── BillingEventConsumer.java
│   │   │       ├── persistence/
│   │   │       │   ├── jpa/
│   │   │       │   │   └── OrdemServicoJpaRepository.java
│   │   │       │   └── repository/
│   │   │       │       └── OrdemServicoRepositoryImpl.java
│   │   │       ├── config/
│   │   │       │   ├── KafkaConfig.java
│   │   │       │   └── DatabaseConfig.java
│   │   │       └── exception/
│   │   │           └── GlobalExceptionHandler.java
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-prod.yml
│   │       └── db/migration/
│   │           ├── V1__create_ordem_servico_table.sql
│   │           └── V2__create_historico_status_table.sql
│   └── test/
│       ├── java/br/com/grupo99/osservice/
│       │   ├── domain/
│       │   ├── application/
│       │   ├── adapter/
│       │   └── infrastructure/
│       │       ├── rest/
│       │       │   └── OrdemServicoControllerIT.java
│       │       └── bdd/
│       │           ├── OrdemServicoSteps.java
│       │           └── fluxo-completo-os.feature
│       └── resources/
│           ├── application-test.yml
│           └── features/
├── k8s/
│   ├── deployment.yaml
│   ├── service.yaml
│   ├── hpa.yaml
│   ├── configmap.yaml
│   └── secret.yaml
├── terraform/
│   ├── rds.tf
│   ├── kafka.tf
│   └── variables.tf
├── .github/
│   └── workflows/
│       └── ci-cd.yml
├── Dockerfile
├── docker-compose.yml
├── pom.xml
└── README.md
```

---

## 🛠️ Tecnologias

| Categoria | Tecnologia | Versão | Justificativa |
|-----------|------------|--------|---------------|
| **Framework** | Spring Boot | 3.3.13 | Framework moderno e produtivo |
| **Linguagem** | Java | 21 | LTS com virtual threads |
| **Banco de Dados** | PostgreSQL | 16 | ACID, relacionamentos fortes |
| **Mensageria** | Apache Kafka | 3.7.2 | Comunicação assíncrona (Event-Driven) |
| **ORM** | Spring Data JPA | - | Simplifica acesso a dados |
| **Migração DB** | Flyway | - | Versionamento de schema |
| **Observabilidade** | New Relic APM | - | Monitoramento e tracing |
| **Testes** | JUnit 5, Cucumber | - | Testes unitários e BDD |
| **Build** | Maven | 3.9+ | Gerenciamento de dependências |
| **Container** | Docker | - | Empacotamento da aplicação |
| **Orquestração** | Kubernetes (EKS) | 1.29 | Deploy e escalabilidade |

---

## 🔌 APIs REST

### Base URL
```
Development: http://localhost:8081/api/v1
Production:  https://api.oficina.com/os-service/api/v1
```

### Endpoints

#### 1. Criar Ordem de Serviço

```http
POST /api/v1/ordens-servico
Content-Type: application/json
Authorization: Bearer <JWT>
```

**Request Body:**
```json
{
  "clienteId": "uuid",
  "veiculoId": "uuid",
  "descricaoProblema": "Barulho no motor ao acelerar"
}
```

**Response:** `201 Created`
```json
{
  "id": "uuid",
  "clienteId": "uuid",
  "veiculoId": "uuid",
  "status": "RECEBIDA",
  "descricaoProblema": "Barulho no motor ao acelerar",
  "valorTotal": 0.00,
  "dataCriacao": "2026-01-31T10:00:00Z",
  "dataFinalizacao": null,
  "dataEntrega": null
}
```

---

#### 2. Buscar Ordem de Serviço por ID

```http
GET /api/v1/ordens-servico/{id}
Authorization: Bearer <JWT>
```

**Response:** `200 OK`
```json
{
  "id": "uuid",
  "clienteId": "uuid",
  "veiculoId": "uuid",
  "status": "EM_EXECUCAO",
  "descricaoProblema": "Barulho no motor",
  "valorTotal": 350.00,
  "dataCriacao": "2026-01-31T10:00:00Z",
  "dataFinalizacao": null,
  "dataEntrega": null,
  "historico": [
    {
      "statusAnterior": "RECEBIDA",
      "novoStatus": "AGUARDANDO_APROVACAO",
      "dataAlteracao": "2026-01-31T10:30:00Z"
    },
    {
      "statusAnterior": "AGUARDANDO_APROVACAO",
      "novoStatus": "EM_EXECUCAO",
      "dataAlteracao": "2026-01-31T11:00:00Z"
    }
  ]
}
```

---

#### 3. Listar Ordens de Serviço

```http
GET /api/v1/ordens-servico?status=EM_EXECUCAO&page=0&size=20
Authorization: Bearer <JWT>
```

**Query Parameters:**
- `status` (opcional): Filtrar por status (RECEBIDA, EM_DIAGNOSTICO, etc.)
- `clienteId` (opcional): Filtrar por cliente
- `page` (default: 0): Número da página
- `size` (default: 20): Tamanho da página

**Response:** `200 OK`
```json
{
  "content": [
    {
      "id": "uuid",
      "clienteId": "uuid",
      "veiculoId": "uuid",
      "status": "EM_EXECUCAO",
      "valorTotal": 350.00,
      "dataCriacao": "2026-01-31T10:00:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 5,
  "totalPages": 1
}
```

---

#### 4. Atualizar Status da OS

```http
PATCH /api/v1/ordens-servico/{id}/status
Content-Type: application/json
Authorization: Bearer <JWT>
```

**Request Body:**
```json
{
  "novoStatus": "EM_DIAGNOSTICO",
  "observacao": "Iniciando diagnóstico do veículo"
}
```

**Response:** `200 OK`
```json
{
  "id": "uuid",
  "status": "EM_DIAGNOSTICO",
  "dataAtualizacao": "2026-01-31T11:00:00Z"
}
```

---

#### 5. Cancelar Ordem de Serviço

```http
DELETE /api/v1/ordens-servico/{id}
Authorization: Bearer <JWT>
```

**Response:** `204 No Content`

---

## 📨 Eventos (Kafka)

### Eventos Publicados

#### 1. **OsAbertaEvent**

Publicado quando uma nova OS é criada.

**Tópico:** `os-events`

**Payload:**
```json
{
  "eventId": "uuid",
  "eventType": "OsAbertaEvent",
  "timestamp": "2026-01-31T10:00:00Z",
  "aggregateId": "os-uuid",
  "version": 1,
  "payload": {
    "osId": "uuid",
    "clienteId": "uuid",
    "veiculoId": "uuid",
    "status": "RECEBIDA",
    "descricaoProblema": "Barulho no motor"
  }
}
```

**Consumidores:**
- Billing Service (para gerar orçamento)

---

#### 2. **OsAtualizadaEvent**

Publicado quando o status da OS muda.

**Tópico:** `os-events`

**Payload:**
```json
{
  "eventId": "uuid",
  "eventType": "OsAtualizadaEvent",
  "timestamp": "2026-01-31T11:00:00Z",
  "aggregateId": "os-uuid",
  "version": 2,
  "payload": {
    "osId": "uuid",
    "statusAnterior": "RECEBIDA",
    "novoStatus": "EM_DIAGNOSTICO"
  }
}
```

---

#### 3. **OsFinalizadaEvent**

Publicado quando a OS é finalizada.

**Tópico:** `os-events`

**Payload:**
```json
{
  "eventId": "uuid",
  "eventType": "OsFinalizadaEvent",
  "timestamp": "2026-01-31T15:00:00Z",
  "aggregateId": "os-uuid",
  "version": 5,
  "payload": {
    "osId": "uuid",
    "dataFinalizacao": "2026-01-31T15:00:00Z",
    "valorTotal": 450.00
  }
}
```

---

#### 4. **OsCanceladaEvent**

Publicado quando a OS é cancelada.

**Tópico:** `os-events`

**Payload:**
```json
{
  "eventId": "uuid",
  "eventType": "OsCanceladaEvent",
  "timestamp": "2026-01-31T12:00:00Z",
  "aggregateId": "os-uuid",
  "version": 3,
  "payload": {
    "osId": "uuid",
    "motivoCancelamento": "Cliente desistiu do serviço"
  }
}
```

---

### Eventos Consumidos

#### 1. **ExecucaoIniciadaEvent** (de Execution Service)

Atualiza status da OS para `EM_EXECUCAO`.

**Tópico consumido:** `execution-events`

---

#### 2. **ExecucaoFinalizadaEvent** (de Execution Service)

Atualiza status da OS para `FINALIZADA`.

**Tópico consumido:** `execution-events`

---

#### 3. **PagamentoConfirmadoEvent** (de Billing Service)

Atualiza status da OS para indicar que o pagamento foi realizado.

**Tópico consumido:** `billing-events`

---

## 💾 Banco de Dados

### PostgreSQL (AWS RDS)

**Justificativa:**
- ✅ **Transações ACID:** Garantia de consistência nas mudanças de status
- ✅ **Relacionamentos fortes:** FK com Cliente e Veículo
- ✅ **Auditoria:** Histórico completo de mudanças de status
- ✅ **Consultas complexas:** JOIN entre OS, Cliente e Veículo

### Schema

#### Tabela: `ordem_servico`

```sql
CREATE TABLE ordem_servico (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cliente_id UUID NOT NULL,
    veiculo_id UUID NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'RECEBIDA',
    descricao_problema TEXT,
    valor_total DECIMAL(10,2) NOT NULL DEFAULT 0,
    data_criacao TIMESTAMP NOT NULL DEFAULT NOW(),
    data_finalizacao TIMESTAMP,
    data_entrega TIMESTAMP,
    version INTEGER NOT NULL DEFAULT 0,  -- Optimistic Locking
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP
);

CREATE INDEX idx_os_status ON ordem_servico(status);
CREATE INDEX idx_os_cliente ON ordem_servico(cliente_id);
CREATE INDEX idx_os_data_criacao ON ordem_servico(data_criacao DESC);
```

#### Tabela: `historico_status`

```sql
CREATE TABLE historico_status (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ordem_servico_id UUID NOT NULL REFERENCES ordem_servico(id) ON DELETE CASCADE,
    status_anterior VARCHAR(30),
    novo_status VARCHAR(30) NOT NULL,
    observacao TEXT,
    usuario_alteracao VARCHAR(100),
    data_alteracao TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_historico_os ON historico_status(ordem_servico_id);
```

---

## ⚙️ Configuração

### Variáveis de Ambiente

```yaml
# Database
DB_HOST: <rds-endpoint>
DB_PORT: 5432
DB_NAME: osservice_db
DB_USERNAME: <from-secrets-manager>
DB_PASSWORD: <from-secrets-manager>

# Apache Kafka
KAFKA_BOOTSTRAP_SERVERS: kafka:9092
KAFKA_TOPIC_OS_EVENTS: os-events
KAFKA_TOPIC_EXECUTION_EVENTS: execution-events
KAFKA_TOPIC_BILLING_EVENTS: billing-events

# Spring Profiles
SPRING_PROFILES_ACTIVE: prod

# Logging
LOG_LEVEL: INFO

# New Relic
NEW_RELIC_LICENSE_KEY: <from-secrets-manager>
NEW_RELIC_APP_NAME: os-service

# JVM
JAVA_OPTS: -Xms512m -Xmx1024m -XX:+UseG1GC
```

### application.yml

```yaml
spring:
  application:
    name: os-service
  datasource:
    url: jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
  flyway:
    enabled: true
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
    consumer:
      group-id: os-service
      auto-offset-reset: earliest

server:
  port: 8081
```

---

## 🚀 Deploy

### Local (Docker Compose)

```bash
docker-compose up -d
```

### Kubernetes (EKS)

```bash
# Aplicar manifests
kubectl apply -f k8s/

# Verificar deploy
kubectl get pods -n os-service
kubectl logs -f deployment/os-service -n os-service
```

---

## 🧪 Testes

### Executar Testes

```bash
# Todos os testes
mvn clean test

# Apenas testes unitários
mvn test -Dtest=*Test

# Apenas testes de integração
mvn test -Dtest=*IT

# Testes BDD
mvn test -Dtest=*BDD
```

### Cobertura

```bash
mvn clean verify jacoco:report

# Relatório em: target/site/jacoco/index.html
```

**Meta:** 80%+ de cobertura

---

## 📊 Monitoramento

### New Relic APM

- Latência de APIs
- Taxa de erro
- Throughput
- Distributed tracing

### Métricas Customizadas

- Total de OS criadas/hora
- Tempo médio por status
- Taxa de cancelamento

### Logs

**Formato JSON estruturado:**
```json
{
  "timestamp": "2026-01-31T10:00:00Z",
  "level": "INFO",
  "service": "os-service",
  "traceId": "abc123",
  "message": "OS criada",
  "osId": "uuid",
  "clienteId": "uuid"
}
```

---

## 🔐 Segurança

- **Autenticação:** JWT via API Gateway
- **Autorização:** RBAC (CLIENTE, MECANICO, ADMIN)
- **Secrets:** AWS Secrets Manager
- **Network:** Security Groups, VPC privada

---

## 📚 Documentação

- **Swagger UI:** http://localhost:8081/swagger-ui.html
- **OpenAPI Spec:** http://localhost:8081/v3/api-docs

---

## 🤝 Contribuição

1. Fork o repositório
2. Crie uma branch (`git checkout -b feature/nova-funcionalidade`)
3. Commit suas mudanças (`git commit -am 'Adiciona nova funcionalidade'`)
4. Push para a branch (`git push origin feature/nova-funcionalidade`)
5. Abra um Pull Request

---

## 📄 Licença

Este projeto está sob a licença MIT.

---

**Última Atualização:** 31/01/2026  
**Versão:** 1.0.0
