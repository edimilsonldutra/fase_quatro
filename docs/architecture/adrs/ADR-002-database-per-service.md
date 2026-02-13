# ADR-002: Database per Service Pattern com Múltiplas Tecnologias

## Status
**Implementado** - Fevereiro 2026

## Changelog
- **Janeiro 2026**: Decisão inicial aceita
- **Fevereiro 2026**: Saga Pattern implementado com eventos Kafka

## Contexto
Na arquitetura de microserviços, cada serviço precisa de persistência de dados. Precisamos decidir:
1. Usar um banco de dados compartilhado entre todos os serviços?
2. Um banco de dados por serviço (Database per Service)?
3. Que tecnologias de banco utilizar para cada serviço?

## Decisão
Adotamos o padrão **Database per Service** com tecnologias específicas para cada caso de uso:

| Microserviço | Banco de Dados | Justificativa |
|--------------|----------------|---------------|
| **OS Service** | PostgreSQL 16.3 | Dados relacionais estruturados, integridade referencial |
| **Billing Service** | DynamoDB | Flexibilidade de schema para orçamentos variáveis |
| **Execution Service** | PostgreSQL 16.3 | Dados relacionais, queries complexas para relatórios |
| **Lambda Auth** | PostgreSQL compartilhado | Acesso read-only à tabela `pessoas` |

## Consequências

### Positivas ✅
- **Independência**: Cada serviço escolhe a tecnologia mais adequada
- **Escalabilidade**: Bancos podem ser escalados independentemente
- **Isolamento de Falhas**: Problema em um banco não afeta outros serviços
- **Deploy Independente**: Mudanças de schema não impactam outros serviços
- **Otimização**: DynamoDB para documentos variáveis, PostgreSQL para relacional

### Negativas ❌
- **Complexidade Operacional**: Gerenciar múltiplos bancos de dados
- **Transações Distribuídas**: Não há ACID entre microserviços
- **Duplicação de Dados**: Alguma redundância é necessária
- **Custo**: Mais instâncias de banco de dados

### Riscos e Mitigações

| Risco | Mitigação |
|-------|-----------|
| Inconsistência de dados | Event Sourcing + idempotência |
| Joins entre serviços | Denormalização e queries via API |
| Backup e restore complexo | Scripts automatizados por serviço |
| Custo elevado | RDS shared instances + autoscaling |

## Detalhamento por Serviço

### OS Service - PostgreSQL

**Tabelas Principais:**
```sql
CREATE TABLE ordens_servico (
    id UUID PRIMARY KEY,
    cliente_id UUID NOT NULL,
    veiculo_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL,
    data_criacao TIMESTAMP NOT NULL,
    data_atualizacao TIMESTAMP,
    CONSTRAINT fk_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id),
    CONSTRAINT fk_veiculo FOREIGN KEY (veiculo_id) REFERENCES veiculos(id)
);

CREATE TABLE historico_status (
    id UUID PRIMARY KEY,
    ordem_servico_id UUID NOT NULL,
    status_anterior VARCHAR(50),
    status_novo VARCHAR(50) NOT NULL,
    data_mudanca TIMESTAMP NOT NULL,
    CONSTRAINT fk_os FOREIGN KEY (ordem_servico_id) REFERENCES ordens_servico(id)
);
```

**Justificativa:**
- Relacionamentos bem definidos (cliente, veículo, OS)
- Integridade referencial crítica
- Queries com JOINs complexos
- ACID necessário para mudanças de status

### Billing Service - DynamoDB

**Tables:**
```javascript
// orcamentos table
{
  "_id": "orcamento-uuid",
  "osId": "os-uuid",
  "status": "AGUARDANDO_APROVACAO",
  "itens": [
    {
      "tipo": "PECA",
      "descricao": "Pastilha de freio",
      "quantidade": 4,
      "valorUnitario": 50.00,
      "valorTotal": 200.00
    },
    {
      "tipo": "SERVICO",
      "descricao": "Troca de óleo",
      "horasEstimadas": 1.5,
      "valorHora": 80.00,
      "valorTotal": 120.00
    }
  ],
  "valorTotal": 320.00,
  "dataCriacao": ISODate("2026-01-20T10:00:00Z"),
  "dataAtualizacao": ISODate("2026-01-20T14:30:00Z")
}

// pagamentos table
{
  "_id": "pagamento-uuid",
  "orcamentoId": "orcamento-uuid",
  "osId": "os-uuid",
  "formaPagamento": "CARTAO_CREDITO",
  "parcelas": 3,
  "valorTotal": 320.00,
  "status": "CONFIRMADO",
  "transacaoId": "txn-12345",
  "dataPagamento": ISODate("2026-01-21T09:00:00Z")
}
```

