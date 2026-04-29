package com.example.smartmeetbe.controller;

import com.example.smartmeetbe.dto.request.ChangePasswordRequest;
import com.example.smartmeetbe.dto.request.UpdateProfileRequest;
import com.example.smartmeetbe.dto.response.ApiResponse;
import com.example.smartmeetbe.dto.response.UserResponse;
import com.example.smartmeetbe.service.UserService;
import com.example.smartmeetbe.utils.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getProfile() {
        String email = SecurityUtil.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.<UserResponse>builder()
                .success(true)
                .message("Profile fetched")
                .data(userService.getProfile(email))
                .build());
    }

    @PatchMapping("/me/profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @RequestBody UpdateProfileRequest request) {
        String email = SecurityUtil.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.<UserResponse>builder()
                .success(true)
                .message("Profile updated")
                .data(userService.updateProfile(email, request))
                .build());
    }

    @PatchMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        String email = SecurityUtil.getCurrentUser();
        userService.changePassword(email, request);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Password changed successfully")
                .build());
    }
}