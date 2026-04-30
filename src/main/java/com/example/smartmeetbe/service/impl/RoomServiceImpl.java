package com.example.smartmeetbe.service.impl;

import com.example.smartmeetbe.config.LiveKitConfig;
import com.example.smartmeetbe.constant.ErrorCode;
import com.example.smartmeetbe.constant.JoinRoomStatus;
import com.example.smartmeetbe.constant.Role;
import com.example.smartmeetbe.constant.RoomStatus;
import com.example.smartmeetbe.dto.mapper.RoomMapper;
import com.example.smartmeetbe.dto.request.RoomRequest;
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
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import lombok.experimental.FieldDefaults;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

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

        // 2. Tạo room trong DB
        Room room = Room.builder()
                .name(request.getName())
                .description(request.getDescription())
                .hostUser(host)
                .hostId(host.getId())
                .status(request.getScheduledAt() == null ? RoomStatus.ACTIVE : RoomStatus.WAITING)
                .scheduledAt(request.getScheduledAt())
                .roomCode(generateUniqueRoomCode())
                .expiresAt(LocalDateTime.now().plusMinutes(durationMinutes))
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
        redisTemplate.opsForValue().set(redisKey, "1", durationMinutes, TimeUnit.MINUTES);

        log.info("Room created: {} by host: {}", room.getRoomCode(), hostEmail);
        return roomMapper.toResponse(room);
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
        Room room = roomRepository.findByRoomCodeAndStatus(code, RoomStatus.ACTIVE)
                .orElseThrow(() -> new RuntimeException("Room not found"));
        return roomMapper.toResponse(room);
    }

    @Override
    public Room findByRoomCodeAndStatus(String code, RoomStatus roomStatus) {
        return roomRepository.findByRoomCodeAndStatus(code, roomStatus)
                .orElseThrow(() -> new RuntimeException("Room not found"));
    }
}