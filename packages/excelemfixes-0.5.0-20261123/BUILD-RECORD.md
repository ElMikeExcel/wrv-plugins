# MERGE LANE — mcMMO XP connector into ExcelEMFixes 0.4.1 tree → merged pack 0.5.0

Started: Nov 23, 2026. Desk work only: no MANIFEST edits, no live-server changes, no player messages.

## Goal
Port ONLY the proven `mcmmo-xp-connector` module (+ XP watchdog) from the connector lane tree
(branched from pack 0.3.1, jar SHA e22466f1…, 10 module sections) into the authoritative 0.4.1
source tree (live jar SHA 0ce10eee…, 316,540 B, 14 modules) and produce the install-ready
merged pack stamped 0.5.0, plus a deploy config = live 0.4.1 config + connector section only.

## Plan / gates
1. PROVENANCE GATE: rebuild the 0.4.1 tree unmodified; jar must be byte-identical to 0ce10eee…
   before any merge work. If not reproducible → STOP and report.
2. Port: module source + unit tests + config section (with doc comments) + README section.
   plugin.yml softdepend = union (must include ExcelItems AND mcMMO). Version → 0.5.0.
3. Deploy config from live 0.4.1 config (SHA 1838b627…) + connector section only
   (BOSS 100 / MINIBOSS 40 / natural 20 / TRASH 10 per level; ceiling 60,000/hr;
   watchdog on, flag 25,000/min + 90,000/hr, cap 120,000/hr).
4. Proof: full unit suites (expect 785 + connector/watchdog checks, 0 failures);
   deterministic build ×2 byte-identical; rig on merged jar + no-gates fork
   (SHA 5821026c…): connector fixture set + field-medic / summon-mount regression smoke;
   SEVERE 0 everywhere.
5. Records: this file, draft MANIFEST entry text, daily-log line, backlog update.

## Progress log
- (init) Lane created; exploring source trees.
- Base tree identified: `~/workspace/potion-vendor-flow-20261123/build-root/` (the mount-fix
  lane applied its BuyFlow fix + D202 prices into this same tree; its jar + build-root jar
  both hash 0ce10eee…). Copied to `base-041/` (cp -r; diff -rq vs source = identical).
- **PROVENANCE GATE: PASS.** `bash build.sh` on the unmodified copy produced
  ExcelEMFixes-0.4.1.jar SHA-256 `0ce10eee6e9b3c3ae8abcffd5addffba31d267d60eedcdffe8c340a204bbd81a`,
  316,540 B — byte-identical to the live jar. Merge proceeds on this verified base.
- Deploy config source: `~/workspace/potion-vendor-flow-20261123/deploy/config.yml`
  (SHA 1838b627…, per mount-fix FIX-RECORD).
- PORT INVENTORY (merged/ vs base-041/, diff -rq): ADDED `src/src/excelemfixes/mcmmo/`
  (13 files), `src/tests/com/winkreyes/excelemfixes/KseaTest.java`, `libs/mcMMO-2.3.003-WRV.jar`.
  MODIFIED (connector hunks only): ExcelEMFixes.java (VERSION→0.5.0), ModuleRegistry.java
  (import + `new McMMOXpConnectorModule()`), TestMain.java (`KseaTest.run()`), plugin.yml
  (version 0.5.0; softdepend union [EliteMobs, Geyser-Spigot, Jobs, ExcelItems, Vault, mcMMO];
  description += connector sentence), src/config.yml + deploy/config.yml (connector section
  appended — diff = 169 added lines, 0 removed, both files; YAML-valid, 15 module sections,
  values boss 100 / miniboss 40 / natural-elite 20 / trash 10, ceiling 60000, watchdog
  enabled flag 25000/min + 90000/hr, cap 120000/hr), build.sh (mcMMO lib + comment, OUT
  0.5.0, jar date 2026-11-23), run-tests.sh (6 mcmmo pure files), README.md (Optional line
  += mcMMO, install jar name, connector section inserted after summon-mount). NOTHING else.
