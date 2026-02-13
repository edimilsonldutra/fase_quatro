# RFC-001: Migração de Monolito para Arquitetura de Microserviços

## Metadata
- **Status**: Aprovado
- **Data de Criação**: Janeiro 2026
- **Autor**: Grupo 99
- **Revisores**: Equipe de Arquitetura, Product Owners
- **Decisão Final**: Janeiro 2026

## Resumo Executivo

Proposta de migração da aplicação monolítica `tech_fiap3` (Oficina Service) para uma arquitetura de microserviços composta por 3 serviços independentes: **OS Service**, **Billing Service** e **Execution Service**.

## Contexto e Problema

### Aplicação Atual (Monolito)

O sistema atual (`tech_fiap3`) é uma aplicação Spring Boot monolítica que gerencia todo o ciclo de vida de uma oficina mecânica:
- Ordens de Serviço
- Orçamentos e Pagamentos
- Execução e Diagnósticos
- Veículos e Clientes

**Desafios Identificados:**

1. **Escalabilidade Limitada**
   - Não é possível escalar componentes individualmente
   - Picos de acesso em orçamentos afetam todo o sistema

2. **Deploy Acoplado**
   - Alteração em módulo de pagamentos requer deploy completo
   - Downtime afeta todas as funcionalidades
   - Rollback complexo e arriscado

3. **Manutenibilidade**
   - Código crescente (>50 classes em um único repo)
   - Dependências transversais dificultam mudanças
   - Testes end-to-end lentos (>5 minutos)

4. **Tecnologia Única**
   - Forçado a usar PostgreSQL para todos os dados
   - Não aproveita NoSQL para dados flexíveis (orçamentos)

5. **Desenvolvimento em Equipe**
   - Múltiplos desenvolvedores trabalhando no mesmo código
   - Conflitos de merge frequentes
   - Dificuldade em trabalhar paralelamente

## Proposta: Arquitetura de Microserviços

### Decomposição de Domínios

Baseado em **Domain-Driven Design (DDD)**, identificamos 3 **Bounded Contexts**:

#### 1. OS Service (Ordem de Serviço)
**Responsabilidade**: Gerenciar ciclo de vida das ordens de serviço

**Entidades:**
- Ordem de Serviço
- Veículo
- Cliente
- Histórico de Status

**APIs:**
- `POST /api/ordens` - Criar OS
- `GET /api/ordens/{id}` - Consultar OS
- `PUT /api/ordens/{id}/status` - Atualizar status

**Banco de Dados**: PostgreSQL (dados relacionais)

#### 2. Billing Service (Faturamento)
**Responsabilidade**: Orçamentos, pagamentos e histórico financeiro

**Entidades:**
- Orçamento
- Item de Orçamento
- Pagamento
- Histórico de Status

**APIs:**
- `POST /api/v1/orcamentos` - Criar orçamento
- `PUT /api/v1/orcamentos/{id}/aprovar` - Aprovar orçamento
- `POST /api/v1/pagamentos` - Processar pagamento

**Banco de Dados**: DynamoDB (schema flexível, Free Tier AWS)

#### 3. Execution Service (Execução)
**Responsabilidade**: Diagnósticos, tarefas, peças e progresso

**Entidades:**
- Execução de OS
- Diagnóstico
- Tarefa
- Uso de Peças

**APIs:**
- `POST /api/v1/execucoes` - Criar execução
- `POST /api/v1/execucoes/{id}/diagnostico` - Adicionar diagnóstico
- `POST /api/v1/execucoes/{id}/tarefas` - Registrar tarefa

**Banco de Dados**: PostgreSQL (queries analíticas complexas)

### Comunicação entre Serviços

- **Síncrona**: REST APIs para consultas diretas
- **Assíncrona**: Apache Kafka para eventos (Saga) + SQS (auxiliares)
- **Tópicos/Filas**:
  - `os-events-queue`
  - `billing-events-queue`
  - `execution-events-queue`

