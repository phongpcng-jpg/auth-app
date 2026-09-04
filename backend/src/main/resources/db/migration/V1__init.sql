CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255) NOT NULL UNIQUE,
    full_name       VARCHAR(255) NOT NULL,
    phone_number    VARCHAR(50),
    password_hash   VARCHAR(255),
    role            VARCHAR(20) NOT NULL DEFAULT 'USER',
    passkey_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

-- Reserved for Google OAuth2 login (implemented in a later step)
CREATE TABLE oauth_accounts (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider          VARCHAR(50) NOT NULL,
    provider_user_id  VARCHAR(255) NOT NULL,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    UNIQUE (provider, provider_user_id)
);

-- Reserved for Passkey / WebAuthn login (implemented in a later step)
CREATE TABLE passkey_credentials (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    credential_id  VARCHAR(1024) NOT NULL UNIQUE,
    public_key     TEXT NOT NULL,
    sign_count     BIGINT NOT NULL DEFAULT 0,
    device_name    VARCHAR(255),
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
