# RECEIPTS - ExcelProtect 0.1.10 (P-PROTECT-WATCH-1, audit watch)

Skeleton written at build time (run 364 W11, Nov 14, 2026). G1 live-premise
slots are BLANK BY DESIGN: they are filled from same-day live pulls on the
filing day by the supervisor, never from disk lineage or this build folder.

## A. Build-side facts (verified at build, not live premises)

- Base tree of record: ~/workspace/protect-i4-fix-20261114/ (ExcelProtect
  0.1.9 sources; the 0.1.9 build IS the P-PROTECT-I4-1 item-break fix build,
  installed Nov 14, 2026 12:43 CST per that folder's INSTALLED.txt).
- Base gate: unmodified 0.1.9 sources rebuilt byte-identical -
  ExcelProtect-0.1.9.jar 269208 B / SHA-256
  893892889318ae2d5ffc08846a55b92d0b83bab4140d9a6f46d9f91f4cf6b626;
  base suite 99 found / 99 successful / 0 failed.
  Evidence: evidence/base-gate-build.log in this folder.
- Candidate: ExcelProtect-0.1.10.jar 295195 B / SHA-256
  dd78718e789492865aec621dda6f5ccefdcce57a4df95b0b4bc360c15de1be61;
  build EXC-PROTECT-0.1.10-WRV-20261114-01-0d1a10; deterministic
  (builds A and B identical); suite 107 found / 107 successful / 0 failed
  (99 base + 8 new audit-watch cases).
  Evidence: evidence/build-0110-a.log, evidence/build-0110-b.log.
- Local rig boot: Paper 26.3 (pre-patched rig directory) + Magic 11.2.5 +
  ExcelSummons 0.1.9 + candidate; Done (40.115s); SEVERE 0; audit watch
  ON line with magic=on summons=on; READY version=0.1.10; clean disable.
  Evidence: evidence/rig-boot.log.

## B. G1 live-premise slots (BLANK - fill on filing day)

- PULL-01 live jar: plugins/ExcelProtect-0.1.9.jar expected 269208 B /
  893892889318ae2d5ffc08846a55b92d0b83bab4140d9a6f46d9f91f4cf6b626 -
  receipt: <same-day pull path + size + SHA-256 + timestamp>
- PULL-02 live config: plugins/ExcelProtect/config.yml (0.1.9 content;
  the 0.1.10 config adds only the log.gameplay section - the swap plan
  must state whether the live config is replaced by the candidate config
  or the gameplay section is merged in) -
  receipt: <same-day pull path + size + SHA-256 + timestamp>
- PULL-03 live ExcelSummons jar + config (the watch reads the reserve
  percent table, ceiling and floor from plugins/ExcelSummons/config.yml
  at runtime; premise = that file exists live with a
  summon-reserve-percent section) -
  receipt: <same-day pull path + size + SHA-256 + timestamp>
- PULL-04 guild state at filing: <GuildGate unlocked state from the
  filing-day pull; run-364 premise was 5/5 unlocked, re-verify at filing>
- PULL-05 live Magic version (premise: Magic present, CastEvent API
  unchanged) - receipt: <filing-day boot log or pull>

## C. Filing-day checks owed

- pr-exists.sh duplicate guard for the records + website publications (G2).
- Config decision recorded: candidate config replaces live config ONLY
  after a diff shows no live-only values would be lost (the Nov 14
  config-refresh landed live config changes; diff PULL-02 against the
  candidate config before choosing replace vs merge).
- Packet lint re-run if this packet's text changes at filing.

---

# FILLED G1 RECEIPTS (run 365 retry, 2026-11-15) — appended at records prep, board run 409 W12

# G1 RECEIPTS (R365 RETRY) — P-PROTECT-WATCH-1 ExcelProtect 0.1.10 (board supervisor run 365)

All timestamps America/Chicago (CST, UTC-6).
Worker run: Sun Nov 15, 2026, 00:16–00:21 CST.
Read first: `RECEIPTS.md` (G1 slots PULL-01..05) + `INSTALL-PACKET.md`
+ run-364 `G1-RECEIPTS.md` (W14, all NOT OBTAINED).
Structured evidence only. No live file edited. Packet `RECEIPTS.md`
and MANIFEST.md NOT edited. Pulls read-only. Nothing filed — the
supervisor files.

## Candidate re-verified locally this run (desk check)

- ExcelProtect-0.1.10.jar: 295,195 B /
  `dd78718e789492865aec621dda6f5ccefdcce57a4df95b0b4bc360c15de1be61` — matches packet EXACT.
- Candidate config `build/config.yml`: 14,885 B /
  `a0ba0318169ecef6f7d9ffcd7e77caf2caa0e259577f2e795d1a1877a76886c2`
  (carries the `log.gameplay` section; for the PULL-02 diff below).

## Route — Homebase probes

Route: Homebase — SSH alias `mike-pc` (`ssh -F ~/.ssh/config`,
DESKTOP-IL05DQR, HostName 100.72.156.1, User mikeexcel, Tailscale
relay), then on-PC rclone remote `shockbyte:` root
`1. Michael's Server/…`.
Prior state: W14 failed 6/6 probes 23:57 CST Nov 14 – 00:03 CST
Nov 15 (relay resets); last successes 23:31–23:38 CST Nov 14 (W8/W9).

| # | Time (CST) | Command | Result |
|---|---|---|---|
| 1 | 2026-11-15 00:16:14 | `ssh -F ~/.ssh/config mike-pc "hostname; echo SSH_OK; whoami"` | Transport ESTABLISHED (no reset); Windows `hostname` usage artifact, exit 1 — not a transport failure |
| 1b (confirmation, same attempt) | 2026-11-15 00:16:35 | `ssh -F ~/.ssh/config mike-pc "echo SSH_OK"` | `SSH_OK`, exit 0 — **PASS on attempt 1 of 3** |

Verdict: Homebase route UP. Last successful probe is now
2026-11-15 00:16:35 CST (this run). No fallback used; no panel touched.

## Pull execution

- On-PC script `pull-r365.ps1` scp-up 00:17:34 CST; executed on PC in
  the window 00:17:34–00:19:20 CST (supervisor-side exec interrupted
  by a service restart mid-call; staging found COMPLETE at the
  00:19:20 CST re-check — pull had finished on-PC).
- Census `rclone lsf plugins/` → `plugins-top.txt`: 17,319 B /
  `c78bcf74c18d4f03526548d8195b427201a9cc9211d0e6d5112ef34a873bc415`
  (byte-identical to W8's 23:33 CST Nov 14 census).
- scp-down 00:19:49 CST to `~/workspace/board-r365/g1-retry/pulls/`;
  local sha256sum = PC Get-FileHash for EVERY pulled file.
- PC cleanup (same pass, verified 00:21:23 CST): staging dir
  `C:\Users\MikeExcel\muse-temp\g1-r365` + scripts deleted;
  `Test-Path = False`.

---

## PULL-01 — Live ExcelProtect jar (core premise)

- Live path: `plugins/ExcelProtect-0.1.9.jar`
- Census: exactly ONE loadable `ExcelProtect*.jar` (0.1.9);
  siblings 0.1.0–0.1.5 `.jar.held-*`, 0.1.6/0.1.7/0.1.8 `.jar.bak-*`.
  No 0.1.10 jar live.
- Fresh pull: **OBTAINED** — 2026-11-15 ~00:18 CST, Homebase rclone
  - Size: 269,208 B
  - SHA-256: `893892889318ae2d5ffc08846a55b92d0b83bab4140d9a6f46d9f91f4cf6b626`
- Expected: 269,208 B / `89389288…f6b626` — Verdict: **EXACT**

## PULL-02 — Live ExcelProtect config

- Live path: `plugins/ExcelProtect/config.yml`
- Fresh pull: **OBTAINED** — 2026-11-15 ~00:18 CST, Homebase rclone
  - Size: 13,164 B
  - SHA-256: `543da9f9cc2a4410a8d4747d531816bb96e580fb754db63fe10fe68558de1f51`
- Matches the config of record (Nov 14 config-refresh / 0.1.9
  install, pre/post EXACT): **EXACT**
- Filing-day decision input: candidate config 14,885 B /
  `a0ba0318…` vs live 13,164 B — delta 1,721 B = the `log.gameplay`
  section per the packet. The replace-vs-merge diff itself is the
  supervisor's filing step (RECEIPTS.md C); both byte sets are now
  in hand.

## PULL-03 — Live ExcelSummons jar + config

- Live paths: `plugins/ExcelSummons-0.1.9.jar` +
  `plugins/ExcelSummons/config.yml` (census: exactly ONE loadable
  ExcelSummons jar, 0.1.9)
- Fresh pulls: **OBTAINED** — 2026-11-15 ~00:18 CST, Homebase rclone
  - Jar: 102,260 B /
    `ead3beec553da8e37d255971c8e8e5f96a707835c0a8811a90df115759e6df46`
  - Config: 5,878 B /
    `659d9e2c643189c9f65e243c1eb8517c7f5d4775a7cad5ed9cb2502662f3c3a9`
  - Both **EXACT** vs W8's fresh same-night pulls (23:32:55 /
    23:32:59 CST Nov 14).
  - Config content read: `summon-reserve-percent:` section PRESENT
    (archer 10, bruiser 15, guardian 25, revenant 20, ghast 25,
    wolf 10, familiar 5, cat 5, mount 10);
    `reserve-ceiling-percent: 75`, `reserve-floor-percent: 20`.
- Verdict: **EXACT — packet premise satisfied** (reserve table,
  ceiling and floor readable live).

## PULL-04 — Guild state at filing

- Live path: `plugins/ExcelGuildGate/state.yml`
- Fresh pull: **OBTAINED** — 2026-11-15 ~00:18 CST, Homebase rclone
  - Size: 1,080 B
  - SHA-256: `d5bffa9922706babbb9d6de70fa687401e4b5dc565c74d29697585391c441890`
  - **EXACT** vs W8's fresh pull (23:33:07 CST Nov 14).
  - Content read: `unlocked: true`; **5 of 5 seals broken**
    (ender_dragon, wither, elder_guardian, raid, warden —
    warden killer `Jester_Turtle`, warden timestamp =
    unlockTimestamp).
- Verdict: **EXACT — premise holds: guild UNLOCKED 5/5.**

## PULL-05 — Live Magic version

- Live path: `plugins/Magic-11.2.5-wrvnpe1.jar` (census: exactly ONE
  loadable Magic jar; siblings same name + `.bak-*`)
- Fresh pull: **OBTAINED** — 2026-11-15 ~00:18 CST, Homebase rclone
  (jar bytes pulled this run — an upgrade on run 364, where only
  the listing filename was held)
  - Size: 7,662,464 B
  - SHA-256: `b78f03b83fe7a298f3d7cfde100392ba693cfc2e312322827d3dd556454b3999`
  - In-jar `plugin.yml`: `name: Magic`, `version: 11.2.5-baa3d91`
- Verdict: **EXACT vs premise** — Magic present live at 11.2.5,
  matching the candidate rig premise (Magic 11.2.5, CastEvent API).

---

## Summary / gate state

- G1 for P-PROTECT-WATCH-1 (ExcelProtect 0.1.10): **SATISFIED by
  this run.** PULL-01..05 all fresh-pulled 2026-11-15 ~00:18 CST
  via Homebase; every slot EXACT against its packet premise.
- Remaining filing steps (supervisor, not G1): config
  replace-vs-merge diff on PULL-02 bytes (now in hand), G2
  duplicate guard, MANIFEST stub filing.
- Fallback used: NONE (Homebase passed attempt 1).
- Retained pulls: `~/workspace/board-r365/g1-retry/pulls/`.
- Files NOT edited: packet `RECEIPTS.md`, MANIFEST.md, any live file.
