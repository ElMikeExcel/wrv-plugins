# excelemfixes-0.17.0-20261125 — RIG RESULTS

From BUILD-RECORD.md (guidance builds recovery lane, Nov 25, 2026):

- Rig proof (full Primis rig): boot clean, 22 modules, SEVERE 0, census 73 givers / 118 glow-capable.
- Baseline: non-carrier sees no glow on King Nemian.
- Staged carry of The Groves Call (server-verified: DialogObjective completed=false, npc=Nemian) -> carrier's packets show Nemian's entity glow flag set + qgobjective team join; server debug "GlowBot <- primis_groves_nemus_leader.yml = OBJECTIVE".
- Second bot beside the first: no glow metadata for Nemian (per-player only).
- Toggle control: objective-enabled false + reload -> glow flag cleared while the quest stayed carried.
- Suites: 1,153 passed / 0 failed. Deterministic double-build byte-identical (and byte-identical to the R1 lane's independent jar).
- Live boot after install (failsafe 20261125-1134): Done (38.132s), 0.17.0 ready, census 73/118, SEVERE 0, reload clean.
