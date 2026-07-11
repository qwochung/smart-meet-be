package com.example.smartmeetbe.service.impl;

import com.example.smartmeetbe.config.LiveKitConfig;
import com.example.smartmeetbe.constant.ErrorCode;
import com.example.smartmeetbe.constant.JoinRoomStatus;
import com.example.smartmeetbe.constant.MeetingType;
import com.example.smartmeetbe.constant.Role;
import com.example.smartmeetbe.constant.RoomStatus;
import com.example.smartmeetbe.dto.mapper.RoomMapper;
import com.example.smartmeetbe.constant.RecurrenceType;
import com.example.smartmeetbe.dto.request.RoomRequest;
import com.example.smartmeetbe.dto.request.ScheduleMeetingRequest;
import com.example.smartmeetbe.dto.response.RoomResponse;
import com.example.smartmeetbe.entity.JoinRoom;
import com.example.smartmeetbe.entity.Room;
import com.example.smartmeetbe.entity.User;
import com.example.smartmeetbe.exception.AppException;
import com.example.smartmeetbe.repository.JoinRoomRepository;
import com.example.smartmeetbe.repository.RoomRepository;
import com.example.smartmeetbe.service.LiveKitTokenService;
import com.example.smartmeetbe.service.RoomService;
import com.example.smartmeetbe.service.UserService;
import com.example.smartmeetbe.utils.RoomCodeGenerator;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import lombok.experimental.FieldDefaults;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.smartmeetbe.document.MeetingSummary;
import com.example.smartmeetbe.document.RoomTranscript;
import com.example.smartmeetbe.dto.response.RoomMinuteResponse;
import com.example.smartmeetbe.dto.response.PageResponse;
import com.example.smartmeetbe.dto.response.DashboardResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import com.example.smartmeetbe.repository.mongo.MeetingSummaryRepository;
import com.example.smartmeetbe.repository.mongo.RoomTranscriptRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
@Slf4j
public class RoomServiceImpl implements RoomService {
    final RoomRepository roomRepository;
    final RoomMapper roomMapper;
    final UserService userService;
    private final LiveKitTokenService liveKitTokenService;
    private final LiveKitConfig liveKitConfig;
    private final StringRedisTemplate redisTemplate;
    private final JoinRoomRepository joinRoomRepository;
    final MeetingSummaryRepository meetingSummaryRepository;
    final RoomTranscriptRepository roomTranscriptRepository;
    private final com.example.smartmeetbe.service.MeetingFinalizationService meetingFinalizationService;

    @Value("${app.room.max-participants}")
    private int maxParticipants;

    @Value("${app.room.max-active-rooms}")
    private int maxActiveRooms;

    @Value("${app.room.duration-minutes}")
    private int durationMinutes;

    // Redis key prefix để track số participant đang trong phòng
    private static final String ROOM_PARTICIPANT_KEY = "room:participants:";
    // Redis key để lock khi join (tránh race condition)
    private static final String ROOM_JOIN_LOCK_KEY  = "room:join:lock:";

    @Override
    @Transactional
    public RoomResponse createRoom(RoomRequest request, String hostEmail) {
        // 1. Enforce max active rooms
        long activeRoomCount = roomRepository.countByStatus(RoomStatus.ACTIVE);
        if (activeRoomCount >= maxActiveRooms) {
            throw new AppException(ErrorCode.MAX_ACTIVE_ROOMS_REACHED);
        }

        User host = userService.findByEmail(hostEmail);

        // 2. Tạo room trong DB — phòng hẹn giờ tính hết hạn từ scheduledAt, không phải từ lúc tạo
        LocalDateTime startBase = request.getScheduledAt() != null ? request.getScheduledAt() : LocalDateTime.now();
        Room room = Room.builder()
                .name(request.getName())
                .description(request.getDescription())
                .hostUser(host)
                .hostId(host.getId())
                .status(request.getScheduledAt() == null ? RoomStatus.ACTIVE : RoomStatus.WAITING)
                .scheduledAt(request.getScheduledAt())
                .roomCode(generateUniqueRoomCode())
                .expiresAt(startBase.plusMinutes(durationMinutes))
                .participants(new ArrayList<>(List.of(host)))
                .typeCode(request.getTypeCode() != null ? request.getTypeCode() : MeetingType.GENERAL)
                .build();

        roomRepository.save(room);

        // 3. Ghi host vào JoinRoom với role HOST
        JoinRoom hostJoin = JoinRoom.builder()
                .room(room)
                .user(host)
                .role(Role.HOST)
                .status(JoinRoomStatus.APPROVED)
                .build();
        joinRoomRepository.save(hostJoin);

        // 4. Khởi tạo counter Redis: host đang trong phòng = 1
        String redisKey = ROOM_PARTICIPANT_KEY + room.getRoomCode();
        // TTL dư 30 phút so với thời lượng phòng để counter không biến mất giữa buổi họp
        redisTemplate.opsForValue().set(redisKey, "1", durationMinutes + 30L, TimeUnit.MINUTES);

        log.info("Room created: {} by host: {}", room.getRoomCode(), hostEmail);

        String livekitToken = liveKitTokenService.generateToken(
                host.getEmail(),
                host.getName(),
                room.getRoomCode(),
                Role.HOST
        );

        var response = roomMapper.toResponse(room);
        response.setLivekitToken(livekitToken);
        response.setLivekitHost(liveKitConfig.getHost());
        return response;
    }

