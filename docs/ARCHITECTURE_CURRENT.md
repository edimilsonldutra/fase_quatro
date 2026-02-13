# 🏗️ Arquitetura Atualizada - Tech Challenge FIAP 4

**Versão:** 2.0  
**Data:** Fevereiro 2026  
**Status:** ✅ PRODUÇÃO | **244/244 Testes Passando** | **Kafka Migration Complete**

---

## 📊 Resumo Executivo

### Tecnologias Principais

| Categoria | Tecnologia | Versão | Status |
|-----------|------------|--------|--------|
| **Linguagem** | Java | 21 LTS | ✅ |
| **Framework** | Spring Boot | 3.3.13 | ✅ |
| **Mensageria** | Apache Kafka | 3.7.2 | ✅ Migrado de SQS |
| **Resiliência** | Resilience4j | 2.2.0 | ✅ Circuit Breaker |
| **Container** | Docker + Kubernetes | EKS 1.29 | ✅ |
| **Cloud** | AWS | - | ✅ |
| **IaC** | Terraform | 1.5.0 | ✅ |
| **Observabilidade** | New Relic | - | ✅ |

---

## 🎯 Arquitetura de Microserviços (10 Serviços)

### Serviços Principais (Saga Pattern com Kafka)

```
                            ┌─────────────────┐
                            │ API Gateway     │
                            │ (ALB + Ingress) │
                            └────────┬────────┘
                                     │
                 ┌───────────────────┼───────────────────┐
                 │                   │                   │
        ┌────────▼────────┐ ┌───────▼────────┐ ┌───────▼────────┐
        │   OS Service    │ │ Execution Svc  │ │ Billing Svc    │
        │   PostgreSQL    │ │   PostgreSQL   │ │   DynamoDB     │
        │   48 tests ✅   │ │   47 tests ✅  │ │  40 tests ✅   │
        └────────┬────────┘ └───────┬────────┘ └───────┬────────┘
                 │                   │                   │
                 └───────────────────┼───────────────────┘
                                     │
                        ┌────────────▼────────────┐
                        │   Apache Kafka Cluster  │
                        │   • os-events           │
                        │   • billing-events      │
                        │   • execution-events    │
                        │   • compensacao-events  │
                        │                         │
                        │   Circuit Breaker ✅    │
                        │   Retry Pattern ✅      │
                        └─────────────────────────┘
```

### 1️⃣ **OS Service** (Ordens de Serviço)
- **Banco:** PostgreSQL 16.3 (RDS)
- **Responsabilidade:** Gerenciar ciclo de vida das OS
- **Eventos Publicados:** `OSCriadaEvent`, `StatusMudadoEvent`, `OSCanceladaEvent`
- **Circuit Breaker:** ✅ 4 métodos protegidos
- **Testes:** 48 ✅

**Endpoints:**
```
POST   /api/v1/ordens-servico
GET    /api/v1/ordens-servico/{id}
PATCH  /api/v1/ordens-servico/{id}/status
```

---

### 2️⃣ **Execution Service** (Execução e Diagnóstico)
- **Banco:** PostgreSQL 16.3 (RDS)
- **Responsabilidade:** Diagnóstico e execução de serviços
- **Eventos Publicados:** `DiagnosticoConcluidoEvent`, `ExecucaoConcluidaEvent`, `ExecucaoFalhouEvent`
- **Eventos Consumidos:** `OSCriadaEvent`, `OrcamentoAprovadoEvent`
- **Circuit Breaker:** ✅ 3 métodos protegidos
- **Testes:** 47 ✅

**Endpoints:**
```
POST   /api/v1/diagnosticos
POST   /api/v1/execucoes
GET    /api/v1/execucoes/{osId}
```

---

### 3️⃣ **Billing Service** (Orçamento e Pagamento)
- **Banco:** DynamoDB (NoSQL) - Schema flexível para orçamentos
- **Responsabilidade:** Orçamentos, aprovações e pagamentos
- **Eventos Publicados:** `OrcamentoProntoEvent`, `OrcamentoAprovadoEvent`, `PagamentoFalhouEvent`
- **Eventos Consumidos:** `OSCriadaEvent`, `DiagnosticoConcluidoEvent`, `ExecucaoConcluidaEvent`
- **Circuit Breaker:** ✅ 4 métodos protegidos
- **Testes:** 40 ✅

**Endpoints:**
```
POST   /api/v1/orcamentos
PUT    /api/v1/orcamentos/{id}/aprovar
POST   /api/v1/pagamentos
```

---

