package com.aegiscare.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "appointments")
public class Appointment {

    @Id
    private String id = UUID.randomUUID().toString();

    @Column(name = "patient_code")
    private String patientCode;

    @Column(name = "doctor_id")
    private String doctorId;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    private String status = "pending"; // pending | confirmed | completed | cancelled

    @Column(name = "appointment_type")
    private String appointmentType = "in_person"; // in_person | video | phone

    @Column(name = "hospital_id", length = 255)
    private String hospitalId;

    @Column(name = "created_by_user_id", length = 36)
    private String createdByUserId;

    private String notes;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getPatientCode() { return patientCode; }
    public void setPatientCode(String patientCode) { this.patientCode = patientCode; }
    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }
    public Instant getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(Instant scheduledAt) { this.scheduledAt = scheduledAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getAppointmentType() { return appointmentType; }
    public void setAppointmentType(String appointmentType) { this.appointmentType = appointmentType; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getHospitalId() { return hospitalId; }
    public void setHospitalId(String hospitalId) { this.hospitalId = hospitalId; }
    public String getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(String createdByUserId) { this.createdByUserId = createdByUserId; }
}
