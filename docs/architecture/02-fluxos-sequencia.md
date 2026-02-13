# 🔄 Diagramas de Sequência - Arquitetura de Microserviços

Este documento descreve os principais fluxos de negócio do sistema através de diagramas de sequência na arquitetura de microserviços.

---

## 1. Fluxo de Autenticação

### Descrição
O fluxo de autenticação permite que clientes se autentiquem usando seu CPF e recebam um token JWT para acessar as APIs protegidas dos microserviços.

### Diagrama de Sequência

```
Cliente          API Gateway     Lambda Auth      RDS (pessoas)      OS Service
  │                  │                │                 │                 │
  │  POST /auth      │                │                 │                 │
  │  {cpf: "..."}    │                │                 │                 │
  ├─────────────────>│                │                 │                 │
  │                  │  Invoke        │                 │                 │
  │                  │  Lambda        │                 │                 │
  │                  ├───────────────>│                 │                 │
  │                  │                │  SELECT pessoa  │                 │
  │                  │                │  WHERE cpf=?    │                 │
  │                  │                ├────────────────>│                 │
  │                  │                │    Pessoa       │                 │
  │                  │                │<────────────────┤                 │
  │                  │                │                 │                 │
  │                  │                │ Generate JWT    │                 │
  │                  │                │ (HMAC256)       │                 │
  │                  │                │                 │                 │
  │                  │  200 + Token   │                 │                 │
  │                  │<───────────────┤                 │                 │
  │  200 OK          │                │                 │                 │
  │  {token: "..."}  │                │                 │                 │
  │<─────────────────┤                │                 │                 │
  │                  │                │                 │                 │
  │  GET /api/ordens │                │                 │                 │
  │  Authorization:  │                │                 │                 │
  │  Bearer {token}  │                │                 │                 │
  ├─────────────────>│                │                 │                 │
  │                  │  Validate JWT  │                 │                 │
  │                  │  + Forward     │                 │                 │
  │                  ├───────────────────────────────────────────────────>│
  │                  │                │                 │   Query DB      │
  │                  │                │                 │<────────────────┤
  │                  │                │                 │   Ordens        │
  │                  │                │                 │─────────────────>│
  │                  │                200 OK + Data                       │
  │                  │<───────────────────────────────────────────────────┤
  │  200 OK + Data   │                │                 │                 │
  │<─────────────────┤                │                 │                 │
```

### Passos Detalhados

| Passo | Origem | Destino | Ação |
|-------|--------|---------|------|
| 1 | Cliente | API Gateway | `POST /auth` com CPF no body |
| 2 | API Gateway | Lambda Auth | Invocação da função Lambda |
| 3 | Lambda Auth | RDS PostgreSQL | `SELECT * FROM pessoas WHERE cpf = ?` |
| 4 | RDS | Lambda Auth | Retorno dos dados do cliente (id, nome, email, ativo) |
| 5 | Lambda Auth | - | Geração do token JWT (HMAC256) com claims |
| 6 | Lambda Auth | API Gateway | Response 200 com token JWT |
| 7 | API Gateway | Cliente | Token JWT para uso nas APIs |
| 8 | Cliente | API Gateway | Request com header `Authorization: Bearer <token>` |
| 9 | API Gateway | - | Validação do token JWT |
| 10 | API Gateway | Microserviço | Forward da request com JWT validado |
| 11 | Microserviço | Cliente | Dados solicitados |

### Estrutura do Token JWT

```json
{
  "header": {
    "alg": "HS256",
    "typ": "JWT"
  },
  "payload": {
    "sub": "cliente-uuid",
    "cpf": "12345678901",
    "name": "João Silva",
    "email": "joao@example.com",
    "role": "CLIENTE",
    "iat": 1705190400,
    "exp": 1705276800
  }
}
```

### Códigos de Resposta

| Código | Descrição |
|--------|-----------|
| 200 | Autenticação bem-sucedida, token retornado |
| 400 | CPF com formato inválido |
| 401 | Token JWT inválido ou expirado |
| 403 | Cliente inativo no sistema |
| 404 | Cliente não encontrado no banco |

---

## 2. Fluxo Completo de Ordem de Serviço (Event-Driven)

### Descrição
O fluxo representa o ciclo de vida completo de uma Ordem de Serviço através dos 3 microserviços, utilizando comunicação assíncrona via Apache Kafka.

