package com.aegiscare.controller;

import com.aegiscare.entity.TriageSession;
import com.aegiscare.repository.TriageSessionRepository;
import com.aegiscare.security.CurrentUserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {
    private final TriageSessionRepository sessionRepository;
    private final CurrentUserService currentUser;
    private static final DateTimeFormatter HOUR_FMT = DateTimeFormatter.ofPattern("HH:00").withZone(ZoneOffset.UTC);

    public AnalyticsController(TriageSessionRepository sessionRepository, CurrentUserService currentUser) {
        this.sessionRepository = sessionRepository;
        this.currentUser = currentUser;
    }

    private List<TriageSession> scopedSessions() {
        return currentUser.isAdmin() ? sessionRepository.findAll()
                : sessionRepository.findByHospitalId(currentUser.requireHospitalId());
    }

    @GetMapping("/volume-trend")
    public List<Map<String, Object>> volumeTrend() {
        Map<String, Long> byHour = scopedSessions().stream().collect(Collectors.groupingBy(
                s -> HOUR_FMT.format(s.getStartedAt()), TreeMap::new, Collectors.counting()));
        List<Map<String, Object>> out = new ArrayList<>();
        byHour.forEach((hour, count) -> out.add(Map.of("hour", hour, "patients", count)));
        return out;
    }

    @GetMapping("/escalation-frequency")
    public Map<String, Long> escalationFrequency() {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("Low", 0L); counts.put("Medium", 0L); counts.put("High", 0L); counts.put("Critical", 0L);
        for (TriageSession session : scopedSessions()) {
            String key = switch (session.getSeverity() == null ? "" : session.getSeverity().toLowerCase()) {
                case "medium" -> "Medium"; case "high" -> "High"; case "critical" -> "Critical"; default -> "Low";
            };
            counts.merge(key, 1L, Long::sum);
        }
        return counts;
    }
}
