package com.example.authapp.service;

import com.example.authapp.dto.PasskeyLoginVerifyRequest;
import com.example.authapp.dto.PasskeyOptionsResponse;
import com.example.authapp.dto.PasskeyRegistrationVerifyRequest;

import java.util.UUID;

public interface PasskeyService {

    /** Requires the user to not already have a passkey (single-passkey-per-account rule). */
    PasskeyOptionsResponse startRegistration(UUID userId);

    void finishRegistration(UUID userId, PasskeyRegistrationVerifyRequest request);

    /** No-op (does not throw) if the user has no passkey. */
    void deletePasskey(UUID userId);

    /** Usernameless/discoverable: no identifying info needed to start a passkey login. */
    PasskeyOptionsResponse startLogin();

    /** Verifies the assertion and issues a session, exactly like a password/Google login. */
    AuthService.LoginResult finishLogin(PasskeyLoginVerifyRequest request);
}
