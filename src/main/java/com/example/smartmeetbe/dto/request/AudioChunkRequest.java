package com.example.smartmeetbe.dto.request;

import lombok.Data;

@Data
public class AudioChunkRequest {
    private String roomId;
    private String participantId;
    private String participantName;
    private Integer chunkIndex;
    private Long startTimeMs;
    private Long endTimeMs;
    private Integer sampleRate;
    private Integer channels;
    private String audioDataBase64;
    private boolean isForceCut;
}
