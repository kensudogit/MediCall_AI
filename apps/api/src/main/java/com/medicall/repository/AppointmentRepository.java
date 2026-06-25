package com.medicall.repository;

import com.medicall.domain.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByTenantIdAndPatientIdOrderByScheduledAtDesc(Long tenantId, Long patientId);
    List<Appointment> findByTenantIdAndStatusOrderByScheduledAtAsc(Long tenantId, String status);
    List<Appointment> findByTenantIdOrderByScheduledAtDesc(Long tenantId);
    Optional<Appointment> findByIdAndTenantId(Long id, Long tenantId);
    long countByTenantId(Long tenantId);
}
