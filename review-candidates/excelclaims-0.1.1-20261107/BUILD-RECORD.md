# ExcelClaims 0.1.0 — BUILD RECORD

**Commission:** Mike, Nov 7, 2026, 12:09 AM CST — "overnight make me a
version of grief prevention that is ours instead. Excel Claims."
**Built:** Nov 7, 2026 (overnight). **Authors:** Iris / Mike.
**Build ID:** EXC-CLAIMS-0.1.0-WRV-20261107-01-7c1a3f
**Status:** Built, tested, rig-proven. **NOT installed** — swap packet
filed PENDING in the install queue (see INSTALL-PACKET.md).

## Clean-room statement

ExcelClaims is an original clean-room reimplementation, written from
GriefPrevention's observable behavior on WRV, its public API surface
(signatures verified with `javap` against the pinned 16.18.7 jar —
signature reference only), the live WRV configuration captures, and
its on-disk data formats. **No GriefPrevention source code was
copied, decompiled, or adapted; no GP source tree exists in the
working area.** Every shipped method body — engine and facade alike —
is original code written for this project in this build session. The
one GP artifact consulted is the compiled jar, for `javap` signature
dumps. Full basis: SOURCE.md.

## What shipped

- **Jar:** `ExcelClaims-0.1.0.jar` (SHA-256 in the records package;
  deterministic double-build, byte-identical).
- **Source:** `ExcelClaims-0.1.0-src.zip` (sources, tests,
  link-check stubs, build script, this record, SOURCE.md,
  INVENTORY.md).
- **Engine** (`com.winkreyes.excelclaims`): claims + subdivisions,
  the trust tiers (Access / Container / Build / Manage, plus Edit for
  owners/managers and a view-only Inventory tier), claim blocks
  (initial 100, accrual 100/hr, max accrued 80,000, bonus, per-player
  limit override), overlap denial with adjacency allowed, admin
  claims, golden-shovel creation/resize/subdivision/admin modes,
  stick investigation, glowstone-corner visualization, expiration
  (chest 7d / unused 14d / all 60d with the ≥10,000-total /
  ≥5,000-bonus exemptions), the full protection set at live values
  (theft, creatures, buttons/switches, fence gates, ender pearls +
  refund, raid triggers, villager trading, lecterns, /sethome access
  list, PvP protection in player/admin/admin-subdivision claims with
  15 s combat timeout and logout punishment, explosion/fire/piston
  rules), first-chest auto-claim (radius 4 → 9×9), the 26-command
  player/staff set from the inventory, GP-format importer + exporter,
  and a fail-soft ExcelProtect audit sink (claim create/delete and
  `claim_trust_changed` publishes ride Protect's API; a Protect
  outage can never break claiming — BossTrial-style isolation).
- **Config-first:** every behavioral knob and player-facing message
  lives in a sectioned, commented `config.yml` seeded with the live
  values (36 parity items, INVENTORY.md §2). Buy/sell claim blocks
  ship disabled exactly as live (the commands answer
  economy-disabled); a dormant reflective Vault bridge is present if
  the economy is ever switched on in config.

## Facade approach (the compatibility decision)

**Chosen:** ExcelClaims ships original facade classes in package
`me.ryanhamshire.GriefPrevention` and its plugin.yml registers the
Bukkit plugin name **"GriefPrevention"** (version/description/authors
brand ExcelClaims 0.1.0; main = the facade plugin class). Every one
of the 11 inventoried consumers (35 touchpoints, INVENTORY.md §1)
keeps working with **zero rebuilds and zero re-points**:

- *Compile-time* (Landmarks `GpHook`, AutoSort `GpRightsHook`,
  Protect `GpAdapter`, AuditLog `GpBridge`): all four adapter files
  compile **unchanged** against the facade classes — proven in the
  build (link-check step) and again live on the rig.
