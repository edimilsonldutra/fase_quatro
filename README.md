# 🚗 Sistema de Gestão de Oficina Mecânica - Tech Challenge Fase 4

Sistema completo de gestão de oficina mecânica baseado em arquitetura de microserviços na AWS, desenvolvido como parte do **Tech Challenge 4 da FIAP**.

## 📋 Visão Geral

Solução cloud-native que gerencia o ciclo de vida completo de uma oficina mecânica, desde a recepção de veículos até a entrega final, incluindo diagnósticos, orçamentos, execução de serviços e pagamentos.

### Arquitetura

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│ OS Service  │────▶│   Billing   │────▶│ Execution   │
│(PostgreSQL) │     │ (DynamoDB)  │     │(PostgreSQL) │
└─────────────┘     └─────────────┘     └─────────────┘
       │                   │                    │
       └───────────────────┴────────────────────┘
                          │
                   Apache Kafka
              (Event-Driven Communication)
```

## 🏗️ Repositórios do Projeto

| Repositório | Descrição | Status |
|-------------|-----------|--------|
| **[oficina-os-service](./oficina-os-service)** | Gerenciamento de Ordens de Serviço | ✅ Produção |
| **[oficina-billing-service](./oficina-billing-service)** | Orçamentos e Pagamentos | ✅ Produção |
| **[oficina-execution-service](./oficina-execution-service)** | Execução e Diagnósticos | ✅ Produção |
| **[lambda-auth-service](./lambda-auth-service)** | Autenticação Serverless | ✅ Produção |
| **[tech_challenge_k8s_infra](./tech_challenge_k8s_infra)** | Infraestrutura Kubernetes | ✅ Produção |
| **[tech_challenge_db_infra](./tech_challenge_db_infra)** | Bancos de Dados | ✅ Produção |
| **[tech_fiap3](./tech_fiap3)** | Aplicação Monolítica Legada | 📦 Deprecated |

## 🚀 Tecnologias Utilizadas

### Backend
- **Java 21** - Runtime principal
- **Spring Boot 3.3** - Framework de aplicação
- **PostgreSQL 16.3** - Banco relacional (OS, Execution e demais serviços)
- **Amazon DynamoDB** - Banco NoSQL (Billing, Catalog)

### Cloud & Infrastructure
- **AWS EKS** - Kubernetes gerenciado (versão 1.29)
- **Amazon RDS** - PostgreSQL gerenciado
- **Amazon DynamoDB** - Banco NoSQL gerenciado (Free Tier)
- **Apache Kafka** - Event streaming platform
- **AWS Lambda** - Autenticação serverless
- **Terraform** - Infrastructure as Code
- **GitHub Actions** - CI/CD

### Observability
- **New Relic APM** - Application Performance Monitoring
- **New Relic Distributed Tracing** - Rastreamento entre microserviços
- **New Relic Logs** - Centralização de logs

### Documentation
- **Swagger / OpenAPI 3.0** - Documentação de APIs
- **Markdown** - Documentação técnica

## 📊 Microserviços

### 1. OS Service (Ordem de Serviço)
**Porta**: 8081  
**Banco**: PostgreSQL  
**Namespace**: os-service

Responsável por gerenciar o ciclo de vida das ordens de serviço.

**Endpoints Principais:**
- `POST /api/ordens` - Criar OS
- `GET /api/ordens/{id}` - Consultar OS
- `PUT /api/ordens/{id}/status` - Atualizar status
- `GET /swagger-ui.html` - Documentação

**Eventos Publicados:**
- `OS_CRIADA` → billing-events-queue, execution-events-queue
- `STATUS_CHANGED` → billing-events-queue, execution-events-queue

### 2. Billing Service (Faturamento)
**Porta**: 8082  
**Banco**: DynamoDB  
**Namespace**: billing-service

Responsável por orçamentos, pagamentos e histórico financeiro.

**Endpoints Principais:**
- `POST /api/v1/orcamentos` - Criar orçamento
- `PUT /api/v1/orcamentos/{id}/aprovar` - Aprovar orçamento
- `POST /api/v1/pagamentos` - Processar pagamento
- `GET /api/v1/swagger-ui.html` - Documentação

**Eventos Publicados:**
- `ORCAMENTO_APROVADO` → os-events-queue, execution-events-queue
- `PAGAMENTO_CONFIRMADO` → os-events-queue

### 3. Execution Service (Execução)
**Porta**: 8083  
**Banco**: PostgreSQL  
**Namespace**: execution-service

Responsável por diagnósticos, tarefas, uso de peças e progresso.

**Endpoints Principais:**
- `POST /api/v1/execucoes` - Criar execução
- `POST /api/v1/execucoes/{id}/diagnostico` - Adicionar diagnóstico
- `POST /api/v1/execucoes/{id}/tarefas` - Registrar tarefa
- `GET /api/v1/swagger-ui.html` - Documentação

**Eventos Publicados:**
- `DIAGNOSTICO_CONCLUIDO` → billing-events-queue
- `EXECUCAO_FINALIZADA` → os-events-queue, billing-events-queue

### 4. Lambda Auth Service (Autenticação)
**Runtime**: Java 21  
**Banco**: PostgreSQL (compartilhado)

Autenticação serverless via CPF e geração de tokens JWT.

**Endpoint:**
- `POST /auth` - Autenticar e obter JWT

## 🔄 Comunicação entre Microserviços

### Event-Driven Architecture

```
OS Service ──(publish)──> os-events (Kafka topic) ──(consume)──> Billing/Execution
Billing    ──(publish)──> billing-events (Kafka topic) ──(consume)──> OS/Execution
Execution  ──(publish)──> execution-events (Kafka topic) ──(consume)──> OS/Billing
```

### Tópicos Kafka

| Tópico | Produtor | Consumidores | Tipos de Eventos |
|--------|----------|--------------|------------------|
| `os-events` | OS Service | Billing, Execution | OS_CRIADA, STATUS_CHANGED, OS_CANCELADA |
| `billing-events` | Billing Service | OS, Execution | ORCAMENTO_CRIADO, ORCAMENTO_APROVADO, PAGAMENTO_CONFIRMADO |
| `execution-events` | Execution Service | OS, Billing | DIAGNOSTICO_CONCLUIDO, TAREFA_CONCLUIDA, EXECUCAO_FINALIZADA |

## 🔀 Saga Pattern: Decisão por Coreografia (Choreography)

### Por que Coreografia ao invés de Orquestração?

Este projeto implementa o **Saga Pattern Coreografado** para coordenar transações distribuídas entre os microserviços. Esta foi uma **decisão arquitetural estratégica** baseada nas características do sistema e requisitos do negócio.

### Comparação: Coreografia vs Orquestração

| Aspecto | ✅ Coreografia (Escolhido) | ❌ Orquestração (Não Escolhido) |
|---------|---------------------------|--------------------------------|
| **Acoplamento** | Baixo - Serviços independentes | Alto - Dependem do orquestrador |
| **Ponto de Falha** | Distribuído - Sem SPOF | Centralizado - Orquestrador é SPOF |
| **Escalabilidade** | Excelente - Escala por serviço | Limitada - Orquestrador pode ser gargalo |
| **Complexidade** | Distribuída - Lógica espalhada | Centralizada - Fácil visualização |
| **Debugging** | Difícil - Rastreamento distribuído | Fácil - Lógica em um lugar |
| **Autonomia** | Alta - Serviços autônomos | Baixa - Coordenação centralizada |
| **Manutenção** | Requer disciplina no time | Simples - Mudanças centralizadas |

### Justificativa da Escolha: Coreografia

#### ✅ Vantagens para Este Projeto

1. **Baixo Acoplamento entre Serviços**
   - Cada microserviço é completamente independente
   - Novos serviços podem ser adicionados sem modificar os existentes
   - Mudanças em um serviço não afetam diretamente os outros

2. **Escalabilidade e Performance**
   - Não há gargalo de um orquestrador central
   - Cada serviço escala independentemente conforme sua carga
   - Apache Kafka gerencia automaticamente picos de mensagens com partições e consumer groups

3. **Resiliência e Disponibilidade**
   - Não existe Single Point of Failure (SPOF)
   - Se um serviço falha, os outros continuam operando
   - Compensações automáticas em caso de falhas

4. **Alinhamento com Event-Driven Architecture**
   - Migramos de SQS para Kafka, ganhando replay de eventos e alta throughput
   - Eventos são naturalmente parte do domínio (OS criada, orçamento aprovado, etc.)
   - Equipe já tinha experiência com mensageria

5. **Simplicidade da Transação Distribuída**
   - Fluxo relativamente simples: OS → Orçamento → Pagamento → Execução
   - Apenas 3 microserviços envolvidos
   - Sem condicionais complexas ou loops no fluxo

#### ⚠️ Desvantagens Aceitas

1. **Complexidade de Debugging**
   - **Mitigação**: New Relic Distributed Tracing para rastreamento completo
   - **Mitigação**: Logs estruturados com correlationId em todas as mensagens

2. **Visibilidade do Fluxo Distribuído**
   - **Mitigação**: Documentação clara dos fluxos em [SAGA_PATTERN_IMPLEMENTATION.md](./docs/SAGA_PATTERN_IMPLEMENTATION.md)
   - **Mitigação**: Dashboards do New Relic para monitoramento end-to-end

3. **Lógica de Negócio Espalhada**
   - **Mitigação**: ADRs documentando cada fluxo e compensação
   - **Mitigação**: Testes de integração validando o fluxo completo

### Quando Reconsiderar a Decisão?

A orquestração seria mais adequada SE:

- ❌ Número de microserviços crescer para >5 serviços no fluxo
- ❌ Lógica de negócio envolver condicionais complexas (if/else, loops)
- ❌ Necessidade de um dashboard visual para não-técnicos acompanharem fluxos
- ❌ Processos de negócio mudarem com muita frequência
- ❌ Time não tiver experiência com event-driven architecture

### Implementação do Saga Pattern

O Saga Coreografado está implementado com:

- ✅ **9 tipos de eventos**: 5 fluxo normal + 4 compensação
- ✅ **Rollback e Compensação**: Automático via try-catch nos listeners
- ✅ **Idempotência**: Verificação de duplicatas antes de processar
- ✅ **Retry e DLQ**: Kafka com retry topics e Dead Letter Topic
- ✅ **Distributed Tracing**: New Relic rastreando todos os eventos

### Documentação Completa do Saga

Para entender a implementação técnica completa:

1. **[SAGA_PATTERN_IMPLEMENTATION.md](./docs/SAGA_PATTERN_IMPLEMENTATION.md)** - Guia completo de implementação
2. **[SAGA_ROLLBACK_COMPENSATION.md](./docs/SAGA_ROLLBACK_COMPENSATION.md)** - Cenários de compensação
3. **[SAGA_ROLLBACK_DIAGRAM.md](./docs/SAGA_ROLLBACK_DIAGRAM.md)** - Diagramas visuais dos fluxos
4. **[ADR-002](./docs/architecture/adrs/ADR-002-database-per-service.md)** - Decisão de Database per Service + Saga

### Fluxo Normal (Happy Path)

```
1. Cliente solicita serviço
   ↓
