# RECEIPTS - P-SPELLBOOK-1 spellbook regroup fix (F1-F4)

Folder: ~/workspace/spellbook-fix-20261114/
Assembled: 2026-11-14 (board supervisor run 329 desk worker)
Scope: audit fix list F1-F4 only. F5 (locked-view supplemental gap) is the
separate owed item P-SPELLBOOK-F5 and is NOT in this packet.

## A. Staging evidence (desk-verified, no live contact)

- Audit: ~/workspace/spellbook-audit-20261114/AUDIT.md
  32,697 B / SHA-256 0a32decb1e052d03841bcd053e04e3b0206fb3a93812c8f21c985917a87e8293
  (fix list in section 10; census in section 9.)
- Staging record: STAGING-RECORD.md (this folder)
  15,193 B / SHA-256 39ffae507a5069ce03ee3255758ebd8459efdf9caf92981cfe5402ce6962ee03
- Verification script: verify.py
  13,286 B / SHA-256 fdd057289c11be908ffbe88733ee07143b5d17bdca672163a30604a29111c13d
- Verification output: VERIFY-OUTPUT.txt
  8,373 B / SHA-256 89c3bc9830fc553ad146b6d988d12a99d11841a3a5c2e7ca7315fcbad4438159
  Final line: "RESULT: ALL CHECKS PASS". Control run reproduces the audit
  census and Mike's Nov 14 screenshots (Apprentice 80 known / Summon 4 /
  Other 3); post-fix Master 114 known / Summon 11 / Other 0.
- SHA256SUMS (this folder): 30 entries (14 staged + 14 diffs + verify.py +
  VERIFY-OUTPUT.txt). Re-run at packet assembly (2026-11-14, run 329):
  sha256sum -c SHA256SUMS -> 30/30 OK.

## B. Candidate table - base (receipted live-true) vs staged

Base hashes are the Nov 11-13 receipted lineage from STAGING-RECORD.md
section 6. They are CANDIDATE premises only; the filing-day pulls in
section C are the install gates.

1. Destination: plugins/ExcelWandBars/spellbook.yml
   Staged: staged/wandbars/spellbook.yml
   Base: 2,547 B / d3bc78d4a9f00f2d23ae075f0067bda95ae4cf4881941fba7bba2cfded10c9f8
   Candidate: 2,698 B / 756c5e3a63fbafd01348fc5ee7d031ccb3b2c22e819ab63c190a913a5e415db1
   Delta: +13/-4 spell lines (F1 regroup + F2 Healing listings)
2. Destination: plugins/Magic/spells.yml
   Staged: staged/spells/live-spells.yml
   Base: 188,164 B / 680068315978206af96f87861fba58d1c47b88ba559edafdcb2704251226ac77
   Candidate: 188,274 B / 75877195b4991f48dd976698fbb53dca3800633dd8230d65a13022bfa2258abf
   Delta: +7 name lines (F3 tamepet + F4 six summons), zero deletions
3. Destination: plugins/ExcelMagicGate/stages/stage2/spells.yml
   Staged: staged/spells/stage2-spells.yml
   Base: 89,176 B / 5312fdd1227182174df3b477f3a1c2cc682f4d213bbfd8e157eb56c82f3fdf9e
   Candidate: 89,208 B / 9be7eda6444471b526c0b20b28ee38a1efa0f25a0b474ff003cd21e9d78f97a5
   Delta: +2 name lines (archer, tamepet-Pet Heal)
4. Destination: plugins/ExcelMagicGate/stages/stage3/spells.yml
   Staged: staged/spells/stage3-spells.yml
   Base: 145,625 B / 0bb76856f8212f9d9c131b2d0755c3676de435833a5effa8bf6c1e183157e837
   Candidate: 145,673 B / 682ab22f57b99e78b6c668e92d1fa8f43893569bf92bae34d8602fde2fb08be7
   Delta: +3 name lines (archer, bruiser, tamepet-Pet Heal)
5. Destination: plugins/ExcelMagicGate/stages/stage4/spells.yml
   Staged: staged/spells/stage4-spells.yml
   Base: 188,164 B / 680068315978206af96f87861fba58d1c47b88ba559edafdcb2704251226ac77
   Candidate: 188,274 B / 75877195b4991f48dd976698fbb53dca3800633dd8230d65a13022bfa2258abf
   Delta: +7 name lines (byte-identical to staged live book, as its base was)
