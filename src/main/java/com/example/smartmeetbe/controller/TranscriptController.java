package com.example.smartmeetbe.controller;

import com.example.smartmeetbe.constant.MergeStatus;
import com.example.smartmeetbe.dto.response.ApiResponse;
import com.example.smartmeetbe.dto.response.MergedTranscriptResponse;
import com.example.smartmeetbe.service.DraftTranscriptService;
import com.example.smartmeetbe.service.MeetingFinalizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rooms")
@RequiredArgsConstructor
public class TranscriptController {

    private final DraftTranscriptService draftTranscriptService;
    private final MeetingFinalizationService meetingFinalizationService;

    @GetMapping("/{code}/transcript/draft")
    public ResponseEntity<ApiResponse<MergedTranscriptResponse>> getDraftTranscript(
            @PathVariable String code) {
        MergedTranscriptResponse response = draftTranscriptService.getDraftTranscript(code);
        return ResponseEntity.ok(ApiResponse.<MergedTranscriptResponse>builder()
                .success(true)
                .message("Draft transcript retrieved successfully")
                .data(response)
                .build());
    }

    @GetMapping("/{code}/transcript/final")
    public ResponseEntity<ApiResponse<MergedTranscriptResponse>> getFinalTranscript(
            @PathVariable String code) {
        MergedTranscriptResponse response = meetingFinalizationService.getFinalTranscript(code);
        return ResponseEntity.ok(ApiResponse.<MergedTranscriptResponse>builder()
                .success(true)
                .message("Final transcript retrieved successfully")
                .data(response)
                .build());
    }

    /** Backward-compatible: LIVE → draft, otherwise → final */
    @GetMapping("/{code}/transcript/merged")
    public ResponseEntity<ApiResponse<MergedTranscriptResponse>> getMergedTranscript(
            @PathVariable String code) {
        MergedTranscriptResponse finalResponse = meetingFinalizationService.getFinalTranscript(code);
        if (finalResponse.status() == MergeStatus.FINAL) {
            return ResponseEntity.ok(ApiResponse.<MergedTranscriptResponse>builder()
                    .success(true)
                    .message("Merged transcript retrieved successfully")
                    .data(finalResponse)
                    .build());
        }
        MergedTranscriptResponse draft = draftTranscriptService.getDraftTranscript(code);
        return ResponseEntity.ok(ApiResponse.<MergedTranscriptResponse>builder()
                .success(true)
                .message("Merged transcript retrieved successfully")
                .data(draft)
                .build());
    }

    @PostMapping("/{code}/transcript/finalize")
    public ResponseEntity<ApiResponse<MergedTranscriptResponse>> finalizeTranscript(
            @PathVariable String code) {
        meetingFinalizationService.finalizeAsync(code);
        MergedTranscriptResponse response = meetingFinalizationService.getFinalTranscript(code);
        return ResponseEntity.ok(ApiResponse.<MergedTranscriptResponse>builder()
                .success(true)
                .message("Transcript finalization started")
                .data(response)
                .build());
    }
}
