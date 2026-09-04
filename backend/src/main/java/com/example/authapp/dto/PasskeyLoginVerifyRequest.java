package com.example.authapp.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** Body of POST /api/auth/passkey/login/verify. */
@Getter
@Setter
public class PasskeyLoginVerifyRequest {

    @NotBlank(message = "Thiếu requestId")
    private String requestId;

    @NotNull(message = "Thiếu dữ liệu credential")
    private JsonNode credential;
}