### Infraestrutura

| Componente | Tecnologia |
|------------|------------|
| **Orquestração** | Amazon EKS (Kubernetes 1.29) |
| **Bancos de Dados** | RDS PostgreSQL 16.3 (OS, Execution), DynamoDB (Billing) |
| **Mensageria** | Apache Kafka (Saga) + Amazon SQS (auxiliares) |
| **Autenticação** | AWS Lambda (compartilhada) |
| **CI/CD** | GitHub Actions (pipelines independentes) |
| **Observabilidade** | New Relic APM + Distributed Tracing |

## Benefícios Esperados

### 1. Escalabilidade Independente ✅

```yaml
# OS Service - Alta demanda
replicas: 5
resources:
  cpu: 500m
  memory: 512Mi

# Billing Service - Baixa demanda
replicas: 2
resources:
  cpu: 100m
  memory: 256Mi
```

**Economia estimada**: 30% de custos de infra ao não escalar serviços de baixo uso

### 2. Deploy Independente ✅

| Cenário | Monolito | Microserviços |
|---------|----------|---------------|
| Alteração em Billing | Deploy completo (5 min downtime) | Deploy apenas Billing (sem downtime) |
| Rollback | Rollback completo | Rollback apenas serviço afetado |
| Hotfix em produção | Alto risco | Risco isolado |

### 3. Tecnologias Adequadas ✅

- **PostgreSQL** para OS e Execution (relacional)
- **DynamoDB** para Billing (flexibilidade de schema)
- Cada equipe escolhe ferramentas mais adequadas

### 4. Desenvolvimento Paralelo ✅

- 3 equipes trabalhando simultaneamente
- Repositórios independentes
- Menos conflitos de merge
- Pipelines de CI/CD isolados

### 5. Resiliência ✅

- Falha em Billing não afeta criação de OS
- Circuit breaker entre serviços
- Mensagens em fila garantem eventual consistency

### 6. Observabilidade ✅

- Distributed tracing entre serviços
- Dashboards específicos por microserviço
- Alertas granulares

## Desafios e Mitigações

### 1. Complexidade Operacional

| Desafio | Mitigação |
|---------|-----------|
| Gerenciar 3 serviços | Terraform + Kubernetes automation |
| Múltiplos bancos | Scripts automatizados de backup |
| Debugging distribuído | New Relic Distributed Tracing |
| Mais logs | Centralização no New Relic Logs |

### 2. Consistência de Dados

| Desafio | Mitigação |
|---------|-----------|
| Transações distribuídas | Saga Pattern com compensação |
| Eventual consistency | Idempotência nos consumers |
| Mensagens duplicadas | Deduplicação via message ID |

### 3. Latência de Rede

| Desafio | Mitigação |
|---------|-----------|
| Chamadas cross-service | Cache de dados frequentes |
| Kafka overhead | Consumer group rebalancing |
| Distributed tracing overhead | Sampling de 10% se necessário |

### 4. Curva de Aprendizado

| Desafio | Mitigação |
|---------|-----------|
| Equipe sem experiência em microserviços | Treinamentos e workshops |
| DevOps skills necessárias | Documentação detalhada + pair programming |
| Troubleshooting complexo | Runbooks e playbooks |

## Estratégia de Migração

### Fase 1: Preparação (Semana 1-2)
- ✅ Criar repositórios dos microserviços
- ✅ Provisionar infraestrutura (Terraform)
- ✅ Configurar CI/CD pipelines
- ✅ Implementar Lambda Auth (compartilhada)

### Fase 2: Extração de Domínios (Semana 3-6)
- ✅ Extrair código de OS Service do monolito
- ✅ Extrair código de Billing Service
- ✅ Extrair código de Execution Service
- ✅ Migrar schemas de banco de dados

### Fase 3: Integração (Semana 7-8)
- ✅ Implementar comunicação via Apache Kafka (Saga)
- ✅ Configurar New Relic Distributed Tracing
- ✅ Testes de integração end-to-end
- ✅ Load testing

