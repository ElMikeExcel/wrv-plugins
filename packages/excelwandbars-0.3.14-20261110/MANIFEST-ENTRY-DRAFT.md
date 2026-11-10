# MANIFEST ENTRY DRAFT — ExcelWandBars 0.3.14 (form contrast) — NOT FILED

Drafted Nov 10, 2026 by the build desk for the board supervisor to file
in ~/workspace/wrv-install-queue/MANIFEST.md. The build desk made NO
MANIFEST edit, per commission. Desk state: built + proven, NOT installed.

## PENDING — ExcelWandBars 0.3.14 (form contrast) — gated jar swap

- **Artifact:** `ExcelWandBars-0.3.14.jar` — 545,907 B, SHA-256
  `c87f6005f92539993d05ec8f9f52726a0b3c99a129c36f94153f7fd723774d63`,
  build id `EXC-WANDBARS-0.3.14-WRV-20261110-01-5a68bd`.
  Staged at `~/workspace/wandbars-form-contrast-build/build-0.3.14/`
  (BUILD-RECORD.md, SOURCE.md, TEST-RESULTS.txt) and in the records
  package `~/workspace/records-catchup-20261103/packages/excelwandbars-0.3.14-20261110/`.
- **Live jar gate (STOP if mismatch):** the live WandBars jar is the
  0.3.13 content — 544,853 B, SHA-256
  `7278f09ed160236b54c02d9dfaa590feeb1ba25027c901b3a17217848a1327e3`.
- **Scope:** JAR-ONLY swap. `config.yml` in the candidate is
  byte-identical to 0.3.13's shipped config — no config step.
  `spellbook.yml` is byte-identical to 0.3.13's and is NOT swapped by
  the repack; this packet does not touch the tame-fix packet's staged
  spellbook data change (separate packet, either install order is safe).
- **Change (one line):** Bedrock form contrast — locked Spell Book rows
  print in dark ink instead of pale §7 grey, and the form renderer
  (`BedrockForms`) normalizes every button label's color codes to
  black, so no WandBars form button can render pale-on-light or
  pale-on-green again (Oct 20, 2026 ExcelMenu defect class; Mike's
  Nov 9, 2026 Summon-form screenshots).
- **Gates:** BACKUP FIRST + cap-at-6 + emptiness gate per standing
  rules (jar change = fully gated, NOT a text live-edit). Boot verify:
  `Enabling ExcelWandBars v0.3.14`, build line
  `EXC-WANDBARS-0.3.14-WRV-20261110-01-5a68bd`, SEVERE 0.
- **Rollback:** restore the pre-install 0.3.13 jar from the window's
  backup; no data or config rollback needed.
- **Player proof owed (deciding evidence, do not pre-claim):** Mike
  reopens the Summon form on Bedrock — locked rows ("Tame Pet",
  "Cat — unlocks at Apprentice") readable BOTH idle and while
  highlighted/selected; a group page without locked rows looks
  unchanged. Secondary: any other WandBars form (Wand controls
  chooser, spell detail) opens normally.
- **Same-pass surfaces owed at install close-out:** What's New note
  BOTH surfaces (website section + in-game ExcelWhatsNew changelog)
  via the registry pipeline; Guide knowledge line (locked spells are
  named in dark ink with their unlock rank in words; no content
  change beyond that line); tips check (no WandBars contrast tip
  exists — a one-line "locked spells say their unlock rank on the
  row" tip is optional, board call); menus — no change (no new
  commands or settings).
- **Records:** package staged (see artifact line); Drive deposit +
  GitHub draft record PR are the parent/board legs per PUSH-NOTES.md
  in the package. Record PRs stay OPEN DRAFTS, never merged.
