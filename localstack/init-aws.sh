#!/bin/bash
# =============================================================================
# Script de Inicialização do LocalStack
# Cria todas as filas SQS necessárias para os microserviços
# =============================================================================

set -e

echo "=============================================="
echo "Iniciando configuração do LocalStack..."
echo "=============================================="

# Endpoint do LocalStack
LOCALSTACK_HOST=localhost
AWS_ENDPOINT="http://${LOCALSTACK_HOST}:4566"
AWS_REGION="us-east-1"

# Configuração do AWS CLI para LocalStack
export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test
export AWS_DEFAULT_REGION=${AWS_REGION}

echo ""
echo ">>> Criando filas SQS..."
echo ""

# =============================================================================
# Filas do OS Service
# =============================================================================
echo "[OS-SERVICE] Criando filas..."
awslocal sqs create-queue --queue-name os-events-queue
awslocal sqs create-queue --queue-name os-events-dlq
echo "  ✓ os-events-queue"
echo "  ✓ os-events-dlq"

# =============================================================================
# Filas do Billing Service
# =============================================================================
echo ""
echo "[BILLING-SERVICE] Criando filas..."
awslocal sqs create-queue --queue-name billing-events-queue
awslocal sqs create-queue --queue-name billing-events-dlq
awslocal sqs create-queue --queue-name payment-events-queue
awslocal sqs create-queue --queue-name payment-events-dlq
echo "  ✓ billing-events-queue"
echo "  ✓ billing-events-dlq"
echo "  ✓ payment-events-queue"
echo "  ✓ payment-events-dlq"

# =============================================================================
# Filas do Execution Service
# =============================================================================
echo ""
echo "[EXECUTION-SERVICE] Criando filas..."
awslocal sqs create-queue --queue-name execution-events-queue
awslocal sqs create-queue --queue-name execution-events-dlq
awslocal sqs create-queue --queue-name diagnostico-events-queue
awslocal sqs create-queue --queue-name diagnostico-events-dlq
echo "  ✓ execution-events-queue"
echo "  ✓ execution-events-dlq"
echo "  ✓ diagnostico-events-queue"
echo "  ✓ diagnostico-events-dlq"

# =============================================================================
# Filas do Customer Service
# =============================================================================
echo ""
echo "[CUSTOMER-SERVICE] Criando filas..."
awslocal sqs create-queue --queue-name customer-events-queue
awslocal sqs create-queue --queue-name customer-events-dlq
awslocal sqs create-queue --queue-name veiculo-events-queue
awslocal sqs create-queue --queue-name veiculo-events-dlq
echo "  ✓ customer-events-queue"
echo "  ✓ customer-events-dlq"
echo "  ✓ veiculo-events-queue"
echo "  ✓ veiculo-events-dlq"

# =============================================================================
# Filas do People Service
# =============================================================================
echo ""
echo "[PEOPLE-SERVICE] Criando filas..."
awslocal sqs create-queue --queue-name pessoas-events-queue
awslocal sqs create-queue --queue-name pessoas-events-dlq
echo "  ✓ pessoas-events-queue"
echo "  ✓ pessoas-events-dlq"

# =============================================================================
# Filas do HR Service
# =============================================================================
echo ""
echo "[HR-SERVICE] Criando filas..."
awslocal sqs create-queue --queue-name hr-events-queue
awslocal sqs create-queue --queue-name hr-events-dlq
awslocal sqs create-queue --queue-name funcionario-events-queue
awslocal sqs create-queue --queue-name funcionario-events-dlq
echo "  ✓ hr-events-queue"
echo "  ✓ hr-events-dlq"
echo "  ✓ funcionario-events-queue"
echo "  ✓ funcionario-events-dlq"

# =============================================================================
# Filas do Catalog Service
# =============================================================================
echo ""
echo "[CATALOG-SERVICE] Criando filas..."
awslocal sqs create-queue --queue-name catalog-events-queue
awslocal sqs create-queue --queue-name catalog-events-dlq
awslocal sqs create-queue --queue-name peca-events-queue
awslocal sqs create-queue --queue-name servico-events-queue
echo "  ✓ catalog-events-queue"
echo "  ✓ catalog-events-dlq"
echo "  ✓ peca-events-queue"
echo "  ✓ servico-events-queue"

