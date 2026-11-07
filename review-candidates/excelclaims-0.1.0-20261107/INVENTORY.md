# ExcelClaims 0.1.0 — Phase 1 inventory (GriefPrevention touchpoints + live parity target)

Desk inventory, Nov 7, 2026. Sources: house build trees under ~/workspace, the pinned
GriefPrevention 16.18.7 jar's public API surface (signatures only, `javap` — the same
verification method ExcelLeaderboard's LiveApiSignatureTests uses), the Oct 15, 2026 live
server backup (`~/workspace/backups/pre-guild-20261015/root/plugins/GriefPreventionData/`),
and the Oct 18, 2026 live pull (`~/workspace/claim-blocks/live-pull/`).

## 1. Code consumers — the exact facade surface ExcelClaims must satisfy

11 consumers. Distinct facade entry points consumed: **35** (counted below).

### Compile-time consumers (link against `me.ryanhamshire.GriefPrevention.*`)

1. **ExcelLandmarks** (`landmarks-build/src/.../hook/GpHook.java`)
   `GriefPrevention.instance`, `instance.dataStore`; `DataStore.getClaimAt(Location, boolean, Claim)`,
   `DataStore.createClaim(World, int, int, int, int, int, int, UUID, Claim, Long, Player)` →
   `CreateClaimResult{succeeded, claim}`; `Claim.parent` (field, walked to top-level);
   `Claim.getLesserBoundaryCorner()`, `getGreaterBoundaryCorner()`; `Claim.setPermission(String, ClaimPermission.Build)`;
   `Claim.managers` (public `ArrayList<String>`, read AND mutated); `DataStore.saveClaim(Claim)`;
   `DataStore.getClaim(long)`; `Claim.getID()`; `Claim.getPermission(String)`;
   `DataStore.deleteClaim(Claim)`; documented behavior: `DataStore.changeClaimOwner` throws
   `DataStore.NoTransferException` for subdivisions ("Subdivisions can't be transferred").
2. **ExcelAutoSort** (`autosort-plugin/src/.../hook/GpRightsHook.java`)
   `GriefPrevention.instance`, `instance.dataStore`, `getClaimAt(Location, boolean, Claim)`,
   `Claim.isAdminClaim()`, `Claim.getOwnerID()`, `Claim.managers`; events
   `events.ClaimDeletedEvent`, `events.ClaimModifiedEvent` (both `getClaim()`), corners.
   Its rule (AutoSort SPEC §6.4): configure = claim owner OR `managers` membership
   (PermissionTrust) OR plot-renter-as-subdivision-manager OR record owner in unclaimed
   land OR `excelautosort.admin` in admin claims. **ContainerTrust/AccessTrust alone
   must NOT satisfy it** — the facade must keep managers distinct from the other lists.
3. **ExcelProtect** (`protect-*-build/src/.../GpAdapter.java`)
   `getClaimAt`, `Claim.getOwnerName()`, `getOwnerID()`, `getArea()`, corners; events
   `ClaimCreatedEvent`, `ClaimDeletedEvent`, `ClaimChangeEvent` (`getTo()`).
4. **ExcelAuditLog** (`excelauditlog/.../GpBridge.java`)
   Same events as Protect plus `ClaimCreatedEvent.getCreator()` (CommandSender) and
   `ClaimChangeEvent.getFrom()`.
5. **Magic** (third-party, `com.elmakers.mine.bukkit.protection.GriefPreventionAPI`)
   `plugin instanceof me.ryanhamshire.GriefPrevention.GriefPrevention`;
   `GriefPrevention.instance`; `instance.dataStore`; `getClaimAt(Location, false, null)`;
   `Claim.allowEdit(Player)` → String (null = allowed); public field
   `GriefPrevention.config_pvp_noCombatInPlayerLandClaims` (boolean).

### Reflection consumers (probe by name/signature; fail-soft when absent)

6. **ExcelTreeCleanup** (`ClaimGateway.java`): `Class.forName("...GriefPrevention")`,
   field `instance`, field `dataStore`, `DataStore.getClaimAt(Location, boolean, Claim)`,
   `Claim.allowBuild(Player, Material)` → String/null.
