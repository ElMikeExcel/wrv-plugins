# ExcelEM Rename Wave 1 - Staging Record (D163)

**Status:** STAGED Nov 23, 2026 - 3 of 4 wave-1 plugins file-ready in one
packet; the 4th (SealLog) is built and rig-proven but BLOCKED on a
cross-plugin runtime coupling (below). Nothing installed, nothing filed
into the MANIFEST.
**Lane:** rename wave-1 staging worker (parent-dispatched). No live
installs, no sends/posts/purchases, no live-data deletions were made.
**Authority:** D163 rename audit, COMPLETE Nov 20, 2026 -
~/workspace/excelem-rename-audit-20261120/RENAME-PLAN.md.
Together + RaidBuff EXCLUDED (Mike declined their renames Nov 22, 2026) -
not touched.

## Wave 1 membership (audit "Recommended sequence")

Cheap standalones, one shared window - no dependents, config-only or
no state: AbilityBedrockBridge, PartyDrops, WitherSkulls, SealLog.

## Live premise (fresh pull, Nov 23, 2026 18:07 CT, Homebase SSH route)

Census on the PC + scp pulls, hashes verified on both sides
(packets/RECEIPTS.md, sha256 abae3d3a...). All four live jars hash-match
the audit's Nov 20 census EXACTLY - no live drift:

- ExcelEliteMobsAbilityBedrockBridge-0.2.0.jar 32,339 B c28f95a1...
- ExcelPartyDrops-0.1.0.jar 14,731 B afae735b...
- ExcelWitherSkulls-0.1.0.jar 15,034 B 6797ce0d...
- ExcelSealLog-1.0.0.jar 8,420 B e75de264...

Data folders: one config.yml each for the first three (hashes in
RECEIPTS.md); ExcelSealLog folder ABSENT live (stateless).

## Per-plugin staging state

### 1. ExcelEliteMobsAbilityBedrockBridge 0.2.0 -> ExcelEMAbilityBedrockBridge
- Candidate: builds/ExcelEMAbilityBedrockBridge-0.2.0.jar, 31,990 B,
  sha256 9c4158212c7443f49be5371784aab229855381cc1f8ba46359a7637b7a32a4c1.
  Jar-level rename of the hash-verified live jar; only the plugin.yml
  name line differs (per-entry byte-identity asserted at build).
- Safety: source (byte-true to live; build-folder jar == live jar) has
  zero own-name string literals; all getPlugin() calls target EliteMobs /
  Geyser-Spigot / floodgate. Command /abilities + node
  exceleitemobsabilitybedrockbridge.admin unchanged.
- Rig: enables under the new name in both boots; self-disables by design
  without EM (same as old build); with EM, /abilities status answers
  "enabled (v0.2.0)", EM link STANDING BY (ACS not ready in a bare rig;
  Geyser/floodgate absent in rig, present live). Config byte-stable.
- Migration: copy folder (config.yml only, 297 B) + byte-verify by hash;
  status read-back at the window. Surfaces: website pair name swap
  (index 2 / knowledge 3 mentions), What's New entry, record PR; menus
  NONE, tips NONE, Guide hibernated.
- State: FILE-READY (in the wave-1 packet).

### 2. ExcelPartyDrops 0.1.0 -> ExcelEMPartyDrops
- Candidate: builds/ExcelEMPartyDrops-0.1.0.jar, 14,543 B,
  sha256 12f3352341b42a14a19977e0ee43a97eee13b1e79062c8e0e0345e33ed0f8df8.
- Safety: source byte-true to live; zero own-name literals; only
  getPlugin target is EliteMobs. /partydrops + excelpartydrops.admin
  unchanged.
- Rig: enables under new name; status reads the MIGRATED live config
  (radius 50.0, step 0.5, cap 3.0, participant cap 5), "EliteMobs not
  hooked (inert)" without EM, "EliteMobs hooked" with EM. Config
  byte-stable across both boots.
