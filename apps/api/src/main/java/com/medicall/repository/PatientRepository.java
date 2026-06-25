package com.medicall.repository;

import com.medicall.domain.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {
    Optional<Patient> findByTenantIdAndPhoneNumber(Long tenantId, String phoneNumber);
    Optional<Patient> findByTenantIdAndFullNameAndDateOfBirthAndPhoneNumber(
            Long tenantId, String fullName, LocalDate dateOfBirth, String phoneNumber);
    List<Patient> findByTenantIdAndPhoneNumberContaining(Long tenantId, String phoneNumber);
    List<Patient> findByTenantIdAndFullNameContainingIgnoreCase(Long tenantId, String fullName);
    List<Patient> findByTenantIdOrderByFullNameAsc(Long tenantId);
    Optional<Patient> findByIdAndTenantId(Long id, Long tenantId);
    long countByTenantId(Long tenantId);
}
