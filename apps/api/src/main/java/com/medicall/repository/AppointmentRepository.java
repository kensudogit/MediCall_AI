package com.medicall.repository;

import com.medicall.domain.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByPatientIdOrderByScheduledAtDesc(Long patientId);
    List<Appointment> findByStatusOrderByScheduledAtAsc(String status);
    List<Appointment> findAllByOrderByScheduledAtDesc();
}
