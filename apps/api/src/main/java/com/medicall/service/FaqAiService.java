package com.medicall.service;

import com.medicall.domain.CallIntent;
import com.medicall.domain.ClinicSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FaqAiService {

    private static final Logger log = LoggerFactory.getLogger(FaqAiService.class);

    private static final String FAQ_SYSTEM_PROMPT = """
            あなたは医療クリニックの電話自動応答AIです。
            提供されたFAQ・医院情報・参考資料のみに基づき、簡潔に1〜3文で回答してください。
            診断・処方・重症度の判断は絶対にしないでください。
            資料にない内容は「担当者におつなぎします」とだけ答えてください。
            """;

    private final FaqService faqService;
    private final RagService ragService;
    private final OpenAiService openAiService;

    public FaqAiService(FaqService faqService, RagService ragService, OpenAiService openAiService) {
        this.faqService = faqService;
        this.ragService = ragService;
        this.openAiService = openAiService;
    }

    public record FaqAnswer(String text, boolean aiGenerated, boolean shouldTransfer) {}

    public FaqAnswer answer(String utterance, CallIntent intent) {
        if (utterance == null || utterance.isBlank()) {
            return new FaqAnswer(null, false, true);
        }

        String direct = faqService.findBestAnswer(utterance);
        if (direct != null) {
            return new FaqAnswer(direct, false, false);
        }

        ClinicSettings clinic = faqService.getClinicSettings();
        String intentHint = intentContext(clinic, intent);
        String rag = ragService.searchContext(utterance, 5);
        String faqCatalog = faqService.buildFaqCatalog();

        if (!openAiService.isConfigured()) {
            if (!intentHint.isBlank()) {
                return new FaqAnswer(intentHint, false, false);
            }
            if (!rag.isBlank()) {
                return new FaqAnswer(firstRagLine(rag), false, false);
            }
            return new FaqAnswer(null, false, true);
        }

        String userMessage = buildUserMessage(utterance, intent, intentHint, faqCatalog, rag);
        String aiResponse = openAiService.chat(FAQ_SYSTEM_PROMPT, userMessage);

        if (aiResponse.contains("担当者におつなぎ") || aiResponse.contains("おつなぎします")) {
            if (!intentHint.isBlank()) {
                return new FaqAnswer(intentHint, false, false);
            }
            return new FaqAnswer(aiResponse, true, true);
        }

        return new FaqAnswer(aiResponse, true, false);
    }

    private String buildUserMessage(String utterance, CallIntent intent, String intentHint,
                                    String faqCatalog, String rag) {
        StringBuilder sb = new StringBuilder();
        sb.append("患者の質問: ").append(utterance).append("\n");
        sb.append("分類: ").append(intent.name()).append("\n\n");
        if (!intentHint.isBlank()) {
            sb.append("【医院情報（このカテゴリ）】\n").append(intentHint).append("\n\n");
        }
        if (!faqCatalog.isBlank()) {
            sb.append("【FAQ一覧】\n").append(faqCatalog).append("\n\n");
        }
        if (!rag.isBlank()) {
            sb.append("【関連資料（RAG）】\n").append(rag).append("\n");
        }
        return sb.toString();
    }

    private String intentContext(ClinicSettings clinic, CallIntent intent) {
        return switch (intent) {
            case HOURS -> clinic.getHoursText();
            case HOLIDAY -> clinic.getHolidaysText();
            case ACCESS -> clinic.getAccessText();
            case BELONGINGS -> clinic.getBelongingsText();
            case LAB -> "検査・採血は予約制です。結果は通常3〜5営業日でご連絡します。";
            case BILLING -> "会計・料金に関するご案内です。";
            case PHARMACY -> "お薬・処方に関するご質問です。内容確認は医師・薬剤師が対応します。";
            case REFERRAL -> "紹介状は診察後、窓口でお申し出ください。";
            default -> "";
        };
    }

    private String firstRagLine(String rag) {
        for (String line : rag.split("\n")) {
            if (line.startsWith("- ")) {
                return line.substring(2).trim();
            }
        }
        return rag.trim();
    }
}
