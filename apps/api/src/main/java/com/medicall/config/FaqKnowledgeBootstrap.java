package com.medicall.config;

import com.medicall.domain.Tenant;
import com.medicall.repository.TenantRepository;
import com.medicall.service.FaqKnowledgeSyncService;
import com.medicall.service.TenantService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class FaqKnowledgeBootstrap {

    private final FaqKnowledgeSyncService syncService;
    private final TenantRepository tenantRepository;
    private final TenantService tenantService;

    public FaqKnowledgeBootstrap(FaqKnowledgeSyncService syncService,
                                 TenantRepository tenantRepository,
                                 TenantService tenantService) {
        this.syncService = syncService;
        this.tenantRepository = tenantRepository;
        this.tenantService = tenantService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        for (Tenant tenant : tenantRepository.findAll()) {
            try {
                tenantService.executeWithTenantId(tenant.getId(), () -> {
                    syncService.syncAll();
                    return null;
                });
            } catch (Exception ex) {
                System.err.println("[medicall] FAQ sync skipped for " + tenant.getSlug() + ": " + ex.getMessage());
            }
        }
    }
}
