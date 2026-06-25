package com.medicall.service;

import com.medicall.domain.ClinicSettings;
import com.medicall.domain.FaqItem;
import com.medicall.repository.ClinicSettingsRepository;
import com.medicall.repository.FaqItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class FaqService {

    private final FaqItemRepository faqItemRepository;
    private final ClinicSettingsRepository clinicSettingsRepository;

    public FaqService(FaqItemRepository faqItemRepository, ClinicSettingsRepository clinicSettingsRepository) {
        this.faqItemRepository = faqItemRepository;
        this.clinicSettingsRepository = clinicSettingsRepository;
    }

    public List<FaqItem> listActive() {
        return faqItemRepository.findByActiveTrueOrderBySortOrderAsc();
    }

    public List<FaqItem> listAll() {
        return faqItemRepository.findAllByOrderBySortOrderAsc();
    }

    public Optional<FaqItem> findById(Long id) {
        return faqItemRepository.findById(id);
    }

    @Transactional
    public FaqItem save(FaqItem item) {
        item.setUpdatedAt(Instant.now());
        return faqItemRepository.save(item);
    }

    @Transactional
    public void delete(Long id) {
        faqItemRepository.deleteById(id);
    }

    public String findBestAnswer(String utterance) {
        if (utterance == null) return null;
        String q = utterance.toLowerCase(Locale.ROOT);
        return listActive().stream()
                .filter(f -> q.contains(f.getQuestion().toLowerCase(Locale.ROOT).substring(0, Math.min(4, f.getQuestion().length())))
                        || f.getQuestion().toLowerCase(Locale.ROOT).contains(q.substring(0, Math.min(4, q.length()))))
                .map(FaqItem::getAnswer)
                .findFirst()
                .orElse(null);
    }

    public ClinicSettings getClinicSettings() {
        return clinicSettingsRepository.findAll().stream().findFirst().orElseGet(this::defaultSettings);
    }

    @Transactional
    public ClinicSettings updateClinicSettings(ClinicSettings settings) {
        settings.setUpdatedAt(Instant.now());
        return clinicSettingsRepository.save(settings);
    }

    private ClinicSettings defaultSettings() {
        ClinicSettings s = new ClinicSettings();
        s.setClinicName("MediCall クリニック");
        s.setHoursText("平日 9:00-18:00");
        s.setHolidaysText("土日祝休診");
        s.setAccessText("駅徒歩5分");
        s.setBelongingsText("保険証・診察券");
        return s;
    }
}
