package com.example.smartmeetbe.controller;

import com.example.smartmeetbe.dto.request.RoomRequest;
import com.example.smartmeetbe.dto.response.RoomResponse;
import com.example.smartmeetbe.service.JoinRoomService;
import com.example.smartmeetbe.service.impl.JoinRoomServiceImpl;
import com.example.smartmeetbe.service.impl.RoomServiceImpl;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/rooms")
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
public class RoomController {
    RoomServiceImpl roomServiceImpl;
    JoinRoomService joinRoomService;
    
    @PostMapping("/")
    public ResponseEntity<RoomResponse> createRoom(@RequestBody RoomRequest roomRequest){
        return ResponseEntity.ok(roomServiceImpl.createRoom(roomRequest));
    }
    
    @GetMapping("/{code}/available")
    public ResponseEntity<RoomResponse> getRoom(@PathVariable String code){
        return ResponseEntity.ok(roomServiceImpl.getRoomByCode(code));
    }

    @PostMapping("/{code}/user/accept")
    public void acceptRoom(@PathVariable String code, @RequestBody Long userId ){
        joinRoomService.acceptJoinRoom(code, userId);
    }
}
