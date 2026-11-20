package com.aegiscare.repository;

import com.aegiscare.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, String> {
    Optional<Patient> findByPatientCode(String patientCode);
    Optional<Patient> findByPatientCodeAndHospitalId(String patientCode, String hospitalId);
}