**Justificativa:**
- Orçamentos têm estrutura variável (diferentes peças/serviços)
- Não há relacionamentos complexos com outras entidades
- Facilidade para adicionar novos campos sem migration
- Documentos auto-contidos facilitam queries

### Execution Service - PostgreSQL

**Tabelas Principais:**
```sql
CREATE TABLE execucoes_os (
    id UUID PRIMARY KEY,
    ordem_servico_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL,
    data_inicio TIMESTAMP NOT NULL,
    data_fim TIMESTAMP,
    mecanico_id UUID
);

CREATE TABLE diagnosticos (
    id UUID PRIMARY KEY,
    execucao_id UUID NOT NULL,
    descricao TEXT NOT NULL,
    observacoes TEXT,
    data_diagnostico TIMESTAMP NOT NULL,
    CONSTRAINT fk_execucao FOREIGN KEY (execucao_id) REFERENCES execucoes_os(id)
);

CREATE TABLE tarefas (
    id UUID PRIMARY KEY,
    execucao_id UUID NOT NULL,
    descricao VARCHAR(500) NOT NULL,
    status VARCHAR(50) NOT NULL,
    tempo_estimado_minutos INTEGER,
    tempo_real_minutos INTEGER,
    CONSTRAINT fk_execucao FOREIGN KEY (execucao_id) REFERENCES execucoes_os(id)
);

CREATE TABLE uso_pecas (
    id UUID PRIMARY KEY,
    tarefa_id UUID NOT NULL,
    peca_codigo VARCHAR(100) NOT NULL,
    quantidade INTEGER NOT NULL,
    CONSTRAINT fk_tarefa FOREIGN KEY (tarefa_id) REFERENCES tarefas(id)
);
```

**Justificativa:**
- Relacionamentos fortes entre execução → diagnóstico → tarefas → peças
- Queries analíticas complexas (tempo médio, eficiência)
- Integridade referencial crítica
- Aggregations e relatórios

## Consistência Eventual com Saga Pattern

Como não há transações ACID entre serviços, implementamos **Saga Pattern Coreografado** usando Apache Kafka.

### Implementação Completa

#### Classes Implementadas

**OS Service:**
- `OSCriadaEvent.java` - Evento de OS criada
- `OrcamentoAprovadoEvent.java` - Evento de orçamento aprovado
- `KafkaEventPublisher.java` - Publicador Kafka
- `KafkaEventListener.java` - Consumidor Kafka
- `KafkaConfig.java` - Configuração Apache Kafka

**Billing Service:**
- `OSCriadaEvent.java` - Recebe OS criada
- `DiagnosticoConcluidoEvent.java` - Recebe diagnóstico
- `OrcamentoProntoEvent.java` - Orçamento calculado
- `OrcamentoAprovadoEvent.java` - Orçamento aprovado
- `BillingEventPublisher.java` - Publicador Kafka
- `BillingEventListener.java` - Consumidor Kafka
- `KafkaConfig.java` - Configuração Apache Kafka

**Execution Service:**
- `OSCriadaEvent.java` - Recebe OS criada
- `DiagnosticoConcluidoEvent.java` - Diagnóstico concluído
- `OrcamentoAprovadoEvent.java` - Recebe aprovação
- `ExecucaoConcluidaEvent.java` - Execução finalizada
- `ExecutionEventPublisher.java` - Publicador Kafka
- `ExecutionEventListener.java` - Consumidor Kafka
- `KafkaConfig.java` - Configuração Apache Kafka

### Fluxo Transacional Implementado

```
1. POST /api/os → OS Service cria OrdemServico
   ✅ OS: INSERT ordem_servico (status='ABERTA')
   ✅ OS: PUBLISH "OS_CRIADA" → os-events (topic)

2. Billing/Execution consomem "OS_CRIADA"
   ✅ Billing: INSERT orcamento (status='AGUARDANDO_DIAGNOSTICO')
   ✅ Execution: INSERT execucao_os (status='AGUARDANDO_DIAGNOSTICO')

3. POST /api/execucoes/{id}/diagnostico → Execution Service
   ✅ Execution: INSERT diagnostico
   ✅ Execution: PUBLISH "DIAGNOSTICO_CONCLUIDO" → execution-events (topic)

4. Billing consome "DIAGNOSTICO_CONCLUIDO"
   ✅ Billing: UPDATE orcamento (valorTotal, status='AGUARDANDO_APROVACAO')
   ✅ Billing: PUBLISH "ORCAMENTO_PRONTO" → billing-events (topic)

5. PUT /api/orcamentos/{id}/aprovar → Billing Service
   ✅ Billing: UPDATE orcamento (status='APROVADO')
   ✅ Billing: PUBLISH "ORCAMENTO_APROVADO" → billing-events (topic)

6. OS/Execution consomem "ORCAMENTO_APROVADO"
   ✅ OS: UPDATE ordem_servico (status='EM_EXECUCAO')
   ✅ Execution: UPDATE execucao_os (status='EM_ANDAMENTO', data_inicio=NOW())
```

