package com.portfolio.app.support;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

/**
 * Shared MariaDB test container using GenericContainer to avoid JdbcDatabaseContainer's
 * host-port-based JDBC readiness check, which fails inside devcontainers.
 *
 * Wait strategy: log message only.
 * Connection: container's internal bridge IP, bypassing host port mapping.
 */
public abstract class MariaDbContainerSupport {

    private static final String DB_NAME = "portfolio";
    private static final String DB_USER = "portfolio";
    private static final String DB_PASSWORD = "portfolio";

    @SuppressWarnings("resource")
    static final GenericContainer<?> MARIADB = new GenericContainer<>(DockerImageName.parse("mariadb:11.2"))
            .withEnv("MARIADB_DATABASE", DB_NAME)
            .withEnv("MARIADB_USER", DB_USER)
            .withEnv("MARIADB_PASSWORD", DB_PASSWORD)
            .withEnv("MARIADB_ROOT_PASSWORD", "root")
            .withExposedPorts(3306)
            .waitingFor(
                    Wait.forLogMessage(".*mariadbd: ready for connections.*", 1)
                            .withStartupTimeout(Duration.ofSeconds(120))
            );

    static {
        MARIADB.start();

        String ip = MARIADB.getContainerInfo()
                .getNetworkSettings()
                .getNetworks()
                .get("bridge")
                .getIpAddress();

        System.setProperty("spring.datasource.url", "jdbc:mariadb://" + ip + ":3306/" + DB_NAME);
        System.setProperty("spring.datasource.username", DB_USER);
        System.setProperty("spring.datasource.password", DB_PASSWORD);
        System.setProperty("spring.datasource.driver-class-name", "org.mariadb.jdbc.Driver");
    }
}
