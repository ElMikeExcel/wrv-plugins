# PUSH-NOTES — excelmagicgate-0.3.2-20261112

Staged by the 0.3.2 desk build (board-supervisor chain). The
build performed NO publishing: no GitHub push, no Drive deposit,
no MANIFEST filing, no install. The supervisor owns those legs.

Deposit contents = the records package folder, as-is (staged both
in this build folder's `records/excelmagicgate-0.3.2-20261112/` and
under `~/workspace/records-catchup-20261103/packages/excelmagicgate-0.3.2-20261112/`):
- ExcelMagicGate-0.3.2.jar (247078 B, SHA-256 895cf91dfd28bcca0c493dc2ef69fcc903257001ea1790bb70fcae3b0a81fb68)
- BUILD-RECORD.md / SOURCE.md / PACKET.md / FINGERPRINTS.md / RIG-VERDICT.md
- TEST-RESULTS.txt / TEST-OUTPUT.txt
- PUBLICATION-NOTE.md / MANIFEST-ENTRY-DRAFT.md
- build.sh / run-tests.sh / config.yml / plugin.yml / excel-build.txt
- src/ (19 main sources — the new SpellTemplateGate.java plus the
  MagicFloorHook.java gate carry the only behavior change),
  stages/ (6 stage packets, byte-identical to 0.3.1),
  tests/ (ExcelMagicGateTest.java, PlacementTool.java)
- FILE-LIST.txt / SHA256SUMS

Supervisor checklist:
1. File the MANIFEST entry from this build folder's
   MANIFEST-ENTRY-DRAFT.md as PENDING. NOTE: its live-state claims
   are GATE-VERIFY by design (this build ran no live pulls) —
   re-base the predecessor identity from a fresh pull at filing.
2. Consider the FINGERPRINT-REGISTRY row for this build ID
   (EXC-MAGICGATE-0.3.2-WRV-20261112-01-9c032b).
3. Install strictly per INSTALL-PACKET.md (live-jar gate
   GATE-VERIFY; BACKUP FIRST + cap-at-6; emptiness gate; config
   and earned.yml untouched; gate spot-check at boot verification
   — no WARNING on a healthy wand population is the expected
   result per the run-228 pull).
4. Same-pass surfaces at install close-out are the installer's
   lane (Guide / What's New both surfaces / tips / menus check) —
   there is NO player-visible change in this build; the surfaces
   work is the install note itself.
5. NOT in this build, still owed as separate packets: the
   diagnosis Fix 2 data cure (if a dead key is ever named) and
   the §6 wand-name messages.yml rename (all stage copies + live).
