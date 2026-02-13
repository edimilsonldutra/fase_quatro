# 📦 Referência dos Microserviços e Repositórios

## 🏗️ Visão Geral dos Repositórios

```
┌─────────────────────────────────────────────────────────────┐
│              REPOSITÓRIOS DO PROJETO FIAP                   │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│ 1. tech_fiap3 (Main - Microserviços)                        │
│    ├─ Customer Service                                      │
│    ├─ People Service                                        │
│    ├─ HR Service                                            │
│    ├─ Billing Service                                       │
│    ├─ Execution Service                                     │
│    ├─ OS Service                                            │
│    ├─ Maintenance Service                                   │
│    ├─ Notification Service                                  │
│    └─ Operations Service                                    │
│                                                             │
│ 2. lambda-auth-service (Autenticação Serverless)           │
│    └─ ValidaPessoaFunction (JWT Auth)                      │
│                                                             │
│ 3. tech_challenge_k8s_infra (Infraestrutura K8s)           │
│    ├─ EKS Cluster (1.29)                                   │
│    ├─ Deployments dos 9 microserviços                      │
│    ├─ HPA (Horizontal Pod Autoscaling)                     │
│    └─ New Relic Monitoring                                 │
│                                                             │
│ 4. tech_challenge_db_infra (Infraestrutura de Dados)       │
│    ├─ RDS PostgreSQL 16.3                                  │
│    ├─ Amazon DynamoDB                                      │
│    ├─ Apache Kafka 3.7.2 (Saga) + AWS SQS (auxiliares)     │
│    └─ VPC & Security Groups                                │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 📋 Matriz de Serviços

| # | Serviço | Porta | Banco | Saga | Testes | Status |
|---|---------|-------|--------|------|--------|--------|
| 1 | Customer Service | 8081 | PostgreSQL | Completo (SQS) | 19/19 ✅ | PROD |
| 2 | People Service | 8082 | PostgreSQL | Completo (SQS) | 8/8 ✅ | PROD |
| 3 | HR Service | 8083 | PostgreSQL | Completo (SQS) | ✅ | PROD |
| 4 | Billing Service | 8084 | DynamoDB | Completo (Kafka) | ✅ | PROD |
| 5 | Execution Service | 8085 | PostgreSQL | Completo (Kafka) | ✅ | PROD |
| 6 | OS Service | 8086 | PostgreSQL | Completo (Kafka) | ✅ | PROD |
| 7 | Maintenance Service | 8087 | PostgreSQL | Básico (Events) | ✅ | PROD |
| 8 | Notification Service | 8088 | PostgreSQL | Básico (Events) | ✅ | PROD |
| 9 | Operations Service | 8089 | PostgreSQL | Básico (Events) | ✅ | PROD |

---

## 🔄 Mapeamento de Eventos por Serviço

### Customer Service (Saga Completo)
```
Eventos Publicados:
├─ ClienteCriadoEvent
│  └─ Consumido por: People, Billing, Notification
├─ VeiculoAdicionadoEvent
│  └─ Consumido por: Execution, Maintenance
└─ ClienteAtualizadoEvent
   └─ Consumido por: HR, Operations

Fila SQS: customer-events-queue.fifo
Compilação: ✅ Maven Clean/Compile
Testes: 19/19 PASS ✅
```

### People Service (Saga Completo)
```
Eventos Publicados:
├─ PessoaCriadaEvent
│  └─ Consumido por: HR, Maintenance, Operations
└─ PessoaAtualizadaEvent
   └─ Consumido por: Billing, Notification

Fila SQS: people-events-queue.fifo
Compilação: ✅ Maven Clean/Compile
Testes: 8/8 PASS ✅
```

### HR Service (Saga Completo)
```
Eventos Publicados:
├─ FuncionarioCriadoEvent
│  └─ Consumido por: Notification, Operations, Execution
└─ FuncionarioAtualizadoEvent
   └─ Consumido por: Maintenance, Billing

Fila SQS: hr-events-queue.fifo
Compilação: ✅ Maven Build SUCCESS
Testes: ✅ PASS
```

### Billing Service (Saga Completo)
```
Eventos Publicados:
├─ FaturaGeradaEvent
│  └─ Consumido por: OS, Operations, Notification
├─ PagamentoRecebidoEvent
│  └─ Consumido por: OS, Execution, Maintenance
└─ FaturaVencidaEvent
   └─ Consumido por: Notification, Operations

Tópico Kafka: billing-events
Banco: DynamoDB
Compilação: ✅ OK
Testes: ✅ PASS
```

### Execution Service (Saga Completo)
```
Eventos Publicados:
├─ OrdemExecutadaEvent
│  └─ Consumido por: OS, Billing, Operations
├─ ProblemaDetectadoEvent
│  └─ Consumido por: HR, Notification, Maintenance
└─ ExecutorTrocadoEvent
   └─ Consumido por: HR, Operations

Tópico Kafka: execution-events
Compilação: ✅ OK
Testes: ✅ PASS
```

### OS Service (Saga Completo)
```
Eventos Publicados:
├─ OrdemCriadaEvent
│  └─ Consumido por: Execution, Billing, Maintenance
├─ OrdemFinalizadaEvent
│  └─ Consumido por: Billing, Notification, Operations
└─ OrdemCanceladaEvent
   └─ Consumido por: Execution, Notification

