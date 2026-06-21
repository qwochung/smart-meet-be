package com.example.smartmeetbe.service.impl;

import com.example.smartmeetbe.constant.MergeStatus;
import com.example.smartmeetbe.document.RoomTranscript;
import com.example.smartmeetbe.document.TranscriptChunk;
import com.example.smartmeetbe.dto.response.MergedTranscriptResponse;
import com.example.smartmeetbe.dto.response.TranscriptSegmentDto;
import com.example.smartmeetbe.repository.mongo.RoomTranscriptRepository;
import com.example.smartmeetbe.service.DraftTranscriptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DraftTranscriptServiceImpl implements DraftTranscriptService {

    private static final String DRAFT_TOPIC = "/topic/rooms/%s/transcript/draft";
    private static final String TYPE_APPEND = "SEGMENT_APPEND";

    private final RoomTranscriptRepository roomTranscriptRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ConcurrentHashMap<String, ReentrantLock> roomLocks = new ConcurrentHashMap<>();

    @Override
    public void appendChunk(TranscriptChunk chunk) {
        if (chunk == null || chunk.getId() == null || chunk.getContent() == null || chunk.getContent().isBlank()) {
            return;
        }

        ReentrantLock lock = roomLocks.computeIfAbsent(chunk.getRoomId(), id -> new ReentrantLock());
        lock.lock();
        try {
            RoomTranscript doc = roomTranscriptRepository.findByRoomId(chunk.getRoomId())
                    .orElse(RoomTranscript.builder()
                            .roomId(chunk.getRoomId())
                            .status(MergeStatus.LIVE)
                            .build());

            if (doc.getStatus() != null && doc.getStatus() != MergeStatus.LIVE) {
                return;
            }

            List<TranscriptSegmentDto> segments = doc.getSegments() != null
                    ? new ArrayList<>(doc.getSegments())
                    : new ArrayList<>();

            boolean alreadyProcessed = segments.stream()
                    .anyMatch(segment -> chunk.getId().equals(segment.sourceChunkId()));
            if (alreadyProcessed) {
                return;
            }

            TranscriptSegmentDto newSegment = TranscriptSegmentDto.builder()
                    .orderIndex(segments.size())
                    .participantId(chunk.getParticipantId())
                    .participantName(chunk.getParticipantName())
                    .startTimeMs(chunk.getStartTimeMs())
                    .endTimeMs(chunk.getEndTimeMs())
                    .content(chunk.getContent())
                    .sourceChunkId(chunk.getId())
                    .build();
            segments.add(newSegment);

            List<TranscriptSegmentDto> sorted = sortAndReindex(segments);
            doc.setSegments(sorted);
            doc.setFullText(buildFullText(sorted));
            doc.setVersion((doc.getVersion() != null ? doc.getVersion() : 0) + 1);
            doc.setProcessedChunkCount(sorted.size());
            doc.setLastProcessedChunkId(chunk.getId());
            doc.setStatus(MergeStatus.LIVE);

            roomTranscriptRepository.save(doc);

            TranscriptSegmentDto pushedSegment = sorted.stream()
                    .filter(segment -> chunk.getId().equals(segment.sourceChunkId()))
                    .findFirst()
                    .orElse(newSegment);
            pushDraftDelta(chunk.getRoomId(), doc.getVersion(), pushedSegment);
            log.debug("Draft transcript updated for room {} v{}", chunk.getRoomId(), doc.getVersion());
        } finally {
            lock.unlock();
        }
    }

    @Override
    public MergedTranscriptResponse getDraftTranscript(String roomId) {
        return roomTranscriptRepository.findByRoomId(roomId)
                .filter(doc -> doc.getStatus() == MergeStatus.LIVE)
                .map(this::toResponse)
                .orElse(emptyResponse(roomId));
    }

    private List<TranscriptSegmentDto> sortAndReindex(List<TranscriptSegmentDto> segments) {
        List<TranscriptSegmentDto> sorted = segments.stream()
                .sorted(Comparator.comparing(
                        TranscriptSegmentDto::startTimeMs,
                        Comparator.nullsLast(Long::compareTo)))
                .collect(Collectors.toCollection(ArrayList::new));

        List<TranscriptSegmentDto> reindexed = new ArrayList<>(sorted.size());
        for (int i = 0; i < sorted.size(); i++) {
            TranscriptSegmentDto segment = sorted.get(i);
            reindexed.add(new TranscriptSegmentDto(
                    i,
                    segment.participantId(),
                    segment.participantName(),
                    segment.startTimeMs(),
                    segment.endTimeMs(),
                    segment.content(),
                    segment.sourceChunkId()
            ));
        }
        return reindexed;
    }

    private String buildFullText(List<TranscriptSegmentDto> segments) {
        return segments.stream()
                .map(segment -> "[" + segment.participantName() + "]: " + segment.content())
                .collect(Collectors.joining("\n"));
    }

    private void pushDraftDelta(String roomId, int version, TranscriptSegmentDto segment) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("roomId", roomId);
        payload.put("version", version);
        payload.put("type", TYPE_APPEND);
        payload.put("segment", segment);
        messagingTemplate.convertAndSend(String.format(DRAFT_TOPIC, roomId), payload);
    }

    private MergedTranscriptResponse toResponse(RoomTranscript doc) {
        LocalDateTime lastMergedAt = doc.getUpdatedAt() != null
                ? LocalDateTime.ofInstant(doc.getUpdatedAt(), ZoneId.systemDefault())
                : null;
        List<TranscriptSegmentDto> segments = doc.getSegments() != null ? doc.getSegments() : List.of();
        return MergedTranscriptResponse.builder()
                .roomId(doc.getRoomId())
                .version(doc.getVersion() != null ? doc.getVersion() : 0)
                .status(doc.getStatus())
                .fullText(doc.getFullText() != null ? doc.getFullText() : "")
                .smoothedText(doc.getSmoothedText() != null ? doc.getSmoothedText() : "")
                .segments(segments)
                .processedChunkCount(doc.getProcessedChunkCount() != null ? doc.getProcessedChunkCount() : 0)
                .lastMergedAt(lastMergedAt)
                .build();
    }

    private MergedTranscriptResponse emptyResponse(String roomId) {
        return MergedTranscriptResponse.builder()
                .roomId(roomId)
                .version(0)
                .status(MergeStatus.LIVE)
                .fullText("")
                .smoothedText("")
                .segments(List.of())
                .processedChunkCount(0)
                .lastMergedAt(null)
                .build();
    }
}
