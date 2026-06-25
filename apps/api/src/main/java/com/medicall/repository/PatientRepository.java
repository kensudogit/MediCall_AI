package com.medicall.repository;

import com.medicall.domain.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {
    Optional<Patient> findByPhoneNumber(String phoneNumber);
    Optional<Patient> findByFullNameAndDateOfBirthAndPhoneNumber(
            String fullName, LocalDate dateOfBirth, String phoneNumber);
}
