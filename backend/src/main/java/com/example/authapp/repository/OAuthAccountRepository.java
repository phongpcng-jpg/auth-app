package com.example.authapp.repository;

import com.example.authapp.entity.OAuthAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OAuthAccountRepository extends JpaRepository<OAuthAccount, UUID> {

    Optional<OAuthAccount> findByProviderAndProviderUserId(String provider, String providerUserId);

    boolean existsByProviderAndProviderUserId(String provider, String providerUserId);
}