### Diagrama de Sequência

```
Cliente    OS Service    Kafka (os-events)    Billing Service    Kafka (billing)    Execution Service
  │             │                │                   │                │                  │
  │─ POST ─────>│                │                   │                │                  │
  │ /ordens     │                │                   │                │                  │
  │             │ Create OS      │                   │                │                  │
  │             │ Status:        │                   │                │                  │
  │             │ RECEBIDA       │                   │                │                  │
  │             │                │                   │                │                  │
  │<─── 201 ────┤                │                   │                │                  │
  │ Created     │                │                   │                │                  │
  │             │ Publish Event  │                   │                │                  │
  │             │ "OS_CRIADA"    │                   │                │                  │
  │             ├───────────────>│                   │                │                  │
  │             │                │  Poll Messages    │                │                  │
  │             │                │<──────────────────┤                │                  │
  │             │                │  Event: OS_CRIADA │                │                  │
  │             │                ├──────────────────>│                │                  │
  │             │                │                   │ Create         │                  │
  │             │                │                   │ Orcamento      │                  │
  │             │                │                   │ (empty)        │                  │
  │             │                │                   │                │  Poll Messages   │
  │             │                │                   │                │<─────────────────┤
  │             │                │                   │                │  Event: OS_CRIADA│
  │             │                │                   │                ├─────────────────>│
  │             │                │                   │                │                  │
  │             │                │                   │                │  Create Execucao │
  │             │                │                   │                │  Status: AGUARD. │
  │             │                │                   │                │  DIAGNOSTICO     │
  │             │                │                   │                │                  │
  │─ PUT ──────>│                │                   │                │                  │
  │/ordens/{id} │ Update Status  │                   │                │                  │
  │/diagnostico │ EM_DIAGNOSTICO │                   │                │                  │
  │             │                │                   │                │                  │
  │<─── 200 ────┤                │                   │                │                  │
  │             │ Publish Event  │                   │                │                  │
  │             │"STATUS_CHANGED"│                   │                │                  │
  │             ├───────────────>│                   │                │                  │
  │             │                │                   │                │  Poll            │
  │             │                │                   │                │<─────────────────┤
  │             │                │                   │                │  Event: STATUS   │
  │             │                │                   │                ├─────────────────>│
  │             │                │                   │                │  Update Execucao │
  │             │                │                   │                │  Status: EM_DIAG │
  │             │                │                   │                │                  │
  │─ POST ─────────────────────────────────────────────────────────────────────────────>│
  │/execucoes/  │                │                   │                │                  │
  │{id}/diagn.  │                │                   │                │  Add Diagnostico │
  │             │                │                   │                │  (desc, pecas)   │
  │<─── 201 ────────────────────────────────────────────────────────────────────────────┤
  │             │                │                   │                │  Publish Event   │
  │             │                │                   │                │ "DIAG_CONCLUIDO" │
  │             │                │                   │                ├─────────────────>│
  │             │                │                   │  Poll          │  execution-topic │
  │             │                │                   │<───────────────┤                  │
  │             │                │                   │  Event: DIAG   │                  │
  │             │                │                   │<───────────────┤                  │
  │             │                │                   │  Calculate     │                  │
  │             │                │                   │  Orcamento     │                  │
  │             │                │                   │  (pecas+mao    │                  │
  │             │                │                   │  de obra)      │                  │
  │             │                │                   │                │                  │
  │─ GET ──────────────────────────────────────────>│                │                  │
  │/orcamentos/ │                │                   │  Return        │                  │
  │{osId}       │                │                   │  Orcamento     │                  │
  │<────────────────────────────────────────────────┤                │                  │
  │ 200 +       │                │                   │                │                  │
  │ Orcamento   │                │                   │                │                  │
  │             │                │                   │                │                  │
  │─ PUT ──────────────────────────────────────────>│                │                  │
  │/orcamentos/ │                │                   │  Approve       │                  │
  │{id}/aprovar │                │                   │  Status:       │                  │
  │             │                │                   │  APROVADO      │                  │
  │<────────────────────────────────────────────────┤                │                  │
  │ 200 OK      │                │                   │  Publish Event │                  │
  │             │                │                   │ "ORC_APROVADO" │                  │
  │             │                │                   ├───────────────>│                  │
  │             │                │  Poll             │  billing-topic │                  │
  │             │<───────────────┤                   │                │                  │
  │             │  Event:        │                   │                │                  │
  │             │  ORC_APROVADO  │                   │                │                  │
  │             │  Update Status │                   │                │                  │
  │             │  EM_EXECUCAO   │                   │                │                  │
  │             │                │                   │                │  Poll            │
  │             │                │                   │                │<─────────────────┤
  │             │                │                   │                │  Event:          │
  │             │                │                   │                │  ORC_APROVADO    │
  │             │                │                   │                │  Update Status   │
  │             │                │                   │                │  EM_EXECUCAO     │
  │             │                │                   │                │                  │
  [... continua com execução, tarefas, pagamento, entrega ...]
```

