package com.example.smartmeetbe.controller;

import com.example.smartmeetbe.service.MeetingExportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/rooms")
@RequiredArgsConstructor
public class MeetingExportController {

    private static final MediaType DOCX_MEDIA_TYPE =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    private final MeetingExportService meetingExportService;

    @GetMapping("/{roomId}/summary/download")
    public ResponseEntity<byte[]> downloadSummary(
            @PathVariable String roomId,
            @RequestParam(name = "format", defaultValue = "pdf") String format) {
        try {
            boolean isDocx = "docx".equalsIgnoreCase(format);

            byte[] bytes = isDocx
                    ? meetingExportService.exportSummaryDocx(roomId)
                    : meetingExportService.exportSummaryPdf(roomId);

            String extension = isDocx ? "docx" : "pdf";
            MediaType mediaType = isDocx ? DOCX_MEDIA_TYPE : MediaType.APPLICATION_PDF;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(mediaType);
            headers.setContentDispositionFormData("attachment", "Meeting_Minutes_" + roomId + "." + extension);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(bytes);

        } catch (IllegalArgumentException e) {
            log.warn("Export failed due to invalid arguments: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (IllegalStateException e) {
            log.warn("Export failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        } catch (Exception e) {
            log.error("Unexpected error occurred while exporting meeting summary for room {}: {}", roomId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
