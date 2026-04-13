package com.example.smartmeetbe.dto.mapper;

import com.example.smartmeetbe.dto.request.UserRequest;
import com.example.smartmeetbe.dto.response.UserResponse;
import com.example.smartmeetbe.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toResponse(User user);
    User toUser(UserRequest request);
}
