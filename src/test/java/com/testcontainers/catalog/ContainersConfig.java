package com.testcontainers.catalog;

import static org.testcontainers.utility.DockerImageName.parse;

import com.testcontainers.catalog.domain.FileStorageService;
import net.bytebuddy.asm.Advice;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.utility.MountableFile;

@TestConfiguration(proxyBeanMethods = false)
public class ContainersConfig {

    static Network network = Network.newNetwork();

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        PostgreSQLContainer<?> selfPostgreSQLContainer =
                new PostgreSQLContainer<>(parse("postgres:16-alpine"))
                        .withNetwork(network).withNetworkAliases("postgres");
        return selfPostgreSQLContainer;
    }

    @Bean
    @ServiceConnection
    KafkaContainer kafkaContainer() {
        return new KafkaContainer(parse("confluentinc/cp-kafka:7.5.0"))
                .withReuse(true).withNetwork(network).withNetworkAliases("kafka");

    }

    @Bean("localstackContainer")
    LocalStackContainer localstackContainer(DynamicPropertyRegistry registry) {
        LocalStackContainer localStack = new LocalStackContainer(parse("localstack/localstack:2.3"))
                .withNetwork(network).withNetworkAliases("localstack").withReuse(true);
        registry.add("spring.cloud.aws.credentials.access-key", localStack::getAccessKey);
        registry.add("spring.cloud.aws.credentials.secret-key", localStack::getSecretKey);
        registry.add("spring.cloud.aws.region.static", localStack::getRegion);
        registry.add("spring.cloud.aws.endpoint", localStack::getEndpoint);
        return localStack;
    }

    @Bean
    @DependsOn("localstackContainer")
    ApplicationRunner awsInitializer(ApplicationProperties properties, FileStorageService fileStorageService) {
        return args -> fileStorageService.createBucket(properties.productImagesBucketName());
    }
}
