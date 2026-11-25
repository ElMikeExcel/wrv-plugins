# ExcelEMFixes 0.3.1 — Merged Filing Candidate (Nov 22, 2026)

**This jar supersedes BOTH staged packets as the single filing
candidate:** the ExcelEMFixes **0.3.0** packet (enderman-resistance
module, `~/workspace/enderman-magic-resistance-20261122/INSTALL-PACKET.md`)
and the ExcelEMFixes **0.2.1** packet (food-heal event-visible delivery
fix, `~/workspace/food-heal-stacking-20261122/RECORD.md` §6). Neither
0.3.0 nor 0.2.1 should be filed separately — 0.3.1 carries both fixes
in one version swap from the live 0.2.0.

Why a merge was needed: the 0.3.0 tree branched from 0.2.0 *before*
the food-heal fix existed, so its food-heal module still healed by
raw `setHealth` (parent verified by source grep, Nov 22, 2026
~21:31 CT: no "RegainHealth" reference anywhere in the 030 tree).
Filing 0.3.0 alone would have shipped the enderman module while
leaving the food-heal heal-accounting blind spot in place.

## 1. The port — source diff summary

Diff of `foodheal/FoodHealModule.java`, 030 tree vs the 0.2.1 fixed
source (`food-heal-stacking-20261122/src-fixed/`): **three hunks,
all part of the one fix** —

1. One import added: `org.bukkit.event.entity.EntityRegainHealthEvent`.
2. `deliver()` javadoc extended (documents the event-visible
   delivery and why silent setHealth was a defect).
3. `deliver()` body: after clamping the amount to missing health,
   it now constructs and fires `EntityRegainHealthEvent(player,
   amount, RegainReason.CUSTOM)` — mirroring EliteMobs' own
   `applyHealing` — honors cancellation and listener-adjusted
   amounts, re-clamps, applies via `setHealth`, and returns the HP
   actually delivered. Previously: `setHealth` directly, no event.

Nothing else differed — no other method, no rates, gates, budget
math, or config keys. The fixed file was copied into the merged
tree, and a full recursive diff of the merged `src/` against the
030 `src/` confirms **FoodHealModule.java is the only differing
file in the entire source tree**. Every other module — including
enderman-resistance — is byte-identical to 0.3.0.

