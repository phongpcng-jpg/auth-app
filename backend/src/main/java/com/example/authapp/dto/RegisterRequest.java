package com.example.authapp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Final submission of the registration wizard (email -> oauth2 verify -> password
 * -> profile). The email-verification sub-step is a real Google OAuth2 check —
 * `googleIdToken` is the credential obtained at that step and is re-verified
 * server-side here (defense in depth: the client-reported `email` must match
 * what Google actually returned).
 *
 * Step 3: passkey setup is no longer part of this payload. The account is
 * created first (passkeyEnabled always starts false), the response logs the
 * user in immediately (see AuthController#register), and — if the user opts
 * in on the next wizard step — a real WebAuthn registration ceremony runs
 * afterwards as its own authenticated request (POST /api/passkey/register/*).
 */
@Getter
@Setter
public class RegisterRequest {

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;

    @NotBlank(message = "Vui lòng xác thực email qua Google trước khi đăng ký")
    private String googleIdToken;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, message = "Mật khẩu phải có ít nhất 6 ký tự")
    private String password;

    @NotBlank(message = "Vui lòng nhập lại mật khẩu")
    private String confirmPassword;

    @NotBlank(message = "Họ và tên không được để trống")
    private String fullName;

    private String phoneNumber;
}
