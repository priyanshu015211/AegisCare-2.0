package com.aegiscare.repository;

import com.aegiscare.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DoctorRepository extends JpaRepository<Doctor, String> {
    List<Doctor> findByHospitalId(String hospitalId);
}