Version stamps 0.3.0 → 0.3.1 in exactly the five places the pack
stamps its version (verified against the 0.2.0 tree's stamp set):
`ExcelEMFixes.VERSION`, `src/plugin.yml`, `build.sh` (comment +
jar name), `README.md` (install line), `rig/build-driver.sh`
(classpath). The two remaining "0.3.0" strings in the tree are
historical references to the standalone ExcelEMLeash 0.3.0 in
leash-module comments — not pack stamps, correctly untouched.

## 2. Build

Deterministic (two consecutive builds byte-identical), house
`build.sh` in `build-root/`:

- `ExcelEMFixes-0.3.1.jar` — **198,859 B** — SHA-256
  `bf49e51df4987809fb2aaa66101e933a3352a64cb41f3b492b849f793d3acc32`
- (0.3.0 was 198,621 B; the +238 B is the deliver() change.)
- Jar sanity: bundled `plugin.yml` reads `version: 0.3.1`; the
  compiled `FoodHealModule.class` references
  `EntityRegainHealthEvent` / `RegainReason` (the references the
  030 tree provably lacked).

Deploy config staged in `deploy/` is **byte-identical to the
0.3.0 packet's deploy set** (recursive diff, no differences):
`config.yml` 17,618 B, SHA-256
`74341d9d17c9f426abb24e84c5786da3deea808d2a2bd45247c9025b4583efc0`
(0.2.0's WRV deltas preserved — arcane-bolt scale 1.5, tracker
folder "ExcelProtect", food-heal debug true + worlds, leash debug
true — plus the `enderman-resistance` section at shipped
defaults), and the four data files (quest-destinations,
drops-index, tracker-catalog, seal-restore) unchanged. The
food-heal fix needs **no config change** — the live config already
carries every food-heal key.

## 3. Test + rig evidence

- **Unit suites** (merged tree, house `run-tests.sh`):
  **524 passed, 0 failed** — the full 0.3.0 suite count, unchanged.
- **Food-heal rig proof, re-run on the merged 0.3.1 jar**
  (`rig-foodheal/`, a copy of the food-heal lane's rig with only
  the jar swapped; log preserved as `merged-runtime-foodheal.log`).
  The fake-player proof (100 HP pool, out of combat, em_primis
  stand-in, per-second sampler + regain-event recorder) reproduces
  the 0.2.1 proof **exactly**:
  - Boot: "ExcelEMFixes 0.3.1 ready — modules:
    [quest-destinations, arcane-bolt, drop-guide, progress-tracker,
    food-heal, leash, seal-restore, jobs-dummy-gate,
    enderman-resistance]" (all nine), SEVERE 0.
  - Phase A (no food): health 40.0 → 85.0, Δ45.0; per-second
    samples identical to the 0.2.1 run, sample for sample.
  - Phase B (steak, budget 9.60%): Δ48.6, samples identical.
  - **Event recorder now captures the module's heals in full:**
    Phase A `CUSTOM = 45.00 HP (n=45)`, Phase B `CUSTOM = 48.60 HP
    (n=58)` — event totals equal delivered HP to the cent in both
    phases, identical to the 0.2.1 fixed run. (On the unpatched
    module the same rig recorded zero events.)
- **Standard rig regression** (merged tree's rig,
  `run-scenarios.py`, receipts in `receipts-031/`):
  - `full` boot: 38 results — the result **set is identical to
    the 0.3.0 baseline receipt**, including the same 2 pre-existing
    JobsDummyRig harness FAILs (`BOOT-gate-off`,
    `A-dummy-pays-CONTROL`) that occur identically on the
    unmodified 0.3.0 jar; SEVERE 0. (Line ordering and the
    EM-active poll-repetition count vary run to run in this
    harness; the PASS/FAIL set does not.)
  - `modoff` boot: 3/3 PASS (modules=[], module commands gone,
    /emfixes present), receipt lines identical to baseline;
    SEVERE 0.
- **Enderman rig: not re-run — its 0.3.0 results stand.** The
  enderman-resistance module is byte-identical to 0.3.0 in this
  tree (proven by the whole-tree source diff in §1: the only
  differing file is FoodHealModule.java), so the 0.3.0 two-boot
  verify (ALL PASS, receipts in
  `~/workspace/excelemfixes-030-20261122/receipts/`) carries over
  unchanged.

## 4. Draft MANIFEST entry (for the parent seat to file — NOT filed here)

```
## PENDING — ExcelEMFixes 0.3.1 (added Nov 22, 2026 — MERGED filing candidate, supersedes the staged 0.3.0 and 0.2.1 packets; file THIS one only. One version swap carrying both of tonight's fixes: (a) the enderman-resistance module (D195) — endermen in EliteMobs scope take projectile/magic damage at a 50% landed fraction instead of 0%, refused hits re-applied once through EM's own damage pipeline so the shooter is credited, melee untouched; (b) the food-heal module's heals now go through EntityRegainHealthEvent (CUSTOM), mirroring EliteMobs' own trickle, instead of silent setHealth — in-game delivery was already correct (designed ~2%/s base, ~2.5%/s with a meal budget), but the module's half was invisible to ExcelProtect's heal ledger, which misread as a stacking failure in the Nov 22 combat investigation. Built + unit-tested 524/524 + deterministic build + food-heal rig re-run on the merged jar (curves identical to the 0.2.1 proof; regain-event totals equal delivered HP to the cent) + standard rig regression identical to the 0.3.0 baseline, SEVERE 0, Nov 22, 2026.)
- Packet: ~/workspace/excelemfixes-031-merged-20261122/RECORD.md (port + diff summary §1, build §2, tests §3).
- Artifacts: ExcelEMFixes-0.3.1.jar — 198,859 B, SHA-256 bf49e51df4987809fb2aaa66101e933a3352a64cb41f3b492b849f793d3acc32 — at ~/workspace/excelemfixes-031-merged-20261122/ExcelEMFixes-0.3.1.jar (deterministic build, two builds byte-identical). Jar replaces plugins/ExcelEMFixes-0.2.0.jar (177,592 B / 733a02d190ec079b37e9017602615a2983340d98fea3d6e2d55687a94528cbe3 — rename the outgoing jar to ExcelEMFixes-0.2.0.jar.bak-excelemfixes-031 as the rollback artifact). Config: deploy/config.yml — 17,618 B, SHA-256 74341d9d17c9f426abb24e84c5786da3deea808d2a2bd45247c9025b4583efc0 — at ~/workspace/excelemfixes-031-merged-20261122/deploy/config.yml, lands as plugins/ExcelEMFixes/config.yml (back up the live config first). This is the 0.3.0 packet's deploy config unchanged (WRV deltas preserved + enderman-resistance section at shipped defaults); the food-heal fix itself needs no config change, and food-heal debug stays ON for the proof. No data files change.
- Marker: write ~/workspace/excelemfixes-031-merged-20261122/INSTALLED.txt at install.
- Notes: Backup-first + the standard two-check verified-empty gate, then restart. Priority 1 player fix (magic/ranged builds currently cannot fight enderman content at all) — install at the first verified-empty moment. Boot-verify: census carries ExcelEMFixes (0.3.1); "ExcelEMFixes 0.3.1 ready — modules: [quest-destinations, arcane-bolt, drop-guide, progress-tracker, food-heal, leash, seal-restore, jobs-dummy-gate, enderman-resistance]" (all nine); other modules' ready lines unchanged from the 0.2.0 boot; SEVERE 0. Write the install-timing ledger line with the window. Rollback: remove the 0.3.1 jar, rename the .bak-excelemfixes-031 back, restore the backed-up config, restart; player data untouched. (Module-level escape hatch: disabling modules.enderman-resistance.enabled removes that module cleanly at /emfixes reload, no restart.) SAME-PASS SURFACES: What's New (in-game ExcelWhatsNew + website) entry for the enderman change, text in the 0.3.0 packet ("Endermen are no longer immune to magic and arrows — they now resist it (half damage)..."); the food-heal fix is an internal accounting fix with no player-facing change — What's New checked-no-entry for it; website plugin-catalog row version bump to 0.3.1 rides the normal records flow; pack README already documents both changes. PROOFS OWED post-install (do not gate): P-ENDERMAN-LIVE-1 (first real Manifested Nightmare fight by a magic player — wand hits register at ~half, WANDS XP flows) and P-JESTER-OOC-HEAL-1 stage 2 per ~/workspace/food-heal-stacking-20261122/RECORD.md §5 — on its pass, food-heal debug flips off (text live-edit) and the proof closes.
```

## 5. Folder contents

- `RECORD.md` — this file.
- `ExcelEMFixes-0.3.1.jar` — the staged merged build.
- `deploy/` — the deploy config set (byte-identical to the
  0.3.0 packet's).
- `build-root/` — the full merged source tree (030 tree + the
  one-file port + version stamps) with build/test scripts; its
  `rig/` ran the standard regression with the 0.3.1 jar.
- `rig-foodheal/` — the food-heal proof rig copy that ran the
  merged jar (jar swap only).
- `merged-runtime-foodheal.log` — the preserved food-heal rig
  runtime log for the 0.3.1 run.
- `receipts-031/` — regression receipts for the merged jar plus
  the 0.3.0 baseline receipts they were compared against.
