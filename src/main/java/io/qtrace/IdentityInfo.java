/*
 * qTrace — QuPath workflow provenance extension
 * Copyright (C) 2026 Romain Tourte
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package io.qtrace;

/**
 * Digital identity snapshot fetched from qtrace.ca — validator key, Polygon anchor
 * status, and public badge state. Read-only mirror of the portal's "Digital Identity"
 * card (anchorTxHash/anchorAt are null until the license's anchoring transaction lands).
 */
public record IdentityInfo(
    String  signingKeyPub,
    String  anchorTxHash,
    Integer anchorChainId,
    String  anchorAt,        // ISO date string, or null if not yet anchored
    boolean identityPublic,
    String  explorerUrl,     // block explorer link for anchorTxHash, or null
    String  badgeUrl         // public badge page URL, or null
) {
    /** Compact fingerprint for display: first 8 + … + last 6 chars. */
    public String signingKeyPubShort() {
        if (signingKeyPub == null || signingKeyPub.length() < 16) return signingKeyPub != null ? signingKeyPub : "";
        return signingKeyPub.substring(0, 8) + "…" + signingKeyPub.substring(signingKeyPub.length() - 6);
    }

    /** Compact tx hash for display: first 10 + … + last 6 chars. */
    public String anchorTxHashShort() {
        if (anchorTxHash == null || anchorTxHash.length() < 20) return anchorTxHash != null ? anchorTxHash : "";
        return anchorTxHash.substring(0, 10) + "…" + anchorTxHash.substring(anchorTxHash.length() - 6);
    }

    public boolean anchored() {
        return anchorTxHash != null && !anchorTxHash.isBlank();
    }
}
