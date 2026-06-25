package com.medicall.service;

import com.medicall.domain.Appointment;
import com.medicall.repository.AppointmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;

    public AppointmentService(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    public List<Appointment> listAll() {
        return appointmentRepository.findByStatusOrderByScheduledAtAsc("CONFIRMED");
    }

    public List<Appointment> listByPatient(Long patientId) {
        return appointmentRepository.findByPatientIdOrderByScheduledAtDesc(patientId);
    }

    @Transactional
    public Appointment create(Long patientId, String scheduledAt, String department, String notes) {
        Appointment appt = new Appointment();
        appt.setPatientId(patientId);
        appt.setScheduledAt(parseDateTime(scheduledAt));
        appt.setDepartment(department != null ? department : "内科");
        appt.setNotes(notes);
        appt.setStatus("CONFIRMED");
        return appointmentRepository.save(appt);
    }

    @Transactional
    public Optional<Appointment> reschedule(Long id, String newScheduledAt) {
        return appointmentRepository.findById(id).map(appt -> {
            appt.setScheduledAt(parseDateTime(newScheduledAt));
            appt.setUpdatedAt(Instant.now());
            return appointmentRepository.save(appt);
        });
    }

    @Transactional
    public Optional<Appointment> cancel(Long id) {
        return appointmentRepository.findById(id).map(appt -> {
            appt.setStatus("CANCELLED");
            appt.setUpdatedAt(Instant.now());
            return appointmentRepository.save(appt);
        });
    }

    public String formatConfirmation(Appointment appt) {
        return String.format("予約を承りました。%s %s のご予約です。",
                appt.getScheduledAt().toLocalDate(),
                appt.getScheduledAt().toLocalTime().withSecond(0).withNano(0));
    }

    private LocalDateTime parseDateTime(String value) {
        try {
            return LocalDateTime.parse(value.replace(" ", "T"));
        } catch (DateTimeParseException e) {
            return LocalDateTime.now().plusDays(3).withHour(10).withMinute(0);
        }
    }
}
