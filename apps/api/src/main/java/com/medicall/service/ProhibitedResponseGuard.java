package com.medicall.service;

import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class ProhibitedResponseGuard {

    private static final List<String> PROHIBITED_PATTERNS = List.of(
            "診断", "診断します", "診断できます", "病名", "疾患",
            "処方", "お薬を", "薬を出し", "投薬",
            "重症", "軽症", "危険", "命に関わる", "すぐに病院",
            "治療方針", "手術が必要", "がん", "癌"
    );

    private static final Pattern DIAGNOSIS_LIKE = Pattern.compile(
            "(あなたは|おそらく|可能性が高い).*(です|でしょう|かもしれません)");

    public boolean isProhibited(String response) {
        if (response == null) return false;
        String text = response.toLowerCase(Locale.ROOT);
        if (PROHIBITED_PATTERNS.stream().anyMatch(text::contains)) return true;
        return DIAGNOSIS_LIKE.matcher(response).find();
    }

    public String safeFallback() {
        return "医療的な判断が必要な内容のため、担当の職員におつなぎします。少々お待ちください。";
    }
}