### Garantias de Consistência

**1. Idempotência (Implementada)**
```java
// Billing Service - BillingEventListener
if (orcamentoRepository.findByOsId(event.getOsId()).isPresent()) {
    log.warn("Orçamento já existe. Ignorando evento duplicado.");
    return; // Não processa novamente
}
```

**2. Chave de Particionamento (Implementada)**
```java
// KafkaEventPublisher - record key para ordenação por OS
kafkaTemplate.send("os-events", event.getOsId().toString(),
    objectMapper.writeValueAsString(event));
```

**3. Consumer Group (Implementada)**
```java
// KafkaEventListener - consumer group configuration
@KafkaListener(topics = "os-events", groupId = "os-service-group")
public void handleEvent(ConsumerRecord<String, String> record) {
    // Kafka gerencia offset e partições automaticamente
}
```

**4. Compensação (Implementada)**
```java
// OS Service - EventListener
public void compensateOSStatus(OrcamentoAprovadoEvent event) {
    OrdemServico os = findById(event.getOsId());
    os.setStatus(StatusOS.AGUARDANDO_APROVACAO); // Reverte
    save(os);
    log.warn("Compensação executada: OS revertida");
}
```

### Tópicos Kafka

```
os-events (topic)
├── OS_CRIADA (publicado por OS Service)
└── Consumido por: Billing Service, Execution Service

billing-events (topic)
├── ORCAMENTO_PRONTO (publicado por Billing Service)
├── ORCAMENTO_APROVADO (publicado por Billing Service)
└── Consumido por: OS Service, Execution Service

execution-events (topic)
├── DIAGNOSTICO_CONCLUIDO (publicado por Execution Service)
├── EXECUCAO_CONCLUIDA (publicado por Execution Service)
└── Consumido por: Billing Service, OS Service
```

### Compensação em Caso de Falha

**Cenário 1: Falha após aprovação do orçamento**
```java
try {
    os.setStatus(StatusOS.EM_EXECUCAO);
    save(os);
} catch (Exception e) {
    // Offset não é commitado → mensagem será reprocessada
    // Retry automático via Kafka consumer retry mechanism
}
```

**Cenário 2: Serviço indisponível**
- Mensagem permanece no tópico (retenção configurável)
- Retry com exponential backoff via Spring Kafka retry
- Após 3 tentativas → DLT (Dead Letter Topic)
- Alerta no New Relic APM
- Intervenção manual ou reprocessamento

## Backup e Disaster Recovery

| Serviço | Estratégia de Backup |
|---------|---------------------|
| OS Service | RDS Automated Backups (7 dias) + Snapshots diários |
| Billing Service | DynamoDB Point-in-Time Recovery |
| Execution Service | RDS Automated Backups (7 dias) + Snapshots diários |

## Alternativas Consideradas

### 1. Shared Database
- **Prós**: Simples, transações ACID, sem duplicação
- **Contras**: Alto acoplamento, gargalo, deploy acoplado
- **Motivo da rejeição**: Viola princípios de microserviços

### 2. Tudo em PostgreSQL
- **Prós**: Uma tecnologia, uniformidade
- **Contras**: Não aproveita flexibilidade do DynamoDB
- **Motivo da rejeição**: Orçamentos se beneficiam de schema flexível

### 3. Tudo em DynamoDB
- **Prós**: Schema flexível para todos
- **Contras**: Perde ACID, relacionamentos complexos difíceis
- **Motivo da rejeição**: OS e Execution precisam de relacional

## Migração de Dados

Para sincronização inicial entre ambientes:

```bash
# Dump OS Service (PostgreSQL)
pg_dump -h $OS_DB_HOST -U postgres osservice_db > os_backup.sql

# Export Billing Service (DynamoDB)
aws dynamodb export-table-to-point-in-time --table-arn $BILLING_TABLE_ARN --s3-bucket $S3_BACKUP_BUCKET --s3-prefix billing_backup

# Dump Execution Service (PostgreSQL)
pg_dump -h $EXEC_DB_HOST -U postgres execution_db > exec_backup.sql
```

## Referências
- [Database per Service Pattern](https://microservices.io/patterns/data/database-per-service.html)
- [AWS RDS Best Practices](https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/CHAP_BestPractices.html)
- [DynamoDB Best Practices](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/best-practices.html)
- [Saga Pattern](https://microservices.io/patterns/data/saga.html)

## Revisão
Esta decisão será revisada em **Julho 2026** ou quando houver necessidade de queries cross-service frequentes.

---

**Autor**: Grupo 99  
**Data**: Janeiro 2026  
**Revisores**: Equipe de Arquitetura e DBA
