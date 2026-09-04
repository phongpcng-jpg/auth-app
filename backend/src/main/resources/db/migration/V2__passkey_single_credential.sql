-- Step 3: Passkey / WebAuthn.
-- Product decision (confirmed with user): at most 1 active passkey per account.
-- The unique constraint on user_id enforces this at the DB level as a safety
-- net in addition to the application-level check in PasskeyServiceImpl.
ALTER TABLE passkey_credentials
    ADD CONSTRAINT uq_passkey_credentials_user UNIQUE (user_id);

-- Transport hints (e.g. "internal,hybrid") returned by the authenticator at
-- registration time. Used to build allowCredentials transport hints on login,
-- which lets the browser skip irrelevant transports (nice-to-have UX, not
-- required for correctness).
ALTER TABLE passkey_credentials
    ADD COLUMN transports VARCHAR(255);

-- Updated on every successful passkey login; not exposed anywhere yet but
-- cheap to capture now so a "last used" hint can be added to the profile
-- page later without another migration.
ALTER TABLE passkey_credentials
    ADD COLUMN last_used_at TIMESTAMP WITH TIME ZONE;
