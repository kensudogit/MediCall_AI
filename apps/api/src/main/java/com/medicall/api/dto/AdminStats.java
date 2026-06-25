package com.medicall.api.dto;

public record AdminStats(
        long patientCount,
        long appointmentCount,
        long confirmedAppointments,
        long callCount,
        long activeCalls,
        long transferredCalls,
        long emergencyCalls
) {}
