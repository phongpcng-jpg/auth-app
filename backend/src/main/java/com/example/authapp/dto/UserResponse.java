package com.example.authapp.dto;

import com.example.authapp.entity.Role;
import com.example.authapp.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class UserResponse {
    private UUID id;
    private String email;
    private String fullName;
    private String phoneNumber;
    private Role role;
    private boolean passkeyEnabled;
    private OffsetDateTime createdAt;

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .passkeyEnabled(user.isPasskeyEnabled())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
