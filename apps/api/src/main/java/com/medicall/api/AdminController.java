package com.medicall.api;

import com.medicall.api.dto.AppointmentView;
import com.medicall.api.dto.AdminStats;
import com.medicall.domain.*;
import com.medicall.repository.CallSessionRepository;
import com.medicall.repository.CallTurnRepository;
import com.medicall.service.AdminStatsService;
import com.medicall.service.AppointmentService;
import com.medicall.service.FaqService;
import com.medicall.service.FaqKnowledgeSyncService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final CallSessionRepository sessionRepository;
    private final CallTurnRepository turnRepository;
    private final FaqService faqService;
    private final AppointmentService appointmentService;
    private final AdminStatsService adminStatsService;
    private final FaqKnowledgeSyncService knowledgeSync;

    public AdminController(CallSessionRepository sessionRepository,
                           CallTurnRepository turnRepository,
                           FaqService faqService,
                           AppointmentService appointmentService,
                           AdminStatsService adminStatsService,
                           FaqKnowledgeSyncService knowledgeSync) {
        this.sessionRepository = sessionRepository;
        this.turnRepository = turnRepository;
        this.faqService = faqService;
        this.appointmentService = appointmentService;
        this.adminStatsService = adminStatsService;
        this.knowledgeSync = knowledgeSync;
    }

    @GetMapping("/stats")
    public AdminStats stats() {
        return adminStatsService.getStats();
    }

    @GetMapping("/calls")
    public List<CallSession> listCalls() {
        return sessionRepository.findAllByOrderByStartedAtDesc();
    }

    @GetMapping("/calls/{id}")
    public ResponseEntity<CallSession> getCall(@PathVariable UUID id) {
        return sessionRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/calls/{id}/turns")
    public List<CallTurn> getTurns(@PathVariable UUID id) {
        return turnRepository.findBySessionIdOrderByCreatedAtAsc(id);
    }

    @GetMapping("/calls/active")
    public List<CallSession> activeCalls() {
        return sessionRepository.findByStatusOrderByStartedAtDesc("ACTIVE");
    }

    @GetMapping("/faq")
    public List<FaqItem> listFaq() {
        return faqService.listAll();
    }

    @PostMapping("/faq")
    public FaqItem createFaq(@RequestBody FaqItem item) {
        return faqService.save(item);
    }

    @PutMapping("/faq/{id}")
    public ResponseEntity<FaqItem> updateFaq(@PathVariable Long id, @RequestBody FaqItem item) {
        return faqService.findById(id).map(existing -> {
            existing.setCategory(item.getCategory());
            existing.setQuestion(item.getQuestion());
            existing.setAnswer(item.getAnswer());
            existing.setActive(item.isActive());
            existing.setSortOrder(item.getSortOrder());
            return ResponseEntity.ok(faqService.save(existing));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/faq/{id}")
    public ResponseEntity<Void> deleteFaq(@PathVariable Long id) {
        faqService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/faq/reindex")
    public ResponseEntity<Map<String, String>> reindexFaq() {
        knowledgeSync.syncAll();
        return ResponseEntity.ok(Map.of("status", "ok", "message", "FAQ knowledge reindexed"));
    }

    @GetMapping("/clinic")
    public ClinicSettings getClinic() {
        return faqService.getClinicSettings();
    }

    @PutMapping("/clinic")
    public ClinicSettings updateClinic(@RequestBody ClinicSettings settings) {
        ClinicSettings current = faqService.getClinicSettings();
        if (settings.getClinicName() != null) current.setClinicName(settings.getClinicName());
        if (settings.getHoursText() != null) current.setHoursText(settings.getHoursText());
        if (settings.getHolidaysText() != null) current.setHolidaysText(settings.getHolidaysText());
        if (settings.getAccessText() != null) current.setAccessText(settings.getAccessText());
        if (settings.getBelongingsText() != null) current.setBelongingsText(settings.getBelongingsText());
        return faqService.updateClinicSettings(current);
    }

    @GetMapping("/appointments")
    public List<AppointmentView> listAppointments() {
        return appointmentService.listAllForAdmin();
    }
}