    private String generateUniqueRoomCode() {
        String code;
        int retry = 0;
        do {
            code = RoomCodeGenerator.generate();
            retry++;
            if (retry > 5) {
                throw new RuntimeException("Cannot generate unique room code");
            }
        } while (roomRepository.existsByRoomCode(code));
        return code;
    }

    @Override
    public RoomResponse getRoomByCode(String code) {
        Room room = roomRepository.findByRoomCode(code)
                .filter(r -> r.getStatus() != RoomStatus.ENDED)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
        return roomMapper.toResponse(room);
    }

    @Override
    public Room findByRoomCodeAndStatus(String code, RoomStatus roomStatus) {
        return roomRepository.findByRoomCodeAndStatus(code, roomStatus)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
    }

    @Override
    @Transactional
    public void endRoom(String roomCode, String hostEmail) {
        Room room = roomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));

        if (!room.getHostUser().getEmail().equals(hostEmail)) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
        if (room.getStatus() == RoomStatus.ENDED) {
            return; // idempotent: bấm kết thúc 2 lần không lỗi
        }

        room.setStatus(RoomStatus.ENDED);
        if (room.getActualEndedAt() == null) {
            room.setActualEndedAt(LocalDateTime.now());
        }
        roomRepository.save(room);

        redisTemplate.delete(ROOM_PARTICIPANT_KEY + roomCode);
        meetingFinalizationService.finalizeAsync(roomCode);
        log.info("Room {} ended by host {}", roomCode, hostEmail);
    }

    @Override
    public PageResponse<RoomMinuteResponse> getRoomMinutesForUser(String userEmail, String name, String dateStr, int page, int size) {
        User user = userService.findByEmail(userEmail);

        LocalDateTime startDate = null;
        LocalDateTime endDate = null;

        if (dateStr != null && !dateStr.isBlank()) {
            try {
                java.time.LocalDate date = java.time.LocalDate.parse(dateStr);
                startDate = date.atStartOfDay();
                endDate = date.atTime(23, 59, 59, 999999999);
            } catch (java.time.format.DateTimeParseException e) {
                log.warn("Invalid date format: {}", dateStr);
            }
        }

        String searchName = (name != null && !name.isBlank()) ? name : null;

        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 100);

        Page<Room> roomPage = roomRepository.findRoomsForUserPaged(
                user.getId(), searchName, startDate, endDate,
                PageRequest.of(safePage, safeSize));

        List<RoomMinuteResponse> items = roomPage.getContent().stream()
                .map(room -> RoomMinuteResponse.builder()
                        .roomCode(room.getRoomCode())
                        .name(room.getName())
                        .description(room.getDescription())
                        .scheduledAt(room.getScheduledAt())
                        .expiresAt(room.getExpiresAt())
                        .status(room.getStatus())
                        .recurrenceRule(room.getRecurrenceRule())
                        .build())
                .collect(Collectors.toList());

        return new PageResponse<>(items, safePage, safeSize,
                roomPage.getTotalElements(), roomPage.getTotalPages());
    }

    @Override
    public DashboardResponse getDashboard(String userEmail) {
        User user = userService.findByEmail(userEmail);

        // Lấy toàn bộ phòng user tham gia (host hoặc participant)
        List<Room> rooms = roomRepository.findRoomsForUser(user.getId(), null, null, null);

        // Mốc tuần hiện tại: thứ Hai 00:00 -> Chủ nhật 23:59:59
        LocalDate today = LocalDate.now();
        LocalDateTime weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
        LocalDateTime weekEnd = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).atTime(23, 59, 59, 999_999_999);

        // ===== Thống kê tổng quan =====
        long totalMeetings = rooms.size();
        long minutesCreated = rooms.stream()
                .filter(r -> r.getStatus() == RoomStatus.ENDED)
                .count();
        long activeRooms = rooms.stream()
                .filter(r -> r.getStatus() == RoomStatus.ACTIVE)
                .count();
        long waitingRooms = rooms.stream()
                .filter(r -> r.getStatus() == RoomStatus.WAITING)
                .count();
        long meetingsThisWeek = rooms.stream()
                .map(Room::getCreatedAt)
                .filter(c -> c != null && !c.isBefore(weekStart) && !c.isAfter(weekEnd))
                .count();

        DashboardResponse.Stats stats = DashboardResponse.Stats.builder()
                .totalMeetings(totalMeetings)
                .meetingsThisWeek(meetingsThisWeek)
                .minutesCreated(minutesCreated)
                .activeRooms(activeRooms)
                .waitingRooms(waitingRooms)
                .build();

        // ===== Xu hướng theo tuần (Thứ 2 -> Chủ nhật) =====
        Map<DayOfWeek, Long> countByDay = rooms.stream()
                .map(Room::getCreatedAt)
                .filter(c -> c != null && !c.isBefore(weekStart) && !c.isAfter(weekEnd))
                .collect(Collectors.groupingBy(c -> c.getDayOfWeek(), Collectors.counting()));

        Map<DayOfWeek, String> dayLabels = new LinkedHashMap<>();
        dayLabels.put(DayOfWeek.MONDAY, "T2");
        dayLabels.put(DayOfWeek.TUESDAY, "T3");
        dayLabels.put(DayOfWeek.WEDNESDAY, "T4");
        dayLabels.put(DayOfWeek.THURSDAY, "T5");
        dayLabels.put(DayOfWeek.FRIDAY, "T6");
        dayLabels.put(DayOfWeek.SATURDAY, "T7");
        dayLabels.put(DayOfWeek.SUNDAY, "CN");

        List<DashboardResponse.TrendPoint> weeklyTrend = dayLabels.entrySet().stream()
                .map(e -> DashboardResponse.TrendPoint.builder()
                        .day(e.getValue())
                        .meetings(countByDay.getOrDefault(e.getKey(), 0L))
                        .build())
                .collect(Collectors.toList());

        // ===== Phân bố cuộc họp theo khung giờ trong ngày (dựa trên createdAt) =====
        String[] hourLabels = {"0-6h", "6-9h", "9-12h", "12-15h", "15-18h", "18-24h"};
        long[] hourCounts = new long[hourLabels.length];
        for (Room r : rooms) {
            LocalDateTime created = r.getCreatedAt();
            if (created == null) continue;
            int hour = created.getHour();
            int bucket;
            if (hour < 6) bucket = 0;
            else if (hour < 9) bucket = 1;
            else if (hour < 12) bucket = 2;
            else if (hour < 15) bucket = 3;
            else if (hour < 18) bucket = 4;
            else bucket = 5;
            hourCounts[bucket]++;
        }
        List<DashboardResponse.HourBucket> hourlyDistribution = new ArrayList<>();
        for (int i = 0; i < hourLabels.length; i++) {
            hourlyDistribution.add(DashboardResponse.HourBucket.builder()
                    .label(hourLabels[i])
                    .meetings(hourCounts[i])
                    .build());
        }

        // ===== Cuộc họp sắp tới (chưa kết thúc), sắp xếp theo thời điểm gần nhất =====
        List<DashboardResponse.MeetingItem> upcomingMeetings = rooms.stream()
                .filter(r -> r.getStatus() != RoomStatus.ENDED)
                .sorted(Comparator.comparing(
                        r -> r.getScheduledAt() != null ? r.getScheduledAt() : r.getExpiresAt(),
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(5)
                .map(r -> DashboardResponse.MeetingItem.builder()
                        .roomCode(r.getRoomCode())
                        .name(r.getName())
                        .status(r.getStatus())
                        .scheduledAt(r.getScheduledAt())
                        .expiresAt(r.getExpiresAt())
                        .participants(getLiveParticipantCount(r.getRoomCode()))
                        .recurrenceRule(r.getRecurrenceRule())
                        .build())
                .collect(Collectors.toList());

        // ===== Biên bản gần đây (phòng đã kết thúc), mới nhất trước =====
        List<DashboardResponse.MeetingItem> recentMinutes = rooms.stream()
                .filter(r -> r.getStatus() == RoomStatus.ENDED)
                .sorted(Comparator.comparing(
                        (Room r) -> r.getActualEndedAt() != null ? r.getActualEndedAt() : r.getExpiresAt(),
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(5)
                .map(r -> DashboardResponse.MeetingItem.builder()
                        .roomCode(r.getRoomCode())
                        .name(r.getName())
                        .status(r.getStatus())
                        .scheduledAt(r.getScheduledAt())
                        .expiresAt(r.getExpiresAt())
                        .participants(0)
                        .build())
                .collect(Collectors.toList());

        // ===== Trạng thái phòng đang trực tiếp =====
        List<DashboardResponse.RoomStatusItem> roomStatus = rooms.stream()
                .filter(r -> r.getStatus() == RoomStatus.ACTIVE)
                .limit(6)
                .map(r -> DashboardResponse.RoomStatusItem.builder()
                        .roomCode(r.getRoomCode())
                        .name(r.getName())
                        .live(true)
                        .participants(getLiveParticipantCount(r.getRoomCode()))
                        .build())
                .collect(Collectors.toList());

        return DashboardResponse.builder()
                .stats(stats)
                .weeklyTrend(weeklyTrend)
                .hourlyDistribution(hourlyDistribution)
                .upcomingMeetings(upcomingMeetings)
                .recentMinutes(recentMinutes)
                .roomStatus(roomStatus)
                .build();
    }

    @Override
    @Transactional
    public List<RoomResponse> scheduleRecurringMeetings(ScheduleMeetingRequest request, String hostEmail) {
        User host = userService.findByEmail(hostEmail);

        RecurrenceType recurrenceType = request.getRecurrenceType() != null
                ? request.getRecurrenceType()
                : RecurrenceType.NONE;
        int occurrences = recurrenceType == RecurrenceType.NONE ? 1 : Math.max(1, request.getOccurrences());
        String recurrenceRule = recurrenceType.label();

        List<RoomResponse> created = new ArrayList<>();
        LocalDateTime firstStart = request.getScheduledAt();

        for (int i = 0; i < occurrences; i++) {
            LocalDateTime startAt = switch (recurrenceType) {
                case DAILY -> firstStart.plusDays(i);
                case WEEKLY -> firstStart.plusWeeks(i);
                case MONTHLY -> firstStart.plusMonths(i);
                default -> firstStart;
            };

            Room room = Room.builder()
                    .name(request.getName())
                    .description(request.getDescription())
                    .hostUser(host)
                    .hostId(host.getId())
                    .status(RoomStatus.WAITING)
                    .scheduledAt(startAt)
                    .roomCode(generateUniqueRoomCode())
                    .expiresAt(startAt.plusMinutes(durationMinutes))
                    .participants(new ArrayList<>(List.of(host)))
                    .recurrenceRule(occurrences > 1 ? recurrenceRule : null)
                    .typeCode(request.getTypeCode() != null ? request.getTypeCode() : MeetingType.GENERAL)
                    .build();

            roomRepository.save(room);

            JoinRoom hostJoin = JoinRoom.builder()
                    .room(room)
                    .user(host)
                    .role(Role.HOST)
                    .status(JoinRoomStatus.APPROVED)
                    .build();
            joinRoomRepository.save(hostJoin);

            created.add(roomMapper.toResponse(room));
        }

        log.info("Scheduled {} recurring meeting(s) ({}) for host {}", occurrences, recurrenceType, hostEmail);
        return created;
    }

    // Đọc số participant đang trong phòng từ Redis (an toàn nếu key không tồn tại)
    private int getLiveParticipantCount(String roomCode) {
        try {
            String value = redisTemplate.opsForValue().get(ROOM_PARTICIPANT_KEY + roomCode);
            return value != null ? Integer.parseInt(value) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}