package com.example.authapp.service.impl;

import com.example.authapp.dto.GoogleEmailVerificationResponse;
import com.example.authapp.dto.GoogleTokenRequest;
import com.example.authapp.dto.LoginRequest;
import com.example.authapp.dto.RegisterRequest;
import com.example.authapp.dto.UserResponse;
import com.example.authapp.entity.OAuthAccount;
import com.example.authapp.entity.Role;
import com.example.authapp.entity.User;
import com.example.authapp.exception.ApiException;
import com.example.authapp.repository.OAuthAccountRepository;
import com.example.authapp.repository.UserRepository;
import com.example.authapp.security.GoogleTokenVerifier;
import com.example.authapp.security.JwtService;
import com.example.authapp.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String GOOGLE_PROVIDER = "google";

    private final UserRepository userRepository;
    private final OAuthAccountRepository oAuthAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final GoogleTokenVerifier googleTokenVerifier;

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Mật khẩu nhập lại không khớp");
        }

        // Defense in depth: don't just trust the email the client claims to
        // have verified earlier in the wizard — re-check the ID token here too.
        GoogleTokenVerifier.GooglePayload googlePayload = googleTokenVerifier.verify(request.getGoogleIdToken());
        if (!googlePayload.email().equalsIgnoreCase(request.getEmail())) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Email chưa khớp với tài khoản Google đã xác thực. Vui lòng xác thực lại email.");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ApiException(HttpStatus.CONFLICT, "Email đã được sử dụng");
        }
        if (oAuthAccountRepository.existsByProviderAndProviderUserId(GOOGLE_PROVIDER, googlePayload.subject())) {
            throw new ApiException(HttpStatus.CONFLICT, "Tài khoản Google này đã được liên kết với một tài khoản khác");
        }

        User user = User.builder()
                .email(request.getEmail())
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                // Starts false; flips to true only after a real WebAuthn
                // registration ceremony succeeds (see PasskeyServiceImpl).
                .passkeyEnabled(false)
                .build();
        user = userRepository.save(user);

        OAuthAccount oAuthAccount = OAuthAccount.builder()
                .user(user)
                .provider(GOOGLE_PROVIDER)
                .providerUserId(googlePayload.subject())
                .build();
        oAuthAccountRepository.save(oAuthAccount);

        return UserResponse.from(user);
    }

    @Override
    public LoginResult login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Email hoặc mật khẩu không đúng"));

        if (user.getPasswordHash() == null
                || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Email hoặc mật khẩu không đúng");
        }

        String token = jwtService.generateToken(user.getId(), user.getEmail());
        return new LoginResult(token, UserResponse.from(user));
    }

    @Override
    public GoogleEmailVerificationResponse verifyGoogleEmail(GoogleTokenRequest request) {
        GoogleTokenVerifier.GooglePayload payload = googleTokenVerifier.verify(request.getIdToken());
        return new GoogleEmailVerificationResponse(payload.email());
    }

    @Override
    @Transactional
    public LoginResult loginWithGoogle(GoogleTokenRequest request) {
        GoogleTokenVerifier.GooglePayload payload = googleTokenVerifier.verify(request.getIdToken());

        Optional<OAuthAccount> linked = oAuthAccountRepository
                .findByProviderAndProviderUserId(GOOGLE_PROVIDER, payload.subject());

        User user;
        if (linked.isPresent()) {
            user = linked.get().getUser();
        } else {
            // No Google account linked yet — auto-link to the existing password
            // account with the same (Google-verified) email, if any.
            user = userRepository.findByEmail(payload.email())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                            "Tài khoản chưa tồn tại. Vui lòng đăng ký trước khi đăng nhập bằng Google."));

            OAuthAccount oAuthAccount = OAuthAccount.builder()
                    .user(user)
                    .provider(GOOGLE_PROVIDER)
                    .providerUserId(payload.subject())
                    .build();
            oAuthAccountRepository.save(oAuthAccount);
        }

        String token = jwtService.generateToken(user.getId(), user.getEmail());
        return new LoginResult(token, UserResponse.from(user));
    }
}
