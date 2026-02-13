# 🐳 LocalStack - Ambiente de Desenvolvimento Local

Este diretório contém a configuração completa para executar os microserviços localmente usando **LocalStack** para emular serviços AWS.

## 📋 Pré-requisitos

- Docker Desktop instalado e rodando
- Docker Compose v2+
- Java 21+
- Maven 3.9+

## 🚀 Início Rápido

### 1. Subir toda a infraestrutura

```bash
# Na raiz do projeto (fase_quatro)
docker-compose -f docker-compose.localstack.yml up -d
```

### 2. Verificar se os serviços estão rodando

```bash
docker-compose -f docker-compose.localstack.yml ps
```

### 3. Verificar as filas SQS e tópicos Kafka criados

```bash
# Listar filas SQS (serviços auxiliares: Customer, People, HR, Catalog, Maintenance, Notification, Operations)
aws --endpoint-url=http://localhost:4566 sqs list-queues --region us-east-1

# Os serviços da Saga (OS, Billing, Execution) utilizam Apache Kafka.
# Para verificar os tópicos Kafka, use os comandos do Kafka CLI ou o Kafka UI.

# Ou acesse o dashboard do LocalStack
# http://localhost:4566/_localstack/extensions/ui
```

### 4. Verificar os bancos de dados

```bash
# Acessar Adminer (PostgreSQL)
# http://localhost:8090
# Sistema: PostgreSQL
# Servidor: postgres
# Usuário: postgres
# Senha: postgres

# DynamoDB Local é acessível via AWS CLI com endpoint http://localhost:4566
```

## 📦 Serviços Disponíveis

| Serviço | Porta | URL |
|---------|-------|-----|
| LocalStack | 4566 | http://localhost:4566 |
| PostgreSQL | 5432 | jdbc:postgresql://localhost:5432/db_name |
| DynamoDB Local | 4566 | http://localhost:4566 (via LocalStack) |
| Adminer | 8090 | http://localhost:8090 |

## 📨 Mensageria — Kafka (Saga) e SQS (Auxiliares)

> **Nota:** Os 3 serviços da Saga (OS, Billing, Execution) foram migrados de SQS para **Apache Kafka**.
> Os demais 6 serviços auxiliares continuam utilizando **filas SQS** via LocalStack.

### Tópicos Kafka (Serviços da Saga)

| Serviço | Tópicos Kafka |
|---------|---------------|
| OS Service | `os-events`, `os-events-dlq` |
| Billing Service | `billing-events`, `billing-events-dlq`, `payment-events`, `payment-events-dlq` |
| Execution Service | `execution-events`, `execution-events-dlq`, `diagnostico-events`, `diagnostico-events-dlq` |
| Saga (Orquestração) | `saga-orchestrator`, `saga-compensation`, `saga-reply` |

### Filas SQS (Serviços Auxiliares — via LocalStack)

| Serviço | Filas SQS |
|---------|-------|
| Customer Service | `customer-events-queue`, `customer-events-dlq`, `veiculo-events-queue`, `veiculo-events-dlq` |
| People Service | `pessoas-events-queue`, `pessoas-events-dlq` |
| HR Service | `hr-events-queue`, `hr-events-dlq`, `funcionario-events-queue`, `funcionario-events-dlq` |
| Catalog Service | `catalog-events-queue`, `catalog-events-dlq`, `peca-events-queue`, `servico-events-queue` |
| Maintenance Service | `maintenance-events-queue`, `maintenance-events-dlq` |
| Notification Service | `notification-events-queue`, `notification-events-dlq`, `email-queue`, `sms-queue` |
| Operations Service | `operations-queue`, `operations-dlq` |

## 🗄️ Bancos de Dados

### PostgreSQL

| Banco | Microserviço |
|-------|--------------|
| `os_db` | oficina-os-service |
| `execution_db` | oficina-execution-service |
| `customer_db` | oficina-customer-service |
| `people_db` | oficina-people-service |
| `hr_db` | oficina-hr-service |
| `catalog_db` | oficina-catalog-service |
| `maintenance_db` | oficina-maintenance-service |
| `notification_db` | oficina-notification-service |
| `operations_db` | oficina-operations-service |
| `tech_fiap_db` | tech_fiap3 |

### DynamoDB Local

