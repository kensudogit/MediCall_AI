package com.medicall.service;

import com.medicall.domain.ClinicSettings;
import com.medicall.domain.FaqItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FaqKnowledgeSyncService {

    private static final Logger log = LoggerFactory.getLogger(FaqKnowledgeSyncService.class);

    private final JdbcTemplate jdbcTemplate;
    private final FaqService faqService;
    private final OpenAiService openAiService;

    public FaqKnowledgeSyncService(JdbcTemplate jdbcTemplate,
                                   FaqService faqService,
                                   OpenAiService openAiService) {
        this.jdbcTemplate = jdbcTemplate;
        this.faqService = faqService;
        this.openAiService = openAiService;
    }

    @Transactional
    public void syncFaqItem(FaqItem item) {
        if (item.getId() == null) return;
        deleteChunk("faq", item.getId().toString());
        if (!item.isActive()) return;

        String content = "Q: " + item.getQuestion() + "\nA: " + item.getAnswer();
        insertChunk("faq", item.getId().toString(), content, item.getCategory());
    }

    @Transactional
    public void deleteFaqItem(Long id) {
        deleteChunk("faq", id.toString());
    }

    @Transactional
    public void syncClinicSettings(ClinicSettings settings) {
        deleteChunksByType("clinic");
        insertChunk("clinic", "hours", "診療時間: " + settings.getHoursText(), "診療時間");
        insertChunk("clinic", "holidays", "休診日: " + settings.getHolidaysText(), "休診日");
        insertChunk("clinic", "access", "アクセス: " + settings.getAccessText(), "アクセス");
        insertChunk("clinic", "belongings", "持ち物: " + settings.getBelongingsText(), "持ち物");
        insertChunk("clinic", "name", "医院名: " + settings.getClinicName(), "一般");
    }

    @Transactional
    public void syncAll() {
        log.info("Syncing FAQ and clinic knowledge to pgvector...");
        faqService.listAll().forEach(this::syncFaqItem);
        syncClinicSettings(faqService.getClinicSettings());
        log.info("Knowledge sync complete (OpenAI configured: {})", openAiService.isConfigured());
    }

    private void deleteChunk(String sourceType, String sourceId) {
        jdbcTemplate.update(
                "DELETE FROM knowledge_chunks WHERE source_type = ? AND source_id = ?",
                sourceType, sourceId);
    }

    private void deleteChunksByType(String sourceType) {
        jdbcTemplate.update("DELETE FROM knowledge_chunks WHERE source_type = ?", sourceType);
    }

    private void insertChunk(String sourceType, String sourceId, String content, String category) {
        float[] embedding = openAiService.embed(content);
        String vector = toPgVector(embedding);
        jdbcTemplate.update("""
                INSERT INTO knowledge_chunks (source_type, source_id, content, embedding, metadata)
                VALUES (?, ?, ?, ?::vector, jsonb_build_object('category', ?))
                """, sourceType, sourceId, content, vector, category);
    }

    private String toPgVector(float[] v) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < v.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(v[i]);
        }
        sb.append("]");
        return sb.toString();
    }
}