### Estados da Ordem de Serviço

| Status | Microserviço | Descrição | Próximo Status |
|--------|--------------|-----------|----------------|
| `RECEBIDA` | OS Service | OS criada, veículo recebido | `EM_DIAGNOSTICO` |
| `EM_DIAGNOSTICO` | OS + Execution | Mecânico avaliando o veículo | `AGUARDANDO_APROVACAO` |
| `AGUARDANDO_APROVACAO` | Billing | Orçamento enviado ao cliente | `EM_EXECUCAO` ou `CANCELADA` |
| `EM_EXECUCAO` | OS + Execution | Serviço em andamento | `AGUARDANDO_PAGAMENTO` |
| `AGUARDANDO_PAGAMENTO` | Billing | Serviço concluído, aguarda pagamento | `FINALIZADA` |
| `FINALIZADA` | OS Service | Pagamento confirmado | `ENTREGUE` |
| `ENTREGUE` | OS Service | Veículo entregue ao cliente | (final) |
| `CANCELADA` | OS Service | OS cancelada | (final) |

### Eventos Publicados no Kafka

#### os-events-topic

| Evento | Produtor | Payload | Consumidores |
|--------|----------|---------|--------------|
| `OS_CRIADA` | OS Service | `{osId, clienteId, veiculoId, timestamp}` | Billing, Execution |
| `STATUS_CHANGED` | OS Service | `{osId, oldStatus, newStatus, timestamp}` | Billing, Execution |
| `OS_CANCELADA` | OS Service | `{osId, motivo, timestamp}` | Billing, Execution |

#### billing-events-topic

| Evento | Produtor | Payload | Consumidores |
|--------|----------|---------|--------------|
| `ORCAMENTO_CRIADO` | Billing Service | `{orcamentoId, osId, valor, timestamp}` | OS |
| `ORCAMENTO_APROVADO` | Billing Service | `{orcamentoId, osId, timestamp}` | OS, Execution |
| `PAGAMENTO_CONFIRMADO` | Billing Service | `{pagamentoId, osId, valor, timestamp}` | OS |

#### execution-events-topic

| Evento | Produtor | Payload | Consumidores |
|--------|----------|---------|--------------|
| `DIAGNOSTICO_CONCLUIDO` | Execution Service | `{execucaoId, osId, diagnostico, pecas, timestamp}` | Billing |
| `TAREFA_CONCLUIDA` | Execution Service | `{tarefaId, execucaoId, osId, timestamp}` | OS |
| `EXECUCAO_FINALIZADA` | Execution Service | `{execucaoId, osId, timestamp}` | OS, Billing |

---

## 3. Fluxo de Criação de Orçamento

### Descrição
Após o diagnóstico ser concluído no Execution Service, o Billing Service consome o evento e calcula o orçamento automaticamente.

### Diagrama Simplificado

```
Execution Service    Kafka (execution-events)    Billing Service    DynamoDB
       │                       │                       │              │
       │ Publish Event         │                       │              │
       │ "DIAG_CONCLUIDO"      │                       │              │
       ├──────────────────────>│                       │              │
       │                       │  Poll Messages        │              │
       │                       │<──────────────────────┤              │
       │                       │  Event: DIAG_CONCL    │              │
       │                       ├──────────────────────>│              │
       │                       │                       │ Calculate    │
       │                       │                       │ Orcamento    │
       │                       │                       │ (pecas+labor)│
       │                       │                       │              │
       │                       │                       │ INSERT       │
       │                       │                       │ orcamento    │
       │                       │                       ├─────────────>│
       │                       │                       │<─────────────┤
       │                       │                       │ Orcamento ID │
```

