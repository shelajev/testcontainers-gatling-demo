package com.testcontainers.catalog;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;

public class ReproduceMe {

    public static void main(String[] args) {

        PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer("postgres:15-alpine");

        postgreSQLContainer.start();
        postgreSQLContainer.getJdbcUrl();

        GenericContainer redis = new GenericContainer("redis:6-alpine")
                .withExposedPorts(6379);

        redis.start();

        System.out.println(redis.getHost() + ":" + redis.getMappedPort(6379));

        redis.stop();
    }
}
