package com.example.smartmeetbe.dto.mapper;

import com.example.smartmeetbe.dto.request.UserRequest;
import com.example.smartmeetbe.dto.response.UserResponse;
import com.example.smartmeetbe.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setAvatar(user.getAvatar());
        return response;
    }

    public User toUser(UserRequest request) {
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        user.setAvatar(request.getAvatar());
        return user;
    }
}
