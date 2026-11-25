# BUILD RECORD — ExcelEMFixes 0.2.0 (module 8: jobs-dummy-gate)

Date: Sun Nov 22, 2026 (CT). Builder: subagent (parent-dispatched).
Task: add a Jobs pay veto for EliteMobs' Adventurers Guild training
dummies to the ExcelEMFixes public fix pack, as new version 0.2.0.
Scope ruled by Mike: guild dummies only — class-trial actor entities
keep paying and are NOT touched.

Diagnosis of record: ~/workspace/trainingdummy-103-20261122/DIAGNOSIS.md.
Summary: the guild dummies (custom bosses training_dummy_lv1/25/50/
75/100, entityType HUSK, regional bosses that respawn instantly) pay
Hunter Jobs XP + money on every death because Jobs pays Kill actions
by entity type at EntityDeathEvent. A player farmed 3 kills in 10 s
(+5.63 XP each) on live. ExcelTrainingDummy 1.0.2's JobsGate (record:
~/workspace/trainingdummy-jobs-xp-20261121/) already implements the
correct veto pattern — LOWEST handlers on JobsPrePaymentEvent +
JobsExpGainEvent, zero + cancel — but keyed to its own dummies only.

## Design

New package `excelemfixes.jobsdummy`, three classes:

- **DummyBossList** (pure, unit-tested): the configured boss list.
  Matching is by boss FILENAME, normalized on both sides (trim,
  lower-case, optional ".yml") — never by entity type.
