# ExcelProtect 0.1.12 MIRROR RIG — REAL EliteMobs path (P-PROTECT-EM-1 re-proof)

**Worker:** board supervisor run 432, rig worker. Desk/mirror work only — no live-server changes, no installs, no MANIFEST edits. None were made.
**Date:** Mon Nov 16, 2026 (boots 11:49–11:57 CST).
**Venue:** Homebase test-server mirror (`C:\Users\MikeExcel\wrv-testserver\server\`), reached only via `ssh -F ~/.ssh/config mike-pc`. Mirror restored byte-exact at the end (proof in §5).
**Design of record:** `~/workspace/board-r432/protect-em-fix/DIAGNOSIS.md` §6 (re-proof plan) against the §5 fix; candidate per `~/workspace/board-r432/protect-build/BUILD-RECORD.md`.

**Headline:** against EliteMobs 10.9.14's REAL interception path — EM's own `InstanceProtector` doing all cancelling, a passive observer cancelling nothing — ExcelProtect 0.1.12 records exactly one attempt per stopped attempt in both EM world classes reachable on the mirror (guild hub + Primis), with correct `via` (`damage` / `can_build`) and correct attempted material, while the terminal events never fire. Toggle OFF records nothing while EM still blocks. The one leg not covered is the instanced `em_id_*` world, blocked headlessly for a precise reason (§3, L4).

## 1. Artifacts under test (hash-verified desk-side and as placed on the mirror)

| Artifact | Size (B) | SHA-256 | Placement evidence |
|---|---:|---|---|
| ExcelProtect-0.1.12.jar (candidate) | 304,634 | `6e3cf3981f38f92d3489bdde60b21f1c44ab202c26a55b70b57f9b5200d51e4b` | prep `placed-jar` line, exact match |
| EliteMobs 10.9.14 (`staged-update-EliteMobs.jar`, `~/workspace/em10914-update-packet-20261114/window-1114/`; full hash also in `~/workspace/board-r432/protect-em-fix/SHA256SUMS`) | 14,563,211 | `4a6bc4a74e382942e39961e9a25a231deb46cea34379d40bc63ae9a9328c44bd` | prep `placed-em` line, exact match |
| Candidate config.yml (build-tree, attempts ON) | 16,002 | `c17204010170fe09d67bfeea348e89f50da071e3526bc834ee1897af69c98a9b` | prep `placed-config` line, exact match |
| EM package — Adventurers Guild Hub (`plugins/EliteMobs/content_packages/adventurers_guild_hub.yml`, mirror's existing content, unmodified; `worldName: em_adventurers_guild`, `protect: true`, `contentType: HUB`) | 873 | `cdaa463d67db49a76c510bd97924ac54c404f81693720f1b1fdbcfd30fec10a4` | prep `pkg-guild` line |
| EM package — Primis Adventure (`plugins/EliteMobs/content_packages/primis_adventure.yml`, mirror's existing content, unmodified; `worldName: em_primis`, `protect: true`, `contentType: OPEN_DUNGEON`) | 708 | `cc1141bd1855dd8319165ebe5418fe7987fd317be906bc0145700bbfe13c0d49` | prep `pkg-primis` line |
| Rig harness `ProtectEmRig.jar` (observer + driver, `src/rigem/ProtectEmRig.java`) | 16,305 (Boot A2/B build) | `4F5AC68FC293EB2FB8EFA88BFE8EAFB4C7245FB82E2B38911E1423E1A59A9328` (uppercase, verbatim from compilefix2 output) | compilefix2 output (Boot A build was 14,735 B / `CFD35277…`, superseded by the A2 build that added the PRIMIS mode; harness source is hashed in SHA256SUMS) |

Mirror pre-rig state (backed up, restored §5): EliteMobs-10.9.10.jar 14,303,365 B / `3d796000…`; ExcelProtect-0.1.8.jar 268,894 B / `73373b90…`; Protect config.yml 7,991 B / `7a423378…`; no `em_*` world folders; no attempts file for 2026-11-16.

## 2. Method — EM is the canceller; the harness cancels nothing

- The harness (`ProtectEmRig`, source in `src/`) registers ONLY a passive MONITOR (`ignoreCancelled=false`) observer for `BlockDamageEvent`, `BlockCanBuildEvent`, `BlockBreakEvent`, `BlockPlaceEvent` — it logs event flow and never calls `setCancelled`/`setBuildable`. This is the exact opposite of the retired rig2 synthetic HIGH canceller (`~/workspace/board-r404/protect-rig2/`), which fired the terminal events itself and is NOT relied on anywhere here.
- Attempts are driven by firing the INITIAL events of the real break/place flows through the PluginManager with two NMS fake players (RigEmA / RigEmB, SURVIVAL, not op, no EM protection bypass): `BlockDamageEvent` for a dig start, `BlockCanBuildEvent` (attempted material DIRT) for a placement. Terminal events are fired ONLY at two labelled non-decisive positions (the merge leg L7 and the overworld control L5), so the observer can prove they never occur at the decisive positions.
- Attribution is pinned by the live handler enumeration the rig dumped at setup (Boot A2 log): `BlockDamageEvent` → `[EliteMobs@LOWEST InstanceProtector] … [ExcelProtect@MONITOR BlockAttemptWatch] [ProtectEmRig@MONITOR …]`; `BlockCanBuildEvent` → `[EliteMobs@LOWEST InstanceProtector] [ExcelProtect@MONITOR BlockAttemptWatch] [ProtectEmRig@MONITOR …]` — EM is the only LOWEST handler on both decisive events, and the only plugin besides Protect/observer on can-build at all. EM's own registry probe (`InstanceProtector.isProtectedWorld`, read-only) returned `true` for `em_adventurers_guild` and `em_primis`, `false` for `world`.
- Boots: Boot A (attempts ON, guild + controls), Boot A2 (attempts ON, Primis legs), Boot B (attempts OFF via config-only flip of `log.attempts.enabled` at config line 112, second boot, no reload — per the boot-only registration design). Each boot self-terminated via clean `Bukkit.shutdown()` (`SERVER-EXIT 0`).

## 3. Per-leg results

| Leg | Requirement | Result | Evidence |
|---|---|---|---|
| L1 | Guild BREAK: EM cancels damage; Protect records exactly ONE attempt, per player, `via=damage`, correct material; no terminal event; block unchanged; no block_change | **PASS** | 1 record: `world=em_adventurers_guild x=5000 action=break block=minecraft:stone via=damage player=RigEmA count=1`; observer: BlockDamage@5000 cancelled=true ×1, BlockBreak@5000 ×0; block stayed STONE; blockChanges@5000 = 0 |
| L2 | Guild PLACE: EM denies can-build; exactly ONE attempt, `via=can_build`, attempted material from the event (DIRT, not the AIR target cell); no terminal event; cell unchanged | **PASS** | 1 record: `x=5002 action=place block=minecraft:dirt via=can_build player=RigEmA count=1`; observer: BlockCanBuild@5002 buildable=false ×1, BlockPlace@5002 ×0; cell stayed AIR |
| L3 | Primis BREAK + PLACE, same assertions, world `em_primis` | **PASS** (Boot A2; see Primis note below) | 1 break record (`x=6000 via=damage block=minecraft:stone`) + 1 place record (`x=6002 via=can_build block=minecraft:dirt`); observer: damage cancelled=true, can-build buildable=false, BlockBreak/BlockPlace ×0; blocks unchanged; EM registry `isProtectedWorld(em_primis)=true` |
| L4 | Instanced `em_id_*` world BREAK + PLACE | **NOTRUN — BLOCKED (precise reason)** | No `em_id_*` world exists on the mirror and none can be created headlessly: instanced worlds are created by EliteMobs' DungeonInstance only when a real player launches an instanced dungeon through EM's menu flow (bot-player testing is declined on this server), and registration with InstanceProtector is per-instance by DungeonInstance — a Bukkit-created `em_id_*` name would not be registered (proven pattern by L3's Boot A probe, where a Bukkit-created `em_primis` read `isProtectedWorld=false` and EM cancelled nothing). The instance leg remains owed to the live re-proof (DIAGNOSIS §6.4), where a real player can launch an instance. |
| L5 | Overworld control (`world`): scope gate intact; uncancelled flows record no attempts; terminal path still records block_change | **PASS** | attempts with `"world":"world"` = 0; damage@7000 cancelled=false; can-build@7004 buildable=true; uncancelled terminal break@7002 produced exactly 1 block_change and 0 attempts |
| L6 | Per-player separation: second player at the same kind of position gets his own record; no cross-player merging | **PASS** | RigEmB break@guild 5004 → exactly 1 record `player=RigEmB via=damage`; RigEmA records@5004 = 0 |
| L7 | No double-count: damage feed + terminal-break feed for one physical attempt merge into ONE record | **PASS** | @guild 5006: exactly 1 record, `count=2`, `via=damage` (first via wins — coalescer key excludes `via`, per BUILD-RECORD) |
| L9 | Toggle leg (Boot B, `log.attempts.enabled=false`, second boot): EM still blocks; Protect records nothing | **PASS** | Boot line `attempts: OFF (config log.attempts.enabled=false)` ×1; attempts file byte-identical across Boot B (2,315 B, SHA-256 `b05d1f63…` before and after); observer/rig: damage cancelled=true, can-build buildable=false in the OFF boot |
| L10 | Per-player file: the attempts project into the player's own timeline file | **PASS** | `plugins\ExcelProtect\players\RigEmA-2026-11-16.jsonl` contains 3 `block_attempt` lines after `excelprotect timelines rebuild 2026-11-16` (Boot A state: guild break + place + merge) |

**Primis note (honest provenance):** in Boot A the mirror had no `em_primis` world (EM loads only the guild hub at boot) and a Bukkit-created `em_primis` was NOT protected (EM registry `false`; probe damage uncancelled, can-build buildable — recorded NOTRUN in Boot A). On the next boot (A2), with the world folder now existing, EM's own package scan loaded `em_primis` and registered it protected (`isProtectedWorld=true` at first poll, before any rig load call), and the full legs then PASSed against EM's real handlers. The terrain in that folder originated from the Boot A Bukkit creation, not EM blueprint generation; what this proof turns on — EM's registration + interception + Protect's recording — is entirely EM's own code path. The rig's reflection fallback (`EliteMobsWorld.loadWorld` via `ContentPackagesConfig.getDungeonPackages`) was armed but not needed.

**Observer totals (Boot A, `logs/observer-ON.log`):** BlockDamage ×5, BlockCanBuild ×3, BlockBreak ×2 (only the two deliberately rig-fired terminal controls: merge leg + overworld), BlockPlace ×0. At every decisive EM position the terminal-event count is 0 — the 0.1.11 blindness mechanism, reproduced and now recorded at the early events.

## 4. Boot health (all three boots)

| Check | Boot A | Boot A2 | Boot B |
|---|---|---|---|
| `Loading server plugin EliteMobs v10.9.14` | ×1 | ×1 | ×1 |
| `Enabling ExcelProtect v0.1.12` | ×1 | ×1 | ×1 |
| attempts boot line | ON ×1 (`attempts: ON … damage/can-build + terminal backstops`) | ON ×1 | OFF ×1 (`attempts: OFF (config log.attempts.enabled=false)`) |
| `READY version=0.1.12` | ×1 | ×1 | ×1 |
| SEVERE count (whole log) | 0 | 0 | 0 |
| Exit | `SERVER-EXIT 0` | `SERVER-EXIT 0` | `SERVER-EXIT 0` |

Attempts stream across the run (`logs/attempts-*.jsonl`): Boot A → header + 4 records (1,593 B / `bcc1abbd…`); after A2 → header + 6 records (2,315 B / `b05d1f63…`); after Boot B → byte-identical to after-A2 (`attempts-final.jsonl` hash equals `attempts-bootA2.jsonl`).

## 5. Mirror restore proof

| Check | Pre-rig | Post-cleanup | Verdict |
|---|---|---|---|
| EliteMobs jar | 10.9.10, 14,303,365 B, `3d796000d1d1073dab3dfebbeddf2a23b6a2e93a1a02ec8515a6ca6afdb9954` | identical hash + size | EXACT |
| ExcelProtect jar | 0.1.8, 268,894 B, `73373b90f78f7789575535b0d2de59847011e52868309dfee7d480908e99719a` | identical hash + size | EXACT |
| Protect config.yml | 7,991 B, `7a423378a29b283b6715557b97cb29bb08015ef9f0d10dc2ec909060a348d605` | identical hash + size | EXACT |
| Candidate / rig jars present | — | all absent (`EliteMobs-10.9.14.jar`, `ExcelProtect-0.1.12.jar`, `ProtectEmRig.jar`, `ProtectEmRig` data folder, `r432-mode.txt`) | REMOVED |
| Server root dirs | `.paper,cache,config,libraries,logs,plugins,versions,world` | identical list; no `em_*` folders | EXACT |
| Attempts file 2026-11-16 | did not exist | removed (rig-created; evidence copies in `logs/`) | RESTORED |
| Rig player files | none | `players\RigEmA-2026-11-16.jsonl`, `players\RigEmB-2026-11-16.jsonl` removed | RESTORED |
| Rig folder / scripts on mirror | none | `r432-rig` folder + all `*r432*` scripts deleted (final leftover count 0 after self-removal) | REMOVED |
| Java | not running | not running | — |

Residual footprint (named, same class as rig2): `usercache.json` gained RigEmA/RigEmB entries and the mirror's ordinary per-stream files gained normal boot/shutdown records from three boots; the mirror `boot.log` now holds the Boot B log (truncated by every mirror boot by design; all three rig boot logs are preserved in `logs/` here). The Boot A fixture blocks sit in the EM-managed guild world the mirror regenerates per boot and in the deleted `em_primis` folder; the overworld fixture blocks at (7000..7004, 99–100, 7000) remain in the mirror's `world` (scratch test world; rig2/primisrig precedent left equivalent fixtures).

## 6. Deliverables in this folder

- `RIG.md` — this file.
- `logs/boot-A.log`, `logs/boot-A2.log`, `logs/boot-B.log` — full server logs (Boot A: L1/L2/L5/L6/L7/L10; Boot A2: L3 + handler enumeration; Boot B: L9 toggle).
- `logs/attempts-bootA.jsonl` (header + 4 records), `logs/attempts-bootA2.jsonl` (header + 6 records), `logs/attempts-final.jsonl` (post-Boot-B, byte-identical to bootA2) — the raw attempts stream states.
- `logs/blocks-bootA.jsonl`, `logs/blocks-final.jsonl` — the blocks stream (overworld control block_change).
- `logs/observer-ON.log`, `logs/observer-PRIMIS.log`, `logs/observer-OFF.log` — the passive observer's own event-flow logs.
- `logs/timeline-RigEmA-bootA.jsonl` — the per-player timeline file evidencing L10.
- `src/rigem/ProtectEmRig.java`, `src/plugin.yml` — harness source as run (A2 build).
- `scripts/` — prep / compilefix / compilefix2 / boot / bootb / collect / cleanup scripts as run.
- `SHA256SUMS` — hashes for all of the above.

VERDICT: RIG PASS
