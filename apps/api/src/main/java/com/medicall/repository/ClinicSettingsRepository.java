package com.medicall.repository;

import com.medicall.domain.ClinicSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ClinicSettingsRepository extends JpaRepository<ClinicSettings, Long> {
    Optional<ClinicSettings> findByTenantId(Long tenantId);
}
