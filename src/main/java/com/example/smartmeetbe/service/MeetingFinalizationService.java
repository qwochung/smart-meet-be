package com.example.smartmeetbe.service;

import com.example.smartmeetbe.dto.response.MergedTranscriptResponse;

public interface MeetingFinalizationService {
    void finalizeAsync(String roomId);

    MergedTranscriptResponse getFinalTranscript(String roomId);
}
