package com.example.smartmeetbe.repository;

import com.example.smartmeetbe.entity.JoinRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JoinRoomRepository extends JpaRepository<JoinRoom, Long> {
     Optional<JoinRoom> findByRoomIdAndUserId(Long roomId, Long userId);

}
