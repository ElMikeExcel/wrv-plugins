# RUN RECORD — Parent hand window, Nov 23, 2026 19:03–19:15 CT

Marker: run_id hand-20261123-190345-guildvendor-rename-tw (D67 v2, parent hand window).
Trigger: D189 escalation — the install failsafe took the D180 poisoned-environment
exit on THREE consecutive runs (18:21, 18:34, 18:51 CT) with PENDING entries, all
on SSH reset-at-setup in the worker environments. The parent seat's own routes
(Panel API via fleet token through the Tailscale proxy) were verified healthy
all evening, so the parent ran the window by hand on that route. Method named:
Panel files API for all pulls/placements, Panel power API for stop/start,
Panel console API for commands — no SSH used anywhere in this window.

## Entries installed (all 4 PENDING)

1. EliteMobs guild-vendor NPC wiring (GATED) — 4 vendor stalls + Grog dialog line.
2. PlayerMap guild-vendor wording restorations (TEXT) — tips/broadcaster/changelog.
3. ExcelTrainingWheels 0.1.4 config true-up (D208) — config only + reload.
4. ExcelEM rename wave 1 (GATED) — 4 renames + ExcelSealAlerts 0.1.3 (D211, atomic pair landed together).

## Timeline (America/Chicago)

- 19:03:45 marker written (phase=open)
- 19:04–19:05 gates pulled fresh via Panel download API: 15/15 hash+size gates
  matched the packets; absence gates all clean (no guild_vendor files, no wave
  ExcelEM jars, no ExcelSealLog/ExcelEMSealLog folders; npcs .yml count 65,
  the 66th list entry is the `primis` subfolder)
- 19:05:00 snapshot started; complete 19:06 (snapshot-11232026.1905, 7,670 MB)
- Emptiness: console list = 0/10 twice (19:04 and 19:06)
- Cap-at-6 prune: snapshot-11232026.0309 (oldest manual; the 02:30 Daily and the
  two newest manuals 1905/0755 untouched). NOTE: the panel delete errored midway
  (WinError 145, a deep FreeMinecraftModels path inside 0309) — the prune target
  is partially deleted on disk; the folder remnant is inert and flagged for
  cleanup. All other snapshots intact.
- 19:06:44 power stop sent (STOP.flag + RCON stop); server offline 19:07:11
- 19:07–19:09 placements (see below); 17/17 placed artifacts hash-verified by
  fresh download before Start
- 19:09:31 power start (STOP.flag cleared by the panel); boot Done (37.785s)
  19:10:20; SEVERE 0 in the boot log
- 19:11–19:13 console verifies + reloads (below)
- Player downtime: ~3.6 min (19:06:44 stop → 19:10:20 Done), at 0 players.
- Fire→verified close: 19:03:45 → ~19:15 ≈ 11 min total window.

## Placements (Panel upload API; it refuses overwrites, so existing files were
## renamed aside first — the panel docstring's os.replace claim does not hold
## in this build; noted for future windows)

- npcs/: guild_vendor_potion/pearls/emptymap/mount.yml (new) +
  special_blacksmith.yml replaced (live renamed to
  special_blacksmith.yml.bak-20261123-1903 first). Post-placement hashes:
  c4c80a90…/c4d2c56a…/c314eb84…/c2364e57…/ac86dfdd… — all match the packet.
  spawnLocation lines read back verbatim: 302.5 / 296.5 / 305.5 / 290.5,
  y=78, z=276.5. Folder .yml count 65 → 69.
  Floor check: discharged from the filing's live vendor-row survey (stalls
  extend the existing Grog/Greg row at the same y/z, uniform 3-block spacing);
  the talk-to-each-NPC proof remains owed and would surface any obstruction.
