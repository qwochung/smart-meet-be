package com.example.smartmeetbe.dto.response;

import com.example.smartmeetbe.constant.MergeStatus;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record MergedTranscriptResponse(
        String roomId,
        int version,
        MergeStatus status,
        String fullText,
        String smoothedText,
        List<TranscriptSegmentDto> segments,
        int processedChunkCount,
        LocalDateTime lastMergedAt
) {
}
