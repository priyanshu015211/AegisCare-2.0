package com.aegiscare.config;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.HashMap;
import java.util.Map;

/**
 * Decides where AegisCare persists its data, BEFORE Spring Boot wires up
 * the DataSource / JPA auto-configuration.
 *
 * Priority:
 *   1. Supabase (Postgres) - used when SUPABASE_JDBC_URL (+ credentials) is
 *      set AND a real connection can be opened within a short timeout.
 *   2. Local fallback - a file-backed H2 database stored under ./data/.
 *      This means the app always has durable storage, even with no cloud
 *      configuration at all, and nothing is silently dropped.
 *
 * The chosen mode is exposed as the "aegiscare.storage.mode" property so
 * SystemController can report it via /api/v1/system/health.
 */
public class StorageDataSourceInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final int CONNECT_TIMEOUT_SECONDS = 3;

    @Override
    public void initialize(ConfigurableApplicationContext ctx) {
        ConfigurableEnvironment env = ctx.getEnvironment();

        String supabaseUrl = firstNonBlank(
                System.getenv("SUPABASE_JDBC_URL"),
                env.getProperty("SUPABASE_JDBC_URL")
        );
        String supabaseUser = firstNonBlank(
                System.getenv("SUPABASE_DB_USER"),
                env.getProperty("SUPABASE_DB_USER"),
                "postgres"
        );
        String supabasePassword = firstNonBlank(
                System.getenv("SUPABASE_DB_PASSWORD"),
                env.getProperty("SUPABASE_DB_PASSWORD")
        );

        Map<String, Object> props = new HashMap<>();

        boolean supabaseConfigured = supabaseUrl != null && !supabaseUrl.isBlank();
        boolean supabaseReachable = supabaseConfigured
                && canConnect(supabaseUrl, supabaseUser, supabasePassword);

        if (supabaseReachable) {
            props.put("spring.datasource.url", supabaseUrl);
            props.put("spring.datasource.username", supabaseUser);
            props.put("spring.datasource.password", supabasePassword);
            props.put("spring.datasource.driver-class-name", "org.postgresql.Driver");
            props.put("spring.jpa.hibernate.ddl-auto", "update");
            props.put("aegiscare.storage.mode", "supabase");
            props.put("aegiscare.storage.detail", "Connected to Supabase Postgres");
        } else {
            String dataDir = System.getProperty("user.dir") + "/data";
            String h2Url = "jdbc:h2:file:" + dataDir + "/aegiscare;DB_CLOSE_ON_EXIT=FALSE;MODE=PostgreSQL";
            props.put("spring.datasource.url", h2Url);
            props.put("spring.datasource.username", "sa");
            props.put("spring.datasource.password", "");
            props.put("spring.datasource.driver-class-name", "org.h2.Driver");
            props.put("spring.jpa.hibernate.ddl-auto", "update");
            props.put("aegiscare.storage.mode", "local-fallback");
            props.put("aegiscare.storage.detail",
                    supabaseConfigured
                            ? "Supabase configured but unreachable - persisting to local file database at " + dataDir
                            : "No Supabase configuration found - persisting to local file database at " + dataDir);
        }

        env.getPropertySources().addFirst(new MapPropertySource("aegiscare-storage-resolution", props));
    }

    private boolean canConnect(String url, String user, String password) {
        try {
            DriverManager.setLoginTimeout(CONNECT_TIMEOUT_SECONDS);
            try (Connection c = DriverManager.getConnection(url, user, password)) {
                return c.isValid(CONNECT_TIMEOUT_SECONDS);
            }
        } catch (Exception ex) {
            return false;
        }
    }

    private String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }
}