### Serviços CRUD (7 Microserviços)

| Serviço | Banco | Responsabilidade | Testes |
|---------|-------|------------------|--------|
| **People Service** | PostgreSQL | Gestão de pessoas | 23 ✅ |
| **Customer Service** | PostgreSQL | Clientes e veículos | 21 ✅ |
| **HR Service** | PostgreSQL | Funcionários | 19 ✅ |
| **Catalog Service** | PostgreSQL | Catálogo de serviços/peças | 18 ✅ |
| **Operations Service** | PostgreSQL | Operações diárias | 12 ✅ |
| **Maintenance Service** | PostgreSQL | Manutenções programadas | 8 ✅ |
| **Notification Service** | PostgreSQL | Notificações | 8 ✅ |

**Características:**
- ❌ Não usam Kafka (apenas HTTP)
- ❌ Não possuem Circuit Breaker (CRUD simples)
- ✅ Clean Architecture
- ✅ 100% testes passando

---

## 🔄 Event-Driven Architecture (Kafka)

### Tópicos Kafka

```yaml
Topics:
  os-events:                    # Eventos de OS
    partitions: 3
    replication-factor: 3
    retention.ms: 604800000     # 7 dias
    
  billing-events:               # Eventos de Faturamento
    partitions: 3
    replication-factor: 3
    
  execution-events:             # Eventos de Execução
    partitions: 3
    replication-factor: 3
    
  compensacao-events:           # Saga Compensation
    partitions: 3
    replication-factor: 3
```

### Fluxo Saga Coreografada

```
┌──────────────────────────────────────────────────────────┐
│                    FLUXO HAPPY PATH                      │
└──────────────────────────────────────────────────────────┘

1. Cliente cria OS
   ↓
2. OS Service publica OSCriadaEvent (os-events)
   ↓
3. Execution Service consome → Faz diagnóstico
   ↓
4. Execution Service publica DiagnosticoConcluidoEvent
   ↓
5. Billing Service consome → Cria orçamento
   ↓
6. Billing Service publica OrcamentoProntoEvent
   ↓
7. Cliente aprova orçamento
   ↓
8. Billing Service publica OrcamentoAprovadoEvent
   ↓
9. Execution Service consome → Executa serviços
   ↓
10. Execution Service publica ExecucaoConcluidaEvent
   ↓
11. Billing Service consome → Registra pagamento
   ↓
12. OS Service atualiza status → FINALIZADA ✅

┌──────────────────────────────────────────────────────────┐
│                 FLUXO COMPENSAÇÃO (Rollback)             │
└──────────────────────────────────────────────────────────┘

A. Falha na Execução
   ↓
B. Execution Service publica ExecucaoFalhouEvent (compensacao-events)
   ↓
C. OS Service consome → Cancela OS
   ↓
D. Billing Service consome → Cancela orçamento
   ↓
E. Estado consistente restaurado ✅
```

---

## 🛡️ Resiliência (Circuit Breaker)

### Resilience4j Configuration

**Implementado em:** OS Service, Execution Service, Billing Service

```yaml
resilience4j:
  circuitbreaker:
    instances:
      kafkaPublisher:
        registerHealthIndicator: true
        slidingWindowSize: 10              # Janela de 10 chamadas
        minimumNumberOfCalls: 5            # Mínimo 5 para avaliar
        failureRateThreshold: 50           # Abre após 50% falhas
        waitDurationInOpenState: 30s       # Aguarda 30s antes de tentar
        permittedCallsInHalfOpen: 3        # 3 tentativas em half-open
        
  retry:
    instances:
      kafkaPublisher:
        maxAttempts: 3                     # 3 tentativas
        waitDuration: 1s                   # 1s inicial
        exponentialBackoff: true           # 1s → 2s → 4s
        
  timelimiter:
    instances:
      kafkaPublisher:
        timeoutDuration: 10s               # Timeout 10s
```

### Uso no Código

```java
@CircuitBreaker(name = "kafkaPublisher", fallbackMethod = "publishEventFallback")
@Retry(name = "kafkaPublisher")
@TimeLimiter(name = "kafkaPublisher")
public void publishOSCriada(OSCriadaEvent event) {
    kafkaTemplate.send("os-events", event.getOsId().toString(), event);
}

// Fallback quando Circuit Breaker abre
public void publishEventFallback(OSCriadaEvent event, Throwable t) {
    log.error("🔴 Circuit Breaker ABERTO - Evento não publicado: {}", t.getMessage());
    // Salvar em outbox table para retry posterior
}
```

