# RECORDS RECORD — ExcelEMLeash 0.1.0

Completed: Nov 20, 2026, ~6:58 PM CST (America/Chicago), records-prep
worker R3 dispatched by board supervisor run 669. PREP ONLY — no
GitHub push, no Drive upload, no live server changes, no installs,
no panel work in this lane. Recording work is pre-approved standard
workflow (Mike, Nov 4, 2026); publication itself is a later lane.
Template followed: ~/workspace/weekendxp-records-20261120/RECORDS-RECORD.md
(ExcelWeekendXP 1.2.1 record), adapted for this plugin.

Install of record: ExcelEMLeash 0.1.0 INSTALLED Nov 20, 2026,
runner-hybrid window (~/workspace/wrv-install-queue/runs/20261120-1335-runner-hybrid/RUN-RECORD.md),
Packet B 20261120-1814-hybrid-b (12/12 legs exit 0; post-swap
verification 10/10); panel Start 18:19 CT, Done (23.437s) 18:20:01 CT;
window backup wrv-fullpush-20261120 (Manual, 8.1 GB). Segment 3
close-out ~18:33 CT: server ONLINE, whitelist OFF. See INSTALLED.txt
in the build folder.

## Publication claim line

Publication claim: wrv-plugins PR #NONE-YET (duplicate check run 2026-11-21T00:55:16Z, result NO MATCH — exit 0 for fragment 'leash-0.1.0' and exit 0 for fragment 'leash'; source: ~/workspace/board-r669/W3.md §2)

## ExcelEMLeash 0.1.0 record package — PREP ONLY (not yet published)

- PR: NONE-YET — no open/draft PR for this version exists (W3 §2).
- Intended package path (for the publishing lane): packages/excelemleash-0.1.0-20261120/
- Intended branch shape (sibling convention): record/excelemleash-0.1.0-20261120
- PR merged: NO — no PR exists yet; when created it stays OPEN DRAFT
  per house rule.
- Duplicate guard (G2): pr-exists.sh wrv-plugins leash — exit 0
  NO MATCH; leash-0.1.0 — exit 0 NO MATCH (W3, 2026-11-21T00:5xZ).
  Generic 'leash' also returned NO MATCH, so no earlier-version
  record PR collides either.
- Source of record (hash-verified this run, not guessed): build
  folder ~/workspace/emleash-build-20261120/ — `sha256sum -c
  SHA256SUMS` returned OK 3/3 this run (jar, source zip,
  INSTALL-PACKET.md); every artifact below was re-hashed by R3
  and matched the task spec, W3 §1e, INSTALLED.txt, and
  BUILD-RECORD.md exactly. Mismatches: NONE.
- Local staging retained (this prep): ~/workspace/board-r669/records/leash-0.1.0/
  (RECORDS-RECORD.md + SHA256SUMS only; no copies of the jar/source
  are made by this prep lane — the publishing lane assembles the
  package from the build folder above).

## Install evidence

- Gate passed: no ExcelEMLeash*.jar existed live before install
  (new plugin, D167) — verified in hybrid Packet A via rclone
  listing (RUN-RECORD.md Segment 2/3 notes).
- Placed: ExcelEMLeash-0.1.0.jar in plugins/ via Packet B swap leg.
- Post-swap verification (hybrid RUN-RECORD.md, 18:15–18:21 CT):
  direct pull + SHA-256 of every placed live file vs candidates —
  ExcelEMLeash-0.1.0.jar 9da74f34… ✓, part of 10/10 EXACT across
  the whole six-entry window.
- Boot: "Enabling ExcelEMLeash v0.1.0"; "ExcelEMLeash 0.1.0 ready —
  aggro-clear on, radii floor 35.0."; boot-time WARN "LEASH radii:
  EliteMobs boss registry is empty — nothing applied yet"
  (expected at enable; registry populates after EM load).
- Segment 3 live proof — radii leg PROVEN live (hybrid RUN-RECORD.md
  check 3): the 5-minute self-heal fired at 00:25:03 UTC
  (18:25:03 CST Nov 20): "LEASH radii applied: bosses seen 755,
  with leash 234, floor 35: raised 122, overrides in force 0,
  values changed 122" + exactly 122 [debug] LEASH lines, including
  "LEASH primis_feral_wolf.yml: leashRadius 20 -> 35 (floor)".
  Seen/with-leash counts exceed the packet estimate (≈252/≈221)
  because the live EM registry holds 755 boss entries; the
  operative numbers (floor 35, 122 raised, feral wolf 20→35) match.
