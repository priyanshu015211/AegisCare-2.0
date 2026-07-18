package com.aegiscare.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "doctors")
public class Doctor {

    @Id
    private String id = UUID.randomUUID().toString();

    private String fullName;
    private String specialty;
    private String department;

    @Column(name = "hospital_id")
    private String hospitalId;

    private String status = "available"; // available | busy | offline

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getSpecialty() { return specialty; }
    public void setSpecialty(String specialty) { this.specialty = specialty; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getHospitalId() { return hospitalId; }
    public void setHospitalId(String hospitalId) { this.hospitalId = hospitalId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
