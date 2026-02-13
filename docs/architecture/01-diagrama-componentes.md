# 🏗️ Diagrama de Componentes - Arquitetura de Microserviços

## Visão Geral

O sistema de oficina mecânica é composto por uma arquitetura cloud-native baseada em microserviços, distribuída em múltiplos serviços AWS, seguindo práticas de infraestrutura como código (IaC), Event-Driven Architecture e DevOps.

## Arquitetura de Alto Nível

```
┌─────────────────────────────────────────────────────────────────────┐
│                           Cliente / Frontend                         │
└───────────────────────────────┬─────────────────────────────────────┘
                                │
                ┌───────────────┴───────────────┐
                │       API Gateway (AWS)        │
                │   - Rate Limiting              │
                │   - JWT Validation             │
                └───────────────┬───────────────┘
                                │
        ┌───────────────────────┼───────────────────────┐
        │                       │                       │
        ▼                       ▼                       ▼
┌──────────────┐    ┌─────────────────┐    ┌──────────────────┐
│   Lambda     │    │  Application    │    │   Application    │
│Auth Service  │    │ Load Balancer   │    │ Load Balancer    │
│ (Serverless) │    │   (OS Service)  │    │ (Billing/Exec)   │
└──────────────┘    └────────┬────────┘    └────────┬─────────┘
                             │                      │
                    ┌────────┴─────────────────────┴────────┐
                    │                                        │
                    ▼                                        ▼
        ┌─────────────────────────────────────────────────────────┐
        │              Amazon EKS (Kubernetes 1.29)               │
        │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐ │
        │  │  OS Service  │  │   Billing    │  │  Execution   │ │
        │  │ (Namespace:  │  │   Service    │  │   Service    │ │
        │  │ os-service)  │  │ (Namespace:  │  │ (Namespace:  │ │
        │  │              │  │billing-srv)  │  │execution-srv)│ │
        │  │ ┌──────────┐ │  │ ┌──────────┐ │  │ ┌──────────┐ │ │
        │  │ │  Pod 1   │ │  │ │  Pod 1   │ │  │ │  Pod 1   │ │ │
        │  │ │  Pod 2   │ │  │ │  Pod 2   │ │  │ │  Pod 2   │ │ │
        │  │ │  HPA     │ │  │ │  HPA     │ │  │ │  HPA     │ │ │
        │  │ │ (2-10)   │ │  │ │ (2-10)   │ │  │ │ (2-10)   │ │ │
        │  │ └──────────┘ │  │ └──────────┘ │  │ └──────────┘ │ │
        │  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘ │
        └─────────┼──────────────────┼──────────────────┼─────────┘
                  │                  │                  │
         ┌────────┴────────┬────────┴────────┬─────────┴────────┐
         │                 │                 │                  │
         ▼                 ▼                 ▼                  ▼
┌─────────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│  RDS PostgreSQL │ │   DynamoDB   │ │RDS PostgreSQL│ │ Apache Kafka │
│   (OS Service)  │ │  (Billing)   │ │ (Execution)  │ │  Messaging   │
└─────────────────┘ └──────────────┘ └──────────────┘ └──────────────┘
                                                               │
                    ┌──────────────────────────────────────────┤
                    │                                          │
            ┌───────▼────────┐  ┌────────────────┐  ┌────────▼────────┐
            │ os-events-topic│  │billing-events- │  │execution-events-│
            │                │  │     topic      │  │     topic       │
            └────────────────┘  └────────────────┘  └─────────────────┘

                    ┌─────────────────────────────────┐
                    │      New Relic Platform         │
                    │  - APM (Distributed Tracing)    │
                    │  - Infrastructure Monitoring    │
                    │  - Logs Management              │
                    │  - Custom Dashboards            │
                    └─────────────────────────────────┘
```

## Componentes Principais

### 1. Camada de Entrada (Edge Layer)

| Componente | Tecnologia | Função |
|------------|------------|--------|
| **API Gateway** | AWS API Gateway | Ponto de entrada para autenticação, roteamento e rate limiting |
| **ALB Ingress** | AWS Application Load Balancer | Load balancer para tráfego dos microserviços no Kubernetes |

