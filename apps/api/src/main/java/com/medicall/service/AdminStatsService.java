package com.medicall.service;

import com.medicall.api.dto.AdminStats;
import com.medicall.api.dto.DailyCallCount;
import com.medicall.api.dto.IntentCount;
import com.medicall.domain.CallSession;
import com.medicall.repository.AppointmentRepository;
import com.medicall.repository.CallSessionRepository;
import com.medicall.repository.PatientRepository;
import com.medicall.tenant.TenantContext;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminStatsService {

    private static final ZoneId JST = ZoneId.of("Asia/Tokyo");

    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final CallSessionRepository callSessionRepository;

    public AdminStatsService(PatientRepository patientRepository,
                             AppointmentRepository appointmentRepository,
                             CallSessionRepository callSessionRepository) {
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
        this.callSessionRepository = callSessionRepository;
    }

    private Long tenantId() {
        return TenantContext.requireTenantId();
    }

    public AdminStats getStats() {
        Long tid = tenantId();
        List<CallSession> allCalls = callSessionRepository.findByTenantIdOrderByStartedAtDesc(tid);
        long calls = allCalls.size();
        long active = allCalls.stream().filter(s -> "ACTIVE".equals(s.getStatus())).count();
        long transferred = allCalls.stream().filter(CallSession::isTransferred).count();
        long emergency = allCalls.stream().filter(CallSession::isEmergencyFlag).count();
        long confirmed = appointmentRepository.findByTenantIdAndStatusOrderByScheduledAtAsc(tid, "CONFIRMED").size();

        List<IntentCount> intentCounts = allCalls.stream()
                .filter(s -> s.getIntent() != null && !s.getIntent().isBlank())
                .collect(Collectors.groupingBy(CallSession::getIntent, Collectors.counting()))
                .entrySet().stream()
                .map(e -> new IntentCount(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingLong(IntentCount::count).reversed())
                .toList();

        LocalDate today = LocalDate.now(JST);
        Map<LocalDate, Long> dailyMap = allCalls.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getStartedAt().atZone(JST).toLocalDate(),
                        Collectors.counting()));

        List<DailyCallCount> last7 = java.util.stream.IntStream.rangeClosed(0, 6)
                .mapToObj(i -> today.minusDays(6 - i))
                .map(d -> new DailyCallCount(d.toString(), dailyMap.getOrDefault(d, 0L)))
                .toList();

        return new AdminStats(
                patientRepository.countByTenantId(tid),
                appointmentRepository.countByTenantId(tid),
                confirmed,
                calls,
                active,
                transferred,
                emergency,
                intentCounts,
                last7
        );
    }

    public List<CallSession> getAttentionQueue() {
        return callSessionRepository.findByTenantIdAndStatusOrderByStartedAtDesc(tenantId(), "ACTIVE").stream()
                .sorted(Comparator
                        .comparing(CallSession::isEmergencyFlag).reversed()
                        .thenComparing(CallSession::isTransferred).reversed()
                        .thenComparing(CallSession::getStartedAt))
                .toList();
    }
}
