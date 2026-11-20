package com.aegiscare.controller;

import com.aegiscare.entity.Appointment;
import com.aegiscare.entity.Doctor;
import com.aegiscare.entity.TriageSession;
import com.aegiscare.repository.AppointmentRepository;
import com.aegiscare.repository.DoctorRepository;
import com.aegiscare.repository.TriageSessionRepository;
import com.aegiscare.security.CurrentUserService;
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
    private final CurrentUserService currentUser;

    public CoordinationController(DoctorRepository doctorRepository, AppointmentRepository appointmentRepository,
                                  TriageSessionRepository sessionRepository, CurrentUserService currentUser) {
        this.doctorRepository = doctorRepository;
        this.appointmentRepository = appointmentRepository;
        this.sessionRepository = sessionRepository;
        this.currentUser = currentUser;
    }

    private List<Doctor> doctorsInScope() {
        return currentUser.isAdmin() ? doctorRepository.findAll() : doctorRepository.findByHospitalId(currentUser.requireHospitalId());
    }
    private List<Appointment> appointmentsInScope() {
        return currentUser.isAdmin() ? appointmentRepository.findTop25ByOrderByScheduledAtAsc()
                : appointmentRepository.findTop25ByHospitalIdOrderByScheduledAtAsc(currentUser.requireHospitalId());
    }
    private List<TriageSession> highRiskInScope() {
        return currentUser.isAdmin() ? sessionRepository.findTop10ByRiskScoreGreaterThanEqualOrderByRiskScoreDesc(70)
                : sessionRepository.findTop10ByHospitalIdAndRiskScoreGreaterThanEqualOrderByRiskScoreDesc(currentUser.requireHospitalId(), 70);
    }

    @GetMapping("/summary")
    public Map<String, Object> summary() {
        List<Doctor> doctors = doctorsInScope();
        long available = doctors.stream().filter(d -> "available".equals(d.getStatus())).count();
        long pending = appointmentsInScope().stream().filter(a -> "pending".equals(a.getStatus())).count();
        return Map.of("active_emergencies", highRiskInScope().size(), "pending_appointments", pending,
                "doctors_available", available, "doctors_total", doctors.size());
    }

    @GetMapping("/queue")
    public List<Map<String, Object>> queue() {
        return highRiskInScope().stream().map(s -> Map.<String, Object>of(
                "patient_id", s.getPatientCode() == null ? "" : s.getPatientCode(),
                "risk_score", s.getRiskScore() == null ? 0 : s.getRiskScore(),
                "action", s.getRiskScore() != null && s.getRiskScore() >= 85 ? "Video Consult" : "Book Appointment"
        )).collect(Collectors.toList());
    }

    @GetMapping("/doctors")
    public List<Map<String, Object>> doctors() {
        return doctorsInScope().stream().map(d -> Map.<String, Object>of(
                "id", d.getId(), "full_name", d.getFullName(), "specialty", d.getSpecialty(),
                "department", d.getDepartment(), "status", d.getStatus()
        )).toList();
    }
}
