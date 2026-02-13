package br.com.grupo99.catalogo.adapter.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import jakarta.annotation.PostConstruct;
import java.net.URI;

/**
 * Configuração DynamoDB para o Catalog Service.
 *
 * ✅ Substitui MongoConfig (@EnableMongoAuditing / @EnableMongoRepositories)
 * ✅ Auto-cria tabelas para desenvolvimento local (LocalStack)
 * ✅ Em produção, tabelas são criadas via Terraform
 */
@Configuration
public class DynamoDbConfig {

    private static final Logger log = LoggerFactory.getLogger(DynamoDbConfig.class);

    @Value("${aws.dynamodb.endpoint:}")
    private String endpoint;

    @Value("${aws.region:us-east-1}")
    private String region;

    @Value("${aws.dynamodb.table-prefix:}")
    private String tablePrefix;

    @Bean
    public DynamoDbClient dynamoDbClient() {
        var builder = DynamoDbClient.builder()
                .region(Region.of(region));

        if (endpoint != null && !endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create("test", "test")));
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }

        return builder.build();
    }

    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient dynamoDbClient) {
        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
    }

    /**
     * Auto-cria tabelas DynamoDB para desenvolvimento local.
     * Em produção, as tabelas são gerenciadas pelo Terraform.
     */
    @PostConstruct
    public void createTablesIfNotExist() {
        if (endpoint == null || endpoint.isBlank()) {
            log.info("DynamoDB endpoint não configurado (produção). Tabelas gerenciadas pelo Terraform.");
            return;
        }

        // Cria client separado para evitar referência circular com o @Bean method
        DynamoDbClient client = DynamoDbClient.builder()
                .region(Region.of(region))
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("test", "test")))
                .build();

        createTableIfNotExists(client, resolveTableName("pecas"), "id");
        createTableIfNotExists(client, resolveTableName("servicos"), "id");

        log.info("✅ Tabelas DynamoDB verificadas/criadas com sucesso");
    }

    private String resolveTableName(String baseName) {
        if (tablePrefix != null && !tablePrefix.isBlank()) {
            return tablePrefix + baseName;
        }
        return baseName;
    }

    private void createTableIfNotExists(DynamoDbClient client, String tableName, String partitionKey) {
        try {
            client.describeTable(DescribeTableRequest.builder().tableName(tableName).build());
            log.info("Tabela '{}' já existe", tableName);
        } catch (ResourceNotFoundException e) {
            log.info("Criando tabela '{}'...", tableName);
            client.createTable(CreateTableRequest.builder()
                    .tableName(tableName)
                    .keySchema(KeySchemaElement.builder()
                            .attributeName(partitionKey)
                            .keyType(KeyType.HASH)
                            .build())
                    .attributeDefinitions(AttributeDefinition.builder()
                            .attributeName(partitionKey)
                            .attributeType(ScalarAttributeType.S)
                            .build())
                    .billingMode(BillingMode.PAY_PER_REQUEST)
                    .build());
            log.info("✅ Tabela '{}' criada com sucesso", tableName);
        }
    }
}