### Cálculo do Orçamento

```java
// Pseudo-código
BigDecimal valorPecas = diagnostico.getPecas()
    .stream()
    .map(p -> p.getValor())
    .reduce(BigDecimal.ZERO, BigDecimal::add);

BigDecimal valorMaoDeObra = diagnostico.getHorasEstimadas()
    .multiply(VALOR_HORA_MECANICO);

BigDecimal valorTotal = valorPecas.add(valorMaoDeObra);

Orcamento orcamento = new Orcamento(
    osId,
    valorPecas,
    valorMaoDeObra,
    valorTotal,
    StatusOrcamento.AGUARDANDO_APROVACAO
);
```

---

## 4. Fluxo de Pagamento

### Descrição
Cliente aprova orçamento e realiza pagamento. Billing Service processa e notifica OS Service.

### Diagrama Simplificado

```
Cliente    Billing Service    DynamoDB    Kafka (billing-events)    OS Service
   │              │               │                 │                  │
   │─ PUT ──────>│               │                 │                  │
   │/pagamentos   │               │                 │                  │
   │{orcamentoId, │ Create        │                 │                  │
   │ forma, valor}│ Pagamento     │                 │                  │
   │              │               │                 │                  │
   │              │ INSERT        │                 │                  │
   │              │ pagamento     │                 │                  │
   │              ├──────────────>│                 │                  │
   │              │<──────────────┤                 │                  │
   │<─── 201 ─────┤ Pagamento ID  │                 │                  │
   │ Created      │               │                 │                  │
   │              │ Publish Event │                 │                  │
   │              │"PAGAM_CONFIRM"│                 │                  │
   │              ├───────────────────────────────>│                  │
   │              │               │                 │  Poll            │
   │              │               │                 │<─────────────────┤
   │              │               │                 │  Event: PAGAM    │
   │              │               │                 ├─────────────────>│
   │              │               │                 │  Update Status   │
   │              │               │                 │  FINALIZADA      │
```

---

## 5. Observabilidade - Distributed Tracing

### Descrição
New Relic rastreia requisições através dos microserviços usando Distributed Tracing.

### Trace ID Flow

```
Cliente Request → API Gateway [Trace-ID: abc123]
                       ↓
                  OS Service [Trace-ID: abc123, Span-ID: 001]
                       ↓
              Kafka Message [Trace-ID: abc123]
                       ↓
              Billing Service [Trace-ID: abc123, Span-ID: 002]
                       ↓
              New Relic APM [Complete Trace Visualization]
```

### Informações Capturadas

- **Latência Total**: Tempo do request inicial até resposta final
- **Span Duration**: Tempo em cada microserviço
- **External Calls**: Chamadas para banco de dados, Kafka
- **Errors**: Exceções e stack traces
- **Attributes**: Headers, query params, response codes

---

## Endpoints das APIs

### OS Service (Port 8081)

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | `/api/ordens` | Criar nova OS |
| GET | `/api/ordens/{id}` | Consultar OS |
| PUT | `/api/ordens/{id}/status` | Atualizar status |
| GET | `/api/ordens` | Listar todas as OS |
| DELETE | `/api/ordens/{id}` | Cancelar OS |

### Billing Service (Port 8082)

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | `/api/v1/orcamentos` | Criar orçamento |
| GET | `/api/v1/orcamentos/{id}` | Consultar orçamento |
| PUT | `/api/v1/orcamentos/{id}/aprovar` | Aprovar orçamento |
| POST | `/api/v1/pagamentos` | Processar pagamento |
| GET | `/api/v1/pagamentos/{osId}` | Consultar pagamentos |

### Execution Service (Port 8083)

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | `/api/v1/execucoes` | Criar execução |
| POST | `/api/v1/execucoes/{id}/diagnostico` | Adicionar diagnóstico |
| POST | `/api/v1/execucoes/{id}/tarefas` | Registrar tarefa |
| PUT | `/api/v1/tarefas/{id}/status` | Atualizar tarefa |
| GET | `/api/v1/execucoes/{osId}` | Consultar execução |

---

*Última atualização: Janeiro 2026*