| Tabela | Microserviço |
|--------|--------------|
| `billing-service-orcamentos` | oficina-billing-service |
| `billing-service-pagamentos` | oficina-billing-service |

## ⚙️ Configuração dos Microserviços

### Opção 1: Usando perfil local

Crie um arquivo `application-local.properties` em cada microserviço:

```properties
# AWS LocalStack
spring.cloud.aws.endpoint=http://localhost:4566
spring.cloud.aws.region.static=us-east-1
spring.cloud.aws.credentials.access-key=test
spring.cloud.aws.credentials.secret-key=test

# Database (ajuste o nome do banco)
spring.datasource.url=jdbc:postgresql://localhost:5432/seu_banco_db
spring.datasource.username=postgres
spring.datasource.password=postgres
```

Execute com:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### Opção 2: Usando variáveis de ambiente

```bash
export AWS_ENDPOINT_URL=http://localhost:4566
export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test
export AWS_REGION=us-east-1

mvn spring-boot:run
```

## 🔧 Comandos Úteis

### Listar filas SQS (serviços auxiliares)

> Os comandos SQS abaixo se aplicam aos **6 serviços auxiliares** (Customer, People, HR, Catalog, Maintenance, Notification, Operations) que ainda utilizam SQS via LocalStack.
> Para os serviços da Saga (OS, Billing, Execution), utilize os comandos do Kafka CLI.

```bash
aws --endpoint-url=http://localhost:4566 sqs list-queues --region us-east-1
```

### Enviar mensagem para uma fila SQS (serviços auxiliares)

```bash
# Exemplo: enviar mensagem para o Customer Service (SQS)
aws --endpoint-url=http://localhost:4566 sqs send-message \
    --queue-url http://localhost:4566/000000000000/customer-events-queue \
    --message-body '{"tipo":"NOVO_CLIENTE","clienteId":"123"}' \
    --region us-east-1
```

### Receber mensagens de uma fila SQS (serviços auxiliares)

```bash
# Exemplo: receber mensagens do Customer Service (SQS)
aws --endpoint-url=http://localhost:4566 sqs receive-message \
    --queue-url http://localhost:4566/000000000000/customer-events-queue \
    --region us-east-1
```

### Ver logs do LocalStack

```bash
docker-compose -f docker-compose.localstack.yml logs -f localstack
```

### Reiniciar todos os serviços

```bash
docker-compose -f docker-compose.localstack.yml down
docker-compose -f docker-compose.localstack.yml up -d
```

### Limpar todos os dados e começar do zero

```bash
docker-compose -f docker-compose.localstack.yml down -v
docker-compose -f docker-compose.localstack.yml up -d
```

## 🐛 Troubleshooting

### Erro: "Queue does not exist"

As filas são criadas automaticamente quando o LocalStack inicia. Se ainda não existirem:

```bash
# Execute o script manualmente
docker exec -it localstack bash /etc/localstack/init/ready.d/init-aws.sh
```

### Erro: "Connection refused" ao conectar ao PostgreSQL

Verifique se o container está rodando:
```bash
docker-compose -f docker-compose.localstack.yml ps postgres
```

### Erro: "Could not connect to DynamoDB"

```bash
# Verificar se LocalStack está rodando
docker-compose -f docker-compose.localstack.yml logs localstack

# Testar conexão
aws --endpoint-url=http://localhost:4566 dynamodb list-tables --region us-east-1
```

### LocalStack não está criando as filas

Verifique os logs:
```bash
docker-compose -f docker-compose.localstack.yml logs localstack
```

## 📁 Estrutura de Arquivos

```
localstack/
├── init-aws.sh              # Script para criar filas SQS (serviços auxiliares) + setup Kafka (Saga)
├── init-postgres.sh         # Script para criar bancos de dados
├── application-local.properties.template  # Template de configuração
└── README.md                # Esta documentação

docker-compose.localstack.yml  # Arquivo principal do Docker Compose (inclui Kafka/Zookeeper para Saga)
```

## 🔗 Links Úteis

- [LocalStack Documentação](https://docs.localstack.cloud/)
- [AWS CLI com LocalStack](https://docs.localstack.cloud/user-guide/integrations/aws-cli/)
- [Spring Cloud AWS](https://docs.awspring.io/spring-cloud-aws/docs/current/reference/html/index.html)
