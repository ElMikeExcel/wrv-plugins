# RIG RESULTS — ExcelDowned 0.1.4

Status: Installed, not Proven. The S7 live retest is still owed (see
below); nothing in this document discharges it.

## Unit suite (build tree)

- Full suite: 39/39 passed, 0 failed (34 carried + 5 new field-shape
  provider tests). See BUILD-RECORD.md.
- Negative control: the 0.1.4 test tree with the pristine 0.1.3
  provider restored fails exactly the 3 real-shape tests —
  36 passed / 3 failed:
  realShapeFieldPartyMembersAreRead (expected 2, was 0);
  partyMembersOfRealShapeReturnsOtherMemberOnly (expected the
  party-mate, was []);
  membershipOfRealShapeTriState (expected PARTY, was UNKNOWN).
  The defect is isolated to the provider fix.

## Mirror / rig lineage

- CastRig v8 geometry-true confirmation session (board run 421):
  caster held at distance 10.0 in-step — M1 provider probe PASS
  (partyMembersOf non-empty, membershipOf = PARTY), M2 CAST-1 PASS
  (wand cast fires, target revived at 50%, revive record written,
  cooldown standing), M3 negative PASS (outsider at 10.0 refused on
  membership alone); perform CAST on both routes + fresh handle;
  tally 37 pass / 4 fail, all 4 fails off the cast chain. The earlier
  Session 2 M2 FAIL is closed as a rig-geometry fault, not a code
  fault.

## Install boot verification (live, Nov 16, 2026)

- Installed by wrv-install-failsafe, 04:43 CST window; placed jar
  pull-back EXACT 59,998 B / 703d7f9a…2339; boot Done (24.169s);
  Enabling ExcelDowned v0.1.4 exactly once; ready line + build
  fingerprint EXC-DOWNED-0.1.4-WRV-20261116-01-d0wn3d exactly once;
  Resurrect registered count 2; SEVERE 0; guild OPEN 5 of 5
  unchanged. See INSTALLED.txt and INSTALL-PACKET.md.
- Boot SELF-CHECK WARNINGS (verbatim, per the packet's
  report-don't-claim rule — grants NOT verified):
  "ExcelDowned: spell template 'resurrection' does NOT resolve at boot — grants of this spell will NPE in Magic's wand build until the template/data mismatch is fixed."
  "ExcelDowned: spell template 'grouprenewal' does NOT resolve at boot — grants of this spell will NPE in Magic's wand build until the template/data mismatch is fixed."
  This is the same boot shape as 0.1.3 and the mirror; deferred-load
  resolution is expected and is tested at the S7 live retest.
- The "ExcelTogether party adapter linked (PartyManager)" line was
  count 0 at boot: it logs at the first party probe after boot, no
  player was online, and no probe occurred. It is carried into S7,
  not claimed as a boot PASS.

## Proof still owed — S7 LIVE RETEST

A real revive in game with real clients (tester class: Marvin or
Mike — a healer/caster + a party member who goes down), per the
protocol in INSTALL-PACKET.md: ExcelTogether party confirmed in
chat before the down; caster wand carries Resurrection, mana >= 100
confirmed; cast from ~10 blocks; PASS = revive lands at 50%,
revive record written, AND the party DOWN / revived alert notices
fire. Run BOTH doors if the session allows (hub door via
/excelparty, EM door via /em party — the EM-door arm also
discharges the mirror N4 carry-over). Until S7 lands, ExcelDowned
0.1.4 is Installed, not Proven.
