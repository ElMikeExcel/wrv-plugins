# RECORDS RECORD — D164 Shared-Loot Fix (config, 5 files, NO jar)

Completed: Nov 20, 2026, ~6:58 PM CST (America/Chicago), records-prep
worker R3 dispatched by board supervisor run 669. PREP ONLY — no
GitHub push, no Drive upload, no live server changes, no installs,
no panel work in this lane. Recording work is pre-approved standard
workflow (Mike, Nov 4, 2026); publication itself is a later lane.
Template followed: ~/workspace/weekendxp-records-20261120/RECORDS-RECORD.md
(ExcelWeekendXP 1.2.1 record), adapted for a config-only fix with
no jar and no source zip.

Install of record: D164 shared-loot fix INSTALLED Nov 20, 2026,
runner-hybrid window (~/workspace/wrv-install-queue/runs/20261120-1335-runner-hybrid/RUN-RECORD.md),
Packet B 20261120-1814-hybrid-b (12/12 legs exit 0; post-swap
verification 10/10); panel Start 18:19 CT, Done (23.437s) 18:20:01 CT;
window backup wrv-fullpush-20261120 (Manual, 8.1 GB). Segment 3
close-out ~18:33 CT: server ONLINE, whitelist OFF. See INSTALLED.txt
in the fix folder. (The D172 Wolf Den lockout reset landed in the
same window's Segment 2; it is a separate item and is not part of
this record except where the retest depends on it.)

## Publication claim line

Publication claim: wrv-plugins PR #NONE-YET (duplicate check run 2026-11-21T00:55:16Z, result NO MATCH — exit 0 for fragments 'sharedloot' and 'shared-loot'; source: ~/workspace/board-r669/W3.md §2)

## D164 shared-loot record package — PREP ONLY (not yet published)

- PR: NONE-YET — no open/draft PR for D164 exists (W3 §2).
- Intended package path (for the publishing lane): packages/d164-sharedloot-20261120/
  (config-fix package: INSTALLED.txt, RECORD.md, INSTALL-PACKET.md,
  edited/ ×5, originals/ ×5, diffs/ ×5 — no jar, by design).
- PR merged: NO — no PR exists yet; when created it stays OPEN DRAFT
  per house rule.
- Duplicate guard (G2): pr-exists.sh wrv-plugins sharedloot —
  exit 0 NO MATCH; shared-loot — exit 0 NO MATCH (W3,
  2026-11-21T00:5xZ).
- Source of record (hash-verified this run, not guessed): fix
  folder ~/workspace/sharedloot-fix-20261120/ — every artifact
  below was re-hashed by R3 and matched the task spec, W3 §1f,
  INSTALLED.txt, RECORD.md, and INSTALL-PACKET.md exactly.
  Mismatches: NONE.
- Local staging retained (this prep): ~/workspace/board-r669/records/d164-sharedloot/
  (RECORDS-RECORD.md + SHA256SUMS only; no copies of the edited/
  originals/ files are made by this prep lane — the publishing
  lane assembles the package from the fix folder above).

## Install evidence

- Live premise (fresh pulls, 2026-11-20 ~11:07 AM CT, Homebase
  rclone from shockbyte: EliteMobs custombosses/primis/ +
  ExcelSharedDungeonLoot config): all four boss files confirmed
  carrying hyper_loot before edit (feral alpha wolf line 23,
  gladius champion p2 line 21, mountain pass guardian p1 line 15,
  mountain pass guardian p2 line 13); no bonus_loot in any of the
  four. Original (pre-edit) hashes are in the change table below
  and in RECORD.md / INSTALL-PACKET.md.
- Placed (each live file first backed up to .bak-20261120):
  the 4 edited boss files + plugins/ExcelSharedDungeonLoot/config.yml
  (debug false→true), via Packet B text-place legs.
- Post-swap verification (hybrid RUN-RECORD.md, 18:15–18:21 CT):
  direct pull + SHA-256 of every placed live file vs candidates —
  alpha b155d316… ✓; gladius e696e4ce… ✓; p1 d7a07609… ✓;
  p2 3c6d8d94… ✓; config 3bd626d5… ✓ — part of 10/10 EXACT across
  the whole six-entry window.
- Apply path: ONE server restart (required — ExcelSharedDungeonLoot
  has no reload command and reads debug once in onEnable(); the
  restart also re-reads the boss files; /em reload alone would
  have covered the boss files but left debug off). Only NEW boss
  spawns/instances pick up the power removal.
- Boot: ExcelSharedDungeonLoot "enabled (enabled=true, debug=true)";
  SEVERE 0; no new WARN/ERROR naming the four bosses. Guild line
  "5 of 5 seals broken, guild OPEN" (window-wide).
