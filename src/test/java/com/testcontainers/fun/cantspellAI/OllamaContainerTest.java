package com.testcontainers.fun.cantspellAI;


import org.junit.jupiter.api.Test;
import org.testcontainers.ollama.OllamaContainer;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

public class OllamaContainerTest {

    @Test
    public void withDefaultConfig() {
        try ( // container {
              OllamaContainer ollama = new OllamaContainer("ollama/ollama:0.1.40")
              // }
        ) {
            ollama.start();

            String version = given()
                    .baseUri(ollama.getEndpoint())
                    .get("/api/version")
                    .jsonPath()
                    .get("version");

            assertThat(version).isEqualTo("0.1.40");
        }
    }
}