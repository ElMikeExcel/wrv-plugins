# RESULT - ExcelDowned fix build (P-DOWNED-REVIVE-1, board run 364)

Structured evidence only. Nothing installed; MANIFEST.md untouched;
live server untouched.

## Base gate - PASS
- Base tree: ~/workspace/downed-012-build/ExcelDowned/ (0.1.2 tree of
  record, per DIAGNOSIS.md lineage)
- Unmodified rebuild (copy at base-0.1.2/, log base-build.log):
  56778 B / SHA-256 2320e812d6c33a21f179b8e64d3f37723f260c97dbefc2ea
  df73cef97c519396 - byte-identical to the shipped 0.1.2 jar.
  Tests 27/27. Base gate hash = 2320e812...c519396.

## Candidate
- Jar: ~/workspace/board-r364/downed-build/ExcelDowned-0.1.3/ExcelDowned-0.1.3.jar
- Size: 59875 B
- SHA-256: 31f278bda631fd6dde7cbce1eba267633f35e430489ea29c6437dc92544d9e93
- Build: EXC-DOWNED-0.1.3-WRV-20261114-01-d0wn3d (fingerprint gate PASS)
- Source zip: ~/workspace/board-r364/downed-build/ExcelDowned-0.1.3-source.zip
  49118 B / SHA-256 3c929d80f07cc60f3bae634c428dcc37cd70f10872f6c11
  82eaced99c230400e
- Deterministic double-build: two independent local-build.sh runs
  (candidate-build-1.log, candidate-build-2.log) produced the SAME
  hash above; each run's internal double-build also byte-identical.
- Change scope vs 0.1.2 (diff-proven): DownedListener.java,
  ExcelDowned.java, MagicBridge.java, BuildInfo.java changed;
  FixPolicy.java new; config.yml, plugin.yml, local-build.sh,
  DownedTest.java changed; DESIGN-0.1.3.md new. Nothing else.

## Fixes (per diagnosis + backlog ruling)
1. Totem first (Defect 3): lethal intercept checks main + off hand
   (FixPolicy.holdsTotem/entersDowned); a held totem is NOT
   intercepted - vanilla consumes it, no down, no cancel. Void stays
   a real death. EntityResurrectEvent MONITOR records totem_save.
2. Window rule (Defect 1, run-363 evidence verdict): damage while
   downed eats 0 bleed-out window by default
   (config damage-eats-window: false; FixPolicy.windowEatenMillis);
   bleed-out is pure 300 s wall-clock. The scaled eat mechanism
   survives only behind the explicit opt-in. Hardening: absorption
   also cancels the event (was zero-only), fall absorption cancels,
   EntityCombustEvent guard, fire ticks cleared on entry / every
   state tick / exit, MONITOR audit WARNING for any downed damage
   reaching MONITOR uncancelled + non-zero, bypassDeath latch
   removed on quit + join. setInvulnerable deliberately NOT used
   (diagnosis open question 5, unproven EliteMobs/Protect interplay).
3. Resurrection / grant NPE (Defect 2) - ExcelDowned-side legs:
   'Resurrect' registration at onLoad (before Magic template load)
   + onEnable; boot self-check probes resurrection AND grouprenewal
   templates and WARNING-names any that do not resolve (probe
   failure is itself a WARNING - never fail-open, unlike the old
   gate probe); ResurrectAction direct target + fallback now both
   require same-world + within revive range (0.1.2 fallback checked
   neither); ranges aligned to 20 (config gate-range 24 -> 20,
   revive-range 20; spells.yml range 16 -> 20 is a packet file leg,
   in the packet draft, not this jar).
   NOT in this jar, named owed: the ExcelMagicGate held-key audit /
   grant-verification fix (the wand-side half of Defect 2) is a
   separate plugin and needs its own build. FixPolicy.grantVerified
   pins the rule that leg must implement (wand presence AND
   template resolves).

## Tests - 34/34 green
- 27 carried tests pass (one renamed pluginYmlPins012 -> 013 with
  version assertions updated; the eatDamage mechanism test stands
  as the opt-in mechanism proof).
- 7 new regression tests: totemInEitherHandPreventsDowning,
  noTotemLethalHitStillDowns (incl. void + second-hit cases),
  damageWhileDownedEatsNoWindowByDefault (28.95 s incident hit
  eats 0; deadline unmoved), windowEatingIsOptInOnly,
  reviveFallbackIsRangeAndWorldBounded,
  grantIsVerifiedOnlyWhenTemplateResolves,
  configPins013WindowRuleAndAlignedRanges.

## Rig verdict - boot PASS, SEVERE 0
- Local rig (copy of rig-0.1.2 server, Paper 26.3 + Magic 11.2.5,
  candidate jar hash verified 31f278bd... before boot), clean state:
  log rig-boot2-clean.log. "Done (79.207s)"; SEVERE count 0;
  sessions restored: 0; ready line carries the 0.1.3 build id,
  bleed-out 300s, factor 0.25.
- OnLoad proof: "'Resurrect' registered" appears at Loading stage
  (05:23:26) and again at Enabling - the early registration works.
- Self-check proof: the probe RAN and WARNING-named BOTH
  resurrection and grouprenewal as not resolving IN THE RIG - the
  rig's stock Magic config carries neither WRV custom spell, so
  the warning is accurate there and proves the check does not fail
  open. The live boot self-check lines are the install-time
  discriminator (packet boot-verify carries them verbatim).
- AdminRig auto-exercise (0.1.2 driver, unchanged): candidate
  16/17 PASS. The one FAIL (console-revive-health-50pct, health
  read 4.0 one second after the revive log line "revived at 10.0
  health") is a rig artifact, proven by control: the UNMODIFIED
  0.1.2 jar in the identical rig scored 12/17 and failed the same
  check (health 7.0 - the revived fake player stands next to the
  attacking zombie and takes variable real damage before the
  read). Control log: control-boot.log. Not a 0.1.3 regression.
- Rig cases NOT run (named, owed at/after install): D1 fire/lava/
  hostile-listener behavior cases, D2-CAST-1 real Resurrection
  revive, D3 totem behavior cases - the rig driver has no fake-
  player totem/cast harness; the pure-policy layer is unit-proven
  above and the packet carries the named-player live proofs
  (D2-CAST-1/D2-LIVE-1, D3 spot-check, D1 records check) as
  proof-owed after install.

## Packet draft (NOT filed - supervisor files)
- Packet: ~/workspace/board-r364/downed-build/INSTALL-PACKET.md
- Lint: 6299 B / max line 87 / non-ASCII 0
- Receipts: ~/workspace/board-r364/downed-build/RECEIPTS.md
  (build-side receipts filled; G1 live-premise slots BLANK by
  design - supervisor fills from a same-day pull before filing;
  the packet's restore sequence renames the held
  ExcelDowned-0.1.2.jar.held-20261114 / ExcelDowned.held-20261114
  items back with this candidate, per the backlog restore note)

## Blockers / gaps
- None for the build itself. Two named follow-ons outside this
  deliverable: (a) ExcelMagicGate grant-side build (above);
  (b) G1 receipts + MANIFEST filing are the supervisor's leg.