- End state (Segment 3): server ONLINE, whitelist OFF.
- Rollback: restore any file from its .bak-20261120 copy and
  restart; the change is five text files, fully reversible.

## Artifact verification (re-hashed by R3 this run)

Fix-folder documents:

| Artifact (in ~/workspace/sharedloot-fix-20261120/) | Bytes | SHA-256 | Verdict |
|---|---|---|---|
| INSTALLED.txt | 1,563 | 54e57ff56f16dd0ae380eecccfd573e9dcd7dcb7f002d5c02b1505b2ed6e715f | MATCH — task spec / W3 §1f |
| RECORD.md (packet prep record) | 5,881 | 181e387c980ce006339ed1982ec3f077eb762eb2f7d1938b1f69b18e3846a90e | MATCH — task spec / W3 §1f |
| INSTALL-PACKET.md | 5,945 | 8a208fcfa6d9e1ae002ae1fdaf636d5371e3f54ce8eccdcdc28a55ca7df5d6af | MATCH — task spec / W3 §1f |

Edited files (the install payload — edited/ in the fix folder;
these are the hashes placed live and post-swap-verified):

| File (edited/) | Bytes | SHA-256 | Verdict |
|---|---|---|---|
| primis_feral_alpha_wolf.yml | 2,502 | b155d316ac5e28967749efbd63f53d856b5309bee00ccbabd3086a789be2f881 | MATCH — W3 §1f / INSTALLED.txt |
| primis_gladius_champion_p2.yml | 2,027 | e696e4ce79d07eb4aa2bfc3bbecbb41733babeeda68e04e1a12f06ce1de07a21 | MATCH — W3 §1f / INSTALLED.txt |
| primis_mountain_pass_guardian_p1.yml | 3,542 | d7a0760930828a9a7a24591a73fa07419eef1cd6473d1b81c4aaa5362d803772 | MATCH — W3 §1f / INSTALLED.txt |
| primis_mountain_pass_guardian_p2.yml | 10,628 | 3c6d8d94f5eff2ecc5c2d9e64b349c8d04f834467548373f163bf2ae35f97658 | MATCH — W3 §1f / INSTALLED.txt |
| sharedloot-config.yml | 386 | 3bd626d5cc6eba075c55519146fe2f9dc791309017aca2d2afb1d879b4c7f380 | MATCH — W3 §1f / INSTALLED.txt |

Originals (pre-edit live copies — originals/ in the fix folder,
for the record package; re-hashed by R3, all MATCH RECORD.md):

| File (originals/) | Bytes | SHA-256 |
|---|---|---|
| primis_feral_alpha_wolf.yml | 2,519 | b6b7c78f496f0f516c742064007d736408966415189b3b0a4813f1794404e5fe |
| primis_gladius_champion_p2.yml | 2,044 | 438a7162e81d73fdd9a0555e1c90040c8420f0113b3816f57e98e4fe8c12ac22 |
| primis_mountain_pass_guardian_p1.yml | 3,559 | 6fbe915546072cbd2f0c97d62113240198d13b07343e85467d0fea2adc7f6bce |
| primis_mountain_pass_guardian_p2.yml | 10,645 | b0b485d91ab8746c1163c964dad441d3999f275586f82dea7535119534ccb436 |
| sharedloot-config.yml | 387 | b6c704c46505cfe40385f23fb99e00775933b3e82dfdb6e8ec7af73959062eb3 |

- Jar: count 0 — config-only install, by design. No jar, no source zip.
- Diffs: diffs/ ×5 present (one per file), each a single-hunk
  deletion/change proving exactly the stated edit (see summary).
- Mismatches: NONE.

## Change summary

- Background (D164 ruling): ExcelSharedDungeonLoot works; the
  Wolf Den test "failure" was EliteMobs' Lua power hyper_loot on
  dungeon bosses — on death it spawns physical, owner-locked loot
  showers per damager over a 10% damage gate, bypassing the shared
  loot path (loot table → vote table → plugin) entirely.
  Investigation of record: ~/workspace/sharedloot-investigation-20261120/RECORD.md.
- Boss files (×4): remove ONLY the single power line
  "- hyper_loot.yml" (17 bytes) from the powers list. Each edited
  file is exactly 17 B smaller than its original; unified diffs
  show a single deletion hunk per file, no other change; edited
  files re-parsed as YAML with zero remaining hyper_loot
  occurrences. The bosses' loot tables already carry their full
  class-loot lists, so signature gear still drops — now shared.
