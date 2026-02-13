# 🔧 Execution Service - Execução e Produção

[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3.13-6DB33F?logo=springboot)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk)](https://openjdk.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-316192?logo=postgresql)](https://www.postgresql.org/)
[![Apache Kafka](https://img.shields.io/badge/Apache_Kafka-3.7.2-231F20?logo=apachekafka)](https://kafka.apache.org/)

Microsserviço responsável por gerenciar a execução, diagnósticos, tarefas e uso de peças em uma oficina mecânica.

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

O **Execution Service** é o microsserviço operacional responsável por gerenciar toda a fase de execução das ordens de serviço, incluindo diagnósticos, tarefas realizadas, uso de peças e controle de estoque.

### Bounded Context

Este serviço representa o **bounded context "Execução e Produção"** no modelo Domain-Driven Design (DDD).

---

## 🔷 Responsabilidades

- ✅ **Criar execução** - Iniciar execução quando orçamento é aprovado e pago
- ✅ **Registrar diagnóstico** - Documentar diagnósticos realizados
- ✅ **Registrar tarefas** - Acompanhar tarefas executadas
- ✅ **Controlar peças** - Gerenciar uso de peças com controle de estoque
- ✅ **Finalizar execução** - Marcar execução como concluída
- ✅ **Publicar eventos** - Notificar outros serviços sobre finalizações

---

## 🏗️ Arquitetura

### Clean Architecture (Hexagonal)

```
┌─────────────────────────────────────────┐
│        Infrastructure Layer             │
│  (REST Controllers, Kafka Listeners,     │
│   PostgreSQL Repositories, Configs)     │
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
| **Banco de Dados** | PostgreSQL | 16 | ACID para controle de estoque |
| **Mensageria** | Apache Kafka | 3.7.2 | Comunicação assíncrona (Event-Driven) |
| **ORM** | Spring Data JPA | - | Simplifica acesso ao banco |
| **Migrations** | Flyway | - | Versionamento de schema |
| **Observabilidade** | New Relic APM | - | Monitoramento e tracing |
| **Testes** | JUnit 5, Cucumber | - | Testes unitários e BDD |
| **Build** | Maven | 3.9+ | Gerenciamento de dependências |
| **Container** | Docker | - | Empacotamento da aplicação |
| **Orquestração** | Kubernetes (EKS) | 1.29 | Deploy e escalabilidade |

---

## 🔌 APIs REST

### Base URL
```
Development: http://localhost:8083/api/v1
Production:  https://api.oficina.com/execution-service/api/v1
```

### Endpoints

#### 1. Criar Execução

```http
POST /api/v1/execucoes
Content-Type: application/json
Authorization: Bearer <JWT>
```

**Request Body:**
```json
{
  "osId": "uuid",
  "orcamentoId": "uuid",
  "mecanico": "João Silva"
}
```

**Response:** `201 Created`

---

#### 2. Buscar Execução por ID

```http
GET /api/v1/execucoes/{id}
Authorization: Bearer <JWT>
```

**Response:** `200 OK`

---

#### 3. Buscar Execução por OS

```http
GET /api/v1/execucoes/os/{osId}
Authorization: Bearer <JWT>
```

**Response:** `200 OK`

---

#### 4. Registrar Diagnóstico

```http
POST /api/v1/execucoes/{id}/diagnosticos
Content-Type: application/json
Authorization: Bearer <JWT>
```

**Request Body:**
```json
{
  "descricao": "Vazamento de óleo no motor",
  "mecanico": "João Silva",
  "observacoes": "Requer troca de junta"
}
```

**Response:** `201 Created`

---

#### 5. Registrar Tarefa

```http
POST /api/v1/execucoes/{id}/tarefas
Content-Type: application/json
Authorization: Bearer <JWT>
```

**Request Body:**
```json
{
  "descricao": "Troca de óleo",
  "mecanico": "João Silva",
  "tempoEstimado": 30,
  "tempoReal": 25
}
```

**Response:** `201 Created`

---

#### 6. Registrar Uso de Peça

```http
POST /api/v1/execucoes/{id}/pecas
Content-Type: application/json
Authorization: Bearer <JWT>
```

**Request Body:**
```json
{
  "pecaId": "uuid",
  "descricao": "Óleo 5W30",
  "quantidade": 4,
  "valorUnitario": 45.00
}
```

**Response:** `201 Created`

---

#### 7. Finalizar Execução

```http
PATCH /api/v1/execucoes/{id}/finalizar
Content-Type: application/json
Authorization: Bearer <JWT>
```

**Request Body:**
```json
{
  "observacoes": "Serviço concluído com sucesso"
}
```

**Response:** `200 OK`

---

## 📨 Eventos (Kafka)

### Eventos Publicados

#### 1. **ExecucaoIniciadaEvent**

Publicado quando execução é iniciada.

**Tópico:** `execution-events`

**Payload:**
```json
{
  "eventId": "uuid",
  "eventType": "ExecucaoIniciadaEvent",
  "timestamp": "2026-01-31T14:00:00Z",
  "aggregateId": "execucao-uuid",
  "version": 1,
  "payload": {
    "execucaoId": "uuid",
    "osId": "uuid",
    "mecanico": "João Silva"
  }
}
```

---

#### 2. **ExecucaoFinalizadaEvent**

Publicado quando execução é finalizada.

**Payload:**
```json
{
  "eventId": "uuid",
  "eventType": "ExecucaoFinalizadaEvent",
  "timestamp": "2026-01-31T16:00:00Z",
  "aggregateId": "execucao-uuid",
  "version": 2,
  "payload": {
    "execucaoId": "uuid",
    "osId": "uuid",
    "dataFinalizacao": "2026-01-31T16:00:00Z",
    "totalPecas": 450.00
  }
}
```

**Consumidores:**
- OS Service (para atualizar status para CONCLUIDA)

---

### Eventos Consumidos

#### 1. **PagamentoConfirmadoEvent** (de Billing Service)

Inicia execução quando pagamento é confirmado.

**Tópico consumido:** `billing-events`

---

## 💾 Banco de Dados

### PostgreSQL 16 (AWS RDS)

**Justificativa:**
- ✅ **ACID:** Transações para controle de estoque
- ✅ **Integridade:** Foreign keys e constraints
- ✅ **Performance:** Índices eficientes
- ✅ **Confiabilidade:** Dados críticos de produção

### Tabelas

#### Tabela: `execucoes_os`

```sql
CREATE TABLE execucoes_os (
    id UUID PRIMARY KEY,
    os_id UUID NOT NULL UNIQUE,
    orcamento_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    mecanico VARCHAR(100) NOT NULL,
    data_inicio TIMESTAMP NOT NULL,
    data_finalizacao TIMESTAMP,
    observacoes TEXT,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_execucoes_os_id ON execucoes_os(os_id);
CREATE INDEX idx_execucoes_status ON execucoes_os(status);
CREATE INDEX idx_execucoes_mecanico ON execucoes_os(mecanico);
```

---

#### Tabela: `diagnosticos`

```sql
CREATE TABLE diagnosticos (
    id UUID PRIMARY KEY,
    execucao_id UUID NOT NULL REFERENCES execucoes_os(id) ON DELETE CASCADE,
    descricao TEXT NOT NULL,
    mecanico VARCHAR(100) NOT NULL,
    data_diagnostico TIMESTAMP NOT NULL,
    observacoes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_diagnosticos_execucao ON diagnosticos(execucao_id);
```

---

#### Tabela: `tarefas`

```sql
CREATE TABLE tarefas (
    id UUID PRIMARY KEY,
    execucao_id UUID NOT NULL REFERENCES execucoes_os(id) ON DELETE CASCADE,
    descricao VARCHAR(200) NOT NULL,
    mecanico VARCHAR(100) NOT NULL,
    tempo_estimado_minutos INTEGER,
    tempo_real_minutos INTEGER,
    status VARCHAR(20) NOT NULL,
    data_inicio TIMESTAMP NOT NULL,
    data_finalizacao TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_tarefas_execucao ON tarefas(execucao_id);
CREATE INDEX idx_tarefas_status ON tarefas(status);
```

---

#### Tabela: `uso_pecas`

```sql
CREATE TABLE uso_pecas (
    id UUID PRIMARY KEY,
    execucao_id UUID NOT NULL REFERENCES execucoes_os(id) ON DELETE CASCADE,
    peca_id UUID NOT NULL,
    descricao VARCHAR(200) NOT NULL,
    quantidade INTEGER NOT NULL,
    valor_unitario DECIMAL(10, 2) NOT NULL,
    valor_total DECIMAL(10, 2) NOT NULL,
    data_uso TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_uso_pecas_execucao ON uso_pecas(execucao_id);
CREATE INDEX idx_uso_pecas_peca ON uso_pecas(peca_id);
```

---

## ⚙️ Configuração

### Variáveis de Ambiente

```yaml
# PostgreSQL
POSTGRES_HOST: execution-postgres.rds.amazonaws.com
POSTGRES_PORT: 5432
POSTGRES_DB: execution_db
POSTGRES_USER: <from-secrets-manager>
POSTGRES_PASSWORD: <from-secrets-manager>

# Apache Kafka
KAFKA_BOOTSTRAP_SERVERS: kafka:9092
KAFKA_TOPIC_BILLING_EVENTS: billing-events
KAFKA_TOPIC_EXECUTION_EVENTS: execution-events

# Spring Profiles
SPRING_PROFILES_ACTIVE: prod

# Logging
LOG_LEVEL: INFO

# New Relic
NEW_RELIC_LICENSE_KEY: <from-secrets-manager>
NEW_RELIC_APP_NAME: execution-service

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
kubectl get pods -n execution-service
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

- Execuções iniciadas/hora
- Tempo médio de execução
- Peças mais utilizadas

---

## 🔐 Segurança

- **Autenticação:** JWT via API Gateway
- **Autorização:** RBAC
- **Secrets:** AWS Secrets Manager
- **Network:** VPC privada

---

## 📚 Documentação

- **Swagger UI:** http://localhost:8083/swagger-ui.html
- **OpenAPI Spec:** http://localhost:8083/v3/api-docs

---

**Última Atualização:** 31/01/2026  
**Versão:** 1.0.0
