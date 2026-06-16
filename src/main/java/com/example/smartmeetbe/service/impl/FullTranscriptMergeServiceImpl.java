package com.example.smartmeetbe.service.impl;

import com.example.smartmeetbe.config.MergeConfig;
import com.example.smartmeetbe.document.TranscriptChunk;
import com.example.smartmeetbe.dto.response.FullMergeResult;
import com.example.smartmeetbe.dto.response.TranscriptSegmentDto;
import com.example.smartmeetbe.merge.MergeSegment;
import com.example.smartmeetbe.merge.ParticipantMergeState;
import com.example.smartmeetbe.merge.RoomMergeState;
import com.example.smartmeetbe.service.FullTranscriptMergeService;
import com.example.smartmeetbe.util.TextOverlapUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class FullTranscriptMergeServiceImpl implements FullTranscriptMergeService {

    private static final Comparator<TranscriptChunk> CHUNK_ORDER = Comparator
            .comparing(TranscriptChunk::getStartTimeMs, Comparator.nullsLast(Long::compareTo))
            .thenComparing(TranscriptChunk::getParticipantId, Comparator.nullsLast(String::compareTo))
            .thenComparing(TranscriptChunk::getChunkIndex, Comparator.nullsLast(Integer::compareTo))
            .thenComparing(TranscriptChunk::getCreatedAt, Comparator.nullsLast(Instant::compareTo))
            .thenComparing(TranscriptChunk::getId, Comparator.nullsLast(String::compareTo));

    private final MergeConfig mergeConfig;

    @Override
    public FullMergeResult mergeAll(String roomId, List<TranscriptChunk> chunks) {
        RoomMergeState state = new RoomMergeState(roomId);
        state.setHydrated(true);

        List<TranscriptChunk> ordered = chunks == null ? List.of() : chunks.stream()
                .sorted(CHUNK_ORDER)
                .toList();

        for (TranscriptChunk chunk : ordered) {
            ingestChunk(state, chunk);
        }

        drainAllPending(state);
        rebuildFullText(state);

        List<TranscriptSegmentDto> segments = state.getTimeline().stream()
                .map(this::toDto)
                .toList();

        return FullMergeResult.builder()
                .fullText(state.getFullTextCache())
                .segments(segments)
                .build();
    }

    private void ingestChunk(RoomMergeState room, TranscriptChunk chunk) {
        if (chunk.getId() == null || room.getProcessedChunkIds().contains(chunk.getId())) {
            return;
        }
        if (chunk.getContent() == null || chunk.getContent().isBlank()) {
            room.getProcessedChunkIds().add(chunk.getId());
            room.setLastProcessedChunkId(chunk.getId());
            return;
        }

        ParticipantMergeState participant = room.getParticipants().computeIfAbsent(
                chunk.getParticipantId(),
                id -> new ParticipantMergeState(id, chunk.getParticipantName())
        );
        if (chunk.getParticipantName() != null && !chunk.getParticipantName().isBlank()) {
            participant.setParticipantName(chunk.getParticipantName());
        }

        Integer chunkIndex = chunk.getChunkIndex() != null ? chunk.getChunkIndex() : -1;
        if (chunkIndex <= participant.getLastMergedChunkIndex()) {
            room.getProcessedChunkIds().add(chunk.getId());
            return;
        }

        int expected = participant.getLastMergedChunkIndex() + 1;
        if (chunkIndex > expected) {
            participant.getPendingByIndex().putIfAbsent(chunkIndex, chunk);
            return;
        }

        mergeChunkNow(room, participant, chunk);
    }

    private void mergeChunkNow(RoomMergeState room, ParticipantMergeState participant, TranscriptChunk chunk) {
        String deduped = TextOverlapUtil.removeOverlap(participant.getOverlapContext(), chunk.getContent());
        room.getProcessedChunkIds().add(chunk.getId());
        room.setLastProcessedChunkId(chunk.getId());

        int chunkIndex = chunk.getChunkIndex() != null ? chunk.getChunkIndex() : participant.getLastMergedChunkIndex() + 1;
        participant.setLastMergedChunkIndex(Math.max(participant.getLastMergedChunkIndex(), chunkIndex));
        participant.getPendingByIndex().remove(chunkIndex);

        if (!deduped.isBlank()) {
            TextOverlapUtil.appendWithSpace(participant.getMergedText(), deduped);
            participant.setOverlapContext(TextOverlapUtil.extractContext(participant.getMergedText().toString()));
            updateTimeline(room, participant, chunk, deduped);
        }

        drainPending(room, participant);
    }

    private void drainPending(RoomMergeState room, ParticipantMergeState participant) {
        while (true) {
            int nextIndex = participant.getLastMergedChunkIndex() + 1;
            TranscriptChunk pending = participant.getPendingByIndex().remove(nextIndex);
            if (pending == null) {
                break;
            }
            mergeChunkNow(room, participant, pending);
        }
    }

    private void drainAllPending(RoomMergeState room) {
        boolean progressed;
        do {
            progressed = false;
            for (ParticipantMergeState participant : room.getParticipants().values()) {
                int sizeBefore = participant.getPendingByIndex().size();
                drainPending(room, participant);
                if (participant.getPendingByIndex().size() < sizeBefore) {
                    progressed = true;
                }
            }
        } while (progressed);

        for (ParticipantMergeState participant : room.getParticipants().values()) {
            if (participant.getPendingByIndex().isEmpty()) {
                continue;
            }
            List<Integer> indexes = participant.getPendingByIndex().keySet().stream().sorted().toList();
            for (Integer index : indexes) {
                TranscriptChunk pending = participant.getPendingByIndex().remove(index);
                if (pending != null) {
                    mergeChunkNow(room, participant, pending);
                }
            }
        }
    }

    private void updateTimeline(
            RoomMergeState room, ParticipantMergeState participant,
            TranscriptChunk chunk, String deduped) {

        MergeSegment open = participant.getOpenSegment();
        long gap = open != null && chunk.getStartTimeMs() != null && open.getEndTimeMs() != null
                ? chunk.getStartTimeMs() - open.getEndTimeMs()
                : Long.MAX_VALUE;

        boolean openIsLastTurn = open != null
                && !room.getTimeline().isEmpty()
                && room.getTimeline().get(room.getTimeline().size() - 1) == open;
        boolean extendSameSpeaker = open != null
                && Objects.equals(open.getParticipantId(), chunk.getParticipantId())
                && gap < mergeConfig.getGapThresholdMs()
                && openIsLastTurn;

        if (extendSameSpeaker) {
            TextOverlapUtil.appendWithSpace(open.getContent(), deduped);
            open.setEndTimeMs(chunk.getEndTimeMs());
            open.setLastSourceChunkId(chunk.getId());
            return;
        }

        MergeSegment segment = new MergeSegment();
        segment.setParticipantId(chunk.getParticipantId());
        segment.setParticipantName(participant.getParticipantName());
        segment.setStartTimeMs(chunk.getStartTimeMs());
        segment.setEndTimeMs(chunk.getEndTimeMs());
        segment.setContent(new StringBuilder(deduped));
        segment.setLastSourceChunkId(chunk.getId());

        insertSegmentByTime(room, segment);
        participant.setOpenSegment(segment);
    }

    private void insertSegmentByTime(RoomMergeState room, MergeSegment segment) {
        int insertAt = room.getTimeline().size();
        Long startMs = segment.getStartTimeMs();
        if (startMs != null) {
            for (int i = 0; i < room.getTimeline().size(); i++) {
                Long existingStart = room.getTimeline().get(i).getStartTimeMs();
                if (existingStart != null && startMs < existingStart) {
                    insertAt = i;
                    break;
                }
            }
        }
        room.getTimeline().add(insertAt, segment);
        reindexTimeline(room);
    }

    private void reindexTimeline(RoomMergeState room) {
        for (int i = 0; i < room.getTimeline().size(); i++) {
            room.getTimeline().get(i).setOrderIndex(i);
        }
    }

    private void rebuildFullText(RoomMergeState state) {
        String fullText = state.getTimeline().stream()
                .map(segment -> "[" + segment.getParticipantName() + "]: " + segment.getContent())
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");
        state.setFullTextCache(fullText);
    }

    private TranscriptSegmentDto toDto(MergeSegment segment) {
        return TranscriptSegmentDto.builder()
                .orderIndex(segment.getOrderIndex())
                .participantId(segment.getParticipantId())
                .participantName(segment.getParticipantName())
                .startTimeMs(segment.getStartTimeMs())
                .endTimeMs(segment.getEndTimeMs())
                .content(segment.getContent().toString())
                .sourceChunkId(segment.getLastSourceChunkId())
                .build();
    }
}
