# ExcelRaidBuff 0.1.2 name scrub (staged, NOT installed)

Rank: before-next-seal (hygiene). The shipped config.yml comment, an admin status line ("<name> rule: ...")
in /raidbuff output, source comments and a test class name used a child's first name.

## What changes
- Comments and tests use "Rowan"; the test class is now RowanCompensationTest (run-tests.sh updated).
- The admin status line now reads "Compensation rule: ...".
- Only ExcelRaidBuff.class (that string) and config.yml (comment) differ from the 0.1.2 rebuild.
- Originals are in pre-scrub-bak/; orig/ holds the old text. Keep both out of any repo. No diff shipped (same reason).

## Artifact
- build/ExcelRaidBuff-0.1.2-scrubbed.jar  sha256 1cee2856fb11fe4c614b5c54ea4aae2da5ba13de87dbe47239766bc818c70de3 (reproducible). Version unchanged on purpose:
  bump to 0.1.3 if Mike prefers a new version number for a jar change.
- Not compared with the live jar (no live RaidBuff jar on the box).

## Checks
1. All 9 test suites pass on orig and fix (orig-tests.log, fix-tests.log; 316 checks).
2. Review: class comparison (above) and a name scan of fix/ (clean). No rig run: the only behavior change is one admin text line.

## Install record (when approved; not done)
- Back up the live jar and the live plugins/ExcelRaidBuff/config.yml as .bak-<date> (keep both).
  The live config keeps the old comment until it is replaced; values are unchanged, so copying the
  scrubbed comment block in by hand is enough. Rollback: restore the .bak files.
