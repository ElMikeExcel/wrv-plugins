# INSTALL PACKET (DRAFT - not filed) - ExcelProtect 0.1.10 Audit Watch

- Item: P-PROTECT-WATCH-1 (Mike, Nov 14, 2026, ~6:03 PM CST) + the
  EXCELPROTECT PER-PLAYER LAW (Mike, Nov 14, 2026, ~6:09 PM CST).
- Packet: ~/workspace/board-r364/protect-watch-build/INSTALL-PACKET.md
- Receipts: ~/workspace/board-r364/protect-watch-build/RECEIPTS.md
  (build-side facts filled; G1 slots blank by design - filing day fills)
- Lint: 5175 B / max line 95 / non-ASCII 0 (targets: <= ~12 KB, lines < 300, minimal non-ASCII)

## Candidate

- ExcelProtect-0.1.10.jar - 295195 B / SHA-256
  dd78718e789492865aec621dda6f5ccefdcce57a4df95b0b4bc360c15de1be61
- Build EXC-PROTECT-0.1.10-WRV-20261114-01-0d1a10; deterministic
  double-build (A = B); tests 107/107 (99 base + 8 new watch cases).
- Base: ExcelProtect 0.1.9 tree of record
  (~/workspace/protect-i4-fix-20261114/), rebuilt byte-identical at the
  base gate (269208 B / 893892889318ae2d5ffc08846a55b92d0b83bab4140d9a6f46d9f91f4cf6b626).
  0.1.9 already contains the P-PROTECT-I4-1 item-break fix as landed;
  this build adds NO new I4 work - the fix rides only as base identity.
- Local rig: boot PASS, Done (40.115s), SEVERE 0, audit watch ON line
  (magic=on, summons=on), READY version=0.1.10. See RECEIPTS.md A.

## Scope

- ADDS the audit watch: a per-player gameplay audit log, daily files
  under plugins/ExcelProtect/audit/<player>-<day>.jsonl, recording
  spell casts (spell + mana cost + mana / mana_max / effective max),
  summon spawn/despawn (observed via the ExcelSummons attribution
  bridge; reasons are observed removal causes), mana-reserve
  re-derivations after each observed spawn/despawn (source=derived;
  CAST-only holds; ceiling/floor clamp; percent table read from the
  LIVE ExcelSummons config), and refusals (emitter-reported
  summon_refusal via the new GameplayEvent door with the exact numbers;
  observed PreCast cancellations of summon-family spells stamped
  source=observed_precast). Every record also lands on the canonical
  audit stream, so inspect / export / player timelines carry it.
- ADDS /excelprotect audit <player> [hours] (read side) and the
  log.gameplay config section (five toggles, all default on).
- ADDS api/GameplayEvent + ExcelProtectAPI.gameplay (the emitter door;
  ExcelSummons adoption is a follow-on build, named in RESULT.md).
- Config: candidate config = live 0.1.9 config + the log.gameplay
  section only. Filing-day diff against PULL-02 decides replace vs
  merge (RECEIPTS.md C).
- Does NOT change: any existing stream, record shape, rollback,
  Lost & Found, the 0.1.9 item-break behavior, any other plugin, any
  player data. No data migration; the audit/ folder is created empty
  at first enable and fills from live play.

## Gates (filing day, supervisor + failsafe)

- G1 receipts filled in RECEIPTS.md B from same-day pulls (PULL-01..05);
  STOP on any mismatch with the premises stated there.
- Marker discipline with re-checks before Shut Down and before Start;
  BACKUP FIRST + cap-at-6 (name prunees; never the Daily newest or the
  two newest manual restore points); emptiness list = 0/10 twice.
- Swap: live ExcelProtect-0.1.9.jar held aside as
  ExcelProtect-0.1.9.jar.bak-<date>-pre0110; candidate placed as
  plugins/ExcelProtect-0.1.10.jar; config per the filing-day decision.
- Verification by script output: bundle commands per P-SCRIPT-1
  (live_pull_check, backup_cap_check, emptiness_check, boot_log_scan)
  named in the filed stub at filing time.

## Boot verify

- "[ExcelProtect] audit watch: ON dir=plugins/ExcelProtect/audit
  per-player daily files magic=on summons=on (casts + summon
  spawn/despawn + derived mana-reserve + refusals)"
- "[ExcelProtect] READY version=0.1.10" + SEVERE 0 attributable.
- Guild state line as pulled at filing (PULL-04).
- Marker: ~/workspace/board-r364/protect-watch-build/INSTALLED.txt
  (written by the installing lane; this draft folder is the home).

## Same-pass surfaces (owed at install, named here)

- What's New BOTH surfaces: website section + in-game
  ExcelWhatsNew changelog entry (new feature players can see working).
- Guide: knowledge + intents - a chunk answering "where can I see my
  casts / summons / mana holds" (/excelprotect audit + the audit log).
- Website pair (winkreyesvalley) updated in the same pass as the Guide.
- Broadcaster tips: check; a tip naming /excelprotect audit is a
  candidate, decided at the surfaces pass.
- In-game menus (ExcelMenu + ExcelBedrockMenu): checked at the
  surfaces pass; no menu change is expected (admin command only).
- Records: wrv-plugins draft PR (jar + source deposit) per G2 with the
  duplicate guard run first; publication claim line in the close-out.

## Proof owed after install

- Player proof (Mike): cast a spell, summon an archer, dismiss it -
  then /excelprotect audit .MikeExcel shows the cast line with mana
  cost, the spawn line, a derived mana_reserve line with the held
  percent, and the despawn line, in that order.
- Emitter proof (follow-on): once ExcelSummons adopts the GameplayEvent
  door, a cap/mana refusal in game produces a summon_refusal record
  carrying the exact reason and numbers shown to the player.
