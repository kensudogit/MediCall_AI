package com.medicall.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class OpenAiService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiService.class);

    @Value("${medicall.openai.api-key:}")
    private String apiKey;

    @Value("${medicall.openai.model:gpt-4o-mini}")
    private String model;

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public String chat(String systemPrompt, String userMessage) {
        if (!isConfigured()) {
            return fallbackResponse(userMessage);
        }
        try {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userMessage)
                    ),
                    "temperature", 0.3
            );
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .timeout(Duration.ofSeconds(30))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = mapper.readTree(response.body());
            return root.path("choices").path(0).path("message").path("content").asText(fallbackResponse(userMessage));
        } catch (Exception e) {
            log.warn("OpenAI call failed: {}", e.getMessage());
            return fallbackResponse(userMessage);
        }
    }

    public String summarizeCall(List<String> turns) {
        String transcript = String.join("\n", turns);
        String system = """
                あなたは医療コールセンターの通話要約アシスタントです。
                通話内容を職員向けに3-5行で要約してください。
                診断・処方・重症度の判断は含めないでください。
                """;
        return chat(system, transcript);
    }

    public float[] embed(String text) {
        if (!isConfigured()) return new float[1536];
        try {
            Map<String, Object> body = Map.of(
                    "model", "text-embedding-3-small",
                    "input", text
            );
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.openai.com/v1/embeddings"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .timeout(Duration.ofSeconds(20))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode arr = mapper.readTree(response.body()).path("data").path(0).path("embedding");
            float[] vec = new float[arr.size()];
            for (int i = 0; i < arr.size(); i++) vec[i] = (float) arr.get(i).asDouble();
            return vec;
        } catch (Exception e) {
            log.warn("Embedding failed: {}", e.getMessage());
            return new float[1536];
        }
    }

    private String fallbackResponse(String userMessage) {
        return "ご用件を承りました。詳細は担当者がご案内いたします。";
    }
}
