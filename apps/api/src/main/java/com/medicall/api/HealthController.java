package com.medicall.api;

import com.medicall.service.OpenAiService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final OpenAiService openAiService;

    public HealthController(OpenAiService openAiService) {
        this.openAiService = openAiService;
    }

    @GetMapping
    public Map<String, Object> health() {
        return Map.of(
                "status", "ok",
                "openai", openAiService.isConfigured()
        );
    }
}
