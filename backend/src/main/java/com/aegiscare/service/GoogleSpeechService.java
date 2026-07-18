package com.aegiscare.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

@Service
public class GoogleSpeechService {

    private static final String SPEECH_RECOGNIZE_URL = "https://speech.googleapis.com/v1/speech:recognize";
    private static final String TEXT_SYNTHESIZE_URL = "https://texttospeech.googleapis.com/v1/text:synthesize";

    @Value("${aegiscare.voice.google-api-key:}")
    private String apiKey;

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public GoogleSpeechService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public static class VoiceNotConfiguredException extends RuntimeException {
        public VoiceNotConfiguredException(String message) {
            super(message);
        }
    }

    public static class SpeechApiException extends RuntimeException {
        public SpeechApiException(String message) {
            super(message);
        }
    }

    private void requireApiKey() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new VoiceNotConfiguredException(
                    "Voice features are not configured. Set the GOOGLE_CLOUD_API_KEY environment variable " +
                            "to a key with the Speech-to-Text and Text-to-Speech APIs enabled.");
        }
    }

    public String transcribe(byte[] audioBytes) {
        requireApiKey();
        try {
            Map<String, Object> config = Map.of(
                    "encoding", "WEBM_OPUS",
                    "languageCode", "en-US",
                    "enableAutomaticPunctuation", true
            );
            Map<String, Object> audio = Map.of(
                    "content", Base64.getEncoder().encodeToString(audioBytes)
            );
            Map<String, Object> body = Map.of("config", config, "audio", audio);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(SPEECH_RECOGNIZE_URL + "?key=" + apiKey))
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new SpeechApiException(
                        "Google Speech-to-Text request failed (" + response.statusCode() + "): " + response.body());
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode results = root.path("results");
            StringBuilder transcript = new StringBuilder();
            for (JsonNode result : results) {
                JsonNode alternatives = result.path("alternatives");
                if (alternatives.isArray() && !alternatives.isEmpty()) {
                    if (!transcript.isEmpty()) {
                        transcript.append(" ");
                    }
                    transcript.append(alternatives.get(0).path("transcript").asText(""));
                }
            }
            return transcript.toString().trim();
        } catch (IOException e) {
            throw new SpeechApiException("Failed to reach Google Speech-to-Text: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SpeechApiException("Speech-to-Text request was interrupted.");
        }
    }

    public byte[] synthesize(String text) {
        requireApiKey();
        try {
            Map<String, Object> input = Map.of("text", text);
            Map<String, Object> voice = Map.of("languageCode", "en-US", "ssmlGender", "NEUTRAL");
            Map<String, Object> audioConfig = Map.of("audioEncoding", "MP3");
            Map<String, Object> body = Map.of("input", input, "voice", voice, "audioConfig", audioConfig);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(TEXT_SYNTHESIZE_URL + "?key=" + apiKey))
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new SpeechApiException(
                        "Google Text-to-Speech request failed (" + response.statusCode() + "): " + response.body());
            }

            JsonNode root = objectMapper.readTree(response.body());
            String base64Audio = root.path("audioContent").asText("");
            if (base64Audio.isEmpty()) {
                throw new SpeechApiException("Google Text-to-Speech returned no audio content.");
            }
            return Base64.getDecoder().decode(base64Audio);
        } catch (IOException e) {
            throw new SpeechApiException("Failed to reach Google Text-to-Speech: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SpeechApiException("Text-to-Speech request was interrupted.");
        }
    }
}
