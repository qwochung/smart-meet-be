package com.example.smartmeetbe.service;

import com.example.smartmeetbe.constant.RoomStatus;
import com.example.smartmeetbe.dto.mapper.RoomMapper;
import com.example.smartmeetbe.dto.request.RoomRequest;
import com.example.smartmeetbe.dto.response.RoomResponse;
import com.example.smartmeetbe.entity.Room;
import com.example.smartmeetbe.entity.User;
import com.example.smartmeetbe.repository.RoomRepository;
import com.example.smartmeetbe.utils.RoomCodeGenerator;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
public class RoomService {
    RoomRepository roomRepository;
    RoomMapper roomMapper;
    UserService userService;
    
    @Transactional
    public RoomResponse createRoom(RoomRequest request){
        User host = userService.findById(request.getHostId());
        Room room = roomMapper.toRoom(request);
        room.setHostId(host.getId());
        room.setHostUser(host);
        room.setStatus(request.getScheduledAt() == null ?
                RoomStatus.ACTIVE : RoomStatus.WAITING);
        
        String roomCode = generateUniqueRoomCode();
        room.setRoomCode(roomCode);
        
        return roomMapper.toResponse(roomRepository.save(room));
    }

    public String generateUniqueRoomCode() {
        String code;
        int retry = 0;

        do {
            code = RoomCodeGenerator.generate();
            retry++;
            if (retry > 5) {
                throw new RuntimeException("Cannot generate unique room code");
            }
        } while (roomRepository.existsByRoomCode((code)));

        return code;
    }

    public RoomResponse getRoomByCode(String code) {
        Room room = roomRepository.findByRoomCode(code)
                .orElseThrow(() -> new RuntimeException("Room not found"));
        return roomMapper.toResponse(room);
    }
}
