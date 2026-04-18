package com.example.smartmeetbe.service.impl;

import com.example.smartmeetbe.constant.ErrorCode;
import com.example.smartmeetbe.entity.RefreshToken;
import com.example.smartmeetbe.entity.User;
import com.example.smartmeetbe.exception.AppException;
import com.example.smartmeetbe.repository.RefreshTokenRepository;
import com.example.smartmeetbe.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    @Override
    @Transactional
    public RefreshToken createRefreshToken(User user) {
        RefreshToken token = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiryDate(Instant.now().plusMillis(refreshExpirationMs))
                .revoked(false)
                .build();
        return refreshTokenRepository.save(token);
    }

    @Override
    public RefreshToken verifyAndGet(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new AppException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));

        if (refreshToken.isRevoked()) {
            throw new AppException(ErrorCode.REFRESH_TOKEN_REVOKED);
        }

        if (refreshToken.getExpiryDate().isBefore(Instant.now())) {
            throw new AppException(ErrorCode.REFRESH_TOKEN_EXPIRED);
        }

        return refreshToken;
    }

    @Override
    @Transactional
    public void revokeAllUserTokens(User user) {
        refreshTokenRepository.revokeAllByUser(user);
    }
}
