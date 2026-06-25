package com.medicall.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EmergencyDetectionServiceTest {

    private final EmergencyDetectionService service = new EmergencyDetectionService();

    @Test
    void detectsChestPain() {
        assertTrue(service.isEmergency("胸が痛くて息もできません"));
    }

    @Test
    void ignoresNormalQuery() {
        assertFalse(service.isEmergency("診療時間を教えてください"));
    }
}
