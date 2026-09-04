package com.example.authapp.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Returned by POST /api/auth/oauth2/verify-email once the ID token checks out. */
@Getter
@AllArgsConstructor
public class GoogleEmailVerificationResponse {
    private String email;
}
