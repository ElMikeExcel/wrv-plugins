# ExcelCosmetics 0.1.3 — SOURCE.md

Ownership: 100% ours. Original Excel plugin for Wink Reyes Valley (no upstream code). Compile-time links: Paper API + VaultAPI 1.7 (soft depend at runtime).

## Base
Built ONLY on the verified 0.1.2 tree `~/workspace/excelcosmetics/build-0.1.2/` — jar SHA-256 `45fc556ca0f84f77209faa1fb13a540f39e5a31248b0b75467d6d5586636509b` and source tarball `82fbe0e784d071292601bd417972073a45a7e1a8934463b1e9de6e6a73ca6f85`, both re-verified this build day (Nov 6, 2026). Authority: disposition `~/workspace/moon-audit-reconciliation/COSMETICS-012-DISPOSITION.md` (CONFIRMED, MODERATE) + brief `BRIEF.md` in this folder + desk review `~/workspace/goals/family-minecraft-server-build/hidden_files/cosmetics-013-brief-desk-review-20261106.md`.

## What changed (nothing else)
1. **Snapshot+compensate (refund defect fix):** `PlayerState.Snapshot`/`snapshot()`/`restore()`, `GrantStore.snapshotPlayer/restorePlayer/drop`, and `GrantStore.grantAndPersist(...)` — snapshot before granting, restore exactly and synchronously inside the call on persist failure (or grant exception), then return false. First-ever purchase compensation drops the created state. `ExcelCosmetics.applyAndPersist` now delegates to `grantAndPersist` with `stateStore.save()` as the persist step. Invariant: false ⇒ nothing changed.
2. **Atomic save:** new core `AtomicWrite.writeAtomically` (write `players.yml.tmp`, then `Files.move` ATOMIC_MOVE+REPLACE_EXISTING, falling back to plain REPLACE_EXISTING only on AtomicMoveNotSupportedException; move inside the failure contract so an IOException returns false and triggers compensation). `StateStore.save()` serializes via `saveToString()` and writes through AtomicWrite.
3. **Desk-review note 1b disposition:** `equipAfterPurchase`'s post-success save is no longer silently ignored — it logs a warning on failure ("WARNING: state save failed", matching the hand-grant path). Rationale recorded: that save is post-SUCCESS and cosmetic-only (equip/set-removal self-heals at the next save; money and grant are already durable); set-removal does NOT belong inside `applyAndPersist` compensation because it has not happened at that point.
4. Version 0.1.2 → 0.1.3 (plugin.yml, build.sh output name).

Out of scope per brief: no pricing/catalog/menu/content changes, no public-build decoupling, no fingerprint bake.

## Tests
New `tests/TestCompensation.java` (31 checks) with a mutating production-order applier (real GrantStore mutated before persist fails): failed-save initial purchase (no grant, equip unchanged, refund exact), failed-save active renewal, failed-save lapsed renewal, failed-save own-upgrade (still rental), failed-save first-ever (no state retained), success-after-failure (no resurrection), healthy control, and AtomicWrite tests (successful replace; blocked tmp write leaves last good file intact). Full suite: **316 checks, 0 failed** (285 baseline 0.1.2 checks stay green + 31 new).

## Build
VM JDK `~/workspace/.jdk25` (Temurin 25.0.4.1; base jar was built with 25.0.5 — per desk-review note 5, whole-jar SHA match to the base is not expected; this is a new version). Deterministic double-build verified byte-identical.

| Artifact | Size | SHA-256 |
|---|---|---|
| ExcelCosmetics-0.1.3.jar | 82,562 B | e18d5af59657ca44f0d2747e62ffe09a71e2056e32225f069ccc9e0b0ad3162b |
| ExcelCosmetics-0.1.3-source.tar.gz | 45,123 B | a1ebbc881ad67e9b94c0d09539af853f7dd5fcde4c7bab236a13d71095ada957 |

## Install gating (not done here)
STAGED only. Install rides the wrv-install-failsafe queue via INSTALL-PACKET.md in this folder: at packet/install time re-pull the live jar from the panel and re-prove SHA `45fc556c…` before swapping; BACKUP FIRST / cap-at-6; empty-server window; boot-verify enable line family "13 colors, 7 effects, 5 sets, emote pack ready"; player proof = one real purchase per shape (rent + own); forced-save-failure stays in the rig, never production.
