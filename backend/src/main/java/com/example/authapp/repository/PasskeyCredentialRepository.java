package com.example.authapp.repository;

import com.example.authapp.entity.PasskeyCredential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PasskeyCredentialRepository extends JpaRepository<PasskeyCredential, UUID> {

    /** At most one row per user by product decision (also enforced by a DB unique constraint). */
    Optional<PasskeyCredential> findByUser_Id(UUID userId);

    boolean existsByUser_Id(UUID userId);

    Optional<PasskeyCredential> findByCredentialId(String credentialId);

    /** Used by CredentialRepository#lookupAll — normally 0 or 1 result given the unique constraint. */
    List<PasskeyCredential> findAllByCredentialId(String credentialId);

    void deleteByUser_Id(UUID userId);
}
