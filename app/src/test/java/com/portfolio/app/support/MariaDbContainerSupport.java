package com.portfolio.app.support;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

/**
 * Shared MariaDB test container using GenericContainer to avoid JdbcDatabaseContainer's
 * host-port-based JDBC readiness check, which fails inside devcontainers.
 *
 * Wait strategy: log message + port listening.
 *   MariaDB Docker init starts a temporary server (socket-only) that logs
 *   "mariadbd: ready for connections" before the real TCP server is up.
 *   Combining the log check with forListeningPort() ensures we wait until
 *   port 3306 is actually accepting TCP connections (real server ready).
 *
 * Connection strategy:
 *  - Inside a Docker container (DinD / devcontainer): use the container's bridge IP directly.
 *    The bridge network is reachable from within Docker, and host port mappings are not.
 *  - On host OS (e.g., Mac with Docker Desktop): Docker runs inside a VM so the bridge IP
 *    is unreachable. Use the host-mapped port on localhost instead.
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
                    new WaitAllStrategy(WaitAllStrategy.Mode.WITH_MAXIMUM_OUTER_TIMEOUT)
                            .withStrategy(Wait.forLogMessage(".*mariadbd: ready for connections.*", 1))
                            .withStrategy(Wait.forListeningPort())
                            .withStartupTimeout(Duration.ofSeconds(120))
            );

    static {
        MARIADB.start();

        String host;
        int port;
        if (new java.io.File("/.dockerenv").exists()) {
            // Inside a Docker container: bridge IP is directly reachable.
            host = MARIADB.getContainerInfo()
                    .getNetworkSettings()
                    .getNetworks()
                    .get("bridge")
                    .getIpAddress();
            port = 3306;
        } else {
            // Host OS: Docker runs in a VM; use the mapped port on localhost.
            host = MARIADB.getHost();
            port = MARIADB.getMappedPort(3306);
        }

        System.setProperty("spring.datasource.url", "jdbc:mariadb://" + host + ":" + port + "/" + DB_NAME);
        System.setProperty("spring.datasource.username", DB_USER);
        System.setProperty("spring.datasource.password", DB_PASSWORD);
        System.setProperty("spring.datasource.driver-class-name", "org.mariadb.jdbc.Driver");
    }
}
