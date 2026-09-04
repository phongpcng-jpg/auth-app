package com.example.authapp.controller;

import com.example.authapp.dto.GoogleEmailVerificationResponse;
import com.example.authapp.dto.GoogleTokenRequest;
import com.example.authapp.dto.LoginRequest;
import com.example.authapp.dto.PasskeyLoginVerifyRequest;
import com.example.authapp.dto.PasskeyOptionsResponse;
import com.example.authapp.dto.RegisterRequest;
import com.example.authapp.dto.UserResponse;
import com.example.authapp.security.JwtService;
import com.example.authapp.service.AuthService;
import com.example.authapp.service.PasskeyService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasskeyService passkeyService;
    private final JwtService jwtService;

    @Value("${app.jwt.cookie-name}")
    private String cookieName;

    @Value("${app.cookie.secure}")
    private boolean cookieSecure;

    /**
     * Step 3: registration now also logs the user in (sets the JWT cookie),
     * same as /login. This is required so the frontend can immediately call
     * authenticated passkey-setup endpoints right after account creation
     * ("thiết lập passkey ngay sau khi đăng ký" — confirmed with user).
     */
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request, HttpServletResponse response) {
        UserResponse user = authService.register(request);
        String token = jwtService.generateToken(user.getId(), user.getEmail());
        response.addHeader("Set-Cookie", buildCookie(token, jwtService.getExpirationSeconds()).toString());
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        AuthService.LoginResult result = authService.login(request);
        response.addHeader("Set-Cookie", buildCookie(result.token(), jwtService.getExpirationSeconds()).toString());
        return ResponseEntity.ok(result.user());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        response.addHeader("Set-Cookie", buildCookie("", 0).toString());
        return ResponseEntity.noContent().build();
    }

    /**
     * Verifies a Google ID token and returns the email it belongs to.
     * Used by the registration wizard's email step. Does not create a
     * session and does not require the account to exist yet.
     */
    @PostMapping("/oauth2/verify-email")
    public ResponseEntity<GoogleEmailVerificationResponse> verifyGoogleEmail(@Valid @RequestBody GoogleTokenRequest request) {
        return ResponseEntity.ok(authService.verifyGoogleEmail(request));
    }

    /** Logs in with a Google ID token (existing account required — see AuthServiceImpl). */
    @PostMapping("/oauth2/google")
    public ResponseEntity<UserResponse> loginWithGoogle(@Valid @RequestBody GoogleTokenRequest request, HttpServletResponse response) {
        AuthService.LoginResult result = authService.loginWithGoogle(request);
        response.addHeader("Set-Cookie", buildCookie(result.token(), jwtService.getExpirationSeconds()).toString());
        return ResponseEntity.ok(result.user());
    }

    // --- Step 3: Passkey login (public — the user isn't authenticated yet) ---

    /** Usernameless: the browser's passkey picker identifies the user, so no request body is needed. */
    @PostMapping("/passkey/login/options")
    public ResponseEntity<PasskeyOptionsResponse> startPasskeyLogin() {
        return ResponseEntity.ok(passkeyService.startLogin());
    }

    @PostMapping("/passkey/login/verify")
    public ResponseEntity<UserResponse> finishPasskeyLogin(
            @Valid @RequestBody PasskeyLoginVerifyRequest request,
            HttpServletResponse response
    ) {
        AuthService.LoginResult result = passkeyService.finishLogin(request);
        response.addHeader("Set-Cookie", buildCookie(result.token(), jwtService.getExpirationSeconds()).toString());
        return ResponseEntity.ok(result.user());
    }

    private ResponseCookie buildCookie(String token, long maxAgeSeconds) {
        return ResponseCookie.from(cookieName, token)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(maxAgeSeconds)
                .sameSite(cookieSecure ? "None" : "Lax")
                .build();
    }
}
