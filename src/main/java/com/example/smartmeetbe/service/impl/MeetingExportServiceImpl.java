package com.example.smartmeetbe.service.impl;

import com.example.smartmeetbe.constant.MinutesFormat;
import com.example.smartmeetbe.document.MeetingSummary;
import com.example.smartmeetbe.entity.Room;
import com.example.smartmeetbe.entity.User;
import com.example.smartmeetbe.repository.RoomRepository;
import com.example.smartmeetbe.repository.mongo.MeetingSummaryRepository;
import com.example.smartmeetbe.service.DocxExportService;
import com.example.smartmeetbe.service.MeetingExportService;
import com.example.smartmeetbe.service.PdfExportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeetingExportServiceImpl implements MeetingExportService {

    private final RoomRepository roomRepository;
    private final MeetingSummaryRepository meetingSummaryRepository;
    private final PdfExportService pdfExportService;
    private final DocxExportService docxExportService;

    @Override
    public byte[] exportSummaryPdf(String roomId) throws Exception {
        Room room = loadRoom(roomId);
        MeetingSummary summary = loadSummary(roomId);
        Map<String, Object> templateData = buildTemplateData(room, summary);
        String templateName = resolveTemplateName(room, summary);

        log.info("Generating PDF report in Service for room {} using template type {}", roomId, templateName);
        return pdfExportService.generateMeetingPdf(templateName, templateData);
    }

    @Override
    public byte[] exportSummaryDocx(String roomId) throws Exception {
        Room room = loadRoom(roomId);
        Map<String, Object> templateData = buildTemplateData(room, loadSummary(roomId));

        log.info("Generating DOCX report in Service for room {}", roomId);
        return docxExportService.generateMeetingDocx(templateData);
    }

    private Room loadRoom(String roomId) {
        return roomRepository.findByRoomCode(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found with code: " + roomId));
    }

    private MeetingSummary loadSummary(String roomId) {
        return meetingSummaryRepository.findByRoomId(roomId)
                .orElseThrow(() -> new IllegalStateException("Meeting summary not generated yet for room: " + roomId));
    }

    /** Gộp metadata phòng họp và dữ liệu tóm tắt AI vào Map dùng chung cho PDF lẫn DOCX. */
    private Map<String, Object> buildTemplateData(Room room, MeetingSummary summary) {
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("meetingName", room.getName());

        LocalDateTime date = (room.getScheduledAt() != null) ? room.getScheduledAt() : room.getCreatedAt();
        if (date == null) {
            date = LocalDateTime.now();
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        templateData.put("meetingDate", date.format(formatter));

        User host = room.getHostUser();
        templateData.put("hostName", (host != null) ? host.getName() : "Chưa cập nhật");

        List<String> attendees = (room.getParticipants() != null)
                ? room.getParticipants().stream().map(User::getName).toList()
                : List.of();
        templateData.put("attendees", attendees);

        templateData.put("summary", summary);

        // Verbatim minutes không có dữ liệu tóm tắt; template và DOCX đọc trực tiếp phần nguyên văn
        templateData.put("verbatimText", summary.getVerbatimText() != null ? summary.getVerbatimText() : "");
        return templateData;
    }

    /**
     * Chọn template PDF: mẫu biên bản VERBATIM có bố cục riêng (bản ghi nguyên văn),
     * các mẫu còn lại vẫn chọn theo typeCode của phòng họp.
     */
    private String resolveTemplateName(Room room, MeetingSummary summary) {
        MinutesFormat format = (summary.getMinutesFormat() != null) ? summary.getMinutesFormat() : room.getMinutesFormat();
        if (format == MinutesFormat.VERBATIM) {
            return "verbatim";
        }

        // Loại do người dùng tự tạo không có template riêng nên dùng bố cục chung
        String meetingType = (room.getTypeCode() != null) ? room.getTypeCode() : "GENERAL";
        switch (meetingType) {
            case "SCRUM_SYNC":
                return "scrum";
            case "CLIENT_SALES":
                return "client";
            case "BRAINSTORMING":
                return "brainstorming";
            case "WEBINAR":
                return "webinar";
            default:
                return "general";
        }
    }
}
