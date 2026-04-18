package com.example.smartmeetbe.controller;

import com.example.smartmeetbe.dto.request.RoomRequest;
import com.example.smartmeetbe.dto.response.RoomResponse;
import com.example.smartmeetbe.service.RoomService;
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
    RoomService roomService;
    
    @PostMapping("/")
    public ResponseEntity<RoomResponse> createRoom(@RequestBody RoomRequest roomRequest){
        return ResponseEntity.ok(roomService.createRoom(roomRequest));
    }
    
    @GetMapping("/{code}/available")
    public ResponseEntity<RoomResponse> getRoom(@PathVariable String code){
        return ResponseEntity.ok(roomService.getRoomByCode(code));
    }
}
