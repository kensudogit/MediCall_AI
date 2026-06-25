package com.medicall.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.Base64;

@Service
public class GoogleSpeechService {

    private static final Logger log = LoggerFactory.getLogger(GoogleSpeechService.class);

    @Value("${medicall.google.speech.enabled:false}")
    private boolean enabled;

    public String transcribe(byte[] audioBytes, String encoding) {
        if (!enabled || audioBytes == null || audioBytes.length == 0) {
            return null;
        }
        try {
            // Production: use Google Cloud Speech-to-Text client with credentials
            log.info("Google Speech transcribe requested ({} bytes, encoding={})", audioBytes.length, encoding);
            return null;
        } catch (Exception e) {
            log.warn("Speech transcription failed: {}", e.getMessage());
            return null;
        }
    }

    public String transcribeBase64(String base64Audio) {
        if (base64Audio == null) return null;
        return transcribe(Base64.getDecoder().decode(base64Audio), "LINEAR16");
    }
}
