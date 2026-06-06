package com.example.smartmeetbe.service;

import com.example.smartmeetbe.dto.request.AudioChunkRequest;
import com.example.smartmeetbe.entity.MeetingTranscript;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class AudioProcessingService {

    private final TextDedupService textDedupService;
    private final TranscriptBufferService transcriptBufferService;
    private final RestTemplate restTemplate;

    @Value("${ai-server.transcribe-url:http://ai-server:8000/transcribe}")
    String transcribeUrl;
    
    @Async
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

            if (payload.isForceCut()) {
                String cleanText = textDedupService.deduplicate(roomId, payload.getParticipantId(), rawText);
                log.debug("After dedup for forced-cut chunk {}: {}", payload.getChunkIndex(), cleanText);

                if (!cleanText.isBlank()) {
                    MeetingTranscript transcript = MeetingTranscript.builder()
                            .roomId(roomId)
                            .participantId(payload.getParticipantId())
                            .participantName(payload.getParticipantName())
                            .chunkIndex(payload.getChunkIndex())
                            .startTimeMs(payload.getStartTimeMs())
                            .endTimeMs(payload.getEndTimeMs())
                            .content(cleanText)
                            .build();
                    transcriptBufferService.addToBuffer(transcript);
                }
                result.put("text", cleanText);
            } else {
                log.debug("Natural cut — skipping dedup for chunk {}", payload.getChunkIndex());

                MeetingTranscript transcript = MeetingTranscript.builder()
                        .roomId(roomId)
                        .participantId(payload.getParticipantId())
                        .participantName(payload.getParticipantName())
                        .chunkIndex(payload.getChunkIndex())
                        .startTimeMs(payload.getStartTimeMs())
                        .endTimeMs(payload.getEndTimeMs())
                        .content(rawText)
                        .build();
                transcriptBufferService.addToBuffer(transcript);
                result.put("text", rawText);
            }

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
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("audio_bytes", new ByteArrayResource(audioBytes) {
            @Override
            public String getFilename() {
                return "audio.wav";
            }
        });
        if (sampleRate != null) body.add("sample_rate", sampleRate);
        if (channels != null) body.add("channels", channels);

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(transcribeUrl, request, Map.class);

        if (response.getBody() != null) {
            return (String) response.getBody().getOrDefault("text", "");
        }
        return "";
    }
}
