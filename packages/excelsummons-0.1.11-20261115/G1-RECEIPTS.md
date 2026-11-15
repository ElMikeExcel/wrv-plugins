# G1 RECEIPTS — P-SUMMONS-TUNING-1 ExcelSummons 0.1.11 (board supervisor run 364, worker W8)

All timestamps America/Chicago (CST, UTC-6).
Worker run: Sat Nov 14, 2026, 23:31–23:34 CST.
Packet read first: `packet/RECEIPTS.md` (G1 slots G1-1…G1-4, blank by
design) + `packet/INSTALL-PACKET.md`.
Deliverable rule followed: structured evidence only — live path, size,
SHA-256, pull timestamp, route used, EXACT / MISMATCH per slot.
No live file was edited. No packet RECEIPTS.md, MANIFEST.md, or live
file was edited. Pulls were read-only.

## Route — Homebase probes (retry pattern)

Route: Homebase — SSH alias `mike-pc` (HostName 100.72.156.1, User
mikeexcel, ProxyCommand `~/workspace/pc-access/ts-connect.py` via the
runtime Tailscale relay), then on-PC rclone remote `shockbyte:` with
root `1. Michael's Server/…`.
Last successful probe before this run: 23:03 CST Nov 14, 2026
(failsafe, PASS attempt 1 — per tasking).
Note: the parallel Menu G1 worker (W7) recorded 3 failed probes
23:26:59–23:28:30 CST (relay reset); the relay had recovered by this
run — no collision with that worker's pulls occurred (it obtained no
connection; this run's pulls ran alone, 0 PULL-FAILED).

| # | Time (CST) | Command | Result |
|---|---|---|---|
| 1 | 2026-11-14 23:31:51 | `ssh mike-pc "hostname; echo SSH_OK; whoami"` | Connection ESTABLISHED (no reset); Windows `hostname` without `.exe` answered with a sethostname usage error, exit 1 — command-semantics artifact, not a transport failure |
| 1b (confirmation, same attempt window) | 2026-11-14 23:31:58 | `ssh mike-pc "echo SSH_OK"` → `SSH_OK`, exit 0; `ssh mike-pc "hostname.exe"` → `DESKTOP-IL05DQR`, exit 0 | **PASS on attempt 1 of 3** — no further attempts needed |

Verdict: Homebase route UP. Last successful probe is now
2026-11-14 23:31:58 CST (this run). No fallback used; no panel touched.

