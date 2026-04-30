package com.example.smartmeetbe.service;

import com.example.smartmeetbe.dto.request.AcceptRejectRequest;
import com.example.smartmeetbe.dto.response.JoinRoomResponse;
import com.example.smartmeetbe.dto.request.JoinRoomRequest;

public interface JoinRoomService {
    JoinRoomResponse joinRoom(JoinRoomRequest request, String userEmail);
    void acceptJoin(String roomCode, AcceptRejectRequest request, String hostEmail);
    void rejectJoin(String roomCode, AcceptRejectRequest request, String hostEmail);
}