- **EmBossIdentity** (bridge, the pack's EmLeashBridge pattern):
  resolves a Bukkit entity to its EliteMobs boss filename through
  EliteMobs' OWN PersistentTagger.getEliteEntity() → CustomBossEntity
  .getCustomBossesConfigFields().getFilename(). Any failure resolves
  to null = "not a listed dummy, payment stands". (Verified against
  the pinned EliteMobs-10.9.10.jar bytecode: the tagger reads the
  elite UUID EM stamps into the entity's PDC and looks it up in
  EntityTracker.)
- **JobsDummyGate** (the veto; references Jobs API classes directly,
  class-loaded ONLY when Jobs is present — the 1.0.2 pattern):
  LOWEST handlers on JobsPrePaymentEvent (zero amount/points/XP +
  cancel) and JobsExpGainEvent (zero + cancel), checking BOTH event
  accessors (for a Kill, Jobs puts the killer in getEntity() and the
  dead mob in getLivingEntity() — bytecode-verified in the 1.0.2
  record). Identity timing belt-and-braces: Jobs fires its payment
  events from inside its MONITOR EntityDeathEvent handler, when EM
  may already have untracked the dead elite — so the gate also runs
  a death watch at HIGHEST (before Jobs' MONITOR handler) that
  resolves the dying entity's filename while the tracker certainly
  holds it and remembers vetoed deaths for 10 s; the payment
  handlers check that memory first, then live resolution.
- **JobsDummyGateModule** (FixModule, id `jobs-dummy-gate`): reads
  its config section (enabled via the core toggle, `boss-files`,
  `debug`). If the Jobs plugin is absent it logs an idle line and
  registers nothing — no Jobs class is ever loaded. plugin.yml
  gains Jobs as a softdepend.

Shipped default `boss-files` = the five training_dummy_lv* ids.
These ship with EliteMobs' own Adventurers Guild content, so the
default is generic, not server-specific; the list is a config knob
for other owners. Post-build grep of the new code + shipped
defaults for WRV strings (winkreyes / em_primis / Primis / Jester /
ExcelProtect / ExcelMenu / Marvin): zero hits. WRV runs this module
at the shipped defaults — the deploy config's new section is
byte-identical to the shipped one, so there is no both-numbers row
for this module (stated in the README module section instead).

Source diff vs 0.1.0 (whole tree): ONLY the VERSION constant, one
ModuleRegistry line, and the new jobsdummy package. Every 0.1.0
module's code is byte-identical in 0.2.0.

## Verification

- **Unit tests: 468/468 PASS** (the 0.1.0 suite's 444 + 24 new
  DummyBossListTest checks: the five shipped ids match with/without
  ".yml", case/whitespace normalization, duplicate collapse,
  class-trial construct/wolf NOT vetoed, unlisted bosses NOT
  vetoed, entity type never a key, degenerate inputs).
- **Jar: deterministic** — two consecutive builds byte-identical.
  `ExcelEMFixes-0.2.0.jar` — 177,592 B — SHA-256
  `733a02d190ec079b37e9017602615a2983340d98fea3d6e2d55687a94528cbe3`.
- **Combined mirror rig (NEW — the first rig with EliteMobs + Jobs
  together)**: Paper 26.2 + EliteMobs 10.9.10 with the full live EM
  content tree + Jobs 5.2.6.3 live stack of record (CMILib, Vault
  2.0.0, EssentialsX 2.22.0, live Jobs config — hunter.yml HUSK row
  5.0/5/5.0) + ExcelEMFixes 0.2.0. Driver JobsDummyRig (rig-only):
  an NMS fake player joins Hunter through the real Jobs API; the
  guild dummies are spawned through EM's own regional-boss spawner
  and killed with the player as last damager (EM's damage model
  zeroes a gearless fake player's raw damage, so the kill is: one
  real player hit to register the damager, then the vanilla health
  kill — the death event carries the player as killer, the exact
  attribution Jobs reads; a MONITOR death probe confirms it).
  Receipts: receipts/rig-jobsdummy-gateon.json / -gateoff.json.
  - **gateon: 21/21 PASS, SEVERE 0.** lv1 dummy death: Hunter XP
    delta 0, balance delta 0, MONITOR probe saw its JobsPrePayment
    CANCELLED, gate log line "Blocked a Jobs payment (job Hunter)
    for EliteMobs boss training_dummy_lv1.yml". Same for lv25
    (all five list entries share the one code path; two exercised
    live, all five unit-proven). Real vanilla husk death: paid
    normally (+6.0 XP / +$6.0, PrePayment cancelled=false) —
    identity is never entity type. (The lv1 boss's EM health read
    2.5127… on the rig — the same 2.51 in the player's report
    video, confirming the rig dummy is the live dummy.)
  - **gateoff (control): 11/11 PASS, SEVERE 0.** Same config with
    only jobs-dummy-gate disabled: the identical lv1 kill PAID
    +6.0 XP / +$6.0, PrePayment cancelled=false — the payment path
    is live and the gate is what stops it. (6.0 = the 5.0 HUSK row
    under the rig Jobs config's boost stack; live showed 5.63
    under live's stack — same row, same shape.)
  - **No-Jobs boot: clean, SEVERE 0.** ExcelEMFixes 0.2.0 without
    Jobs.jar: all 8 modules enabled, log line "Jobs Dummy Gate:
    Jobs plugin not found — module idles (nothing to veto)", no
    class-loading errors.
- **Legacy pack scenarios re-run against 0.2.0** (receipts
  rig-full.json / rig-modoff.json): modoff 3/3 PASS, SEVERE 0.
  Full: 51 results, SEVERE 0, with 2 FAILs — both classified as
  NOT 0.2.0 regressions:
  - BOOT-modules: the 0.1.0-era FixesRig asserts the enabled set
    equals exactly the old 7 modules; 0.2.0 correctly enables 8.
    Stale test expectation (source read).
  - SEAL-restore: reproduces IDENTICALLY with the 0.1.0 jar in the
    same combined rig (51 results, that single fail) — an artifact
    of the combined-rig environment (Jobs stack loaded + a rig
    world mutated by many successive runs), not of the 0.2.0 code;
    the seal module's source is byte-identical between versions
    (whole-tree diff above). On the pristine 0.1.0 rig the same
    scenario passed 51/51 at 0.1.0 filing time.

## Module inventory addition (per the pack standard)

| Module | Defect / behavior | Fix | Toggle | Default | WRV runs |
|---|---|---|---|---|---|
| jobs-dummy-gate | EliteMobs' guild training dummies are HUSK regional bosses that respawn instantly; Jobs pays Kill by entity type, so every dummy death pays full husk money + XP — an infinite farm at a "training" target | veto on Jobs' own payment events (zero + cancel) keyed on the entity's EliteMobs boss filename for the configured list; real husks unaffected | modules.jobs-dummy-gate.enabled | on (boss list = EM's five guild dummies) | shipped defaults, unchanged |

## Filing

Packet: INSTALL-PACKET.md (this folder). NOT filed in the MANIFEST
and NOT installed by this lane (parent files and installs).
