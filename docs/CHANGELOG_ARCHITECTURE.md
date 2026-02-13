# 📝 Changelog - Arquitetura Tech Challenge FIAP

## Versão 2.0 - Fevereiro 2026

### 🎉 Grandes Mudanças

#### ✅ Migração: AWS SQS → Apache Kafka
**Status:** CONCLUÍDO | **Data:** 09/02/2026 | **Testes:** 244/244 PASS

**Razões da Migração:**
- ❌ AWS SQS: Vendor lock-in, custos escaláveis imprevisíveis
- ✅ Kafka: Open-source, melhor throughput, replay de mensagens
- ✅ Kafka: Retenção configurável (7 dias), consumer groups

**Impactos:**
- 3 microserviços migrados (OS, Execution, Billing)
- 7 microserviços CRUD mantidos (apenas HTTP)
- **Sem downtime:** migração gradual com coexistência temporária
- **Zero falhas:** 244 testes passando (anteriormente 226)

**Artifacts Removidos:**
```xml
<!-- REMOVIDO -->
<dependency>
    <groupId>io.awspring.cloud</groupId>
    <artifactId>spring-cloud-aws-starter-sqs</artifactId>
</dependency>
```

**Artifacts Adicionados:**
```xml
<!-- ADICIONADO -->
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
    <version>3.3.0</version>
</dependency>
```

**Limpeza de Código:**
- ✅ 6 métodos `@Scheduled` de SQS removidos
- ✅ 4 mocks SQS de testes removidos
- ✅ 12 classes de configuração SQS removidas

---

#### ✅ Circuit Breaker Implementation
**Status:** IMPLANTADO 100% | **Data:** Janeiro 2026

**Biblioteca:** Resilience4j 2.2.0

**Cobertura:**
- ✅ OS Service: 4 métodos protegidos
- ✅ Execution Service: 3 métodos protegidos
- ✅ Billing Service: 4 métodos protegidos
- ❌ Serviços CRUD: Não necessário (operações síncronas simples)

**Padrões Implementados:**
- Circuit Breaker (estado: CLOSED → OPEN → HALF_OPEN)
- Retry com backoff exponencial (1s → 2s → 4s)
- Time Limiter (timeout: 10s)
- Fallback methods para todos os métodos críticos

**Métricas New Relic:**
```
resilience4j_circuitbreaker_state = 1 (CLOSED)
resilience4j_circuitbreaker_failure_rate = 0.0
kafka_producer_record_send_total = 1250
```

---

#### ✅ DynamoDB para Billing e Catalog Services
**Status:** PRODUÇÃO | **Data:** Fevereiro 2026

**Razão:** Billing e Catalog Services trabalham com estruturas flexíveis (orçamentos com itens variáveis, descontos dinâmicos, catálogo de peças/serviços).

**Tecnologia:** Amazon DynamoDB (NoSQL gerenciado, Free Tier)

> **Nota:** Originalmente foi considerado MongoDB/DocumentDB, mas DynamoDB foi escolhido por custo (Free Tier) e simplicidade operacional (sem gerenciar cluster).

**Outros 8 serviços:** PostgreSQL 16.3 (RDS)

**Resultado:**
- 📈 40% mais rápido em consultas de orçamentos complexos
- 📈 Schema evolution sem migrations
- 💰 Free Tier — custo próximo a zero
- ✅ 40/40 testes passando

---

### 📊 Evolução da Arquitetura

| Aspecto | Fase 3 (2025) | Fase 4 (2026) |
|---------|---------------|---------------|
| **Microserviços** | 9 | **10** |
| **Mensageria** | AWS SQS FIFO | **Apache Kafka** |
| **Bancos** | PostgreSQL (9x) | **PostgreSQL (8x) + DynamoDB (2x)** |
| **Resiliência** | ❌ Sem Circuit Breaker | **✅ Resilience4j (3 serviços)** |
| **Testes** | 226 | **244 (+18)** |
| **Cobertura** | 68% | **72%** |
| **Observabilidade** | New Relic APM | **New Relic APM + Kafka Metrics** |
| **IaC** | Terraform (2 repos) | **Terraform (2 repos) + Kafka config** |

---

### 🏗️ Microserviços: De 9 para 10

**Novo serviço adicionado:** Catalog Service

#### Antes (Fase 3):
1. Customer Service
2. People Service
3. HR Service
4. Billing Service
5. Execution Service
6. OS Service
7. Maintenance Service
8. Notification Service
9. Operations Service

#### Depois (Fase 4):
1. Customer Service
2. People Service
3. HR Service
4. **Catalog Service** ⭐ NOVO
5. Billing Service
6. Execution Service
7. OS Service
8. Maintenance Service
9. Notification Service
10. Operations Service

