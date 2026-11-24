# RECEIPTS — ExcelEM rename wave 1 staging (Nov 23, 2026)

All live evidence pulled fresh Nov 23, 2026 via the Homebase SSH route
(ssh-pc.sh, Tailscale proxy; background only, zero console input).
Census script: `../pulls/census-wave1.ps1`, run on the PC 2026-11-23
18:07:26 -06:00; output `../pulls/rename-wave1-census.txt`.

## Live jars (PC census hash == pulled-bytes hash, both sides verified)

| Live file | Size | SHA-256 |
|---|---|---|
| ExcelEliteMobsAbilityBedrockBridge-0.2.0.jar | 32,339 | c28f95a1a893075bc046f622c59ca598c3f45422d1691fb9b073aef9ede74f8f |
| ExcelPartyDrops-0.1.0.jar | 14,731 | afae735b991d683cf45f4aa30187f51aaf53bf2b7b114761d4279979beba6196 |
| ExcelWitherSkulls-0.1.0.jar | 15,034 | 6797ce0dce6afcc451d486d1907b9d47171f5d83f1abce3063057bc243b56d9a |
| ExcelSealLog-1.0.0.jar | 8,420 | e75de2643cf0e5f1e34902574f2bc3732144356ae6609dae1c15b231b61eb0be |
| ExcelSealAlerts-0.1.2-runtime-diagnostic-OFFLINE.jar (witness only) | 43,426 | b3f1bb5470d3fcf79653b05b822b33b70e21d028f0d6a46750da853bf5cf8345 |
| EliteMobs-10.9.10.jar (rig only) | 14,563,211 | 4a6bc4a74e382942e39961e9a25a231deb46cea34379d40bc63ae9a9328c44bd |

All four rename targets hash-match the D163 audit's Nov 20 census exactly —
no live drift since the audit. Pulled copies: `../pulls/live-jars/`.

## Live data folders (PC census, recursive)

| Folder | Files | Detail |
|---|---|---|
| ExcelEliteMobsAbilityBedrockBridge | 1 | config.yml 297 B a99d7fbe87fdc0f20979d38b6213ef784d357f74c45a84d912637ffa3d3a78e1 |
| ExcelPartyDrops | 1 | config.yml 2,117 B 7f64d4b4ac8d16594f310bbe8c054c492229808e442d47eae20e30863b30dc31 |
| ExcelWitherSkulls | 1 | config.yml 1,507 B a0c5a868fe05d38555fd8b1467b053193fcf82d525f15b1c1ec3a7c81a10da46 |
| ExcelSealLog | ABSENT | no data folder live; plugin is stateless |

Pulled config copies: `../pulls/live-data/` (hashes re-verified after scp).

## Rename-safety scans (script output, this staging pass)

- Source grep (live-true sources; build-folder jars hash-match live jars):
  zero occurrences of any plugin's own name as a string literal in
  AbilityBridge 0.2.0 / PartyDrops / WitherSkulls sources. All getPlugin()
  calls target OTHER plugins (EliteMobs, Geyser-Spigot, floodgate).
- Bytecode constant-pool scan of all four live jars: no class in any jar
  holds its own plugin name as a standalone string constant. SealLog's jar
  references getPlugin nowhere.
- Build-folder jars == live jars (sha256): AbilityBridge c28f95a1...,
  PartyDrops afae735b..., WitherSkulls 6797ce0d... — local sources are
  byte-true to live for 3 of 4; SealLog verified at bytecode level instead.

## SealAlerts witness scan (the wave-1 blocker)

- Live SealAlerts plugin.yml: softdepend [ExcelSealLog, ExcelGuildGate,
  ExcelBossTrial] (also in audit sweep, plugin-yml-sweep.txt line 228).
- Bytecode: ExcelSealAlerts.class holds standalone constants "ExcelSealLog",
  "ExcelGuildGate", "ExcelBossTrial" plus getPlugin/getPluginManager refs.
- Source (~/workspace/sealalerts-fix/source-0.1.2, the live build's source):
  line 33 `TAPPED = List.of("ExcelSealLog", "ExcelGuildGate", "ExcelBossTrial")`;
  line 57 null-guarded tap loop `getPlugin(n); if (p != null && p.isEnabled())
  tap(p)` — tap attaches a log handler to that plugin's logger; line 138
  re-taps on PluginEnableEvent by plugin name; line 149 heartbeat
  `plugins_ok = taps.keySet().containsAll(TAPPED)`.
- Rig proof (boots C1/C2, logs in ../rig/): control with old-name SealLog ->
  SealAlerts READY `taps=[ExcelSealLog]`; with renamed ExcelEMSealLog ->
  `taps=[]`, SEVERE 0, no error — the seal-watch log feed detaches SILENTLY
  and heartbeat plugins_ok would read false.

## Rig boots (local rig: Paper 26.3 build 256 = live build, Temurin JRE
25.0.5.1, EliteMobs jar = live jar for boot B; logs in ../rig/)

- Boot A (no EM): all four renamed plugins enable under new names; Done
  (76.101s); SEVERE 0; PartyDrops status reads migrated config (radius
  50.0, step 0.5, cap 3.0, inert without EM); WitherSkulls status target
  25.0%, seam UNAVAILABLE; AbilityBridge self-disables by design when EM
  is absent (same as old build). Log: boot-A-noem.log.
- Boot B (live EM jar): Done (28.700s); SEVERE 0; PartyDrops "EliteMobs
  hooked"; WitherSkulls "seam BOUND"; /abilities status answers under
  ExcelEMAbilityBedrockBridge (enabled v0.2.0, EM link STANDING BY — ACS
  not ready in a bare rig; Geyser/floodgate absent in rig, present live);
  permission node exceleitemobsabilitybedrockbridge.admin unchanged.
  Log: boot-B-em.log.
- Config byte-stability: all three migrated config.yml files hash-identical
  before and after both boots (no plugin rewrites its config on boot).
- SealLog created no data folder under the new name either (stateless).

## Surfaces greps

- Live ExcelMenu + ExcelBedrockMenu configs (SSH Select-String, Nov 23):
  MENU HITS: NONE for all four names and their commands.
- Live ExcelTips + AutoBroadcaster configs: NONE. ExcelWhatsNew
  changelog.yml: one HISTORICAL entry only
  (2026-11-14-excelelitemobsabilitybedrockbridge-rename) — history is not
  edited; the wave adds a new entry.
- Live website pair (canon-476 live-verify copies, byte-verified equal to
  live Nov 23): old-name mentions — AbilityBridge index.html 2 /
  ai-knowledge.json 3; PartyDrops 2 / 4; WitherSkulls 1 / 1; SealLog 1 / 1
  (plugin-list entries; same-pass text swap at install).
