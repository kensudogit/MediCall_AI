package com.medicall.repository;

import com.medicall.domain.CallTurn;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface CallTurnRepository extends JpaRepository<CallTurn, Long> {
    List<CallTurn> findBySessionIdOrderByCreatedAtAsc(UUID sessionId);
}