### Métricas de Resiliência

| Serviço | Métodos Protegidos | Fallbacks | Coverage |
|---------|-------------------|-----------|----------|
| OS Service | 4 | ✅ | 100% |
| Execution Service | 3 | ✅ | 100% |
| Billing Service | 4 | ✅ | 100% |
| **TOTAL** | **11** | **11** | **100%** |

---

## 🏛️ Clean Architecture

### Estrutura de Camadas
```
src/main/java/br/com/grupo99/{service}/
├── domain/              # Camada de Domínio (Entidades, VOs, Regras)
│   ├── model/          # Entidades e Aggregates
│   ├── events/         # Domain Events
│   └── ports/          # Interfaces (Ports)
│
├── application/        # Camada de Aplicação (Use Cases)
│   └── usecases/      # Casos de uso (orquestração)
│
└── infrastructure/     # Camada de Infraestrutura (Adaptadores)
    ├── rest/          # Controllers REST
    ├── messaging/     # Kafka Publishers/Listeners
    ├── persistence/   # Repositories JPA/DynamoDB
    └── config/        # Configurações Spring
```

### Princípios Implementados

✅ **Dependency Inversion:** Domain não conhece Infrastructure  
✅ **Single Responsibility:** Cada camada tem uma responsabilidade clara  
✅ **Open/Closed:** Extensível via interfaces (ports)  
✅ **Interface Segregation:** Ports específicos por contexto  
✅ **Liskov Substitution:** Implementações intercambiáveis

---

## 🗄️ Estratégia de Bancos de Dados

### Database per Service

| Serviço | Banco | Justificativa |
|---------|-------|---------------|
| **OS Service** | PostgreSQL | Dados estruturados, relacionamentos complexos |
| **Execution Service** | PostgreSQL | Dados estruturados, tarefas e diagnósticos |
| **Billing Service** | **DynamoDB** | Schema flexível para orçamentos variáveis |
| **People Service** | PostgreSQL | Dados estruturados de pessoas |
| **Customer Service** | PostgreSQL | Clientes e veículos relacionados |
| **HR Service** | PostgreSQL | Funcionários e departamentos |
| **Catalog Service** | PostgreSQL | Catálogo de produtos/serviços |
| **Operations Service** | PostgreSQL | Operações diárias |
| **Maintenance Service** | PostgreSQL | Manutenções agendadas |
| **Notification Service** | PostgreSQL | Histórico de notificações |

**Total:** 9 PostgreSQL + 1 DynamoDB

---

## ☁️ Infraestrutura Cloud (AWS)

### Componentes

```
┌─────────────────────────────────────────────────────┐
│                     AWS Cloud                       │
│                                                     │
│  ┌──────────────┐      ┌──────────────┐           │
│  │   Route 53   │──────│     ALB      │           │
│  │     DNS      │      │ Load Balance │           │
│  └──────────────┘      └──────┬───────┘           │
│                                │                    │
│                    ┌───────────▼───────────┐       │
│                    │   Amazon EKS 1.29     │       │
│                    │   (Kubernetes)        │       │
│                    │   • 10 Deployments    │       │
│                    │   • HPA enabled       │       │
│                    │   • 3+ nodes          │       │
│                    └───────────┬───────────┘       │
│                                │                    │
│         ┌──────────────────────┼──────────────────┐│
│         │                      │                  ││
│  ┌──────▼──────┐   ┌──────────▼─────┐  ┌────────▼▼─────┐
│  │ RDS (9x)    │   │   DynamoDB    │  │  Apache Kafka  │
│  │ PostgreSQL  │   │ (Billing +   │  │   Cluster      │
│  │   16.3      │   │  Catalog)     │  │   3.7.2        │
│  └─────────────┘   └────────────────┘  └────────────────┘
│                                                     │
│  ┌──────────────────────────────────────────────┐ │
│  │         AWS SSM Parameter Store              │ │
│  │         (Secrets Management)                 │ │
│  └──────────────────────────────────────────────┘ │
│                                                     │
│  ┌──────────────────────────────────────────────┐ │
│  │            New Relic APM                     │ │
│  │            (Observability)                   │ │
│  └──────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────┘
```

### Terraform Modules

```
tech_challenge_db_infra/
├── rds.tf                  # 9 instâncias PostgreSQL + DynamoDB
├── security.tf            # Security Groups
└── ssm.tf                 # Parameter Store

tech_challenge_k8s_infra/
├── eks.tf                 # Cluster EKS
├── vpc.tf                 # Networking
├── newrelic.tf            # Observabilidade
└── dashboards.tf          # Dashboards
```

