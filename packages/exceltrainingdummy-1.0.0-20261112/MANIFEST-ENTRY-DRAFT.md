# MANIFEST ENTRY DRAFT — ExcelTrainingDummy 1.0.0 (for the board supervisor to file)

Drafted by the desk build lane (Nov 12, 2026). The supervisor
verifies and files this as a PENDING entry in
`~/workspace/wrv-install-queue/MANIFEST.md`. The build lane did NOT
edit the MANIFEST, the parent-backlog, or board state, and ran NO
live pulls — every live-state claim below is marked GATE-VERIFY
for the filing/install gates to check fresh.

---

## PENDING — ExcelTrainingDummy 1.0.0 (NEW plugin: WoW Dome / GW2-style training dummies — spawn menu 1/3/5, Practice/Dungeon/Seal tiers + Endless, 25% DPS checkpoints + final report, boss-armor display-scale preset, hazardous mode with 1 HP stop + Lethal toggle, execute preset; virtual pool, clean by construction) — packet: this draft + ~/workspace/training-dummy-build-20261112/build-1.0.0/BUILD-RECORD.md

- **Packet:** build record
  `~/workspace/training-dummy-build-20261112/build-1.0.0/BUILD-RECORD.md`;
  design of record `~/workspace/training-dummy-20261110/DESIGN.md`;
  rig proof `~/workspace/training-dummy-build-20261112/build-1.0.0/rig-1.0.0/RIG-RESULTS.md`
  (Paper 26.3 desk rig, 11/11, SEVERE 0); unit tests 67/67
  (`TEST-RESULTS.txt` in the build folder).
- **Artifacts (jar + generated config, one window):**
  - `~/workspace/training-dummy-build-20261112/build-1.0.0/ExcelTrainingDummy-1.0.0.jar` —
    **49,376 B** — SHA-256
    `b46250ed7a408a447749b8f62e10c432c235c9050a1e4dcff06a8138c516c098`
    — build `EXC-TDUMMY-1.0.0-WRV-20261112-01-178829`
    (double-build byte-identical; fingerprint gate PASS).
  - No config swap: config.yml ships inside the jar and is written
    by saveDefaultConfig on first enable. No data files, no merges.
- **Live-premise claims — ALL GATE-VERIFY (no live pull was run by
  the build lane):**
  - GATE-VERIFY: no `ExcelTrainingDummy*.jar` exists in the live
    plugins folder (new plugin — nothing to swap, nothing to
    conflict; any existing same-name jar STOPs the window).
  - GATE-VERIFY: live server runs Paper 26.3 and ExcelMenu +
    ExcelBedrockMenu at their current live versions (softdepend
    coexistence was rig-proven against Geyser/floodgate only).
  - GATE-VERIFY: live ExcelClaims answers the GriefPrevention API
    the ClaimGate reflects on (plugin name "GriefPrevention",
    DataStore.getClaimAt) — desk-verified against fork source,
    not live-proven; if the check fails, spawns fail CLOSED by
    design (wilderness spawns also deny) and the window should
    surface that before players hit it.
  - GATE-VERIFY: compass server_menu layout headroom for the new
    entry (below) — count free slots in the live ExcelMenu +
    ExcelBedrockMenu menu configs first (the Nov 7 settings-group
    overflow lesson); if the group is short, split the group as
    the design's two-doors section directs.
- **Marker path (hand window / failsafe per queue rules):**
  `~/workspace/wrv-install-queue/WINDOW-IN-PROGRESS.txt` — create
  at window start, delete at finish; re-check foreign markers
  before Shut Down and before Start.
- **Gates:** BACKUP FIRST + cap-at-6 (name prunees; never the Daily
  newest or the two newest manuals); emptiness `list` = 0 twice.
- **Boot-verify lines (all must appear):**
  - `Enabling ExcelTrainingDummy v1.0.0`;
  - enable line `ExcelTrainingDummy 1.0.0 enabled (build EXC-TDUMMY-1.0.0-WRV-20261112-01-178829); tiers Practice 1,000 / Dungeon 5,000 / Seal 20,000; caps 5 per player, 25 server.`;
  - SEVERE 0 attributable; no ClaimGate "claims API unreadable"
    warning (its presence = claim checks failing closed — surface
    it, do not call the install clean);
  - console `dummy version` answers with build
    `EXC-TDUMMY-1.0.0-WRV-20261112-01-178829`.
- **Same-pass surfaces (install window, per house rule):**
  - **Compass menu entry (DESIGN two-doors §1):** add a "Training
    Dummy" entry to the ExcelMenu server menu AND the
    ExcelBedrockMenu equivalent, action = run `/dummy` (the same
    contract as the Details Settings / Guide entries), placed with
    the settings/utility group; headroom GATE-VERIFY above governs
    placement/splitting. Both menus or the surface is owed, named.
  - **Guide:** knowledge + intent for training dummies (spawn,
    tiers, hazardous, execute, tap-to-edit) — live-edit exemption
    rules apply to the text placement; the jar install stays gated.
  - **What's New BOTH surfaces:** website section + in-game
    ExcelWhatsNew changelog entry.
  - **Tips check:** broadcaster tip candidate — "Practice your
    rotation: /dummy spawns a training dummy — checkpoints print
    your DPS as you go." (file or explicitly skip with the reason).
  - **Menus check:** ExcelMenu + ExcelBedrockMenu verified after
    the entry lands (open both, entry present, opens /dummy).
- **Player proof owed after install (the deciding evidence):**
  spawn from the compass entry on Java + Bedrock; Bedrock forms
  (spawn + per-dummy settings); tap a dummy with a compass (owner
  settings, non-owner readout); a full Practice kill with
  checkpoint prints + final report read in chat; hazardous Light
  pulse stopping at 1 HP; endless lap reset; spawn denied in
  someone else's claim, allowed in wilderness + own claim.
- **Rollback:** remove `ExcelTrainingDummy-1.0.0.jar` +
  `plugins/ExcelTrainingDummy/` (config only — the plugin keeps no
  player data; live dummies are in-memory and vanish on stop),
  revert the two menu-config entries + text surfaces, restart.
- **Records:** package staged at
  `~/workspace/records-catchup-20261103/packages/exceltrainingdummy-1.0.0-20261112/`
  (publication claim line belongs to the supervisor's close-out —
  the build lane published nothing).