6. Destination: plugins/ExcelMagicGate/stages/stage5/spells.yml
   Staged: staged/spells/stage5-spells.yml
   Base: 188,164 B / ad10c4eb9dc24ceaaa25fef32b912df51526fc1ea9fed653e639a208d79754f1
   Candidate: 188,274 B / 66ebb58cabb58b30a5cc611cf2b376b8c449c4511179c60805e769d33fbe44a7
   Delta: +7 name lines
7. Destination: plugins/ExcelMagicGate/stages/stage3/paths.yml
   Staged: staged/magicgate/stage3/paths.yml
   Base: 2,629 B / 1fb6d5ad30cb455c58eff9a8f8b2d07529464c16908084007d7353ef7f72be2e
   Candidate: 2,646 B / 8da818eed48dab93e7821a257c042bdd0ba607a2ecc6047669e6c3e1f1902753
   Delta: +grouprenewal (Apprentice); no resurrection (absent from stage3 book)
8. Destination: plugins/ExcelMagicGate/stages/stage3/wands.yml
   Staged: staged/magicgate/stage3/wands.yml
   Base: 5,857 B / e3bfc5c11204af068bf044246fefcbc80a087a2fba89bf02fccf97c630fc3ed8
   Candidate: 5,874 B / 60b67eb90dfe4fe07a9962fd1288e0c9c234f2af918346a7dc5d670b3baef460
   Delta: +grouprenewal (Apprentice)
9. Destination: plugins/ExcelMagicGate/stages/stage4/paths.yml
   Staged: staged/magicgate/stage4/paths.yml
   Base: 2,983 B / 78c887929828a5cc0b1776e68cd15efae3cb8afb16b7ff9b0c3eeb5c0ade96a3
   Candidate: 3,017 B / 42829227d0ca2629055d87d55b69234f54d3a4e0d88aa703479e35640baa14c7
   Delta: +grouprenewal (Apprentice), +resurrection (Master)
10. Destination: plugins/ExcelMagicGate/stages/stage4/wands.yml
    Staged: staged/magicgate/stage4/wands.yml
    Base: 6,648 B / 744cb385292cfb570978fb0b4fdfb7dd76948b6f3e1364c8096cdb03c044af2d
    Candidate: 6,682 B / 8359247d2ca81e786327b41562150fc78b2cca36c42c4a7ca0514be9927aeb47
    Delta: +grouprenewal (Apprentice), +resurrection (Master)
11. Destination: plugins/ExcelMagicGate/stages/stage5/paths.yml
    Staged: staged/magicgate/stage5/paths.yml
    Base: 2,983 B / 78c887929828a5cc0b1776e68cd15efae3cb8afb16b7ff9b0c3eeb5c0ade96a3
    Candidate: 3,017 B / 42829227d0ca2629055d87d55b69234f54d3a4e0d88aa703479e35640baa14c7
    Delta: +grouprenewal (Apprentice), +resurrection (Master)
12. Destination: plugins/ExcelMagicGate/stages/stage5/wands.yml
    Staged: staged/magicgate/stage5/wands.yml
    Base: 6,648 B / 744cb385292cfb570978fb0b4fdfb7dd76948b6f3e1364c8096cdb03c044af2d
    Candidate: 6,682 B / 8359247d2ca81e786327b41562150fc78b2cca36c42c4a7ca0514be9927aeb47
    Delta: +grouprenewal (Apprentice), +resurrection (Master)
13. Destination: plugins/Magic/paths.yml
    Staged: staged/magic/paths.yml
    Base: 2,983 B / 78c887929828a5cc0b1776e68cd15efae3cb8afb16b7ff9b0c3eeb5c0ade96a3
    Candidate: 3,017 B / 42829227d0ca2629055d87d55b69234f54d3a4e0d88aa703479e35640baa14c7
    Delta: +grouprenewal (Apprentice), +resurrection (Master)
14. Destination: plugins/Magic/wands.yml
    Staged: staged/magic/wands.yml
    Base: 6,648 B / 744cb385292cfb570978fb0b4fdfb7dd76948b6f3e1364c8096cdb03c044af2d
    Candidate: 6,682 B / 8359247d2ca81e786327b41562150fc78b2cca36c42c4a7ca0514be9927aeb47
    Delta: +grouprenewal (Apprentice), +resurrection (Master)

