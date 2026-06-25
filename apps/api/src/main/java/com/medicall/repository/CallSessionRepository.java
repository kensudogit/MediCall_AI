package com.medicall.repository;

import com.medicall.domain.CallSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CallSessionRepository extends JpaRepository<CallSession, UUID> {
    Optional<CallSession> findByConnectContactId(String connectContactId);
    List<CallSession> findAllByOrderByStartedAtDesc();
    List<CallSession> findByStatusOrderByStartedAtDesc(String status);
}
