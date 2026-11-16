# RIG RESULTS — ExcelDowned 0.1.3

Status: Installed Nov 15, 2026, not Proven. The live proofs named in
INSTALL-PACKET.md (D1 / D2 / D3 below) are still owed; nothing in this
document discharges them. ExcelDowned 0.1.3 was superseded live by
0.1.4 on Nov 16, 2026; this package is the records close-out for the
0.1.3 install of record.

## Unit suite (build tree)

- Full suite: 34/34 passed, 0 failed (27 carried + 7 new regression
  tests: totemInEitherHandPreventsDowning,
  noTotemLethalHitStillDowns, damageWhileDownedEatsNoWindowByDefault,
  windowEatingIsOptInOnly, reviveFallbackIsRangeAndWorldBounded,
  grantIsVerifiedOnlyWhenTemplateResolves,
  configPins013WindowRuleAndAlignedRanges). See BUILD-RECORD.md.
- Determinism: two independent local-build.sh runs produced the same
  jar hash; each run's internal double-build also byte-identical.
- Base gate: unmodified 0.1.2 tree rebuilt byte-identical to the
  shipped 0.1.2 jar (56,778 B / 2320e812…9396), 27/27.

## Local rig (build time, run 364)

- Local rig (copy of rig-0.1.2 server, Paper 26.3 + Magic 11.2.5,
  candidate jar hash verified 31f278bd… before boot), clean state:
  log rig-boot2-clean.log in the build folder. Done (79.207s);
  SEVERE count 0; sessions restored: 0; ready line carries the 0.1.3
  build ID, bleed-out 300s, factor 0.25.
- OnLoad proof: "'Resurrect' registered" appears at Loading stage and
  again at Enabling — the early registration works.
- Self-check proof: the probe RAN and WARNING-named BOTH resurrection
  and grouprenewal as not resolving IN THE RIG (the rig's stock Magic
  config carries neither WRV custom spell) — the check does not fail
  open.
- AdminRig auto-exercise (0.1.2 driver, unchanged): candidate 16/17
  PASS. The one FAIL (console-revive-health-50pct) is a rig artifact,
  proven by control: the UNMODIFIED 0.1.2 jar in the identical rig
  scored 12/17 and failed the same check. Not a 0.1.3 regression.

## Install boot verification (live, Nov 15, 2026)

- Installed by wrv-install-failsafe, 12:59 CST resume window
  (full-push chain, Install 6 of 13); Homebase lane, probe PASS
  attempt 1, no fallback. Window backup
  Downed-0.1.3-pre-install-20261115-1259; cap-at-6 held (prunee:
  "BB11 ExcelSummons 0.1.10 1115").
- Placed jar pull-back EXACT 59,875 B / 31f278bd…9e93; placed config
  pull-back EXACT 1,727 B / 8f3671e7…156; both spells.yml legs
  (Magic + stage4, range 16 -> 20, exactly 1 line each) pull-back
  EXACT vs the run-393 simulated post-leg hashes.
- Boot: Done (24.435s); Enabling ExcelDowned v0.1.3 exactly once;
  ready line + build fingerprint
  EXC-DOWNED-0.1.3-WRV-20261114-01-d0wn3d exactly once; Resurrect
  registered count 2 (load + enable); SEVERE 0; guild OPEN 5 of 5
  unchanged. SCRIPT-OUTPUT: 25 verbatim blocks, all RESULT: PASS.
  Full record: OUTCOME.txt in
  ~/workspace/board-r364/downed-build/install-20261115-resume1259/.
- Boot SELF-CHECK WARNINGS (verbatim, per the packet's
  report-don't-claim rule — grants NOT verified):
  "ExcelDowned: spell template 'resurrection' does NOT resolve at boot — grants of this spell will NPE in Magic's wand build until the template/data mismatch is fixed."
  "ExcelDowned: spell template 'grouprenewal' does NOT resolve at boot — grants of this spell will NPE in Magic's wand build until the template/data mismatch is fixed."
  Same boot shape as the rig and, later, the 0.1.4 install.

## Proof still owed (named in INSTALL-PACKET.md)

- D2-CAST-1 / D2-LIVE-1: a named-player live Resurrection revive
  (two accounts, one ExcelTogether party).
- D3 live spot-check: lethal hit with a held totem fires the totem
  and does not down.
- D1 live spot-check: damage taken while downed leaves the bleed-out
  clock at wall-clock pace (records rows show
  window_seconds_eaten: 0.0).
Until these land, ExcelDowned 0.1.3 is recorded as Installed,
not Proven.
