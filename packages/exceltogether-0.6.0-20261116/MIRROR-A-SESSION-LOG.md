# Session log — Mirror Branch A, run 427 W2 (Mon Nov 16, 2026, America/Chicago)

All times CST (PC-local log timestamps). Venue: Homebase test-server mirror ONLY
(`C:\Users\MikeExcel\wrv-testserver\server`, Paper 26.3 build 245). Live server
never touched; no panel contact; no MANIFEST/backlog/state edits.

| Time | Action | Result |
|---|---|---|
| 07:47 | Homebase probe attempt 1: `ssh -F ~/.ssh/config mike-pc hostname` | PASS — `DESKTOP-IL05DQR`, exit 0. No fallback used (none exists for a mirror rig). |
| 07:47 | Mirror inspect (inspect427.ps1) | JAVA NONE; Together 0.5.3 `494073DC…F9F5`; EM file EliteMobs-10.9.10.jar (10.9.12) `3D796000…B995`; Downed 0.1.4 `703D7F9A…339` (current left state); xpfixrig EM 10.9.14 present `4A6BC4A7…C44BD`. |
| 07:48 | Candidate verified on VM | `~/workspace/board-r426/together-parts23/work/build-0.6.0-r426p23/ExcelTogether-0.6.0.jar` = 195,872 B / `6e935f161e2b76c9349b71f73183970beeb8a659a306fb0279075b25f6ebc964` — exact vs run-426 W6 REPORT.md §5 + SHA256SUMS. |
| 07:49 | Upload candidate + driver src + scripts to `muse-temp\together-rig427` | Candidate hash re-verified on PC staging: `6E935F16…C964`, 195,872 B. annotations.jar copied from rig418 staging. |
| 07:49 | Driver build attempt 1 | javac exit 1 — missing `ServerPlayer` import in the new driver; fixed (one import line), no other change. |
| 07:50 | Driver build attempt 2 | javac exit 0; TogetherRig427.jar 12,972 B / `14339C34967F776929104250C97E33AB1F689811EA72F0A0EB5DA17A84CE5668`. |
| 07:50 | Stage (stage-r427.ps1) | census-before written; data folders backed up to `wrv-testserver\r427-backup`; candidate-on-mirror 195,872 B / `6E935F16…C964`; em10914-on-mirror 14,563,211 B / `4A6BC4A7…C44BD`; driver staged. STAGE COMPLETE. |
| 07:50–07:51 | Boot NA #1 (mode NA, RigA/B22188) | A1/A2/A3 driver PASS, SEVERE 0, 0 revert lines — BUT boot log shows `no roster.yml yet (first boot)` + `first-boot seed: seeded a party of 2 from EliteMobs … origin LOGIN_REBUILD` at 07:50:58. A2 completion CONFOUNDED by the first-boot seed (see REPORT.md §2). Boot retained as `evidence/bootNA1-seedconfounded.log`. Side effect used deliberately: roster.yml now exists, so the seed is disarmed for all later boots — the S1/S2 premise. |
| 07:53 | Boot NA #2 launch attempt via a fire-and-forget launcher | FAILED to start: boot.log truncated to 0 B, java never ran (launcher session exited before the detached start survived). No server state changed (JAVA NONE verified). Reverted to the proven run-mode script pattern. |
| 08:01–08:02 | Boot NA #2 (mode NA, RigA/B6897) — THE SCORED A1/A2/A3 BOOT | Roster restored 1 party from roster.yml at enable (seed NOT armed). A1 PASS, A2 PASS (origin DOOR), A3 PASS. SEVERE 0, revert lines 0. `evidence/bootNA2.log`. (SSH session dropped with a broken pipe mid-run — the S1/S2 pattern; the detached boot completed on the PC and the log was recovered on re-probe. Relay-path evidence only.) |
| 08:04 | Boot NB (A4i, RigC/D56846) | PASS. SEVERE 0, revert lines 2. `evidence/bootNB.log`. |
| 08:05–08:06 | Boot NC (A4ii, RigE/F33906) | PASS. SEVERE 0, revert lines 1. `evidence/bootNC.log`. |
| 08:07 | Boot NE (A5, RigI/J18309) | PASS. SEVERE 0, revert lines 0. `evidence/bootNE.log`. (Broken pipe again; boot completed on PC, log recovered.) |
| 08:09–08:10 | Boot ND (A4iii, RigG/H50093) | PASS. SEVERE 0, revert lines 1 (at exactly +60 s). `evidence/bootND.log`. (Broken pipe again; same recovery.) |
| 08:11 | Cleanup (cleanup-r427.ps1) | Data folders restored from r427-backup; **CENSUS IDENTICAL** (after-cleanup vs before); Together restored `494073DC…F9F5`; EliteMobs restored `3D796000…B995`; **JAVA NONE**. PC staging left in place per house pattern: `muse-temp\together-rig427` + `wrv-testserver\r427-backup` + `boot-r427-*.log` copies. No scheduled tasks created; debug Chrome never touched. |
