package com.example.smartmeetbe.utils;

import com.example.smartmeetbe.constant.RoomStatus;
import com.example.smartmeetbe.entity.Room;
import com.example.smartmeetbe.repository.RoomRepository;
import com.example.smartmeetbe.service.MeetingFinalizationService;
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
    private final MeetingFinalizationService meetingFinalizationService;

    /**
     * Chạy mỗi 15 phút, tìm các phòng ACTIVE đã quá expiresAt → chuyển sang ENDED.
     */
    @Scheduled(fixedDelay = 15 * 60 * 1000)
    @Transactional
    public void cleanupExpiredRooms() {
        LocalDateTime now = LocalDateTime.now();
        List<Room> expiredActive = roomRepository
                .findByStatusAndExpiresAtBefore(RoomStatus.ACTIVE, now);
        // Phòng hẹn giờ không ai mở cũng phải kết thúc, nhưng không cần tổng hợp biên bản
        List<Room> expiredWaiting = roomRepository
                .findByStatusAndExpiresAtBefore(RoomStatus.WAITING, now);

        if (expiredActive.isEmpty() && expiredWaiting.isEmpty()) return;

        expiredActive.forEach(room -> {
            room.setStatus(RoomStatus.ENDED);
            if (room.getActualEndedAt() == null) {
                room.setActualEndedAt(room.getExpiresAt());
            }
            redisTemplate.delete("room:participants:" + room.getRoomCode());
            meetingFinalizationService.finalizeAsync(room.getRoomCode());
            log.info("Room expired and cleaned up: {}", room.getRoomCode());
        });

        expiredWaiting.forEach(room -> {
            room.setStatus(RoomStatus.ENDED);
            log.info("Scheduled room never started, marked ended: {}", room.getRoomCode());
        });

        roomRepository.saveAll(expiredActive);
        roomRepository.saveAll(expiredWaiting);
        log.info("Cleaned up {} expired rooms", expiredActive.size() + expiredWaiting.size());
    }
}