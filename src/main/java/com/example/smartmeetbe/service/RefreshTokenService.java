package com.example.smartmeetbe.service;

import com.example.smartmeetbe.entity.RefreshToken;
import com.example.smartmeetbe.entity.User;

public interface RefreshTokenService {
    RefreshToken createRefreshToken(User user);
    RefreshToken verifyAndGet(String token);
    void revokeAllUserTokens(User user);
}
