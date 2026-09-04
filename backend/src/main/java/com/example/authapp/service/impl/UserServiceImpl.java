package com.example.authapp.service.impl;

import com.example.authapp.dto.ChangePasswordRequest;
import com.example.authapp.dto.UserResponse;
import com.example.authapp.entity.User;
import com.example.authapp.exception.ApiException;
import com.example.authapp.repository.UserRepository;
import com.example.authapp.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserResponse getById(UUID userId) {
        return UserResponse.from(findUserOrThrow(userId));
    }

    @Override
    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Mật khẩu mới nhập lại không khớp");
        }

        User user = findUserOrThrow(userId);

        if (user.getPasswordHash() == null
                || !passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Mật khẩu hiện tại không đúng");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    private User findUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"));
    }
}
