package com.testcontainers;

import com.github.dockerjava.api.model.HostConfig;
import com.testcontainers.fun.awaitility.CloudflaredContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;

import static org.testcontainers.utility.DockerImageName.parse;

public class TestMain {

    public static void main(String[] args) throws IOException, InterruptedException {
        Network network = Network.newNetwork();

        PostgreSQLContainer<?> postgres =
                new PostgreSQLContainer<>(parse("postgres:16-alpine"))
                        .withNetwork(network).withNetworkAliases("postgres");

        KafkaContainer kafka = new KafkaContainer(parse("confluentinc/cp-kafka:7.5.0"))
               .withNetwork(network).withNetworkAliases("kafka");

        LocalStackContainer localStack = new LocalStackContainer(parse("localstack/localstack:2.3"))
                .withNetwork(network).withNetworkAliases("localstack");


        Path dockerfile = Paths.get("Dockerfile");

        ;

        GenericContainer<?> app = new GenericContainer<>(new ImageFromDockerfile("gatling-demo-app", false)
                .withFileFromPath("Dockerfile", Paths.get("Dockerfile"))
                .withFileFromPath("target/java-local-development-workshop-0.0.1-SNAPSHOT.jar", Paths.get("target/java-local-development-workshop-0.0.1-SNAPSHOT.jar"))
        )
                .withEnv("SPRING_KAFKA_BOOTSTRAP_SERVERS", "BROKER://kafka:9092")
                .withEnv("SPRING_DATASOURCE_URL", "jdbc:postgresql://postgres:5432/test")
                .withEnv("SPRING_DATASOURCE_USERNAME", "test")
                .withEnv("SPRING_DATASOURCE_PASSWORD", "test")
                .withEnv("SPRING_CLOUD_AWS_CREDENTIALS_ACCESS-KEY", localStack.getAccessKey())
                .withEnv("SPRING_CLOUD_AWS_CREDENTIALS_SECRET-KEY", localStack.getSecretKey())
                .withEnv("SPRING_CLOUD_AWS_REGION_STATIC", localStack.getRegion())
                .withEnv("SPRING_CLOUD_AWS_ENDPOINT", "localstack:4566")
                .withExposedPorts(8080)
                .withNetwork(network)
                .waitingFor(Wait.forHttp("/actuator/health"));

//                .withCreateContainerCmdModifier(createContainerCmd -> {
//                            var hostConfig = new HostConfig();
//                            hostConfig.withMemory(1024L * 1024L * 1024L);
//                            hostConfig.withCpuCount(1L);
//                            createContainerCmd.withHostConfig(hostConfig);
//                        }
//                );

        Startables.deepStart(postgres, kafka, localStack).join();
        localStack.execInContainer("awslocal", "s3api", "create-bucket", "--bucket product-images");

        app.setPortBindings(List.of("8080:8080"));
        app.start();

        CloudflaredContainer cloudflaredContainer = new CloudflaredContainer(parse("cloudflare/cloudflared"), app.getMappedPort(8080));
        cloudflaredContainer.start();

        String publicUrl = cloudflaredContainer.getPublicUrl();

        System.out.println(publicUrl);


        new Scanner(System.in).nextLine();

    }
}