2. OS Service cria OS → publica OS_CRIADA
   ↓
3. Execution Service recebe evento → cria diagnóstico → publica DIAGNOSTICO_CONCLUIDO
   ↓
4. Billing Service recebe evento → cria orçamento → publica ORCAMENTO_PRONTO
   ↓
5. Cliente aprova orçamento
   ↓
6. Billing publica ORCAMENTO_APROVADO
   ↓
7. Execution recebe evento → executa serviço → publica EXECUCAO_FINALIZADA
   ↓
8. Billing processa pagamento → publica PAGAMENTO_CONFIRMADO
   ↓
9. OS Service atualiza status → OS CONCLUÍDA
```

### Fluxo de Compensação (Unhappy Path)

```
Se FALHA em qualquer etapa:
   ↓
Serviço publica evento de compensação
   ↓
Outros serviços recebem e fazem rollback:
   - OS_CANCELADA → Billing e Execution cancelam registros
   - ORCAMENTO_REJEITADO → Execution cancela agendamento
   - EXECUCAO_FALHOU → OS marca como falha, Billing cancela cobrança
   - PAGAMENTO_FALHOU → Execution para trabalho, OS marca pendência
```

## 📦 Deploy e CI/CD

### Pipelines GitHub Actions

Cada microserviço possui pipeline padronizado com 4 jobs:

1. **Test** - Executa testes unitários e de integração
2. **Build** - Constrói imagem Docker e faz push para ECR
3. **Deploy** - Aplica manifestos Kubernetes e cria secrets
4. **Rollback** - Restaura versão anterior em caso de falha

### Comandos de Deploy

```bash
# Deploy de infraestrutura Kubernetes
cd tech_challenge_k8s_infra
terraform init
terraform plan -out=tfplan
terraform apply tfplan

