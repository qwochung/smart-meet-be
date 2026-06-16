package com.example.smartmeetbe.merge;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MergeSegment {

    private int orderIndex;
    private String participantId;
    private String participantName;
    private Long startTimeMs;
    private Long endTimeMs;
    private StringBuilder content = new StringBuilder();
    private String lastSourceChunkId;
}
