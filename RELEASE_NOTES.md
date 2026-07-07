## What's new in v1.0.11

### Fix — auto-update never actually converged
`QTraceCompliancePlugin.COMPLIANCE_VERSION` (and `QTraceController.VERSION`) were hardcoded string literals, last updated for v1.0.8 and never touched again across the 1.0.9/1.0.10 bumps. Even with a single, correctly-named JAR on disk and no stale duplicate, the loaded class kept reporting `"1.0.8"` forever — so the update dialog re-offered the same "upgrade" after every restart no matter what was actually installed. Both constants now read the version straight from the JAR manifest instead of a literal that can silently go stale.

---

### Fixed
- **Version constants now read from the JAR manifest** (`Implementation-Version`) instead of a hardcoded literal, so the auto-updater's version comparison can never drift out of sync with an actual release again

---

## Installation

Drop `qtrace-core-1.0.11.jar` into your QuPath extensions folder:

| Platform | Path |
|---|---|
| macOS | `~/Library/Application Support/QuPath/v0.7/extensions/` |
| Windows | `%APPDATA%\QuPath\v0.7\extensions\` |
| Linux | `~/.local/share/QuPath/v0.7/extensions/` |

Requires **QuPath 0.5+** (tested on 0.7.x).
