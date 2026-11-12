# TEST RESULTS — ExcelSummons 0.1.6 (Nov 12, 2026)

## Unit / logic harness (SummonsTest, via local-build.sh)

**78/78 tests successful, 0 failed** — reproduced identically
in three independent builds (in-tree double-build, clean-room
rebuild, rebuild from the shipped source zip; all three jars
byte-identical at SHA-256 08da4f10…f70f1).

Composition: the 74 carried 0.1.5 tests, of which 11 changed,
each named in the build record: 9 reserve fixtures re-based to
CAST origin (under Mike's ruling their old default-origin
records ARE adopted records, which must hold nothing — every
assertion carried verbatim, only the fixture origin changed)
and 2 version re-pins (pluginYmlPins015 → pluginYmlPins016:
version + fingerprint lineage only; configPins015 →
configPins016: rename only — config.yml is byte-identical to
0.1.5, every assertion carried verbatim). Plus 4 new 0.1.6
tests, all on the pure ReservePolicy:

- adoptedMenagerieHoldsNoMana — regression fixture (a):
  Jester's exact Nov 12, 2026 set (5 wolves + 2 cats + 3
  familiars + 1 mount, ALL origin=adopted; 85% under 0.1.5):
  raw total 0, reserve counts empty, desired modifier state
  empty, applied 0% (full pool usable), all 11 animals still
  registered, no record claiming a hold — and a CAST archer
  registered alongside holds its 10% alone.
- adoptedAndCastOfSameFamilyCountSeparately — mixed origins in
  the same families: the cast wolf/cat count (bare keys, 15
  total), the adopted pair adds no tier and no percent.
- castSetClampedAtSyncToCeilingAndFloor — regression fixture
  (b): a 90% raw CAST set (guardian 25 + ghast 25 + revenant
  20 + bruiser 15 + cat 5) clamps at sync to applied exactly
  75% with usable 25% ≥ the 20% floor; holds apply in registry
  order (the bruiser, which would pass the cap, holds nothing;
  the smaller cat behind it still fits at exactly 75); the
  registry keeps all 5 summons; the clamped-out bruiser claims
  no hold via recordHolds while the guardian and cat do; the
  explicit (75, 20) form agrees with the default form.
- syncClampFloorBindsBelowTheCeiling — at a hypothetical
  ceiling of 90 the floor-derived cap (100 − 20 = 80) binds:
  an 85% raw set applies 70, the last bruiser holding nothing.

Spell-logic gate: this packet changes no spells.yml and no
spell config of any kind (bridge plugin change only), so the
spell harness has nothing to re-run; the config pins
(configPins016) confirm the bridge config — caps, durations,
reserve rows, ceiling 75 / floor 20 — is unchanged from 0.1.5.

## Desk rig (Paper 26.3 + Magic 11.2.5)

**21/21 behavior checks PASS, SEVERE 0, reserve-template
warnings 0, archer-equipment soft-fail lines 0.** The 19
carried 0.1.5 checks (phases A–E at their verdicts, with the
A–D reserve expectations corrected to the Nov 12, 2026 ruling —
adopted holds nothing) + phase F's 5 archer-equipment checks
carried unchanged + phase G's 2 new checks (the menagerie
fixture and the cast clamp, both against real Magic modifier
state). Full results: `rig-0.1.6/RIG-RESULTS.md`; log:
`rig-0.1.6/boot.log`.

## Not in this run (by design)

The in-game proof is Jester's, post-install (named in
INSTALL-PACKET.md): his usable mana back at the full 200 pool
at his next login, his menagerie untouched. The desk rig
proves the same state transitions against the real Magic API
on real spawned entities, but no desk run substitutes for the
affected player's own screen.