# Deploy de bancos de dados
cd tech_challenge_db_infra
terraform init
terraform plan -out=tfplan
terraform apply tfplan

# Deploy de microserviços (via GitHub Actions)
# Push para branch master dispara pipeline automaticamente
git push origin master
```

### Undeploy

Cada microserviço possui workflow de undeploy:

```bash
# Via GitHub Actions → Undeploy [Service Name]
# Digitar "DESTROY" para confirmar
# Selecionar environment (dev/staging/production)
```

## 🔒 Segurança

### Autenticação JWT

Todos os endpoints protegidos requerem token JWT no header:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Obtenção do Token

```bash
curl -X POST https://api-gateway-url/auth \
  -H "Content-Type: application/json" \
  -d '{"cpf": "12345678901"}'
```

### Secrets Kubernetes

Criados automaticamente pelo CI/CD pipeline:

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: os-service-secrets
data:
  DB_USERNAME: <base64>
  DB_PASSWORD: <base64>
  NEW_RELIC_LICENSE_KEY: <base64>
```

## 📈 Observabilidade

### New Relic APM

- **Distributed Tracing**: Rastreamento entre microserviços
- **Transaction Tracer**: Detalhamento de transações lentas
- **Error Collector**: Captura e análise de erros
- **Application Logging**: Logs centralizados com contexto

