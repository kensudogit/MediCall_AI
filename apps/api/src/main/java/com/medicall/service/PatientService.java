package com.medicall.service;

import com.medicall.api.dto.AppointmentView;
import com.medicall.api.dto.PatientDetail;
import com.medicall.domain.Patient;
import com.medicall.repository.AppointmentRepository;
import com.medicall.repository.CallSessionRepository;
import com.medicall.repository.PatientRepository;
import com.medicall.tenant.TenantContext;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class PatientService {

    private final PatientRepository patientRepository;
    private final CallSessionRepository sessionRepository;
    private final AppointmentRepository appointmentRepository;

    public PatientService(PatientRepository patientRepository,
                          CallSessionRepository sessionRepository,
                          AppointmentRepository appointmentRepository) {
        this.patientRepository = patientRepository;
        this.sessionRepository = sessionRepository;
        this.appointmentRepository = appointmentRepository;
    }

    private Long tenantId() {
        return TenantContext.requireTenantId();
    }

    public List<Patient> search(String phone, String name) {
        boolean hasPhone = phone != null && !phone.isBlank();
        boolean hasName = name != null && !name.isBlank();
        if (hasPhone && hasName) {
            String p = phone.trim();
            String n = name.trim();
            return patientRepository.findByTenantIdAndPhoneNumberContaining(tenantId(), p).stream()
                    .filter(pt -> pt.getFullName().toLowerCase().contains(n.toLowerCase()))
                    .toList();
        }
        if (hasPhone) {
            return patientRepository.findByTenantIdAndPhoneNumberContaining(tenantId(), phone.trim());
        }
        if (hasName) {
            return patientRepository.findByTenantIdAndFullNameContainingIgnoreCase(tenantId(), name.trim());
        }
        return patientRepository.findByTenantIdOrderByFullNameAsc(tenantId());
    }

    public Optional<PatientDetail> getDetail(Long id) {
        return patientRepository.findByIdAndTenantId(id, tenantId()).map(patient -> {
            var calls = sessionRepository.findByTenantIdAndPatientIdOrderByStartedAtDesc(tenantId(), patient.getId());
            var appointments = appointmentRepository.findByTenantIdAndPatientIdOrderByScheduledAtDesc(tenantId(), patient.getId())
                    .stream()
                    .map(a -> new AppointmentView(
                            a.getId(),
                            a.getPatientId(),
                            patient.getFullName(),
                            patient.getPhoneNumber(),
                            a.getScheduledAt(),
                            a.getDepartment(),
                            a.getStatus(),
                            a.getNotes()))
                    .toList();
            return new PatientDetail(patient, calls, appointments);
        });
    }
}
