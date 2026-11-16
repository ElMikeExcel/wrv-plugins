# TEST REPORT - ExcelTour 1.0.1 (BB-09)

## Suites (run by build.sh, both full runs identical)

- Carried TourTest: 42 checks / 0 failures - EXACTLY the 1.0.0
  count (route parsing, advancement per trigger, skip/restart/
  reset, per-player isolation, persistence, shipped-route walk).
  No carried test file was modified.
- NEW BackNavTest: 17 checks / 0 failures.
  - Source contract (11): slot-26 Back in all 3 TourMenu states;
    ARROW + "Back to Server Menu" label; old Close barrier gone;
    carried menu controls survive; slot-26 click wiring (4
    openServerMenu call sites); opener runs performCommand("menu")
    after closeInventory and touches no progress; step form gains
    Back last with action "backmenu"; carried form buttons
    survive; backmenu branch is pure navigation (no engine call,
    no save, no start).
  - Behavioral walk (6): mid-tour Back progress survival (in
    memory + save/load round-trip), tour still advances after
    Back, not-started unchanged, completed (1x) unchanged.
- Total: 59 checks / 0 failures.

## Local rig (scratch copy of the 1.0.0 rig server, 1.0.1 jar)

- Rig: BB-09/rig/server - Paper 26.3, Geyser-Spigot + floodgate
  co-loaded, TourRig driver (NMS fake player). Jar SHA-256
  verified in the rig plugins folder before boot (ab94d4a1...).
- Boot: "Enabling ExcelTour v1.0.1"; enable line "ExcelTour
  v1.0.1 enabled: 2 route(s) loaded [guild-first-visit, rig-test]
  (build EXC-TOUR-1.0.1-WRV-20261114-01-bb09)"; Done (39.974s);
  SEVERE 0 in the whole log; clean "Disabling ExcelTour v1.0.1".
  Log: BB-09/rig/boot-101.log.
- Driver exercise: RIGOBS 4 PASS / 1 FAIL - fake-player join,
  world-arrival auto-fire, menu advance, status surface PASS;
  "arrival-fired-and-completed" FAIL.
- CONTROL VERDICT: the hash-verified, unmodified 1.0.0 jar on
  an identical fresh rig copy (BB-09/rig/boot-100-control.log,
  Done (63.556s), SEVERE 0) scores the IDENTICAL 4 PASS / 1
  FAIL on the same step. The failure is a property of the rig /
  environment today, NOT of the 1.0.1 delta - which is also
  structurally incapable of causing it: the source diff touches
  no engine, trigger, ticker or teleport code (core diff is
  empty; only TourMenu, Presentation, ExcelTour wiring and
  BuildInfo changed). Carried rig behaviour 1.0.0 == 1.0.1.
- A first boot attempt on a stale-state rig copy scored the same
  4/1 (log kept as rig/boot-101-stale-state.log); a port clash
  with a parallel rig (41234) was resolved by moving this rig to
  port 41249 - rig-only server.properties change, not shipped.

## Desk walk (screen x platform x Back -> landing)

| Screen | Platform | Back control | Lands on | Progress effect |
|---|---|---|---|---|
| Guide view, not started | Java | Slot 26 ARROW "Back to Server Menu" | ExcelMenu Server Menu (/menu) | None (no tour exists) |
| Guide view, in progress | Java | Slot 26 ARROW "Back to Server Menu" | ExcelMenu Server Menu (/menu) | None - same step/skips (BackNavTest walk) |
| Guide view, completed | Java | Slot 26 ARROW "Back to Server Menu" | ExcelMenu Server Menu (/menu) | None - completions kept |
| Guide view, restart-confirm state | Java | Slot 26 ARROW (unchanged in confirm state) | ExcelMenu Server Menu (/menu) | None - confirm state is view-only; no restart fires |
| Guide view fallback (form unavailable) | Bedrock | Same TourMenu inventory, slot 26 | ExcelMenu Server Menu (/menu) | None (same code path as Java) |
| Tour step form, mid-tour | Bedrock | Last button "Back to Server Menu" (action backmenu) | ExcelMenu Server Menu (/menu) | None - step, skips, completions field-identical after Back + relog round-trip (BackNavTest walk) |
| Tour step form | Bedrock | Form X / close (carried) | Nowhere (no callback) | None (carried behaviour, unchanged) |

## Residual (named, not hidden)

Real-client tap walks cannot run in this desk lane (no Minecraft
client): Java Guide Back tap, Bedrock fallback Back tap, and the
Bedrock step-form mid-tour Back tap are the packet's player proof
owed (Mike / Kailey spot taps - the batch acceptance). The mirror
(Homebase) walk is also owed: the Homebase route was down for
this whole run, so no mirror deploy was possible.
