## What's new in v1.0.10

### Fix — auto-update could get stuck re-offering the same version
The JAR superseded by an update (e.g. the bare legacy `qtrace-compliance.jar`) was only cleaned up by `reapOldJars` on the *next* startup, so right after clicking "Install" and restarting, both the old and new JAR were still present at once. QuPath's classloader only ever resolves one `Class` object per fully-qualified name, so the "pick the plugin reporting the highest version" logic couldn't out-vote whichever JAR the classloader happened to load first — the update could silently never take effect, with the identical update dialog reappearing on every subsequent restart. The superseded file is now removed immediately after the new one is written, so a single restart is enough.

---

### Fixed
- **Auto-update stuck loop** — the superseded JAR for a module is now reaped immediately after install instead of waiting for the following startup, so the swap reliably takes effect after one restart

---

## Installation

Drop `qtrace-core-1.0.10.jar` into your QuPath extensions folder:

| Platform | Path |
|---|---|
| macOS | `~/Library/Application Support/QuPath/v0.7/extensions/` |
| Windows | `%APPDATA%\QuPath\v0.7\extensions\` |
| Linux | `~/.local/share/QuPath/v0.7/extensions/` |

Requires **QuPath 0.5+** (tested on 0.7.x).
