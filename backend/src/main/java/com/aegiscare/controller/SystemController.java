package com.aegiscare.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/system")
public class SystemController {
    private final JdbcTemplate jdbcTemplate;
    @Value("${aegiscare.storage.mode:unknown}") private String storageMode;
    @Value("${aegiscare.storage.detail:}") private String storageDetail;

    public SystemController(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }

    @GetMapping("/health")
    public Map<String, Object> health() {
        boolean connected;
        try {
            connected = Integer.valueOf(1).equals(jdbcTemplate.queryForObject("SELECT 1", Integer.class));
        } catch (Exception ex) {
            connected = false;
        }
        return Map.of("status", connected ? "ok" : "degraded", "storage", Map.of(
                "mode", storageMode, "detail", storageDetail, "connected", connected));
    }
}
