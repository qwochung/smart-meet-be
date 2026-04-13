package com.example.smartmeetbe.dto.mapper;

import com.example.smartmeetbe.dto.request.RoomRequest;
import com.example.smartmeetbe.dto.response.RoomResponse;
import com.example.smartmeetbe.entity.Room;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RoomMapper {
    RoomResponse toResponse(Room room);
    Room toRoom(RoomRequest request);
}
