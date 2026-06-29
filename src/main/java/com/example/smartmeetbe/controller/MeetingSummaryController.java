package com.example.smartmeetbe.controller;

import com.example.smartmeetbe.dto.response.ApiResponse;
import com.example.smartmeetbe.dto.response.MasterMeetingSummaryDto;
import com.example.smartmeetbe.dto.response.MeetingSummaryDetailResponse;
import com.example.smartmeetbe.service.MeetingSummaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/rooms")
@RequiredArgsConstructor
public class MeetingSummaryController {

    private final MeetingSummaryService meetingSummaryService;

    @GetMapping("/{roomId}/summary")
    public ResponseEntity<ApiResponse<MeetingSummaryDetailResponse>> getSummary(@PathVariable String roomId) {
        try {
            MeetingSummaryDetailResponse detail = meetingSummaryService.getSummaryDetail(roomId);
            return ResponseEntity.ok(ApiResponse.<MeetingSummaryDetailResponse>builder()
                    .success(true)
                    .message("Meeting summary retrieved successfully")
                    .data(detail)
                    .build());
        } catch (IllegalStateException e) {
            // Tóm tắt AI được sinh bất đồng bộ, có thể chưa sẵn sàng -> trả 404 để FE thử lại
            log.debug("Summary not ready yet for room {}: {}", roomId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.<MeetingSummaryDetailResponse>builder()
                    .success(false)
                    .message("Meeting summary not generated yet")
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.<MeetingSummaryDetailResponse>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }

    @PutMapping("/{roomId}/summary")
    public ResponseEntity<ApiResponse<MasterMeetingSummaryDto>> updateSummary(
            @PathVariable String roomId,
            @RequestBody MasterMeetingSummaryDto edited) {
        MasterMeetingSummaryDto updated = meetingSummaryService.updateSummary(roomId, edited);
        return ResponseEntity.ok(ApiResponse.<MasterMeetingSummaryDto>builder()
                .success(true)
                .message("Meeting summary updated successfully")
                .data(updated)
                .build());
    }
}