- Plugin config (×1): plugins/ExcelSharedDungeonLoot/config.yml —
  exactly one line changed, debug: false → debug: true (387 B →
  386 B), so every stand-down reason is visible in the log during
  the retest. This flip is TEMPORARY by design: debug stays true
  until the Wolf Den pair retest passes, then flips back to false
  (owed leg — see Proofs owed).
- Standing screening rule (from the packet, carried into the
  record): every future dungeon content import is checked for
  loot-generating Lua powers (hyper_loot, bonus_loot) before
  install. The 12 other live boss files carrying these powers are
  overworld event bosses — outside D139/D164 scope, untouched.

## Drive deposits — OWED / NOT APPLICABLE AS JAR DEPOSIT (prep lane)

- Status: OWED-UNVERIFIED — PREP ONLY lane made no Drive uploads.
- There is no jar or source zip to deposit for D164 (config-only).
  Whether the publishing lane deposits the edited/originals set to
  Drive alongside the wrv-plugins record PR is for that lane; no
  Drive IDs exist for D164 and none are claimed here.

## Surfaces state (same-pass rule; only what evidence proves)

- Guide (knowledge + intents): OWED-UNVERIFIED — no evidence in
  the fix folder, hybrid RUN-RECORD.md, or W3 that a Guide leg
  landed. Note: Guide development is hibernated per Mike's
  Nov 16, 2026 ruling; whether a Guide leg is owed at all is for
  the supervisor chain, but no landed Guide surface is proven here.
- Website: OWED-UNVERIFIED — the packet names What's New / website
  copy as riding the normal same-pass surfaces at install time,
  but no evidence of a landed website change is in the evidence set.
- What's New — website section: OWED-UNVERIFIED.
- What's New — in-game ExcelWhatsNew changelog: OWED-UNVERIFIED.
- Rotation tips: OWED-UNVERIFIED — no evidence of a tips check
  for this change in the evidence set.
- Menus (ExcelMenu + ExcelBedrockMenu): OWED-UNVERIFIED — no
  evidence of a menus check in the evidence set; the fix adds no
  command or menu of its own, but a checked-and-not-applicable
  verdict is itself unproven here, so it is recorded as owed,
  not assumed.
- Player-facing effect (for the surfaces lane): dungeon boss loot
  in the Primis dungeons now shares to the whole eligible party
  instead of showering per-damager floor loot.

## Proofs owed

- Wolf Den pair retest (player lane; OWED): Marvin + Jester pair,
  fresh Wolf Den instance (their D172 loot-lockout reset for this
  boss landed in the same window's Segment 2), confirming ALL of:
  (1) NO physical loot shower on the floor from the Feral Alpha
  Wolf; (2) plugin INFO line per kill reads "Shared N item(s) x 2
  player(s) = K clone(s)" with identical shared item(s) delivered
  into BOTH players' inventories and [debug] snapshot/intercept
  lines present; (3) /sharedloot status counters (snapshots /
  intercepts / suppressed / delivered) increment per kill versus
  the pre-retest read; (4) both testers confirm receipt.
- Flip debug off (OWED LEG, gated on a passing retest): D164 left
  ExcelSharedDungeonLoot debug=true deliberately until the Wolf
  Den pair retest passes. After a pass, a follow-up one-line text
  edit flips debug back to false in
  plugins/ExcelSharedDungeonLoot/config.yml (takes effect at the
  next restart; no restart urgency). Until then debug=true is the
  correct live state, NOT a defect.
- Records publication (OWED): wrv-plugins record PR (OPEN DRAFT) —
  see section above. PR = NONE-YET.

## Sums verification summary

- Task-spec hashes vs disk: 3/3 MATCH (INSTALLED.txt, RECORD.md,
  INSTALL-PACKET.md — the three hashes named in the R3 task).
- W3 §1f hashes vs disk: 8/8 MATCH (3 documents + 5 edited files).
- INSTALLED.txt placed hashes vs edited/ on disk: 5/5 MATCH.
- Originals vs RECORD.md live-premise table: 5/5 MATCH.
- This folder's SHA256SUMS: covers this RECORDS-RECORD.md plus
  every referenced artifact in the tables above (documents +
  edited payload + originals; hashes as verified in their source
  folder; see SHA256SUMS in this folder).
- Publication: NONE-YET — duplicate check NO MATCH (above).

## Notes / blockers

- No mismatches, no blockers in the prep lane.
- debug=true on ExcelSharedDungeonLoot is intentional and
  retest-gated (see Proofs owed) — the same pattern as Leash and
  FoodHeal in the same window, but D164's flip is explicitly
  recorded here as an owed leg so it cannot be lost.
