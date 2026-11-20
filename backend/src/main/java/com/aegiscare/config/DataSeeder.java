package com.aegiscare.config;

import com.aegiscare.entity.*;
import com.aegiscare.repository.*;
import com.aegiscare.service.RiskScoringService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@ConditionalOnProperty(name = "aegiscare.demo-data.enabled", havingValue = "true")
public class DataSeeder implements CommandLineRunner {

    private final HospitalRepository hospitalRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final TriageSessionRepository sessionRepository;
    private final AppointmentRepository appointmentRepository;
    private final RiskScoringService riskScoringService;

    public DataSeeder(HospitalRepository hospitalRepository, DoctorRepository doctorRepository,
                       PatientRepository patientRepository, TriageSessionRepository sessionRepository,
                       AppointmentRepository appointmentRepository, RiskScoringService riskScoringService) {
        this.hospitalRepository = hospitalRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.sessionRepository = sessionRepository;
        this.appointmentRepository = appointmentRepository;
        this.riskScoringService = riskScoringService;
    }

    @Override
    public void run(String... args) {
        if (hospitalRepository.count() == 0) {
            seedHospitals();
        }
        if (doctorRepository.count() == 0) {
            seedDoctors();
        }
        if (sessionRepository.count() == 0) {
            seedSessions();
        }
        if (appointmentRepository.count() == 0) {
            seedAppointments();
        }
    }

    private void seedHospitals() {
        hospitalRepository.saveAll(List.of(
                hospital("St. Augustine General", "Sholapur", 220, 168, 40, 31, 6),
                hospital("Riverside Medical Center", "Sholapur", 150, 95, 25, 14, 4),
                hospital("Northgate Community Hospital", "Sholapur", 90, 71, 15, 12, 2)
        ));
    }

    private Hospital hospital(String name, String city, int beds, int occ, int er, int erOcc, int amb) {
        Hospital h = new Hospital();
        h.setName(name);
        h.setCity(city);
        h.setTotalBeds(beds);
        h.setCurrentOccupancy(occ);
        h.setErCapacity(er);
        h.setErOccupancy(erOcc);
        h.setAmbulances(amb);
        return h;
    }

    private void seedDoctors() {
        List<Hospital> hospitals = hospitalRepository.findAll();
        String h0 = hospitals.get(0).getId();
        doctorRepository.saveAll(List.of(
                doctor("Dr. A. Whitfield", "Emergency Medicine", "ER", h0, "available"),
                doctor("Dr. R. Mehta", "Cardiology", "Cardiology", h0, "busy"),
                doctor("Dr. K. Osei", "Internal Medicine", "General", h0, "available"),
                doctor("Dr. L. Nakamura", "Pediatrics", "Pediatrics", h0, "offline"),
                doctor("Dr. S. Ibrahim", "Emergency Medicine", "ER", h0, "available"),
                doctor("Dr. T. Alvarez", "Pulmonology", "Respiratory", h0, "busy"),
                doctor("Dr. E. Larsen", "Neurology", "Neurology", h0, "available"),
                doctor("Dr. P. Gomez", "Orthopedics", "Ortho", h0, "offline"),
                doctor("Dr. N. Chowdhury", "General Surgery", "Surgery", h0, "busy")
        ));
    }

    private Doctor doctor(String name, String specialty, String dept, String hospitalId, String status) {
        Doctor d = new Doctor();
        d.setFullName(name);
        d.setSpecialty(specialty);
        d.setDepartment(dept);
        d.setHospitalId(hospitalId);
        d.setStatus(status);
        return d;
    }

    private void seedSessions() {
        seedOne("P-0987", List.of("chest pain", "shortness of breath", "dizziness"), "2 hours", 12);
        seedOne("P-0991", List.of("fever", "cough", "fatigue"), "3 days", 40);
        seedOne("P-0994", List.of("breathing difficulty", "chest pain"), "1 day", 5);
        seedOne("P-1198", List.of("headache", "nausea"), "6 hours", 90);
        seedOne("P-1201", List.of("chest pain", "sweating", "arm pain"), "45 minutes", 20);
        seedOne("P-1043", List.of("fever", "cough", "fatigue"), "2 days", 200);
        seedOne("P-1077", List.of("sore throat"), "1 day", 260);
        seedOne("P-1099", List.of("abdominal pain", "vomiting"), "5 hours", 340);
    }

    private void seedOne(String patientCode, List<String> symptoms, String duration, int minutesAgo) {
        patientRepository.findByPatientCode(patientCode).orElseGet(() -> {
            Patient p = new Patient();
            p.setPatientCode(patientCode);
            p.setFullName(patientCode);
            p.setHospitalId(primaryHospitalId());
            return patientRepository.save(p);
        });

        RiskScoringService.RiskResult result = riskScoringService.assess(symptoms, duration);

        TriageSession session = new TriageSession();
        session.setPatientCode(patientCode);
        session.setHospitalId(primaryHospitalId());
        session.setSymptomsCsv(String.join(", ", symptoms));
        session.setDuration(duration);
        session.setSeverity(result.severity());
        session.setRiskScore(result.riskScore());
        session.setReasoning(result.reasoning());
        session.setFollowUpQuestion(result.followUpQuestion());
        session.setEscalationNeeded(result.escalationNeeded());
        session.setStatus(result.escalationNeeded() ? "escalated" : "active");
        session.setStartedAt(Instant.now().minus(minutesAgo, ChronoUnit.MINUTES));
        sessionRepository.save(session);
    }

    private void seedAppointments() {
        List<Doctor> doctors = doctorRepository.findAll();
        String d0 = doctors.get(0).getId();
        String d1 = doctors.size() > 1 ? doctors.get(1).getId() : d0;

        appointmentRepository.saveAll(List.of(
                appointment("P-1201", d0, "video", "pending", 30),
                appointment("P-1198", d1, "in_person", "pending", 90),
                appointment("P-0994", d0, "in_person", "confirmed", 15),
                appointment("P-1077", d1, "phone", "pending", 180)
        ));
    }

    private Appointment appointment(String patientCode, String doctorId, String type, String status, int minutesFromNow) {
        Appointment a = new Appointment();
        a.setPatientCode(patientCode);
        a.setHospitalId(doctorRepository.findById(doctorId).map(Doctor::getHospitalId).orElse(null));
        a.setDoctorId(doctorId);
        a.setAppointmentType(type);
        a.setStatus(status);
        a.setScheduledAt(Instant.now().plus(minutesFromNow, ChronoUnit.MINUTES));
        return a;
    }

    private String primaryHospitalId() {
        return hospitalRepository.findAll().stream().findFirst().map(Hospital::getId).orElse(null);
    }
}
