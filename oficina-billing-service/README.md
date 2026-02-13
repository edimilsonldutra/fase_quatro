# 💰 Billing Service - Orçamento e Pagamento

[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3.13-6DB33F?logo=springboot)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk)](https://openjdk.org/)
[![DynamoDB](https://img.shields.io/badge/DynamoDB-4053D6?logo=amazondynamodb&logoColor=white)](https://aws.amazon.com/dynamodb/)
[![Apache Kafka](https://img.shields.io/badge/Kafka-3.7.2-231F20?logo=apachekafka)](https://kafka.apache.org/)

Microsserviço responsável por gerenciar orçamentos e pagamentos em uma oficina mecânica.

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

O **Billing Service** é o microsserviço financeiro responsável por gerenciar todo o ciclo de orçamentos e pagamentos relacionados às ordens de serviço da oficina.

### Bounded Context

Este serviço representa o **bounded context "Faturamento e Pagamentos"** no modelo Domain-Driven Design (DDD).

---

## 🔷 Responsabilidades

- ✅ **Gerar orçamento** - Criar orçamento automaticamente quando uma OS é aberta
- ✅ **Aprovar/Rejeitar orçamento** - Registrar decisão do cliente
- ✅ **Processar pagamento** - Registrar confirmação de pagamento
- ✅ **Enviar notificações** - Notificar cliente sobre orçamento
- ✅ **Histórico financeiro** - Manter registro completo de aprovações e pagamentos
- ✅ **Publicar eventos** - Notificar outros serviços sobre mudanças financeiras

---

## 🏗️ Arquitetura

### Clean Architecture (Hexagonal)

```
┌─────────────────────────────────────────┐
│        Infrastructure Layer             │
│  (REST Controllers, Kafka Listeners,      │
│   DynamoDB Repositories, Configs)        │
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

---

## 🛠️ Tecnologias

| Categoria | Tecnologia | Versão | Justificativa |
|-----------|------------|--------|---------------|
| **Framework** | Spring Boot | 3.3.13 | Framework moderno e produtivo |
| **Linguagem** | Java | 21 | LTS com virtual threads |
| **Banco de Dados** | DynamoDB | - | Flexibilidade para documentos variáveis, Free Tier AWS |
| **Mensageria** | Apache Kafka | 3.7.2 | Comunicação assíncrona event-driven |
| **SDK** | AWS SDK DynamoDB Enhanced | 2.x | Simplifica acesso ao DynamoDB |
| **Observabilidade** | New Relic APM | - | Monitoramento e tracing |
| **Testes** | JUnit 5, Cucumber | - | Testes unitários e BDD |
| **Build** | Maven | 3.9+ | Gerenciamento de dependências |
| **Container** | Docker | - | Empacotamento da aplicação |
| **Orquestração** | Kubernetes (EKS) | 1.29 | Deploy e escalabilidade |

---

## 🔌 APIs REST

### Base URL
```
Development: http://localhost:8082/api/v1
Production:  https://api.oficina.com/billing-service/api/v1
```

### Endpoints

#### 1. Criar Orçamento

```http
POST /api/v1/orcamentos
Content-Type: application/json
Authorization: Bearer <JWT>
```

**Request Body:**
```json
{
  "osId": "uuid",
  "itens": [
    {
      "tipo": "SERVICO",
      "descricao": "Troca de óleo",
      "quantidade": 1,
      "valorUnitario": 150.00
    },
    {
      "tipo": "PECA",
      "descricao": "Óleo 5W30",
      "quantidade": 4,
      "valorUnitario": 45.00
    }
  ]
}
```

**Response:** `201 Created`
```json
{
  "id": "uuid",
  "osId": "uuid",
  "status": "PENDENTE",
  "itens": [...],
  "valorTotal": 330.00,
  "dataGeracao": "2026-01-31T10:00:00Z",
  "dataAprovacao": null
}
```

---

#### 2. Buscar Orçamento por ID

```http
GET /api/v1/orcamentos/{id}
Authorization: Bearer <JWT>
```

**Response:** `200 OK`

---

#### 3. Buscar Orçamento por OS

```http
GET /api/v1/orcamentos/os/{osId}
Authorization: Bearer <JWT>
```

**Response:** `200 OK`

---

#### 4. Aprovar Orçamento

```http
PATCH /api/v1/orcamentos/{id}/aprovar
Content-Type: application/json
Authorization: Bearer <JWT>
```

**Request Body:**
```json
{
  "observacao": "Cliente aprovou orçamento"
}
```

**Response:** `200 OK`

---

#### 5. Rejeitar Orçamento

```http
PATCH /api/v1/orcamentos/{id}/rejeitar
Content-Type: application/json
Authorization: Bearer <JWT>
```

**Request Body:**
```json
{
  "motivo": "Valor acima do esperado"
}
```

**Response:** `200 OK`

---

#### 6. Registrar Pagamento

```http
POST /api/v1/pagamentos
Content-Type: application/json
Authorization: Bearer <JWT>
```

**Request Body:**
```json
{
  "orcamentoId": "uuid",
  "formaPagamento": "CARTAO_CREDITO",
  "valor": 330.00,
  "comprovante": "txn_123456"
}
```

**Response:** `201 Created`

---

## 📨 Eventos (Kafka)

### Eventos Publicados

#### 1. **OrcamentoGeradoEvent**

Publicado quando um orçamento é criado.

**Tópico:** `billing-events`

**Payload:**
```json
{
  "eventId": "uuid",
  "eventType": "OrcamentoGeradoEvent",
  "timestamp": "2026-01-31T10:00:00Z",
  "aggregateId": "orcamento-uuid",
  "version": 1,
  "payload": {
    "orcamentoId": "uuid",
    "osId": "uuid",
    "valorTotal": 330.00
  }
}
```

---

#### 2. **OrcamentoAprovadoEvent**

Publicado quando cliente aprova orçamento.

**Tópico:** `billing-events`

**Payload:**
```json
{
  "eventId": "uuid",
  "eventType": "OrcamentoAprovadoEvent",
  "timestamp": "2026-01-31T11:00:00Z",
  "aggregateId": "orcamento-uuid",
  "version": 2,
  "payload": {
    "orcamentoId": "uuid",
    "osId": "uuid",
    "dataAprovacao": "2026-01-31T11:00:00Z"
  }
}
```

**Consumidores:**
- Execution Service (para iniciar execução)
- OS Service (para atualizar status)

---

#### 3. **OrcamentoRejeitadoEvent**

Publicado quando cliente rejeita orçamento.

**Payload:**
```json
{
  "eventId": "uuid",
  "eventType": "OrcamentoRejeitadoEvent",
  "timestamp": "2026-01-31T12:00:00Z",
  "aggregateId": "orcamento-uuid",
  "version": 2,
  "payload": {
    "orcamentoId": "uuid",
    "osId": "uuid",
    "motivoRejeicao": "Valor acima do esperado"
  }
}
```

---

#### 4. **PagamentoConfirmadoEvent**

Publicado quando pagamento é confirmado.

**Payload:**
```json
{
  "eventId": "uuid",
  "eventType": "PagamentoConfirmadoEvent",
  "timestamp": "2026-01-31T13:00:00Z",
  "aggregateId": "pagamento-uuid",
  "version": 1,
  "payload": {
    "pagamentoId": "uuid",
    "orcamentoId": "uuid",
    "osId": "uuid",
    "valor": 330.00,
    "formaPagamento": "CARTAO_CREDITO"
  }
}
```

**Consumidores:**
- Execution Service (para liberar execução)
- OS Service (para atualizar status)

---

### Eventos Consumidos

#### 1. **OsAbertaEvent** (de OS Service)

Cria orçamento automaticamente quando OS é aberta.

**Tópico consumido:** `os-events`

---

## 💾 Banco de Dados

### DynamoDB (Amazon)

**Justificativa:**
- ✅ **Flexibilidade:** Orçamentos com estruturas variáveis (número de itens, tipos diferentes)
- ✅ **Free Tier:** Custo próximo a zero para desenvolvimento e baixo volume
- ✅ **Gerenciado:** Sem necessidade de administrar cluster ou instâncias
- ✅ **Escalabilidade automática:** On-demand capacity

### Tabelas

#### Tabela: `billing-service-orcamentos`

```json
{
  "_id": "uuid",
  "osId": "uuid",
  "status": "PENDENTE",
  "itens": [
    {
      "tipo": "SERVICO",
      "descricao": "Troca de óleo",
      "quantidade": 1,
      "valorUnitario": 150.00,
      "valorTotal": 150.00
    }
  ],
  "valorTotal": 330.00,
  "dataGeracao": ISODate("2026-01-31T10:00:00Z"),
  "dataAprovacao": null,
  "dataRejeicao": null,
  "observacao": null,
  "version": 1,
  "historico": [
    {
      "statusAnterior": null,
      "novoStatus": "PENDENTE",
      "data": ISODate("2026-01-31T10:00:00Z"),
      "usuario": "system"
    }
  ],
  "createdAt": ISODate("2026-01-31T10:00:00Z"),
  "updatedAt": ISODate("2026-01-31T10:00:00Z")
}
```

**Índices:**
```javascript
db.orcamentos.createIndex({ "osId": 1 })
db.orcamentos.createIndex({ "status": 1 })
db.orcamentos.createIndex({ "dataGeracao": -1 })
```

---

#### Tabela: `billing-service-pagamentos`

```json
{
  "_id": "uuid",
  "orcamentoId": "uuid",
  "osId": "uuid",
  "status": "CONFIRMADO",
  "valor": 330.00,
  "formaPagamento": "CARTAO_CREDITO",
  "comprovante": "txn_123456",
  "dataPagamento": ISODate("2026-01-31T13:00:00Z"),
  "createdAt": ISODate("2026-01-31T13:00:00Z"),
  "updatedAt": ISODate("2026-01-31T13:00:00Z")
}
```

**Índices:**
```javascript
db.pagamentos.createIndex({ "orcamentoId": 1 })
db.pagamentos.createIndex({ "osId": 1 })
db.pagamentos.createIndex({ "status": 1 })
```

---

## ⚙️ Configuração

### Variáveis de Ambiente

```yaml
# DynamoDB
AWS_DYNAMODB_ENDPOINT: https://dynamodb.us-east-1.amazonaws.com
AWS_DYNAMODB_TABLE_PREFIX: billing-service
AWS_REGION: us-east-1

# Kafka
KAFKA_BOOTSTRAP_SERVERS: ${KAFKA_BOOTSTRAP_SERVERS}
OS_EVENTS_TOPIC: os-events
BILLING_EVENTS_TOPIC: billing-events

# Spring Profiles
SPRING_PROFILES_ACTIVE: prod

# Logging
LOG_LEVEL: INFO

# New Relic
NEW_RELIC_LICENSE_KEY: <from-secrets-manager>
NEW_RELIC_APP_NAME: billing-service

# JVM
JAVA_OPTS: -Xms512m -Xmx1024m -XX:+UseG1GC
```

---

## 🚀 Deploy

### Local (Docker Compose)

```bash
docker-compose up -d
```

### Kubernetes (EKS)

```bash
kubectl apply -f k8s/
kubectl get pods -n billing-service
```

---

## 🧪 Testes

### Executar Testes

```bash
mvn clean test
```

### Cobertura

```bash
mvn clean verify jacoco:report
```

**Meta:** 80%+ de cobertura

---

## 📊 Monitoramento

### New Relic APM

- Latência de APIs
- Taxa de erro
- Distributed tracing

### Métricas Customizadas

- Orçamentos gerados/hora
- Taxa de aprovação
- Valor médio de orçamentos

---

## 🔐 Segurança

- **Autenticação:** JWT via API Gateway
- **Autorização:** RBAC
- **Secrets:** AWS Secrets Manager
- **Network:** VPC privada

---

## 📚 Documentação

- **Swagger UI:** http://localhost:8082/swagger-ui.html
- **OpenAPI Spec:** http://localhost:8082/v3/api-docs

---

**Última Atualização:** 31/01/2026  
**Versão:** 1.0.0
