package com.example.smartmeetbe.service.impl;

import com.example.smartmeetbe.constant.ErrorCode;
import com.example.smartmeetbe.constant.Role;
import com.example.smartmeetbe.dto.request.*;
import com.example.smartmeetbe.dto.response.AuthResponse;
import com.example.smartmeetbe.dto.response.UserResponse;
import com.example.smartmeetbe.entity.RefreshToken;
import com.example.smartmeetbe.entity.User;
import com.example.smartmeetbe.exception.AppException;
import com.example.smartmeetbe.repository.UserRepository;
import com.example.smartmeetbe.security.UserDetailsServiceImpl;
import com.example.smartmeetbe.service.AuthService;
import com.example.smartmeetbe.service.EmailService;
import com.example.smartmeetbe.service.RefreshTokenService;
import com.example.smartmeetbe.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsServiceImpl userDetailsService;
    private final RefreshTokenService refreshTokenService;
    private final EmailService emailService;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        String verificationToken = UUID.randomUUID().toString();

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .enabled(false)
                .verificationToken(verificationToken)
                .verificationTokenExpiry(LocalDateTime.now().plusHours(24))
                .build();

        userRepository.save(user);
        emailService.sendVerificationEmail(user.getEmail(), verificationToken);

        log.info("New user registered: {}", user.getEmail());
        return buildAuthResponse(user);
    }


    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (DisabledException e) {
            throw new AppException(ErrorCode.ACCOUNT_NOT_VERIFIED);
        } catch (LockedException e) {
            throw new AppException(ErrorCode.ACCOUNT_LOCKED);
        } catch (BadCredentialsException e) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Revoke existing refresh tokens (rotation)
        refreshTokenService.revokeAllUserTokens(user);

        log.info("User logged in: {}", user.getEmail());
        return buildAuthResponse(user);
    }


    @Override
    @Transactional
    public void logout(String refreshToken) {
        RefreshToken token = refreshTokenService.verifyAndGet(refreshToken);
        refreshTokenService.revokeAllUserTokens(token.getUser());
        log.info("User logged out, all refresh tokens revoked for: {}", token.getUser().getEmail());
    }


    @Override
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenService.verifyAndGet(request.getRefreshToken());
        User user = refreshToken.getUser();

        // Rotate: revoke old, issue new
        refreshTokenService.revokeAllUserTokens(user);

        log.info("Token refreshed for user: {}", user.getEmail());
        return buildAuthResponse(user);
    }


    @Override
    @Transactional
    public void verifyEmail(String token) {
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_TOKEN));

        if (user.getVerificationTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new AppException(ErrorCode.TOKEN_EXPIRED);
        }

        user.setEnabled(true);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiry(null);
        userRepository.save(user);

        log.info("Email verified for user: {}", user.getEmail());
    }


    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        // Always return OK to prevent email enumeration
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            String resetToken = UUID.randomUUID().toString();
            user.setResetPasswordToken(resetToken);
            user.setResetPasswordTokenExpiry(LocalDateTime.now().plusMinutes(30));
            userRepository.save(user);
            emailService.sendPasswordResetEmail(user.getEmail(), resetToken);
        });
    }


    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByResetPasswordToken(request.getToken())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_TOKEN));

        if (user.getResetPasswordTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new AppException(ErrorCode.TOKEN_EXPIRED);
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setResetPasswordToken(null);
        user.setResetPasswordTokenExpiry(null);
        userRepository.save(user);

        // Revoke all refresh tokens after password reset
        refreshTokenService.revokeAllUserTokens(user);

        log.info("Password reset for user: {}", user.getEmail());
    }


    private AuthResponse buildAuthResponse(User user) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String accessToken = jwtUtil.generateToken(userDetails);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        UserResponse userResponse = UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .avatar(user.getAvatar())
                .role(user.getRole())
                .enabled(user.isEnabled())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getExpirationMs() / 1000)
                .user(userResponse)
                .build();
    }
}
