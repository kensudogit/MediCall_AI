package com.medicall.config;

import com.medicall.service.FaqKnowledgeSyncService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class FaqKnowledgeBootstrap {

    private final FaqKnowledgeSyncService syncService;

    public FaqKnowledgeBootstrap(FaqKnowledgeSyncService syncService) {
        this.syncService = syncService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        syncService.syncAll();
    }
}
