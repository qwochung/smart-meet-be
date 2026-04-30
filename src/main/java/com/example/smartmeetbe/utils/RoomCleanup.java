package com.example.smartmeetbe.utils;

import com.example.smartmeetbe.constant.RoomStatus;
import com.example.smartmeetbe.entity.Room;
import com.example.smartmeetbe.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoomCleanup {

    private final RoomRepository roomRepository;
    private final StringRedisTemplate redisTemplate;

    /**
     * Chạy mỗi 15 phút, tìm các phòng ACTIVE đã quá expiresAt → chuyển sang ENDED.
     */
    @Scheduled(fixedDelay = 15 * 60 * 1000)
    @Transactional
    public void cleanupExpiredRooms() {
        List<Room> expiredRooms = roomRepository
                .findByStatusAndExpiresAtBefore(RoomStatus.ACTIVE, LocalDateTime.now());

        if (expiredRooms.isEmpty()) return;

        expiredRooms.forEach(room -> {
            room.setStatus(RoomStatus.ENDED);
            redisTemplate.delete("room:participants:" + room.getRoomCode());
            log.info("Room expired and cleaned up: {}", room.getRoomCode());
        });

        roomRepository.saveAll(expiredRooms);
        log.info("Cleaned up {} expired rooms", expiredRooms.size());
    }
}