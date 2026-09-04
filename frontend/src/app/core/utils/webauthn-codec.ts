/**
 * Thin wrappers around the browser's own WebAuthn JSON (de)serialization
 * methods (WebAuthn Level 3 spec — Baseline since March 2025 in Chrome/Edge
 * 129+, Firefox 119+, Safari 18.4+). The backend (Yubico java-webauthn-server)
 * produces/consumes exactly this same standard JSON shape, so handing the
 * raw server JSON straight to these built-ins avoids any risk of a
 * hand-written base64url encoder/decoder getting a field name or nesting
 * level wrong.
 *
 * `as any` casts below are only to work around TypeScript's bundled DOM
 * typings not yet declaring these (newer) methods — the methods themselves
 * exist and work at runtime in all supported browsers (see PasskeyService.isSupported()).
 */

export function decodeCreationOptions(json: any): PublicKeyCredentialCreationOptions {
  return (PublicKeyCredential as any).parseCreationOptionsFromJSON(json);
}

export function decodeRequestOptions(json: any): PublicKeyCredentialRequestOptions {
  return (PublicKeyCredential as any).parseRequestOptionsFromJSON(json);
}

/** credential.toJSON() returns the standard RegistrationResponseJSON shape the backend expects. */
export function encodeRegistrationCredential(credential: PublicKeyCredential): unknown {
  return (credential as any).toJSON();
}

/** credential.toJSON() returns the standard AuthenticationResponseJSON shape the backend expects. */
export function encodeAssertionCredential(credential: PublicKeyCredential): unknown {
  return (credential as any).toJSON();
}