- Boot health for the window: SEVERE 0; only the 3 known baseline
  ERROR lines; guild line "5 of 5 seals broken, guild OPEN".
- End state (Segment 3): server ONLINE, whitelist OFF.
- Rollback artifacts retained: .bak-20261120 jars/files on the live
  tree (window-wide) + window backup wrv-fullpush-20261120. Leash
  rollback per packet: remove the jar + generated
  plugins/ExcelEMLeash/ folder and restart (radii live only in
  EM's in-memory fields).

## Artifact verification (re-hashed by R3 this run)

| Artifact (in ~/workspace/emleash-build-20261120/) | Bytes | SHA-256 | Verdict |
|---|---|---|---|
| ExcelEMLeash-0.1.0.jar | 11,438 | 9da74f34ec97da8227b3531e76e4fe7e921abcdf2325742ead08cf2d29dbc90b | MATCH — task spec / W3 §1e / INSTALLED.txt / build SHA256SUMS |
| ExcelEMLeash-0.1.0-source.zip | 12,850 | bd9b7ff597aa8a1e2aa1b49b13cfb9c7fc13574aa9b07f1c0d5a859272cc25d1 | MATCH — W3 §1e / build SHA256SUMS |
| INSTALLED.txt | 1,257 | dc9170c1c46c51f00d4491b77eed7a0b2b893220140559f3675513733ee4318b | MATCH — task spec / W3 §1e |
| BUILD-RECORD.md | 16,350 | a9e250089b26739a5f09206db0a2af78025035543bfa5048718a3c5814cd8373 | MATCH — task spec / W3 §1e |
| INSTALL-PACKET.md | 4,746 | a54e00e8360e0b203ca7f78aaca22af1c82951137d25394fe89a73ebcc88f742 | MATCH — W3 §1e / build SHA256SUMS |

- Build-folder SHA256SUMS: 3/3 OK locally this run.
- Mismatches: NONE.

## Build / change summary

- Why (D167): EliteMobs' regional-boss leash returns a boss home
  and heals it, but never clears its target, so it charges straight
  back out and the fight loops; EM exposes no drop-aggro config.
  Separately, Primis leash radii are often fight-breaking tight
  (50 bosses at 20, tail down to 2). Full investigation:
  ~/workspace/em-leash-investigation-20261120/RECORD.md.
- Leg A — aggro-clear: listens for EM's WanderBackToPointEndEvent
  (native leash-return completion); on the event, for regional
  bosses only, clears Mob.setTarget(null) + EliteEntity.clearDamagers()
  (EM's own reset bundle: damagers, aggro/threat, skill contributions,
  forced target). Deliberately NOT cleared: health (EM full-heals by
  design), inCombat flag, spawn point, powers, follow range.
  Scope guard: non-regional / unresolved entities are a no-op.
  Documented limit: EM's fallback spirit-walk path does not fire
  this event; that path is not active on this platform.
- Leg B — radii management (RUNTIME route, preference 1, P0-proven):
  mutates EM's loaded CustomBossesConfigFields objects in memory via
  setLeashRadius(double) (bare putfield, no file persistence; the
  persisting runtimeSetLeashRadius is deliberately NOT used).
  Effective radius = override if set, else max(file radius, floor 35);
  applied only to bosses that already have a leash radius; values
  above the floor are never lowered except by explicit override.
  Re-applies 20 ticks after enable and on a 5-minute self-heal, so
  EM reloads / content-pack updates cannot silently undo it. No boss
  file is edited — by hand or by the plugin. The managed-file route
  was NOT needed and is not used.
- Config (default, as installed): aggro-clear enabled, radii
  enabled, minimum-radius 35, overrides {}, debug: true (house
  precedent — flip off after the aggro-clear live proof; see
  Proofs owed).
- Tests / build: unit tests 30/30 (RadiusPolicy + AggroClearPolicy);
  double-build byte-identical (both 11,438 B / 9da74f34…, cmp
  identical, and identical to the on-disk jar from the original
  build). plugin.yml: name ExcelEMLeash, version 0.1.0,
  softdepend [EliteMobs]; pinned EM jar for P0/rig: EliteMobs
  10.9.14 (file named 10.9.10), 14,563,211 B /
  4a6bc4a74e382942e39961e9a25a231deb46cea34379d40bc63ae9a9328c44bd.
- Rig (Paper 26.2-123 + real pinned EM): 8 PASS / 1 FAIL, SEVERE 0.
  Leg B proven end-to-end against EM's real registry objects
  (floor, override up/down, no-leash untouched, file untouched,
  reload re-apply). The 1 FAIL (A2 regional aggro-clear end-to-end)
  is a rig limit, not a plugin defect: a full EM regional boss
  cannot spawn on the scratch rig (FreeMinecraftModels absent —
  CustomModel.modelPlugin null; EM's tracker also refuses the
  fallback binding), so the shipped handler correctly logged
  SKIP_NO_ELITE and touched nothing. Leg A's decision core is
  unit-tested (CLEAR branch) and its skip paths are rig-proven;
  the live drag proof below is the end-to-end Leg A test (same
  posture as sibling builds: mirror proof accepted, player-flow
  proof after release).

## Drive deposits — OWED (not done by this prep lane)

- Status: OWED-UNVERIFIED — PREP ONLY lane made no Drive uploads.
- Owed for the publishing lane (sibling convention): the jar to
  Build files and the source zip (+ SOURCE.md if the lane follows
  the 1.2.1 pattern) to Source, each verified by Drive-reported
  size + MD5 EXACT and download-back MD5 + SHA-256 EXACT.
  No Drive IDs exist yet for 0.1.0; none are claimed here.

## Surfaces state (same-pass rule; only what evidence proves)

- Guide (knowledge + intents): OWED-UNVERIFIED — no evidence in
  the build folder, hybrid RUN-RECORD.md, or W3 that a Guide leg
  landed. Note: Guide development is hibernated per Mike's
  Nov 16, 2026 ruling; whether a Guide leg is owed at all is for
  the supervisor chain, but no landed Guide surface is proven here.
- Website (plugin list / detail): OWED-UNVERIFIED — no evidence
  of a website change for Leash 0.1.0 in the evidence set.
- What's New — website section: OWED-UNVERIFIED.
- What's New — in-game ExcelWhatsNew changelog: OWED-UNVERIFIED.
  (Packet-drafted line exists in INSTALL-PACKET.md: "Bosses that
  get dragged too far from home now stay home after they return —
  no more teleport-loop fights — and the tightest boss leashes got
  some breathing room." Drafted ≠ placed.)
- Rotation tips: OWED-UNVERIFIED — no evidence of a tips check
  for this change in the evidence set.
- Menus (ExcelMenu + ExcelBedrockMenu): OWED-UNVERIFIED — no
  evidence of a menus check in the evidence set; the plugin adds
  no player command/menu of its own per BUILD-RECORD.md/plugin.yml
  in the evidence read, but a checked-and-not-applicable verdict
  is itself unproven here, so it is recorded as owed, not assumed.

## Proofs owed

- Aggro-clear end-to-end drag proof (player lane, D154 pool;
  OWED): in Primis, drag the feral wolf (primis_feral_wolf.yml,
  stock radius 20, floored to 35) past 35 blocks; the leash return
  completes and the boss STAYS home (healed, as EM designs) and
  does NOT charge back out; expected debug evidence:
  "[ExcelEMLeash] [debug] LEASH aggro cleared: <boss> stays home
  (hardObjective=…)". Also confirm no return starts at 10/20
  blocks anymore (Leg B floor in play).
- Flip debug off (OWED, gated on the proof above): set debug:
  false in plugins/ExcelEMLeash/config.yml (live-edit + restart
  or reload of ExcelEMLeash only), per INSTALLED.txt + packet.
- Regression spot-check (OWED, rides the same proof session): a
  dungeon/instance boss and a natural elite behave exactly as
  before (Leg A never touches them).
- Records publication (OWED): wrv-plugins record PR (OPEN DRAFT)
  + Drive deposits — see sections above. PR = NONE-YET.

## Sums verification summary

- Build-folder SHA256SUMS: 3/3 OK locally this run (R3 re-hash).
- Task-spec hashes vs disk: 3/3 MATCH (jar, INSTALLED.txt,
  BUILD-RECORD.md — the three hashes named in the R3 task).
- W3 §1e hashes vs disk: 5/5 MATCH (table above).
- This folder's SHA256SUMS: covers this RECORDS-RECORD.md plus
  every referenced artifact in the table above (hashes as verified
  in their source folder; see SHA256SUMS in this folder).
- Publication: NONE-YET — duplicate check NO MATCH (above).

## Notes / blockers

- No mismatches, no blockers in the prep lane. The only install-time
  surprise is already documented in the evidence: live registry
  counts (755 seen / 234 leashed) exceed the packet's rig-based
  estimate (≈252 / ≈221); the floor, raised count, and proof-boss
  line all matched, and the hybrid record carries the explanation.
- debug=true is intentional at install (house precedent) and is
  NOT a defect; its flip is an owed leg gated on the drag proof.