- ExcelTrainingWheels: live config copied in place to
  config.yml.bak-trueup-20261123 (5,845 B, the packet rollback), live file
  renamed to config.yml.live-20261123-1903, candidate placed; placed +
  post-reload hash = 7e5beaf1… (packet hash; the packet's 6,088 B size figure
  was stale — the candidate file is 6,122 B and hashes exactly).
- Rename folders: plugins/ExcelEMAbilityBedrockBridge, ExcelEMPartyDrops,
  ExcelEMWitherSkulls created; each old folder's config.yml copied byte-exact
  (a99d7fbe…/7f64d4b4…/a0c5a868…). Old data folders left in place as backup.
- Jars: 5 candidates placed (9c415821…/12f33523…/9361dd79…/efa5719c…/
  26c2bb34…), 5 old jars deleted in the same stop/start cycle — the SealLog
  rename + SealAlerts 0.1.3 swap landed atomically per D211.
  plugins/ExcelSealAlerts/ folder never written (config still 434ca631…).
- TEXT surfaces (in-place .bak-20261123-1903 nets): ExcelTips tips.yml
  c3076319…, AutoBroadcaster config.yml ebd5c6bd…, ExcelWhatsNew
  changelog.yml 00b8f7ab… (167 entries; 2026-11-23-guild-vendor id once).

## Boot + console verifies (all PASS)

- Done (37.785s) 19:10:20; SEVERE 0; TPS 20.0/19.9/19.9 after verifies.
- Enables: ExcelEMAbilityBedrockBridge v0.2.0, ExcelEMPartyDrops v0.1.0,
  ExcelEMWitherSkulls v0.1.0, [SealLog] Enabling ExcelEMSealLog v1.0.0 +
  READY version=1.0.0, ExcelSealAlerts v0.1.3 READY taps=[ExcelBossTrial,
  ExcelGuildGate, ExcelEMSealLog] (all three; order differs, set equal).
  No old-name enables. No NPC config errors in the boot slice.
- trainingwheels reload → "Training Wheels reloaded: 1 protected, siege
  threshold 60s, copper tier 45s, compensation seal 5 (elite factor 0.1289,
  source GuildGate)." — the packet's expected line verbatim; D208 is live
  (Rylie's delivered take = 0.2500x base = 75% mitigation at seals 1–5).
- partydrops status → ON, radius 50.0, step 0.5, multiplier cap 3.0,
  participant cap 5, EliteMobs hooked.
- witherskulls status → top-up ON, target 25.0%, EliteMobs seam BOUND.
- abilities status → enabled (v0.2.0), EliteMobs link UP, ACS READY
  (the status header still prints the plugin's own legacy label string;
  cosmetic, the plugin runs under the new name).
- sealalerts status → ExcelSealAlerts 0.1.3, Watching: [ExcelEMSealLog,
  ExcelGuildGate, ExcelBossTrial], posts ok/failed 1/0, last ok 7:10 PM —
  the post-boot server_started event posted: the D211 live pair proof.
- tips reload → 35 tips; autobroadcaster reload → OK; whatsnew reload →
  167 entries (observed: reload banner names 2026-11-23-quest-beacon as
  newest — same-date ordering; the guild-vendor entry is present and
  counted; 132 warnings are the standing /whatsnew check backlog).

## End state

Server online, 0/10 players, whitelist untouched (never enabled this window),
marker deleted at close by its owner. MANIFEST: 4 entries converted to
INSTALLED tombstones; full stubs + this outcome appended to MANIFEST-ARCHIVE.md.
INSTALLED.txt markers written in all four packet folders.

## Still owed (unchanged proof/records lanes)

- Talk to each vendor NPC (Java + Bedrock) + Grog's shop opens; TrainingWheels
  first-elite-hit-on-Rylie proof (factor 0.1289); rename wave same-pass
  surfaces: website pair name swap + What's New website entry + wrv-plugins
  draft record PR; NPC What's New website entry via the registry flow;
  snapshot-11232026.0309 remnant folder cleanup on the PC.