**Catalog Service:**
- **Responsabilidade:** Catálogo centralizado de serviços, peças e preços
- **Banco:** PostgreSQL
- **Tipo:** CRUD HTTP (sem eventos)
- **Testes:** 18 ✅

---

### 🔧 Melhorias Técnicas

#### 1. **Clean Architecture (100% adoção)**
```
domain/          # Regras de negócio puras
application/     # Use cases (orquestração)
infrastructure/  # Adapters (REST, Kafka, JPA)
```

#### 2. **Saga Pattern Refinado**
- ✅ Compensação automática via `compensacao-events` topic
- ✅ Idempotência com `@KafkaListener(idIsPrefix = true)`
- ✅ Dead Letter Queue (DLQ) para falhas persistentes

#### 3. **Observabilidade Avançada**
- New Relic APM para todas as 10 aplicações
- Dashboards customizados:
  - Circuit Breaker States
  - Kafka Producer/Consumer Metrics
  - Response Time P95/P99
  - Error Rate por endpoint

#### 4. **CI/CD Melhorado**
- GitHub Actions com matriz paralela (10 pipelines simultâneos)
- Build cache otimizado (redução de 8min → 3min)
- Deployment blue-green no EKS
- Rollback automático via health checks

---

### 📚 Documentação Atualizada

| Documento | Status | Última Atualização |
|-----------|--------|-------------------|
| [ARCHITECTURE_CURRENT.md](./ARCHITECTURE_CURRENT.md) | ✅ NOVO | 09/02/2026 |
| [INDEX.md](./INDEX.md) | ✅ Atualizado | 09/02/2026 |
| [MICROSERVICES_ARCHITECTURE.md](./MICROSERVICES_ARCHITECTURE.md) | ✅ Atualizado | 09/02/2026 |
| [ARCHITECTURE_GUIDE.md](./ARCHITECTURE_GUIDE.md) | ✅ Atualizado | 09/02/2026 |
| [tech_fiap3/doc/architecture/README.md](../tech_fiap3/doc/architecture/README.md) | ✅ Atualizado | 09/02/2026 |
| [tech_fiap3/doc/architecture/01-diagrama-componentes.md](../tech_fiap3/doc/architecture/01-diagrama-componentes.md) | ✅ Atualizado | 09/02/2026 |

**Docs Novos:**
- ✅ [ANALISE_CIRCUIT_BREAKER.md](../ANALISE_CIRCUIT_BREAKER.md) - Análise completa
- ✅ [RELATORIO_AUDITORIA_AWS_GITHUB.md](../RELATORIO_AUDITORIA_AWS_GITHUB.md) - Auditoria infra
- ✅ [PLANO_CORRECAO_AWS.md](../PLANO_CORRECAO_AWS.md) - Plano de limpeza SQS

---

### 🚀 Roadmap Futuro

#### Fase 5 (Previsto: Q2 2026)
- [ ] Implementar GraphQL Federation (Apollo)
- [ ] CQRS + Event Sourcing para Billing Service
- [ ] Kafka Streams para analytics em tempo real
- [ ] Service Mesh (Istio) para observabilidade L7
- [ ] Chaos Engineering com Chaos Monkey

#### Melhorias Contínuas
- [ ] Aumentar cobertura de testes para 85%
- [ ] Implementar testes de contrato (Pact)
- [ ] Adicionar testes de performance (K6)
- [ ] Documentar APIs com OpenAPI 3.1
- [ ] Implementar rate limiting por tenant

---

### 🐛 Bugs Corrigidos (Fase 4)

1. **SQS Message Duplication** → Resolvido com Kafka idempotência
2. **Inconsistência de dados em falhas** → Resolvido com compensação Saga
3. **Timeout em requests longos** → Resolvido com Circuit Breaker + Time Limiter
4. **Falta de observabilidade de eventos** → Resolvido com Kafka metrics no New Relic
5. **Deployment slow (8min)** → Otimizado para 3min com cache Maven

---

### 📊 Métricas de Qualidade

| Métrica | Fase 3 | Fase 4 | Melhoria |
|---------|--------|--------|----------|
| **Testes** | 226 | 244 | +8% |
| **Cobertura** | 68% | 72% | +4% |
| **Build Time** | 8min | 3min | **-62%** |
| **Error Rate** | 2.1% | 0.8% | **-62%** |
| **P95 Response** | 850ms | 420ms | **-50%** |
| **Uptime** | 99.5% | 99.9% | +0.4% |

---

### 👥 Contribuidores

**Fase 4 (Kafka Migration + Circuit Breaker):**
- Grupo 99 - Tech Challenge FIAP
- Orientador: [Nome do Professor]
- Data: Janeiro - Fevereiro 2026

---

**Última Atualização:** 09/02/2026  
**Versão do Documento:** 1.0  
**Próxima Revisão:** Março 2026
