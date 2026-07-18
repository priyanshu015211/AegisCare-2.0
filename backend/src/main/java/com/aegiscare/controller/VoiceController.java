package com.aegiscare.controller;

import com.aegiscare.service.GoogleSpeechService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/voice")
public class VoiceController {

    private final GoogleSpeechService speechService;

    public VoiceController(GoogleSpeechService speechService) {
        this.speechService = speechService;
    }

    public record SpeakRequest(String text) {}

    @PostMapping(value = "/transcribe", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> transcribe(@RequestPart("audio") MultipartFile audio) {
        try {
            byte[] bytes = audio.getBytes();
            if (bytes.length == 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "No audio received."));
            }
            String transcript = speechService.transcribe(bytes);
            return ResponseEntity.ok(Map.of("transcript", transcript));
        } catch (GoogleSpeechService.VoiceNotConfiguredException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", e.getMessage()));
        } catch (GoogleSpeechService.SpeechApiException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Could not read uploaded audio: " + e.getMessage()));
        }
    }

    @PostMapping("/speak")
    public ResponseEntity<byte[]> speak(@RequestBody SpeakRequest request) {
        if (request.text() == null || request.text().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        try {
            byte[] audio = speechService.synthesize(request.text());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, "audio/mpeg")
                    .body(audio);
        } catch (GoogleSpeechService.VoiceNotConfiguredException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        } catch (GoogleSpeechService.SpeechApiException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }
}
