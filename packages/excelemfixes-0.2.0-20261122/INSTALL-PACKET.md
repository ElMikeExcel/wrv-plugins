# INSTALL PACKET — ExcelEMFixes 0.2.0 (Nov 22, 2026)

A version swap of the pack jar + a purely additive config section.
0.2.0 adds ONE module, **jobs-dummy-gate**: a veto on Jobs payments
for EliteMobs' Adventurers Guild training dummies (they are HUSK
regional bosses that respawn instantly, and Jobs pays Kill by
entity type — an infinite money/XP farm; diagnosis:
~/workspace/trainingdummy-103-20261122/DIAGNOSIS.md). All 0.1.0
module code is byte-identical in 0.2.0 (whole-tree source diff:
version constant, one registry line, the new package — see
BUILD-RECORD.md).

## Artifacts (all in ~/workspace/excelemfixes-020-20261122/)

- `ExcelEMFixes-0.2.0.jar` — 177,592 B — SHA-256
  `733a02d190ec079b37e9017602615a2983340d98fea3d6e2d55687a94528cbe3`
  (deterministic build: two consecutive builds, identical hash)
  → replaces `plugins/ExcelEMFixes-0.1.0.jar` (169,041 B /
  `777d2c6a…`, the live jar — keep it as the rollback artifact).
- `deploy/config.yml` — 13,928 B — SHA-256
  `7c06f7d45867dd5940f64b647b7be6ea4593f9d6859d5d2905862b47233b37be`
  → lands as `plugins/ExcelEMFixes/config.yml`.

## Config delta vs the live 0.1.0 config (spelled out)

The live config is `~/workspace/excelemfixes-20261122/deploy/config.yml`
(11,800 B / `9badff9c…`). The 0.2.0 deploy config differs from it by
EXACTLY ONE change — a new module section appended after
seal-restore (values = the shipped defaults; WRV tunes nothing in
this module):

```yaml
  # ==========================================================
  # MODULE: jobs-dummy-gate  (no commands — a background fix)
  # ==========================================================
  jobs-dummy-gate:
    enabled: true
    # (defect-then-fix comment block, as shipped)
    boss-files:
      - training_dummy_lv1
      - training_dummy_lv25
      - training_dummy_lv50
      - training_dummy_lv75
      - training_dummy_lv100
    debug: false
```

Every other key is unchanged: shipped↔deploy diff inside the 0.2.0
project is the same five WRV deltas 0.1.0 already runs (arcane-bolt
scale 1.5, kill-records-folder "ExcelProtect", food-heal debug true,
food-heal worlds [em_primis, em_adventurers_guild], leash debug
true) — verified by diff at build time. No data files change:
quest-destinations.yml, drops-index.yml, tracker-catalog.yml and
seal-restore.yml stay exactly as installed with 0.1.0.

## Swap sequence (single window, verified-empty gate, backup-first)

1. Snapshot + the usual pre-window backup.
2. In `plugins/`: rename `ExcelEMFixes-0.1.0.jar` →
   `ExcelEMFixes-0.1.0.jar.bak-020` (rollback), place
   `ExcelEMFixes-0.2.0.jar`.
3. Replace `plugins/ExcelEMFixes/config.yml` with the 0.2.0 deploy
   config (keep the outgoing one as `config.yml.bak-010`).
4. Start the server; verify from the log:
   - "ExcelEMFixes 0.2.0 ready — modules: [quest-destinations,
     arcane-bolt, drop-guide, progress-tracker, food-heal, leash,
     seal-restore, jobs-dummy-gate]" (all eight).
   - "Jobs Dummy Gate ready — Jobs payments vetoed for 5 listed
     boss file(s)".
   - The other modules' ready lines unchanged from the 0.1.0 boot
     (drop guide counts, tracker counts, seal-restore rows,
     coverage scan uncovered=[]).
   - No SEVERE.
5. Write the marker (below) and the install-timing ledger line.

## Marker

`~/workspace/excelemfixes-020-20261122/INSTALLED.txt` — written by
the installing lane with window id, timestamp, and post-install
log checks.

## Same-pass surfaces (owed with this install)

In-game What's New (`plugins/ExcelWhatsNew/changelog.yml`, edit the
pulled live file, never regenerate) + the website changelog/catalog
rows. Entry (house style — plain words, no em dashes):

- id: `2026-11-22-guild-dummies-no-pay`
- date: Nov 22 / posted: 2026-11-22
- title: `FIXED: Guild training dummies no longer pay out`
- changes:
  - `The training dummies at the Adventurers Guild were paying Hunter job XP and money on every kill, and they respawn on the spot.`
  - `They now pay nothing, like a practice target should. Real husks out in the world still pay as normal.`

Website: add the changelog entry and bump the ExcelEMFixes catalog
row to 0.2.0, noting the new jobs-dummy-gate module (eight modules).

## Proof owed after install (named)

- **P-JOBSDUMMY-LIVE-1**: the next natural guild-dummy kill on
  live pays 0 — the log carries "[JobsDummyGate] Blocked a Jobs
  payment (job Hunter) for EliteMobs boss training_dummy_lv1.yml"
  (or the level actually killed), and the killer's Hunter XP /
  balance do not move. Marvin's next guild session is the natural
  witness (he reported the bug).

## Rollback

Remove `ExcelEMFixes-0.2.0.jar`, rename
`ExcelEMFixes-0.1.0.jar.bak-020` back to `ExcelEMFixes-0.1.0.jar`,
restore `plugins/ExcelEMFixes/config.yml.bak-010` as config.yml,
restart. Player data is untouched by this install (the module only
cancels Jobs payment events; nothing is written or migrated).
