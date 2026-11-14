# P-SPELLBOOK-1 — STAGING RECORD (spellbook regroup fix)

**Date:** 2026-11-14 · **Worker:** board supervisor run 328 desk worker (continuation)
**Scope source:** `~/workspace/spellbook-audit-20261114/AUDIT.md` — 32,697 B / SHA-256
`0a32decb1e052d03841bcd053e04e3b0206fb3a93812c8f21c985917a87e8293` (§10 fix list F1–F5),
with `RECEIPTS.md` beside it. **Grouping mechanism of record:** a spell's book group is
decided ONLY by `plugins/ExcelWandBars/spellbook.yml` categories (ExcelWandBars 0.3.17
`SpellbookGroups.java`; first listing wins; unlisted → "Other").
**Desk staging only:** no live pulls, no installs, no MANIFEST filing (supervisor files).
All bases are the audit's receipted live-true copies (Nov 11–13 lineage, §0 of the audit);
the PC route failed its probes in the audit run and this staging worked from those copies
per task instruction. Every staged base must be re-based on a fresh filing-day pull (G1
list in §7) — the staged hashes below are candidate hashes, not install gates.

---

## 1. F1 — Regroup in `spellbook.yml` (STAGED)

Staged file: `staged/wandbars/spellbook.yml` → destination `plugins/ExcelWandBars/spellbook.yml`.
Diff (`diffs/wandbars-spellbook.diff`): **+13 / −4 lines, spell lines only.**

Moves (exactly audit §10 F1, plus the two F2 Healing listings from §3):
- Summon: ADD `archer`, `bruiser`, `guardian`, `revenant`, `ghast`, `mount`, `tamepet`
  (appended after `tame`) → **11** = familiar, wolf, cat, tame, archer, bruiser, guardian,
  revenant, ghast, mount, tamepet.
- Damage: REMOVE `flare`, `counterspell`; ADD `earth` (from Building), `anvil` (from
  Utility) → stays **32**.
- Building: REMOVE `earth` → **8**. Utility: REMOVE `anvil`; ADD `flare`, `counterspell`
  → **23**. Healing: ADD `grouprenewal`, `resurrection` (F2 listings) → **12**.

Post-fix group counts **by counting** (verify.py over the staged file; full output in
`VERIFY-OUTPUT.txt`):

| Group | Base listings | Staged listings |
|---|---|---|
| Healing | 10 | **12** |
| Defense | 8 | 8 |
| Damage | 32 | 32 |
| Movement | 14 | 14 |
| Summon | 4 | **11** |
| Building | 9 | **8** |
| World | 6 | 6 |
| Utility | 22 | **23** |
| **Total listings** | **105** | **114** |

**Audit discrepancy (flagged, counted value stands):** AUDIT.md §7/§10 print post-fix
Utility as 24. Counting gives **23** (22 − anvil + flare + counterspell = 23); the audit's
own total reconciles only with 23 (105 + 7 summon + 2 healing = 114 listings). This is an
arithmetic slip in the audit prose, not a scope difference — the staged file implements
exactly the moves the audit names.

Player-visible book, counted the audit's way (cumulative stage4 paths + the live
supplemental-grant map `student:[archer]`, `apprentice:[bruiser]`,
`master:[guardian, revenant, ghast, mount]` from ExcelMagicGate 0.3.2
`WandInventorySync.java`):

| Rank | Known before | Known after | Summon after | Other after |
|---|---|---|---|---|
| Beginner | 26 | 26 | 0 | **0** |
| Student | 58 | 58 | 4 | **0** |
| Apprentice | 80 | 81 (+grouprenewal) | 7 | **0** |
| Master | 112 | 114 | **11** | **0** |

