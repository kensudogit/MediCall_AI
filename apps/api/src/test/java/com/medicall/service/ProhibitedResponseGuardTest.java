package com.medicall.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProhibitedResponseGuardTest {

    private final ProhibitedResponseGuard guard = new ProhibitedResponseGuard();

    @Test
    void blocksDiagnosis() {
        assertTrue(guard.isProhibited("おそらくインフルエンザです"));
    }

    @Test
    void allowsGeneralInfo() {
        assertFalse(guard.isProhibited("診療時間は9時から18時です"));
    }
}
