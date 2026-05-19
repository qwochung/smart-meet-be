package com.example.smartmeetbe.repository;

import com.example.smartmeetbe.constant.Role;
import com.example.smartmeetbe.entity.JoinRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JoinRoomRepository extends JpaRepository<JoinRoom, Long> {
     Optional<JoinRoom> findByRoomIdAndUserId(Long roomId, Long userId);
     
     Optional<JoinRoom> findByRoomIdAndUser_Email(Long roomId, String userEmail);
     
     Optional<JoinRoom> findByRoomIdAndUser_EmailAndRole(Long roomId, String userEmail, Role role);

}
