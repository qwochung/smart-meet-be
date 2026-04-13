package com.example.smartmeetbe.entity;

import com.example.smartmeetbe.constant.RoomStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;
 
@Entity
@Table(name = "rooms")
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Room extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    
    String name;
    
    String description;

    @Column(name = "room_code", unique = true, nullable = false, length = 20)
    private String roomCode;
    
    @Enumerated(EnumType.STRING)
    RoomStatus status;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id", nullable = false)
    User hostUser;
    
    @Column(name = "host_id", insertable = false, updatable = false)
    Long hostId;
    
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "room_participants",
        joinColumns = @JoinColumn(name = "room_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    List<User> participants;
    LocalDateTime scheduledAt;

}
