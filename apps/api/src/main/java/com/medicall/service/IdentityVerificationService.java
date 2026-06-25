package com.medicall.service;

import com.medicall.domain.Patient;
import com.medicall.repository.PatientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Optional;

@Service
public class IdentityVerificationService {

    private final PatientRepository patientRepository;

    public IdentityVerificationService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    public record IdentityInput(String fullName, String dateOfBirth, String phoneNumber) {}

    public record VerificationResult(boolean success, String message, Long patientId) {}

    @Transactional
    public VerificationResult verify(IdentityInput input) {
        if (isBlank(input.fullName()) || isBlank(input.dateOfBirth()) || isBlank(input.phoneNumber())) {
            return new VerificationResult(false, "氏名・生年月日・電話番号をすべてお伝えください。", null);
        }

        LocalDate dob;
        try {
            dob = LocalDate.parse(input.dateOfBirth().replace("/", "-"));
        } catch (DateTimeParseException e) {
            return new VerificationResult(false, "生年月日の形式が正しくありません。例: 1980-01-15", null);
        }

        String phone = normalizePhone(input.phoneNumber());
        Optional<Patient> existing = patientRepository.findByFullNameAndDateOfBirthAndPhoneNumber(
                input.fullName().trim(), dob, phone);

        if (existing.isPresent()) {
            Patient p = existing.get();
            p.setVerified(true);
            patientRepository.save(p);
            return new VerificationResult(true, "本人確認が完了しました。", p.getId());
        }

        Optional<Patient> byPhone = patientRepository.findByPhoneNumber(phone);
        if (byPhone.isPresent()) {
            return new VerificationResult(false, "登録情報と一致しません。職員におつなぎします。", null);
        }

        Patient patient = new Patient();
        patient.setFullName(input.fullName().trim());
        patient.setDateOfBirth(dob);
        patient.setPhoneNumber(phone);
        patient.setVerified(true);
        patient = patientRepository.save(patient);
        return new VerificationResult(true, "新規登録と本人確認が完了しました。", patient.getId());
    }

    private String normalizePhone(String phone) {
        return phone.replaceAll("[^0-9+]", "");
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
