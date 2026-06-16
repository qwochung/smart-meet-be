package com.example.smartmeetbe.service;

import com.example.smartmeetbe.document.TranscriptChunk;
import com.example.smartmeetbe.dto.response.FullMergeResult;

import java.util.List;

public interface FullTranscriptMergeService {
    FullMergeResult mergeAll(String roomId, List<TranscriptChunk> chunks);
}
