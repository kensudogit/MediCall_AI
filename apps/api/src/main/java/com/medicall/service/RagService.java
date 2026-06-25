package com.medicall.service;

import com.medicall.tenant.TenantContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service
public class RagService {

    private final JdbcTemplate jdbcTemplate;
    private final OpenAiService openAiService;

    public RagService(JdbcTemplate jdbcTemplate, OpenAiService openAiService) {
        this.jdbcTemplate = jdbcTemplate;
        this.openAiService = openAiService;
    }

    public String searchContext(String query, int limit) {
        float[] embedding = openAiService.embed(query);
        String vectorLiteral = toPgVector(embedding);
        Long tenantId = TenantContext.requireTenantId();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT content, 1 - (embedding <=> ?::vector) AS score
                FROM knowledge_chunks
                WHERE tenant_id = ?
                ORDER BY embedding <=> ?::vector
                LIMIT ?
                """, vectorLiteral, tenantId, vectorLiteral, limit);

        if (rows.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> row : rows) {
            sb.append("- ").append(row.get("content")).append("\n");
        }
        return sb.toString();
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
