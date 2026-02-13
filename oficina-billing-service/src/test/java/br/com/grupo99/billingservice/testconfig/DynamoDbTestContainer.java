package br.com.grupo99.billingservice.testconfig;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Testcontainers support para DynamoDB Local.
 * Inicia um container DynamoDB Local compartilhado entre todos os testes.
 *
 * Uso: @ContextConfiguration(initializers =
 * DynamoDbTestContainer.Initializer.class)
 */
public class DynamoDbTestContainer {

    private static final GenericContainer<?> DYNAMODB_LOCAL;

    static {
        DYNAMODB_LOCAL = new GenericContainer<>(DockerImageName.parse("amazon/dynamodb-local:2.2.1"))
                .withExposedPorts(8000)
                .withCommand("-jar DynamoDBLocal.jar -inMemory -sharedDb");
        DYNAMODB_LOCAL.start();
    }

    public static String getEndpoint() {
        return "http://" + DYNAMODB_LOCAL.getHost() + ":" + DYNAMODB_LOCAL.getMappedPort(8000);
    }

    /**
     * ApplicationContextInitializer que injeta o endpoint do DynamoDB Local
     * nas propriedades do Spring antes da inicialização do contexto.
     */
    public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext context) {
            TestPropertyValues.of(
                    "aws.dynamodb.endpoint=" + getEndpoint(),
                    "aws.dynamodb.table-prefix=test-",
                    "aws.region=us-east-1").applyTo(context.getEnvironment());
        }
    }
}
