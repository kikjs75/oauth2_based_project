package com.portfolio.app.support;

import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Shared Testcontainers MariaDB instance (reused across all integration tests via static field).
 */
public abstract class MariaDbContainerSupport {

    static final MariaDBContainer<?> MARIADB = new MariaDBContainer<>(DockerImageName.parse("mariadb:11.2"))
            .withDatabaseName("portfolio")
            .withUsername("portfolio")
            .withPassword("portfolio");

    static {
        MARIADB.start();
        System.setProperty("spring.datasource.url", MARIADB.getJdbcUrl());
        System.setProperty("spring.datasource.username", MARIADB.getUsername());
        System.setProperty("spring.datasource.password", MARIADB.getPassword());
        System.setProperty("spring.datasource.driver-class-name", "org.mariadb.jdbc.Driver");
    }
}