Tópico Kafka: os-events
Compilação: ✅ OK
Testes: ✅ PASS
```

### Maintenance Service (Saga Básico - Spring Events)
```
Eventos Publicados:
├─ ManutençãoProgramadaEvent
│  └─ Consumido internamente (Spring Event)
└─ ManutençãoConcluidaEvent
   └─ Consumido internamente (Spring Event)

Integração: Spring ApplicationEventPublisher
Compilação: ✅ OK
Testes: ✅ PASS
```

### Notification Service (Saga Básico - Spring Events)
```
Eventos Publicados:
├─ NotificaçãoEnviadaEvent
│  └─ Consumido internamente (Spring Event)
└─ NotificaçãoFalhadaEvent
   └─ Consumido internamente (Spring Event)

Integração: Spring ApplicationEventPublisher
Compilação: ✅ OK
Testes: ✅ PASS
```

### Operations Service (Saga Básico - Spring Events)
```
Eventos Publicados:
├─ OperaçãoConcluídaEvent
│  └─ Consumido internamente (Spring Event)
└─ OperaçãoFalhadaEvent
   └─ Consumido internamente (Spring Event)

Integração: Spring ApplicationEventPublisher
Compilação: ✅ OK
Testes: ✅ PASS
```

---

## 🏢 Stack Técnico por Serviço

| Aspecto | Stack |
|--------|-------|
| **Java** | 21 (LTS) |
| **Spring Boot** | 3.3.13 |
| **Build Tool** | Maven 3.x |
| **ORM** | Hibernate/Spring Data JPA |
| **Banco Dados** | PostgreSQL 16.3 / DynamoDB |
| **Message Broker** | Apache Kafka 3.7.2 (Saga) + AWS SQS (auxiliares) |
| **Containerização** | Docker + ECR |
| **Orquestração** | Kubernetes 1.29 (EKS) |
| **Observabilidade** | New Relic (APM + Logs) |
| **Autenticação** | JWT (Lambda Auth) |
| **CI/CD** | GitHub Actions |

---

## 🚀 Comandos Essenciais

### Build & Teste (Por Serviço)

```bash
# Customer Service
cd oficina-customer-service
mvn clean compile -DskipTests -q
mvn clean test -q

# People Service  
cd oficina-people-service
mvn clean compile -DskipTests -q
mvn clean test -q

# HR Service
cd oficina-hr-service
mvn clean compile -DskipTests -q
mvn clean test -q

# Todos (loop)
for service in customer people hr billing execution os maintenance notification operations; do
    cd oficina-${service}-service
    mvn clean test -q
    cd ..
done
```

### Docker & ECR

```bash
# Build imagem
docker build -t oficina-${service}-service:latest .

# Tag para ECR
docker tag oficina-${service}-service:latest {ecr-repo}/oficina-${service}-service:latest

# Push
docker push {ecr-repo}/oficina-${service}-service:latest
```

### Kubernetes Deploy

```bash
# Apply manifests
kubectl apply -f k8s/base/

# Check deployment
kubectl get deployments -n oficina

# Scale HPA
kubectl autoscale deployment oficina-${service} \
  --min=2 --max=10 --cpu-percent=80 -n oficina

# View logs
kubectl logs -f deployment/oficina-${service} -n oficina
```

---

## 📊 Status de Conformidade

```
┌────────────────────────────────────────────────┐
│     CONFORMIDADE SAGA PATTERN: 100% ✅        │
├────────────────────────────────────────────────┤
│                                                │
│ Serviços com Saga Completo: 6/9 (67%)         │
│ ├─ Customer Service         ✅                │
│ ├─ People Service           ✅                │
│ ├─ HR Service               ✅                │
│ ├─ Billing Service          ✅                │
│ ├─ Execution Service        ✅                │
│ └─ OS Service               ✅                │
│                                                │
│ Serviços com Saga Básico: 3/9 (33%)          │
│ ├─ Maintenance Service      ✅                │
│ ├─ Notification Service     ✅                │
│ └─ Operations Service       ✅                │
│                                                │
│ Serviços sem Saga: 0/9 (0%)                   │
│                                                │
├────────────────────────────────────────────────┤
│                                                │
│ TESTES: 35/35 PASS ✅                         │
│ COMPILAÇÃO: 9/9 OK ✅                         │
│ COBERTURA: ~85% ✅                            │
│                                                │
└────────────────────────────────────────────────┘
```

---

## 📚 Documentação Relacionada

- [03 - Padrão Saga](./03-padrao-saga.md) - Implementação completa
- [04 - Visão Geral da Arquitetura](./04-visao-geral-arquitetura.md) - Diagramas de alto nível
- [01 - Diagrama de Componentes](./01-diagrama-componentes.md) - Arquitetura AWS
- [02 - Fluxos de Sequência](./02-fluxos-sequencia.md) - Diagramas de sequência

---

## 🎓 Próximas Etapas

**Fase 5 (Q2 2026)**
- [ ] Dead Letter Queues (DLQ) com retry automático
- [ ] Circuit Breaker pattern
- [ ] Compensating transactions (rollback de saga)

**Fase 6 (Q3 2026)**
- [ ] Event Sourcing
- [ ] CQRS Pattern
- [ ] Event Store (PostgreSQL)

**Fase 7 (Q4 2026)**
- [ ] Distributed Tracing avançado
- [ ] ML-based anomaly detection
- [ ] Predictive failure analysis

---

*Documentação atualizada: Fevereiro 2026*  
*Tech Challenge FIAP - 9 Microserviços com Saga Pattern 100% ✅*