7. **ExcelGuide** (`GpClaims.java`) and 8. **ExcelLeaderboard** (`SoftAdapters.java`) —
   identical probe: field `instance`, field `dataStore`, `DataStore.getPlayerData(UUID)`,
   `PlayerData.getAccruedClaimBlocks()`, `getBonusClaimBlocks()`, `getClaims()`,
   `Claim.getArea()`, `Claim.parent` (field), `Claim.getLesserBoundaryCorner()`,
   `GriefPrevention.claimsEnabledForWorld(World)`.
9. **ExcelShops** (`GriefPreventionOwnership.java`): `getPluginManager().getPlugin("GriefPrevention")`,
   `dataStore` field read off the plugin instance's class, `getClaimAt`, `Claim.getOwnerID()`,
   `Claim.getID()`, `Claim.parent` (field), corners.
10. **ExcelBossTrial** (`GriefPreventionWildernessGuard.java`): plugin-by-name lookup,
    `dataStore` field, `getClaimAt(Location, boolean, Claim)` (null = wilderness).
11. **ExcelMenu / ExcelBedrockMenu childproof screens** (`ChildproofScreen.java` /
    `ChildproofMenu.java`): plugin-by-name lookup, `dataStore` field, `getClaimAt` by
    signature scan, `ClaimPermission` enum constants `Build`/`Container`/`Access`,
    `Claim.checkPermission(UUID, ClaimPermission, Event)` → `Supplier<String>`
    (null return = allowed).

### Facade entry-point tally (distinct members consumed)

- `GriefPrevention`: `instance` field, `dataStore` field, `claimsEnabledForWorld(World)`,
  `config_pvp_noCombatInPlayerLandClaims` field = **4**
- `DataStore`: `getClaimAt(3-arg)`, `getClaim(long)`, `createClaim(11-arg)`, `saveClaim`,
  `deleteClaim`, `getPlayerData(UUID)`, `changeClaimOwner` (+ `NoTransferException`) = **7**
- `Claim`: `parent`, `managers`, `getLesserBoundaryCorner`, `getGreaterBoundaryCorner`,
  `getID`, `getArea`, `getOwnerID`, `getOwnerName`, `isAdminClaim`, `setPermission`,
  `getPermission(String)`, `allowBuild`, `allowEdit` = **13**
- `PlayerData`: `getAccruedClaimBlocks`, `getBonusClaimBlocks`, `getClaims` = **3**
- `ClaimPermission`: enum + constants Edit/Build/Container/Access/Manage/Inventory = **1 type**
- Events: `ClaimCreatedEvent` (+`getCreator`), `ClaimDeletedEvent`, `ClaimModifiedEvent`,
  `ClaimChangeEvent` (`getFrom`/`getTo`) = **4 types**
- `CreateClaimResult`: `succeeded`, `claim` = **1 type**
Total: 4 + 7 + 13 + 3 + 1 + 4 + 1 = **33 types/members**, plus `checkPermission`
(UUID overload) counted inside Claim's 13 → **34**; plus the plugin-name registration
itself ("GriefPrevention") that consumers 9–11 look up = **35 touchpoints**.

**Compatibility decision (see BUILD-RECORD):** ExcelClaims ships facade classes in the
`me.ryanhamshire.GriefPrevention` package AND registers its Bukkit plugin under the
name `GriefPrevention` (version/description/authors brand it ExcelClaims 0.1.0), so
every consumer above — compile-time, reflection, by-name, and Magic's `instanceof` —
keeps working with **zero rebuilds and zero re-points**.

## 2. Live GP configuration — the parity target (GP-as-WRV-uses-it)

From the full live `config.yml` (Oct 15, 2026 capture; values re-confirmed against the
Oct 18, 2026 transcription in `claim-blocks/live-pull/gp-config-summary.txt`):

