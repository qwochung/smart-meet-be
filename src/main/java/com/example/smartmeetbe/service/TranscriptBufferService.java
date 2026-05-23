package com.example.smartmeetbe.service;

import com.example.smartmeetbe.entity.MeetingTranscript;
import com.example.smartmeetbe.repository.MeetingTranscriptRepository;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
@Service
@RequiredArgsConstructor
public class TranscriptBufferService {

    private final MeetingTranscriptRepository repository;

    private final ConcurrentLinkedQueue<MeetingTranscript> buffer = new ConcurrentLinkedQueue<>();

    public void addToBuffer(MeetingTranscript transcript) {
        if (transcript != null) {
            buffer.offer(transcript);
            log.info("Buffered transcript chunk {} for participant {} in room {}",
                    transcript.getChunkIndex(), transcript.getParticipantId(), transcript.getRoomId());
        }
    }

    @Scheduled(fixedRate = 5000)
    public void flushToDatabase() {
        if (buffer.isEmpty()) {
            return;
        }

        List<MeetingTranscript> batch = new ArrayList<>();
        MeetingTranscript entity;
        while ((entity = buffer.poll()) != null) {
            batch.add(entity);
        }

        try {
            repository.saveAll(batch);
            log.info("Flushed {} transcript(s) to database", batch.size());
        } catch (Exception e) {
            log.error("Failed to flush {} transcript(s) to database: {}", batch.size(), e.getMessage(), e);
            buffer.addAll(batch);
        }
    }

    @PreDestroy
    void onShutdown() {
        log.info("Application shutting down — flushing remaining {} transcript(s)", buffer.size());
        flushToDatabase();
    }
}
