package com.medicall.service;

import com.medicall.domain.CallIntent;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class IntentClassificationServiceTest {

    private final IntentClassificationService service = new IntentClassificationService();

    @Test
    void classifiesHours() {
        assertEquals(CallIntent.HOURS, service.classify("診療時間を教えてください"));
    }

    @Test
    void classifiesUnknownAsFaq() {
        assertEquals(CallIntent.FAQ, service.classify("初診の流れを教えてください"));
    }
}
