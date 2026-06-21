package com.example.smartmeetbe.repository;

import com.example.smartmeetbe.constant.RoomStatus;
import com.example.smartmeetbe.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    Optional<Room> findByRoomCode(String roomCode);

    boolean existsByRoomCode(String roomCode);

    Optional<Room> findByRoomCodeAndStatus(String roomCode, RoomStatus status);

    // kiểm tra giới hạn 5 phòng
    long countByStatus(RoomStatus status);

    // tìm phòng hết hạn
    List<Room> findByStatusAndExpiresAtBefore(RoomStatus status, LocalDateTime dateTime);

    @Query(value = """
    SELECT DISTINCT r.*
    FROM rooms r
    LEFT JOIN room_participants rp
        ON r.id = rp.room_id
    WHERE (r.host_id = :userId OR rp.user_id = :userId)
      AND (:name IS NULL OR r.name ILIKE CONCAT('%', :name, '%'))
      AND (CAST(:startDate AS timestamp) IS NULL OR r.expires_at >= :startDate)
      AND (CAST(:endDate AS timestamp) IS NULL OR r.expires_at <= :endDate)
    ORDER BY r.expires_at DESC
    """
            , nativeQuery = true
    )
    List<Room> findRoomsForUser(
            @Param("userId") Long userId,
            @Param("name") String name,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}
