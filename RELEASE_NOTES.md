## What's new in v1.0.8

### Detection correction audit
When an analyst manually deletes or subdivides detections — removing false positives, separating merged cells, correcting AI over-segmentation — qTrace now **captures the event and prompts for a justification note**.

A non-intrusive dialog appears automatically:
- Heading shows the count: *"3 detections manually deleted"*
- Free-text field for a short rationale: *"merged artifact — two clearly distinct nuclei"*
- **Save** records the note · **Skip** records the event silently

All corrections are exported to the `.qtrace` sidecar under `manual_detection_corrections[]`, with timestamp, author, deleted object UUIDs, centroid coordinates, and the justification note. Split operations (delete + re-draw) are recorded as type `"split"` with both `deleted[]` and `created[]` arrays.

**Silent mode:** disable the prompt via Settings → Capture → "Prompt for detection correction note". Events are still recorded without asking.

### Fix — multi-detection deletion in QuPath 0.7
QuPath 0.7 fires `OTHER_STRUCTURE_CHANGE` (not `REMOVED`) when the user deletes ≥ 3 objects via the confirmation dialog — with an empty `changed` list, making the deletion invisible to standard hierarchy listeners.

The fix uses a `PathObjectSelectionListener` to snapshot selected detections before the dialog opens, then identifies removed objects by diffing against the hierarchy after the event. Deletions of 1–2 objects (no dialog) continue to use the direct `REMOVED` path.

---

### Added
- **Detection correction audit** — manual detection deletions and splits are captured with timestamp, author, deleted UUIDs, centroid coordinates, and an optional justification note
  - Dialog: count heading + free-text note field + Save / Skip
  - Silent mode: Settings → Capture → "Prompt for detection correction note"
  - Exported to `.qtrace` under `manual_detection_corrections[]`
  - Split (delete + re-draw) recorded as type `"split"` with `deleted[]` and `created[]`

### Fixed
- **QuPath 0.7 multi-deletion event** — QuPath 0.7 fires `OTHER_STRUCTURE_CHANGE` with empty `changed` list for confirmed bulk deletions; qTrace now uses `PathObjectSelectionListener` snapshot + hierarchy diff to identify removed detections

---

## Installation

Drop `qtrace-core-1.0.8.jar` into your QuPath extensions folder:

| Platform | Path |
|---|---|
| macOS | `~/Library/Application Support/QuPath/v0.7/extensions/` |
| Windows | `%APPDATA%\QuPath\v0.7\extensions\` |
| Linux | `~/.local/share/QuPath/v0.7/extensions/` |

Requires **QuPath 0.5+** (tested on 0.7.x).
