# PUSH-NOTES — excelmagicgate-0.3.1-20261110

Staged by the 0.3.1 desk build (board-supervisor chain). The
build performed NO publishing: no GitHub push, no Drive deposit,
no MANIFEST filing, no install. The supervisor owns those legs.

Deposit contents = the records package folder in this build
folder, as-is:
`records/excelmagicgate-0.3.1-20261110/`
- ExcelMagicGate-0.3.1.jar (243885 B, SHA-256 454186c0dd145a57af08757d88b4de92cbd55551c5bdd71bc5677806e9bf42a2)
- BUILD-RECORD.md / SOURCE.md / PACKET.md / FINGERPRINTS.md / RIG-VERDICT.md
- TEST-RESULTS.txt / TEST-OUTPUT.txt
- PUBLICATION-NOTE.md
- build.sh / run-tests.sh / config.yml / plugin.yml / excel-build.txt
- src/ (18 main sources — WandLoreHook.java carries the only
  behavior change), stages/ (6 stage packets, byte-identical to
  amended 0.3.0), tests/ (ExcelMagicGateTest.java, PlacementTool.java)
- FILE-LIST.txt / SHA256SUMS

Supervisor checklist:
1. File the MANIFEST entry from this build folder's
   MANIFEST-ENTRY-DRAFT.md as PENDING (supersedes nothing filed —
   the amended 0.3.0 entry converts to INSTALLED on its own
   record; 0.3.1 is the next version in line).
2. Consider the FINGERPRINT-REGISTRY row for this build ID
   (EXC-MAGICGATE-0.3.1-WRV-20261110-01-7a0311).
3. Install strictly per INSTALL-PACKET.md (live gate = the
   amended -02 jar, 243572 B / 4cab2689…; config and
   earned.yml untouched; lore spot-check at boot verification).
4. Same-pass surfaces at install close-out are the installer's
   lane (Guide / What's New both surfaces / tips / menus check) —
   the visible change is the wand-lore milestone lines only.
