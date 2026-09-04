package com.example.authapp.security.webauthn;

import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.data.RelyingPartyIdentity;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration
@RequiredArgsConstructor
public class WebAuthnConfig {

    private final JpaCredentialRepository credentialRepository;

    @Value("${app.passkey.rp-id}")
    private String rpId;

    @Value("${app.passkey.rp-name}")
    private String rpName;

    @Value("${app.passkey.origin}")
    private String origin;

    @Bean
    public RelyingParty relyingParty() {
        RelyingPartyIdentity identity = RelyingPartyIdentity.builder()
                .id(rpId)
                .name(rpName)
                .build();

        return RelyingParty.builder()
                .identity(identity)
                .credentialRepository(credentialRepository)
                .origins(Set.of(origin))
                // We don't need to know/verify which authenticator model was used,
                // only that a valid WebAuthn credential was presented — most
                // consumer apps skip attestation for this reason. Leaving trust
                // source unset means attestation statements are accepted without
                // being checked against a trust root (RegistrationResult#isAttestationTrusted()
                // will simply report false, which we don't use for anything).
                .allowOriginPort(false)
                .allowOriginSubdomain(false)
                .build();
    }
}
