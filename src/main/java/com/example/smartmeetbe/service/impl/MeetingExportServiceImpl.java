package com.example.smartmeetbe.service.impl;

import com.example.smartmeetbe.document.MeetingSummary;
import com.example.smartmeetbe.entity.Room;
import com.example.smartmeetbe.entity.User;
import com.example.smartmeetbe.repository.RoomRepository;
import com.example.smartmeetbe.repository.mongo.MeetingSummaryRepository;
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

    @Override
    public byte[] exportSummaryPdf(String roomId) throws Exception {
        // 1. Tìm thông tin phòng họp từ Postgres
        Room room = roomRepository.findByRoomCode(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found with code: " + roomId));

        // 2. Tìm thông tin tóm tắt từ MongoDB
        MeetingSummary summary = meetingSummaryRepository.findByRoomId(roomId)
                .orElseThrow(() -> new IllegalStateException("Meeting summary not generated yet for room: " + roomId));

        // 3. Gộp metadata và dữ liệu tóm tắt vào Map
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("meetingName", room.getName());

        // Định dạng ngày họp
        LocalDateTime date = (room.getScheduledAt() != null) ? room.getScheduledAt() : room.getCreatedAt();
        if (date == null) {
            date = LocalDateTime.now();
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        templateData.put("meetingDate", date.format(formatter));

        // Người chủ trì và thành viên
        User host = room.getHostUser();
        templateData.put("hostName", (host != null) ? host.getName() : "Chưa cập nhật");

        List<String> attendees = (room.getParticipants() != null)
                ? room.getParticipants().stream().map(User::getName).toList()
                : List.of();
        templateData.put("attendees", attendees);

        // Dữ liệu tóm tắt AI
        templateData.put("summary", summary);

        // 4. Chọn loại template tương ứng dựa trên typeCode của phòng họp
        String meetingType = (room.getTypeCode() != null) ? room.getTypeCode().name() : "GENERAL";

        String templateName;
        switch (meetingType) {
            case "SCRUM_SYNC":
                templateName = "scrum";
                break;
            case "CLIENT_SALES":
                templateName = "client";
                break;
            case "BRAINSTORMING":
                templateName = "brainstorming";
                break;
            case "WEBINAR":
                templateName = "webinar";
                break;
            case "INTERVIEW":
                templateName = "interview";
                break;
            default:
                templateName = "general";
                break;
        }

        log.info("Generating PDF report in Service for room {} using template type {}", roomId, templateName);
        return pdfExportService.generateMeetingPdf(templateName, templateData);
    }
}
