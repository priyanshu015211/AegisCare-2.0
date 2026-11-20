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

/** Selects PostgreSQL or an explicitly development-only local H2 fallback. */
public class StorageDataSourceInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final int CONNECT_TIMEOUT_SECONDS = 5;

    @Override
    public void initialize(ConfigurableApplicationContext ctx) {
        ConfigurableEnvironment env = ctx.getEnvironment();
        String explicitUrl = env.getProperty("spring.datasource.url");
        String explicitUser = env.getProperty("spring.datasource.username");
        String explicitPassword = env.getProperty("spring.datasource.password");
        String explicitDriver = env.getProperty("spring.datasource.driver-class-name");
        Map<String, Object> properties = new HashMap<>();
        boolean requirePostgres = env.getProperty("aegiscare.storage.require-postgres", Boolean.class, false);
        if (explicitUrl != null && !explicitUrl.isBlank()) {
            // Explicit datasource settings are useful for controlled test environments.
            if (requirePostgres && (!explicitUrl.startsWith("jdbc:postgresql:") || explicitUser == null
                    || explicitPassword == null || !canConnect(explicitUrl, explicitUser, explicitPassword))) {
                throw new IllegalStateException("PostgreSQL is required but the explicitly configured datasource is not a reachable PostgreSQL database.");
            }
            properties.put("spring.datasource.url", explicitUrl);
            if (explicitUser != null) properties.put("spring.datasource.username", explicitUser);
            if (explicitPassword != null) properties.put("spring.datasource.password", explicitPassword);
            if (explicitDriver != null) properties.put("spring.datasource.driver-class-name", explicitDriver);
            properties.put("aegiscare.storage.mode", explicitUrl.startsWith("jdbc:postgresql:") ? "supabase" : "configured");
            properties.put("aegiscare.storage.detail", "Using explicitly configured datasource");
        } else {
        String url = firstNonBlank(env.getProperty("SUPABASE_JDBC_URL"), System.getenv("SUPABASE_JDBC_URL"));
        String user = firstNonBlank(env.getProperty("SUPABASE_DB_USER"), System.getenv("SUPABASE_DB_USER"));
        String password = firstNonBlank(env.getProperty("SUPABASE_DB_PASSWORD"), System.getenv("SUPABASE_DB_PASSWORD"));
        boolean configured = url != null && user != null && password != null;
        boolean reachable = configured && canConnect(url, user, password);

        if (reachable) {
            properties.put("spring.datasource.url", url);
            properties.put("spring.datasource.username", user);
            properties.put("spring.datasource.password", password);
            properties.put("spring.datasource.driver-class-name", "org.postgresql.Driver");
            properties.put("aegiscare.storage.mode", "supabase");
            properties.put("aegiscare.storage.detail", "Connected to configured PostgreSQL database");
        } else {
            if (requirePostgres) {
                String reason = configured ? "PostgreSQL is configured but cannot be reached" :
                        "PostgreSQL is required but SUPABASE_JDBC_URL, SUPABASE_DB_USER and SUPABASE_DB_PASSWORD are not all configured";
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
                properties.put("aegiscare.storage.mode", "local-fallback");
                properties.put("aegiscare.storage.detail", configured
                        ? "PostgreSQL unreachable; local H2 fallback is active for development only"
                        : "No PostgreSQL configuration found; local H2 fallback is active for development only");
            } catch (Exception ex) {
                throw new IllegalStateException("Could not prepare the local development database directory", ex);
            }
        }
        }
        // Hibernate must never silently mutate production schemas. Flyway owns schema changes.
        properties.put("spring.jpa.hibernate.ddl-auto", "validate");
        env.getPropertySources().addFirst(new MapPropertySource("aegiscare-storage-resolution", properties));
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

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value;
        }
        return null;
    }
}
