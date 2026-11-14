# INSTALL PACKET - EliteMobs 10.9.12 -> 10.9.14

Status: FILED Nov 14, 2026 (qualification REPORT.md + RIG-CHECK.md in
~/workspace/em-update-qualification-20261114/; receipts in this
folder's RECEIPTS.md).
Lint: 9001 B / max line 69 / non-ASCII 0
Class: GATED (one jar swap + restart). Lane: Homebase primary,
panel for backup + Shut Down/Start.
Window rule (Mike's choice, Nov 14, 2026, 10:27 AM CST): a DAYTIME
gated window TODAY, Nov 14, 2026, that closes well before the
evening Warden muster (~8:30 PM CST, flexible). Do NOT run this
inside the evening Warden / 5-of-5 sequence.

## What changes
- plugins/EliteMobs-10.9.10.jar (content 10.9.12) -> content 10.9.14,
  placed at the same live filename (house retention pattern).
- No config files are edited by this packet. EliteMobs' own
  first-boot passes (outdated-file archiver, language check) run
  under observation (step 5) and are reviewed in verify step 5.
- What 10.9.14 brings: native Bedrock class controls (sneak opens
  the ability window), the .13/.14 ACS ability updates, Smite/Bane
  and Thorns counting every level on equipped gear, PlaceholderAPI
  party placeholders, self-updating language files. Full changelog
  analysis: qualification REPORT.md sections 1-4.

## Candidate + live gate
- Candidate: EliteMobs.jar, 14,563,211 B, SHA-256
  4a6bc4a74e382942e39961e9a25a231deb46cea34379d40bc63ae9a9328c44bd
  (Modrinth official file; SHA-512 matches the API-published hash -
  RECEIPTS.md). Staged on Homebase at
  C:\Users\MikeExcel\muse-tools\em10914\EliteMobs.jar.
- LIVE-JAR GATE (STOP on mismatch): live
  plugins/EliteMobs-10.9.10.jar = 14,303,365 B / SHA-256
  3d796000d1d1073dab3dfebbdeddf2a23b6a2e93a1a02ec8515a6ca6afdb9954
  (G1 pull Nov 14, 2026, 10:42 CST, Homebase rclone - RECEIPTS.md).
- Live tuned keys (same G1 pull, re-verified fresh in gate 3):
  ItemSettings.yml flatDropRateV3: 0.0; EconomySettings.yml
  enableCurrencyShower: false (SealFreeze D50 rider);
  EconomySettings.yml currencyShowerTierMultiplier: 0.5;
  spigot.yml settings.attribute.maxHealth.max = 100000000.

## Pre-filing rig check - DONE, PASS (do not re-run)
- RIG-CHECK.md (qualification folder): the .14 jar booted on the PC
  test-server mirror - SEVERE 0, boot Done 20.916s, EliteMobs
  v10.9.14, ExcelAbilityBridge 0.1.1 enabled and logged "ACS is up -
  Bedrock ability bridge online" with no degradation line; javap
  comparison shows every ACS member the bridge uses unchanged in
  .14 (one additive module method; AbilityFailureReason grows
  10 -> 16 constants - runtime degradation path documented in
  RIG-CHECK.md section 2, follow-up folds into the bridge rename
  build, NOT a gate here). Archiver moved zero files on the mirror;
  spigot.yml byte-identical after the .14 boot.
- Mirror scope limits: the mirror carries no EM world folders and
  no FMM jar, so world loading (verify step 4) and the FMM version
  record (gate 3) are live-window checks by design.

## Acquisition (pick one in the window; record which in OUTCOME)
- PRIMARY (in-game, no credentials): console/RCON
  /em downloadpluginupdate - the jar stages server-side and applies
  at the restart in step 4 (nightbreak
  .applyPluginUpdatesWithoutRestart default). Confirm the staged
  version from the console reply before Shut Down.
- FALLBACK (Homebase): push the staged candidate jar (above) via
  the PC rclone/SFTP lane during the window; hash-verify the placed
  file against the candidate SHA-256 before Start.
- FORBIDDEN: /em applypluginupdate (no-restart swap). It refuses
  while foreign plugins depend on EliteMobs, and a hot swap would
  end running content.

## Gates (all must pass before the swap)
1. Marker discipline: no WINDOW-IN-PROGRESS marker from another
   actor; create ours at open; RE-CHECK the marker immediately
   before Shut Down and again before Start (foreign marker under
   3 h old = coordination abort per standing protocol).
2. Emptiness: console list = 0/10 players, twice, 60 s apart.
3. Fresh pre-flight pulls (PC lane, sizes + SHA-256 into OUTCOME;
   STOP on any mismatch vs the receipts in this packet):
   - Live jar matches the LIVE-JAR GATE above.
   - Tuned keys read exact: flatDropRateV3 0.0;
     enableCurrencyShower false; currencyShowerTierMultiplier 0.5.
   - spigot.yml settings.attribute.maxHealth.max reads 100000000
     (the live baseline; .13+ only raises a lower value toward
     2048, so any change here is a surprise - STOP and report).
   - GuildGate state: LOCKED, 4 of 5. SealFreeze state: ARMED,
     11-player all-zero baseline. EliteMobs player_data.db opens,
     11 rows, all progression + currency fields 0.
   - Boot-log FMM version line recorded (Spellcaster dependency
     baseline; the furniture filing verified live
     FreeMinecraftModels-2.15.0.jar).
4. Backup: ONE fresh Shockbyte backup at window open ("EM 10.9.14
   pre-install 1114"); CAP-AT-6 prune of the OLDEST manual backups,
   prunees named in OUTCOME; never the Daily Backup's newest copy
   or the two newest manual restore points.

## Steps
1. Open window: marker on, backup (gate 4), gate-3 pulls verified.
2. Acquire the jar (see Acquisition). If fallback: the staged
   jar's SHA-256 is re-verified on the PC before it moves.
3. Shut Down (panel). Swap: old jar renamed aside on the server to
   EliteMobs-10.9.10.jar.held-20261114-pre-em10914; new jar placed
   at the live filename. If the in-game download route was used,
   the restart applies the staged jar instead - verify step 1
   decides either way.
4. Pull-back verify: placed jar size + SHA-256 EXACT vs the
   candidate record (fallback route), or the console's staged
   version reply (primary route).
5. Start (panel). Watch the boot and record: (a) EliteMobs version
   line, (b) outdated-file archiver count + batch folder, (c) any
   language merge line, (d) FMM version line, (e) SEVERE count for
   the whole boot - must be 0.

## Post-install verification (ALL must pass; any fail = rollback)
1. /em version (RCON) reports 10.9.14; boot Done line present.
2. Tuned keys re-read from live files, EXACT vs gate 3:
   flatDropRateV3 0.0; enableCurrencyShower false;
   currencyShowerTierMultiplier 0.5; spigot.yml maxHealth.max
   still 100000000. Any OTHER drift in the four pulled EM configs
   must be shown additive (new default keys) in OUTCOME, else
   rollback.
3. Guild LOCKED 4 of 5; SealFreeze ARMED, baseline intact;
   player_data.db 11 rows still zeroed; ExcelExpansions level
   store re-seeded from zeros; M-02 proposal state unchanged
   (still Mike's tap at 5/5).
4. Worlds loaded: em_primis, em_primis_elementals_arena, guild
   world; ValidWorlds intact; Primis content census 17/17.
5. Archiver count from step 5b reviewed: every moved file is a
   bundled default (Lua power / enchantment script / menu / NPC /
   quest / skill bonus). Any moved file we customized = stop,
   restore it from plugins/MagmaCore/outdated files/EliteMobs/
   <batch>/, and report before the window closes.
6. ExcelAbilityBridge enabled with its "bridge online" line and
   no degradation/stand-down line (the live /abilities fire proof
   is a post-5/5 player proof, below).
7. Spellcaster: FMM staff/wand definitions loaded;
   /em class info spellcaster not locked for missing FMM content.
   If it IS locked for missing content, that is a separate FMM
   qualification - not a reason to roll EliteMobs back.
8. ExcelSealFreeze, ExcelExpansions, GuildGate, BossTrial all
   enabled; SEVERE 0 attributable to the whole window.

## Rollback
Shut Down; remove the 10.9.14 jar; restore the held-aside jar to
the live filename; if any config was archived or changed beyond
additive defaults, restore the whole plugins/EliteMobs folder from
the gate-3 pulls; Start; re-run verify steps 1-4 against the OLD
values; guild 4/5 + freeze ARMED confirmed before the window
closes.

## Player proofs owed after install (not window gates)
- Bedrock: native sneak-gesture ability fire in guild content
  (post-5/5) AND one /abilities form fire - side by side, first
  guild session (Kailey or Mike on Bedrock).
- Cleric Mend cast on another player in Primis (Bedrock + Java).
- ExcelDetails meter sanity during the first EM content run.
- Bridge follow-up build (separate item, already ordered):
  rename to ExcelEliteMobsAbilityBedrockBridge + map the 6 new
  AbilityFailureReason constants (RIG-CHECK.md section 2).

## Surfaces (same pass as the install, per standing rule)
- What's New entry + Guide chunk: "EliteMobs updated - Bedrock
  players get native class controls (sneak, then jump / attack /
  use to fire abilities); Smite, Bane and Thorns now count every
  level on your gear; several class abilities improved
  (Benediction, Purify, Refuse Death and more)."
- Website plugin table version cell 10.9.12 -> 10.9.14 (both
  files); records draft PR in wrv-plugins with this packet +
  OUTCOME (jar excluded from GitHub per standing rule - Drive
  deposit carries it).

## Marker
~/workspace/em10914-update-packet-20261114/INSTALLED.txt
