# 📚 Referências Técnicas Complementares

**Versão:** 1.0  
**Data:** 09/02/2026  
**Propósito:** Consolidar informações técnicas importantes que complementam a documentação de arquitetura oficial

---

## 📖 Sobre Este Documento

Este arquivo consolida **informações técnicas complementares** dos documentos:
- Configurações de ferramentas (New Relic, Swagger)
- Validações técnicas (Clean Architecture, Cobertura)
- Detalhes de implementação (Saga Rollback, Pipelines CI/CD)
- Sumários de migração

**IMPORTANTE:** Para documentação de arquitetura oficial, consulte:
- [QUICK_START_ARCHITECTURE.md](./QUICK_START_ARCHITECTURE.md)
- [ARCHITECTURE_CURRENT.md](./ARCHITECTURE_CURRENT.md)
- [INDEX.md](./INDEX.md)

---

## 📊 Índice

1. [Saga Pattern - Rollback e Compensação](#saga-rollback)
2. [Validação Clean Architecture](#clean-architecture)
3. [Pipelines CI/CD](#cicd-pipelines)
4. [Configuração New Relic](#new-relic)
5. [Cobertura de Testes](#cobertura)
6. [Documentação Swagger/OpenAPI](#swagger)
7. [Organização de Infraestrutura](#infra-org)
8. [Sumário de Migração Kafka](#migration)

---

<a name="saga-rollback"></a>
## 🔄 Saga Pattern - Rollback e Compensação

**Status:** ✅ IMPLEMENTADO

### Eventos de Compensação

#### OS Service
- **OSCanceladaEvent** - Cancela toda a saga
  - Quando: Falha crítica ou cancelamento manual
  - Propaga para: Billing Service, Execution Service

#### Billing Service
- **OrcamentoRejeitadoEvent** - Rejeição de orçamento
  - Quando: Cliente rejeita ou falha no billing
  - Propaga para: OS Service, Execution Service

- **PagamentoFalhouEvent** - Falha no pagamento
  - Quando: Gateway retorna erro
  - Propaga para: OS Service, Execution Service

#### Execution Service
- **ExecucaoFalhouEvent** - Falha na execução
  - Quando: Erro ao executar serviço
  - Propaga para: OS Service, Billing Service

### Fluxos de Compensação

**Cenário 1: Falha ao Atualizar Status da OS**
```
Billing: Orçamento aprovado → ORCAMENTO_APROVADO
OS Service: ❌ ERRO ao atualizar → Publicar OS_CANCELADA
Billing: Recebe OS_CANCELADA → Cancelar orçamento ✅
Execution: Recebe OS_CANCELADA → Cancelar execução ✅
```

**Cenário 2: Falha no Pagamento**
```
Billing: Gateway falha → Publicar PAGAMENTO_FALHOU
OS Service: Atualiza status → AGUARDANDO_PAGAMENTO
Execution: Pausa execução até novo pagamento
```

**Cenário 3: Falha na Execução**
```
Execution: Erro técnico → Publicar EXECUCAO_FALHOU
OS Service: Atualiza status → CANCELADA
Billing: Emite estorno/cancelamento de orçamento
```

### Garantias de Consistência
- ✅ Idempotência: Eventos podem ser processados múltiplas vezes
- ✅ Retry automático: 3 tentativas com backoff exponencial
- ✅ Dead Letter Queue (DLQ): Eventos com falha persistente
- ✅ Auditoria: Todos os eventos compensatórios são logados

---

<a name="clean-architecture"></a>
## 🏛️ Validação Clean Architecture

**Status:** ✅ VALIDADO (01/02/2026)

### Estrutura de Camadas

```
microservice/
├── domain/              ← Lógica de negócio pura
│   ├── model/          (Entities, Value Objects)
│   ├── repository/     (Interfaces)
│   └── events/         (Domain Events)
│
├── application/        ← Casos de uso
│   ├── service/       (Application Services)
│   ├── dto/           (DTOs)
│   └── event/         (Event Handlers)
│
└── infrastructure/     ← Implementação técnica
    ├── persistence/   (JPA Repositories)
    ├── messaging/     (Kafka Publishers/Listeners)
    └── config/        (Spring Configuration)
```

### Princípios Validados

✅ **Dependency Inversion**
- Domain não conhece Infrastructure
- Application conhece Domain (via interfaces)
- Infrastructure implementa interfaces do Domain

✅ **Single Responsibility**
- Cada camada tem responsabilidade clara
- Separação entre lógica de negócio e infraestrutura

✅ **Interface Segregation**
- Ports específicos por contexto
- Contratos mínimos e focados

### Conformidade por Serviço

| Serviço | Domain | Application | Infrastructure | Status |
|---------|--------|-------------|----------------|--------|
| OS Service | ✅ | ✅ | ✅ | 100% |
| Billing Service | ✅ | ✅ | ✅ | 100% |
| Execution Service | ✅ | ✅ | ✅ | 100% |
| 7 CRUD Services | ✅ | ✅ | ✅ | 100% |

### Testes por Camada

**Domain (Testes Unitários):**
- Sem dependências externas
- Mocks apenas para interfaces do próprio domain
- Cobertura: 85%+

**Application (Testes Integração):**
- Mocks de repositories
- Validação de orquestração
- Cobertura: 75%+

**Infrastructure (Testes E2E):**
- Banco H2 in-memory
- Kafka Embedded
- Cobertura: 60%+

---

<a name="cicd-pipelines"></a>
## 🔄 Pipelines CI/CD

### Estrutura dos Pipelines

**Jobs:**
1. **Test** - Testes unitários e integração
2. **Build** - Build + Docker push para ECR
3. **Deploy** - Deploy no EKS
4. **Rollback** - Rollback automático em falha

**Fluxo:**
```
Test → Build → Deploy
                 ↓ (se falhar)
             Rollback
```

### Triggers
- Push nas branches `master` ou `main`
- Workflow Dispatch manual

### Variáveis Comuns

```yaml
AWS_REGION: us-east-1
EKS_CLUSTER_NAME: tech-challenge-cluster
JAVA_VERSION: '21'
K8S_INFRA_REPO: 'tech_challenge_k8s_infra'
```

### Variáveis por Serviço

| Serviço | ECR_REPOSITORY | SERVICE_NAME |
|---------|----------------|--------------|
| OS Service | os-service | os-service |
| Execution Service | execution-service | execution-service |
| Billing Service | billing-service | billing-service |
| People Service | people-service | people-service |
| Customer Service | customer-service | customer-service |
| HR Service | hr-service | hr-service |
| Catalog Service | catalog-service | catalog-service |
| Operations Service | operations-service | operations-service |
| Maintenance Service | maintenance-service | maintenance-service |
| Notification Service | notification-service | notification-service |

### Secrets Necessários

```yaml
AWS_ACCESS_KEY_ID       # Credencial AWS
AWS_SECRET_ACCESS_KEY   # Credencial AWS
NEW_RELIC_LICENSE_KEY   # New Relic APM
```

### Health Checks

**Liveness Probe:**
```yaml
httpGet:
  path: /actuator/health/liveness
  port: 8080
initialDelaySeconds: 30
periodSeconds: 10
```

**Readiness Probe:**
```yaml
httpGet:
  path: /actuator/health/readiness
  port: 8080
initialDelaySeconds: 15
periodSeconds: 5
```

---

<a name="new-relic"></a>
## 📊 Configuração New Relic

### Dependência Maven

```xml
<dependency>
    <groupId>com.newrelic.agent.java</groupId>
    <artifactId>newrelic-api</artifactId>
    <version>8.8.0</version>
</dependency>
```

### Configuração por Serviço

**newrelic.yml Template:**
```yaml
common: &default_settings
  license_key: '<%= ENV['NEW_RELIC_LICENSE_KEY'] %>'
  app_name: '{SERVICE_NAME} - ${NEW_RELIC_ENVIRONMENT:dev}'
  
  labels:
    Service: {service-name}
    Team: Grupo99
    Environment: ${NEW_RELIC_ENVIRONMENT:dev}
  
  distributed_tracing:
    enabled: true
  
  application_logging:
    forwarding:
      enabled: true
    metrics:
      enabled: true
```

### Apps no New Relic

| Serviço | App Name | Labels |
|---------|----------|--------|
| OS Service | OS Service - dev | Service: os-service |
| Execution Service | Execution Service - dev | Service: execution-service |
| Billing Service | Billing Service - dev | Service: billing-service |
| People Service | People Service - dev | Service: people-service |
| Customer Service | Customer Service - dev | Service: customer-service |
| HR Service | HR Service - dev | Service: hr-service |
| Catalog Service | Catalog Service - dev | Service: catalog-service |
| Operations Service | Operations Service - dev | Service: operations-service |
| Maintenance Service | Maintenance Service - dev | Service: maintenance-service |
| Notification Service | Notification Service - dev | Service: notification-service |

### Dockerfile Integration

```dockerfile
# Baixar New Relic Agent
RUN curl -L -o newrelic-java.zip https://download.newrelic.com/newrelic/java-agent/newrelic-agent/current/newrelic-java.zip && \
    unzip newrelic-java.zip && \
    rm newrelic-java.zip

# Copiar configuração
COPY src/main/resources/newrelic.yml /app/newrelic/newrelic.yml

# Executar com agent
ENTRYPOINT ["java", "-javaagent:/app/newrelic/newrelic.jar", "-jar", "app.jar"]
```

### Dashboards Disponíveis

1. **Application Performance**
   - Response time (P50, P95, P99)
   - Throughput (req/min)
   - Error rate (%)
   - Apdex score

2. **Distributed Tracing**
   - Trace completa das Sagas
   - Latency por serviço
   - Dependências entre serviços

3. **Kafka Metrics**
   - Producer send rate
   - Consumer lag
   - Topic throughput

4. **Circuit Breaker**
   - Estado atual (OPEN/CLOSED/HALF_OPEN)
   - Failure rate
   - Calls blocked

---

<a name="cobertura"></a>
## 📈 Cobertura de Testes

### Resumo Global

| Métrica | Valor | Meta |
|---------|-------|------|
| **Testes Totais** | 244 | - |
| **Passando** | 244 (100%) | 100% |
| **Cobertura Média** | 72% | 80% |
| **Cobertura Domain** | 85% | 90% |
| **Cobertura Application** | 75% | 80% |
| **Cobertura Infrastructure** | 60% | 70% |

### Por Microserviço

| Serviço | Testes | Cobertura | Status |
|---------|--------|-----------|--------|
| OS Service | 48 | 85% | ✅ |
| Execution Service | 47 | 78% | ✅ |
| Billing Service | 40 | 72% | ✅ |
| People Service | 23 | 65% | ⚠️ |
| Customer Service | 21 | 68% | ⚠️ |
| HR Service | 19 | 62% | ⚠️ |
| Catalog Service | 18 | 70% | ✅ |
| Operations Service | 12 | 58% | ⚠️ |
| Maintenance Service | 8 | 55% | ⚠️ |
| Notification Service | 8 | 60% | ⚠️ |

### Tipos de Testes

**Unitários (180 testes):**
- Domain layer
- Application services (sem I/O)
- Cobertura: 85%

**Integração (50 testes):**
- Repository layer
- Kafka integration
- Cobertura: 70%

**BDD - Cucumber (14 testes):**
- Cenários de negócio end-to-end
- Cobertura: 60%

### Ferramentas

- **JUnit 5** - Framework de testes
- **Mockito** - Mocks
- **JaCoCo** - Cobertura
- **Cucumber** - BDD
- **TestContainers** - Kafka/PostgreSQL/DynamoDB Local

### Comandos

```bash
# Rodar todos os testes
mvn clean test

# Gerar relatório de cobertura
mvn jacoco:report

# Ver relatório
open target/site/jacoco/index.html
```

---

<a name="swagger"></a>
## 📖 Documentação Swagger/OpenAPI

### Dependência Spring Doc

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.3.0</version>
</dependency>
```

### URLs de Acesso

| Serviço | Swagger UI | OpenAPI JSON |
|---------|------------|--------------|
| OS Service | http://localhost:8081/swagger-ui.html | http://localhost:8081/v3/api-docs |
| Execution Service | http://localhost:8082/swagger-ui.html | http://localhost:8082/v3/api-docs |
| Billing Service | http://localhost:8083/swagger-ui.html | http://localhost:8083/v3/api-docs |
| People Service | http://localhost:8084/swagger-ui.html | http://localhost:8084/v3/api-docs |
| Customer Service | http://localhost:8085/swagger-ui.html | http://localhost:8085/v3/api-docs |
| HR Service | http://localhost:8086/swagger-ui.html | http://localhost:8086/v3/api-docs |
| Catalog Service | http://localhost:8087/swagger-ui.html | http://localhost:8087/v3/api-docs |
| Operations Service | http://localhost:8088/swagger-ui.html | http://localhost:8088/v3/api-docs |
| Maintenance Service | http://localhost:8089/swagger-ui.html | http://localhost:8089/v3/api-docs |
| Notification Service | http://localhost:8090/swagger-ui.html | http://localhost:8090/v3/api-docs |

### Exemplo de Anotações

```java
@RestController
@RequestMapping("/api/v1/ordens-servico")
@Tag(name = "Ordens de Serviço", description = "Gerenciamento de ordens de serviço")
public class OrdemServicoController {

    @PostMapping
    @Operation(summary = "Criar nova OS", description = "Cria uma nova ordem de serviço")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "OS criada com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "500", description = "Erro interno")
    })
    public ResponseEntity<OSResponse> criar(
        @RequestBody @Valid OSRequest request
    ) {
        // implementação
    }
}
```

### Configuração

```yaml
springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
    operationsSorter: method
```

---

<a name="infra-org"></a>
## 🏗️ Organização de Infraestrutura

### Repositórios Terraform

**1. tech_challenge_db_infra**
- PostgreSQL RDS (9 instâncias)
- DynamoDB (Billing + Catalog)
- Security Groups
- SSM Parameter Store (secrets)

**2. tech_challenge_k8s_infra**
- Amazon EKS cluster
- VPC e Networking
- Kafka cluster (MSK)
- New Relic integration
- Dashboards

### Estrutura de Ambientes

```
tech_challenge_k8s_infra/
├── base/                    # Recursos compartilhados
│   ├── kafka/              # Tópicos Kafka
│   ├── monitoring/         # New Relic
│   └── networking/         # VPC, Subnets
│
├── microservices/          # Deployments K8s
│   ├── os-service/
│   ├── billing-service/
│   └── ...
│
└── environments/           # Configurações por ambiente
    ├── dev/
    ├── staging/
    └── prod/
```

### Tópicos Kafka

```yaml
Topics:
  os-events:
    partitions: 3
    replication-factor: 3
    retention.ms: 604800000  # 7 dias
    
  billing-events:
    partitions: 3
    replication-factor: 3
    
  execution-events:
    partitions: 3
    replication-factor: 3
    
  compensacao-events:
    partitions: 3
    replication-factor: 3
```

### Bancos de Dados

**PostgreSQL RDS:**
- 9 instâncias (uma por serviço)
- Engine: PostgreSQL 16.3
- Instance: db.t3.micro (dev), db.t3.medium (prod)
- Multi-AZ: Sim (prod)
- Backup: 7 dias

**Amazon DynamoDB:**
- Tabela: billing-service-table
- Tipo: On-Demand (Free Tier)
- Chave primária: id (partition key)
- Sem gerenciamento de cluster
- Backup: Point-in-time recovery

---

<a name="migration"></a>
## 🔄 Sumário de Migração Kafka

### Resumo da Migração

**Data:** Fevereiro 2026  
**Duração:** 3 semanas  
**Status:** ✅ COMPLETO

**De:** AWS SQS FIFO Queues  
**Para:** Apache Kafka 3.7.2

### Razões da Migração

1. **Vendor Lock-in:** SQS é específico da AWS
2. **Custo:** Kafka mais econômico em alto volume
3. **Features:** Replay de mensagens, retenção configurável
4. **Performance:** Melhor throughput
5. **Comunidade:** Mais ferramentas e integrações

### Estratégia de Migração

**Fase 1: Preparação (1 semana)**
- ✅ Setup Kafka cluster (MSK)
- ✅ Configuração de tópicos
- ✅ Testes de conectividade

**Fase 2: Coexistência (1 semana)**
- ✅ Publicar em SQS + Kafka (dual write)
- ✅ Consumir apenas de SQS
- ✅ Validar mensagens no Kafka

**Fase 3: Migração (1 semana)**
- ✅ Consumir de Kafka
- ✅ Parar publicação no SQS
- ✅ Validação completa (244 testes)
- ✅ Monitoramento New Relic

### Mudanças no Código

**Dependências Removidas:**
```xml
<!-- REMOVIDO -->
<dependency>
    <groupId>io.awspring.cloud</groupId>
    <artifactId>spring-cloud-aws-starter-sqs</artifactId>
</dependency>
```

**Dependências Adicionadas:**
```xml
<!-- ADICIONADO -->
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
    <version>3.3.0</version>
</dependency>
```

**Configuração:**
```yaml
# SQS (REMOVIDO)
cloud:
  aws:
    sqs:
      endpoint: https://sqs.us-east-1.amazonaws.com

# Kafka (ADICIONADO)
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS}
    consumer:
      group-id: ${spring.application.name}
      auto-offset-reset: earliest
    producer:
      acks: all
      retries: 3
```

### Resultados

| Métrica | SQS | Kafka | Melhoria |
|---------|-----|-------|----------|
| **Latency P95** | 850ms | 420ms | -50% |
| **Throughput** | 100 msg/s | 1000 msg/s | +900% |
| **Custo/mês** | $150 | $80 | -47% |
| **Replay** | ❌ Não | ✅ Sim | - |
| **Retenção** | 14 dias | 7 dias (configurável) | - |

### Lições Aprendidas

1. **Idempotência é crítica:** Kafka pode entregar mensagens duplicadas
2. **Monitoramento:** Consumer lag é métrica essencial
3. **Particionamento:** Usar chave de partição (osId) para ordem
4. **DLQ:** Implementar dead letter queue para erros
5. **Circuit Breaker:** Essencial para resiliência

---

## 🔗 Referências

### Documentação Oficial
- [Quick Start Architecture](./QUICK_START_ARCHITECTURE.md)
- [Architecture Current](./ARCHITECTURE_CURRENT.md)
- [Changelog Architecture](./CHANGELOG_ARCHITECTURE.md)
- [Index](./INDEX.md)

### Análises Técnicas
- [Histórico de Trabalho Técnico](../HISTORICO_TRABALHO_TECNICO.md) - Índice de 34 documentos

### Ferramentas
- [Spring Boot 3.3 Docs](https://docs.spring.io/spring-boot/docs/3.3.x/reference/html/)
- [Apache Kafka Docs](https://kafka.apache.org/documentation/)
- [New Relic Java APM](https://docs.newrelic.com/docs/apm/agents/java-agent/)
- [Resilience4j Docs](https://resilience4j.readme.io/)

---

**Documento criado em:** 09/02/2026  
**Última atualização:** 09/02/2026  
**Mantenedor:** Grupo 99 - Tech Challenge FIAP  
**Versão:** 1.0
