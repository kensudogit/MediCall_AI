package com.medicall.service;

import com.medicall.api.dto.AdminStats;
import com.medicall.repository.AppointmentRepository;
import com.medicall.repository.CallSessionRepository;
import com.medicall.repository.PatientRepository;
import org.springframework.stereotype.Service;

@Service
public class AdminStatsService {

    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final CallSessionRepository callSessionRepository;

    public AdminStatsService(PatientRepository patientRepository,
                             AppointmentRepository appointmentRepository,
                             CallSessionRepository callSessionRepository) {
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
        this.callSessionRepository = callSessionRepository;
    }

    public AdminStats getStats() {
        long calls = callSessionRepository.count();
        long active = callSessionRepository.findByStatusOrderByStartedAtDesc("ACTIVE").size();
        long transferred = callSessionRepository.findAll().stream().filter(s -> s.isTransferred()).count();
        long emergency = callSessionRepository.findAll().stream().filter(s -> s.isEmergencyFlag()).count();
        long confirmed = appointmentRepository.findByStatusOrderByScheduledAtAsc("CONFIRMED").size();

        return new AdminStats(
                patientRepository.count(),
                appointmentRepository.count(),
                confirmed,
                calls,
                active,
                transferred,
                emergency
        );
    }
}
