package com.example.smartmeetbe.service;

import com.example.smartmeetbe.constant.ErrorCode;
import com.example.smartmeetbe.dto.mapper.UserMapper;
import com.example.smartmeetbe.dto.request.UserRequest;
import com.example.smartmeetbe.dto.response.UserResponse;
import com.example.smartmeetbe.entity.User;
import com.example.smartmeetbe.exception.AppException;
import com.example.smartmeetbe.repository.UserRepository;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
public class UserService {
    UserRepository userRepository;
    UserMapper userMapper;
    
    @Transactional
    public UserResponse createUser(UserRequest user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        User entity = userMapper.toUser(user);
        return userMapper.toResponse(userRepository.save(entity));
    }
    
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }
    
}
