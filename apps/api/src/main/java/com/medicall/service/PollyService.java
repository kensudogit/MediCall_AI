package com.medicall.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.polly.PollyClient;
import software.amazon.awssdk.services.polly.model.OutputFormat;
import software.amazon.awssdk.services.polly.model.SynthesizeSpeechRequest;
import software.amazon.awssdk.services.polly.model.VoiceId;
import java.util.Base64;

@Service
public class PollyService {

    private static final Logger log = LoggerFactory.getLogger(PollyService.class);

    @Value("${medicall.aws.region:ap-northeast-1}")
    private String region;

    @Value("${medicall.polly.enabled:false}")
    private boolean enabled;

    public String synthesize(String text) {
        if (!enabled || text == null || text.isBlank()) return null;
        try (PollyClient client = PollyClient.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build()) {
            var response = client.synthesizeSpeech(SynthesizeSpeechRequest.builder()
                    .text(text)
                    .voiceId(VoiceId.MIZUKI)
                    .outputFormat(OutputFormat.MP3)
                    .build());
            byte[] bytes = response.readAllBytes();
            return "data:audio/mp3;base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            log.warn("Polly synthesis failed: {}", e.getMessage());
            return null;
        }
    }
}
