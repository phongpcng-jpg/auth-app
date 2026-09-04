package com.example.authapp.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Body of POST /api/passkey/register/verify.
 * `credential` is the JSON produced by our PasskeyService.encodeRegistrationResponse()
 * on the frontend (the standard WebAuthn PublicKeyCredential JSON serialization),
 * re-serialized here to a String and handed to
 * PublicKeyCredential.parseRegistrationResponseJson(String).
 */
@Getter
@Setter
public class PasskeyRegistrationVerifyRequest {

    @NotBlank(message = "Thiếu requestId")
    private String requestId;

    @NotNull(message = "Thiếu dữ liệu credential")
    private JsonNode credential;

    /** Optional friendly label (e.g. "Laptop cá nhân"); shown nowhere yet but stored for later. */
    private String deviceName;
}
