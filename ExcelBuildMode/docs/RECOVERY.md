# Recovery journal and administrator notes

## On-disk format

`plugins/ExcelBuildMode/recovery/<player UUID>/state.bin` is the authoritative versioned, checksummed state record: session UUID plus active/completed flag. Each session has its own directory with `survival.bin` and `survival.backup.bin`. Both are written with CREATE_NEW, contain the same payload and checksum, and are never edited or deleted by the plugin. The payload includes only the explicit Survival fields described in README. Files may contain private player inventories; do not share them publicly.

Each envelope is magic EBM1 (32-bit), payload byte length (32-bit), 32-byte SHA-256, then payload. The state payload format is version1, session UUID as two 64-bit fields, and active boolean. Snapshot payload version1 uses Paper's native item byte serialization, avoiding Java object deserialization.

Entry order:
1. Read/verify previous state and refuse if active or unreadable.
2. Capture the player's explicit Survival fields, before clearing them.
3. Write primary immutable snapshot; force file.
4. Write backup immutable snapshot; force file and archive directory.
5. Read and compare both copies.
6. Write/force a temporary state file, ATOMIC_MOVE it to state.bin, then force directory.
7. Only after all preceding operations return, clear Creative inventory/XP, change mode and add any temporary permission attachment.
8. Ask vanilla to save the Build Mode state.

Restore order:
1. Read/verify active state and primary snapshot, falling back to the backup only if primary reading/checksum fails.
2. Decode and validate the full snapshot before clearing current contents.
3. For airborne forced recovery, leave journal active and enter the landing hold. Do not expose survival items in this hold.
4. At safe ground, remove attachment, clear Build contents, return to Survival and restore the explicit fields.
5. Call Player.saveData().
6. Atomically write/force a completed tombstone. Retain both snapshots.

Any thrown restore/save/completion error leaves the player locked/kicked and recovery available unless completion itself already committed. Re-running a pending recovery reapplies the same snapshot, not a snapshot of Creative/partially-restored inventory. A failure reported after completed state was committed can safely be inspected on restart; no new capture occurs automatically.

## Crash boundary and manual operations

There is no shared transaction/commit protocol between this journal and Minecraft's playerdata save. `saveData()` may log internal failures without throwing, and it does not confirm disk flush. The journal cannot make that boundary crash-proof. Do not remove, individually roll back, hand-edit or mix-and-match recovery/playerdata files. Back up and restore the whole server consistently while stopped. Keep completed snapshots until an administrator has reviewed a separate retention policy.

A missing state.bin is interpreted as no committed session, because initial/aborted preparation also has no state. Manual deletion or storage loss of that authoritative file cannot be reliably distinguished from “entry never committed.” Corruption of an existing state.bin fails closed. The state file has an atomic write and checksum, but no separate stale-state fallback; falling back to an old completed/active state could silently restore the wrong session.

ATOMIC_MOVE and directory fsync are required. There is no weaker fallback for an unsupported filesystem. Testing to date is on this Linux filesystem. Checksums detect accidental corruption, not malicious modifications. Snapshot schema1 is not a promise of cross-version Minecraft compatibility.

## Error response

Stop the affected player's activity. Keep all journal copies and server logs; take a coherent backup. Do not simply grant Creative, delete state.bin, or create a fresh snapshot over the old one. Investigate storage space/permissions and check the original payload's checksum/data-version compatibility. This prototype has no administrator import/reset command; a reviewed recovery tool/manual procedure is required before deployment.

A landing hold can persist across logout/restart. The player returns to Survival with temporary flight at the same coordinates, no carried Build contents, and controls locked. Their saved items return only at safe ground. Removing the plugin during that interval removes its ability to enforce the hold. The server must keep the plugin available or keep the player offline until recovery is handled.
