package com.aegiscare.controller;

import com.aegiscare.entity.TriageSession;
import com.aegiscare.repository.TriageSessionRepository;
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
    private static final DateTimeFormatter HOUR_FMT = DateTimeFormatter.ofPattern("HH:00").withZone(ZoneOffset.UTC);

    public AnalyticsController(TriageSessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @GetMapping("/volume-trend")
    public List<Map<String, Object>> volumeTrend() {
        List<TriageSession> sessions = sessionRepository.findAll();
        Map<String, Long> byHour = sessions.stream()
                .collect(Collectors.groupingBy(
                        s -> HOUR_FMT.format(s.getStartedAt()),
                        TreeMap::new,
                        Collectors.counting()
                ));
        List<Map<String, Object>> out = new ArrayList<>();
        byHour.forEach((hour, count) -> out.add(Map.of("hour", hour, "patients", count)));
        return out;
    }

    @GetMapping("/escalation-frequency")
    public Map<String, Long> escalationFrequency() {
        List<TriageSession> sessions = sessionRepository.findAll();
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("Low", 0L);
        counts.put("Medium", 0L);
        counts.put("High", 0L);
        counts.put("Critical", 0L);
        for (TriageSession s : sessions) {
            String key = switch (s.getSeverity()) {
                case "low" -> "Low";
                case "medium" -> "Medium";
                case "high" -> "High";
                case "critical" -> "Critical";
                default -> "Low";
            };
            counts.merge(key, 1L, Long::sum);
        }
        return counts;
    }
}
