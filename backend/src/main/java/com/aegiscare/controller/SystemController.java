package com.aegiscare.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/system")
public class SystemController {

    @Value("${aegiscare.storage.mode:unknown}")
    private String storageMode;

    @Value("${aegiscare.storage.detail:}")
    private String storageDetail;

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "ok",
                "storage", Map.of(
                        "mode", storageMode,
                        "detail", storageDetail,
                        "connected", true
                )
        );
    }
}