- *Reflection* (TreeCleanup, Guide, Leaderboard, Shops, BossTrial,
  the two menus' childproof screens): every probed field, method,
  and enum constant exists with the probed shape (unit-tested
  reflectively in FacadeSignatureTest; name-lookup and `dataStore`
  field reads exercised on the rig).
- *Magic* (third-party): its integration activates on
  `plugin instanceof GriefPrevention`, reads `instance`,
  `getClaimAt(loc, false, null)`, `Claim.allowEdit(Player)`, and the
  `config_pvp_noCombatInPlayerLandClaims` field — all present with
  matching shapes (rig-probed).

**Rejected alternative:** registering as "ExcelClaims" and
re-pointing consumers. It would have forced rebuilds of four house
plugins, broken every reflection probe that looks the plugin up by
the name "GriefPrevention", and silently disabled Magic's claim
integration (Magic has no ExcelClaims hook). The facade's cost —
our plugin answering to the old name at the Bukkit layer — is
confined to plugin-list output, and /excelclaims status states the
registration openly.

### Two behavior interpretations, evidence-based

1. **`getPermission` reporting precedence.** Landmarks' rig-proven
   flow (Nov 5, 2026, against real GP) grants a plot buyer Build via
   `setPermission`, adds the buyer to `managers` directly, saves,
   re-reads — and `getPermission(buyer)` reports **Build**, while
   the managers membership (and every rights check) still counts in
   full. ExcelClaims reproduces exactly that: `Trust.reportedOn`
   reads grant lists in Build → Container → Access → Manage order
   for the *reported* tier, while rights checks (`checkPermission`)
   use the ladder where Manage implies everything. Pinned by unit
   test (`landmarksDualMembershipContract`) and rig probe
   (`plot-dual-membership`).
2. **`isAdminClaim()` is top-level-only.** AutoSort SPEC §6.4
   requires the renter-as-manager rule inside Landmarks plots and
   the staff rule inside the top-level admin claim; GP titles
   subdivisions to the parent owner (Landmarks TEST-REPORT), so a
   plot's owner is null — the facade therefore reports
   `isAdminClaim()` true only for ownerless **top-level** claims.
   Engine-side protection still treats admin subdivisions as
   admin-protected (explicit top-claim check). Pinned by unit test
   (`adminClaimFacadeSemantics`) and rig probes (`plot-shape`,
   `autosort-renter`, `autosort-stranger`).

Other documented interpretations: claims protect the full column
from the claim's lesser Y to the sky (the stored greater Y is the
click height, reported faithfully); `allowEdit` passes the Build
tier (the gate Magic relies on); Inventory (view-only) holders fail
`allowContainers` in 0.1.0 — no view-only GUI ships, so view-only
trust never silently becomes take-trust; "unused" expiration uses
the created==modified approximation recorded in the code.

## Migration (Phase 3) — verdict: zero-loss both ways, one cosmetic byte

- **Importer:** GP flat files → ExcelClaims store (native files keep
  GP's core keys, so the store stays human-diffable; Excel
  bookkeeping lives in clearly-separated extra keys /
  `player-extra.yml`). Orphaned subdivisions are re-rooted to
  top-level and **reported**, never dropped silently. Legacy name
  entries and `public` entries survive verbatim.
- **Exporter/rollback:** ExcelClaims → pure GP-format files (exactly
  GP's 9 claim keys; player files in the old byte shape), written to
  `GriefPreventionData-restore`. The swap's primary rollback remains
  the pre-swap backup (see packet); the exporter is the second,
  data-level path.
- **Round-trip proof, unit level:** synthetic GP set shaped like
  WRV's (real owners/corners, trust lists incl. `public` + a legacy
  name, an admin claim + plot subdivision, Bedrock-form UUIDs):
  import → verify counts + spot content → export → re-import → deep
  model equality. **Lossless.**
- **Round-trip proof, rig level (real data):** the actual Oct 15,
  2026 backup claim/player files + hand-added spawn admin claim and
  plot subdivision + trust entries on a real claim: import reported
  **8 claims, 5 player accounts**; status reconciled **8 (1 admin,
  1 subdivision), 5 players**; the exported restore set's ClaimData
  is **byte-identical** to the staged GP files (all 8 claims), and
  PlayerData matches value-for-value.
- **The one documented degradation:** ExcelClaims' view-only
  Inventory tier has no field in GP's files; on export it returns as
  ordinary Container trust (the trust survives; the view-only nuance
  does not). Pinned by a unit test so it can never happen silently.
  No WRV player holds Inventory trust today, so the live migration
  is unaffected. Separately, exported player files omit one trailing
  blank line the backup copies carry — values identical; GP parses
  positionally, so it is inert.

## Tests — honest tallies

- **Unit (JUnit, in the build): 45/45 pass.** Trust matrix incl.
  the AutoSort configure case and both interpretation contracts
  (12), claim rules — create/charge/overlap/adjacency/too-small/
  blocks/subdivisions/delete-parent/transfer/resize/auto-claim/
  world-gating/claim-limit (13), accrual vs live values incl.
  cap and non-dividing rates (7), import/export round-trip (5),
  store round-trip (2), facade signatures (6). Total 45.