- Migration: copy folder (config.yml, 2,117 B) + hash verify; status
  read-back is the state proof (stateless kill-time math otherwise).
  Surfaces: website pair (2 / 4), What's New, record PR; menus/tips NONE.
- State: FILE-READY (in the wave-1 packet).

### 3. ExcelWitherSkulls 0.1.0 -> ExcelEMWitherSkulls
- Candidate: builds/ExcelEMWitherSkulls-0.1.0.jar, 14,862 B,
  sha256 9361dd79f9b0dd639d2b0256bfb866f57d3160ebe1e850e718d01a80e6abf8ed.
- Safety: source byte-true to live; zero own-name literals; only
  getPlugin target is EliteMobs. /witherskulls (+wskulls alias) and
  excelwitherskulls.admin unchanged.
- Rig: enables under new name; status "top-up ON, target 25.0%" from the
  migrated config; seam UNAVAILABLE without EM (designed stand-down
  warning), BOUND with EM. Config byte-stable.
- Migration: copy folder (config.yml, 1,507 B) + hash verify; status
  read-back is the proof. Surfaces: website pair (1 / 1), What's New,
  record PR; menus/tips NONE.
- State: FILE-READY (in the wave-1 packet).

### 4. ExcelSealLog 1.0.0 -> ExcelEMSealLog - BLOCKED (staged, not filed)
- Candidate built: builds/ExcelEMSealLog-1.0.0.jar, 8,344 B,
  sha256 efa5719c33993c4704bdfe20af47c8d6b5133010cfcb071cffb65b89741c2c92.
  Bytecode scan: no own-name lookup constant, no getPlugin anywhere.
  Rig-proven standalone: enables clean as ExcelEMSealLog, READY line
  prints, stateless (no data folder live; none created under the new
  name). plugin.yml keeps `prefix: SealLog` deliberately (log prefix is
  not plugin identity; byte-minimal rename per the audit shape).
- BLOCKING STEP: live ExcelSealAlerts (0.1.2-runtime-diagnostic, jar
  b3f1bb54... = audit census) taps SealLog's logger BY PLUGIN NAME:
  TAPPED = List.of("ExcelSealLog", "ExcelGuildGate", "ExcelBossTrial")
  with getPlugin(name) lookups and PluginEnableEvent re-taps by name
  (source ~/workspace/sealalerts-fix/source-0.1.2 lines 33/57/138;
  bytecode constants agree). Rig control pair (logs boot-C1/C2):
  old-name SealLog -> SealAlerts READY taps=[ExcelSealLog]; renamed ->
  taps=[] with SEVERE 0 and no error - the seal-watch fight-line feed
  detaches SILENTLY and SealAlerts' heartbeat plugins_ok goes false.
- The audit put SealAlerts' string update in Wave 3 and described this
  coupling as softdepend-only; the runtime tap is what it missed.
  Renaming SealLog in wave 1 without touching SealAlerts trades a
  cosmetic name for a silently degraded seal-watch - not filed.
- Unblock options (parent decision): (a) one-string SealAlerts build
  (TAPPED entry + plugin.yml softdepend -> ExcelEMSealLog; source on
  hand) rides the wave-1 window and SealLog joins this packet; or
  (b) SealLog moves to Wave 3 with SealAlerts.

## Rig summary (~/workspace/rename-wave1-staging-20261123/rig/)

Local rig: Paper 26.3 build 256 (live build), Temurin JRE 25.0.5.1,
vanilla jar sha1-verified against Mojang's manifest; EliteMobs jar =
the live jar (4a6bc4a7...). Driver: rig/rig_driver.py (console commands
via stdin). Logs: boot-A-noem.log (Done 76.101s, SEVERE 0),
boot-B-em.log (Done 28.700s, SEVERE 0, all statuses green),
boot-C1-oldseallog.log (taps=[ExcelSealLog]),
boot-C2-renamedseallog.log (taps=[], SEVERE 0). The one ERROR line in
boot A is /abilities invoked against the by-design self-disabled bridge
in the no-EM boot - expected, and the EM boot answers the same command
cleanly. No control boot of the old-name set was needed: candidates
differ from live jars by exactly one plugin.yml line, and both the
stand-down and EM-linked behaviors are demonstrated.

