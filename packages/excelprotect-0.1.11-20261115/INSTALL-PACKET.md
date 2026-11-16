# INSTALL PACKET (DRAFT - not filed) - ExcelProtect 0.1.11 EM Attempts

- Item: P-PROTECT-EM-1 Option A (Decision D71, Nov 15, 2026) + build-brief
  calls D72 (Q1 em_ prefix scope + config list, Q2 coalesce 2000 ms,
  Q3 separate attempts stream, Q5 attempted-type block field,
  Q6 no held-item field, Q7 no alerting / no new command) + the
  EXCELPROTECT PER-PLAYER LAW (Mike, Nov 14, 2026).
- Packet: ~/workspace/protect-0111-build-20261115/INSTALL-PACKET.md
- Receipts: ~/workspace/protect-0111-build-20261115/RECEIPTS.md
  (build-side facts filled; G1 slots blank by design - filing day fills)
- Lint: 5980 B / max line 95 / non-ASCII 0 (targets: <= ~12 KB, lines < 300, minimal non-ASCII)

## Candidate

- ExcelProtect-0.1.11.jar - 303587 B / SHA-256
  ba10bbcd84de50f199f78b1e09d354ee189f8919ba9732d654ed2a4bcc94260f
- Build EXC-PROTECT-0.1.11-WRV-20261115-01-0d1a11; deterministic
  double-build (A = B); tests 117/117 (107 base + 10 new attempt cases).
- Base: ExcelProtect 0.1.10 tree of record
  (~/workspace/board-r364/protect-watch-build/build/), rebuilt
  byte-identical at the base gate (295195 B /
  dd78718e789492865aec621dda6f5ccefdcce57a4df95b0b4bc360c15de1be61).
- Rig: PASS - mirror rig Q4 PROVEN 7/7, run 404 W5b
  (~/workspace/board-r404/protect-rig2/RIG.md, sums 10/10 OK):
  two boots clean, Enabling 0.1.11 x1, SEVERE 0, placed jar EXACT
  ba10bbcd... The filing gate on the rig leg is SATISFIED.

## Scope

- ADDS block attempts: a MONITOR, ignoreCancelled=false listener records
  ONLY block break/place events already cancelled at observation time,
  ONLY in EM-scope worlds (name starts with the configured prefix,
  default em_, or the world is in log.attempts.worlds). Attempts merge
  per player + position + action + block inside the coalesce window
  (log.attempts.coalesce-ms, default 2000, clamped 250-30000) and close
  as block_attempt records on the NEW separate attempts stream
  (attempts-<day>.jsonl), with the player as the TOP-LEVEL actor field,
  so per-player timelines, export and inspect carry them.
- Records carry type block_attempt, cancelled=true and a count, and NO
  before/after/sign fields; they live outside the blocks stream -
  rollback exclusion holds by construction (three locks, unit-pinned).
- Read side is the existing surface only: /excelprotect export
  (stream=attempts), inspect player (new attempts section after
  blocks), inspect at (attempts stream added to the position scan).
  No new command, no alerting, no held-item field.
- Config: candidate config = live 0.1.10 config + the log.attempts
  section + attempts added to writer.shed-order after blocks.
  Filing-day diff against PULL-02 decides replace vs merge.
- Does NOT change: any existing handler (Blocks keeps MONITOR /
  ignoreCancelled=true verbatim), any existing stream or record shape,
  rollback, Lost & Found, the audit watch, any other plugin, any
  EliteMobs file, any player data. No data migration; the attempts
  stream files are created by the writer on first emit.
- Volume note (scoping R2): coalescing collapses repeats at ONE
  position; multi-position attempt sweeps still write one record per
  position per window - estimated, not measured, in the build task.

## Gates (filing day, supervisor + failsafe)

- G1 receipts filled in RECEIPTS.md from same-day pulls (PULL-01..04);
  STOP on any mismatch with the premises stated there.
- Rig leg (U7) complete and PASS, including the Q4 proof; a refuted Q4
  returns the packet to the supervisor - it does not file.
- Marker discipline with re-checks before Shut Down and before Start;
  BACKUP FIRST + cap-at-6 (name prunees; never the Daily newest or the
  two newest manual restore points); emptiness list = 0/10 twice.
- Swap: live ExcelProtect-0.1.10.jar held aside as
  ExcelProtect-0.1.10.jar.bak-<date>-pre0111; candidate placed as
  plugins/ExcelProtect-0.1.11.jar; config per the filing-day decision.
- Verification by script output: bundle commands per P-SCRIPT-1
  (live_pull_check, backup_cap_check, emptiness_check, boot_log_scan)
  named in the filed stub at filing time.

## Boot verify

- "[ExcelProtect] attempts: ON stream=attempts scope=prefix 'em_' +
  config list coalesce=2000ms (cancelled block break/place in EM
  worlds only)"
- "[ExcelProtect] READY version=0.1.11" + SEVERE 0 attributable.
- Guild state line as pulled at filing (PULL-04).
- Marker: ~/workspace/protect-0111-build-20261115/INSTALLED.txt
  (written by the installing lane; this draft folder is the home).

## Same-pass surfaces (owed at install, named here)

- What's New BOTH surfaces: website section + in-game ExcelWhatsNew
  changelog entry. Wording must not promise players a self-serve
  attempt viewer - attempt records are staff evidence.
- Guide: knowledge + intents for the staff read paths (export /
  inspect over the attempts stream).
- Website pair (winkreyesvalley) updated in the same pass as the Guide.
- Broadcaster tips: check; no player-facing tip expected (staff-only
  surface) - decided at the surfaces pass.
- In-game menus (ExcelMenu + ExcelBedrockMenu): checked at the
  surfaces pass; no menu change is expected (no new command).
- Records: wrv-plugins draft PR (jar + source deposit) per G2 with the
  duplicate guard run first; publication claim line in the close-out.

## Proof owed after install

- Player proof (Mike): in live EM worlds (guild hub + em_primis + one
  Primis instance), attempt to break and to place a block - the block
  is unchanged, exactly one coalesced block_attempt per position/action
  lands in attempts-<day>.jsonl and in the player's timeline file, no
  block_change row appears, and export stream=attempts + inspect
  <player> read it back in game. A cancelled attempt outside EM scope
  produces no record; a successful normal-world edit produces exactly
  one block_change and no attempt. Kailey is not assigned any proof.
- Toggle proof: log.attempts.enabled=false stops recording with all
  other streams unaffected; re-enable resumes it (D71 reversal path).
