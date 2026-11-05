# TEST REPORT — ExcelLandmarks 0.1.0

Built Nov 5, 2026 in `~/workspace/landmarks-build/` per SPEC.md (the build
contract written from `~/workspace/village-identity/REMAP-DIRECTION-20261103.md`,
plus Mike's two Nov 5 menu directives, recorded in SPEC §6).

**Artifact:** `ExcelLandmarks-0.1.0.jar` — **77,996 B**, SHA-256
`466c14c4a4002b87fb72e399411355d4df442b8d19b6fcbc58122073521246dc`.
Deterministic: assembled twice per build with a hash assert, and two full
`build.sh` runs produced the identical SHA-256.

## Unit + property tests — 101,018 checks, 0 failures

| Suite | Checks | Covers |
|---|---|---|
| RentClockTest | 100,904 | Rent/grace boundaries to the millisecond (at paid-through = still ACTIVE, at grace end = still GRACE, +1 ms = LAPSED/due); renewal stacking (early renew stacks from paid-through, grace renew from now); hourly state classification across a full period+grace; 20,000-case randomized sweep (due ⇔ past grace end; renew formula `max(now, paidThrough) + period`) |
| AttributionTest | 29 | Protect `blocks-*.jsonl` parser (field order, malformed/non-numeric lines dropped, never thrown); latest-per-position resolution (later break beats earlier place, newer ts wins out of order); renter-only region tally (region edges inclusive, other players excluded, broken-after-place excluded, air excluded); file-level read through the real `ProtectAttribution` incl. duplicate collapse and missing-dir fail-soft; GP adapter `NONE` contract (create → null, delete → satisfied) |
| TapRouterTest | 11 | The one OPEN_MENU case (main-hand RIGHT_CLICK_BLOCK on a registered sign past the debounce) + every IGNORE branch (left click, off-hand echo, unregistered sign, air, physical) and the 299/300/301 ms debounce edges |
| SpotMenuModelTest | 42 | Available → Rent/Buy only; own rental → Renew/Buy Outright/Release; grace → Renew first, labelled with the deadline; own owned → Release only; **someone else's spot → Close is the ONLY button** (both modes) with the info view intact; confirm screens (exact charge, buy-out no-refund note only when buying out a rental, plot-release redemption warning); sign lines for all four states |
| CoreTest | 32 | Mike's adopted prices pinned as the config defaults ($500/$1,500/$3,000 rent; $10,000/$25,000/$50,000 buy; 30-day period, 7-day grace); money formatting; region inclusion; registry sign index, ownership queries, id sequence restore |

Purchase/renew/release *flow* logic is additionally covered end-to-end by
the rig (real Vault charges against EssentialsX), below.

## Live rig — Paper 26.3-235, two boots, SEVERE 0, RIG OVERALL PASS

Driver: NMS fake players firing REAL events (SignChangeEvent,
PlayerInteractEvent, InventoryClickEvent, BlockPlaceEvent) — the exact
path a phone tap takes. Evidence: `rig/evidence/boot-a.log`,
`rig/evidence/boot-b.log`.

**Boot A** (Vault + EssentialsX + GriefPrevention 16.18.7 + ExcelProtect
0.1.1 present) — **28/28 PASS.** Enable line:
`gp=ON (subdivisions), protect-attribution=ON`. Proven: admin parent
claim; sign registration rewrites the face (Available + both prices);
tap → menu → Rent → confirm (balance untouched before Confirm) → charged
exactly $500 → owner on registry + sign; subdivision created with the
buyer holding **Build** rights; guest gets a view-only menu (no action
buttons present in the inventory at all); unregistered sign opens
nothing; Protect records the renter's placements; grace holds a 3-day-old
rental and the sign reads `Grace til …`; a 10-day-old rental finalizes —
spot freed, sign reset, attributed blocks (2 glowstone + the placed
chest) cleared, subdivision deleted, and the redemption box beside the
sign contains exactly {5 diamonds (container contents), 3 emeralds
(loose items), 2 glowstone + 1 chest (attribution materials)}; console
`landmarks admin status` answers.

**Boot B** (GriefPrevention + ExcelProtect jars REMOVED) — **9/9 PASS.**
Enable line `gp=OFF, protect-attribution=OFF`; a LANDMARK and a PLOT both
register and rent naming-only (claim ids stay null, charges exact:
$10,000 → $9,500 → $6,500), the plot finalizes past grace with an empty
tally and no errors, the untouched landmark rental survives, and no
redemption is fabricated. **Fail-soft proven.**

### Rig-found defects, fixed and re-proven in the shipped jar

1. **GP subdivisions can't be transferred.** First GpHook design assumed
   claim ownership for the buyer; GP's `changeClaimOwner` throws
   `NoTransferException` for subdivisions, and `createClaim` with a
   parent silently titles the subdivision to the parent owner. Shipped
   model = GP trust: Build permission + manager membership for the
   buyer, verified by re-reading the claim (SPEC §5 amended).
2. **Protect path resolved one level too high.** `protect.logs-dir`
   (`../ExcelProtect`) was resolved against the plugins folder instead
   of this plugin's data folder, so attribution read a nonexistent
   `<server>/ExcelProtect`. Fixed at both call sites; Boot A then reads
   `protect-attribution=ON` and the tally works.
3. Rig-harness lessons (not plugin defects): paperclip needs the base
   server's `cache/` + `.paper/`; GP's data folder is
   `GriefPreventionData/` (wipe lists corrected); the driver now places
   signs at surface height so the redemption-box search is
   terrain-independent. The first Boot A attempt also ran with the
   GP/Protect jars missing entirely (the previous run's Boot B swap had
   removed them) — that accidental boot independently re-proved the
   fail-soft path before Boot B did it deliberately.

### Rig limitations (named, by design)

- **Bedrock form rendering is not rig-tested**: a fake player is a Java
  client, so the Floodgate SimpleForm send path can't be exercised here
  (fleet precedent — BrainGames Phase 1 had the same limit). The form is
  built from the same core model the tests pin, through a guarded
  adapter that falls back to the inventory menu if the send fails. The
  Bedrock tap→form proof is a named player proof in the install packet.
- ExcelMenu itself was not on the rig: the compass door is a registry
  entry file of the exact BrainGames shape (descriptor-parsed in
  build.sh), verified live at install via the registry count gate
  (85 → 86) in the packet.

## Deviations from the direction doc

None in policy. Two implementation notes, both evidence-backed above:
(1) plot build rights are GP **trust** (Build + manager) on an
admin-titled subdivision, because GP makes subdivision title transfer
impossible — the player-visible outcome (buyer builds, manages trust,
subdivision deleted at finalize) is the settled design; (2) the spot
menu renders through the same dual stack ExcelMenu 2.0 uses (Cumulus
SimpleForm + chest inventory from one core model) because ExcelMenu's
public API is entry-registration only — the compass door is a true
registry entry. The ExcelMenu-API judgment call was flagged to the
parent in the build handoff.
