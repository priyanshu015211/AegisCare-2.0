package com.aegiscare.config;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.HashMap;
import java.util.Map;

/** Selects MySQL 8 for configured environments and a development-only H2 fallback. */
public class StorageDataSourceInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final int CONNECT_TIMEOUT_SECONDS = 5;
    private static final int MYSQL_STARTUP_ATTEMPTS = 12;
    private static final long MYSQL_RETRY_DELAY_MILLIS = 5_000L;
    private static final String MYSQL_PREFIX = "jdbc:mysql:";
    private static final String MYSQL_DRIVER = "com.mysql.cj.jdbc.Driver";

    @Override
    public void initialize(ConfigurableApplicationContext ctx) {
        ConfigurableEnvironment env = ctx.getEnvironment();
        String explicitUrl = env.getProperty("spring.datasource.url");
        String explicitUser = env.getProperty("spring.datasource.username");
        String explicitPassword = env.getProperty("spring.datasource.password");
        String explicitDriver = env.getProperty("spring.datasource.driver-class-name");
        boolean requireMysql = env.getProperty("aegiscare.storage.require-mysql", Boolean.class, false);
        Map<String, Object> properties = new HashMap<>();

        if (explicitUrl != null && !explicitUrl.isBlank()) {
            boolean mysqlUrl = explicitUrl.startsWith(MYSQL_PREFIX);
            if (requireMysql && (!mysqlUrl || isBlank(explicitUser) || isBlank(explicitPassword)
                    || !waitForMysql(explicitUrl, explicitUser, explicitPassword))) {
                throw new IllegalStateException(
                        "MySQL is required but the explicitly configured datasource is not a reachable MySQL database.");
            }
            properties.put("spring.datasource.url", explicitUrl);
            if (explicitUser != null) properties.put("spring.datasource.username", explicitUser);
            if (explicitPassword != null) properties.put("spring.datasource.password", explicitPassword);
            if (explicitDriver != null) {
                properties.put("spring.datasource.driver-class-name", explicitDriver);
            } else if (mysqlUrl) {
                properties.put("spring.datasource.driver-class-name", MYSQL_DRIVER);
            }
            properties.put("aegiscare.storage.mode", mysqlUrl ? "mysql" : "configured");
            properties.put("aegiscare.storage.detail", mysqlUrl
                    ? "Using explicitly configured MySQL datasource"
                    : "Using explicitly configured datasource");
            if (mysqlUrl) useMysqlMigrations(properties);
        } else {
            String url = env.getProperty("AEGISCARE_MYSQL_JDBC_URL");
            String user = env.getProperty("AEGISCARE_MYSQL_USERNAME");
            String password = env.getProperty("AEGISCARE_MYSQL_PASSWORD");
            boolean anyMysqlSetting = !isBlank(url) || !isBlank(user) || !isBlank(password);
            boolean configured = !isBlank(url) && !isBlank(user) && !isBlank(password);
            boolean reachable = configured && (requireMysql
                    ? waitForMysql(url, user, password)
                    : canConnect(url, user, password));

            if (reachable) {
                properties.put("spring.datasource.url", url);
                properties.put("spring.datasource.username", user);
                properties.put("spring.datasource.password", password);
                properties.put("spring.datasource.driver-class-name", MYSQL_DRIVER);
                properties.put("aegiscare.storage.mode", "mysql");
                properties.put("aegiscare.storage.detail", "Connected to configured MySQL database");
                useMysqlMigrations(properties);
            } else {
                if (requireMysql) {
                    String reason = configured
                            ? "MySQL is configured but cannot be reached"
                            : "MySQL is required but AEGISCARE_MYSQL_JDBC_URL, AEGISCARE_MYSQL_USERNAME and AEGISCARE_MYSQL_PASSWORD are not all configured";
                    throw new IllegalStateException(reason + ". Refusing to start with local fallback storage.");
                }
                try {
                    Path dataDirectory = Path.of(System.getProperty("user.dir"), "data").toAbsolutePath();
                    Files.createDirectories(dataDirectory);
                    String h2Url = "jdbc:h2:file:" + dataDirectory.resolve("aegiscare").toString().replace('\\', '/')
                            + ";DB_CLOSE_ON_EXIT=FALSE;MODE=PostgreSQL";
                    properties.put("spring.datasource.url", h2Url);
                    properties.put("spring.datasource.username", "sa");
                    properties.put("spring.datasource.password", "");
                    properties.put("spring.datasource.driver-class-name", "org.h2.Driver");
                    properties.put("aegiscare.storage.mode", "local-h2");
                    properties.put("aegiscare.storage.detail", anyMysqlSetting
                            ? "MySQL configuration is incomplete or unreachable; development-only local H2 fallback is active"
                            : "No MySQL configuration found; development-only local H2 fallback is active");
                } catch (Exception ex) {
                    throw new IllegalStateException("Could not prepare the local development database directory", ex);
                }
            }
        }

        // Hibernate must never silently mutate production schemas. Flyway owns schema changes.
        properties.put("spring.jpa.hibernate.ddl-auto", "validate");
        properties.put("spring.jpa.properties.hibernate.jdbc.time_zone", "UTC");
        env.getPropertySources().addFirst(new MapPropertySource("aegiscare-storage-resolution", properties));
    }

    private void useMysqlMigrations(Map<String, Object> properties) {
        // The MySQL migration chain is for a new, empty MySQL database. Do not
        // baseline an unknown pre-existing schema and then risk applying V2 to
        // tables with an incompatible shape. Existing data needs an explicit,
        // separately tested export/transform/import process.
        properties.put("spring.flyway.locations", "classpath:db/migration/mysql");
        properties.put("spring.flyway.baseline-on-migrate", false);
    }

    private boolean waitForMysql(String url, String user, String password) {
        for (int attempt = 1; attempt <= MYSQL_STARTUP_ATTEMPTS; attempt++) {
            if (canConnect(url, user, password)) return true;
            if (attempt < MYSQL_STARTUP_ATTEMPTS) {
                try {
                    Thread.sleep(MYSQL_RETRY_DELAY_MILLIS);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        return false;
    }

    private boolean canConnect(String url, String user, String password) {
        try {
            DriverManager.setLoginTimeout(CONNECT_TIMEOUT_SECONDS);
            try (Connection connection = DriverManager.getConnection(url, user, password)) {
                return connection.isValid(CONNECT_TIMEOUT_SECONDS);
            }
        } catch (Exception ex) {
            return false;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
