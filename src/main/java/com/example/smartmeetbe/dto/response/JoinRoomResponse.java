package com.example.smartmeetbe.dto.response;

import com.example.smartmeetbe.constant.Role;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JoinRoomResponse {
    private String livekitToken;   // frontend dùng token này kết nối LiveKit
    private String livekitHost;
    private String roomCode;
    private String roomName;
    private Role role;
    private boolean pending;
}