# =============================================================================
# Filas do Maintenance Service
# =============================================================================
echo ""
echo "[MAINTENANCE-SERVICE] Criando filas..."
awslocal sqs create-queue --queue-name maintenance-events-queue
awslocal sqs create-queue --queue-name maintenance-events-dlq
echo "  ✓ maintenance-events-queue"
echo "  ✓ maintenance-events-dlq"

# =============================================================================
# Filas do Notification Service
# =============================================================================
echo ""
echo "[NOTIFICATION-SERVICE] Criando filas..."
awslocal sqs create-queue --queue-name notification-events-queue
awslocal sqs create-queue --queue-name notification-events-dlq
awslocal sqs create-queue --queue-name email-queue
awslocal sqs create-queue --queue-name sms-queue
echo "  ✓ notification-events-queue"
echo "  ✓ notification-events-dlq"
echo "  ✓ email-queue"
echo "  ✓ sms-queue"

# =============================================================================
# Filas do Operations Service
# =============================================================================
echo ""
echo "[OPERATIONS-SERVICE] Criando filas..."
awslocal sqs create-queue --queue-name operations-queue
awslocal sqs create-queue --queue-name operations-dlq
echo "  ✓ operations-queue"
echo "  ✓ operations-dlq"

# =============================================================================
# Filas de Saga (Orquestração)
# =============================================================================
echo ""
echo "[SAGA] Criando filas de orquestração..."
awslocal sqs create-queue --queue-name saga-orchestrator-queue
awslocal sqs create-queue --queue-name saga-compensation-queue
awslocal sqs create-queue --queue-name saga-reply-queue
echo "  ✓ saga-orchestrator-queue"
echo "  ✓ saga-compensation-queue"
echo "  ✓ saga-reply-queue"

# =============================================================================
# Tabelas DynamoDB (Billing + Catalog Services)
# Substituiu MongoDB/DocumentDB - DynamoDB é Free Tier na AWS
# =============================================================================
echo ""
echo ">>> Criando tabelas DynamoDB..."
echo ""

echo "[BILLING-SERVICE] Criando tabelas DynamoDB..."
awslocal dynamodb create-table \
    --table-name orcamentos \
    --attribute-definitions AttributeName=id,AttributeType=S \
    --key-schema AttributeName=id,KeyType=HASH \
    --billing-mode PAY_PER_REQUEST
echo "  ✓ orcamentos"

awslocal dynamodb create-table \
    --table-name pagamentos \
    --attribute-definitions AttributeName=id,AttributeType=S \
    --key-schema AttributeName=id,KeyType=HASH \
    --billing-mode PAY_PER_REQUEST
echo "  ✓ pagamentos"

echo ""
echo "[CATALOG-SERVICE] Criando tabelas DynamoDB..."
awslocal dynamodb create-table \
    --table-name pecas \
    --attribute-definitions AttributeName=id,AttributeType=S \
    --key-schema AttributeName=id,KeyType=HASH \
    --billing-mode PAY_PER_REQUEST
echo "  ✓ pecas"

awslocal dynamodb create-table \
    --table-name servicos \
    --attribute-definitions AttributeName=id,AttributeType=S \
    --key-schema AttributeName=id,KeyType=HASH \
    --billing-mode PAY_PER_REQUEST
echo "  ✓ servicos"

echo ""
echo "Tabelas DynamoDB criadas:"
awslocal dynamodb list-tables --output table

# =============================================================================
# Listar todas as filas criadas
# =============================================================================
echo ""
echo "=============================================="
echo "Filas SQS criadas com sucesso!"
echo "=============================================="
echo ""
echo "Lista de todas as filas:"
awslocal sqs list-queues --output table

echo ""
echo "=============================================="
echo "LocalStack inicializado com sucesso!"
echo "=============================================="
echo ""
echo "Endpoints:"
echo "  SQS: http://localhost:4566"
echo ""
echo "Credenciais de teste:"
echo "  AWS_ACCESS_KEY_ID: test"
echo "  AWS_SECRET_ACCESS_KEY: test"
echo "  AWS_REGION: us-east-1"
echo ""
