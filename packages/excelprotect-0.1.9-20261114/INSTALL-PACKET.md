# PENDING - ExcelProtect 0.1.9 (P-PROTECT-I4-1, item-break fix)

Lint: 9992 B / max line 84 / non-ASCII 0
Packet: ~/workspace/protect-i4-fix-20261114/INSTALL-PACKET.md
Prepared: Sat Nov 14, 2026, board-supervisor worker run 340 (filing
prep only - the supervisor files this packet; the failsafe installs).

## Summary

Gated jar swap ExcelProtect 0.1.8 -> 0.1.9. The 0.1.8 item-break
handler throws IllegalStateException ("We don't have max_damage!")
and records nothing when an item's meta carries damage but no
max_damage component. 0.1.9 guards with hasMaxDamage() + try/catch
and falls back to the material's max durability, so the break is
always recorded. Build record: BUILD-RECORD.md in this folder
(5,474 B / a7ada63d6914b517c0c1a4641cdfd52b7d7cd61be74742ca35b942ab8897f3d1).

## Live-premise receipts (G1)

Receipts file: ~/workspace/protect-i4-fix-20261114/RECEIPTS.md
(6,861 B / SHA-256 7e4e96c521b72ccc9bacb01c3e4c432bbe354298f4313d81804c9e962e64dfaa)
All pulls Homebase (PC) rclone route, 2026-11-14 11:13-11:17 CST;
probe PASS (DESKTOP-IL05DQR); no fallback used.

- Live jar claim: plugins/ExcelProtect-0.1.8.jar is 268,894 B /
  73373b90f78f7789575535b0d2de59847011e52868309dfee7d480908e99719a
  (RECEIPTS Receipt 1; local pull live-pull/live-018.jar).
- Live config claim: plugins/ExcelProtect/config.yml is 13,164 B /
  543da9f9cc2a4410a8d4747d531816bb96e580fb754db63fe10fe68558de1f51
  (Receipt 2). NOTE: this is the REFRESHED config - the config-
  refresh entry landed today (failsafe 10:33 CST, boot 10:53 CST)
  and placed exactly these bytes. The pre-refresh expectation
  (7,991 B / 7a423378...) is superseded by this fresh pull.
- Census claim: exactly ONE active ExcelProtect jar in live
  plugins/ (the 0.1.8 above; 8 older jars inert as .held/.bak;
  383 top-level entries; Receipt 3).
- Boot baseline claim: live boot (config-refresh boot) shows
  Initialized 88 plugins, READY version=0.1.8 retention-days=30,
  guild LOCKED 4 of 5, SEVERE 0 (Receipt 4).

## Candidate

- Artifact: ~/workspace/protect-i4-fix-20261114/ExcelProtect-0.1.9.jar
- 269,208 B / SHA-256
  893892889318ae2d5ffc08846a55b92d0b83bab4140d9a6f46d9f91f4cf6b626
  (fingerprint EXC-PROTECT-0.1.9-WRV-20261114-01-0af02b).
- Base gate: 0.1.8 sources rebuild byte-identical to the live jar.
- Deterministic: two independent builds, identical hash.
- Suite: 99/99 on 0.1.9; regression discrimination PASS (the 2 new
  T-I4 tests fail on 0.1.8, pass on 0.1.9).
- Mirror rig T-I4: PASS - boot Done, READY version=0.1.9, 2
  item_break records (incl. the exact crash shape, recorded with
  material fallback 59), 0 max_damage exceptions, SEVERE 0; mirror
  restored byte-exact (11,218 files / 3,525,741,824 B).

## Live-jar gate (STOP on mismatch)

- Live file plugins/ExcelProtect-0.1.8.jar = 268,894 B /
  73373b90f78f7789575535b0d2de59847011e52868309dfee7d480908e99719a
  (Receipt 1; re-pull fresh at the window and re-check - any
  mismatch stops the window, no exceptions).

## Scope

- CHANGES: one file only - the ExcelProtect jar (0.1.8 -> 0.1.9).
- Config-untouched + ordering-independent: this swap never reads,
  writes, or replaces plugins/ExcelProtect/config.yml or any data
  file. It is independent of the config-refresh entry in either
  order, because (a) the jar swap touches no config bytes, (b)
  0.1.9's Settings defaults and shipped config text are unchanged
  from 0.1.8 (the fix is confined to the item-break handler), so
  0.1.9 runs identically on the pre-refresh text (7,991 B) and the
  refreshed text (13,164 B) - the refreshed text's keys are all
  already-default values under both jars, and (c) each entry gates
  on its own file's hash only. As filed, the refresh has already
  landed; the window proves config-untouched by a post-boot pull
  matching the at-window pre-swap pull exactly.
- DOES NOT CHANGE: config, any data/stream file, any other
  plugin's jar/config/data, player data, or world content.

## Gates (gated jar swap - full discipline)

- Marker discipline: WINDOW-IN-PROGRESS.txt at window start,
  deleted at finish; re-check the marker immediately before Shut
  Down and again before Start (a foreign marker under 3 hours old
  = coordination abort: restore swaps, entry stays PENDING).
- BACKUP FIRST: one fresh Shockbyte backup when the window opens;
  cap-at-6 - prune the OLDEST manual backups until the post-
  creation total is <= 6, name the prunees in the marker and the
  install report; never delete the Daily Backup's newest copy or
  the two newest manual restore points.
- Emptiness: list = 0/10 twice before the swap, again before
  Shut Down; server left Online after Start.
- Held-aside: before placing, rename the live jar to
  plugins/ExcelProtect-0.1.8.jar.bak-20261114-pre019 and verify its
  pull-back equals the gate hash EXACTLY (268,894 B / 73373b90...).