Unified diffs for all 14 destinations: diffs/ (one per destination,
hashes in SHA256SUMS).

## C. Filing-day G1 live-pull list (FILLED 2026-11-14, run 330 W5 - all 15 slots, PC route rclone, no fallback)

Every value below is filled ONLY from a fresh same-day pull at filing
(rclone pull via the PC route; if the PC route fails its probes, a panel
File Manager pull labeled FALLBACK (panel) per PACKET-TEMPLATE.md).
NEVER copy a value from the audit census: the PC route was down during
the audit and its census rows are marked not-re-verified. A pulled hash
that differs from the section B base hash for that destination = STOP:
re-base that staged file from the pull and re-run verify.py (expect
"RESULT: ALL CHECKS PASS") before filing.

PULL-01  live path: plugins/ExcelWandBars/spellbook.yml
         size: 2,547  SHA-256: d3bc78d4a9f00f2d23ae075f0067bda95ae4cf4881941fba7bba2cfded10c9f8  pull timestamp: 2026-11-14 02:42:51 CST
         transport: rclone (PC route, run 330 W5)
         re-base verdict: EXACT vs section B base (2,547 B / d3bc78d4...)
PULL-02  live path: plugins/Magic/spells.yml
         size: 188,164  SHA-256: 680068315978206af96f87861fba58d1c47b88ba559edafdcb2704251226ac77  pull timestamp: 2026-11-14 02:42:56 CST
         transport: rclone (PC route, run 330 W5)
         re-base verdict: EXACT vs section B base (188,164 B / 68006831...)
PULL-03  live path: plugins/ExcelMagicGate/stages/stage2/spells.yml
         size: 89,176  SHA-256: 5312fdd1227182174df3b477f3a1c2cc682f4d213bbfd8e157eb56c82f3fdf9e  pull timestamp: 2026-11-14 02:42:59 CST
         transport: rclone (PC route, run 330 W5)
         re-base verdict: EXACT vs section B base (89,176 B / 5312fdd1...)
PULL-04  live path: plugins/ExcelMagicGate/stages/stage3/spells.yml
         size: 145,625  SHA-256: 0bb76856f8212f9d9c131b2d0755c3676de435833a5effa8bf6c1e183157e837  pull timestamp: 2026-11-14 02:43:03 CST
         transport: rclone (PC route, run 330 W5)
         re-base verdict: EXACT vs section B base (145,625 B / 0bb76856...)
PULL-05  live path: plugins/ExcelMagicGate/stages/stage4/spells.yml
         size: 188,164  SHA-256: 680068315978206af96f87861fba58d1c47b88ba559edafdcb2704251226ac77  pull timestamp: 2026-11-14 02:43:07 CST
         transport: rclone (PC route, run 330 W5)
         re-base verdict: EXACT vs section B base (188,164 B / 68006831...)
PULL-06  live path: plugins/ExcelMagicGate/stages/stage5/spells.yml
         size: 188,164  SHA-256: ad10c4eb9dc24ceaaa25fef32b912df51526fc1ea9fed653e639a208d79754f1  pull timestamp: 2026-11-14 02:43:11 CST
         transport: rclone (PC route, run 330 W5)
         re-base verdict: EXACT vs section B base (188,164 B / ad10c4eb...)
PULL-07  live path: plugins/ExcelMagicGate/stages/stage3/paths.yml
         size: 2,629  SHA-256: 1fb6d5ad30cb455c58eff9a8f8b2d07529464c16908084007d7353ef7f72be2e  pull timestamp: 2026-11-14 02:43:15 CST
         transport: rclone (PC route, run 330 W5)
         re-base verdict: EXACT vs section B base (2,629 B / 1fb6d5ad...)
PULL-08  live path: plugins/ExcelMagicGate/stages/stage3/wands.yml
         size: 5,857  SHA-256: e3bfc5c11204af068bf044246fefcbc80a087a2fba89bf02fccf97c630fc3ed8  pull timestamp: 2026-11-14 02:43:19 CST
         transport: rclone (PC route, run 330 W5)
         re-base verdict: EXACT vs section B base (5,857 B / e3bfc5c11...)
