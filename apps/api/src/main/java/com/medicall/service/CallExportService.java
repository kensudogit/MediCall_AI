package com.medicall.service;

import com.medicall.domain.CallSession;
import com.medicall.repository.CallSessionRepository;
import com.medicall.tenant.TenantContext;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class CallExportService {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("Asia/Tokyo"));

    private final CallSessionRepository sessionRepository;

    public CallExportService(CallSessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    public byte[] exportCsv(Instant from, Instant to) {
        Long tenantId = TenantContext.requireTenantId();
        List<CallSession> sessions = sessionRepository.findByTenantIdOrderByStartedAtDesc(tenantId).stream()
                .filter(s -> !s.getStartedAt().isBefore(from) && !s.getStartedAt().isAfter(to))
                .toList();

        StringBuilder sb = new StringBuilder();
        sb.append("開始日時,終了日時,電話番号,状態,意図,本人確認,緊急,転送,転送理由,要約\n");
        for (CallSession s : sessions) {
            sb.append(csv(FMT.format(s.getStartedAt()))).append(',');
            sb.append(csv(s.getEndedAt() != null ? FMT.format(s.getEndedAt()) : "")).append(',');
            sb.append(csv(s.getCallerPhone())).append(',');
            sb.append(csv(s.getStatus())).append(',');
            sb.append(csv(s.getIntent())).append(',');
            sb.append(s.isVerified() ? "済" : "未").append(',');
            sb.append(s.isEmergencyFlag() ? "はい" : "いいえ").append(',');
            sb.append(s.isTransferred() ? "はい" : "いいえ").append(',');
            sb.append(csv(s.getTransferReason())).append(',');
            sb.append(csv(s.getSummary())).append('\n');
        }
        byte[] bom = new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };
        byte[] body = sb.toString().getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[bom.length + body.length];
        System.arraycopy(bom, 0, result, 0, bom.length);
        System.arraycopy(body, 0, result, bom.length, body.length);
        return result;
    }

    private static String csv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
