package com.medicall.service;

import com.medicall.domain.CallIntent;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Locale;

@Service
public class EmergencyDetectionService {

    private static final List<String> EMERGENCY_KEYWORDS = List.of(
            "胸痛", "胸が痛", "意識", "意識がない", "意識障害", "呼吸困難", "息ができない",
            "呼吸が苦しい", "大量出血", "痙攣", "倒れた", "倒れ", "救急", "119",
            "心筋梗塞", "脳卒中", "くも膜下", "窒息"
    );

    public boolean isEmergency(String utterance) {
        if (utterance == null || utterance.isBlank()) return false;
        String normalized = utterance.toLowerCase(Locale.ROOT);
        return EMERGENCY_KEYWORDS.stream().anyMatch(normalized::contains);
    }

    public CallIntent emergencyIntent() {
        return CallIntent.EMERGENCY;
    }
}
