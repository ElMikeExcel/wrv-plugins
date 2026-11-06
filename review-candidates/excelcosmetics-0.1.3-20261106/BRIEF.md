# ExcelCosmetics 0.1.3 — BUILD BRIEF (staged Nov 5, 2026 — NOT built)

**Purpose:** close the CONFIRMED "refund while retaining" defect
in the live 0.1.2 economy path. One defect, one adjacent
crash-safety fix in the same method, nothing else.

**Gate:** build ONLY on the verified 0.1.2 tree —
`~/workspace/excelcosmetics/build-0.1.2/` (jar SHA-256
`45fc556c…`, source tarball `82fbe0e7…`; see its
`ExcelCosmetics-0.1.2-SOURCE.md`). At packet time, re-pull the
live jar from the panel and re-prove the SHA match before
swapping (the live↔source match is hash-proven as of
Oct 27, 2026; the disposition's boundary note asks for a
fresh pull to close it for install day).

**Authority:** full defect disposition —
`~/workspace/moon-audit-reconciliation/COSMETICS-012-DISPOSITION.md`
(Oct 31, 2026; verdict CONFIRMED, severity MODERATE,
fault-gated, no known live incident; independently reproduced
at `~/workspace/moon-audit-reconciliation/cosmetics-repro/`).

## The defect in one paragraph

`PurchaseService.buy()` refunds the charge whenever the grant
applier returns false, trusting that false means "nothing
changed." The production adapter (`ExcelCosmetics.applyAndPersist`)
grants in memory FIRST (`stateStore.store().grant(...)` — new
grant, rental extension, rental→PERMANENT upgrade, auto-equip
side effects) and persists SECOND (`stateStore.save()`).
`StateStore.save()` returns false on IOException without
restoring the prior state. Net: on a save failure the player
is refunded AND keeps the cosmetic, which becomes permanent
at the next successful save of any kind. The shipped test
suite missed it because its `FakeApplier` mutates nothing on
failure.

## Fix shape (from the disposition)

1. **Snapshot + compensate in the adapter (RECOMMENDED —
   smallest correct change).** In `applyAndPersist`, snapshot
   the player's pre-purchase state before granting: the
   grants list (`PlayerState.grants()` already returns a
   copy) plus the three equipped ids. A small
   `GrantStore`/`PlayerState` snapshot+restore facility is
   the clean implementation. On `save()` returning false,
   restore the snapshot exactly, then return false — so the
   refund proceeds against a truly unchanged state.
   The snapshot must cover EVERY mutation `applyGrant`
   performs:
   - fresh grant added;
   - active rental's expiry extended (renewal);
   - rental replaced by a PERMANENT grant (own-it upgrade);
   - `autoEquip` side effects, including set-removed
     interactions;
   - first-ever purchase, where `state()` creates the
     `PlayerState` — restore must DROP the created state,
     not leave an empty one behind.
2. **Alternative — stage, then commit (acceptable, larger
   blast radius):** apply the grant to a cloned `PlayerState`,
   persist the store-with-clone, swap the clone into the live
   map only on success. Equally correct; riskier in a
   synchronized store read by async chat threads. Only choose
   this if the build finds the compensation route unsound,
   and record why in the build record.
   Invariant either way: **false ⇒ nothing changed.**
3. **Adjacent, same method — atomic save (do in this build):**
   `StateStore.save()` currently writes `players.yml`
   directly; a crash mid-write can truncate the only copy.
   Write to `players.yml.tmp`, then
   `Files.move(..., ATOMIC_MOVE, REPLACE_EXISTING)`.
   Independent of the refund defect; same method is open.

## Tests required before "ready" (house rule)

A production-order applier test whose fake **mutates the
store before failing** (the exact shipped-suite blind spot):

- failed-save initial purchase → no grant, no equip change,
  refund exact;
- failed-save renewal → expiry unchanged;
- failed-save own-upgrade → still a rental (not permanent);
- failed-save first-ever purchase → no player state retained;
- success-after-failure → a subsequent healthy save must NOT
  resurrect the compensated grant;
- atomic-save test → a failed/partial tmp write leaves the
  last good `players.yml` intact.

The existing suite (285 checks on 0.1.2) stays green.
Deterministic double-build, byte-identical jars, per the
lineage's established pattern.

## Explicitly out of scope

- No pricing, catalog, menu, or cosmetic-content changes.
- No public-build decoupling (Vault charge API / generic
  shop hook / neutral sets) — that is the listing pass, a
  separate later build.
- No fingerprint bake in this build (bake is its own pass;
  the 0.1.2 bake is unblocked and independent).

## Install gating (when built)

Normal queue path: INSTALL-PACKET → BACKUP FIRST → failsafe
empty-server window → boot-verify (enable line family:
"13 colors, 7 effects, 5 sets, emote pack ready") → player
proof = one real purchase per shape (rent + own) on the live
server; the forced-save-failure cases stay in the rig, never
in production.

## Dispatch note

This brief is complete enough to dispatch the build in one
step: base tree verified, fix shape chosen, test list fixed,
gating defined. Nothing here has been built yet.
