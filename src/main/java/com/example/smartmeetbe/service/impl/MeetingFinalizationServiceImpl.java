package com.example.smartmeetbe.service.impl;

import com.example.smartmeetbe.constant.MergeStatus;
import com.example.smartmeetbe.document.RoomTranscript;
import com.example.smartmeetbe.document.TranscriptChunk;
import com.example.smartmeetbe.dto.response.FullMergeResult;
import com.example.smartmeetbe.dto.response.MergedTranscriptResponse;
import com.example.smartmeetbe.repository.mongo.RoomTranscriptRepository;
import com.example.smartmeetbe.repository.mongo.TranscriptChunkRepository;
import com.example.smartmeetbe.service.FullTranscriptMergeService;
import com.example.smartmeetbe.service.MeetingFinalizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingFinalizationServiceImpl implements MeetingFinalizationService {

    private final TranscriptChunkRepository transcriptChunkRepository;
    private final RoomTranscriptRepository roomTranscriptRepository;
    private final FullTranscriptMergeService fullTranscriptMergeService;
    private final ConcurrentHashMap<String, ReentrantLock> roomLocks = new ConcurrentHashMap<>();

    @Async
    @Override
    public void finalizeAsync(String roomId) {
        ReentrantLock lock = roomLocks.computeIfAbsent(roomId, id -> new ReentrantLock());
        if (!lock.tryLock()) {
            log.info("Finalize already in progress for room {}", roomId);
            return;
        }

        try {
            RoomTranscript existing = roomTranscriptRepository.findByRoomId(roomId).orElse(null);
            if (existing != null && existing.getStatus() == MergeStatus.FINAL) {
                log.info("Room {} transcript already FINAL", roomId);
                return;
            }

            RoomTranscript doc = existing != null
                    ? existing
                    : RoomTranscript.builder().roomId(roomId).build();
            doc.setStatus(MergeStatus.PROCESSING);
            roomTranscriptRepository.save(doc);

            List<TranscriptChunk> chunks =
                    transcriptChunkRepository.findByRoomIdOrderByStartTimeMsAscCreatedAtAsc(roomId);
            FullMergeResult result = fullTranscriptMergeService.mergeAll(roomId, chunks);

            doc.setFullText(result.fullText());
            doc.setSegments(result.segments());
            doc.setVersion((doc.getVersion() != null ? doc.getVersion() : 0) + 1);
            doc.setProcessedChunkCount(chunks.size());
            doc.setLastProcessedChunkId(chunks.isEmpty() ? null : chunks.get(chunks.size() - 1).getId());
            doc.setStatus(MergeStatus.FINAL);
            roomTranscriptRepository.save(doc);

            log.info("Finalized transcript for room {} with {} segments from {} chunks",
                    roomId, result.segments().size(), chunks.size());
        } catch (Exception e) {
            log.error("Failed to finalize transcript for room {}: {}", roomId, e.getMessage(), e);
            roomTranscriptRepository.findByRoomId(roomId).ifPresent(doc -> {
                doc.setStatus(MergeStatus.FAILED);
                roomTranscriptRepository.save(doc);
            });
        } finally {
            lock.unlock();
        }
    }

    @Override
    public MergedTranscriptResponse getFinalTranscript(String roomId) {
        return roomTranscriptRepository.findByRoomId(roomId)
                .filter(doc -> doc.getStatus() == MergeStatus.FINAL
                        || doc.getStatus() == MergeStatus.PROCESSING
                        || doc.getStatus() == MergeStatus.FAILED)
                .map(this::toResponse)
                .orElse(MergedTranscriptResponse.builder()
                        .roomId(roomId)
                        .version(0)
                        .status(MergeStatus.PROCESSING)
                        .fullText("")
                        .segments(List.of())
                        .processedChunkCount(0)
                        .lastMergedAt(null)
                        .build());
    }

    private MergedTranscriptResponse toResponse(RoomTranscript doc) {
        LocalDateTime lastMergedAt = doc.getUpdatedAt() != null
                ? LocalDateTime.ofInstant(doc.getUpdatedAt(), ZoneId.systemDefault())
                : null;
        return MergedTranscriptResponse.builder()
                .roomId(doc.getRoomId())
                .version(doc.getVersion() != null ? doc.getVersion() : 0)
                .status(doc.getStatus())
                .fullText(doc.getFullText() != null ? doc.getFullText() : "")
                .segments(doc.getSegments() != null ? doc.getSegments() : List.of())
                .processedChunkCount(doc.getProcessedChunkCount() != null ? doc.getProcessedChunkCount() : 0)
                .lastMergedAt(lastMergedAt)
                .build();
    }
}
