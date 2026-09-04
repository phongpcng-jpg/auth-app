package com.example.authapp.security.webauthn;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.yubico.webauthn.AssertionRequest;
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Holds the server-generated challenge/options between "start" and "finish"
 * of a WebAuthn ceremony. The REST API here is otherwise fully stateless
 * (JWT in an httpOnly cookie, no HttpSession) so this small in-memory cache
 * is the one piece of short-lived server-side state, keyed by a random
 * requestId handed to the client and echoed back on the "finish" call.
 *
 * Single backend instance is assumed (fine for local dev and a single Render
 * web service). If this app is ever scaled to multiple instances, replace
 * this with a shared store (e.g. Redis) keyed the same way.
 */
@Component
public class PasskeyChallengeStore {

    private static final Duration TTL = Duration.ofMinutes(5);

    private final Cache<String, RegistrationChallenge> registrationChallenges =
            Caffeine.newBuilder().expireAfterWrite(TTL).maximumSize(10_000).build();

    private final Cache<String, AssertionRequest> loginChallenges =
            Caffeine.newBuilder().expireAfterWrite(TTL).maximumSize(10_000).build();

    public String putRegistration(UUID userId, PublicKeyCredentialCreationOptions options) {
        String requestId = UUID.randomUUID().toString();
        registrationChallenges.put(requestId, new RegistrationChallenge(userId, options));
        return requestId;
    }

    /** Removed on read (single-use) whether or not the caller goes on to use it, to prevent replay. */
    public Optional<RegistrationChallenge> takeRegistration(String requestId) {
        RegistrationChallenge value = registrationChallenges.asMap().remove(requestId);
        return Optional.ofNullable(value);
    }

    public String putLogin(AssertionRequest request) {
        String requestId = UUID.randomUUID().toString();
        loginChallenges.put(requestId, request);
        return requestId;
    }

    public Optional<AssertionRequest> takeLogin(String requestId) {
        AssertionRequest value = loginChallenges.asMap().remove(requestId);
        return Optional.ofNullable(value);
    }

    public record RegistrationChallenge(UUID userId, PublicKeyCredentialCreationOptions options) {
    }
}
