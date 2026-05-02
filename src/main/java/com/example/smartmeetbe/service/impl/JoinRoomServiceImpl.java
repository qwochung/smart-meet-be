package com.example.smartmeetbe.service.impl;

import com.example.smartmeetbe.config.LiveKitConfig;
import com.example.smartmeetbe.constant.*;
import com.example.smartmeetbe.dto.request.AcceptRejectRequest;
import com.example.smartmeetbe.dto.response.JoinRoomResponse;
import com.example.smartmeetbe.entity.JoinRoom;
import com.example.smartmeetbe.entity.Room;
import com.example.smartmeetbe.entity.User;
import com.example.smartmeetbe.exception.AppException;
import com.example.smartmeetbe.repository.JoinRoomRepository;
import com.example.smartmeetbe.repository.RoomRepository;
import com.example.smartmeetbe.service.JoinRoomService;
import com.example.smartmeetbe.service.LiveKitTokenService;
import com.example.smartmeetbe.service.RoomService;
import com.example.smartmeetbe.service.UserService;
import com.example.smartmeetbe.dto.request.JoinRoomRequest;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
@Slf4j
public class JoinRoomServiceImpl implements JoinRoomService {
    final JoinRoomRepository joinRoomRepository;
    final RoomService roomService;
    final UserService userService;
    final SimpMessagingTemplate messagingTemplate;
    private final RoomRepository roomRepository;
    private final LiveKitTokenService liveKitTokenService;
    private final LiveKitConfig liveKitConfig;
    // Redis key prefix để track số participant đang trong phòng
    private static final String ROOM_PARTICIPANT_KEY = "room:participants:";
    // Redis key để lock khi join (tránh race condition)
    private static final String ROOM_JOIN_LOCK_KEY = "room:join:lock:";
    private final StringRedisTemplate redisTemplate;

    @Value("${app.room.max-participants}")
    private int maxParticipants;

    @Value("${app.room.max-active-rooms}")
    private int maxActiveRooms;

    @Value("${app.room.duration-minutes}")
    private int durationMinutes;

