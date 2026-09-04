package com.example.authapp.service;

import com.example.authapp.dto.ChangePasswordRequest;
import com.example.authapp.dto.UserResponse;

import java.util.UUID;

public interface UserService {

    UserResponse getById(UUID userId);

    void changePassword(UUID userId, ChangePasswordRequest request);
}
