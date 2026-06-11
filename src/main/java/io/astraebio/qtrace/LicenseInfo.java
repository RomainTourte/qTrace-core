package io.astraebio.qtrace;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Decoded payload from a validated .qtlicense JWT.
 */
public record LicenseInfo(
    String  clerkUserId,
    String  name,
    String  institution,
    boolean verified,
    Instant issuedAt,
    Instant expiresAt
) {
    public boolean expired() {
        return Instant.now().isAfter(expiresAt);
    }

    /** Human-readable expiry date (ISO local date). */
    public String expiresAtFormatted() {
        return LocalDate.ofInstant(expiresAt, ZoneId.systemDefault()).toString();
    }
}