## Packet

- packets/INSTALL-PACKET.md - wave 1 for the three unblocked plugins:
  live-premise receipts cited, candidates + gates hashed, window steps,
  boot-verify lines (rig-proven verbatim), same-pass surfaces named per
  plugin, proof owed, SealLog blocker + filing note. Lint (final):
  7,701 B / max line 118 / non-ASCII 0 (template targets met).
- packets/RECEIPTS.md - all live evidence (census, pulls, scans, rig
  results, surfaces greps), sha256
  abae3d3abeb8c75c33ae61898a89e08dc92a7c0ac6c44397d09c34f62b49a814.
- builds/SHA256SUMS.txt - the four candidate jars.
- NOT filed into the MANIFEST: the audited wave is four plugins in one
  window; splitting it or adding the SealAlerts companion build is the
  parent seat's call on the blocker. On that call, filing the packet
  above is a P2 pointer stub.

## Surfaces list (per plugin, for the install pass)

- Website pair (index.html + ai-knowledge.json): name swaps -
  AbilityBridge 2/3 mentions, PartyDrops 2/4, WitherSkulls 1/1,
  SealLog 1/1 (when it moves). Plus Pages deploy check.
- What's New BOTH surfaces: one combined wave-1 entry, new id; the
  2026-11-14 historical rename entry stays untouched.
- In-game menus (ExcelMenu + ExcelBedrockMenu): verified NONE for all
  four names/commands (live grep Nov 23) - no menu work.
- Tips / AutoBroadcaster: verified NONE.
- Guide: hibernated - noted, no action (per audit).
- GitHub: wrv-plugins draft record PR at install (G2 check first).

## Open items handed back

1. Parent decision on the SealLog blocker: option (a) companion
   SealAlerts one-string build in wave 1, or (b) SealLog to Wave 3.
2. On that decision: file packets/INSTALL-PACKET.md as a MANIFEST
   pointer stub (3 plugins, or 4 under option (a)).
3. Audit record discrepancy worth folding into the D163 plan: the
   SealAlerts -> SealLog coupling is a runtime name tap, not
   softdepend-only (RENAME-PLAN.md Wave 3 note understates it; the
   same tap pattern applies to GuildGate and BossTrial in Wave 3 -
   those two rename WITH SealAlerts watching, so Wave 3 must carry the
   SealAlerts TAPPED update for all three names in that window).

## Addendum - D211 executed (Nov 23, 2026, later lane)

The parent decided open item 1 as option (a) - D211 (decision log,
Nov 23, 2026 ~6:21 PM CT): the SealAlerts companion build rides
wave 1 and SealLog joins the packet. Executed and FILED the same
evening; full record in D211-FILING-RECORD.md (this folder).

- ExcelSealAlerts 0.1.3 built from source-0.1.2: its by-name SealLog
  reference moved at THREE functional sites (TAPPED list, plugin.yml
  softdepend, AND the SealParser.accept dispatch key at
  SealParser.java:76 - the tap hands the parser p.getName(), so a
  TAPPED-only fix would have attached the tap yet silently dropped
  every SealLog fight line; dispatch harness 4/4 PASS is the proof).
  Deterministic double build: 43,153 B / sha256 26c2bb34....
- Rig pair: D1 (0.1.3 + ExcelEMSealLog) READY taps=[ExcelEMSealLog],
  SEVERE 0; D2 (0.1.3 + old-name SealLog) taps=[] - both mismatch
  directions (C2 + D2) fail silently, so the packet header now
  carries the same-window atomicity requirement for changes 4+5.
- packets/INSTALL-PACKET.md rewritten to cover all 5 changes and
  FILED as a MANIFEST PENDING pointer stub (Nov 23, 2026). Open
  item 2 is closed. Open item 3 stands and is now also noted in
  the packet's D211 section (Wave 3 must move SealAlerts' TAPPED +
  parser keys for GuildGate/BossTrial in their rename window).
