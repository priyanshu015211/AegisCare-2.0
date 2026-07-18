package com.aegiscare.controller;

import com.aegiscare.entity.TriageSession;
import com.aegiscare.repository.TriageSessionRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/emergency")
public class EmergencyController {

    private final TriageSessionRepository sessionRepository;

    public EmergencyController(TriageSessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @GetMapping("/high-risk")
    public List<Map<String, Object>> highRisk() {
        List<TriageSession> sessions = sessionRepository.findTop10ByRiskScoreGreaterThanEqualOrderByRiskScoreDesc(70);
        return sessions.stream().map(s -> Map.<String, Object>of(
                "patient_id", s.getPatientCode(),
                "risk_score", s.getRiskScore(),
                "severity", s.getSeverity(),
                "status", statusLabel(s),
                "last_update", relativeTime(s.getStartedAt())
        )).collect(Collectors.toList());
    }

    private String statusLabel(TriageSession s) {
        if ("critical".equals(s.getSeverity())) return "Critical";
        if (Boolean.TRUE.equals(s.getEscalationNeeded())) return "Escalated";
        return "Under Review";
    }

    private String relativeTime(Instant startedAt) {
        long minutes = Duration.between(startedAt, Instant.now()).toMinutes();
        if (minutes < 1) return "just now";
        if (minutes < 60) return minutes + " min ago";
        long hours = minutes / 60;
        return hours + " hr ago";
    }
}
