package com.example.smartmeetbe.repository;

import com.example.smartmeetbe.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room,Long> {
    Optional<Room> findByRoomCode(String roomCode);

    boolean existsByRoomCode(String roomCode);
}
