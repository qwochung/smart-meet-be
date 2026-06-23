package com.example.smartmeetbe.controller;

import com.example.smartmeetbe.dto.request.AcceptRejectRequest;
import com.example.smartmeetbe.dto.request.JoinRoomRequest;
import com.example.smartmeetbe.dto.request.RoomRequest;
import com.example.smartmeetbe.dto.request.ScheduleMeetingRequest;
import com.example.smartmeetbe.dto.response.ApiResponse;
import com.example.smartmeetbe.dto.response.JoinRoomResponse;
import com.example.smartmeetbe.dto.response.RoomResponse;
import com.example.smartmeetbe.dto.response.DashboardResponse;
import com.example.smartmeetbe.service.JoinRoomService;
import com.example.smartmeetbe.service.RoomService;
import com.example.smartmeetbe.utils.SecurityUtil;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import com.example.smartmeetbe.dto.response.RoomMinuteResponse;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rooms")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class RoomController {

    RoomService roomService;
    JoinRoomService joinRoomService;

    @PostMapping("/")
    public ResponseEntity<ApiResponse<RoomResponse>> createRoom(@Valid @RequestBody RoomRequest roomRequest) {
        String hostEmail = SecurityUtil.getCurrentUser();
        RoomResponse room = roomService.createRoom(roomRequest, hostEmail);
        return ResponseEntity.ok(ApiResponse.<RoomResponse>builder()
                .success(true)
                .message("Room created successfully")
                .data(room)
                .build());
    }

    @GetMapping("/{code}/available")
    public ResponseEntity<ApiResponse<RoomResponse>> getRoom(@PathVariable String code) {
        RoomResponse room = roomService.getRoomByCode(code);
        return ResponseEntity.ok(ApiResponse.<RoomResponse>builder()
                .success(true)
                .message("Room retrieved successfully")
                .data(room)
                .build());
    }

    @PostMapping("/{code}/join")
    public ResponseEntity<ApiResponse<JoinRoomResponse>> joinRoom(
            @PathVariable String code,
            @RequestBody(required = false) JoinRoomRequest body) {
        String userEmail = SecurityUtil.getCurrentUser();
        JoinRoomRequest request = body != null ? body : new JoinRoomRequest();
        request.setRoomCode(code);
        JoinRoomResponse response = joinRoomService.joinRoom(request, userEmail);
        return ResponseEntity.ok(ApiResponse.<JoinRoomResponse>builder()
                .success(true)
                .message("Join request processed successfully")
                .data(response)
                .build());
    }

    @PostMapping("/{code}/join/accept")
    public ResponseEntity<ApiResponse<Void>> acceptJoin(
            @PathVariable String code,
            @Valid @RequestBody AcceptRejectRequest request) {
        String hostEmail = SecurityUtil.getCurrentUser();
        joinRoomService.acceptJoin(code, request, hostEmail);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("User accepted successfully")
                .build());
    }

    @PostMapping("/{code}/join/reject")
    public ResponseEntity<ApiResponse<Void>> rejectJoin(
            @PathVariable String code,
            @Valid @RequestBody AcceptRejectRequest request) {
        String hostEmail = SecurityUtil.getCurrentUser();
        joinRoomService.rejectJoin(code, request, hostEmail);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("User rejected successfully")
                .build());
    }

    @PostMapping("/schedule")
    public ResponseEntity<ApiResponse<List<RoomResponse>>> scheduleRecurringMeetings(
            @Valid @RequestBody ScheduleMeetingRequest request) {
        String hostEmail = SecurityUtil.getCurrentUser();
        List<RoomResponse> rooms = roomService.scheduleRecurringMeetings(request, hostEmail);
        return ResponseEntity.ok(ApiResponse.<List<RoomResponse>>builder()
                .success(true)
                .message("Scheduled " + rooms.size() + " meeting(s) successfully")
                .data(rooms)
                .build());
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboard() {
        String userEmail = SecurityUtil.getCurrentUser();
        DashboardResponse dashboard = roomService.getDashboard(userEmail);
        return ResponseEntity.ok(ApiResponse.<DashboardResponse>builder()
                .success(true)
                .message("Dashboard retrieved successfully")
                .data(dashboard)
                .build());
    }

    @GetMapping("/minutes")
    public ResponseEntity<ApiResponse<List<RoomMinuteResponse>>> getRoomMinutes(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String date) {
        String userEmail = SecurityUtil.getCurrentUser();
        List<RoomMinuteResponse> minutes = roomService.getRoomMinutesForUser(userEmail, name, date);
        return ResponseEntity.ok(ApiResponse.<List<RoomMinuteResponse>>builder()
                .success(true)
                .message("Room minutes retrieved successfully")
                .data(minutes)
                .build());
    }
}