package com.aegiscare.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "triage_sessions")
public class TriageSession {

    @Id
    private String id = UUID.randomUUID().toString();

    @Column(name = "patient_code")
    private String patientCode;

    @Column(name = "symptoms", length = 2000)
    private String symptomsCsv = "";

    private String duration;

    private String severity = "low";

    @Column(name = "risk_score")
    private Integer riskScore = 0;

    @Column(length = 4000)
    private String reasoning;

    @Column(name = "follow_up_question", length = 1000)
    private String followUpQuestion;

    @Column(name = "escalation_needed")
    private Boolean escalationNeeded = false;

    private String status = "active";

    @Column(name = "hospital_id", length = 255)
    private String hospitalId;

    @Column(name = "created_by_user_id", length = 36)
    private String createdByUserId;

    @Column(name = "started_at")
    private Instant startedAt = Instant.now();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getPatientCode() { return patientCode; }
    public void setPatientCode(String patientCode) { this.patientCode = patientCode; }
    public String getSymptomsCsv() { return symptomsCsv; }
    public void setSymptomsCsv(String symptomsCsv) { this.symptomsCsv = symptomsCsv; }
    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public Integer getRiskScore() { return riskScore; }
    public void setRiskScore(Integer riskScore) { this.riskScore = riskScore; }
    public String getReasoning() { return reasoning; }
    public void setReasoning(String reasoning) { this.reasoning = reasoning; }
    public String getFollowUpQuestion() { return followUpQuestion; }
    public void setFollowUpQuestion(String followUpQuestion) { this.followUpQuestion = followUpQuestion; }
    public Boolean getEscalationNeeded() { return escalationNeeded; }
    public void setEscalationNeeded(Boolean escalationNeeded) { this.escalationNeeded = escalationNeeded; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public String getHospitalId() { return hospitalId; }
    public void setHospitalId(String hospitalId) { this.hospitalId = hospitalId; }
    public String getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(String createdByUserId) { this.createdByUserId = createdByUserId; }
}
