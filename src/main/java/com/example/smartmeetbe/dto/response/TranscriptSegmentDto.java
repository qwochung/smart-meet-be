package com.example.smartmeetbe.dto.response;

import lombok.Builder;

@Builder
public record TranscriptSegmentDto(
        int orderIndex,
        String participantId,
        String participantName,
        Long startTimeMs,
        Long endTimeMs,
        String content,
        String sourceChunkId
) {
}