### 2. Camada de Autenticação (Serverless)

| Componente | Tecnologia | Função |
|------------|------------|--------|
| **Lambda Auth Service** | AWS Lambda (Java 21) | Validação de CPF e geração de tokens JWT |
| **VPC Integration** | ENI em subnets privadas | Acesso seguro ao banco de dados RDS |

**Características:**
- Timeout: 30 segundos
- Memória: 512 MB
- Runtime: Java 21 com container Docker
- Autoscaling automático gerenciado pela AWS
- Banco: PostgreSQL compartilhado (tabela `pessoas`)

### 3. Camada de Microserviços (Kubernetes/EKS)

#### 3.1 OS Service (Ordem de Serviço)

| Atributo | Valor |
|----------|-------|
| **Namespace** | os-service |
| **Porta** | 8081 |
| **Banco de Dados** | RDS PostgreSQL 16.3 |
| **Réplicas** | 2-10 (HPA) |
| **Responsabilidade** | Gerenciar ciclo de vida das ordens de serviço |

**Recursos:**
```yaml
resources:
  requests:
    cpu: 100m
    memory: 256Mi
  limits:
    cpu: 500m
    memory: 512Mi
```

**Endpoints Principais:**
- `POST /api/ordens` - Criar ordem de serviço
- `GET /api/ordens/{id}` - Consultar ordem
- `PUT /api/ordens/{id}/status` - Atualizar status
- `GET /swagger-ui.html` - Documentação API

#### 3.2 Billing Service (Faturamento)

| Atributo | Valor |
|----------|-------|
| **Namespace** | billing-service |
| **Porta** | 8082 |
| **Banco de Dados** | Amazon DynamoDB |
| **Réplicas** | 2-10 (HPA) |
| **Responsabilidade** | Orçamentos, pagamentos e histórico financeiro |

**Recursos:**
```yaml
resources:
  requests:
    cpu: 100m
    memory: 256Mi
  limits:
    cpu: 500m
    memory: 512Mi
```

**Endpoints Principais:**
- `POST /api/v1/orcamentos` - Criar orçamento
- `GET /api/v1/orcamentos/{id}` - Consultar orçamento
- `POST /api/v1/pagamentos` - Processar pagamento
- `GET /api/v1/swagger-ui.html` - Documentação API

#### 3.3 Execution Service (Execução)

| Atributo | Valor |
|----------|-------|
| **Namespace** | execution-service |
| **Porta** | 8083 |
| **Banco de Dados** | RDS PostgreSQL 16.3 |
| **Réplicas** | 2-10 (HPA) |
| **Responsabilidade** | Diagnósticos, tarefas, uso de peças e progresso |

**Recursos:**
```yaml
resources:
  requests:
    cpu: 100m
    memory: 256Mi
  limits:
    cpu: 500m
    memory: 512Mi
```

**Endpoints Principais:**
- `POST /api/v1/execucoes` - Criar execução
- `POST /api/v1/execucoes/{id}/diagnostico` - Adicionar diagnóstico
- `POST /api/v1/execucoes/{id}/tarefas` - Registrar tarefa
- `GET /api/v1/swagger-ui.html` - Documentação API

### 4. Camada de Dados (Data Layer)

#### 4.1 RDS PostgreSQL (OS Service)

| Configuração | Valor |
|--------------|-------|
| **Engine** | PostgreSQL 16.3 |
| **Storage** | 20 GB (expansível) |
| **Multi-AZ** | Configurável |
| **VPC** | Subnets privadas |
| **Tabelas Principais** | ordens_servico, historico_status, veiculos |

#### 4.2 DynamoDB (Billing Service)

| Configuração | Valor |
|--------------| ------|
| **Engine** | Amazon DynamoDB |
| **Storage** | 20 GB (expansível) |
| **Tables** | orcamentos, pagamentos, historico_status |
| **Flexibilidade** | Schema-less para dados variáveis |

#### 4.3 RDS PostgreSQL (Execution Service)

| Configuração | Valor |
|--------------|-------|
| **Engine** | PostgreSQL 16.3 |
| **Storage** | 20 GB (expansível) |
| **Multi-AZ** | Configurável |
| **Tabelas Principais** | execucoes_os, diagnosticos, tarefas, uso_pecas |

