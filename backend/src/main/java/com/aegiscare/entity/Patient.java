package com.aegiscare.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "patients")
public class Patient {

    @Id
    private String id = UUID.randomUUID().toString();

    @Column(name = "patient_code", unique = true)
    private String patientCode;

    private String fullName;
    private Integer age;
    private String gender;
    private String phone;

    @Column(name = "known_conditions")
    private String knownConditionsCsv = "";

    @Column(name = "current_medications")
    private String currentMedicationsCsv = "";

    @Column(name = "allergies")
    private String allergiesCsv = "";

    @Column(name = "hospital_id", length = 255)
    private String hospitalId;

    @Column(name = "created_by_user_id", length = 36)
    private String createdByUserId;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getPatientCode() { return patientCode; }
    public void setPatientCode(String patientCode) { this.patientCode = patientCode; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getKnownConditionsCsv() { return knownConditionsCsv; }
    public void setKnownConditionsCsv(String v) { this.knownConditionsCsv = v; }
    public String getCurrentMedicationsCsv() { return currentMedicationsCsv; }
    public void setCurrentMedicationsCsv(String v) { this.currentMedicationsCsv = v; }
    public String getAllergiesCsv() { return allergiesCsv; }
    public void setAllergiesCsv(String v) { this.allergiesCsv = v; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public String getHospitalId() { return hospitalId; }
    public void setHospitalId(String hospitalId) { this.hospitalId = hospitalId; }
    public String getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(String createdByUserId) { this.createdByUserId = createdByUserId; }
}
