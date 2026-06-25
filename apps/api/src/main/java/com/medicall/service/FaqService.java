package com.medicall.service;

import com.medicall.domain.ClinicSettings;
import com.medicall.domain.FaqItem;
import com.medicall.repository.ClinicSettingsRepository;
import com.medicall.repository.FaqItemRepository;
import org.springframework.context.annotation.Lazy;
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
    private final FaqKnowledgeSyncService knowledgeSync;

    public FaqService(FaqItemRepository faqItemRepository,
                      ClinicSettingsRepository clinicSettingsRepository,
                      @Lazy FaqKnowledgeSyncService knowledgeSync) {
        this.faqItemRepository = faqItemRepository;
        this.clinicSettingsRepository = clinicSettingsRepository;
        this.knowledgeSync = knowledgeSync;
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
        FaqItem saved = faqItemRepository.save(item);
        knowledgeSync.syncFaqItem(saved);
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        faqItemRepository.deleteById(id);
        knowledgeSync.deleteFaqItem(id);
    }

    public String buildFaqCatalog() {
        StringBuilder sb = new StringBuilder();
        for (FaqItem item : listActive()) {
            sb.append("- Q: ").append(item.getQuestion())
                    .append(" / A: ").append(item.getAnswer()).append("\n");
        }
        return sb.toString();
    }

    public String findBestAnswer(String utterance) {
        if (utterance == null) return null;
        String q = utterance.toLowerCase(Locale.ROOT).trim();

        return listActive().stream()
                .filter(f -> matchesQuestion(q, f.getQuestion()))
                .map(FaqItem::getAnswer)
                .findFirst()
                .orElse(null);
    }

    private boolean matchesQuestion(String utterance, String question) {
        String fq = question.toLowerCase(Locale.ROOT).trim();
        if (utterance.contains(fq) || fq.contains(utterance)) return true;
        int minLen = Math.min(4, Math.min(utterance.length(), fq.length()));
        if (minLen <= 0) return false;
        String uPrefix = utterance.substring(0, minLen);
        String qPrefix = fq.substring(0, minLen);
        return utterance.contains(qPrefix) || fq.contains(uPrefix);
    }

    public ClinicSettings getClinicSettings() {
        return clinicSettingsRepository.findAll().stream().findFirst().orElseGet(this::defaultSettings);
    }

    @Transactional
    public ClinicSettings updateClinicSettings(ClinicSettings settings) {
        settings.setUpdatedAt(Instant.now());
        ClinicSettings saved = clinicSettingsRepository.save(settings);
        knowledgeSync.syncClinicSettings(saved);
        return saved;
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
