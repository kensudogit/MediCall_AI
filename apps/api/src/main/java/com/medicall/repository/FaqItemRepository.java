package com.medicall.repository;

import com.medicall.domain.FaqItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FaqItemRepository extends JpaRepository<FaqItem, Long> {
    List<FaqItem> findByTenantIdAndActiveTrueOrderBySortOrderAsc(Long tenantId);
    List<FaqItem> findByTenantIdOrderBySortOrderAsc(Long tenantId);
    Optional<FaqItem> findByIdAndTenantId(Long id, Long tenantId);
}
