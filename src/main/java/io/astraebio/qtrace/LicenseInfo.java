package io.astraebio.qtrace;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Decoded payload from a validated .qtlicense JWT.
 * validatorKey: ED25519 public key (base64url, SPKI-DER encoded, ~44 chars).
 *               Stable per validator — used for stamp signing and future blockchain anchoring.
 */
public record LicenseInfo(
    String  clerkUserId,
    String  name,
    String  institution,
    boolean verified,
    Instant issuedAt,
    Instant expiresAt,
    String  validatorKey   // ED25519 public key base64url, may be empty for older licenses
) {
    public boolean expired() {
        return Instant.now().isAfter(expiresAt);
    }

    /** Human-readable expiry date (ISO local date). */
    public String expiresAtFormatted() {
        return LocalDate.ofInstant(expiresAt, ZoneId.systemDefault()).toString();
    }

    /** Compact fingerprint for display: first 8 + … + last 6 chars. */
    public String validatorKeyShort() {
        if (validatorKey == null || validatorKey.length() < 16) return validatorKey != null ? validatorKey : "";
        return validatorKey.substring(0, 8) + "…" + validatorKey.substring(validatorKey.length() - 6);
    }
}
