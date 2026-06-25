package com.medicall.api;

import com.medicall.domain.CallSession;
import com.medicall.repository.CallSessionRepository;
import com.medicall.service.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/connect")
public class ConnectWebhookController {

    private final CallOrchestrationService orchestration;
    private final CallSummaryService summaryService;
    private final CallSessionRepository sessionRepository;
    private final GoogleSpeechService speechService;
    private final TenantService tenantService;

    public ConnectWebhookController(CallOrchestrationService orchestration,
                                    CallSummaryService summaryService,
                                    CallSessionRepository sessionRepository,
                                    GoogleSpeechService speechService,
                                    TenantService tenantService) {
        this.orchestration = orchestration;
        this.summaryService = summaryService;
        this.sessionRepository = sessionRepository;
        this.speechService = speechService;
        this.tenantService = tenantService;
    }

    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> start(@RequestBody Map<String, String> body) {
        String tenantSlug = body.get("tenantSlug");
        if (tenantSlug != null && !tenantSlug.isBlank()) {
            return tenantService.executeWithTenant(tenantSlug, () -> ResponseEntity.ok(startSession(body)));
        }
        return ResponseEntity.ok(startSession(body));
    }

    private Map<String, Object> startSession(Map<String, String> body) {
        String contactId = body.getOrDefault("contactId", UUID.randomUUID().toString());
        String phone = body.getOrDefault("callerPhone", body.get("phone"));
        var result = orchestration.startSession(contactId, phone);
        return Map.of(
                "sessionId", result.session().getId().toString(),
                "status", result.session().getStatus(),
                "greeting", result.greeting(),
                "tenantSlug", tenantService.current().getSlug()
        );
    }

    @PostMapping("/utterance")
    public ResponseEntity<CallOrchestrationService.CallResponse> utterance(@RequestBody Map<String, String> body) {
        UUID sessionId = UUID.fromString(body.get("sessionId"));
        CallSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));

        return tenantService.executeWithTenantId(session.getTenantId(), () -> {
            String audioText = body.get("utterance");
            if ((audioText == null || audioText.isBlank()) && body.containsKey("audioBase64")) {
                audioText = speechService.transcribeBase64(body.get("audioBase64"));
            }
            var req = new CallOrchestrationService.CallRequest(
                    sessionId,
                    audioText != null ? audioText : "",
                    body.get("callerPhone"),
                    body.get("fullName"),
                    body.get("dateOfBirth")
            );
            return ResponseEntity.ok(orchestration.processUtterance(req));
        });
    }

    @PostMapping("/end")
    public ResponseEntity<Map<String, String>> end(@RequestBody Map<String, String> body) {
        UUID sessionId = UUID.fromString(body.get("sessionId"));
        CallSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));

        return tenantService.executeWithTenantId(session.getTenantId(), () -> {
            String summary = summaryService.summarizeAndSave(sessionId);
            return ResponseEntity.ok(Map.of("summary", summary));
        });
    }
}
