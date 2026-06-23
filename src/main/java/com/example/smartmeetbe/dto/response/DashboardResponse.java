package com.example.smartmeetbe.dto.response;

import com.example.smartmeetbe.constant.RoomStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DashboardResponse {

    Stats stats;
    List<TrendPoint> weeklyTrend;
    List<HourBucket> hourlyDistribution;
    List<MeetingItem> upcomingMeetings;
    List<MeetingItem> recentMinutes;
    List<RoomStatusItem> roomStatus;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Stats {
        long totalMeetings;
        long meetingsThisWeek;
        long minutesCreated;
        long activeRooms;
        long waitingRooms;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class HourBucket {
        String label;
        long meetings;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class TrendPoint {
        String day;
        long meetings;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class MeetingItem {
        String roomCode;
        String name;
        RoomStatus status;
        LocalDateTime scheduledAt;
        LocalDateTime expiresAt;
        int participants;
        String recurrenceRule;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class RoomStatusItem {
        String roomCode;
        String name;
        boolean live;
        int participants;
    }
}
