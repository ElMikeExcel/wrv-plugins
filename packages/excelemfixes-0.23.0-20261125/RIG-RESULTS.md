# excelemfixes-0.23.0-20261125 — RIG RESULTS (summary; full record in BUILD-RECORD.md)

Rig: Paper 26.2 + EliteMobs 10.9.10; the standalone's proven SkullRigDriver + MONITOR census; logs in the build folder's `rig2/`. Packet steps performed on the rig first.

- Phase A — target 100.0, debug on: ready line "Wither Skulls ready: top-up ON, target skull chance 100.0% (realized 97.56% with no Looting), EliteMobs seam BOUND"; 25 modules; SEVERE 0.
  - 100% leg: ELITE 40 kills → 40 skulls (38 module ADDED + 2 vanilla).
  - PLAIN 20 → 1 skull (vanilla only); ELITE no-player-kill 10 → 0.
  - 0% leg (config → target 0.0, `/witherskulls reload` → "reloaded - top-up ON, target 0.0%"): ELITE 40 → 2 skulls, 0 module adds (vanilla-only; module inert at 0%).
  - TAGGER-MISMATCH 0 / MULTI-SKULL 0 / CONVERT-FAILED 0.
- Phase B — modules.wither-skulls.enabled=false, target 100: ready line lists 24 modules (wither-skulls absent, command not registered); ELITE 40 → 0 skulls, 0 adds; SEVERE 0. Toggle OFF = exact vanilla rate.
- Rig honesty note (from BUILD-RECORD): the first Phase A attempt booted with an invalid plugin.yml (a description sentence appended at EOF) and ExcelEMFixes did not load; fixed (sentence moved onto the description line, YAML re-validated), jar rebuilt, Phase A re-run clean from scratch. The spoiled boot was used for nothing.
- Live boot (install of record): ready line "Wither Skulls ready: top-up ON, target skull chance 25.0% (realized 24.44% with no Looting), EliteMobs seam BOUND" — exact match to the packet's required line; RCON `witherskulls status` → "ExcelWitherSkulls: top-up ON, target 25.0%, debug log off, EliteMobs seam BOUND".