### Dashboards Criados

1. **Microservices Overview** - Visão geral de todos os serviços
2. **Distributed Traces** - Traces completos entre serviços
3. **Kafka Monitoring** - Monitoramento de tópicos e consumer groups
4. **Database Performance** - Queries lentas e conexões

### Alertas Configurados

| Alerta | Condição | Severidade |
|--------|----------|------------|
| High Latency | P95 > 3s por 5 min | Critical |
| Error Rate Spike | Taxa > 5% | Critical |
| Service Down | Apdex < 0.5 | Critical |
| Kafka DLT Growing | DLT > 10 msgs | Warning |

## 📖 Documentação

### Arquitetura

- **[README Principal](./docs/architecture/README.md)** - Visão geral completa
- **[Diagrama de Componentes](./docs/architecture/01-diagrama-componentes.md)** - Arquitetura detalhada
- **[Fluxos de Sequência](./docs/architecture/02-fluxos-sequencia.md)** - Diagramas de interação
- **[Modelo de Dados](./docs/architecture/05-modelo-dados.md)** - Schemas de banco

### Decisões Arquiteturais (ADRs)

- **[ADR-001](./docs/architecture/adrs/ADR-001-event-driven-architecture.md)** - Event-Driven Architecture
- **[ADR-002](./docs/architecture/adrs/ADR-002-database-per-service.md)** - Database per Service
- **[ADR-003](./docs/architecture/adrs/ADR-003-distributed-tracing-newrelic.md)** - Distributed Tracing

