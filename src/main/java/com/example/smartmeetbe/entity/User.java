package com.example.smartmeetbe.entity;

import com.example.smartmeetbe.constant.Role;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false)
    String name;

    @Column(nullable = false, unique = true)
    String email;

    @Column(nullable = false)
    String password;

    String avatar;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(255) default 'USER'")
    @Builder.Default
    Role role = Role.USER;

    @Builder.Default
    @Column(columnDefinition = "boolean default false")
    boolean enabled = false;

    @Builder.Default
    @Column(columnDefinition = "boolean default false")
    boolean accountLocked = false;

    String verificationToken;
    LocalDateTime verificationTokenExpiry;

    String resetPasswordToken;
    LocalDateTime resetPasswordTokenExpiry;
}
