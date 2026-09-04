package com.example.authapp.service;

import com.example.authapp.dto.GoogleEmailVerificationResponse;
import com.example.authapp.dto.GoogleTokenRequest;
import com.example.authapp.dto.LoginRequest;
import com.example.authapp.dto.RegisterRequest;
import com.example.authapp.dto.UserResponse;

public interface AuthService {

    /**
     * Creates the user account at the end of the registration wizard.
     * Re-verifies the Google ID token collected earlier in the wizard and
     * links a Google OAuthAccount to the new user.
     */
    UserResponse register(RegisterRequest request);

    /** Validates credentials and returns the signed JWT to be set as a cookie. */
    LoginResult login(LoginRequest request);

    /**
     * Verifies a Google ID token and returns the email it belongs to.
     * Used by the registration wizard's email step; does not create/modify
     * anything — it's a pure verification check.
     */
    GoogleEmailVerificationResponse verifyGoogleEmail(GoogleTokenRequest request);

    /**
     * Logs in with a Google ID token. Looks up the linked account by Google
     * subject first; if none is linked yet but a password account exists
     * with the same verified email, auto-links Google to that account
     * (per product decision: same email = same person). Fails with 404 if
     * no account exists at all for that email.
     */
    LoginResult loginWithGoogle(GoogleTokenRequest request);

    record LoginResult(String token, UserResponse user) {}
}
