package com.aegiscare.repository;

import com.aegiscare.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, String> {
    List<Appointment> findTop25ByOrderByScheduledAtAsc();
    List<Appointment> findTop25ByHospitalIdOrderByScheduledAtAsc(String hospitalId);
}
