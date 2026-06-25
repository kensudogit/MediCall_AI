package com.medicall.repository;

import com.medicall.domain.CallSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CallSessionRepository extends JpaRepository<CallSession, UUID> {
    Optional<CallSession> findByTenantIdAndConnectContactId(Long tenantId, String connectContactId);
    List<CallSession> findByTenantIdOrderByStartedAtDesc(Long tenantId);
    List<CallSession> findByTenantIdAndStatusOrderByStartedAtDesc(Long tenantId, String status);
    List<CallSession> findByTenantIdAndPatientIdOrderByStartedAtDesc(Long tenantId, Long patientId);
    Optional<CallSession> findByIdAndTenantId(UUID id, Long tenantId);
    long countByTenantId(Long tenantId);
}
