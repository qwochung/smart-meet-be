package com.example.smartmeetbe.service;

import com.example.smartmeetbe.constant.Role;

public interface LiveKitTokenService {
    String generateToken(String identity, String displayName, String roomName, Role role);

    default String generateToken(String identity, String roomName, Role role) {
        return generateToken(identity, identity, roomName, role);
    }
}