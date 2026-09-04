package com.example.authapp.controller;

import com.example.authapp.dto.PasskeyOptionsResponse;
import com.example.authapp.dto.PasskeyRegistrationVerifyRequest;
import com.example.authapp.entity.User;
import com.example.authapp.service.PasskeyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Passkey management for the currently-authenticated user (add / remove).
 * Requires a valid session, unlike the login endpoints under /api/auth/passkey/**
 * (see AuthController) which must work before the user is authenticated.
 */
@RestController
@RequestMapping("/api/passkey")
@RequiredArgsConstructor
public class PasskeyController {

    private final PasskeyService passkeyService;

    @PostMapping("/register/options")
    public ResponseEntity<PasskeyOptionsResponse> startRegistration(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(passkeyService.startRegistration(user.getId()));
    }

    @PostMapping("/register/verify")
    public ResponseEntity<Void> finishRegistration(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody PasskeyRegistrationVerifyRequest request
    ) {
        passkeyService.finishRegistration(user.getId(), request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> deletePasskey(@AuthenticationPrincipal User user) {
        passkeyService.deletePasskey(user.getId());
        return ResponseEntity.noContent().build();
    }
}
