package com.example.smartmeetbe.service;

import com.example.smartmeetbe.document.TranscriptChunk;
import com.example.smartmeetbe.dto.response.MergedTranscriptResponse;

public interface DraftTranscriptService {
    void appendChunk(TranscriptChunk chunk);

    MergedTranscriptResponse getDraftTranscript(String roomId);
}
