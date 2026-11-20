package com.aegiscare.controller;

import com.aegiscare.entity.TriageSession;
import com.aegiscare.repository.TriageSessionRepository;
import com.aegiscare.security.CurrentUserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/emergency")
public class EmergencyController {
    private final TriageSessionRepository sessionRepository;
    private final CurrentUserService currentUser;

    public EmergencyController(TriageSessionRepository sessionRepository, CurrentUserService currentUser) {
        this.sessionRepository = sessionRepository;
        this.currentUser = currentUser;
    }

    @GetMapping("/high-risk")
    public List<Map<String, Object>> highRisk() {
        List<TriageSession> sessions = currentUser.isAdmin()
                ? sessionRepository.findTop10ByRiskScoreGreaterThanEqualOrderByRiskScoreDesc(70)
                : sessionRepository.findTop10ByHospitalIdAndRiskScoreGreaterThanEqualOrderByRiskScoreDesc(currentUser.requireHospitalId(), 70);
        return sessions.stream().map(s -> Map.<String, Object>of(
                "patient_id", s.getPatientCode() == null ? "" : s.getPatientCode(),
                "risk_score", s.getRiskScore() == null ? 0 : s.getRiskScore(),
                "severity", s.getSeverity() == null ? "unknown" : s.getSeverity(),
                "status", statusLabel(s), "last_update", relativeTime(s.getStartedAt())
        )).toList();
    }

    private String statusLabel(TriageSession session) {
        if ("critical".equals(session.getSeverity())) return "Critical";
        if (Boolean.TRUE.equals(session.getEscalationNeeded())) return "Escalated";
        return "Under Review";
    }

    private String relativeTime(Instant startedAt) {
        long minutes = Duration.between(startedAt, Instant.now()).toMinutes();
        if (minutes < 1) return "just now";
        if (minutes < 60) return minutes + " min ago";
        return (minutes / 60) + " hr ago";
    }
}
