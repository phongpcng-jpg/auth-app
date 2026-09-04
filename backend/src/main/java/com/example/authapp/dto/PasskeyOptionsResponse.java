package com.example.authapp.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Response for both /passkey/register/options and /auth/passkey/login/options.
 * `publicKey` is the raw WebAuthn options object (PublicKeyCredentialCreationOptions
 * or PublicKeyCredentialRequestOptions, produced by the Yubico library's
 * toCredentialsCreateJson()/toCredentialsGetJson()) — the frontend base64url-decodes
 * its binary fields and passes it straight into navigator.credentials.create()/get().
 * `requestId` must be echoed back on the matching "verify" call so the server can
 * retrieve the challenge it generated.
 */
@Getter
@AllArgsConstructor
public class PasskeyOptionsResponse {
    private String requestId;
    private JsonNode publicKey;
}