- UNIT SUITES (merged tree): **857 passed, 0 failed** (run-tests.sh) + mount suites
  **50 passed, 0 failed** (run-mount-tests.sh). Reconciliation: 785 (0.4.1 baseline) + 72
  (connector net: 596 − 524 0.3.1 baseline) = 857 exactly. No flake observed.
- MERGED JAR: `ExcelEMFixes-0.5.0.jar`, **359,925 B, SHA-256
  `81f26cee5d14ba568ac2c537025e3e9a25a0f5c02efc1fb38fbac75d4fe260ba`**, built twice
  byte-identical (deterministic).

## Rig results (merged jar 81f26cee…; receipts in this lane home)

### KSEA rig — merged jar + no-gates fork (5821026c…) — receipts: `merged/receipts/`
Rig copied from the connector lane (the exact environment that produced its 74/74);
only the pack jar was swapped. Config per scenario written by the harness from the
MERGED src config. **74 passed, 0 failed, SEVERE 0 in all 10 boots — identical to the
connector tree's own run.**

| Scenario | Checks | Verdict |
|---|---|---|
| full (ship pair) | 22 | ALL PASS — veto, exact award 1000, passive/dummy 0, party veto+share, pet award/cast-zero, INSTANT-INWORLD, PAYS-INWORLD |
| ceiling (rig ceiling 2500) | 2 | CEILING-FIT 1000+1000+500+0 |
| ceiling2 (restart) | 2 | CEILING-PERSISTED |
| modoff | 5 | module off = stock mcMMO + Layer-2 only |
| spike (ship pair) | 13 | INSTANT-PAY 1000 in-world, queue 0; GATHERING-PAYS 951 = 951 |
| spike-modoff | 3 | in-world gains pay with module off |
| watchdog | 8 | LEGIT clean; FLAG record + log lines; CAP zeroes capped skill only |
| watchdog-off | 2 | 150,000 pays uncapped, no flag |
| fallback (live gated fork) | 14 | GATE-INWORLD 0, DEFER-QUEUED, DEFER-PAID on exit |
| nomcmmo | 3 | module stands down with its one log line |

### Regression smoke — the 0.4.1 modules on the merged jar
- **Vendor rig** (potion lane's rig, copied to `rig-vendor/`; pack jar swapped to the
  merged jar; base config = merged src config via the lane-home `build-root/src/`
  path): **full 21/21 driver + 3/3 log checks, modoff 4/4, noitems 2/2 + 2/2 log
  checks, SEVERE 0, FAILURES: 0** — identical to the fixed 0.4.1 run. Field Bandage
  (ExcelItems ID 6) and Strong Healing Potion (ID 7) registrations and both buy
  flows proven on the merged jar. Receipts: `receipts/rig-vendor-*.json`.
- **Mount rig** (mount-fix lane's rig, copied to `rig-mount/`; pack jar swapped;
  base config = merged src config via the lane-home `module/src/` path):
  **full 30/30, modoff 3/3, noitems 4/4, SEVERE 0** — identical to the fixed 0.4.1
  run, registry counts included (full = 7 definitions; modoff = 6, bandage 6 +
  potion 7 still register, no mount claim). Mount Charm (ID 5) registration,
  toggle cycle, two-venue buy flow at config prices, combat gate, loss cooldown,
  death despawn all proven on the merged jar. Receipts: `receipts/rig-*.json`.
  Bonus evidence: the noitems receipt's module list names all 15 modules
  including `mcmmo-xp-connector`.

**No defects found.** No tree conflicts surfaced during the port; every shared-file
hunk was additive or a version stamp.

## Deliverables (this lane home)
- `ExcelEMFixes-0.5.0.jar` — merged pack, 359,925 B, SHA-256
  `81f26cee5d14ba568ac2c537025e3e9a25a0f5c02efc1fb38fbac75d4fe260ba`.
