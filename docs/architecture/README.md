# 📐 Documentação da Arquitetura - Sistema de Gestão de Oficina (Microserviços)

Esta documentação descreve a arquitetura completa do sistema de gestão de oficina mecânica baseado em microserviços, desenvolvido como parte do Tech Challenge 4 da FIAP.

## 📋 Índice

| Documento | Descrição |
|-----------|-----------|
| [01 - Diagrama de Componentes](./01-diagrama-componentes.md) | Visão geral da arquitetura de microserviços na AWS |
| [02 - Fluxos de Sequência](./02-fluxos-sequencia.md) | Diagramas de sequência para comunicação entre microserviços |
| [03 - Padrão Saga](./03-padrao-saga.md) | **NOVO** - Implementação do padrão Saga Event-Driven (100% conformidade) |
| [04 - Visão Geral da Arquitetura](./04-visao-geral-arquitetura.md) | **NOVO** - Visão de alto nível com diagramas completos |
| [05 - Modelo de Dados](./05-modelo-dados.md) | Modelos de dados por microserviço |
| [06 - Referência de Microserviços](./06-referencia-microservicos.md) | **NOVO** - Matriz de serviços, eventos, testes e stack |
| [RFCs](./rfcs/) | Decisões técnicas relevantes |
| [ADRs](./adrs/) | Decisões arquiteturais permanentes |

## 🏗️ Visão Geral da Arquitetura

### Repositórios do Projeto

| Repositório | Propósito | Tecnologias |
|-------------|-----------|-------------|
| **oficina-os-service** | Gerenciamento de Ordens de Serviço | Java 21, Spring Boot 3.3, PostgreSQL, Kafka |
| **oficina-billing-service** | Orçamentos e Pagamentos | Java 21, Spring Boot 3.3, DynamoDB, Kafka |
| **oficina-execution-service** | Execução e Diagnósticos | Java 21, Spring Boot 3.3, PostgreSQL, Kafka |
| **lambda-auth-service** | Autenticação serverless | Java 21, AWS Lambda, API Gateway, JWT |
| **tech_challenge_k8s_infra** | Infraestrutura Kubernetes | Terraform, EKS 1.29, Helm, New Relic |
| **tech_challenge_db_infra** | Bancos de dados gerenciados | Terraform, RDS PostgreSQL 16.3, DynamoDB |

### Stack Tecnológica

- **Cloud Provider**: AWS
- **Container Orchestration**: Amazon EKS (Kubernetes 1.29)
- **Databases**: 
  - Amazon RDS PostgreSQL 16.3 (OS Service, Execution Service)
  - Amazon DynamoDB (Billing Service)
- **Message Broker**: Apache Kafka 3.7.2 (Saga) + Amazon SQS (auxiliares)
- **Serverless**: AWS Lambda + API Gateway
- **IaC**: Terraform
- **CI/CD**: GitHub Actions
- **Observabilidade**: New Relic (APM, Infrastructure, Logs, Distributed Tracing)

## 📊 Arquitetura de Microserviços

### Microserviços

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   OS Service    │────▶│ Billing Service │────▶│Execution Service│
│  (PostgreSQL)   │     │   (DynamoDB)    │     │  (PostgreSQL)   │
└─────────────────┘     └─────────────────┘     └─────────────────┘
        │                       │                        │
        └───────────────────────┴────────────────────────┘
                              │
                        Apache Kafka
                    (Event-Driven Communication)
