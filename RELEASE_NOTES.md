## What's new in v1.0.9

### Dashboard Card 7 — Detection corrections
New Dashboard card surfacing the manual detection corrections captured by the 1.0.8 audit feature — deleted/split detections per image, with the justification note and timestamp shown inline.

### Fix — updater endpoints dropped the license on redirect
`qtrace.ca` 308-redirects to `www.qtrace.ca`. `HttpClient` drops the `Authorization` header on a cross-host redirect, which made the licensed Compliance JAR download fail with HTTP 401 even with a valid license — and the license status check (`QTraceLicenseGate`) silently reported "offline" instead of following the redirect. Both now call `www.qtrace.ca` directly.

### Fix — old JAR left on the classpath after an update
`reapOldJars` never removed the legacy unversioned `qtrace-<module>.jar` (from before the auto-updater's versioned naming). Left in place, it gave QuPath two definitions of the same plugin class to choose from, so an update could silently appear to have no effect. It is now cleaned up whenever a versioned JAR is present.

---

### Added
- **Dashboard Card 7** — manual detection corrections (deletions/splits) shown per image, with note and timestamp

### Fixed
- **Auto-updater / license check redirects** — `QTraceUpdater` and `QTraceLicenseGate` now call `www.qtrace.ca` directly instead of `qtrace.ca`, avoiding the `Authorization`-header-dropping cross-host redirect
- **`reapOldJars` legacy cleanup** — unversioned `qtrace-<module>.jar` is now removed alongside superseded versioned JARs

---

## Installation

Drop `qtrace-core-1.0.9.jar` into your QuPath extensions folder:

| Platform | Path |
|---|---|
| macOS | `~/Library/Application Support/QuPath/v0.7/extensions/` |
| Windows | `%APPDATA%\QuPath\v0.7\extensions\` |
| Linux | `~/.local/share/QuPath/v0.7/extensions/` |

Requires **QuPath 0.5+** (tested on 0.7.x).
