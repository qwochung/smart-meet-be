package com.example.smartmeetbe.service;

import com.example.smartmeetbe.constant.RoomStatus;
import com.example.smartmeetbe.dto.request.RoomRequest;
import com.example.smartmeetbe.dto.response.RoomResponse;
import com.example.smartmeetbe.dto.response.RoomMinuteResponse;
import com.example.smartmeetbe.entity.Room;

import java.util.List;

public interface RoomService {
    RoomResponse createRoom(RoomRequest request, String hostEmail);
    RoomResponse getRoomByCode(String code);
    Room findByRoomCodeAndStatus(String code, RoomStatus roomStatus);
    List<RoomMinuteResponse> getRoomMinutesForUser(String userEmail, String name, String date);
}