```

### Responsabilidades

| Microserviço | Responsabilidade | Porta | Banco de Dados |
|--------------|-----------------|-------|----------------|
| **OS Service** | Gerenciar ciclo de vida das ordens de serviço | 8081 | PostgreSQL |
| **Billing Service** | Criar orçamentos, processar pagamentos | 8082 | DynamoDB |
| **Execution Service** | Registrar diagnósticos, tarefas e uso de peças | 8083 | PostgreSQL |
| **Lambda Auth** | Autenticar usuários via CPF e gerar JWT | - | PostgreSQL (compartilhado) |

### Comunicação entre Microserviços

- **Síncrona**: REST APIs (quando necessário consulta imediata)
- **Assíncrona**: Apache Kafka (Saga) / Amazon SQS (auxiliares)
- **Tópicos Kafka (Saga)**:
  - `os-events-queue` - Eventos de ordens de serviço
  - `billing-events-queue` - Eventos de orçamentos/pagamentos
  - `execution-events-queue` - Eventos de execução

## 🎯 Padrões Arquiteturais

### Saga Pattern (Event-Driven)
Implementação completa do padrão Saga com 100% de conformidade:
- **6 Serviços com Saga Completo**: Customer, People, HR, Billing, Execution, OS (Apache Kafka)
- **3 Serviços com Saga Básico**: Maintenance, Notification, Operations (Spring Events)
- **0 Serviços sem Saga**: Migração 100% concluída
- **Testes**: 35/35 PASS ✅

[→ Ver documentação completa de Saga Pattern](./03-padrao-saga.md)

### Domain-Driven Design (DDD)
Cada microserviço segue DDD com:
- **Domain**: Entidades e lógica de negócio
- **Application**: DTOs e casos de uso
- **Infrastructure**: Repositórios, configs, integrações

### Event-Driven Architecture
- Eventos publicados em tópicos Kafka quando há mudanças de estado
- Microserviços consomem eventos de forma assíncrona
- Desacoplamento entre serviços

### Database per Service
- Cada microserviço possui seu próprio banco de dados
- OS Service e Execution Service: PostgreSQL
- Billing Service: DynamoDB (dados mais flexíveis)

## 🚀 CI/CD e Deploy

### Pipelines GitHub Actions

Cada microserviço possui pipeline padronizado com 4 jobs:

1. **Test**: Executa testes unitários e de integração
2. **Build**: Constrói imagem Docker e faz push para ECR
3. **Deploy**: Aplica manifestos Kubernetes e cria secrets
4. **Rollback**: Restaura versão anterior em caso de falha

### Infraestrutura como Código

| Componente | Ferramenta | Repositório |
|------------|------------|-------------|
| Cluster EKS | Terraform | tech_challenge_k8s_infra |
| Bancos de Dados | Terraform | tech_challenge_db_infra |
| Kubernetes Manifests | YAML | tech_challenge_k8s_infra/microservices/* |
| Lambda Auth | SAM | lambda-auth-service |

## 📈 Observabilidade

### New Relic APM

Todos os microserviços possuem New Relic Agent integrado:

- **Distributed Tracing**: Rastreamento de requisições entre serviços
- **Transaction Tracer**: Detalhamento de transações lentas
- **Error Collector**: Captura e análise de erros
- **Application Logging**: Logs centralizados com contexto

### Métricas Monitoradas

- Latência P50, P95, P99
- Throughput (requisições/minuto)
- Taxa de erro
- Consumo de CPU/Memória
- Conexões de banco de dados
- Lag dos consumer groups Kafka

## 📖 Documentação de APIs

### Swagger/OpenAPI 3.0

Todos os microserviços expõem documentação interativa:

- **OS Service**: http://{host}:8081/swagger-ui.html
- **Billing Service**: http://{host}:8082/api/v1/swagger-ui.html
- **Execution Service**: http://{host}:8083/api/v1/swagger-ui.html

Autenticação JWT configurada em todas as APIs.

## 🔐 Segurança

- **Autenticação**: JWT Bearer Token via Lambda Auth Service
- **Secrets**: Kubernetes Secrets para credenciais sensíveis
- **Network**: Security Groups e Network Policies
- **SSL/TLS**: Certificados gerenciados pelo ALB

## 🎓 Equipe

**Grupo 99 - FIAP Tech Challenge Fase 4**

---

*Última atualização: Fevereiro 2026*

*Status: ✅ Saga Pattern 100% implementado em 9 microserviços | Todos os testes PASS | Documentação completa*
