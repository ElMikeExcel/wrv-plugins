# ExcelBuildMode 1.1 — construction-only pilot candidate

Built October 14, 2026 by Argus from Moon's frozen ExcelBuildMode 1.0 checkpoint
(recovery core reused unchanged). Implements the approved supported-actions
specification (ExcelBuildMode-Supported-Actions-Spec.md, decisions recorded
October 14, 2026). NOT INSTALLED. No live server was touched.

## What changed vs the frozen 1.0 helper

1. **Pilot profile (`pilot-construction-only: true`, the shipped default).**
   The SessionCoordinator is constructed without a RewardLedger, so Build entry
   requires only: private allowlist UUID, `excelbuildmode.use`, and the existing
   clean-state checks (Survival, solid ground, no vehicle, empty cursor and
   crafting grid, verified recovery storage and cohort data). The reward
   framework code is untouched and its registration state is not faked; with
   `pilot-construction-only: false` the strict 1.0 admission gate applies
   exactly as before (proven by PilotGateTests).
2. **Session permission attachment, always created on entry.** Contents come
   from `BuildPolicy.sessionPermissions(worldEdit)`: `jobs.use = false` always,
   plus the fixed 16 WorldEdit nodes only when temporary WorldEdit is enabled.
   Removed on every exit path (normal exit, death, quit, plugin disable,
   landing hold, recovery). The WorldEdit effectiveness checks from 1.0 are
   kept; if the `jobs.use` denial is not effective (another permission source
   wins), the plugin logs a warning instead of guessing.
3. **Early input gates at LOWEST priority, building players only** (new in
   `ExcelBuildMode.java`), so denial lands before Jobs, mcMMO and EliteMobs
   handlers run:
   - melee and projectile damage dealt by the builder (the 1.0 HIGHEST guard
     remains as backstop);
   - projectile launches;
   - fishing (cast and reel);
   - `PlayerInteractAtEntityEvent` (the EliteMobs NPC event the 1.0 helper
     missed) alongside the existing `PlayerInteractEntityEvent` guard;
   - swap-hand input (the EliteMobs class-ability trigger; tradeoff: no
     offhand swap while building);
   - right-click harvesting/stripping via `BuildPolicy.blocksHarvestInteraction`:
     composter, sweet berry bush, beehive, bee nest, campfire, soul campfire
     with anything in hand, and an axe used on logs, stems, wood, hyphae or
     bamboo blocks. All other placement and breaking stays open, per the spec.
4. **plugin.yml version is 1.1**; build output is `target/ExcelBuildMode-1.1.jar`.

## Enforced by code vs operator checklist

- Code: entry conditions, snapshot-first recovery, session permissions, the
  gates above, the 1.0 container/shop/drop/pickup/command guards.
- Operator checklist at install time (not code): the builder finishes combat
  and unsettled EliteMobs party loot and leaves any Elite party while still in
  Survival before entering; the plugin cannot see EliteMobs party state
  without an integration that does not exist yet. Party commands are denied
  during the session, so he cannot rejoin mid-build.
- Known remaining gaps (from Moon's audits, unchanged by this candidate):
  EliteMobs action events that start from block breaking/tilling cannot be
  gated without blocking construction itself; mcMMO party proximity counting
  includes nearby members regardless of mode, which is why the entry
  checklist (not code) covers party state; a plugin delivering an item
  directly into the builder's inventory mid-session can still lose it at
  restore, and a delayed Survival XP delivery can still be discarded —
  both disclosed in the spec and accepted by Mike on October 14, 2026.

## Test results (October 14, 2026)

All suites run by build.sh against the official Paper 26.2 build 123 library
set, Java 25:

- CoreTests: PASS 5,709 assertions (matches Moon's recorded 1.0 count)
- RewardTests: PASS 5,283 assertions (matches)
- RecoverySafetyTests: PASS 84 assertions (matches)
- PlayerDataProofTests: PASS 145 assertions (matches)
- PilotGateTests: PASS 53 assertions (new: WorldEdit set shape, session
  permission shape with WorldEdit on and off, the harvest/stripping matrix,
  command allowlist spot checks, pilot admission with zero reward providers,
  strict admission still refusing entry)

Artifact: `ExcelBuildMode-1.1.jar`
SHA-256 (first build): `cf1b2a1b92895ca16a3432edd02120ea88ba91f60a7fc7673dc644a86250ec06`
Clean-rebuild reproduction: CONFIRMED October 14, 2026 — a second full
build.sh run produced the identical SHA-256.

## Statuses (kept separate)

- Built: yes
- Automated tests passed: yes (above)
- Test server passed: boot smoke test only — the jar loads and enables on a
  bare Paper 26.2 build 123 / Java 25 server (October 14, 2026), prints the
  pilot banner, and fails closed exactly as designed because no private
  cohort, eligibility, or recovery storage is provisioned there. No client
  gameplay test has run.
- Installed live: no
- Java player check passed: no (requires install + Marvin)
- Bedrock-mobile check: not applicable (Marvin is Java; the menu is the
  existing inventory menu)

## Private install steps (later, with Mike's approval and a restart window)

1. Install official WorldEdit 7.4.5; merge `proposals/worldedit-config-fragment.yml`
   into its generated config (5,000-block cap, 10 history entries).
2. Provision the recovery directories, cohort replica files, and review-hold
   storage the plugin requires (it refuses entry without them, by design).
3. Private config: add Marvin's real UUID to `eligible-builder-uuids`, set
   `temporary-worldedit: true`, keep `pilot-construction-only: true`.
4. Fresh console zero-player check, panel shutdown, install exactly one
   ExcelBuildMode jar, verify its hash on the server, start, run the spec's
   acceptance tests with Marvin on Java.
