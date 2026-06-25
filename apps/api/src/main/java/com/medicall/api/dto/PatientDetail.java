package com.medicall.api.dto;

import com.medicall.domain.CallSession;
import com.medicall.domain.Patient;
import java.util.List;

public record PatientDetail(
        Patient patient,
        List<CallSession> recentCalls,
        List<AppointmentView> appointments
) {}
