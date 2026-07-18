package com.aegiscare.controller;

import com.aegiscare.entity.Patient;
import com.aegiscare.entity.TriageSession;
import com.aegiscare.repository.PatientRepository;
import com.aegiscare.repository.TriageSessionRepository;
import com.aegiscare.service.RiskScoringService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
public class TriageController {

    private final RiskScoringService riskScoringService;
    private final PatientRepository patientRepository;
    private final TriageSessionRepository sessionRepository;

    public TriageController(RiskScoringService riskScoringService,
                             PatientRepository patientRepository,
                             TriageSessionRepository sessionRepository) {
        this.riskScoringService = riskScoringService;
        this.patientRepository = patientRepository;
        this.sessionRepository = sessionRepository;
    }

    public record AnalyzeRequest(
            @NotBlank String patientId,
            @NotEmpty List<String> symptoms,
            String duration
    ) {}

    @PostMapping("/ai/analyze")
    public Map<String, Object> analyze(@RequestBody AnalyzeRequest req) {
        RiskScoringService.RiskResult result = riskScoringService.assess(req.symptoms(), req.duration());

        patientRepository.findByPatientCode(req.patientId()).orElseGet(() -> {
            Patient p = new Patient();
            p.setPatientCode(req.patientId());
            p.setFullName(req.patientId());
            return patientRepository.save(p);
        });

        TriageSession session = new TriageSession();
        session.setPatientCode(req.patientId());
        session.setSymptomsCsv(String.join(", ", req.symptoms()));
        session.setDuration(req.duration());
        session.setSeverity(result.severity());
        session.setRiskScore(result.riskScore());
        session.setReasoning(result.reasoning());
        session.setFollowUpQuestion(result.followUpQuestion());
        session.setEscalationNeeded(result.escalationNeeded());
        session.setStatus(result.escalationNeeded() ? "escalated" : "active");
        session = sessionRepository.save(session);

        return Map.of(
                "analysis", Map.of(
                        "severity", result.severity(),
                        "risk_score", result.riskScore(),
                        "reasoning", result.reasoning(),
                        "follow_up_question", result.followUpQuestion(),
                        "escalation_needed", result.escalationNeeded()
                ),
                "current_state", Map.of(
                        "session_id", session.getId(),
                        "patient_id", req.patientId(),
                        "symptoms", req.symptoms(),
                        "duration", req.duration() == null ? "" : req.duration(),
                        "status", session.getStatus()
                )
        );
    }

    @GetMapping("/triage/sessions")
    public List<Map<String, Object>> recentSessions() {
        return sessionRepository.findTop25ByOrderByStartedAtDesc().stream()
                .map(this::toMap)
                .collect(Collectors.toList());
    }

    private Map<String, Object> toMap(TriageSession s) {
        return Map.of(
                "session_id", s.getId(),
                "patient_id", s.getPatientCode(),
                "symptoms", s.getSymptomsCsv(),
                "severity", s.getSeverity(),
                "risk_score", s.getRiskScore(),
                "status", s.getStatus(),
                "started_at", s.getStartedAt().toString()
        );
    }
}