Pull execution (on-PC PowerShell `pull-w8.ps1`, retained in
`pull-g1-w8/`):
- Staging: `C:\Users\MikeExcel\muse-temp\g1-w8-summons-r364`
- rclone copyto ×4: 23:32:55 (jar) / 23:32:59 (config) /
  23:33:03 (modifiers) / 23:33:07 CST (guild state); plugins
  listing 23:33:11 CST; script `DONE w8` 23:33:14 CST, exit 0,
  0 PULL-FAILED. (Known cosmetic artifact: PC-side HASHES.txt
  self-read error while the hash loop enumerated the file it was
  writing — all other files' PC hashes were written normally.)
- scp down to `pull-g1-w8/` at 23:33 CST; local SHA-256 (23:33:28
  CST) matches PC-side Get-FileHash for every pulled file —
  byte-identical transport.
- PC cleanup (PC cleanup rule): staging dir + pull script deleted
  from the PC in the same pass, 23:34:01 CST; verified
  `Test-Path = False`.

---

## G1-1 — Live ExcelSummons jar

- Live path: `plugins/ExcelSummons-0.1.9.jar`
  - Census (fresh `rclone lsf plugins/`, 23:32:24 CST +
    `plugins-top.txt` 17,319 B / `c78bcf74c18d4f03526548d8195b4272`
    `01a9cc9211d0e6d5112ef34a873bc415`): exactly ONE loadable
    ExcelSummons jar — `ExcelSummons-0.1.9.jar`. Siblings are
    non-loadable: 0.1.2/0.1.4/0.1.5/0.1.6/0.1.7 `.jar.bak-<date>`,
    0.1.8 `.jar.held-sum019`. No 0.1.10 jar exists live.
- Size: **102,260 B**
- SHA-256: **`ead3beec553da8e37d255971c8e8e5f96a707835c0a8811a90df115759e6df46`**
  (PC Get-FileHash = local sha256sum, exact)
- Pulled: 2026-11-14 23:32:55 CST — Route: Homebase (SSH/rclone)
- In-jar proof: `plugin.yml` version **0.1.9**; `excel-build.txt` =
  `EXC-SUMMONS-0.1.9-WRV-20261114-02-c9a114` (seq-02)
- Verdict vs tasking expectation (live 0.1.9 seq-02 candidate of
  record, installed Nov 14: 102,260 B / `ead3beec…`): **EXACT**
- Verdict vs packet G1-1 / INSTALL-PACKET live-jar gate (must be
  BB-11 0.1.10: `plugins/ExcelSummons-0.1.10.jar` = 102,597 B /
  `97e442165b755fb7fac325e712f634c2607490bf7f0de0dd83b28510036ff7d0`):
  **MISMATCH — STOP condition in effect.** BB-11 (0.1.10) has NOT
  landed; live is still 0.1.9. Per INSTALL-PACKET.md: "If BB-11
  has not landed, STOP — do not install 0.1.11 over live 0.1.9."
  This packet must HOLD / install after BB-11; the G1-1 slot can
  only be filled with the gate value after BB-11 installs.

## G1-2 — Live ExcelSummons config

- Live path: `plugins/ExcelSummons/config.yml`
- Size: **5,878 B**
- SHA-256: **`659d9e2c643189c9f65e243c1eb8517c7f5d4775a7cad5ed9cb2502662f3c3a9`**
- Pulled: 2026-11-14 23:32:59 CST — Route: Homebase (SSH/rclone)
- Verdict: **EXACT** vs the live config of record (unchanged since
  the Nov 11, 2026 install; same hash in the PC-route drill and in
  run 335 G1 Receipt S-3). No drift.

## G1-3 — Live Magic modifiers.yml (reserve blocks)

- Live path: `plugins/Magic/modifiers.yml`
- Whole file — Size: **263,522 B**
- Whole file — SHA-256: **`3d42a9a77c31c869ca35d01d3801e22d032f23b323047e86bb501229017724bc`**
- Pulled: 2026-11-14 23:33:03 CST — Route: Homebase (SSH/rclone)
- Verdict (whole file): **EXACT** vs the last merged modifiers of
  record (Nov 11, 2026 parent merge placed 263,522 B /
  `3d42a9a7…`). No drift.
- Reserve blocks (parsed from the pulled file this run):
  1,023 top-level keys total; exactly **18** `wrv_summon_reserve_*`
  keys — the packet's expected key set, all present, with the OLD
  (pre-0.1.11) boosts the replacement file is written against:
  archer −0.10, archer2 −0.20, bruiser −0.15, bruiser2 −0.30,
  guardian −0.25, revenant −0.20, ghast −0.25, wolf −0.10,
  wolf2 −0.20, wolf3 −0.30, wolf4 −0.40, wolf5 −0.50,
  familiar −0.05, familiar2 −0.10, familiar3 −0.15, cat −0.05,
  cat2 −0.10, mount −0.10 (all `mana_max_boost`).
- Packet replacement file (desk check, not live):
  `packet/modifiers-replacement-0.1.11.yml` — 3,516 B /
  `62c6975ebf9ced1ec0d3a3e73465b782a1e47c59a64f957ada5ee7c0e13a63be`,
  same 18 keys.

## G1-4 — Guild state

- Live path: `plugins/ExcelGuildGate/state.yml`
- Size: **1,080 B**
- SHA-256: **`d5bffa9922706babbb9d6de70fa687401e4b5dc565c74d29697585391c441890`**
- Pulled: 2026-11-14 23:33:07 CST — Route: Homebase (SSH/rclone)
- Content: `unlocked: true`; **5 of 5 seals broken** (ender_dragon,
  wither, elder_guardian, raid, warden — warden killer
  `Jester_Turtle`, warden timestamp = unlockTimestamp). Guild
  UNLOCKED 5/5 — the post-Warden state from tonight's attempt.
  This is the baseline the packet's "guild state unchanged"
  boot-verify must compare against at install time.

## Candidate config hash at filing (desk, for RECEIPTS.md slot)

- `packet/config.yml` (candidate bridge config, D64 reserve rows):
  **6,187 B / `696005bae6201ab93a8485394021a87cf304d151eda0af0d711944a078106df8`**
  (sha256sum this run; not a live pull)

---

## Summary / gate state

- G1 pulls: **COMPLETE** — all four slots obtained fresh this run
  via Homebase, PC hash = local hash for every file, no fallback.
- G1-2, G1-3, G1-4: EXACT / as-premised.
- G1-1: EXACT vs the current-live expectation (0.1.9 seq-02,
  102,260 B / `ead3beec…`) but **MISMATCH vs the packet's own
  live-jar gate** (BB-11 0.1.10, 102,597 B / `97e44216…`).
  Filing consequence: P-SUMMONS-TUNING-1 (0.1.11) is NOT
  install-ready until BB-11 (ExcelSummons 0.1.10) installs first;
  its G1-1 gate receipt must be re-pulled after that landing.
- Retained pulls (this run): `pull-g1-w8/` beside this file —
  `plugins__ExcelSummons-0.1.9.jar`,
  `plugins__ExcelSummons__config.yml`,
  `plugins__Magic__modifiers.yml`,
  `plugins__ExcelGuildGate__state.yml`, `plugins-top.txt`,
  `HASHES.txt` (PC-side), `pull-w8.ps1`.
- PC-side cleanup: DONE + verified (staging deleted 23:34:01 CST,
  Test-Path False). No tabs/sessions touched beyond the SSH pulls.
