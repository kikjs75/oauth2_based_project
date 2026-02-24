package com.portfolio.app.support;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

/**
 * Shared PostgreSQL test container using GenericContainer to avoid JdbcDatabaseContainer's
 * host-port-based JDBC readiness check, which fails inside devcontainers.
 *
 * Wait strategy: log message only.
 * Connection: container's internal bridge IP, bypassing host port mapping.
 */
public abstract class PostgresContainerSupport {

    private static final String DB_NAME = "portfolio";
    private static final String DB_USER = "portfolio";
    private static final String DB_PASSWORD = "portfolio";

    @SuppressWarnings("resource")
    static final GenericContainer<?> POSTGRES = new GenericContainer<>(DockerImageName.parse("postgres:16.2"))
            .withEnv("POSTGRES_DB", DB_NAME)
            .withEnv("POSTGRES_USER", DB_USER)
            .withEnv("POSTGRES_PASSWORD", DB_PASSWORD)
            .withExposedPorts(5432)
            .waitingFor(
                    Wait.forLogMessage(".*database system is ready to accept connections.*", 2)
                            .withStartupTimeout(Duration.ofSeconds(120))
            );

    static {
        POSTGRES.start();

        String ip = POSTGRES.getContainerInfo()
                .getNetworkSettings()
                .getNetworks()
                .get("bridge")
                .getIpAddress();

        System.setProperty("spring.datasource.url", "jdbc:postgresql://" + ip + ":5432/" + DB_NAME);
        System.setProperty("spring.datasource.username", DB_USER);
        System.setProperty("spring.datasource.password", DB_PASSWORD);
        System.setProperty("spring.datasource.driver-class-name", "org.postgresql.Driver");
    }
}
