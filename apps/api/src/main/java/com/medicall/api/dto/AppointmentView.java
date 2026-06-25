package com.medicall.api.dto;

import java.time.LocalDateTime;

public record AppointmentView(
        Long id,
        Long patientId,
        String patientName,
        String patientPhone,
        LocalDateTime scheduledAt,
        String department,
        String status,
        String notes
) {}
