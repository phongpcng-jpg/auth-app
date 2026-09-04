package com.example.authapp.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Payload for the two Google-related endpoints:
 * - POST /api/auth/oauth2/verify-email (registration wizard, email step)
 * - POST /api/auth/oauth2/google (login)
 *
 * `idToken` is the credential returned by Google Identity Services on the
 * frontend (google.accounts.id callback), NOT an OAuth2 access token.
 */
@Getter
@Setter
public class GoogleTokenRequest {

    @NotBlank(message = "Thiếu Google ID token")
    private String idToken;
}
