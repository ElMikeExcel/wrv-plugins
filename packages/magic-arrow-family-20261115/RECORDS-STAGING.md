# RECORDS STAGING — Arrow family balance fix (Magic spells.yml x5)

Staged: 2026-11-15 (board supervisor run 391, worker W8).
PREP ONLY — no GitHub push, no Drive upload, no MANIFEST
edit was made by this worker.
Install: INSTALLED Nov 15, 2026, wrv-install-failsafe
09:03 CST full-push chain (Install 3 of 13); landing
verified PASS in run 390 (VERIFY.md in this package).

## Adjudication — PUBLISH (verdict)

Decision: YES — this config-only fix DOES get a
wrv-plugins records package, as an OPEN DRAFT PR, never
merged. Drive deposit: NOT APPLICABLE (no jar).

- Precedent FOR (controlling): run-365
  records-applicability §1 R2 — a config-only change that
  IS the standalone work product gets a records PR. The
  decided case is the SpellBook regroup (config-only,
  PR #216, package 38 files): its RECORDS-STAGING.md
  states verbatim "Decision: YES — config-only fixes DO
  get a wrv-plugins records package", citing
  records-catchup-20261103 JOBS.md row 27 (Magic spell
  packets #1–#3 + Familiar/Cat cooldown config, 43 files,
  draft record PR) and Magic stage-files PR #91. The
  distinguishing feature: multi-file Magic config state
  that is new live state held in no other canonical
  record.
- Arrow matches that feature exactly: 5 placed
  spells.yml files, 42 changed arrow_damage lines, across
  live + the 4 ExcelMagicGate stage templates. The placed
  state exists canonically nowhere else — the packet /
  install evidence folders are working records, and the
  What's New registry (surfaces leg, still owed) carries
  only the player-facing text, not the config bytes.
- Packet silence, stated plainly: the Arrow
  INSTALL-PACKET.md names NO records leg — its close-out
  line is "Same-pass surfaces (owed at install close-out,
  parent lane)" only. Silence is not a design decision
  against records: the SpellBook packet was likewise
  surfaces-scoped and its records package was still
  adjudicated YES at staging time.
- Counter-precedent considered and distinguished: the
  golden-shovel listing (run-365 §1 R3) got no PR for a
  shop-config change. It differs on the R2/R3
  reconciliation in run-365 §1: its placed bytes were
  catalog/menu values whose canonical record is the
  What's New registry + website chain that DID land for
  it, and it touched one shop's config surface, not a
  multi-destination spell-state family. Arrow follows
  the SpellBook / Magic-packets line, per the W4 audit
  instruction (READINESS.md §1b) and R2.
- No-jar consequence: no Drive jar/source deposit leg
  exists for this unit (B14 is jar-keyed); the package
  itself is the whole records close-out.

## Package

- Proposed repo path: packages/magic-arrow-family-20261115/
- Proposed branch: record/magic-arrow-family-20261115
- Proposed PR title: Record: Magic arrow family balance
  fix — spells.yml x5 (2026-11-15)
- Local package: this folder — 28 files total including
  FILE-LIST.txt and SHA256SUMS (SHA256SUMS carries 27
  entries, every file except itself). Contents: SOURCE.md,
  this file, INSTALL-PACKET.md, RECEIPTS.md, RESULT.md,
  INSTALLED.txt, OUTCOME.txt, SCRIPT-OUTPUT.txt,
  VERIFY.md, build.py, model_test.py, diffs/ (5),
  evidence/ (5), placed-files/ (5 placed spells.yml).
- Verification at publication: sha256sum -c SHA256SUMS
  inside the package (result recorded in the prep record,
  ~/workspace/board-r391/arrow-records/PREP.md).

## Duplicate guard (G2)

Cited, not re-run (per task instruction — the guard was
run this run by the W4 audit worker):
~/workspace/workflow-tools/pr-exists.sh wrv-plugins arrow
at 2026-11-15T17:26:54Z — exit 0, "NO MATCH: no open/draft
PR in ElMikeExcel/wrv-plugins matches 'arrow'".
Publication claim: wrv-plugins PR #___ (duplicate check
run 2026-11-15T17:26:54Z, result NO MATCH for 'arrow') —
PR number filled in at publication time by the
supervisor's publish leg, not in this file.

## Still owed (outside this package)

- Publication itself: a later supervisor leg via the
  Homebase native-git lane (OPEN DRAFT, never merged).
- Surfaces staging/filing for the Arrow fix (What's New
  provisional seq 18, Guide chunk, website spell numbers)
  — a separate leg, per READINESS.md §1c.
- Player proof: Jester recast set — player lane; gates
  Proven status, not this records close-out.
