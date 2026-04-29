package com.example.smartmeetbe.service;

import com.example.smartmeetbe.dto.request.ChangePasswordRequest;
import com.example.smartmeetbe.dto.request.UpdateProfileRequest;
import com.example.smartmeetbe.dto.request.UserRequest;
import com.example.smartmeetbe.dto.response.UserResponse;
import com.example.smartmeetbe.entity.User;

public interface UserService {
    UserResponse getProfile(String email);
    UserResponse updateProfile(String email, UpdateProfileRequest request);
    void changePassword(String email, ChangePasswordRequest request);
    UserResponse createUser(UserRequest user);
    User findById(Long id);
    User findByEmail(String email);
}