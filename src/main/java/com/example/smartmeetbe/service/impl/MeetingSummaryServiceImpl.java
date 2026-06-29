package com.example.smartmeetbe.service.impl;

import com.example.smartmeetbe.document.MeetingSummary;
import com.example.smartmeetbe.dto.response.MasterMeetingSummaryDto;
import com.example.smartmeetbe.dto.response.MeetingSummaryDetailResponse;
import com.example.smartmeetbe.entity.Room;
import com.example.smartmeetbe.entity.User;
import com.example.smartmeetbe.repository.RoomRepository;
import com.example.smartmeetbe.repository.mongo.MeetingSummaryRepository;
import com.example.smartmeetbe.service.MeetingSummaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingSummaryServiceImpl implements MeetingSummaryService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final RoomRepository roomRepository;
    private final MeetingSummaryRepository meetingSummaryRepository;

    @Override
    @Transactional(readOnly = true)
    public MeetingSummaryDetailResponse getSummaryDetail(String roomId) {
        Room room = roomRepository.findByRoomCode(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found with code: " + roomId));

        MeetingSummary summary = meetingSummaryRepository.findByRoomId(roomId)
                .orElseThrow(() -> new IllegalStateException("Meeting summary not generated yet for room: " + roomId));

        User host = room.getHostUser();
        List<String> attendees = (room.getParticipants() != null)
                ? room.getParticipants().stream().map(User::getName).toList()
                : List.of();

        LocalDateTime date = (room.getScheduledAt() != null) ? room.getScheduledAt() : room.getCreatedAt();

        return MeetingSummaryDetailResponse.builder()
                .roomId(roomId)
                .meetingName(room.getName())
                .meetingDate(date != null ? date.format(DATE_FORMATTER) : "")
                .hostName(host != null ? host.getName() : "Chưa cập nhật")
                .attendees(attendees)
                .attendeeCount(attendees.size())
                .durationMinutes(computeDurationMinutes(room))
                .summary(toDto(summary))
                .build();
    }

    @Override
    @Transactional
    public MasterMeetingSummaryDto updateSummary(String roomId, MasterMeetingSummaryDto edited) {
        if (edited == null || edited.getExecutiveSummary() == null || edited.getExecutiveSummary().isBlank()) {
            throw new IllegalArgumentException("Executive summary must not be empty");
        }

        MeetingSummary summary = meetingSummaryRepository.findByRoomId(roomId)
                .orElseThrow(() -> new IllegalStateException("Meeting summary not generated yet for room: " + roomId));

        summary.setExecutiveSummary(edited.getExecutiveSummary());
        summary.setDiscussionTopics(edited.getDiscussionTopics());
        summary.setDecisionsMade(edited.getDecisionsMade());
        summary.setActionItems(edited.getActionItems());
        summary.setQaPairs(edited.getQaPairs());
        summary.setPainPoints(edited.getPainPoints());
        summary.setProsAndCons(edited.getProsAndCons());

        MeetingSummary saved = meetingSummaryRepository.save(summary);
        log.info("Updated meeting summary for room {}", roomId);
        return toDto(saved);
    }

    private Long computeDurationMinutes(Room room) {
        if (room.getActualStartedAt() == null || room.getActualEndedAt() == null) {
            return null;
        }
        long minutes = Duration.between(room.getActualStartedAt(), room.getActualEndedAt()).toMinutes();
        return minutes >= 0 ? minutes : null;
    }

    private MasterMeetingSummaryDto toDto(MeetingSummary doc) {
        return MasterMeetingSummaryDto.builder()
                .executiveSummary(doc.getExecutiveSummary())
                .discussionTopics(doc.getDiscussionTopics())
                .decisionsMade(doc.getDecisionsMade())
                .actionItems(doc.getActionItems())
                .qaPairs(doc.getQaPairs())
                .painPoints(doc.getPainPoints())
                .prosAndCons(doc.getProsAndCons())
                .build();
    }
}
