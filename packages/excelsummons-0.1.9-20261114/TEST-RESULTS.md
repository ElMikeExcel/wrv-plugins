# ExcelSummons 0.1.9 seq 02 (cap fix) — test results

Build: `ExcelSummons/local-build.sh` (JUnit runs FIRST, then the
deterministic double-build + fingerprint gate), run twice in
`~/workspace/summons-cap-build-20261114/` on 2026-11-14.
Both runs identical.

## Base gate (before any edit)

The copied 0.1.9 seq-01 tree was built UNMODIFIED in this folder
first:
- 104 tests found, 104 successful, 0 failed.
- Jar SHA-256 `753977e07a4f51e91d6a53afe2cedb60e1680cf70849b5043e470e337fc58d1a`
  — byte-identical to the shipped (built, unfiled) 0.1.9 jar in
  `~/workspace/summons-019-build-20261113/`. Clean-room base
  reproduction CONFIRMED before editing.

## Cap-fix run (both runs identical)

```
[       107 tests found           ]
[       107 tests successful      ]
[         0 tests failed          ]

ead3beec553da8e37d255971c8e8e5f96a707835c0a8811a90df115759e6df46  ExcelSummons-0.1.9.jar
ead3beec553da8e37d255971c8e8e5f96a707835c0a8811a90df115759e6df46  ExcelSummons-0.1.9-repro.jar
double-build: byte-identical
fingerprint gate: PASS (excel-build.txt + BuildInfo fragments, build EXC-SUMMONS-0.1.9-WRV-20261114-02-c9a114)
```

107 = the 104 carried tests + 3 new cap-fix tests. Carried-test
changes, all in the cap area (see BUILD-RECORD.md): six counted-
total tests migrated from the ADOPTED-default `rec()` helper to
`recCast()`; `refusalMessagesNameTheirReason`'s adopted-only
TOTAL_CAP segment re-pinned to the new law; `pluginYmlPins019`'s
fingerprint assertion re-pinned seq 01 → 02 (date + seq only;
version assertion unchanged at 0.1.9).

## The 3 new tests (all in SummonsTest.java)

1. `castSummonsAtCapStillBlocked` — (a) an Apprentice (cap 3)
   with 3 CAST summons across branches (PACK wolf + COMPANION
   cat + COMBAT archer) is TOTAL_CAP-refused; `countCounted`
   = 3; the refusal message reads "allows 3 summons, and you
   have 3 out: Wolf, Cat, Archer" with the dismiss advice.
2. `adoptedPetsOverCapDoNotBlockCasts` — (b) the Nov 14
   incident inverted: 8 ADOPTED wolves registered, Apprentice
   cap 3. `countCounted` = 0, `countedNames` empty; casts 1–3
   check NONE; after 3 CAST registrations `countCounted` = 3
   and the 4th CAST cast is TOTAL_CAP; the refusal message
   contains "you have 3 out: Archer, Cat, Familiar" and never
   contains "Wolf".
3. `adoptedPetsConsumeNoMana` — (c) 8 ADOPTED wolves:
   `ReservePolicy.totalPercent` = 0, `appliedPercent` = 0,
   `desiredModifiers` empty, `reserveCounts` empty; adding one
   CAST wolf makes raw + applied totals 10 — cast mana
   behavior unchanged.

## NOT covered here (owed, by design of this run)

No rig, no live server, no install (per task: desk build only,
do NOT file MANIFEST, do NOT install). The rig proof owed at
filing time is the 0.1.9 seq-01 list (BUILD-RECORD of the base
tree) PLUS: on the test-server mirror, a player with adopted
pets over the rank cap casts successfully up to the cast cap,
and the in-game TOTAL_CAP message names cast summons only.
Player proof (Mike): with his adopted pack out, casts work
again up to 3 cast summons (Apprentice).
