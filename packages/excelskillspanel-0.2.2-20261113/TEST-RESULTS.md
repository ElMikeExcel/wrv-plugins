# ExcelSkillsPanel 0.2.2 — TEST RESULTS

Harness: `PanelTest` via `local-build.sh` (JUnit platform
console, run from the project root). **38 tests found, 38
successful, 0 failed** — in the project build (4 containers).
Base gate: the 0.2.1 rebuild ran **33/33 tests pass** before
this build (BUILD-RECORD.md §3).

## Compile gate first (F0)

The source as the prior lane left it did not compile its own
tests: the 0.2.2 `PanelTest` calls
`MagicApiReads.wrvDisplayName(...)` / `wrvNextKey(...)`, which
were package-private — 13 javac errors on the first rebuild
attempt. Fix (this lane, before any build): both methods made
`public` in `MagicApiReads.java` (pure functions; the tests pin
their contract). No behaviour change. The 38/38 result above
is after F0.

## What the 38 cover (per BUILD-RECORD.md §3–§4)

The 0.2.2 test cases pin the new behaviour: the WRV rank
order (Apprentice → Master, never Magic's "Wizard"), the
carried-wand resolution, the effective-mana display, the
"Credited casts" label from the MagicGate ledger, and the
removal of the Class line and of the ExcelMagicLevel
`totalCasts` fallback. The per-test names live in
`tests/com/winkreyes/excelskillspanel/PanelTest.java` in this
package; no separate test log was shipped by the build lane,
and none is invented here.

## Relation to the rig

JUnit proves the units; the desk rig proves the behaviour
live (RIG-RESULTS.md): both rig phases 71/0 on real Paper
26.3 + Magic 11.2.5 with Mike's real mage fixture. The rig's
prior-run 7 failures were expectation/driver issues resolved
in BUILD-RECORD.md §2, not test gaps — the 38/38 suite and
the 71/0 rig agree on the shipped behaviour.