- `deploy/config.yml` — live 0.4.1 deploy config (1838b627…) + connector section
  only, SHA-256 `25c90e1e92d42ed3f233ba3b06bd18a70cdf53989fd6917a9c2f8def1ec2f2f2`.
- `mcMMO-nogates.jar` — copy of the connector lane's fork deliverable,
  3,711,102 B, SHA-256 `5821026cecf5c5484fad4a5a10b9a01b9f2c7171fcdb17004072ada675efe2d6`.
- `merged/` — the merged source tree (build.sh / run-tests.sh / run-mount-tests.sh;
  rigs in `merged/rig/`, `rig-vendor/`, `rig-mount/`); `base-041/` — the pristine
  provenance-verified 0.4.1 copy.

## Draft MANIFEST entry (for the parent lane to file — NOT filed here)

> **ExcelEMFixes 0.5.0 + mcMMO fork gate retirement (mcMMO XP connector,
> P-MCMMO-EM-REBALANCE-20261123):** THREE placements, ONE window, all-or-nothing.
> (1) Pack jar → `ExcelEMFixes-0.5.0.jar` (359,925 B, SHA-256
> `81f26cee5d14ba568ac2c537025e3e9a25a0f5c02efc1fb38fbac75d4fe260ba`) — the live
> 0.4.1 pack (all 14 modules, byte-proven base) + the `mcmmo-xp-connector`
> module as module 15: per-damage mcMMO XP from EM-tracked entities zeroed in
> every world; kill awards rate × level (BOSS 100 / MINIBOSS 40 / natural 20 /
> TRASH 10) under a persisted 60,000/hr per-skill ceiling; passive/dummy
> targets pay 0; XP watchdog (flag 25,000/min or 90,000/hr per skill, cap
> 120,000/hr per skill). (2) Pack config → deploy config SHA-256
> `25c90e1e92d42ed3f233ba3b06bd18a70cdf53989fd6917a9c2f8def1ec2f2f2`
> (live config + the connector section only; every WRV-tuned value unchanged,
> mount stays 15000/11250). (3) mcMMO fork jar swap → `mcMMO-nogates.jar`
> (3,711,102 B, SHA-256 `5821026cecf5c5484fad4a5a10b9a01b9f2c7171fcdb17004072ada675efe2d6`):
> the Nov 15 world gates DELETED (stopgap retirement); the Layer-2
> dummy/summon patch is byte-identical.
> **PAIRING CONSTRAINT: the fork swap and the pack land together or neither.**
> Fork alone (0.4.1 pack has no connector) reopens raw per-damage mcMMO XP in
> EliteMobs worlds — only the Layer-2 dummy/summon zeroing remains. Module
> alone against the still-gated fork is safe but not the ship shape: awards
> defer and pay on world exit (rig-proven fallback).
> **Live gates at filing:** pack jar `0ce10eee6e9b3c3ae8abcffd5addffba31d267d60eedcdffe8c340a204bbd81a`
> (316,540 B), pack config `1838b62745793b10efa55f306eb411f12e95b38754f952759e44cbaea5877039`,
> live fork jar `404025cc…c8501` (3,721,966 B).
> **Rollback:** restore all three backups together (pack jar .bak, pack config
> .bak, fork jar .bak) and restart — returns the server to the exact 0.4.1 +
> gated-fork state.
> **Behavior change to surface at install:** mcMMO gathering/mining/fishing XP
> inside EliteMobs worlds pays normal rates again (the Nov 15 stopgap is
> retired); EM kill awards pay instantly in-world. Same-pass surfaces: README
> entry (in tree), What's New + Guide/website rows for the rebalance.
> **Proof:** unit suites 857/0 + mount suites 50/0; KSEA rig 74/74 SEVERE 0;
> vendor rig 21/21+3/3, 4/4, 2/2+2/2 SEVERE 0; mount rig 30/30, 3/3, 4/4
> SEVERE 0. Records: `~/workspace/mcmmo-merge-20261123/RECORD.md`;
> connector design/build record: `~/workspace/mcmmo-connector-module-20261123/RECORD.md`.
