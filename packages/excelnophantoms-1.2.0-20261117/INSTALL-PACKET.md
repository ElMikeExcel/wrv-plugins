# ExcelNoPhantoms 1.2.0 — INSTALL PACKET (Phantom Debt event mode)

Rides the pending install window. Do NOT install outside an approved
window; the live server is untouched by the build. Under Mike's install
rule the swap may run when NO ONE is online (jar swap is reversible —
the 1.1.0 jar is the rollback).

## What lands
- ExcelNoPhantoms-1.2.0.jar (40,631 B, SHA-256
  f705ac0e657645c09ed20427289c738dd25b5eb6c3983f3a383c825a057d94f8)
  replacing ExcelNoPhantoms-1.1.0.jar in plugins/.
- plugins/ExcelNoPhantoms/config.yml — the packet-config/config.yml in
  this project folder (mode: event, event-active: true, story lines +
  25-minute interval, ward line enabled). The live data folder
  currently has NO config.yml; if one appears before the install,
  STOP and reconcile instead of overwriting blind.

## Steps
1. Confirm nobody is online. Stop the server cleanly.
2. Back up: rename the live ExcelNoPhantoms-1.1.0.jar to
   ExcelNoPhantoms-1.1.0.jar.bak (kept in plugins/ or moved to the
   backups folder — it is the rollback; the pristine copy also lives at
   ~/workspace/renames-2026-10-16/ExcelNoPhantoms-1.1.0.jar).
3. Copy ExcelNoPhantoms-1.2.0.jar into plugins/. Verify its SHA-256
   against the value above before starting the server.
4. Copy packet-config/config.yml to
   plugins/ExcelNoPhantoms/config.yml (create the data folder if the
   first boot hasn't). Leave any state.yml alone if present (1.1.0
   never wrote one; if one exists it holds broadcast state — keep it).
5. Start the server.

## Hard gates (ALL must pass; any failure = roll back)
1. Console: `version ExcelNoPhantoms` reports 1.2.0.
2. Boot line present and correct for the install day, e.g. on a Sunday:
   "ExcelNoPhantoms 1.2.0 enabled: mode=event, event ACTIVE — Phantom
   Debt: WARDS HOLD tonight (Sunday)." — it MUST show mode=event and
   tonight's state; a mode=always-off line means the config didn't
   land (hard fail).
3. No SEVERE naming ExcelNoPhantoms; no config-malformed warnings
   (the packet config parses clean by construction — a warning means
   the file that landed isn't the file that was tested).
4. `/nophantoms status` (console or op) shows: Mode: event; Event
   active: true; Dragon seal (ender_dragon): not broken (NOT
   "unreadable" — GuildGate is installed live, so the reflection read
   must succeed); tonight's label matching the boot line. Capture this
   status output line as the install evidence.

## Player-pass checks after install
- First ward evening (Fri–Sun): the ward line fires once at first
  nightfall (world time ≥13000, someone online) and does NOT repeat
  that evening.
- First haunted night (Mon Oct 19, 2026): phantoms spawn again for
  sleepless players (vanilla insomnia); story line 1 appears at first
  nightfall check, the next lines ~25 minutes apart. If nobody sleeps,
  phantom pressure is vanilla — that IS the event.
- When Seal 1 falls (ExcelDragonTrial / GuildGate), the next boot line
  and /nophantoms status must read ENDED and phantoms stop for good.
  (Mid-session: the gate reads the seal live on every spawn, so the
  change takes effect the moment the seal breaks — no restart needed.)

## Ending the event by hand (operator)
Set event-active: false in plugins/ExcelNoPhantoms/config.yml and
restart. The boot line flips to ENDED and suppression is permanent.
Re-setting it true resumes the weekly cycle (unless the seal has
broken — the seal always wins).

## Rollback
1. Stop the server.
2. Remove ExcelNoPhantoms-1.2.0.jar; restore the 1.1.0 jar
   (ExcelNoPhantoms-1.1.0.jar.bak → ExcelNoPhantoms-1.1.0.jar).
3. Delete plugins/ExcelNoPhantoms/config.yml (1.1.0 has no config; the
   file must not linger). state.yml may stay or go — 1.1.0 ignores it;
   deleting it resets the story rotation and ward-line date for any
   future 1.2.0 reinstall.
4. Start the server; `version ExcelNoPhantoms` must report 1.1.0 and
   the boot line returns to "ExcelNoPhantoms enabled: phantoms will
   not spawn."
