# WRVWandDiagnostic 0.1.0 — INSTALL RECORD (reconstructed Nov 17, 2026)

Gap class G-A: pre-manifest / no MANIFEST lineage. No `## INSTALLED`
heading names this plugin in MANIFEST.md or MANIFEST-ARCHIVE.md
(word-boundary grep Nov 17, 2026: 0 hits), and the exact install date is
not recorded anywhere found — the plugin was already booting by
Nov 9, 2026 and its candidate package was deposited to Drive
Oct 26, 2026, so installation falls between those dates.

Live evidence (all primary, re-verified Nov 17, 2026):

- Census (Homebase rclone plugins listing, Nov 15, 2026,
  `~/workspace/board-r366/upkeep-a9/pulls/plugins-top.txt`):
  `WRVWandDiagnostic-0.1.0.jar` + `WRVWandDiagnostic/` data folder.
- Census (Nov 16, 2026,
  `~/workspace/board-r420/b11-rebuild/census/plugins-top.txt` lines
  487–488): same jar + folder.
- Boot log Nov 15, 2026 00:47 CST (preserved in
  `~/workspace/board-r394/summons-mana-build/packet/window-20261115-1833/latest.log`):
  `[WRVWandDiagnostic] Loading server plugin WRVWandDiagnostic v0.1.0-offline-diagnostic`
  and `Enabling WRVWandDiagnostic v0.1.0-offline-diagnostic`.
- Boot log Nov 17, 2026 09:45 CST
  (`~/workspace/board-r468/failsafe-20261117-0421/latest-after.log`):
  same Loading + Enabling lines.
- Nov 9, 2026 daily log: counted as the 79th booting plugin in the
  website plugin-table census; described there as a behind-the-scenes
  diagnostic in the AuditLog/MenuStats precedent class.

Owed (named, not hidden): a fresh live pull of
`plugins/WRVWandDiagnostic-0.1.0.jar` hash-checked against the candidate
jar `a234c934…` — not taken in the recovery run; the census + boot
evidence above proves presence and content version, not jar bytes.