Control: the same counter over the BASE files reproduces the audit exactly (Master 112 /
Summon 4 / Other 7; Apprentice 80 / Summon 4 / Other 3 = Mike's Nov 14 screenshots), so the
post-fix numbers are produced by a counter validated against ground truth.

## 2. F2 — Grant path for `grouprenewal` (Apprentice) + `resurrection` (Master) (DESIGNED + STAGED)

**Design decision (config route, not code):** grant both spells through the rank
`paths.yml` + `wands.yml` lists — the standing mechanism ExcelMagicGate's
WandInventorySync reconciles onto wands (audit §1) — instead of adding them to the
supplemental-grants map. Reasons: (a) config-only, no MagicGate jar rebuild; (b) spells on
the template ladder also appear in the locked/gray view for lower ranks, while
supplemental spells never do (audit check 6) — the config route gives both orphans a
locked preview for free; (c) it matches the tame-fix precedent (paths + wands edited
together). Existing wands pick both spells up from the standing sync; no per-wand grant
pass is staged or needed. The supplemental map is untouched.

**Insertion rule (uniform, diff-verified):**
- `grouprenewal`: in the **apprentice** block, `- grouprenewal` immediately after
  `- greaterheal` (audit recommendation: Apprentice, beside superheal/greaterheal in the
  heal line; greaterheal is the last heal-line entry in every apprentice list).
- `resurrection`: in the **master** block, `- resurrection` immediately after
  `- layonhands` (audit recommendation: Master, its stage-4 unlock tier;
  resurrection's own `upgrade_required_path: master` agrees).

**Stage applicability (evidence-bound, counted in verify.py):** a template set only
receives a spell that exists in that stage's spell book.

| Template set | grouprenewal | resurrection | Evidence |
|---|---|---|---|
| stage3 paths+wands | YES | **NO** | grouprenewal in stage3 book; resurrection ABSENT from stage3 book (also no layonhands anchor in stage3 master lists) |
| stage4 paths+wands | YES | YES | both in stage4 book |
| stage5 paths+wands | YES | YES | both in stage5 book |
| live Magic paths+wands | YES | YES | live book == stage4 book |

No stage2 template set is staged: no receipted stage2 MagicGate template exists in the
audit evidence (receipts cover stage3/4/5 templates + live Magic only), and neither spell
exists in the stage2 book — named here as not-applicable, not silently skipped.
**Healing listings:** both spells are listed under Healing in the staged spellbook.yml
(§1), so once granted they shelve in Healing, never Other. Banding verified by counting:
grouprenewal first appears at Apprentice cumulative, resurrection first at Master
cumulative; stage4 path cumulative counts move 78 → 79 (Apprentice) and 106 → 108 (Master).

## 3. F3 — `tamepet` display rename → "Pet Heal" (STAGED)

`  name: Pet Heal` inserted as the first child line of the `tamepet:` block in **all five**
staged spell books (the key is untouched — display-only, the Nov 8 rename shape). With F1
placing tamepet in Summon, the pair reads Tame (tame a wild animal) vs Pet Heal (heal your
tamed pet). Files: see §6 (live, stage2, stage3, stage4, stage5 books).

## 4. F4 — Display names for the six WRV summons (STAGED, per-book matrix)

`  name: <Title>` inserted as the first child line of each base block, **only where the
base spell exists in that book** (absent spells stay absent — they are seal-gated per
stage; inventing them would corrupt the stage books). Tier keys (`archer|2` etc.) get no
name line; the base entry is the display-name source.

| Book | archer | bruiser | guardian | revenant | ghast | mount | tamepet (F3) | Name lines added |
|---|---|---|---|---|---|---|---|---|
| live | Archer | Bruiser | Guardian | Revenant | Ghast | Mount | Pet Heal | 7 |
| stage2 | Archer | — absent | — absent | — absent | — absent | — absent | Pet Heal | 2 |
| stage3 | Archer | Bruiser | — absent | — absent | — absent | — absent | Pet Heal | 3 |
| stage4 | Archer | Bruiser | Guardian | Revenant | Ghast | Mount | Pet Heal | 7 |
| stage5 | Archer | Bruiser | Guardian | Revenant | Ghast | Mount | Pet Heal | 7 |

Total F3+F4 name lines: **26**, additions only, zero deletions in all five books
(diff-verified). Top-level key counts unchanged (222 / 109 / 176 / 222 / 222).

## 5. F5 — Locked-view supplemental gap: OWED ITEM, NOT BUILT

**P-SPELLBOOK-F5 (owed, plugin-level — owner lane: supervisor to queue as a separate
WandBars build item; does NOT block F1–F4):** WandBars' locked entries are built from wand
templates only, so the six supplemental-grant spells (archer, bruiser, guardian, revenant,
ghast, mount) never preview as locked for lower ranks (audit check 6). Fix shape per the
audit: include the MagicGate supplemental map (or an equivalent config list in WandBars)
in the locked/band computation. This staging deliberately does **not** build it — it needs
a WandBars code change + build + rig proof, a separate packet. Note the F2 config route
(§2) already gives grouprenewal/resurrection locked previews; the remaining F5 gap covers
only the supplemental six.

## 6. Staged files — destination, base (receipted), staged size + SHA-256, delta

Bases: spellbook + stage paths/wands from `tame-rebase-20261110/staged/` (installed Nov 11,
pull-back byte-verified per audit §0); live Magic paths/wands from
`tame-rebase-20261110/verify-20261111/pulled/magic-{paths,wands}.yml` (byte-identical to the
stage4 staged copies); spell books from
`spell-audit/packet-lightning-timing-20261111/staged/` (Lightning install Nov 11, live-true
per audit §0). Spell-book destinations per that packet: live receives the stage4 bytes at
`plugins/Magic/spells.yml`; stage books land at the ExcelMagicGate stage2/3/4/5 template
`spells.yml` files.

| Destination | Base size / SHA-256 | Staged file | Staged size / SHA-256 | Delta |
|---|---|---|---|---|
| plugins/ExcelWandBars/spellbook.yml | 2,547 / d3bc78d4a9f00f2d23ae075f0067bda95ae4cf4881941fba7bba2cfded10c9f8 | staged/wandbars/spellbook.yml | 2,698 / 756c5e3a63fbafd01348fc5ee7d031ccb3b2c22e819ab63c190a913a5e415db1 | +13/−4 |
| plugins/Magic/spells.yml | 188,164 / 680068315978206af96f87861fba58d1c47b88ba559edafdcb2704251226ac77 | staged/spells/live-spells.yml | 188,274 / 75877195b4991f48dd976698fbb53dca3800633dd8230d65a13022bfa2258abf | +7 name lines |
| ExcelMagicGate stage2 template spells.yml | 89,176 / 5312fdd1227182174df3b477f3a1c2cc682f4d213bbfd8e157eb56c82f3fdf9e | staged/spells/stage2-spells.yml | 89,208 / 9be7eda6444471b526c0b20b28ee38a1efa0f25a0b474ff003cd21e9d78f97a5 | +2 name lines |
| ExcelMagicGate stage3 template spells.yml | 145,625 / 0bb76856f8212f9d9c131b2d0755c3676de435833a5effa8bf6c1e183157e837 | staged/spells/stage3-spells.yml | 145,673 / 682ab22f57b99e78b6c668e92d1fa8f43893569bf92bae34d8602fde2fb08be7 | +3 name lines |
| ExcelMagicGate stage4 template spells.yml | 188,164 / 680068315978206af96f87861fba58d1c47b88ba559edafdcb2704251226ac77 | staged/spells/stage4-spells.yml | 188,274 / 75877195b4991f48dd976698fbb53dca3800633dd8230d65a13022bfa2258abf | +7 name lines (byte-identical to staged live, as its base was) |
| ExcelMagicGate stage5 template spells.yml | 188,164 / ad10c4eb9dc24ceaaa25fef32b912df51526fc1ea9fed653e639a208d79754f1 | staged/spells/stage5-spells.yml | 188,274 / 66ebb58cabb58b30a5cc611cf2b376b8c449c4511179c60805e769d33fbe44a7 | +7 name lines |
| plugins/ExcelMagicGate/stages/stage3/paths.yml | 2,629 / 1fb6d5ad30cb455c58eff9a8f8b2d07529464c16908084007d7353ef7f72be2e | staged/magicgate/stage3/paths.yml | 2,646 / 8da818eed48dab93e7821a257c042bdd0ba607a2ecc6047669e6c3e1f1902753 | +grouprenewal |
| plugins/ExcelMagicGate/stages/stage3/wands.yml | 5,857 / e3bfc5c11204af068bf044246fefcbc80a087a2fba89bf02fccf97c630fc3ed8 | staged/magicgate/stage3/wands.yml | 5,874 / 60b67eb90dfe4fe07a9962fd1288e0c9c234f2af918346a7dc5d670b3baef460 | +grouprenewal |
| plugins/ExcelMagicGate/stages/stage4/paths.yml | 2,983 / 78c887929828a5cc0b1776e68cd15efae3cb8afb16b7ff9b0c3eeb5c0ade96a3 | staged/magicgate/stage4/paths.yml | 3,017 / 42829227d0ca2629055d87d55b69234f54d3a4e0d88aa703479e35640baa14c7 | +grouprenewal, +resurrection |
| plugins/ExcelMagicGate/stages/stage4/wands.yml | 6,648 / 744cb385292cfb570978fb0b4fdfb7dd76948b6f3e1364c8096cdb03c044af2d | staged/magicgate/stage4/wands.yml | 6,682 / 8359247d2ca81e786327b41562150fc78b2cca36c42c4a7ca0514be9927aeb47 | +grouprenewal, +resurrection |
| plugins/ExcelMagicGate/stages/stage5/paths.yml | 2,983 / 78c887929828a5cc0b1776e68cd15efae3cb8afb16b7ff9b0c3eeb5c0ade96a3 | staged/magicgate/stage5/paths.yml | 3,017 / 42829227d0ca2629055d87d55b69234f54d3a4e0d88aa703479e35640baa14c7 | +grouprenewal, +resurrection |
| plugins/ExcelMagicGate/stages/stage5/wands.yml | 6,648 / 744cb385292cfb570978fb0b4fdfb7dd76948b6f3e1364c8096cdb03c044af2d | staged/magicgate/stage5/wands.yml | 6,682 / 8359247d2ca81e786327b41562150fc78b2cca36c42c4a7ca0514be9927aeb47 | +grouprenewal, +resurrection |
| plugins/Magic/paths.yml | 2,983 / 78c887929828a5cc0b1776e68cd15efae3cb8afb16b7ff9b0c3eeb5c0ade96a3 | staged/magic/paths.yml | 3,017 / 42829227d0ca2629055d87d55b69234f54d3a4e0d88aa703479e35640baa14c7 | +grouprenewal, +resurrection |
| plugins/Magic/wands.yml | 6,648 / 744cb385292cfb570978fb0b4fdfb7dd76948b6f3e1364c8096cdb03c044af2d | staged/magic/wands.yml | 6,682 / 8359247d2ca81e786327b41562150fc78b2cca36c42c4a7ca0514be9927aeb47 | +grouprenewal, +resurrection |

(Staged live-Magic and stage4/5 paths/wands converge to identical bytes because their
receipted bases were identical and the edits are identical — expected, not an error.)
Unified diffs for all 14 files: `diffs/` (one per destination). Verification script:
`verify.py` (13,286 B / fdd057289c11be908ffbe88733ee07143b5d17bdca672163a30604a29111c13d);
its full output `VERIFY-OUTPUT.txt` (8,373 B /
89c3bc9830fc553ad146b6d988d12a99d11841a3a5c2e7ca7315fcbad4438159) ends
**"RESULT: ALL CHECKS PASS"** — control reproduction, post-fix counts, delta purity,
name matrix, YAML validity, banding, stage applicability. `SHA256SUMS` in this folder
covers all 28 staged/diff files + verify.py + VERIFY-OUTPUT.txt (30 entries);
`sha256sum -c SHA256SUMS` was run at staging time and returned **OK for every entry**
(this record itself is excluded from the sums to avoid a self-reference).

## 7. Filing-day G1 pulls still owed (STOP gates for the supervisor's packet)

Fresh same-day live pulls (path, size, SHA-256, timestamp) are owed for **every**
destination in §6 before filing — the staged bases are Nov 11–13 live-true lineage and
never satisfy an install gate:
1. `plugins/ExcelWandBars/spellbook.yml`
2. `plugins/Magic/spells.yml` + the four ExcelMagicGate stage template `spells.yml` files
   (stage2/3/4/5)
3. `plugins/Magic/paths.yml` and `plugins/Magic/wands.yml`
4. `plugins/ExcelMagicGate/stages/stage3|stage4|stage5/paths.yml` and `wands.yml` (6 files)
5. The active guild-stage state (which stage template set is live at filing) — the F2
   live-Magic staged pair assumes the stage4-generation content the audit receipted; if
   the stage has flipped, re-base the live pair from the newly active set's pull.
Any pulled file whose hash differs from its §6 base = re-base that staged file from the
pull and re-run verify.py before filing; never file on the stale base.

## 8. Also owed at install (named, not staged here — outside this desk scope)

- Same-pass surfaces for the install packet: Guide knowledge, What's New (website +
  in-game ExcelWhatsNew), rotation-tips check, menus check, and the player-facing
  what-changed explanation (Summon family complete; Group Renewal at Apprentice;
  Resurrection at Master; Tame Pet renamed Pet Heal) per the Nov 13 player-explanation
  rule. The supervisor's packet must carry or explicitly owe these.
- Acceptance (audit §10): Mike's in-game glance — Summon group showing the full 11,
  Other empty/absent on a Master wand, spot-check on Java + Bedrock; Apprentice wand
  shows Group Renewal in Healing and Pet Heal in Summon.
