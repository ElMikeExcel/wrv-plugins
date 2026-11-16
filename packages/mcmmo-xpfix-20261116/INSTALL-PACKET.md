# INSTALL PACKET — mcMMO XP-fix, final combined build (P-MCMMO-XPFIX-INSTALL-1)

Filed: Nov 16, 2026 (parent, under Mike's "Push now" full-push directive).
Build record: BUILD-REPORT.md (same folder; ADDENDUM 1 = Taming scoping, ADDENDUM 2 = EM-world rule).
Rig record: RIG-REPORT.md (same folder) — verdict RIG GREEN against this jar's hash.

## Candidate
- File: ~/workspace/mcmmo-xpfix-build-20261115/mcMMO.jar — 3,721,966 B / SHA-256 404025cc63f5733fa135dd9303783dc9812248ce6ad9bb0d408643c8879c8501
- Installs under the live jar name: plugins/mcMMO-2.3.003-WRV-XP-2-ExcelShare-1.jar (single-jar swap, same name).
- Full suite: 1,976 tests, 0 failures.

## Live-premise receipts (G1)
- Live jar (mirror-clone census, rig restore inventory-verified Nov 16, 2026): plugins/mcMMO-2.3.003-WRV-XP-2-ExcelShare-1.jar, 3,695,180 B (the house fork build the patch is based on). BASELINE-STRENGTH only — the window re-pulls and re-hashes fresh; a size/hash mismatch against the fork's pristine base = STOP.
- Behaviour being installed (Mike's rulings Nov 15, 2026): (1) training dummies + Magic/ExcelSummons CAST summons + RewardGuard-denied targets pay ZERO mcMMO XP (adopted pets unaffected — origin read via ExcelSummonsAPI, CAST only); (2) ALL mcMMO XP is zero for a player standing in an EliteMobs world (em_ prefix + GoblinKingdom_vf + instanced copies; config extras WRV.No_XP_Worlds ship empty); Jobs money, vanilla XP, EM rewards untouched.

## Scope
- ONE jar swap. No config edit in the window: the fork seeds the WRV.No_XP_Worlds section into mcMMO's config.yml itself; verify its presence in the placed config after boot (absence with correct XP behaviour = note it, not a failure).
- No other jar/config/data touched. No LuckPerms changes.

## Gates (standard, all stand)
- Marker discipline + re-checks before Shut Down and before Start.
- BACKUP FIRST: one fresh Shockbyte backup for the window (cap-at-6 enforced, prunees named). If this install chains inside the EternalTD window's run, that window's backup covers it per the one-backup-per-window rule; if it opens its own window, it takes its own.
- Emptiness: list x2 (~1 min apart) = 0, plus the pre-swap re-check.
- Hash gates: fresh pull of the live jar hashed in full (must match the fork base build, 3,695,180 B class); candidate hashed on the PC staging path; placed jar pull-back hash must equal 404025cc...

## Boot verification
- Boot Done line present; attributable SEVERE = 0; mcMMO enabling line present; ExcelGuildGate line shows guild OPEN 5/5 (unchanged).
- Script-bundle verbatim blocks per FAILSAFE spec (backup_cap, emptiness x2, live_pull, verify_hash, boot_log_scan, verify_counts, verify_marker) into OUTCOME.txt / SCRIPT-OUTPUT.txt in this folder.
- Marker on install: ~/workspace/mcmmo-xpfix-build-20261115/INSTALLED.txt

## After install (same-pass, other lanes)
- Surfaces TEXT: Guide + What's New (both surfaces) — mcMMO XP pays nothing in EliteMobs worlds; training dummies and cast summons pay no mcMMO XP anywhere; adopted pets unaffected. Supervisor chain files; failsafe places under the live-edit exemption.
- Records: wrv-plugins OPEN DRAFT PR + Drive jar deposit (recording work, pre-approved).