---

## 🔄 CI/CD Pipeline

### GitHub Actions Workflows

**Para cada microserviço:**

```yaml
name: Build and Deploy {Service}

on:
  push:
    branches: [master, main]
  workflow_dispatch:

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
      - run: mvn clean test
      
  build:
    needs: test
    runs-on: ubuntu-latest
    steps:
      - run: mvn package -DskipTests
      - uses: aws-actions/amazon-ecr-login@v2
      - run: docker build -t {service}:${{ github.sha }} .
      - run: docker push ${{ ecr.registry }}/{service}:${{ github.sha }}
      
  deploy:
    needs: build
    runs-on: ubuntu-latest
    steps:
      - run: aws eks update-kubeconfig --name tech-challenge-cluster
      - run: kubectl set image deployment/{service} {service}=${{ ecr.image }}
      - run: kubectl rollout status deployment/{service}
```

**Total Pipelines:** 10 microserviços + 2 infraestrutura = **12 pipelines**

---

## 📈 Testes e Cobertura

### Estatísticas Globais

| Categoria | Total | Passando | % |
|-----------|-------|----------|---|
| **Testes Totais** | 244 | 244 | **100%** ✅ |
| **Testes Unitários** | 180 | 180 | 100% |
| **Testes Integração** | 50 | 50 | 100% |
| **Testes BDD (Cucumber)** | 14 | 14 | 100% |

### Por Microserviço

| Serviço | Testes | Cobertura | Status |
|---------|--------|-----------|--------|
| OS Service | 48 | 85% | ✅ |
| Execution Service | 47 | 78% | ✅ |
| Billing Service | 40 | 72% | ✅ |
| People Service | 23 | 65% | ✅ |
| Customer Service | 21 | 68% | ✅ |
| HR Service | 19 | 62% | ✅ |
| Catalog Service | 18 | 70% | ✅ |
| Operations Service | 12 | 58% | ✅ |
| Maintenance Service | 8 | 55% | ✅ |
| Notification Service | 8 | 60% | ✅ |

---

## 📊 Observabilidade

### New Relic Integration

```yaml
Dashboards:
  - Application Performance (APM)
  - Infrastructure Monitoring
  - Kafka Metrics
  - Circuit Breaker States
  - Error Rates
  - Response Times

Alertas:
  - Circuit Breaker OPEN
  - Error Rate > 5%
  - Response Time > 1s p95
  - Kafka Consumer Lag > 100
```

### Métricas Prometheus

```
# Circuit Breaker
resilience4j_circuitbreaker_state{name="kafkaPublisher"} 1
resilience4j_circuitbreaker_failure_rate{name="kafkaPublisher"} 0.0

# Kafka
kafka_producer_record_send_total{topic="os-events"} 1250
kafka_consumer_records_consumed_total{topic="billing-events"} 1180

# Application
http_server_requests_seconds_count{uri="/api/v1/ordens-servico"} 850
```

---

## 📚 Referências

### Documentos Técnicos
- [Análise Circuit Breaker](../ANALISE_CIRCUIT_BREAKER.md)
- [Relatório Auditoria AWS/GitHub](../RELATORIO_AUDITORIA_AWS_GITHUB.md)
- [Plano Correção AWS](../PLANO_CORRECAO_AWS.md)
- [Migration Summary](./MIGRATION_SUMMARY.md)

### Padrões Implementados
- ✅ Clean Architecture
- ✅ Saga Pattern (Coreografada)
- ✅ Event-Driven Architecture
- ✅ Circuit Breaker Pattern
- ✅ Retry Pattern
- ✅ Database per Service
- ✅ API Gateway

### RFC/ADR
- [RFC-001: Escolha AWS](./architecture/rfcs/RFC-001-escolha-cloud-aws.md)
- [RFC-002: Estratégia Banco de Dados](./architecture/rfcs/RFC-002-estrategia-banco-dados.md)
- [RFC-003: Kafka vs SQS](./architecture/rfcs/RFC-003-kafka-migration.md)
- [ADR-001: Padrão REST](./architecture/adrs/ADR-001-padrao-comunicacao-rest.md)
- [ADR-002: HPA Kubernetes](./architecture/adrs/ADR-002-uso-hpa-kubernetes.md)
- [ADR-003: New Relic](./architecture/adrs/ADR-003-observabilidade-newrelic.md)

---

**Gerado em:** 09/02/2026  
**Próxima Atualização:** Março 2026  
**Versão:** 2.0
