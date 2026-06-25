package com.medicall.api;

import com.medicall.domain.*;
import com.medicall.repository.CallSessionRepository;
import com.medicall.repository.CallTurnRepository;
import com.medicall.service.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/connect")
public class ConnectWebhookController {

    private final CallOrchestrationService orchestration;
    private final CallSummaryService summaryService;
    private final CallSessionRepository sessionRepository;
    private final GoogleSpeechService speechService;

    public ConnectWebhookController(CallOrchestrationService orchestration,
                                    CallSummaryService summaryService,
                                    CallSessionRepository sessionRepository,
                                    GoogleSpeechService speechService) {
        this.orchestration = orchestration;
        this.summaryService = summaryService;
        this.sessionRepository = sessionRepository;
        this.speechService = speechService;
    }

    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> start(@RequestBody Map<String, String> body) {
        String contactId = body.getOrDefault("contactId", UUID.randomUUID().toString());
        String phone = body.getOrDefault("callerPhone", body.get("phone"));
        var result = orchestration.startSession(contactId, phone);
        return ResponseEntity.ok(Map.of(
                "sessionId", result.session().getId().toString(),
                "status", result.session().getStatus(),
                "greeting", result.greeting()
        ));
    }

    @PostMapping("/utterance")
    public ResponseEntity<CallOrchestrationService.CallResponse> utterance(@RequestBody Map<String, String> body) {
        String audioText = body.get("utterance");
        if ((audioText == null || audioText.isBlank()) && body.containsKey("audioBase64")) {
            audioText = speechService.transcribeBase64(body.get("audioBase64"));
        }
        var req = new CallOrchestrationService.CallRequest(
                UUID.fromString(body.get("sessionId")),
                audioText != null ? audioText : "",
                body.get("callerPhone"),
                body.get("fullName"),
                body.get("dateOfBirth")
        );
        return ResponseEntity.ok(orchestration.processUtterance(req));
    }

    @PostMapping("/end")
    public ResponseEntity<Map<String, String>> end(@RequestBody Map<String, String> body) {
        UUID sessionId = UUID.fromString(body.get("sessionId"));
        String summary = summaryService.summarizeAndSave(sessionId);
        return ResponseEntity.ok(Map.of("summary", summary));
    }
}