    @Transactional
    @Override
    public JoinRoomResponse joinRoom(JoinRoomRequest request, String userEmail) {
        Room room = roomRepository.findByRoomCodeAndStatus(request.getRoomCode(), RoomStatus.ACTIVE)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));

        if (room.getExpiresAt() != null && LocalDateTime.now().isAfter(room.getExpiresAt())) {
            expireRoom(room);
            throw new AppException(ErrorCode.ROOM_EXPIRED);
        }

        User user = userService.findByEmail(userEmail);
        boolean isHost = room.getHostUser().getId().equals(user.getId());

        if (isHost) {
            // Host vào thẳng, không cần duyệt
            return buildTokenResponse(room, user, Role.HOST);
        }

        // Participant → lưu PENDING, notify host
        enforceMaxParticipants(room.getRoomCode());

        JoinRoom joinRoom = joinRoomRepository
                .findByRoomIdAndUserId(room.getId(), user.getId())
                .orElse(JoinRoom.builder()
                        .room(room)
                        .user(user)
                        .role(Role.PARTICIPANT)
                        .build());

        // Reset về PENDING nếu đang là trạng thái cũ
        joinRoom.setStatus(JoinRoomStatus.PENDING);
        joinRoomRepository.save(joinRoom);

        // Notify host có người xin vào
        notifyHost(room.getRoomCode(), "JOIN_REQUEST", Map.of(
                "userId",   user.getId(),
                "userName", user.getName(),
                "avatar",   user.getAvatar() != null ? user.getAvatar() : "",
                "email",    user.getEmail()
        ));

        log.info("Join request PENDING: user={} room={}", userEmail, room.getRoomCode());

        // Trả pending = true, FE hiển thị màn hình "Đang chờ host duyệt"
        return JoinRoomResponse.builder()
                .pending(true)
                .roomCode(room.getRoomCode())
                .roomName(room.getName())
                .build();
    }

    @Override
    public void acceptJoin(String roomCode, AcceptRejectRequest request, String hostEmail) {
        Room room = getActiveRoom(roomCode);
        assertIsHost(room, hostEmail);

        User participant = userService.findById(request.getUserId());

        JoinRoom joinRoom = joinRoomRepository
                .findByRoomIdAndUserId(room.getId(), participant.getId())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST));

        if (joinRoom.getStatus() != JoinRoomStatus.PENDING) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        joinRoom.setStatus(JoinRoomStatus.APPROVED);
        joinRoomRepository.save(joinRoom);

        // Generate LiveKit token cho participant
        String livekitToken = liveKitTokenService.generateToken(
                participant.getEmail(),
                roomCode,
                Role.PARTICIPANT
        );

        // Gửi token tới participant qua WebSocket
        // FE participant đang subscribe kênh này và chờ
        notifyParticipant(roomCode, participant.getId(), "JOIN_APPROVED", Map.of(
                "livekitToken", livekitToken,
                "livekitHost", liveKitConfig.getHost(),
                "roomCode", roomCode,
                "roomName", room.getName()
        ));

        log.info("Host {} accepted user {} into room {}", hostEmail, participant.getEmail(), roomCode);
    }

    @Override
    public void rejectJoin(String roomCode, AcceptRejectRequest request, String hostEmail) {
        Room room = getActiveRoom(roomCode);
        assertIsHost(room, hostEmail);

        User participant = userService.findById(request.getUserId());

        JoinRoom joinRoom = joinRoomRepository
                .findByRoomIdAndUserId(room.getId(), participant.getId())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST));

        if (joinRoom.getStatus() != JoinRoomStatus.PENDING) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        joinRoom.setStatus(JoinRoomStatus.REJECTED);
        joinRoomRepository.save(joinRoom);

        // Rollback Redis counter vì participant không được vào
        redisTemplate.opsForValue().decrement(ROOM_PARTICIPANT_KEY + roomCode);

        // Notify participant bị từ chối
        notifyParticipant(roomCode, participant.getId(), "JOIN_REJECTED", Map.of(
                "reason", "Host has rejected your request"
        ));

        log.info("Host {} rejected user {} from room {}", hostEmail, participant.getEmail(), roomCode);
    }

    private void enforceMaxParticipants(String roomCode) {
        String countKey = ROOM_PARTICIPANT_KEY + roomCode;
        String lockKey = ROOM_JOIN_LOCK_KEY + roomCode;

        // Set lock 5 giây
        Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", Duration.ofSeconds(5));

        if (Boolean.FALSE.equals(locked)) {
            throw new AppException(ErrorCode.ROOM_JOIN_CONFLICT);
        }

        try {
            Long current = redisTemplate.opsForValue().increment(countKey);
            if (current == null || current > maxParticipants) {
                // Rollback counter
                redisTemplate.opsForValue().decrement(countKey);
                throw new AppException(ErrorCode.ROOM_FULL);
            }
        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    private void expireRoom(Room room) {
        room.setStatus(RoomStatus.ENDED);
        roomRepository.save(room);
        redisTemplate.delete(ROOM_PARTICIPANT_KEY + room.getRoomCode());
    }

    private Room getActiveRoom(String roomCode) {
        return roomRepository.findByRoomCodeAndStatus(roomCode, RoomStatus.ACTIVE)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
    }

    private void assertIsHost(Room room, String email) {
        if (!room.getHostUser().getEmail().equals(email)) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
    }

    private void notifyParticipant(String roomCode, Long userId, String type, Map<String, Object> data) {
        messagingTemplate.convertAndSend(
                "/topic/room/" + roomCode + "/participant/" + userId,
                Map.of("type", type, "data", data)
        );
    }

    private void notifyHost(String roomCode, String type, Map<String, Object> data) {
        messagingTemplate.convertAndSend(
                "/topic/room/" + roomCode + "/host-events",
                Map.of("type", type, "data", data)
        );
    }

    private JoinRoomResponse buildTokenResponse(Room room, User user, Role role) {
        String token = liveKitTokenService.generateToken(user.getEmail(), room.getRoomCode(), role);
        return JoinRoomResponse.builder()
                .pending(false)
                .livekitToken(token)
                .livekitHost(liveKitConfig.getHost())
                .roomCode(room.getRoomCode())
                .roomName(room.getName())
                .role(Role.valueOf(role.name()))
                .build();
    }

}
