package com.example.smartmeetbe.service;

import com.example.smartmeetbe.dto.request.*;
import com.example.smartmeetbe.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    void logout(String refreshToken);
    AuthResponse refreshToken(RefreshTokenRequest request);
    void verifyEmail(String token);
    void forgotPassword(ForgotPasswordRequest request);
    void resetPassword(ResetPasswordRequest request);
}
