package com.medicall.api;

import com.medicall.api.dto.AppointmentView;
import com.medicall.api.dto.AdminStats;
import com.medicall.api.dto.CreateTenantRequest;
import com.medicall.api.dto.PatientDetail;
import com.medicall.api.dto.TenantView;
import com.medicall.domain.*;
import com.medicall.repository.CallSessionRepository;
import com.medicall.repository.CallTurnRepository;
import com.medicall.service.AdminStatsService;
import com.medicall.service.AppointmentService;
import com.medicall.service.CallExportService;
import com.medicall.service.FaqService;
import com.medicall.service.FaqKnowledgeSyncService;
import com.medicall.service.PatientService;
import com.medicall.service.TenantService;
import com.medicall.tenant.TenantContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
    private final PatientService patientService;
    private final CallExportService callExportService;
    private final TenantService tenantService;

    public AdminController(CallSessionRepository sessionRepository,
                           CallTurnRepository turnRepository,
                           FaqService faqService,
                           AppointmentService appointmentService,
                           AdminStatsService adminStatsService,
                           FaqKnowledgeSyncService knowledgeSync,
                           PatientService patientService,
                           CallExportService callExportService,
                           TenantService tenantService) {
        this.sessionRepository = sessionRepository;
        this.turnRepository = turnRepository;
        this.faqService = faqService;
        this.appointmentService = appointmentService;
        this.adminStatsService = adminStatsService;
        this.knowledgeSync = knowledgeSync;
        this.patientService = patientService;
        this.callExportService = callExportService;
        this.tenantService = tenantService;
    }

    private Long tenantId() {
        return TenantContext.requireTenantId();
    }

    @GetMapping("/tenant")
    public TenantView currentTenant() {
        Tenant t = tenantService.current();
        return toView(t);
    }

    @GetMapping("/tenants")
    public List<TenantView> listTenants() {
        return tenantService.listAll().stream().map(this::toView).toList();
    }

    @PostMapping("/tenants")
    public TenantView createTenant(@RequestBody CreateTenantRequest req) {
        Tenant created = tenantService.create(req);
        tenantService.executeWithTenantId(created.getId(), () -> {
            knowledgeSync.syncAll();
            return null;
        });
        return toView(created);
    }

    @GetMapping("/stats")
    public AdminStats stats() {
        return adminStatsService.getStats();
    }

    @GetMapping("/calls")
    public List<CallSession> listCalls() {
        return sessionRepository.findByTenantIdOrderByStartedAtDesc(tenantId());
    }

    @GetMapping("/calls/export")
    public ResponseEntity<byte[]> exportCalls(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        Instant toInstant = to != null ? Instant.parse(to) : Instant.now();
        Instant fromInstant = from != null
                ? Instant.parse(from)
                : toInstant.minus(30, ChronoUnit.DAYS);
        byte[] csv = callExportService.exportCsv(fromInstant, toInstant);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"calls.csv\"")
                .contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
                .body(csv);
    }

    @GetMapping("/calls/queue")
    public List<CallSession> queueCalls() {
        return adminStatsService.getAttentionQueue();
    }

    @GetMapping("/calls/{id}")
    public ResponseEntity<CallSession> getCall(@PathVariable UUID id) {
        return sessionRepository.findByIdAndTenantId(id, tenantId())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/calls/{id}/turns")
    public ResponseEntity<List<CallTurn>> getTurns(@PathVariable UUID id) {
        return sessionRepository.findByIdAndTenantId(id, tenantId())
                .map(s -> ResponseEntity.ok(turnRepository.findBySessionIdOrderByCreatedAtAsc(id)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/calls/active")
    public List<CallSession> activeCalls() {
        return sessionRepository.findByTenantIdAndStatusOrderByStartedAtDesc(tenantId(), "ACTIVE");
    }

    @GetMapping("/patients")
    public List<Patient> searchPatients(
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String name) {
        return patientService.search(phone, name);
    }

    @GetMapping("/patients/{id}")
    public ResponseEntity<PatientDetail> getPatient(@PathVariable Long id) {
        return patientService.getDetail(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
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

    private TenantView toView(Tenant t) {
        return new TenantView(t.getId(), t.getSlug(), t.getName(), t.getStatus(), t.getPlan(), t.getCreatedAt());
    }
}
