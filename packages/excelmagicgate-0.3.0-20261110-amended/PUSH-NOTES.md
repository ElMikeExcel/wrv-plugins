# PUSH-NOTES — excelmagicgate-0.3.0-20261110-amended

Staged by the 0.3.0 AMENDMENT build (board-supervisor dispatch,
decide-and-log D2). The build performed NO publishing: no GitHub
push, no Drive deposit, no MANIFEST filing, no install. The
supervisor owns those legs.

**Sequencing vs PR #172:** the unamended 0.3.0 records package
(`excelmagicgate-0.3.0-20261110/`) was already being uploaded (PR
#172) when this amendment was built. This amended package is staged
as a SIBLING folder (`excelmagicgate-0.3.0-20261110-amended/`) and
must NOT overwrite the in-flight package; its publication follows as
an UPDATE to the same record branch. The amended build is the one
that installs; the unamended build (-01) is superseded pre-install.

Deposit contents = the amended package folder, as-is:
- ExcelMagicGate-0.3.0.jar (243,572 B, SHA-256 4cab2689d073a2a4e7141a31c24fd1a05cd7011fdf846a2fd84193e5a1836384)
- BUILD-RECORD.md / SOURCE.md / PACKET.md / FINGERPRINTS.md / RIG-VERDICT.md
- TEST-RESULTS.txt / TEST-OUTPUT.txt
- PUBLICATION-NOTE.md (the PR #172 sequencing note above)
- build.sh / run-tests.sh / config.yml / plugin.yml / excel-build.txt
- src/ (18 main sources — WandInventorySync.java is new in the amendment),
  stages/ (5 stage packets, byte-identical to 0.2.1),
  tests/ (ExcelMagicGateTest.java, PlacementTool.java)
- FILE-LIST.txt / SHA256SUMS (regenerate FILE-LIST after any addition)

Supervisor checklist:
1. File the MANIFEST entry from the amended build folder's
   MANIFEST-ENTRY-DRAFT.md — it SUPERSEDES the unamended 0.3.0
   PENDING draft wholesale; never file both.
2. Consider the FINGERPRINT-REGISTRY row for this build ID
   (EXC-MAGICGATE-0.3.0-WRV-20261110-02-d2a310) — the -01 row should
   be skipped (superseded pre-install) or marked superseded.
3. Install strictly per PACKET.md (unchanged steps vs the unamended
   packet; amended jar identity; placement STOP gate §8.3;
   earned.yml pre-write before first boot).
4. Player-facing heads-up before the install window: carried
   demotion notes PLUS the one-time "spell book fills in" grant
   (the summoning tree becoming visible is the fix for the Nov 10
   invisibility report).
