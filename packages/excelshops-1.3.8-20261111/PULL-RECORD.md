# ExcelShops 1.3.8 re-base — live-jar pull record (Nov 11, 2026, board supervisor run 189)

- Route: rclone through Mike's PC (SSH over Tailscale, host mike-pc / DESKTOP-IL05DQR). Route had been down since ~4:52 AM (7 consecutive proxy-closed failures, runs 183–188); Mike's PC rebooted overnight for a Windows update and he signed in ~9:16 AM — Tailscale/SSH/rclone all live again (daily log Nov 11).
- Pull (09:22 CST): `rclone copyto "shockbyte:1. Michael's Server/plugins/ExcelShops-1.3.7-analytics-OFFLINE.jar"` → PC `wrv-pulls/`, 246.421 KiB transferred; then scp to this folder.
- Landed here: `ExcelShops-1.3.7-analytics-OFFLINE.jar` — 252,335 B / SHA-256 12cf683a3b3125f4d1376ebe180699770ed630fb392a4b2c43f956a06a29988f. PC-side certutil hash = VM-side sha256sum EXACT match.
- Identity verified: plugin.yml inside the jar — name ExcelShops, version '1.3.7-analytics-OFFLINE', main com.winkreyes.shops.WRVShops, class tree dated 2026-10-29. Size matches the console-proven live size (246.42 KB).
- Next unit (chain): reconstruct the 1.3.7 source tree from this jar (decompile), re-base the read-only console-admin delta onto it as ExcelShops 1.3.8, re-verify (tests + rig + deterministic build), re-file as a new PENDING entry superseding HELD 1.3.5. Owner: board supervisor (chain).
- Side benefit of route recovery: the owed SFTP SHA re-verifies (Lightning|3 spells files, TAME gate caveat, Summons 0.1.4 placed jar) are unblocked — queue them in the next run.
