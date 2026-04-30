package com.example.smartmeetbe.service.impl;


import com.example.smartmeetbe.config.LiveKitConfig;
import com.example.smartmeetbe.constant.Role;
import com.example.smartmeetbe.service.LiveKitTokenService;
import io.livekit.server.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class LiveKitTokenServiceImpl implements LiveKitTokenService {

    private final LiveKitConfig liveKitConfig;

    @Value("${app.room.duration-minutes:60}")
    private int durationMinutes;

    /**
     * Generate LiveKit JWT token cho user tham gia phòng.
     *
     * @param identity  email — định danh duy nhất trong room
     * @param roomName  roomCode, dùng làm room name trên LiveKit
     * @param role      HOST hoặc PARTICIPANT
     */
    public String generateToken(String identity, String roomName, Role role) {
        AccessToken token = new AccessToken(
                liveKitConfig.getApiKey(),
                liveKitConfig.getApiSecret()
        );

        token.setIdentity(identity);
        token.setName(identity);
        token.setTtl(TimeUnit.MINUTES.toMillis(durationMinutes));
        token.addGrants(buildGrants(roomName, role));

        return token.toJwt();
    }

    private VideoGrant[] buildGrants(String roomName, Role role) {
        if (role == Role.HOST) {
            return new VideoGrant[]{
                    new RoomName(roomName),
                    new RoomJoin(true),
                    new CanPublish(true),
                    new CanSubscribe(true),
                    new CanPublishData(true),
                    new RoomAdmin(true)
            };
        } else {
            return new VideoGrant[]{
                    new RoomName(roomName),
                    new RoomJoin(true),
                    new CanPublish(true),
                    new CanSubscribe(true),
                    new CanPublishData(true)
            };
        }
    }
}