| # | Setting | Live value |
|---|---------|-----------|
| 1 | Claims mode: world | Survival |
| 2 | Claims mode: world_nether / world_the_end | Disabled / Disabled |
| 3 | InitialBlocks | 100 |
| 4 | Accrued per hour | 100 |
| 5 | Max accrued | 80,000 |
| 6 | Accrued idle threshold / percent | 0 / 0 (no idle discount) |
| 7 | AbandonReturnRatio | 1.0 |
| 8 | AutomaticNewPlayerClaimsRadius | 4 (first-chest claim 9×9 = 81) |
| 9 | AutomaticNewPlayerClaimsRadiusMinimum | 0 |
| 10 | MinimumWidth / MinimumArea | 5 / 100 |
| 11 | ExtendIntoGroundDistance | 5 |
| 12 | ModificationTool / InvestigationTool | GOLDEN_SHOVEL / STICK |
| 13 | Expiration: ChestClaimDays | 7 |
| 14 | Expiration: UnusedClaimDays | 14 |
| 15 | Expiration: AllClaims DaysInactive | 60 |
| 16 | Expiration exemptions | owner total blocks ≥ 10,000 or bonus ≥ 5,000 |
| 17 | MaximumNumberOfClaimsPerPlayer | 0 (unlimited) |
| 18 | Economy buy/sell claim blocks | DISABLED (cost 0.0, sell 0.0, max bonus 0) |
| 19 | PreventTheft | true |
| 20 | ProtectCreatures | true (+ horses/donkeys/llamas true) |
| 21 | PreventButtonsSwitches | true |
| 22 | LockWoodenDoors / LockTrapDoors / LockFenceGates | false / false / true |
| 23 | EnderPearlsRequireAccessTrust (+ refund denied) | true / true |
| 24 | RaidTriggersRequireBuildTrust | true |
| 25 | VillagerTradingRequiresPermission | true |
| 26 | HoppersRequireBuildTrust | false |
| 27 | LecternReadingRequiresAccessTrust | true |
| 28 | CommandsRequiringAccessTrust | /sethome |
| 29 | PvP: ProtectPlayersInLandClaims (player / admin / admin-subdiv) | true / true / true |
| 30 | PvP: CombatTimeoutSeconds / PunishLogout | 15 / true |
| 31 | BlockLandClaimExplosions / surface creeper / surface other | true / true / true |
| 32 | FireSpreadsInClaims / FireDamagesInClaims | false / false |
| 33 | FireSpreads / FireDestroys (global) | false / false |
| 34 | PistonMovement | CLAIMS_ONLY |
| 35 | EndermenMoveBlocks / SilverfishBreakBlocks / CreaturesTrampleCrops | false / false / false |
| 36 | RabbitsEatCrops | true |

**36 parity items.** Live claim census at last pull (Oct 18): 10 claims, all in `world`,
all top-level (parent −1), owners = Kailey/Marvin/Jester (+ the spawn admin claim added
Nov 3 for ExcelLandmarks, post-pull); 6 PlayerData files (accrued 100–3,492, bonus all 0).
The Landmarks precondition: one big GP **admin claim over spawn**; plots are subdivisions
of it; the buyer gets Build + manager on the subdivision (subdivisions cannot change owner).

## 3. Player-facing command set as used on WRV

From ExcelBedrockMenu's COMMANDS-CATALOG (what the menus actually offer players):
`/claim`, `/claimslist`, `/trust`, `/extendclaim`, `/buyclaimblocks`, `/trapped`,
`/abandonclaim`. Plus the GP trust family the catalog/menus imply and players use:
`/untrust`, `/containertrust`, `/accesstrust`, `/permissiontrust`, `/trustlist`,
`/subdivideclaims`, `/abandonallclaims`, `/abandontopclaim`. Admin/staff set:
`/adminclaims`, `/adjustclaimblocks`, `/adjustbonusclaimblocks`, `/deleteclaim`,
`/deleteallclaims`, `/transferclaim`, `/ignoreclaims` (player utility). GP's golden-shovel
creation/resize flow and stick investigation are core UX parity items.

## 4. GP flatfile data-store layout (importer/exporter reference)

`plugins/GriefPreventionData/`:
- `ClaimData/<id>.yml` — YAML: `Lesser Boundary Corner: world;x;y;z`,
  `Greater Boundary Corner: world;x;y;z`, `Owner: <uuid>` (empty = admin claim),
  `Builders: []`, `Containers: []`, `Accessors: []`, `Managers: []` (UUID strings,
  player names in legacy files, or `public`), `Parent Claim ID: -1`, `inheritNothing: false`.
- `ClaimData/_nextClaimID` — plain integer.
- `PlayerData/<uuid>` — plain text: line 1 blank (legacy), line 2 accrued int,
  line 3 bonus int.
- `_schemaVersion`, `config.yml`, `messages.yml`, `bannedWords.txt`, `Logs/` — not imported
  (config values were transcribed into ExcelClaims' own config; messages are ours).
