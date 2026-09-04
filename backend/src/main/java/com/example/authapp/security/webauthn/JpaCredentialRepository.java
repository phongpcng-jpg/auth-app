package com.example.authapp.security.webauthn;

import com.example.authapp.entity.PasskeyCredential;
import com.example.authapp.entity.User;
import com.example.authapp.repository.PasskeyCredentialRepository;
import com.example.authapp.repository.UserRepository;
import com.yubico.webauthn.CredentialRepository;
import com.yubico.webauthn.RegisteredCredential;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.PublicKeyCredentialDescriptor;
import com.yubico.webauthn.data.exception.Base64UrlException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Database-backed implementation of Yubico's {@link CredentialRepository}.
 * "username" throughout this class is the user's email — the same value we
 * use as the login identifier everywhere else in the app. The WebAuthn
 * "user handle" is derived from User.id (see {@link UserHandles}).
 */
@Component
@RequiredArgsConstructor
public class JpaCredentialRepository implements CredentialRepository {

    private final UserRepository userRepository;
    private final PasskeyCredentialRepository passkeyCredentialRepository;

    @Override
    public Set<PublicKeyCredentialDescriptor> getCredentialIdsForUsername(String username) {
        return userRepository.findByEmail(username)
                .flatMap(user -> passkeyCredentialRepository.findByUser_Id(user.getId()))
                .map(cred -> Set.of(descriptorOf(cred)))
                .orElseGet(Set::of);
    }

    @Override
    public Optional<ByteArray> getUserHandleForUsername(String username) {
        return userRepository.findByEmail(username).map(User::getId).map(UserHandles::of);
    }

    @Override
    public Optional<String> getUsernameForUserHandle(ByteArray userHandle) {
        return userRepository.findById(UserHandles.toUuid(userHandle)).map(User::getEmail);
    }

    @Override
    public Optional<RegisteredCredential> lookup(ByteArray credentialId, ByteArray userHandle) {
        return passkeyCredentialRepository.findByCredentialId(credentialId.getBase64Url())
                .filter(cred -> UserHandles.of(cred.getUser().getId()).equals(userHandle))
                .map(this::toRegisteredCredential);
    }

    @Override
    public Set<RegisteredCredential> lookupAll(ByteArray credentialId) {
        return passkeyCredentialRepository.findAllByCredentialId(credentialId.getBase64Url()).stream()
                .map(this::toRegisteredCredential)
                .collect(Collectors.toSet());
    }

    private RegisteredCredential toRegisteredCredential(PasskeyCredential cred) {
        try {
            return RegisteredCredential.builder()
                    .credentialId(ByteArray.fromBase64Url(cred.getCredentialId()))
                    .userHandle(UserHandles.of(cred.getUser().getId()))
                    .publicKeyCose(ByteArray.fromBase64Url(cred.getPublicKey()))
                    .signatureCount(cred.getSignCount())
                    .build();
        } catch (Base64UrlException e) {
            throw new IllegalArgumentException(
                    "Invalid Base64URL data in passkey credential. credentialId=" + cred.getCredentialId(), e);
        }
    }

    private PublicKeyCredentialDescriptor descriptorOf(PasskeyCredential cred) {
        try {
            return PublicKeyCredentialDescriptor.builder()
                    .id(ByteArray.fromBase64Url(cred.getCredentialId()))
                    .build();
        } catch (Base64UrlException e) {
            throw new IllegalArgumentException("Invalid Base64URL credential ID: " + cred.getCredentialId(), e);
        }
    }
}
