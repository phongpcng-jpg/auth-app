package com.example.authapp.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A registered WebAuthn/Passkey credential (Step 3). Product decision: at
 * most one active credential per user (see V2__passkey_single_credential.sql
 * for the DB-level unique constraint on user_id) — a single passkey can
 * still be used across devices via the standard WebAuthn cross-device
 * ("hybrid"/QR) flow, so this doesn't limit users to one physical device.
 *
 * credentialId / publicKey are stored Base64Url-encoded (matches
 * com.yubico.webauthn.data.ByteArray#getBase64Url()) so they can be decoded
 * straight back into a ByteArray without a separate encoding scheme.
 */
@Entity
@Table(name = "passkey_credentials")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasskeyCredential {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Base64Url-encoded WebAuthn credential ID. */
    @Column(name = "credential_id", nullable = false, unique = true)
    private String credentialId;

    /** Base64Url-encoded COSE_Key-format public key (RegistrationResult#getPublicKeyCose). */
    @Column(name = "public_key", nullable = false, columnDefinition = "TEXT")
    private String publicKey;

    @Column(name = "sign_count", nullable = false)
    @Builder.Default
    private long signCount = 0L;

    @Column(name = "device_name")
    private String deviceName;

    /** Comma-separated AuthenticatorTransport names (e.g. "internal,hybrid"), if reported. */
    @Column(name = "transports")
    private String transports;

    @Column(name = "last_used_at")
    private OffsetDateTime lastUsedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
