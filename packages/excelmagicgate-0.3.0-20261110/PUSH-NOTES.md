# PUSH-NOTES — excelmagicgate-0.3.0-20261110

Staged by the 0.3.0 build (board-supervisor dispatch, run 125 follow-up).
The build performed NO publishing: no GitHub push, no Drive deposit, no
MANIFEST filing, no install. The supervisor owns those legs.

Deposit contents = this package folder, as-is:
- ExcelMagicGate-0.3.0.jar (237,422 B, SHA-256 515fe1c3194e56da734d5bdab257badef3e5074e1ac415ed11a375f100f88018)
- BUILD-RECORD.md / SOURCE.md / PACKET.md / FINGERPRINTS.md / RIG-VERDICT.md
- TEST-RESULTS.txt / TEST-OUTPUT.txt
- build.sh / run-tests.sh / config.yml / plugin.yml / excel-build.txt
- src/ (17 main sources), stages/ (5 stage packets, byte-identical to 0.2.1),
  tests/ (ExcelMagicGateTest.java, PlacementTool.java)
- FILE-LIST.txt / SHA256SUMS (regenerate FILE-LIST after any addition)

Supervisor checklist:
1. File the MANIFEST entry from the build folder's MANIFEST-ENTRY-DRAFT.md.
2. Consider the FINGERPRINT-REGISTRY row for this build ID
   (EXC-MAGICGATE-0.3.0-WRV-20261110-01-5af7d1) — 0.2.1's row is also absent.
3. Install strictly per PACKET.md (live-jar gate, BACKUP FIRST + cap-at-6,
   emptiness gate, placement STOP gate §8.3, earned.yml pre-write before
   first boot — regeneration via tests/PlacementTool.java on a fresh pull).
4. Player-facing heads-up before the install window: demotions land at next
   wand activation; tier variants above the earned rank stop casting until
   re-earned (rig evidence in RIG-VERDICT.md).
