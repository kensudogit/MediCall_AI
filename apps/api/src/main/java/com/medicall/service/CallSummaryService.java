package com.medicall.service;

import com.medicall.domain.CallSession;
import com.medicall.domain.CallTurn;
import com.medicall.repository.CallSessionRepository;
import com.medicall.repository.CallTurnRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CallSummaryService {

    private final CallSessionRepository sessionRepository;
    private final CallTurnRepository turnRepository;
    private final OpenAiService openAiService;

    public CallSummaryService(CallSessionRepository sessionRepository,
                              CallTurnRepository turnRepository,
                              OpenAiService openAiService) {
        this.sessionRepository = sessionRepository;
        this.turnRepository = turnRepository;
        this.openAiService = openAiService;
    }

    @Transactional
    public String summarizeAndSave(UUID sessionId) {
        List<CallTurn> turns = turnRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        List<String> lines = turns.stream()
                .map(t -> t.getRole() + ": " + t.getContent())
                .collect(Collectors.toList());
        String summary = openAiService.summarizeCall(lines);

        sessionRepository.findById(sessionId).ifPresent(s -> {
            s.setSummary(summary);
            s.setStatus("ENDED");
            s.setEndedAt(Instant.now());
            sessionRepository.save(s);
        });
        return summary;
    }
}
