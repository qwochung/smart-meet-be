package com.example.smartmeetbe.service.impl;

import com.example.smartmeetbe.constant.MeetingType;
import com.example.smartmeetbe.constant.MergeStatus;
import com.example.smartmeetbe.document.MeetingSummary;
import com.example.smartmeetbe.document.RoomTranscript;
import com.example.smartmeetbe.document.TranscriptChunk;
import com.example.smartmeetbe.dto.response.FullMergeResult;
import com.example.smartmeetbe.dto.response.MasterMeetingSummaryDto;
import com.example.smartmeetbe.dto.response.MergedTranscriptResponse;
import com.example.smartmeetbe.entity.Room;
import com.example.smartmeetbe.repository.RoomRepository;
import com.example.smartmeetbe.repository.mongo.MeetingSummaryRepository;
import com.example.smartmeetbe.repository.mongo.RoomTranscriptRepository;
import com.example.smartmeetbe.repository.mongo.TranscriptChunkRepository;
import com.example.smartmeetbe.service.FullTranscriptMergeService;
import com.example.smartmeetbe.service.GeminiService;
import com.example.smartmeetbe.service.MeetingFinalizationService;
import com.example.smartmeetbe.strategy.MeetingSummaryContext;
import com.example.smartmeetbe.strategy.MeetingSummaryStrategy;
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
    private final GeminiService geminiService;
    private final RoomRepository roomRepository;
    private final MeetingSummaryRepository meetingSummaryRepository;
    private final MeetingSummaryContext meetingSummaryContext;
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

            // Smooth text using Gemini API
            String smoothed = geminiService.smoothTranscript(result.fullText());
            doc.setSmoothedText(smoothed);

            roomTranscriptRepository.save(doc);

            // Tóm tắt cuộc họp thông minh theo từng loại (Dynamic Prompting - Strategy Pattern)
            try {
                log.info("Generating dynamic AI summary for room {}...", roomId);
                Room room = roomRepository.findByRoomCode(roomId).orElse(null);
                MeetingType typeCode = (room != null && room.getTypeCode() != null) ? room.getTypeCode() : MeetingType.GENERAL;

                // Chốt thời điểm kết thúc thực tế để tính thời lượng cuộc họp
                if (room != null && room.getActualEndedAt() == null) {
                    room.setActualEndedAt(LocalDateTime.now());
                    roomRepository.save(room);
                }
                
                MeetingSummaryStrategy strategy = meetingSummaryContext.getStrategy(typeCode);
                MasterMeetingSummaryDto summaryDto = strategy.generateSummary(roomId, result.fullText());
                
                MeetingSummary summaryDoc = meetingSummaryRepository.findByRoomId(roomId)
                        .orElse(MeetingSummary.builder().roomId(roomId).build());
                
                summaryDoc.setExecutiveSummary(summaryDto.getExecutiveSummary());
                summaryDoc.setDiscussionTopics(summaryDto.getDiscussionTopics());
                summaryDoc.setDecisionsMade(summaryDto.getDecisionsMade());
                summaryDoc.setActionItems(summaryDto.getActionItems());
                summaryDoc.setQaPairs(summaryDto.getQaPairs());
                summaryDoc.setPainPoints(summaryDto.getPainPoints());
                summaryDoc.setProsAndCons(summaryDto.getProsAndCons());
                
                meetingSummaryRepository.save(summaryDoc);
                log.info("Successfully generated and saved AI summary for room {} using strategy {}", roomId, typeCode);
            } catch (Exception e) {
                log.error("Failed to generate dynamic AI summary for room {}: {}", roomId, e.getMessage(), e);
            }

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
                        .smoothedText("")
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
                .smoothedText(doc.getSmoothedText() != null ? doc.getSmoothedText() : "")
                .segments(doc.getSegments() != null ? doc.getSegments() : List.of())
                .processedChunkCount(doc.getProcessedChunkCount() != null ? doc.getProcessedChunkCount() : 0)
                .lastMergedAt(lastMergedAt)
                .build();
    }
}