### Fase 4: Deploy e Monitoramento (Semana 9-10)
- ✅ Deploy em ambiente de staging
- ✅ Validação com testes automatizados
- ✅ Deploy em produção (blue-green)
- ✅ Monitoramento 24/7 por 1 semana

### Rollback Plan

Se problemas críticos forem identificados:
1. Reverter ALB para apontar para monolito
2. Manter monolito em standby por 2 semanas
3. Após estabilização, desligar monolito

## Métricas de Sucesso

| Métrica | Baseline (Monolito) | Meta (Microserviços) |
|---------|---------------------|----------------------|
| **Latência P95** | 2.5s | < 2.0s |
| **Deploy Frequency** | 1x/semana | 5x/semana |
| **Deploy Downtime** | 5 minutos | 0 minutos (rolling update) |
| **MTTR** | 2 horas | < 30 minutos |
| **Taxa de Erro** | 2% | < 1% |
| **Throughput** | 500 req/min | 1000+ req/min |

## Custos Estimados

### Infraestrutura AWS (Mensal)

| Componente | Monolito | Microserviços | Diferença |
|------------|----------|---------------|-----------|
| **EKS** | $73 | $73 | $0 |
| **EC2 Nodes** | $150 (3 nodes) | $200 (4 nodes) | +$50 |
| **RDS PostgreSQL** | $100 (1 instância) | $200 (2 instâncias) | +$100 |
| **DynamoDB** | - | $80 | +$80 |
| **Kafka (MSK)** | - | $15 | +$15 |
| **New Relic** | $100 | $150 | +$50 |
| **Total** | **$423** | **$713** | **+$290** |

**ROI Esperado:**
- Redução de downtime: $500/mês
- Velocidade de desenvolvimento: $1000/mês
- **ROI positivo em 2 meses**

## Alternativas Consideradas

### 1. Manter Monolito com Otimizações
- **Prós**: Menor complexidade, sem custo adicional
- **Contras**: Não resolve escalabilidade, deploy acoplado
- **Decisão**: Rejeitado - não atende crescimento futuro

### 2. Serverless Completo (Lambda Functions)
- **Prós**: Zero gerenciamento de infra, pay-per-use
- **Contras**: Cold start, vendor lock-in, limite de 15min
- **Decisão**: Rejeitado - alto acoplamento com AWS

### 3. Modular Monolith
- **Prós**: Meio termo, menor complexidade
- **Contras**: Ainda deploy acoplado, escalabilidade limitada
- **Decisão**: Rejeitado - não resolve problemas principais

## Decisão

✅ **APROVADO** - Migrar para arquitetura de microserviços

**Justificativa:**
- Benefícios superam desafios
- ROI positivo em 2 meses
- Escalabilidade atende crescimento projetado
- Tecnologia mais adequada por domínio
- Desenvolvimento paralelo acelera entregas

## Próximos Passos

1. ✅ Comunicar decisão para todas as equipes
2. ✅ Iniciar Fase 1 (Preparação)
3. ✅ Criar documentação técnica detalhada
4. ✅ Agendar treinamentos de microserviços
5. ✅ Definir ownership de cada microserviço

## Referências

- [Microservices Pattern](https://microservices.io/)
- [Domain-Driven Design](https://martinfowler.com/tags/domain%20driven%20design.html)
- [AWS Microservices Best Practices](https://aws.amazon.com/microservices/)
- [Kubernetes Best Practices](https://kubernetes.io/docs/concepts/cluster-administration/manage-deployment/)

## Revisão e Feedback

Esta RFC está aberta para comentários até **15/01/2026**.  
Após aprovação, será convertida em decisão arquitetural permanente (ADR).

---

**Autor**: Grupo 99  
**Data de Criação**: Janeiro 2026  
**Última Atualização**: Janeiro 2026  
**Status**: Aprovado e Em Implementação ✅
