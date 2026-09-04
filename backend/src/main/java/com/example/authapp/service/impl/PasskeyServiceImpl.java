package com.example.authapp.service.impl;

import com.example.authapp.dto.PasskeyLoginVerifyRequest;
import com.example.authapp.dto.PasskeyOptionsResponse;
import com.example.authapp.dto.PasskeyRegistrationVerifyRequest;
import com.example.authapp.dto.UserResponse;
import com.example.authapp.entity.PasskeyCredential;
import com.example.authapp.entity.User;
import com.example.authapp.exception.ApiException;
import com.example.authapp.repository.PasskeyCredentialRepository;
import com.example.authapp.repository.UserRepository;
import com.example.authapp.security.JwtService;
import com.example.authapp.security.webauthn.PasskeyChallengeStore;
import com.example.authapp.security.webauthn.UserHandles;
import com.example.authapp.service.AuthService;
import com.example.authapp.service.PasskeyService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yubico.webauthn.*;
import com.yubico.webauthn.data.*;
import com.yubico.webauthn.exception.AssertionFailedException;
import com.yubico.webauthn.exception.RegistrationFailedException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PasskeyServiceImpl implements PasskeyService {

    private final RelyingParty relyingParty;
    private final PasskeyChallengeStore challengeStore;
    private final UserRepository userRepository;
    private final PasskeyCredentialRepository passkeyCredentialRepository;
    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    @Override
    public PasskeyOptionsResponse startRegistration(UUID userId) {
        User user = findUserOrThrow(userId);
        if (passkeyCredentialRepository.existsByUser_Id(userId)) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Tài khoản đã có passkey. Vui lòng xóa passkey hiện tại trước khi thêm mới.");
        }

        UserIdentity userIdentity = UserIdentity.builder()
                .name(user.getEmail())
                .displayName(user.getFullName())
                .id(UserHandles.of(user.getId()))
                .build();

        PublicKeyCredentialCreationOptions options = relyingParty.startRegistration(
                StartRegistrationOptions.builder()
                        .user(userIdentity)
                        .authenticatorSelection(AuthenticatorSelectionCriteria.builder()
                                // REQUIRED = discoverable credential (passkey), needed so login
                                // can work without asking for an email first (per product decision).
                                .residentKey(ResidentKeyRequirement.REQUIRED)
                                .userVerification(UserVerificationRequirement.PREFERRED)
                                .build())
                        .build());

        String requestId = challengeStore.putRegistration(userId, options);
        return new PasskeyOptionsResponse(requestId, toJson(safeJson(options)));
    }

    @Override
    @Transactional
    public void finishRegistration(UUID userId, PasskeyRegistrationVerifyRequest request) {
        PasskeyChallengeStore.RegistrationChallenge challenge = challengeStore.takeRegistration(request.getRequestId())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST,
                        "Yêu cầu thiết lập passkey đã hết hạn. Vui lòng thử lại."));

        if (!challenge.userId().equals(userId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Yêu cầu không hợp lệ.");
        }
        if (passkeyCredentialRepository.existsByUser_Id(userId)) {
            throw new ApiException(HttpStatus.CONFLICT, "Tài khoản đã có passkey.");
        }

        PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> pkc =
                parseRegistrationResponse(request.getCredential());

        RegistrationResult result;
        try {
            result = relyingParty.finishRegistration(FinishRegistrationOptions.builder()
                    .request(challenge.options())
                    .response(pkc)
                    .build());
        } catch (RegistrationFailedException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Thiết lập passkey thất bại. Vui lòng thử lại.");
        }

        User user = findUserOrThrow(userId);

        String transports = pkc.getResponse().getTransports().stream()
                .map(AuthenticatorTransport::getId)
                .collect(Collectors.joining(","));

        PasskeyCredential credential = PasskeyCredential.builder()
                .user(user)
                .credentialId(result.getKeyId().getId().getBase64Url())
                .publicKey(result.getPublicKeyCose().getBase64Url())
                .signCount(result.getSignatureCount())
                .deviceName(blankToNull(request.getDeviceName()))
                .transports(transports.isBlank() ? null : transports)
                .build();
        passkeyCredentialRepository.save(credential);

        user.setPasskeyEnabled(true);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void deletePasskey(UUID userId) {
        passkeyCredentialRepository.findByUser_Id(userId).ifPresent(credential -> {
            passkeyCredentialRepository.delete(credential);
            User user = credential.getUser();
            user.setPasskeyEnabled(false);
            userRepository.save(user);
        });
    }

    @Override
    public PasskeyOptionsResponse startLogin() {
        // No username/userHandle given -> usernameless/discoverable ceremony: the
        // browser itself prompts the user to pick a saved passkey for this site.
        AssertionRequest request = relyingParty.startAssertion(StartAssertionOptions.builder()
                .userVerification(UserVerificationRequirement.PREFERRED)
                .build());

        String requestId = challengeStore.putLogin(request);
        return new PasskeyOptionsResponse(requestId, toJson(safeJson(request)));
    }

    @Override
    @Transactional
    public AuthService.LoginResult finishLogin(PasskeyLoginVerifyRequest request) {
        AssertionRequest assertionRequest = challengeStore.takeLogin(request.getRequestId())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST,
                        "Yêu cầu đăng nhập bằng passkey đã hết hạn. Vui lòng thử lại."));

        PublicKeyCredential<AuthenticatorAssertionResponse, ClientAssertionExtensionOutputs> pkc =
                parseAssertionResponse(request.getCredential());

        AssertionResult result;
        try {
            result = relyingParty.finishAssertion(FinishAssertionOptions.builder()
                    .request(assertionRequest)
                    .response(pkc)
                    .build());
        } catch (AssertionFailedException e) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Xác thực passkey thất bại.");
        }

        if (!result.isSuccess()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Xác thực passkey thất bại.");
        }

        User user = userRepository.findByEmail(result.getUsername())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Tài khoản không tồn tại."));

        passkeyCredentialRepository.findByCredentialId(result.getCredentialId().getBase64Url())
                .ifPresent(credential -> {
                    credential.setSignCount(result.getSignatureCount());
                    credential.setLastUsedAt(OffsetDateTime.now());
                    passkeyCredentialRepository.save(credential);
                });

        String token = jwtService.generateToken(user.getId(), user.getEmail());
        return new AuthService.LoginResult(token, UserResponse.from(user));
    }

    // --- helpers ---

    private User findUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"));
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    private String safeJson(PublicKeyCredentialCreationOptions options) {
        try {
            return options.toCredentialsCreateJson();
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể tạo yêu cầu thiết lập passkey.");
        }
    }

    private String safeJson(AssertionRequest request) {
        try {
            return request.toCredentialsGetJson();
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể tạo yêu cầu đăng nhập bằng passkey.");
        }
    }

    private JsonNode toJson(String rawJson) {
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            // Yubico's toCredentialsCreateJson()/toCredentialsGetJson() already return
            // {"publicKey": {...}} — the exact shape ready to hand to
            // navigator.credentials.create()/get() — NOT the flat options dict itself.
            // Unwrap that layer here so PasskeyOptionsResponse.publicKey holds the
            // actual options object; otherwise the frontend ends up with it double
            // nested (publicKey.publicKey.challenge) and every required field looks
            // "undefined" to the browser's WebAuthn parser.
            JsonNode inner = root.get("publicKey");
            return inner != null ? inner : root;
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể xử lý dữ liệu passkey.");
        }
    }

    private PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> parseRegistrationResponse(JsonNode credentialNode) {
        try {
            return PublicKeyCredential.parseRegistrationResponseJson(objectMapper.writeValueAsString(credentialNode));
        } catch (IOException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Dữ liệu passkey không hợp lệ.");
        }
    }

    private PublicKeyCredential<AuthenticatorAssertionResponse, ClientAssertionExtensionOutputs> parseAssertionResponse(JsonNode credentialNode) {
        try {
            return PublicKeyCredential.parseAssertionResponseJson(objectMapper.writeValueAsString(credentialNode));
        } catch (IOException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Dữ liệu passkey không hợp lệ.");
        }
    }
}
