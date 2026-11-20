package com.aegiscare.controller;

import com.aegiscare.entity.Hospital;
import com.aegiscare.entity.Patient;
import com.aegiscare.entity.TriageSession;
import com.aegiscare.repository.HospitalRepository;
import com.aegiscare.repository.PatientRepository;
import com.aegiscare.repository.TriageSessionRepository;
import com.aegiscare.security.CurrentUserService;
import com.aegiscare.service.RiskScoringService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class TriageController {
    private final RiskScoringService riskScoringService;
    private final PatientRepository patientRepository;
    private final TriageSessionRepository sessionRepository;
    private final HospitalRepository hospitalRepository;
    private final CurrentUserService currentUser;

    public TriageController(RiskScoringService riskScoringService, PatientRepository patientRepository,
                            TriageSessionRepository sessionRepository, HospitalRepository hospitalRepository,
                            CurrentUserService currentUser) {
        this.riskScoringService = riskScoringService;
        this.patientRepository = patientRepository;
        this.sessionRepository = sessionRepository;
        this.hospitalRepository = hospitalRepository;
        this.currentUser = currentUser;
    }

    public record AnalyzeRequest(
            @NotBlank @Size(max = 64) String patientId,
            @NotEmpty @Size(max = 12) List<@NotBlank @Size(max = 100) String> symptoms,
            @Size(max = 100) String duration,
            @Size(max = 120) String patientName,
            @Size(max = 36) String hospitalId
    ) { }

    @PostMapping("/ai/analyze")
    @Transactional
    public Map<String, Object> analyze(@Valid @RequestBody AnalyzeRequest request) {
        String hospitalId = resolveHospitalId(request.hospitalId());
        String patientCode = request.patientId().trim();

        Patient patient = patientRepository.findByPatientCode(patientCode).orElse(null);
        if (patient == null) {
            patient = new Patient();
            patient.setPatientCode(patientCode);
            patient.setFullName(request.patientName() == null || request.patientName().isBlank()
                    ? patientCode : request.patientName().trim());
            patient.setHospitalId(hospitalId);
            patient.setCreatedByUserId(currentUser.userId());
            patient = patientRepository.save(patient);
        } else if (!hospitalId.equals(patient.getHospitalId())) {
            // Do not reveal whether a patient code exists at another facility.
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient record not found in your facility");
        }

        List<String> symptoms = request.symptoms().stream().map(String::trim).filter(s -> !s.isBlank()).toList();
        if (symptoms.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one symptom is required");
        RiskScoringService.RiskResult result = riskScoringService.assess(symptoms, request.duration());

        TriageSession session = new TriageSession();
        session.setPatientCode(patientCode);
        session.setHospitalId(hospitalId);
        session.setCreatedByUserId(currentUser.userId());
        session.setSymptomsCsv(String.join(", ", symptoms));
        session.setDuration(request.duration());
        session.setSeverity(result.severity());
        session.setRiskScore(result.riskScore());
        session.setReasoning(result.reasoning());
        session.setFollowUpQuestion(result.followUpQuestion());
        session.setEscalationNeeded(result.escalationNeeded());
        session.setStatus(result.escalationNeeded() ? "escalated" : "active");
        session = sessionRepository.save(session);

        return Map.of(
                "analysis", Map.of("severity", result.severity(), "risk_score", result.riskScore(),
                        "reasoning", result.reasoning(), "follow_up_question", result.followUpQuestion(),
                        "escalation_needed", result.escalationNeeded()),
                "current_state", Map.of("session_id", session.getId(), "patient_id", patientCode,
                        "symptoms", symptoms, "duration", request.duration() == null ? "" : request.duration(),
                        "status", session.getStatus())
        );
    }

    @GetMapping("/triage/sessions")
    public List<Map<String, Object>> recentSessions() {
        List<TriageSession> sessions = currentUser.isAdmin()
                ? sessionRepository.findTop25ByOrderByStartedAtDesc()
                : sessionRepository.findTop25ByHospitalIdOrderByStartedAtDesc(currentUser.requireHospitalId());
        return sessions.stream().map(this::toMap).toList();
    }

    private String resolveHospitalId(String requestedHospitalId) {
        if (!currentUser.isAdmin()) return currentUser.requireHospitalId();
        if (requestedHospitalId != null && !requestedHospitalId.isBlank()) {
            if (!hospitalRepository.existsById(requestedHospitalId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The selected hospital does not exist");
            }
            return requestedHospitalId;
        }
        return hospitalRepository.findAll().stream().findFirst().map(Hospital::getId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Create a hospital before recording a triage session"));
    }

    private Map<String, Object> toMap(TriageSession session) {
        return Map.of("session_id", session.getId(), "patient_id", session.getPatientCode(),
                "symptoms", session.getSymptomsCsv(), "severity", session.getSeverity(),
                "risk_score", session.getRiskScore(), "status", session.getStatus(),
                "started_at", session.getStartedAt().toString());
    }
}
