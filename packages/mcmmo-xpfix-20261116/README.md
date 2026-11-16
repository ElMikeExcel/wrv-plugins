# Record: mcMMO XP-fix final combined build (P-MCMMO-XPFIX-INSTALL-1)

- Install: Nov 16, 2026 (wrv-install-failsafe; chained behind EternalTD 1.7.10
  in the same full-push window per Mike's "Push now" directive,
  Nov 16, 2026, 12:45 AM CST). Install marker: INSTALLED.txt in this package
  (installed=2026-11-16).
- Plugin: mcMMO, version 2.3.003-WRV-XP-2-ExcelShare-1 (XP-fix final
  combined build). EM worlds pay zero mcMMO XP; dummy/summon holes closed;
  cast summons pay no Taming XP, adopted pets still do.
- Entry: P-MCMMO-XPFIX-INSTALL-1. Server: Michael's Server (Shockbyte).
  Lane: Homebase (rclone/SFTP over SSH + RCON; panel for restart).

## Artifact identities

- Candidate jar (INSTALLED, pull-back verified EXACT vs build jar):
  mcMMO-2.3.003-WRV-XP-2-ExcelShare-1.jar — 3,721,966 B /
  SHA-256 404025cc63f5733fa135dd9303783dc9812248ce6ad9bb0d408643c8879c8501
  (jar binary is NOT in this PR, per the records-package template;
  it is deposited in Drive "Build files" — see the close-out record.)
- Gate (pre-swap live jar, fresh pull at install):
  3,695,180 B /
  SHA-256 4276bd3e07bdaf60c4edbb5461c1c4a61c3252e24f15f324faaa58d7300dff5c
  (house fork base class; in-place backup kept live as
  mcMMO-2.3.003-WRV-XP-2-ExcelShare-1.jar.bak-20261116)

## Package contents

- xpfix.diff (70,555 B / ab5ed9d91231985d91e1ceaa74d91da69f077c46dfdd6b2c06abea9f5ffe7ca7)
  — full diff vs pristine fork, 1,636 lines, 12 files.
- xpfix-origin-addendum.diff (17,660 B / 1f3d25e649afd5cfa14386eba6642928dc2ce1e7b57272b44cfeb3b054edbbac)
- xpfix-world-addendum.diff (35,571 B / 7df21e0d0ed9a20df861b0be574dddff1a5ff26886077e2fbb410f3017b8c0b6)
- BUILD-REPORT.md — build record incl. ADDENDUM 1 (Taming scoping) and
  ADDENDUM 2 (EM-world rule). Full suite: 1,976 tests, 0 failures.
- RIG-REPORT.md — rig verdict RIG GREEN for candidate 404025cc…
- INSTALL-PACKET.md — the install packet of record.
- INSTALLED.txt / OUTCOME.txt / SCRIPT-OUTPUT.txt — install evidence
  (boot Done line, 0 SEVERE, guild state unchanged 5/5).

## Notes

- R6 finding (from the prep of record): the Magic mmob admin path carries
  no PDC stamps; no player exposure; accepted for this install.
- Source of record: the patched working tree at
  mcmmo-xpfix-build-20261115/repo/ (git HEAD 058279c14, uncommitted —
  11 modified + 10 untracked entries). The Drive Source deposit snapshots
  that working tree; the three diffs above are this PR's payload.
- Publication claim: wrv-plugins PR #___ (duplicate check run
  2026-11-16 01:29:05 CST, result NO MATCH / exit 0) — completed claim
  line with the PR number is in the close-out record (PUBLISH.md,
  board run 418). This PR stays an OPEN DRAFT, never merged.
