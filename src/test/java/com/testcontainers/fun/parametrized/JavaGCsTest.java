package com.testcontainers.fun.parametrized;



import com.github.dockerjava.api.model.HostConfig;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.startupcheck.IndefiniteWaitOneShotStartupCheckStrategy;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

public class JavaGCsTest {

    @CsvSource({
            ".*SerialGC.*true.*, 1791",
            ".*G1GC.*true.*, 1792"}
    )
    @ParameterizedTest
    void whichGCWillJavaChoose(String gcRegex, long memoryLimitInMB) throws IOException {
        try (var container =
                     new GenericContainer<>("eclipse-temurin:17-jdk")
                             .withCreateContainerCmdModifier(createContainerCmd -> {
                                         var hostConfig = new HostConfig();
                                         hostConfig.withMemory(memoryLimitInMB * 1024L * 1024L);
                                         hostConfig.withCpuCount(1L);
                                         createContainerCmd.withHostConfig(hostConfig);
                                     }
                             )
                             .withCommand("java -XX:+PrintFlagsFinal -version && sleep infinity")
                             .withStartupCheckStrategy(new IndefiniteWaitOneShotStartupCheckStrategy())
        ) {
            container.start();
            var logs = container.getLogs();
            Assertions.assertThat(logs).containsPattern(gcRegex);
        }
    }

    @TestFactory
    Collection<DynamicTest> dynamicTestsWithCollection() {
        return Arrays.asList(
                DynamicTest.dynamicTest("TestSerialGC",
                        () -> whichGCWillJavaChoose(".*SerialGC.*true.*", 1791)),
                DynamicTest.dynamicTest("TestG1GC",
                        () ->  whichGCWillJavaChoose(".*G1GC.*true.*", 1792)));
    }
}