- Place ONLY the candidate jar from this folder as
  plugins/ExcelProtect-0.1.9.jar; verify the placed file by
  pull-back hash EXACTLY (269,208 B / 89389288...) before Start;
  confirm exactly one active ExcelProtect jar after placement.

## Verification by script output (P-SCRIPT-1 bundle)

Scripts: ~/workspace/wrv-install-queue/tools/. Every step below
lands as a verbatim 4-line result block (STEP / EXPECTED / ACTUAL /
RESULT) in the window's SCRIPT-OUTPUT.txt in this folder; any
RESULT: FAIL stops the window at that step.

- Gate re-pull: live_pull_check.py --remote-path
  "plugins/ExcelProtect-0.1.8.jar" --size 268894 --sha256
  73373b90... (full hash above) --lane rclone --allow-pull
  -> RESULT: PASS
- Backup cap: backup_cap_check.py on the window backup listing
  -> EXPECTED: total<=6, RESULT: PASS (the lane names and prunes
  under its own gates; the script counts).
- Emptiness (twice + pre-Shut Down): emptiness_check.py on each
  captured list output -> EXPECTED: online=0 max=10, PASS x3.
- Held-aside jar: verify_hash.py on its pull-back --size 268894
  --sha256 73373b90... -> PASS (held bytes equal the gate bytes).
- Placed jar: verify_hash.py on its pull-back --size 269208
  --sha256 89389288... -> PASS.
- Config untouched: verify_hash.py on pre-swap and post-boot pulls
  of plugins/ExcelProtect/config.yml - the two pulls must equal
  EACH OTHER exactly (value read at the window; at filing it is
  13,164 B / 543da9f9... per Receipt 2) -> PASS x2.
- Boot scan: boot_log_scan.py on the pulled boot log
  --expected-severe 0 --guild-locked "<LOCKED value read pre-
  window>" -> EXPECTED: done=present severe=0, RESULT: PASS.
- Boot lines (verify_counts.py on the boot log, mode lines, each
  expected count=1): "Enabling ExcelProtect v0.1.9";
  "READY version=0.1.9" with "retention-days=30" on the same line;
  "Initialized 88 plugins" (count unchanged).
- Fix live: verify_counts.py on the post-boot log --pattern
  "max_damage" --expected 0 -> PASS (no exception text).
- Marker: verify_marker.py on the marker below -> present,
  plugin=ExcelProtect, version=0.1.9.

## Boot verify (expected)

- Done line present; plugin count unchanged (Initialized 88
  plugins, per Receipt 4 baseline - if the count legitimately
  moved since filing, unchanged-vs-pre-window-read governs).
- ExcelProtect: Enabling v0.1.9; READY version=0.1.9,
  retention-days=30, gp/vault/mcmmo/we adapter states as baseline.
- Guild LOCKED state unchanged from the pre-window read (4 of 5
  at filing, per Receipt 4 - the Warden sequence may legitimately
  move it tonight; unchanged-vs-pre-window is the gate).
- SEVERE 0 attributable; no "max_damage" exception text.

## Rollback

- Stop the server (emptiness rules as at install), remove
  plugins/ExcelProtect-0.1.9.jar, restore the held-aside jar to
  plugins/ExcelProtect-0.1.8.jar, verify its pull-back equals the
  gate hash EXACTLY, Start, and re-run the boot verify lines with
  version=0.1.8 expected. Config/data were never touched, so no
  other restore step exists.

## Same-pass surfaces

Protect is a backend/audit plugin; the fix changes no player-
visible behavior, command, price, or rule. Checked against the
live surface files pulled 2026-11-14 (Receipt 5) + website desk
copies:

- Guide knowledge (plugins/ExcelGuide/knowledge.yml): CHANGE -
  the All-plugins row claims "ExcelProtect Created by us 0.1.8
  installed November 8, 2026" (1 version claim; the dated news
  mention "(ExcelProtect 0.1.8)" is history and stays). OWED:
  one TEXT live-edit bumping the row to 0.1.9, filed by the
  supervisor in the same pass as this packet; this jar window
  does not close until that placement lands or is filed PENDING.
- Guide intents (intents.yml): NO CHANGE - 2 mentions, both
  version-free ("written down in ExcelProtect", AutoSort rows).
- What's New, website surface: NO CHANGE - no Protect version
  claim on the website desk copies (0 mentions in the newest
  section work); the site AI knowledge row mirrors the Guide
  knowledge row and is discharged by the Guide leg above.
- What's New, in-game (ExcelWhatsNew changelog.yml): NO CHANGE -
  2 mentions, both version-free; no entry owed for a backend fix.
- Broadcaster tips (ExcelTips tips.yml): NO CHANGE - 0
  ExcelProtect mentions, 0 version claims.
- In-game menus (ExcelMenu + ExcelBedrockMenu): NO CHANGE - the
  Lost & Found entries carry no Protect version claim (checked
  entries/lostfound.yml + entries/yi_lostfound.yml live pulls).

## Proof owed after install

- None player-facing (backend fix; no player proof owed).
- Supervisor close-out (desk proof, not player proof): the boot
  lines above, plus a post-install item_break stream check -
  console-observable: after Start, confirm the items stream
  (plugins/ExcelProtect data, items-YYYY-MM-DD.jsonl) keeps
  appending and the post-boot log holds 0 "max_damage" lines
  (verify_counts above). The supervisor closes P-PROTECT-I4-1
  from those two reads.

## Marker on install

- ~/workspace/protect-i4-fix-20261114/INSTALLED.txt
  (plugin=ExcelProtect, version=0.1.9, plus the window backup
  name, prunees, gate results, and placed hash, per house shape)
