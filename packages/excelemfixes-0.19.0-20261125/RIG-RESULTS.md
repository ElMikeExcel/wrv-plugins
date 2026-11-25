# excelemfixes-0.19.0-20261125 — RIG RESULTS

Rig proof (local test rig only, Nov 25, 2026). Evidence log:
~/workspace/excelemfixes-beacon-r2b-20261125/rig/boot-r2b.log —
VERDICT: PASS, clean boot.

- Rig plugins/ExcelEMFixes-0.19.0.jar hashes to the candidate hash
  a499891d3bef50937e8d41e41288c547495b33ddd08d501602f3c286cdb3ac13
  (verified by sha256sum).
- "[ExcelEMFixes] Enabling ExcelEMFixes v0.19.0"
- "[ExcelEMFixes] ExcelEMFixes 0.19.0 ready" with all 22 modules
  listed, incl. quest-destinations, quest-beacon and
  quest-giver-glow.
- "Done (75.409s)! For help, type "help"" at 17:45:05 UTC, then a
  clean RCON stop at 17:46:10 UTC.
- A second rig boot log (rig/boot-r2b2.log) shows the same jar
  loading as ExcelEMFixes (0.19.0) alongside EliteMobs (10.9.14).

Note: rig/boot-finishonly.log is NOT a verdict — it ends mid-boot at
"Preparing spawn area: 21%" on the older Paper 26.1.2 rig jar and
never reaches Done. The filing lane ran no fresh boot because
boot-r2b.log already establishes the clean boot of this exact jar.

Unit suites (rig/test-rerun-finishonly.log): TOTAL: 1,192 passed,
1 failed. The one failure is "Jester-shaped: armor named first on
the tie" in the skill-nudge checks — the documented pre-existing
tie-break flake carried in prior ExcelEMFixes packets, unrelated to
the quest-destinations change.

Live acceptance (install window wrv-install-failsafe-20261125-1333):
boot Done (34.161s), "ExcelEMFixes 0.19.0 ready" with 22 modules,
emfixes reload ZERO reload-failed, SEVERE 0. Confirmatory live proof
owed: Iris staging check on a no-table-row dialog quest (King
Nemian) + FEEDBACK-WATCH on Jester's next dialog-target quest.
