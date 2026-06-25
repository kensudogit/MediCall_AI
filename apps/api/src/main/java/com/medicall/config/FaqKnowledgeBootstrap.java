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
        try {
            syncService.syncAll();
        } catch (Exception ex) {
            // pgvector / OpenAI 未設定でも通話 API は動作させる
            System.err.println("[medicall] FAQ knowledge sync skipped: " + ex.getMessage());
        }
    }
}
