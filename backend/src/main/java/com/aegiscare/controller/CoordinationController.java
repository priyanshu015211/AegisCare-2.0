package com.aegiscare.controller;

import com.aegiscare.entity.Appointment;
import com.aegiscare.entity.Doctor;
import com.aegiscare.entity.TriageSession;
import com.aegiscare.repository.AppointmentRepository;
import com.aegiscare.repository.DoctorRepository;
import com.aegiscare.repository.TriageSessionRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/coordination")
public class CoordinationController {

    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final TriageSessionRepository sessionRepository;

    public CoordinationController(DoctorRepository doctorRepository,
                                   AppointmentRepository appointmentRepository,
                                   TriageSessionRepository sessionRepository) {
        this.doctorRepository = doctorRepository;
        this.appointmentRepository = appointmentRepository;
        this.sessionRepository = sessionRepository;
    }

    @GetMapping("/summary")
    public Map<String, Object> summary() {
        List<Doctor> doctors = doctorRepository.findAll();
        long available = doctors.stream().filter(d -> "available".equals(d.getStatus())).count();
        List<Appointment> pending = appointmentRepository.findTop25ByOrderByScheduledAtAsc().stream()
                .filter(a -> "pending".equals(a.getStatus())).collect(Collectors.toList());
        long activeEmergencies = sessionRepository.findTop10ByRiskScoreGreaterThanEqualOrderByRiskScoreDesc(70).size();

        return Map.of(
                "active_emergencies", activeEmergencies,
                "pending_appointments", pending.size(),
                "doctors_available", available,
                "doctors_total", doctors.size()
        );
    }

    @GetMapping("/queue")
    public List<Map<String, Object>> queue() {
        List<TriageSession> highRisk = sessionRepository.findTop10ByRiskScoreGreaterThanEqualOrderByRiskScoreDesc(75);
        return highRisk.stream().map(s -> Map.<String, Object>of(
                "patient_id", s.getPatientCode(),
                "risk_score", s.getRiskScore(),
                "action", s.getRiskScore() >= 85 ? "Video Consult" : "Book Appointment"
        )).collect(Collectors.toList());
    }

    @GetMapping("/doctors")
    public List<Map<String, Object>> doctors() {
        return doctorRepository.findAll().stream().map(d -> Map.<String, Object>of(
                "id", d.getId(),
                "full_name", d.getFullName(),
                "specialty", d.getSpecialty(),
                "department", d.getDepartment(),
                "status", d.getStatus()
        )).collect(Collectors.toList());
    }
}
