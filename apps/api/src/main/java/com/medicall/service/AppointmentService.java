package com.medicall.service;

import com.medicall.domain.Appointment;
import com.medicall.domain.Patient;
import com.medicall.repository.AppointmentRepository;
import com.medicall.repository.PatientRepository;
import com.medicall.api.dto.AppointmentView;
import com.medicall.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;

    public AppointmentService(AppointmentRepository appointmentRepository,
                              PatientRepository patientRepository) {
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
    }

    private Long tenantId() {
        return TenantContext.requireTenantId();
    }

    public List<Appointment> listAll() {
        return appointmentRepository.findByTenantIdAndStatusOrderByScheduledAtAsc(tenantId(), "CONFIRMED");
    }

    public List<AppointmentView> listAllForAdmin() {
        Map<Long, Patient> patients = patientRepository.findByTenantIdOrderByFullNameAsc(tenantId()).stream()
                .collect(Collectors.toMap(Patient::getId, p -> p));
        return appointmentRepository.findByTenantIdOrderByScheduledAtDesc(tenantId()).stream()
                .map(a -> toView(a, patients.get(a.getPatientId())))
                .toList();
    }

    private AppointmentView toView(Appointment a, Patient p) {
        return new AppointmentView(
                a.getId(),
                a.getPatientId(),
                p != null ? p.getFullName() : null,
                p != null ? p.getPhoneNumber() : null,
                a.getScheduledAt(),
                a.getDepartment(),
                a.getStatus(),
                a.getNotes()
        );
    }

    public List<Appointment> listByPatient(Long patientId) {
        return appointmentRepository.findByTenantIdAndPatientIdOrderByScheduledAtDesc(tenantId(), patientId);
    }

    @Transactional
    public Appointment create(Long patientId, String scheduledAt, String department, String notes) {
        Appointment appt = new Appointment();
        appt.setTenantId(tenantId());
        appt.setPatientId(patientId);
        appt.setScheduledAt(parseDateTime(scheduledAt));
        appt.setDepartment(department != null ? department : "内科");
        appt.setNotes(notes);
        appt.setStatus("CONFIRMED");
        return appointmentRepository.save(appt);
    }

    @Transactional
    public Optional<Appointment> reschedule(Long id, String newScheduledAt) {
        return appointmentRepository.findByIdAndTenantId(id, tenantId()).map(appt -> {
            appt.setScheduledAt(parseDateTime(newScheduledAt));
            appt.setUpdatedAt(Instant.now());
            return appointmentRepository.save(appt);
        });
    }

    @Transactional
    public Optional<Appointment> cancel(Long id) {
        return appointmentRepository.findByIdAndTenantId(id, tenantId()).map(appt -> {
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