- **Link-check (in the build): PASS** — Landmarks `GpHook`+`GpAdapter`,
  AutoSort `GpRightsHook`+`GpRights`, Protect `GpAdapter`, AuditLog
  `GpBridge` compile unchanged against the facade (the latter two
  with compile-only stubs for their own plugins' internal classes).
- **Rig (Paper 26.3-231, real server):** boot 1 — enable clean,
  console import/status/export over the staged real set, counts
  reconciled (above). Boot 2 — **FACADE-SMOKE 14/14 PASS**: Magic
  instanceof/fields, world gating, imported claims resolve (spawn +
  Kailey's + Marvin's), trust import spot-checks, admin/plot shape,
  dual-membership contract, AutoSort renter/stranger/staff probes,
  childproof `checkPermission` probe, Guide player math (Kailey:
  accrued 1844, remaining 745, 4 claims — matches hand computation
  from the staged files), and the full Landmarks subdivision flow
  live (create → grant → save → re-read → delete).
- **Not covered (stated plainly):** no live-player rig pass (nobody
  can swing a shovel headlessly) — shovel/tool flows and protection
  listeners are unit-covered at the service/trust level and code-
  reviewed against the live config, but the in-game feel pass is a
  post-install verification step in the packet, same as every house
  plugin. The Oct 18 claim set (10 claims) post-dates the rig's
  Oct 15 staging set (6 claims + 2 hand-added); the packet's
  live-reconciliation gate exists precisely to re-verify counts
  against the live store inside the swap window.

## Deliberate non-goals (GP features WRV does not use — out of scope)

Siege mode; the GP spam filter / SmartBan / banned-words system;
MaxPlayersPerIpAddress; nature restoration on claim expiry;
`/givepet`; manual claim-block delivery to offline players;
ProtectFreshSpawns; death-drop protection in claims (not enabled on
WRV); MySQL/SQLite storage backends (WRV runs flatfile; the importer
is flatfile-only); GP's creative-mode claims (WRV runs Survival
only). Each is absent because the live config disables it or WRV
never used it — the parity target is GP-as-WRV-uses-it (36 items),
not all of GP. Any of these can be added later behind config; none
is silently half-present.

## Surfaces

ExcelClaims is invisible as a "new" plugin to players (same plugin
name, same commands, same tools), so the same-pass surfaces reduce
to: this record + SOURCE.md + INVENTORY.md (records package), the
install-queue MANIFEST entry, and — at install time — the Guide /
What's New / website updates named in the packet's post-swap steps
(the swap makes ExcelClaims the claims plugin; the Guide's claims
answers stay accurate because commands and behavior are parity).

---

# ExcelClaims 0.1.1 — BUILD RECORD (expiration safety fix)

**Commission:** Iris (parent), Nov 7, 2026 — fix the expiration
incident of the same morning. **Built:** Nov 7, 2026.
**Authors:** Iris / Mike.
**Build ID:** EXC-CLAIMS-0.1.1-WRV-20261107-02-5a1e9c
**Jar:** `ExcelClaims-0.1.1.jar` — 97,503 B, SHA-256
ff5ea0810d1df5866a423ac126e3145354bca25d2600cfc42a5536362b540706
(deterministic double-build, byte-identical; source zip hash in
SHA256SUMS). **Status:** Built, tested, rig-proven. **NOT
installed** — swap packet filed PENDING (INSTALL-PACKET-0.1.1.md).

## The incident (Nov 7, 2026, ~04:58 CST)

The 0.1.0 swap imported the live GriefPrevention store correctly —
the in-window reconciliation gate read 10 claims / 11 accounts,
exact. But GP's flat files carry no dates, and the 0.1.0 importer
left every imported claim's created/modified stamps and every
player's last-seen stamp at zero. ExcelClaims' expiration sweep
(Core schedules it every minute; `lastExpirationRun` starts at 0,
so the first tick ~1 minute after any boot is always due) computed
each owner's last-seen as `max(createdAt, lastModifiedAt)` = 0,
read "days away" as ~20,000, and expired all nine imported player
claims at the first due tick of the 04:57 boot (the spawn admin
claim is immune by design). No data was destroyed: the GP
originals in `plugins/GriefPreventionData/` were never touched,
and the claims were restored the same morning by freezing the
live config clocks at 99999 and re-importing (recovery window
08:00–08:11 CST, backup "ExcelClaims recovery 20261107"; a
field-level diff of all 10 recovered claims against the GP
originals matched on every shared field). 0.1.1 makes the defect
class impossible; its install packet restores the sane clocks.

## The fixes (only these)

- **A — importer date seeding (`GpImporter`).** An imported claim's
  created/modified stamps are seeded from the GP claim file's
  on-disk mtime (the last moment GP itself touched the claim) —
  but only where the file carried no date of its own; carried
  dates always win (pinned by test). An imported player's
  last-seen is seeded from Bukkit's `OfflinePlayer.getLastPlayed()`
  when a server is running, else the player file's mtime, else 0.
  The Bukkit consult is guarded exactly like `Core.nameOf`, so
  headless runs degrade cleanly and unit tests exercise the real
  code path.
