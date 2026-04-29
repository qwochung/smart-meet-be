package com.example.smartmeetbe.service.impl;

import com.example.smartmeetbe.constant.RoomStatus;
import com.example.smartmeetbe.dto.mapper.RoomMapper;
import com.example.smartmeetbe.dto.request.RoomRequest;
import com.example.smartmeetbe.dto.response.RoomResponse;
import com.example.smartmeetbe.entity.Room;
import com.example.smartmeetbe.entity.User;
import com.example.smartmeetbe.repository.RoomRepository;
import com.example.smartmeetbe.service.RoomService;
import com.example.smartmeetbe.service.UserService;
import com.example.smartmeetbe.utils.RoomCodeGenerator;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
public class RoomServiceImpl implements RoomService {
    RoomRepository roomRepository;
    RoomMapper roomMapper;
    UserService userService;

    @Override
    @Transactional
    public RoomResponse createRoom(RoomRequest request, String hostEmail) {
        User host = userService.findByEmail(hostEmail);
        Room room = roomMapper.toRoom(request);
        room.setHostId(host.getId());
        room.setHostUser(host);
        room.setStatus(request.getScheduledAt() == null ?
                RoomStatus.ACTIVE : RoomStatus.WAITING);

        String roomCode = generateUniqueRoomCode();
        room.setRoomCode(roomCode);

        return roomMapper.toResponse(roomRepository.save(room));
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