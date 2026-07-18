package com.aegiscare.service;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class RiskScoringService {

    private static final Set<String> CRITICAL_SYMPTOMS = Set.of(
            "breathing difficulty", "chest pain", "shortness of breath"
    );

    public record RiskResult(int riskScore, String severity, String reasoning, String followUpQuestion, boolean escalationNeeded) {}

    public RiskResult assess(List<String> symptoms, String duration) {
        int score = 30;
        boolean hasCritical = symptoms.stream()
                .map(String::toLowerCase)
                .anyMatch(CRITICAL_SYMPTOMS::contains);

        if (hasCritical) score += 40;
        if (symptoms.size() >= 3) score += 15;
        if (duration != null && (duration.toLowerCase().contains("day") || duration.toLowerCase().contains("week"))) score += 10;

        score = Math.min(Math.max(score, 0), 100);
        String severity = severityFor(score);
        boolean escalate = score >= 75;

        String reasoning = buildReasoning(symptoms, duration, hasCritical, score, severity);
        String followUp = buildFollowUp(symptoms, hasCritical);

        return new RiskResult(score, severity, reasoning, followUp, escalate);
    }

    private String severityFor(int score) {
        if (score >= 85) return "critical";
        if (score >= 75) return "high";
        if (score >= 50) return "medium";
        return "low";
    }

    private String buildReasoning(List<String> symptoms, String duration, boolean hasCritical, int score, String severity) {
        StringBuilder sb = new StringBuilder();
        sb.append("Reported symptoms (").append(String.join(", ", symptoms)).append(") ");
        if (hasCritical) {
            sb.append("include at least one high-acuity symptom (e.g. chest pain or breathing difficulty), which materially raises risk. ");
        } else {
            sb.append("do not include a recognized high-acuity symptom on their own. ");
        }
        if (symptoms.size() >= 3) {
            sb.append("The number of concurrent symptoms (").append(symptoms.size()).append(") adds to the overall burden. ");
        }
        if (duration != null && !duration.isBlank()) {
            sb.append("Reported duration of \"").append(duration).append("\" was factored into the score. ");
        }
        sb.append("Composite risk score: ").append(score).append("/100, classified as ").append(severity).append(".");
        return sb.toString();
    }

    private String buildFollowUp(List<String> symptoms, boolean hasCritical) {
        if (hasCritical) {
            return "Is the chest pain or breathing difficulty getting worse, or accompanied by dizziness, sweating, or pain radiating to the arm or jaw?";
        }
        if (symptoms.stream().anyMatch(s -> s.toLowerCase().contains("fever"))) {
            return "How high has the fever measured, and has it responded to any medication taken so far?";
        }
        return "Has anything made the symptoms noticeably better or worse since they started?";
    }
}
