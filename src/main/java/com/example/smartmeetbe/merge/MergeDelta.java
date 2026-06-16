package com.example.smartmeetbe.merge;

import com.example.smartmeetbe.dto.response.TranscriptSegmentDto;
import lombok.Builder;

import java.util.List;

@Builder
public record MergeDelta(
        int version,
        String fullText,
        List<TranscriptSegmentDto> deltas,
        String pushType
) {
    public static final String TYPE_APPEND = "SEGMENT_APPEND";
    public static final String TYPE_UPDATE = "SEGMENT_UPDATE";
}
