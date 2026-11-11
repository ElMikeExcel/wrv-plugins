# ExcelAimAssist 0.1.5 — SOURCE record

Excel — Wink Reyes Valley. Authors: Iris / Mike. November 10, 2026.
Commission: the console-admin coverage audit (Nov 9, 2026,
`~/workspace/console-admin-audit-20261109/AUDIT.md` §4) under
Mike's Nov 3, 2026 console-admin rule. The audit recommended
"0.1.4"; that number was already taken by the live menu-toggle
build, so the packet is re-based to 0.1.5 per the supplement
in the audit folder.

## Provenance

- Project tree: `~/workspace/aimassist-015-build/ExcelAimAssist/`
  (plugin.yml version 0.1.5, main
  com.winkreyes.excelaimassist.ExcelAimAssist,
  api-version '1.13').
- Base: the 0.1.4 tree
  `~/workspace/aimassist-toggle/work/aimassist-0.1.4/`
  (the newest authoritative tree on disk; its jar is the live
  jar of record). Shipped 0.1.4 jar SHA-256
  `7d040d98fc063418b3635c676e8a6f3db5adf093a4e99dec28a8f1c1ac1e6a44`
  (19,401 B) re-verified on disk Nov 10, 2026; the 0.1.4 folder
  is untouched by this build, and the copied tree was verified
  byte-identical before the delta was applied.
- Built by `build.sh` (the house deterministic recipe from
  0.1.4: manual javac, JDK 25 at `~/workspace/.jdk25`,
  `--release 21`, house Paper-API + kyori 4.20 classpath,
  PlaceholderAPI 2.11.6 compile-time only, jar entry timestamps
  pinned, sorted entry list). Runtime target is the live Paper
  server; every API used is long-stable Bukkit API.
- Ownership: all code is ours (Iris / Mike lineage). NO runtime
  dependencies beyond the 0.1.4 softdepend set (PlaceholderAPI,
  still softdepend and reached only when present); no
  third-party code is included in the jar.

## What 0.1.5 changes vs 0.1.4 (the whole delta)

CONSOLE ADMIN. The audit found 0.1.3/0.1.4 a FAIL: /aimassist
was self-only and player-gated, so the console could neither
inspect nor fix another player's toggle. In short:

- NEW `AdminCommands` (core, Bukkit-free): the gate decision
  (console explicitly allowed; any other sender needs
  `excelaimassist.admin`), the set-value parser, and the exact
  reply texts. Status formats the player's real ToggleStore
  state — effective state plus the explicit/default
  distinction — and mutates nothing; set writes through
  `ToggleStore.set`, the same call the self-service on/off
  forms make.
- `ExcelAimAssist`: an `admin` dispatch in `onCommand` ahead
  of the player-only gate; `handleAdmin` checks the gate,
  resolves the target (online name first, then raw UUID — an
  offline player is addressable by UUID only; no name→UUID
  record is invented), persists after a set via the existing
  saveToggles(), and logs each set. The self-service forms
  below the branch are byte-for-byte the 0.1.4 code.
- `plugin.yml`: version 0.1.5, usage/description name the
  admin forms, new `excelaimassist.admin` (default op). The
  `excelaimassist.toggle` node is unchanged and there is NO
  command-level permission gate (the self forms stay open).
- Tests: new `AdminCommandsTest` (44 checks) + TestMain
  registration; build.sh / run-tests.sh version knobs.
  NOTHING else: config.yml is byte-identical to 0.1.4 (in the
  tree and inside the jar), and AimMath / TargetPicker /
  ToggleStore / the PlaceholderAPI expansion are byte-for-byte
  the 0.1.4 sources and classes.
- Explicitly NOT built (outside the audit's SMALL sizing):
  any aim-behaviour or config change, any menu/placeholder
  change, and any offline name→UUID lookup.

## Artifacts

- ExcelAimAssist-0.1.5.jar — 22,745 bytes
  SHA-256 9e5d926b74f0ddaba9b0d9adaaacf9cd41ace56a15a97dbe44a7a28b1fbc7a50
  Deterministic double-build byte-identical. (No in-jar
  build-ID fingerprint exists in this lineage; version + hash
  identify it.)
- ExcelAimAssist-0.1.5-source.zip — 36,076 bytes
  SHA-256 278ce4e8ed37cf4803418be8c86007616e4f27cd2f2b7b72c56bd99ac196afd4
  (the whole ExcelAimAssist/ tree; timestamps pinned
  2026-11-10.)

## Verification summary

Desk 234/234 (190 carried 0.1.4 checks unchanged + 44 new
AdminCommands checks). Rig on Paper 26.3-231 with the 0.1.5
jar + the live PlaceholderAPI 2.12.3 and seeded toggles.txt
(rig-0.1.5 in the build folder): clean 0.1.5 enable with the
aimassist expansion registered, zero SEVERE, clean stop,
19/19 behavior checks typed on the real console stdin —
status lines for explicit OFF / explicit ON / default-ON
records (verbatim), both bad-name rejections, both sets with
was-states + CONSOLE log lines, malformed and bare-admin
usage forms, the on-disk proof that status reads changed
nothing and the final file holds exactly the two set
outcomes, and the console self-form gate line byte-for-byte
the 0.1.4 one. Limitations (targets addressed by UUID — no
player joined; the name path is the house getPlayerExact
shape and is smoked live at install; the non-op denial is
harness-pinned, not rig-observed) are stated in
rig-0.1.5/RIG-RESULTS.md.
NOT installed — built + staged only.
