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
 * Connection strategy:
 *  - Inside a Docker container (DinD / devcontainer): use the container's bridge IP directly.
 *    The bridge network is reachable from within Docker, and host port mappings are not.
 *  - On host OS (e.g., Mac with Docker Desktop): Docker runs inside a VM so the bridge IP
 *    is unreachable. Use the host-mapped port on localhost instead.
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

        String host;
        int port;
        if (new java.io.File("/.dockerenv").exists()) {
            // Inside a Docker container: bridge IP is directly reachable.
            host = POSTGRES.getContainerInfo()
                    .getNetworkSettings()
                    .getNetworks()
                    .get("bridge")
                    .getIpAddress();
            port = 5432;
        } else {
            // Host OS: Docker runs in a VM; use the mapped port on localhost.
            host = POSTGRES.getHost();
            port = POSTGRES.getMappedPort(5432);
        }

        System.setProperty("spring.datasource.url", "jdbc:postgresql://" + host + ":" + port + "/" + DB_NAME);
        System.setProperty("spring.datasource.username", DB_USER);
        System.setProperty("spring.datasource.password", DB_PASSWORD);
        System.setProperty("spring.datasource.driver-class-name", "org.postgresql.Driver");
    }
}