### 5. Camada de Mensageria (Messaging Layer)

#### Apache Kafka

| Tópico | Produtor | Consumidor | Tipo de Evento |
|--------|----------|------------|----------------|
| **os-events-topic** | OS Service | Billing, Execution | Mudanças de status da OS |
| **billing-events-topic** | Billing Service | OS, Execution | Orçamentos aprovados/pagamentos |
| **execution-events-topic** | Execution Service | OS, Billing | Tarefas concluídas |

**Características:**
- Dead Letter Topic (DLT) para mensagens com falha
- Consumer Group isolation por microserviço
- Message Retention: 7 dias
- Partições: 3 por tópico

### 6. Camada de Observabilidade (Observability Layer)

| Componente | Tecnologia | Função |
|------------|------------|--------|
| **New Relic APM** | Java Agent | Monitoramento de performance e distributed tracing |
| **New Relic Infrastructure** | nri-bundle (Helm) | Monitoramento do cluster EKS e pods |
| **New Relic Logging** | Fluent Bit | Coleta e centralização de logs |
| **Custom Dashboards** | New Relic One | Dashboards personalizados por microserviço |

**Métricas Monitoradas:**
- Latência das APIs (P50, P95, P99)
- Throughput (requisições/minuto)
- Taxa de erro (4xx, 5xx)
- Consumo de CPU e memória dos pods
- Healthchecks e uptime
- Lag dos consumer groups Kafka
- Conexões de banco de dados
- Distributed traces entre microserviços

### 7. Infraestrutura como Código (IaC)

| Componente | Tecnologia | Repositório |
|------------|------------|-------------|
| **EKS Cluster** | Terraform | tech_challenge_k8s_infra |
| **VPC e Networking** | Terraform | tech_challenge_k8s_infra |
| **RDS PostgreSQL** | Terraform | tech_challenge_db_infra |
| **DynamoDB** | Terraform | tech_challenge_db_infra |
| **Kafka Topics** | Terraform | tech_challenge_k8s_infra |
| **K8s Manifests** | YAML | tech_challenge_k8s_infra/microservices/* |
| **Lambda Auth** | SAM | lambda-auth-service |
| **New Relic** | Helm | tech_challenge_k8s_infra |

---

## Fluxos de Comunicação

### Fluxo Síncrono (REST)
```
Cliente → API Gateway → ALB → Microserviço → Database → Response
```

### Fluxo Assíncrono (Event-Driven)
```
Microserviço A → Kafka Topic → Microserviço B (Consumer)
```

### Fluxo de Autenticação
```
Cliente → API Gateway → Lambda Auth → RDS → JWT Token → Cliente
```

### Fluxo de Monitoramento
```
Todos os Componentes → New Relic APM/Infrastructure → Dashboards/Alerts
```

---

## Repositórios e Responsabilidades

| Repositório | Componentes Gerenciados |
|-------------|------------------------|
| `oficina-os-service` | OS Service, Dockerfile, CI/CD pipeline |
| `oficina-billing-service` | Billing Service, Dockerfile, CI/CD pipeline |
| `oficina-execution-service` | Execution Service, Dockerfile, CI/CD pipeline |
| `lambda-auth-service` | Lambda Auth, API Gateway (SAM template) |
| `tech_challenge_k8s_infra` | EKS, VPC, Kafka, K8s manifests, New Relic Bundle |
| `tech_challenge_db_infra` | RDS PostgreSQL (2x), DynamoDB, VPC, Security Groups |

---

## Padrões de Design Implementados

### Database per Service
Cada microserviço possui seu próprio banco de dados isolado.

### API Gateway Pattern
Ponto único de entrada com roteamento e autenticação.

### Event-Driven Architecture
Comunicação assíncrona via Apache Kafka para desacoplamento.

### Circuit Breaker
Proteção contra falhas em cascata (via Resilience4j).

### Health Check Pattern
Endpoints `/actuator/health` em todos os serviços.

### Distributed Tracing
Rastreamento de requisições entre microserviços via New Relic.

---

*Última atualização: Janeiro 2026*
