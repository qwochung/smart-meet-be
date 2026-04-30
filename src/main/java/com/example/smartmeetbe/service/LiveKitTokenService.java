package com.example.smartmeetbe.service;

import com.example.smartmeetbe.constant.Role;

public interface LiveKitTokenService {
    String generateToken(String identity, String roomName, Role role);
}