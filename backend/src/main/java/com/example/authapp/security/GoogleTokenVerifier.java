package com.example.authapp.security;

import com.example.authapp.exception.ApiException;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

/**
 * Verifies Google-issued ID tokens coming from Google Identity Services on the
 * frontend (the "Sign in with Google" JS widget). This is signature + audience
 * + expiry verification only — it requires just the OAuth2 Client ID (as the
 * expected "audience"), no client secret, and makes no network call to Google
 * for each request beyond periodic refresh of Google's public signing keys
 * (handled internally by GoogleIdTokenVerifier).
 */
@Component
public class GoogleTokenVerifier {

    private final String clientId;
    private GoogleIdTokenVerifier verifier;

    public GoogleTokenVerifier(@Value("${app.oauth2.google.client-id}") String clientId) {
        this.clientId = clientId;
    }

    @PostConstruct
    void init() {
        this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(clientId))
                .build();
    }

    /**
     * Verifies the token and returns the Google account's subject (stable
     * user id) and email. Throws ApiException(401) on any invalid/expired/
     * wrong-audience token, or ApiException(500) if GOOGLE_CLIENT_ID isn't
     * configured on the server.
     */
    public GooglePayload verify(String idToken) {
        if (clientId == null || clientId.isBlank()) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Server chưa cấu hình GOOGLE_CLIENT_ID");
        }
        try {
            GoogleIdToken token = verifier.verify(idToken);
            if (token == null) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "Google ID token không hợp lệ hoặc đã hết hạn");
            }
            GoogleIdToken.Payload payload = token.getPayload();
            if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "Email Google chưa được xác minh");
            }
            return new GooglePayload(payload.getSubject(), payload.getEmail());
        } catch (ApiException e) {
            throw e;
        } catch (GeneralSecurityException | IOException | IllegalArgumentException e) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Không thể xác thực Google ID token");
        }
    }

    /** subject = Google's stable per-account id ("sub" claim); email = verified email. */
    public record GooglePayload(String subject, String email) {}
}
