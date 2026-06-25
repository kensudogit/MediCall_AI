package com.medicall.api.dto;

import java.util.List;

public record AdminStats(
        long patientCount,
        long appointmentCount,
        long confirmedAppointments,
        long callCount,
        long activeCalls,
        long transferredCalls,
        long emergencyCalls,
        List<IntentCount> intentCounts,
        List<DailyCallCount> callsLast7Days
) {}
