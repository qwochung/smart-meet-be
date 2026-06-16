package com.example.smartmeetbe.merge;

import com.example.smartmeetbe.document.TranscriptChunk;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class ParticipantMergeState {

    private final String participantId;

    @Setter
    private String participantName;

    private final StringBuilder mergedText = new StringBuilder();

    @Setter
    private String overlapContext = "";

    @Setter
    private int lastMergedChunkIndex = -1;

    @Setter
    private MergeSegment openSegment;

    private final Map<Integer, TranscriptChunk> pendingByIndex = new HashMap<>();

    @Setter
    private long firstPendingAtMs = 0;

    public ParticipantMergeState(String participantId, String participantName) {
        this.participantId = participantId;
        this.participantName = participantName;
    }
}