### RFCs

- **[RFC-001](./docs/architecture/rfcs/RFC-001-migracao-microservicos.md)** - Migração para Microserviços

### Guias Técnicos

- **[CI/CD Pipelines](./CICD-PIPELINES.md)** - Documentação dos pipelines
- **[New Relic Config](./NEW-RELIC-CONFIG.md)** - Configuração do New Relic
- **[Swagger Documentation](./SWAGGER-DOCUMENTATION.md)** - Documentação das APIs
- **[Saga Pattern Implementation](./docs/SAGA_PATTERN_IMPLEMENTATION.md)** - Implementação completa do Saga
- **[Saga Rollback & Compensation](./docs/SAGA_ROLLBACK_COMPENSATION.md)** - Cenários de compensação
- **[Saga Flow Diagrams](./docs/SAGA_ROLLBACK_DIAGRAM.md)** - Diagramas visuais dos fluxos

## 🧪 Testes

### Executar Testes Localmente

```bash
# OS Service
cd oficina-os-service
mvn clean test

# Billing Service
cd oficina-billing-service
mvn clean test

# Execution Service
cd oficina-execution-service
mvn clean test
```

### Coverage

- **OS Service**: 85%
- **Billing Service**: 82%
- **Execution Service**: 80%
- **Target**: > 80%

## 🌐 URLs de Acesso

### Desenvolvimento Local

| Serviço | URL | Swagger |
|---------|-----|---------|
| OS Service | http://localhost:8081 | http://localhost:8081/swagger-ui.html |
| Billing Service | http://localhost:8082 | http://localhost:8082/api/v1/swagger-ui.html |
| Execution Service | http://localhost:8083 | http://localhost:8083/api/v1/swagger-ui.html |

### Produção (EKS)

Após provisionamento da infraestrutura:

```bash
# Obter LoadBalancers
kubectl get svc -n os-service
kubectl get svc -n billing-service
kubectl get svc -n execution-service
```

## 💰 Custos Estimados (AWS)

| Componente | Custo Mensal (USD) |
|------------|--------------------|
| EKS Cluster | $73 |
| EC2 Nodes (4x t3.medium) | $200 |
| RDS PostgreSQL (2 instâncias) | $200 |
| DynamoDB | $25 (Free Tier) |
| Kafka (MSK) | $15 |
| New Relic | $150 |
| **Total** | **$713** |

## 🎯 Roadmap

### Fase 4 (Atual) ✅
- [x] Arquitetura de microserviços
- [x] Event-Driven com Apache Kafka (migrado de SQS)
- [x] CI/CD pipelines
- [x] New Relic Distributed Tracing
- [x] Swagger documentation

### Fase 5 (Futuro)
- [ ] API Gateway centralizado
- [ ] Cache Redis
- [ ] Circuit Breaker (Resilience4j)
- [ ] Service Mesh (Istio)
- [ ] GraphQL Federation

## 👥 Equipe

**Grupo 99 - FIAP Tech Challenge Fase 4**

| Nome | Responsabilidade |
|------|------------------|
| Desenvolvedor 1 | OS Service + Infraestrutura |
| Desenvolvedor 2 | Billing Service + CI/CD |
| Desenvolvedor 3 | Execution Service + Observabilidade |

## 📞 Suporte

Para dúvidas ou problemas:
- **Email**: grupo99@fiap.com.br
- **Issues**: GitHub Issues em cada repositório

## 📄 Licença

Este projeto foi desenvolvido para fins educacionais como parte do Tech Challenge da FIAP.

---

**Última atualização**: Janeiro 2026  
**Versão**: 4.0.0
