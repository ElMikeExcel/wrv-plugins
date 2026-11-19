# PENDING - ExcelDowned 1.0.2 (open Revive Potion targeting)

- Packet: ~/workspace/downed-102-partyhint-20261119/INSTALL-PACKET.md
- Build record: ~/workspace/downed-102-partyhint-20261119/BUILD-REPORT.md
- Filing record: ~/workspace/downed-102-partyhint-20261119/FILING-RECORD.md
- Filed: Nov 19, 2026 (worker build; MANIFEST write is the
  supervisor's alone)

## Decision (stated prominently, per the filing instruction)

**ExcelDowned 1.0.2 SUPERSEDES design S-1's party-only Revive
Potion targeting, per Mike's decision of Nov 19, 2026, 00:07 CST:**
"I like the idea of being able to just use it on anyone you come
across to be helpful." The potion now works on ANY downed player.
Party membership is not consulted anywhere on the potion path.

## Candidate

- ~/workspace/downed-102-partyhint-20261119/ExcelDowned-1.0.2.jar -
  139,867 B / SHA-256
  8237327f781c6a63e78637153d4e1b75b52d34cc20169e3f30538bef9085609f
- Build: EXC-DOWNED-1.0.2-WRV-20261119-01-d0wn3d
- Tests: 79/79 PASS (77 carried from the 1.0.1 suite + 2 new 1.0.2
  tests), double-build byte-identical (jar == repro).
- Load check: VM-local Paper 26.2 rig PASS (D122) — Done (48.600s),
  SEVERE 0, ExcelItems enables before ExcelDowned v1.0.2, registry
  behavior line for ID 4 present, ready line carries the 1.0.2
  build ID. Log: work/vm-rig-boot.log (6ac29f48...). The Homebase
  mirror was NOT used: it sits in found state under the idle-queue
  calibration lane tonight; the VM-local rig is the same substitute
  M1's G-4 used.
- Source tree: work/build-1.0.2/ (DESIGN-1.0.2.md carries the
  change write-up).

## Live-premise / live-jar gate (STOP on mismatch)

- 1.0.2's premise is **ExcelDowned 1.0.1 INSTALLED** (the M1
  migration build). At install provenance, live
  plugins/ExcelDowned-1.0.1.jar must equal 140,323 B /
  9e8ae0f5b297045a5e913e2c8e7c3e2cccb858c1fe32bbed6e8bb2f7566c32b3
  (re-pull fresh at the gate; never cite this line as the gate).
- **If live still runs 1.0.0, DO NOT install 1.0.2** — M1
  (ExcelDowned 1.0.1, PENDING above this entry, itself D125-gated)
  installs first. This sequencing gate is logical per the D122
  amendment: it protects against landing a migration-premised jar
  on the wrong base (1.0.2's code IS 1.0.1's code plus this change;
  there is no 1.0.0-based 1.0.2).

## Scope

- Changes: jar swap 1.0.1 -> 1.0.2 only. Party membership removed
  from the Revive Potion path at all three consult points:
  prompt policy (RevivePotionPolicy.promptEligible), the
  right-click interact (RevivePotionUi.onInteractEntity), and the
  confirm verdict (RevivePotionPolicy.confirmVerdict — the
  NOT_PARTY refusal, its enum value, and its UI message are
  removed, no dead code). Potion-facing copy updated to match
  (plugin.yml description, two DownedCommands lines, the
  downed-player message).
- Does NOT change: config.yml (byte-identical to the 1.0.1 tree's,
  never replaced), data files, $5,000 shop price, passive carry,
  consumption-after-land, 50% HP revive, 10-minute per-carrier
  potion cooldown, totem-first deferral, 300 s bleed-out,
  self-revive exclusion, ranges, buy/give verb split, the
  Resurrection spell's party rule (MagicBridge — separate code),
  **the Cleric Raise Ally gate (explicitly out of scope — a
  separate question for Mike)**, ExcelItems (live 0.1.0 stays).

## Same-pass surfaces (staged in this packet)

- What's New registry delta (drives BOTH the website What's New
  and the in-game ExcelWhatsNew entry at the window):
  staged/whatsnew-registry-delta.yml — carries the player-facing
  line verbatim: "Revive Potions now work on any downed player —
  no party needed." If the install lands on a later date than the
  filing date, the installer sets `posted` to the install date.
- Website knowledge (ai-knowledge.json) draft:
  staged/website-copy.md.
- Guide leg: SKIPPED — Guide development is hibernated (Mike,
  Nov 16, 2026); website stays the canon.
- Menus/tips: no menu references the potion's party rule (the
  potion is bought in the Excel Shop and used by right-click);
  no tips change. Checked at filing.

## Marker + proof

- Marker: ~/workspace/downed-102-partyhint-20261119/INSTALLED.txt
- Proof owed after install: none gated (D122). Post-release live
  check per the standing pattern: a carrier revives a downed
  non-party player once on live and the records JSONL shows the
  potion_revive line.
