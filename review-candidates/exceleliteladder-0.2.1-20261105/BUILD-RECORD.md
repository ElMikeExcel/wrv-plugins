# ExcelEliteLadder 0.2.1 — build record (Nov 5, 2026)

- **What:** EliteLadder 0.2.1 = the Nov 5, 2026 retune bundle in one jar:
  (1) natural elites gain a hidden Speed potion buff (config
  `elite-speed:` in config.yml; default Speed I, permanent, naturals
  only, spawn + combat re-assert; chunk-load catch-up NOT implemented —
  no clean EliteEntity registry lookup exists in EliteMobs 10.9.12);
  (2) the built-in default table (bundled ladder.yml resource +
  in-code fallback) becomes the retuned values, so the fallback can
  never resurrect pre-retune tuning.
- **Jar:** `ExcelEliteLadder-0.2.1.jar` — 47,232 B — SHA-256
  `d15c5df2cbb2e8c7b6b15ba6b7543b1b0bac59394463416f4a43cc2593f03dd4`
  (build.sh double-build, byte-identical).
- **Base:** the 0.2.0 build tree, copied verbatim and verified
  byte-identical before edits (see SOURCE.md).
- **Tests:** 272 plain-JVM checks, 0 failed (TEST-REPORT.md,
  TEST-RESULTS.txt). Rig: Paper 26.3-235 + real EliteMobs 10.9.12 +
  GuildGate 0.4.0 — fresh-install generation + baseline adoption at the
  NEW S3 row + toggle boot, SEVERE 0; the natural-only gate proven on a
  live server by exclusion (summon-converted elites are CUSTOM-reason →
  isNaturalEntity false by EM's constructor rule → NBT readback shows
  zero effects). The positive spawn application is not producible on a
  playerless rig and is the install packet's post-install player proof.
- **Install state at staging time:** NOT installed. The combined window
  (jar + ladder.yml + MobCombatSettings hand-alignment + `em reload`)
  is specified in INSTALL-PACKET.md (canonical copy:
  `~/workspace/eliteladder-retune-20261105/INSTALL-PACKET-BUNDLE.md`);
  the parent files the gated MANIFEST entry.
- **Staging scope:** this package is the source-side record only
  (source, tests, configs, reports, rig evidence). The built jar lives
  in the build folder `~/workspace/eliteladder-speed-build/` pending the
  install window; INSTALLED.txt / post-install artifacts are written by
  the close-out lane after the window lands.