- **B — sweep safety (`ClaimService.runExpirationIfDue`).** The
  last-seen basis is now the NEWEST positive evidence across all
  sources: the stored last-seen stamp, Bukkit's last-played record
  consulted live in the sweep, and the claim's created/modified
  stamps. If every source is silent (basis ≤ 0), the claim NEVER
  expires; such claims are counted and reported in one log line
  per run ("expiration check skipped N claim(s) — owner last-seen
  unknown"), never spammed per claim.
- Nothing else changed: facade, trust semantics, config keys,
  GP-format export (still exactly GP's nine keys; a rollback
  re-import now also benefits — its claims seed from the export
  files' mtimes instead of zero).

## Named design notes (deliberate choices)

1. **The basis widening is one-directional.** 0.1.0's chain used
   the stored stamp when present, else the claim stamps. Taking
   the maximum across sources (plus the new Bukkit source) can
   only ever keep a claim LONGER than 0.1.0 would have — e.g. a
   claim a manager edited recently no longer expires on a stale
   owner stamp alone. Expire on proof of absence, never on
   absence of proof.
2. **Mtime seeding is a floor, not the defense.** A GP claim file
   untouched for 200 days seeds a 200-day-old created stamp; what
   protects an ACTIVE owner is the Bukkit last-played seed (A)
   and the sweep's live consult (B). Rig B boot 2 proves the live
   consult carries a claim on its own (stored stamp zeroed by
   hand, claim stamps 200 days old, owner saved purely by
   Bukkit's record).
3. **Unknown stays unknown.** A 0 basis is not treated as "very
   old" or as "brand new" — it is excluded from expiration
   entirely until some source speaks. The affected claims are
   exactly the ones with no evidence either way; on WRV that is
   the correct side to err on.

## Tests — honest tallies

- **Unit (JUnit, in the build): 55/55 pass** — the 45 from 0.1.0
  unchanged, plus 10 new in `ExpirationSafetyTest`: basis math
  (newest-evidence across sources), seed choice (Bukkit → mtime →
  unknown), claim-date seeding from file mtime, player last-seen
  seeding from file mtime headless, carried-dates-win, the exact
  incident shape never expires (record AND file survive), a
  genuinely absent owner (100 d on every source) still expires,
  an active owner survives 300-day-old claim stamps, the
  first-chest short clock still expires at 30 days (isolated from
  the other clocks), and the block-exemption still protects.
- **Link-check (in the build): PASS** — the same four consumer
  adapters compile unchanged against the 0.1.1 facade.
- **Rig A (Paper 26.3-231, the real Oct 15 staged set;
  `rig/server-0.1.1/`, logs `rig/boot1-0.1.1.log` +
  `rig/boot2-0.1.1.log`):** boot 1 — enable line carries the
  0.1.1 build ID; import 8 claims / 5 accounts; status reconciles
  8 (1 admin, 1 subdivision) / 5; export byte-identical to the
  staged GP files (all 8 claims + `_nextClaimID`). Boot 2 —
  **persistence gate:** the fresh boot's enable line reads
  "8 claims, 5 player accounts" loaded from disk, and
  **FACADE-SMOKE passes 14/14 unchanged**, resolving the imported
  claims from that disk-loaded store.
- **Rig B (expiration fixture; `rig/server-expire/`, staging
  `rig/stage-expire.py`, values in `rig/expire-fixture.json`,
  logs `rig/expire-boot1.log` + `rig/expire-boot2.log`):** the
  Bukkit records themselves are probe-verified — a RigProbe
  driver plugin prints `Bukkit.getOfflinePlayer(...).getLastPlayed()`
  for the three fixture owners and reads back exactly the staged
  stamps (Andy = now, Alice = now−100 d, Ghost = 0) from
  CraftBukkit-format playerdata files. Boot 1 — import 2/2, then
  a 100 s soak past the first due tick: the sweep expires Alice's
  claim ONLY ("expiring claim 1", file deleted, status 2 → 1);
  Andy's claim survives, and the written store proves the seeds
  landed exactly: his claim file carries Created/Modified = his
  GP file's staged mtime (now−200 d) and his stored last-seen =
  his Bukkit last-played (NOT the 200-day-old player-file mtime).
  Boot 2 — the recovered-store shape: Andy's stored stamp zeroed
  by hand and a Ghost claim added with every date 0 and no Bukkit
  record; after the soak, status still reads 2 claims, both files
  remain on disk, the only sweep output is the one-line
  skipped-unknown note for Ghost, and Andy's survival is
  attributable solely to the sweep's live Bukkit consult.
- **Not covered (stated plainly):** the same live-player feel
  pass as 0.1.0 (post-install packet step). No other behavior was
  touched, so no other rig surface was re-run beyond the facade
  smoke set, which is the consumer contract.
- **Build-session note (disclosed, not hidden):** across the
  session's suite runs, one run reported a single test failure
  (54/55); the failing test was not identified — that run's
  detailed output was not retained — and the failure did not
  reproduce in repeated back-to-back suite runs before or after.
  The shipped artifacts come from a fully green build run
  (55/55), with the jar byte-identical across every run.
