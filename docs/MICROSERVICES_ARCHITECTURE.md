# 🏗️ Arquitetura de Microsserviços - Oficina Mecânica

## 📋 Índice
- [Visão Geral](#visão-geral)
- [Bounded Contexts e Microsserviços](#bounded-contexts-e-microsserviços)
- [Saga Pattern](#saga-pattern)
- [Estratégia de Bancos de Dados](#estratégia-de-bancos-de-dados)
- [Comunicação entre Serviços](#comunicação-entre-serviços)
- [Arquitetura Cloud (AWS)](#arquitetura-cloud-aws)
- [CI/CD e DevOps](#cicd-e-devops)
- [Observabilidade](#observabilidade)

---

## 🎯 Visão Geral

Este projeto implementa uma aplicação de gestão de oficina mecânica com **10 microserviços independentes**, cada um com seu próprio banco de dados, infraestrutura e pipeline CI/CD. A arquitetura utiliza **Apache Kafka** para comunicação assíncrona e implementa **Circuit Breaker** (Resilience4j) para resiliência.

### Princípios Arquiteturais

- ✅ **Separação por Bounded Context** (Domain-Driven Design)
- ✅ **Database per Service** (cada serviço tem seu próprio banco)
- ✅ **API First** (contratos bem definidos)
- ✅ **Eventual Consistency** (via mensageria)
- ✅ **Saga Pattern** para transações distribuídas
- ✅ **Cloud Native** (Kubernetes, containers)

---

## 🔷 Bounded Contexts e Microsserviços

### 1️⃣ **OS Service** (Ordem de Serviço)

**Responsabilidade:** Gerenciar o ciclo de vida das ordens de serviço

**Domínio:**
- Criar ordem de serviço
- Atualizar status da OS
- Consultar status e histórico
- Rastreamento de estados

**Entidades Principais:**
- `OrdemServico` (agregado raiz)
- `StatusOS` (enum: RECEBIDA, EM_DIAGNOSTICO, AGUARDANDO_APROVACAO, EM_EXECUCAO, FINALIZADA, ENTREGUE, CANCELADA)
- `Cliente` (referência)
- `Veiculo` (referência)

**Banco de Dados:** PostgreSQL (relacional)

**Eventos Publicados (Kafka):**
- `OSCriadaEvent` (topic: os-events)
- `StatusMudadoEvent` (topic: os-events)
- `OSCanceladaEvent` (topic: compensacao-events)

**Circuit Breaker:** ✅ Resilience4j com fallbacks
**Retry Pattern:** ✅ Exponential backoff (3 tentativas)

**APIs REST:**
```
POST   /api/v1/ordens-servico
GET    /api/v1/ordens-servico/{id}
GET    /api/v1/ordens-servico
PATCH  /api/v1/ordens-servico/{id}/status
DELETE /api/v1/ordens-servico/{id}
```

---

### 2️⃣ **Billing Service** (Orçamento e Pagamento)

**Responsabilidade:** Gerenciar orçamentos, aprovações e pagamentos

**Domínio:**
- Gerar orçamento baseado em serviços e peças
- Enviar orçamento para aprovação (email/notificação)
- Registrar aprovação/rejeição do cliente
- Processar pagamentos
- Atualizar status da OS após pagamento

**Entidades Principais:**
- `Orcamento` (agregado raiz)
- `ItemOrcamento` (serviços e peças)
- `Pagamento`
- `StatusOrcamento` (PENDENTE, APROVADO, REJEITADO)
- `StatusPagamento` (AGUARDANDO, PAGO, CANCELADO)

**Banco de Dados:** DynamoDB (NoSQL)
- **Justificativa:** Flexibilidade para armazenar estruturas variáveis de orçamentos, histórico de aprovações e documentos de pagamento. Custo beneficiado pelo Free Tier da AWS

**Eventos Consumidos (Kafka):**
- `OSCriadaEvent` (topic: os-events) → Gera orçamento inicial
- `DiagnosticoConcluidoEvent` (topic: execution-events)

**Eventos Publicados (Kafka):**
- `OrcamentoProntoEvent` (topic: billing-events)
- `OrcamentoAprovadoEvent` (topic: billing-events)
- `OrcamentoRejeitadoEvent` (topic: compensacao-events)
- `PagamentoFalhouEvent` (topic: compensacao-events)

**Circuit Breaker:** ✅ Resilience4j com fallbacks
**Retry Pattern:** ✅ Exponential backoff (3 tentativas)

**APIs REST:**
```
POST   /api/v1/orcamentos
GET    /api/v1/orcamentos/{id}
GET    /api/v1/orcamentos/os/{osId}
PATCH  /api/v1/orcamentos/{id}/aprovar
PATCH  /api/v1/orcamentos/{id}/rejeitar
POST   /api/v1/pagamentos
GET    /api/v1/pagamentos/{id}
```

---

### 3️⃣ **Execution Service** (Execução e Produção)

**Responsabilidade:** Gerenciar execução, diagnóstico e reparos

**Domínio:**
- Gerenciar fila de execução de OS
- Atribuir mecânicos às tarefas
- Registrar diagnóstico do veículo
- Registrar progresso dos reparos
- Controlar uso de peças do estoque
- Comunicar finalização ao OS Service

**Entidades Principais:**
- `ExecucaoOS` (agregado raiz)
- `Diagnostico`
- `Tarefa` (serviço executado)
- `UsoDeliPeca`
- `Mecanico` (referência)
- `StatusExecucao` (AGUARDANDO, EM_DIAGNOSTICO, EM_REPARO, FINALIZADO)

**Banco de Dados:** PostgreSQL 16.3 (RDS)
- **Justificativa:** Necessidade de transações ACID para controle de estoque de peças e atribuição de tarefas

**Eventos Consumidos (Kafka):**
- `OrcamentoAprovadoEvent` → Inicia execução
- `PagamentoConfirmadoEvent` → Libera para execução

**Eventos Publicados (Kafka):**
- `ExecucaoIniciadaEvent`
- `DiagnosticoConcluidoEvent`
- `ReparoConcluidoEvent`
- `ExecucaoFinalizadaEvent`

**APIs REST:**
```
POST   /api/v1/execucoes
GET    /api/v1/execucoes/{id}
GET    /api/v1/execucoes/fila
PATCH  /api/v1/execucoes/{id}/iniciar-diagnostico
PATCH  /api/v1/execucoes/{id}/iniciar-reparo
PATCH  /api/v1/execucoes/{id}/finalizar
POST   /api/v1/execucoes/{id}/usar-peca
```

---

## 🔄 Saga Pattern

### Escolha: **Coreografia** (Event-Driven)

**Justificativa:**
- ✅ **Baixo acoplamento:** Serviços não precisam conhecer uns aos outros
- ✅ **Escalabilidade:** Cada serviço pode escalar independentemente
- ✅ **Resiliência:** Falhas em um serviço não bloqueiam outros
- ✅ **Simplicidade inicial:** Não requer orquestrador centralizado
- ❌ **Desvantagem:** Dificulta visualização do fluxo completo (mitigado com observabilidade)

### Fluxo do Saga: Abertura de OS até Finalização

```
1. OS Service: Cria OS (status: RECEBIDA)
   └─> Publica: OsAbertaEvent
   
2. Billing Service: Escuta OsAbertaEvent
   └─> Gera orçamento automático
   └─> Publica: OrcamentoGeradoEvent
   
3. Cliente aprova orçamento (via API do Billing)
   └─> Billing Service: Publica OrcamentoAprovadoEvent
   
4. Cliente efetua pagamento (via API do Billing)
   └─> Billing Service: Publica PagamentoConfirmadoEvent
   
5. Execution Service: Escuta PagamentoConfirmadoEvent
   └─> Inicia execução da OS
   └─> Publica: ExecucaoIniciadaEvent
   
6. OS Service: Escuta ExecucaoIniciadaEvent
   └─> Atualiza status: EM_EXECUCAO
   
7. Execution Service: Finaliza reparos
   └─> Publica: ExecucaoFinalizadaEvent
   
8. OS Service: Escuta ExecucaoFinalizadaEvent
   └─> Atualiza status: FINALIZADA
```

### Estratégia de Compensação (Rollback)

**Cenário 1: Cliente rejeita orçamento**
```
Billing Service: OrcamentoRejeitadoEvent
└─> OS Service escuta → Atualiza status: CANCELADA
```

**Cenário 2: Pagamento falha**
```
Billing Service: PagamentoFalhadoEvent
└─> OS Service escuta → Atualiza status: AGUARDANDO_PAGAMENTO
└─> Execution Service escuta → Remove da fila de execução
```

**Cenário 3: Estoque insuficiente durante execução**
```
Execution Service: EstoqueInsuficienteEvent
└─> Billing Service escuta → Gera orçamento adicional
└─> OS Service escuta → Atualiza status: AGUARDANDO_APROVACAO
```

**Implementação Técnica:**
- Dead Letter Queue (DLQ) para eventos não processados
- Retry policy com exponential backoff
- Idempotência nos consumidores de eventos
- Timeout de 5 minutos por etapa do Saga

---

## 💾 Estratégia de Bancos de Dados

### Database per Service Pattern

| Microsserviço | Banco de Dados | Justificativa |
|---------------|----------------|---------------|
| **OS Service** | PostgreSQL | • Transações ACID para mudanças de status<br>• Relacionamentos fortes (Cliente, Veículo)<br>• Histórico auditável |
| **Billing Service** | DynamoDB | • Flexibilidade para estruturas variáveis de orçamento<br>• Armazenamento de documentos NoSQL<br>• Free Tier da AWS, custo reduzido |
| **Execution Service** | PostgreSQL | • Controle transacional de estoque<br>• Integridade referencial (Mecânicos, Peças)<br>• ACID para movimentações de estoque |

### Separação de Dados

**Princípio:** Cada serviço possui seu próprio schema/database e **nunca** acessa diretamente o banco de outro serviço.

**Dados Compartilhados:**
- **Replicação via Eventos:** Cada serviço mantém cópia dos dados necessários (eventual consistency)
- **Exemplo:** Execution Service mantém cache de `clienteId` e `veiculoId`, mas não acessa tabela de clientes

**Migração de Dados:**
```
Monolito (PostgreSQL único)
  ├─ pessoas, clientes, veiculos → OS Service DB
  ├─ ordem_servico, status_os → OS Service DB
  ├─ orcamentos, pagamentos → Billing Service DB (migração para DynamoDB)
  └─ execucoes, pecas, estoque → Execution Service DB
```

---

## 🔗 Comunicação entre Serviços

### Comunicação Síncrona (REST)

**Quando usar:**
- Consultas em tempo real (leitura)
- Operações idempotentes
- Necessidade de resposta imediata

**Exemplo:**
```http
GET /api/v1/ordens-servico/{id}
→ Resposta imediata com dados da OS
```

### Comunicação Assíncrona (Apache Kafka)

**Quando usar:**
- Comandos que iniciam processos de negócio
- Eventos que notificam mudanças de estado
- Operações que não exigem resposta imediata

**Configuração Kafka:**
```yaml
Tópicos:
  - os-events (para Billing e Execution escutarem)
  - billing-events (para OS e Execution escutarem)
  - execution-events (para OS e Billing escutarem)
  - compensacao-events (para rollback da Saga)
  
Características:
  - Partições: 3
  - Replicação: 3
  - Retenção: 7 dias
  - Consumer Groups: por serviço
```

**Formato de Mensagem:**
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
    "status": "RECEBIDA"
  }
}
```

### Padrões de Integração

1. **Event Notification:** Serviço notifica mudança de estado
2. **Event-Carried State Transfer:** Evento carrega dados relevantes para evitar chamadas síncronas
3. **Outbox Pattern:** Eventos salvos em tabela local antes de publicar (garantia de entrega)

---

## ☁️ Arquitetura Cloud (AWS)

### Visão Geral

```
┌─────────────────────────────────────────────────────┐
│                   API Gateway                        │
│              (Autenticação JWT)                      │
└───────────────────┬─────────────────────────────────┘
                    │
        ┌───────────┼───────────┬───────────────┐
        │           │           │               │
┌───────▼─────┐ ┌──▼──────┐ ┌──▼──────────┐  │
│ OS Service  │ │ Billing │ │ Execution   │  │
│ (EKS Pod)   │ │ Service │ │ Service     │  │
│             │ │ (EKS)   │ │ (EKS)       │  │
└─────┬───────┘ └──┬──────┘ └──┬──────────┘  │
      │            │            │              │
      ▼            ▼            ▼              │
┌──────────┐ ┌──────────┐ ┌──────────┐       │
│PostgreSQL│ │ DynamoDB │ │PostgreSQL│       │
│  (RDS)   │ │  (NoSQL) │ │  (RDS)   │       │
│          │ │          │ │          │       │
└──────────┘ └──────────┘ └──────────┘       │
                                               │
      ┌────────────────────────────────────────┘
      │
      ▼
┌─────────────────┐
│  Apache Kafka   │
│  (4 tópicos)    │
└─────────────────┘
```

### Componentes AWS

| Componente | Serviço AWS | Configuração |
|------------|-------------|--------------|
| **Kubernetes** | Amazon EKS | • 3 node groups (1 por serviço)<br>• Auto-scaling habilitado |
| **OS Service DB** | RDS PostgreSQL | • db.t3.medium<br>• Multi-AZ para produção |
| **Billing Service DB** | DynamoDB | • NoSQL gerenciado<br>• Free Tier • On-demand capacity |
| **Execution Service DB** | RDS PostgreSQL | • db.t3.medium<br>• Multi-AZ |
| **Mensageria** | Apache Kafka | • 4 tópicos<br>• Event-driven |
| **API Gateway** | AWS API Gateway | • Rate limiting<br>• Autenticação JWT |
| **Observabilidade** | New Relic + CloudWatch | • APM em todos os serviços<br>• Logs centralizados |
| **Secrets** | AWS Secrets Manager | • Credenciais de banco<br>• Chaves API |

---

## 🚀 CI/CD e DevOps

### Estratégia de Repositórios

Cada microsserviço em repositório separado:
```
- oficina-os-service/          (OS Service)
- oficina-billing-service/     (Billing Service)
- oficina-execution-service/   (Execution Service)
- oficina-infrastructure/      (Terraform compartilhado)
```

### Pipeline CI/CD (GitHub Actions)

**Cada microsserviço possui pipeline independente:**

```yaml
Stages:
  1. Build & Test
     - Compilação Maven/Gradle
     - Testes unitários (JUnit 5)
     - Cobertura de código (JaCoCo > 80%)
     
  2. Quality Gate
     - SonarQube analysis
     - Falha se qualidade < Grade A
     
  3. Security Scan
     - Dependabot
     - Trivy (scan de vulnerabilidades em imagem Docker)
     
  4. Build Docker Image
     - Multi-stage build
     - Tag: <service>:<commit-sha>
     - Push para ECR
     
  5. Deploy to Dev
     - Helm chart deploy
     - Smoke tests
     
  6. Deploy to Staging (approval required)
     - Helm chart deploy
     - Integration tests
     
  7. Deploy to Production (approval required)
     - Blue/Green deployment
     - Rollback automático se health check falhar
```

### Branch Protection

```yaml
main branch:
  - Pull Request obrigatório
  - Mínimo 1 aprovação
  - CI deve passar (build + tests + SonarQube)
  - Não permite force push
```

---

## 📊 Observabilidade

### Pilares de Observabilidade

#### 1. **Métricas** (New Relic + Prometheus)

**Métricas de Negócio:**
- Total de OS abertas por hora
- Tempo médio de aprovação de orçamento
- Taxa de conversão (orçamento → pagamento)
- Tempo médio de execução de OS

**Métricas Técnicas:**
- Latência de APIs (p50, p95, p99)
- Taxa de erro (4xx, 5xx)
- Throughput (requisições/segundo)
- Consumo de CPU/memória por pod

#### 2. **Logs** (Structured Logging)

**Formato JSON:**
```json
{
  "timestamp": "2026-01-31T10:00:00Z",
  "level": "INFO",
  "service": "os-service",
  "traceId": "abc123",
  "spanId": "def456",
  "message": "OS criada com sucesso",
  "osId": "uuid",
  "clienteId": "uuid"
}
```

**Centralização:** New Relic Logging ou CloudWatch Logs Insights

#### 3. **Tracing Distribuído** (New Relic APM)

- Rastreamento de requisições entre microsserviços
- Identificação de gargalos
- Análise de latência end-to-end
- Exemplo: `POST /ordens-servico` → evento Kafka → Billing Service

#### 4. **Alertas**

```yaml
Alertas Críticos:
  - Taxa de erro > 5% (5 minutos)
  - Latência p95 > 2 segundos
  - Consumer lag Kafka > 1000 mensagens
  - Pods em CrashLoopBackOff
  
Canal: PagerDuty → Slack → Email
```

---

## 🧪 Testes e Qualidade

### Pirâmide de Testes

```
       /\
      /  \     E2E Tests (BDD)
     /----\    
    / Inte \   Integration Tests
   / gration\  
  /----------\ 
 /   Unit     \ Unit Tests (80%+)
/--------------\
```

### Estratégia de Testes por Serviço

#### Testes Unitários (80%+ cobertura)
- Todos os services, controllers, validators
- Mock de dependências externas
- Ferramenta: JUnit 5 + Mockito + AssertJ

#### Testes de Integração
- Teste com bancos de dados reais (Testcontainers)
- Teste de publicação/consumo de eventos Kafka (Embedded Kafka)
- Spring Boot Test

#### Testes BDD (Behavior-Driven Development)
**Feature:** Fluxo completo de abertura de OS até finalização

```gherkin
Feature: Fluxo completo de Ordem de Serviço

  Scenario: Cliente abre OS, aprova orçamento, paga e serviço é executado
    Given um cliente cadastrado com CPF "123.456.789-00"
    And um veículo cadastrado com placa "ABC-1234"
    When o cliente abre uma ordem de serviço para o veículo
    Then a OS deve ser criada com status "RECEBIDA"
    And um orçamento deve ser gerado automaticamente
    When o cliente aprova o orçamento
    And o cliente efetua o pagamento
    Then a OS deve entrar na fila de execução
    When o mecânico inicia o diagnóstico
    Then a OS deve ter status "EM_DIAGNOSTICO"
    When o mecânico finaliza o reparo
    Then a OS deve ter status "FINALIZADA"
```

**Ferramenta:** Cucumber + Spring Boot + RestAssured

#### SonarQube Quality Gates
```yaml
Qualidade Mínima:
  - Cobertura: 80%
  - Duplicação: < 3%
  - Code Smells: Grade A
  - Bugs: 0
  - Vulnerabilidades: 0
```

---

## 📚 Documentação de APIs

### OpenAPI 3.0 (Swagger)

Cada serviço expõe documentação em:
```
http://<service>/swagger-ui.html
http://<service>/v3/api-docs
```

### Contratos de Eventos (AsyncAPI)

Documentação de eventos Kafka em formato AsyncAPI 2.0

---

## 🔐 Segurança

### Autenticação e Autorização

- **Lambda Auth Service:** Validação de CPF/CNPJ e geração de JWT
- **API Gateway:** Validação de JWT em todas as requisições
- **RBAC:** Roles: CLIENTE, MECANICO, ADMIN

### Secrets Management

- Credenciais de banco no AWS Secrets Manager
- Kubernetes Secrets sincronizados com External Secrets Operator

---

## 📝 Decisões Arquiteturais (ADRs)

### ADR-001: Uso de Saga Coreografado

**Contexto:** Necessidade de coordenar transações distribuídas entre microsserviços

**Decisão:** Implementar Saga Pattern via coreografia (event-driven)

**Consequências:**
- ✅ Baixo acoplamento entre serviços
- ✅ Escalabilidade independente
- ❌ Complexidade na visualização do fluxo (mitigado com tracing)

---

### ADR-002: DynamoDB para Billing Service

**Contexto:** Billing Service precisa armazenar orçamentos com estruturas variáveis

**Decisão:** Usar DynamoDB (NoSQL) ao invés de PostgreSQL

**Consequências:**
- ✅ Flexibilidade para estruturas de orçamento dinâmicas
- ✅ Free Tier da AWS - custo reduzido
- ✅ Escalabilidade automática sem gerenciamento de cluster
- ❌ Ausência de transactions distribuídas (mitigado por Saga)

---

### ADR-003: EKS para orquestração

**Contexto:** Necessidade de orquestração de containers

**Decisão:** Usar Amazon EKS ao invés de ECS

**Consequências:**
- ✅ Portabilidade (Kubernetes standard)
- ✅ Ecosistema maduro (Helm, Operators)
- ❌ Maior complexidade operacional

---

## 🎯 Roadmap de Implementação

### Fase 1: Fundação (Semanas 1-2)
- [x] Definição de arquitetura
- [ ] Criação de repositórios
- [ ] Setup de infraestrutura base (Terraform)
- [ ] Configuração de pipelines CI/CD

### Fase 2: Desenvolvimento (Semanas 3-6)
- [ ] Implementação do OS Service
- [ ] Implementação do Billing Service
- [ ] Implementação do Execution Service
- [ ] Integração via Kafka

### Fase 3: Qualidade (Semana 7)
- [ ] Testes unitários (80%+ cobertura)
- [ ] Testes de integração
- [ ] Teste BDD end-to-end
- [ ] SonarQube quality gates

### Fase 4: Deployment (Semana 8)
- [ ] Deploy em ambiente de desenvolvimento
- [ ] Deploy em ambiente de staging
- [ ] Testes de carga (K6)
- [ ] Deploy em produção

---

## 📞 Contatos e Suporte

**Equipe de Desenvolvimento:**
- Arquitetura: [Equipe de Arquitetura]
- DevOps: [Equipe de DevOps]
- QA: [Equipe de QA]

**Monitoramento:**
- New Relic Dashboard: [URL]
- PagerDuty: [URL]

---

**Última Atualização:** 31/01/2026
**Versão:** 1.0.0
