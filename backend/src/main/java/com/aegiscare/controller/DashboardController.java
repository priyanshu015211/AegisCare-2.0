package com.aegiscare.controller;

import com.aegiscare.entity.Hospital;
import com.aegiscare.entity.TriageSession;
import com.aegiscare.repository.HospitalRepository;
import com.aegiscare.repository.TriageSessionRepository;
import com.aegiscare.security.CurrentUserService;
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
    private final CurrentUserService currentUser;

    public DashboardController(HospitalRepository hospitalRepository, TriageSessionRepository sessionRepository,
                               CurrentUserService currentUser) {
        this.hospitalRepository = hospitalRepository;
        this.sessionRepository = sessionRepository;
        this.currentUser = currentUser;
    }

    @GetMapping("/summary")
    public Map<String, Object> summary() {
        List<Hospital> hospitals = currentUser.isAdmin()
                ? hospitalRepository.findAll()
                : hospitalRepository.findById(currentUser.requireHospitalId()).stream().toList();
        List<TriageSession> sessions = currentUser.isAdmin()
                ? sessionRepository.findTop25ByOrderByStartedAtDesc()
                : sessionRepository.findTop25ByHospitalIdOrderByStartedAtDesc(currentUser.requireHospitalId());

        int totalBeds = hospitals.stream().mapToInt(h -> nz(h.getTotalBeds())).sum();
        int occupied = hospitals.stream().mapToInt(h -> nz(h.getCurrentOccupancy())).sum();
        int loadPct = totalBeds == 0 ? 0 : (int) Math.round((occupied * 100.0) / totalBeds);
        long activeEmergencies = sessions.stream()
                .filter(s -> "escalated".equals(s.getStatus()) || "critical".equals(s.getSeverity())).count();
        Map<String, Long> riskDistribution = sessions.stream()
                .collect(Collectors.groupingBy(TriageSession::getSeverity, LinkedHashMap::new, Collectors.counting()));
        for (String level : List.of("low", "medium", "high", "critical")) riskDistribution.putIfAbsent(level, 0L);

        List<Map<String, Object>> recentActivity = sessions.stream().limit(8).map(s -> Map.<String, Object>of(
                "patient_id", safe(s.getPatientCode()), "severity", safe(s.getSeverity()),
                "risk_score", nz(s.getRiskScore()), "started_at", s.getStartedAt().toString()
        )).toList();
        return Map.of("hospital_load_pct", loadPct, "hospitals_tracked", hospitals.size(),
                "active_emergencies", activeEmergencies, "total_sessions", sessions.size(),
                "risk_distribution", riskDistribution, "recent_activity", recentActivity);
    }

    private int nz(Integer value) { return value == null ? 0 : value; }
    private String safe(String value) { return value == null ? "" : value; }
}
