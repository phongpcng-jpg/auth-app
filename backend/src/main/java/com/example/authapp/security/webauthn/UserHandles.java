package com.example.authapp.security.webauthn;

import com.yubico.webauthn.data.ByteArray;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Converts between our User.id (UUID) and the WebAuthn "user handle"
 * (an opaque ByteArray). Reusing the existing UUID means we don't need a
 * separate column just to hold a random user handle.
 */
public final class UserHandles {

    private UserHandles() {
    }

    public static ByteArray of(UUID userId) {
        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.putLong(userId.getMostSignificantBits());
        buffer.putLong(userId.getLeastSignificantBits());
        return new ByteArray(buffer.array());
    }

    public static UUID toUuid(ByteArray userHandle) {
        ByteBuffer buffer = ByteBuffer.wrap(userHandle.getBytes());
        long mostSigBits = buffer.getLong();
        long leastSigBits = buffer.getLong();
        return new UUID(mostSigBits, leastSigBits);
    }
}
