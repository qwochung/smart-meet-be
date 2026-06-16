package com.example.smartmeetbe.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record FullMergeResult(
        String fullText,
        List<TranscriptSegmentDto> segments
) {
}