PULL-09  live path: plugins/ExcelMagicGate/stages/stage4/paths.yml
         size: 2,983  SHA-256: 78c887929828a5cc0b1776e68cd15efae3cb8afb16b7ff9b0c3eeb5c0ade96a3  pull timestamp: 2026-11-14 02:43:23 CST
         transport: rclone (PC route, run 330 W5)
         re-base verdict: EXACT vs section B base (2,983 B / 78c88792...)
PULL-10  live path: plugins/ExcelMagicGate/stages/stage4/wands.yml
         size: 6,648  SHA-256: 744cb385292cfb570978fb0b4fdfb7dd76948b6f3e1364c8096cdb03c044af2d  pull timestamp: 2026-11-14 02:43:26 CST
         transport: rclone (PC route, run 330 W5)
         re-base verdict: EXACT vs section B base (6,648 B / 744cb385...)
PULL-11  live path: plugins/ExcelMagicGate/stages/stage5/paths.yml
         size: 2,983  SHA-256: 78c887929828a5cc0b1776e68cd15efae3cb8afb16b7ff9b0c3eeb5c0ade96a3  pull timestamp: 2026-11-14 02:43:30 CST
         transport: rclone (PC route, run 330 W5)
         re-base verdict: EXACT vs section B base (2,983 B / 78c88792...)
PULL-12  live path: plugins/ExcelMagicGate/stages/stage5/wands.yml
         size: 6,648  SHA-256: 744cb385292cfb570978fb0b4fdfb7dd76948b6f3e1364c8096cdb03c044af2d  pull timestamp: 2026-11-14 02:43:34 CST
         transport: rclone (PC route, run 330 W5)
         re-base verdict: EXACT vs section B base (6,648 B / 744cb385...)
PULL-13  live path: plugins/Magic/paths.yml
         size: 2,983  SHA-256: 78c887929828a5cc0b1776e68cd15efae3cb8afb16b7ff9b0c3eeb5c0ade96a3  pull timestamp: 2026-11-14 02:43:37 CST
         transport: rclone (PC route, run 330 W5)
         re-base verdict: EXACT vs section B base (2,983 B / 78c88792...)
PULL-14  live path: plugins/Magic/wands.yml
         size: 6,648  SHA-256: 744cb385292cfb570978fb0b4fdfb7dd76948b6f3e1364c8096cdb03c044af2d  pull timestamp: 2026-11-14 02:43:41 CST
         transport: rclone (PC route, run 330 W5)
         re-base verdict: EXACT vs section B base (6,648 B / 744cb385...)
PULL-15  active guild-stage state (which ExcelMagicGate stage template
         set is live at filing): stage 4  evidence: logs/latest.log
         (258,617 B / a95e13f46e1e9931da65547efd95a2a7a3a4d627e8186d95d8e99b23c16950cb,
         pulled 2026-11-14 02:43:48 CST, rclone) line 666 verbatim:
         "[ExcelMagicGate] ExcelMagicGate enabled: applied stage 4 of 5,
         watching ExcelGuildGate seals every 60s; build
         EXC-MAGICGATE-0.3.2-WRV-20261112-01-9c032b; stage ceiling master";
         corroborated line 340: "ExcelGuildGate enabled: 4 of 5 seals
         broken, guild LOCKED". Hash cross-check: live
         plugins/Magic/spells.yml (PULL-02, 68006831...) is byte-identical
         to the stage4 template pull (PULL-05, 68006831...) and differs
         from the stage5 template pull (PULL-06, ad10c4eb...); live
         Magic paths/wands (PULL-13/14) equal the stage4 pulls
         (PULL-09/10). Supporting pull: plugins/ExcelMagicGate/config.yml
         2,926 B / 6c46a7070d07de9c7c369e3a46f69e1573f1f16316b4f078950b66a9b7addf2e,
         pulled 2026-11-14 02:43:45 CST (no stage field in config; the
         applied stage is runtime state, proven by the log line above).
         (The staged live-Magic pair assumes the stage4-generation
         content receipted in the audit; if the stage has flipped,
         re-base the live pair from the newly active set's pull.)
         Verdict: stage has NOT flipped - stage4 generation is live,
         exactly as the packet assumes. No re-base owed.
