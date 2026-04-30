package com.example.smartmeetbe.repository;

import com.example.smartmeetbe.constant.RoomStatus;
import com.example.smartmeetbe.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RoomRepository extends JpaRepository<Room,Long> {
    Optional<Room> findByRoomCode(String roomCode);

    boolean existsByRoomCode(String roomCode);

    Optional<Room> findByRoomCodeAndStatus(String roomCode, RoomStatus status);

    // kiểm tra giới hạn 5 phòng
    long countByStatus(RoomStatus status);

    // tìm phòng hết hạn
    List<Room> findByStatusAndExpiresAtBefore(RoomStatus status, LocalDateTime dateTime);
}
