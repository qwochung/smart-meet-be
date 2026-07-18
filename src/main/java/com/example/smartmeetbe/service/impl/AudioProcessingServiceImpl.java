package com.example.smartmeetbe.service.impl;

import com.example.smartmeetbe.document.TranscriptChunk;
import com.example.smartmeetbe.dto.request.AudioChunkRequest;
import com.example.smartmeetbe.repository.mongo.TranscriptChunkRepository;
import com.example.smartmeetbe.service.AudioProcessingService;
import com.example.smartmeetbe.service.DraftTranscriptService;
import com.example.smartmeetbe.service.TextDedupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class AudioProcessingServiceImpl implements AudioProcessingService {

    private final TextDedupService textDedupService;
    private final TranscriptChunkRepository transcriptChunkRepository;
    private final DraftTranscriptService draftTranscriptService;
    private final RestTemplate aiRestTemplate;

    @Value("${ai-server.transcribe-url:http://localhost:8000/api/transcribe/chunk}")
    String transcribeUrl;

    @Value("${ai-server.token:}")
    String transcribeToken;

    // ASR dự phòng: khi server chính không kết nối được (chưa start / gateway chết) thì tự chuyển sang đây.
    // Để trống -> không có fallback.
    @Value("${ai-server.fallback-url:}")
    String fallbackUrl;

    @Value("${ai-server.fallback-token:}")
    String fallbackToken;

    private static final int MAX_TRANSCRIBE_ATTEMPTS = 3;

    @Async
    @Override
    public CompletableFuture<Map<String, Object>> processAudio(String roomId, AudioChunkRequest payload) {
        try {
            byte[] audioBytes = Base64.getDecoder().decode(payload.getAudioDataBase64());
            log.info("Decoded audio chunk {} from participant {} in room {}, size={} bytes, isForceCut={}",
                    payload.getChunkIndex(), payload.getParticipantId(), roomId, audioBytes.length, payload.isForceCut());

            String rawText = callTranscribeApi(audioBytes, payload.getSampleRate(), payload.getChannels());
            log.debug("AI transcript for chunk {}: {}", payload.getChunkIndex(), rawText);

            Map<String, Object> result = new HashMap<>();
            result.put("text", rawText);
            result.put("isFinal", !payload.isForceCut());

            if (rawText.isBlank()) {
                return CompletableFuture.completedFuture(result);
            }

            String cleanText = textDedupService.deduplicate(roomId, payload.getParticipantId(), rawText);
            log.debug("After dedup for chunk {}: {}", payload.getChunkIndex(), cleanText);

            if (!cleanText.isBlank()) {
                TranscriptChunk transcript = TranscriptChunk.builder()
                        .roomId(roomId)
                        .participantId(payload.getParticipantId())
                        .participantName(payload.getParticipantName())
                        .chunkIndex(payload.getChunkIndex())
                        .startTimeMs(payload.getStartTimeMs())
                        .endTimeMs(payload.getEndTimeMs())
                        .content(cleanText)
                        .isForceCut(payload.isForceCut())
                        .build();
                TranscriptChunk saved = transcriptChunkRepository.save(transcript);
                draftTranscriptService.appendChunk(saved);
            }
            result.put("text", cleanText);

            return CompletableFuture.completedFuture(result);
        } catch (IllegalArgumentException e) {
            log.error("Failed to decode base64 audio from participant {} in room {}: {}",
                    payload.getParticipantId(), roomId, e.getMessage());
            return CompletableFuture.failedFuture(e);
        } catch (Exception e) {
            log.error("Error processing audio chunk {} from participant {} in room {}: {}",
                    payload.getChunkIndex(), payload.getParticipantId(), roomId, e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    @SuppressWarnings("unchecked")
    private String callTranscribeApi(byte[] audioBytes, Integer sampleRate, Integer channels) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("audio_bytes", new ByteArrayResource(audioBytes) {
            @Override
            public String getFilename() {
                return "audio.wav";
            }
        });
        if (sampleRate != null) body.add("sample_rate", sampleRate);
        if (channels != null) body.add("channels", channels);

        // Ưu tiên server ASR chính.
        try {
            return extractText(postWithRetry(transcribeUrl, transcribeToken, body));
        } catch (ResourceAccessException | HttpServerErrorException primaryError) {
            // Chính không kết nối được (chưa start) hoặc gateway lỗi 5xx -> thử server dự phòng nếu có.
            if (fallbackUrl == null || fallbackUrl.isBlank()) {
                throw primaryError;
            }
            log.warn("ASR chính [{}] lỗi ({}), chuyển sang ASR dự phòng [{}]",
                    transcribeUrl, primaryError.getMessage(), fallbackUrl);
            return extractText(postWithRetry(fallbackUrl, fallbackToken, body));
        }
    }

    @SuppressWarnings("rawtypes")
    private String extractText(ResponseEntity<Map> response) {
        if (response.getBody() != null) {
            return (String) response.getBody().getOrDefault("text", "");
        }
        return "";
    }

    @SuppressWarnings("rawtypes")
    private ResponseEntity<Map> postWithRetry(String url, String token, MultiValueMap<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        // Gateway ASR public (Syrix) yêu cầu Bearer token; local server thì để trống -> bỏ qua
        if (token != null && !token.isBlank()) {
            headers.setBearerAuth(token);
        }
        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

        ResourceAccessException lastError = null;
        for (int attempt = 1; attempt <= MAX_TRANSCRIBE_ATTEMPTS; attempt++) {
            try {
                return aiRestTemplate.postForEntity(url, request, Map.class);
            } catch (ResourceAccessException e) {
                lastError = e;
                log.warn("Transcribe request tới {} thất bại (lần {}/{}), thử lại: {}",
                        url, attempt, MAX_TRANSCRIBE_ATTEMPTS, e.getMessage());
                if (attempt < MAX_TRANSCRIBE_ATTEMPTS) {
                    try {
                        // Backoff tăng dần để không dồn thêm tải khi AI server đang quá tải
                        Thread.sleep(500L * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw lastError;
                    }
                }
            }
        }
        throw lastError;
    }
}
