# ExcelProtect 0.1.8 — SOURCE

Excel — Wink Reyes Valley. Authors: Iris / Mike.

- **What:** ExcelProtect is the server's protection/audit plugin (block +
  container rollback, Lost & Found, combat/dealt/healing incident streams).
  Design and rollout record: `~/workspace/protect-rollout-item13/` and
  backlog Item 13 / ㉚ in
  `~/workspace/goals/family-minecraft-server-build/hidden_files/parent-backlog.md`.
- **0.1.8 delta (backlog ㉚.10 + ㉚.5-half, REBUILD of record):** this is the
  REBUILT candidate from `~/workspace/protect-018-rebuild/` — it supersedes
  the first staged 0.1.8 jar (134,207 B / ba8b58c1…, built from a 0.1.1-era
  tree, NEVER installable; the MANIFEST carried an explicit STOP on it).
  Onto the proven-live 0.1.7 "Parity" base (byte-identical repro gate passed
  before any edit) it merges: (a) heal attribution — Magic spell heals,
  splash-potion heals and regen captured with healer attribution
  (who healed whom, effective vs overheal), claim-first damage labeling so
  Magic's potion-costume damage reads `delivery="spell"` while real potions
  read `delivery="potion"` and melee always reads `delivery="hand"`;
  (b) per-spell healing report (`/excelprotect healing`, §S3);
  (c) item_break capture (`ItemEvents.onItemBreak` +
  `ItemFact.durabilityRemaining`, backlog ㉚.5-half).
  Spec: `~/workspace/protect-rollout-item13/SPELL-ATTRIBUTION-GAP-20261108.md`
  §S0–S3 (§S4 summons-heal records out of scope — summons phase 2).
- **Source:** this folder (`src/com/winkreyes/excelprotect/`), built by
  `local-build.sh` (deterministic double-build, suite first, fingerprint
  gate). Tests: 97/97 (86 carried from 0.1.7 + 7 ported + 4 new in the
  rebuild; one baseline test adapted and named in BUILD-RECORD.md).
  Class inventory: 101 classes ⊇ live 0.1.7's 91, with ONE named exception —
  `CombatCore$Claim`, the private nested record of 0.1.7's retired claim
  model, replaced by `HealAttribution$Claim` (documented in BUILD-RECORD.md).
- **Candidate jar:** `ExcelProtect-0.1.8.jar`, 268,894 B,
  sha256 `73373b90f78f7789575535b0d2de59847011e52868309dfee7d480908e99719a`
  (at `~/workspace/protect-018-rebuild/ExcelProtect-0.1.8.jar`; deterministic
  double-build byte-identical).
- **Build ID:** EXC-PROTECT-0.1.8-WRV-20261108-01-a018d8
  (FINGERPRINT-STANDARD Layer B, verified in-jar by the build).
- **Desk rig:** `~/workspace/protect-018-rebuild/rig/` + `rig-evidence/` —
  Paper 26.2 + Magic 11.2.5 scratch rig, fake players; run-3 verdicts
  (a)–(d) PASS read back from the persisted streams (BUILD-RECORD.md).
- **Status:** INSTALLED Nov 8, 2026, 10:18–11:04 PM CST as one half of the
  BossTrial 0.4.0 + Protect 0.1.8 pair (parent hand window; boot 10:59 PM:
  Protect READY 0.1.8 + attribution hooked, status version=0.1.8, plugins
  78, SEVERE 0; MANIFEST INSTALLED entry + marker
  `~/workspace/bosstrial-fix-20261108/build/INSTALLED.txt`). Install-time
  config: Protect config untouched by the pair window. Post-install live
  proofs (healing attribution in a real fight) still owed — player-proof
  class, rides the chapter re-acceptance fights.
- **Package staged:** Nov 8, 2026, board-manager run 77, from
  `~/workspace/protect-018-rebuild/` (src + tests + plugin.yml + config.yml
  + local-build.sh + BUILD-RECORD.md + PACKET-DRAFT.md + this SOURCE.md;
  jar itself NOT in the package per house rule — it rides the Drive deposit).
  Publication (GitHub draft record PR + Drive deposit) is the next leg.
