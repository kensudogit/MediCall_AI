package com.medicall.service;

import com.medicall.domain.CallIntent;
import org.springframework.stereotype.Service;
import java.util.Locale;
import java.util.Map;

@Service
public class IntentClassificationService {

    private static final Map<String, CallIntent> KEYWORD_MAP = Map.ofEntries(
            Map.entry("診療時間", CallIntent.HOURS),
            Map.entry("開院", CallIntent.HOURS),
            Map.entry("何時", CallIntent.HOURS),
            Map.entry("休診", CallIntent.HOLIDAY),
            Map.entry("休み", CallIntent.HOLIDAY),
            Map.entry("アクセス", CallIntent.ACCESS),
            Map.entry("行き方", CallIntent.ACCESS),
            Map.entry("駐車", CallIntent.ACCESS),
            Map.entry("持ち物", CallIntent.BELONGINGS),
            Map.entry("保険証", CallIntent.BELONGINGS),
            Map.entry("予約", CallIntent.APPOINTMENT_NEW),
            Map.entry("予約変更", CallIntent.APPOINTMENT_CHANGE),
            Map.entry("変更", CallIntent.APPOINTMENT_CHANGE),
            Map.entry("キャンセル", CallIntent.APPOINTMENT_CANCEL),
            Map.entry("検査", CallIntent.LAB),
            Map.entry("血液", CallIntent.LAB),
            Map.entry("会計", CallIntent.BILLING),
            Map.entry("料金", CallIntent.BILLING),
            Map.entry("薬", CallIntent.PHARMACY),
            Map.entry("処方", CallIntent.PHARMACY),
            Map.entry("紹介状", CallIntent.REFERRAL),
            Map.entry("苦情", CallIntent.COMPLAINT),
            Map.entry("怒", CallIntent.COMPLAINT),
            Map.entry("診断", CallIntent.MEDICAL_QUESTION),
            Map.entry("症状", CallIntent.MEDICAL_QUESTION),
            Map.entry("痛い", CallIntent.MEDICAL_QUESTION)
    );

    public CallIntent classify(String utterance) {
        if (utterance == null || utterance.isBlank()) return CallIntent.UNKNOWN;
        String text = utterance.toLowerCase(Locale.ROOT);

        if (text.contains("予約") && text.contains("変更")) return CallIntent.APPOINTMENT_CHANGE;
        if (text.contains("予約") && text.contains("キャンセル")) return CallIntent.APPOINTMENT_CANCEL;
        if (text.contains("予約") && (text.contains("取り") || text.contains("新規"))) return CallIntent.APPOINTMENT_NEW;

        for (var entry : KEYWORD_MAP.entrySet()) {
            if (text.contains(entry.getKey().toLowerCase(Locale.ROOT))) {
                return entry.getValue();
            }
        }
        return CallIntent.UNKNOWN;
    }
}
