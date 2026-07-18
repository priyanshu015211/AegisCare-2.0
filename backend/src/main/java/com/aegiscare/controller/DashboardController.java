package com.aegiscare.controller;

import com.aegiscare.entity.Hospital;
import com.aegiscare.entity.TriageSession;
import com.aegiscare.repository.HospitalRepository;
import com.aegiscare.repository.TriageSessionRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final HospitalRepository hospitalRepository;
    private final TriageSessionRepository sessionRepository;

    public DashboardController(HospitalRepository hospitalRepository, TriageSessionRepository sessionRepository) {
        this.hospitalRepository = hospitalRepository;
        this.sessionRepository = sessionRepository;
    }

    @GetMapping("/summary")
    public Map<String, Object> summary() {
        List<Hospital> hospitals = hospitalRepository.findAll();
        int totalBeds = hospitals.stream().mapToInt(h -> nz(h.getTotalBeds())).sum();
        int occupied = hospitals.stream().mapToInt(h -> nz(h.getCurrentOccupancy())).sum();
        int loadPct = totalBeds == 0 ? 0 : (int) Math.round((occupied * 100.0) / totalBeds);

        List<TriageSession> sessions = sessionRepository.findTop25ByOrderByStartedAtDesc();
        long activeEmergencies = sessions.stream()
                .filter(s -> "escalated".equals(s.getStatus()) || "critical".equals(s.getSeverity()))
                .count();

        Map<String, Long> riskDistribution = sessions.stream()
                .collect(Collectors.groupingBy(TriageSession::getSeverity, LinkedHashMap::new, Collectors.counting()));
        for (String level : List.of("low", "medium", "high", "critical")) {
            riskDistribution.putIfAbsent(level, 0L);
        }

        List<Map<String, Object>> recentActivity = sessions.stream().limit(8).map(s -> Map.<String, Object>of(
                "patient_id", s.getPatientCode(),
                "severity", s.getSeverity(),
                "risk_score", s.getRiskScore(),
                "started_at", s.getStartedAt().toString()
        )).collect(Collectors.toList());

        return Map.of(
                "hospital_load_pct", loadPct,
                "hospitals_tracked", hospitals.size(),
                "active_emergencies", activeEmergencies,
                "total_sessions", sessions.size(),
                "risk_distribution", riskDistribution,
                "recent_activity", recentActivity
        );
    }

    private int nz(Integer i) { return i == null ? 0 : i; }
}
