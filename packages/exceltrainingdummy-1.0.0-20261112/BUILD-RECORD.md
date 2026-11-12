# ExcelTrainingDummy 1.0.0 — build record

Commission: board supervisor dispatch (Nov 12, 2026) — build
ExcelTrainingDummy 1.0.0 per the design of record at
`~/workspace/training-dummy-20261110/DESIGN.md` (Mike's direction,
Nov 10, 2026: copy WoW's Dummy Dome + GW2's Special Forces Training
Area; Jesús's original request; the instrument for the spell audit's
controlled tests).

Desk work only: built, unit-tested, rig-proven. Nothing installed,
nothing published, no MANIFEST edit (a draft entry is staged for the
board supervisor to file — `MANIFEST-ENTRY-DRAFT.md`).

---

## 1. WHAT WAS BUILT — design § by §

Standalone Paper plugin (Java, `--release 21`, house deterministic
javac recipe copied from ExcelTour 1.0.0), package
`com.winkreyes.exceltrainingdummy`. The dummy is a zombie with AI,
gravity, collision and daylight-burning off; its health pool is
**virtual** (core `TestSession`): the damage listener reads each
hit's final damage, records it, then zeroes the event's damage —
the entity never takes real damage, so no death event can fire.

| DESIGN | Implementation |
|---|---|
| §1 Spawn menu (/dummy): 1 / 3 / 5, remove-all, owner + despawn rules, config caps, flat-ground + claim checks | `DummyMenus` spawn menu (Java 54-slot + Bedrock form), `ExcelTrainingDummy.spawn/planSpots` (all-or-nothing: caps → zone → ground, every spot checked before any spawn), caps `caps.per-player` 5 / `caps.server` 25, idle despawn 10 min (`despawn.idle-minutes`), logout despawn, remove-all button + command |
| §2 Tiers Practice 1000 / Dungeon 5000 / Seal 20000 + Endless | `core/Tier` + config `tiers.*`; Endless = the chosen pool as a lap: lap finish prints the report, dummy resets to full after `endless.reset-seconds` (5) |
| §3 Checkpoints at each 25% + final report (duration, total, avg DPS, breakdown; HPS for healing tests) | `core/TestSession` checkpoint quartiles (25/50/75% consumed) + `core/ReportText`; healing during a hazardous test is tracked from EntityRegainHealthEvent and reported as HPS |
| §4 Boss-armor preset (display-scale x1000 presentation) | Per-setup + per-dummy toggle; every printed number scaled by `boss-armor.display-scale-factor` (1000); ledgers stay raw |
| §5 Hazardous Light / Medium / Intense, stops at 1 HP, Lethal toggle | `core/Hazard.pulseDamage` clamp (unit-proven); ticker pulses every `hazard.pulse-ticks` (40) for owners within `hazard.range-blocks` (20); damage per level in config (2/4/8) |
| §6 Execute preset (spawn at 25%) | Setup toggle pre-consumes 75% of the pool (past checkpoints pre-fired); dummy settings also carry an Execute reset |
| §7 Clean by construction | Virtual pool ⇒ no EntityDeathEvent, no XP (vanilla or mcMMO), no loot, no Jobs pay, no economy, no seal/boss credit, no DeathProtection interaction. Belt-and-braces: a death-event guard clears drops + XP if a dummy somehow dies. Summons (Tameable damagers, incl. projectile shooters) attribute to their owner as SUMMONS damage |
| §8 Bedrock parity | Same picks as Cumulus SimpleForms via the house reflection pattern (ExcelTour's BedrockForms, copied); report lines are the same chat text on both platforms; form buttons carry NO legacy color codes (the WandBars 0.3.14 contrast lesson) |
| Two doors §1 Compass menu entry | `/dummy` IS the entry point both server menus call (the ExcelTour contract). The ExcelMenu + ExcelBedrockMenu config entries are a same-pass install surface — designed in the draft MANIFEST entry, with the live server_menu headroom check as a GATE-VERIFY step |
| Two doors §2 Tap the dummy | PlayerInteractEntityEvent with a compass in the main hand: owner → that dummy's settings menu/form (tier, hazard, lethal, boss-armor, checkpoints, execute reset, remove); anyone else → live status + readout in chat. Without a compass, nothing intercepts the tap |

Claim checks: wilderness / own claim / admin training zone, via
`ClaimGate` — reflection against the claims plugin (WRV runs
ExcelClaims, the house GriefPrevention fork: plugin name
"GriefPrevention", `DataStore.getClaimAt`, `Claim.getOwnerID /
isAdminClaim / parent`; subclaims resolve to the parent's owner).
The pure verdict (`core/ZoneCheck`) is conservative: foreign claims
deny even with build trust; admin claims allow only the
`exceltrainingdummy.admin` permission (staff-run zones); a claims
plugin that is present but unreadable fails CLOSED. No claims
plugin at all ⇒ wilderness everywhere.

## 2. DESIGN DECISIONS MADE AT THE DESK (named, per the commission)

1. **Report breakdown comes from the plugin's own damage tracking,
   not ExcelDetails.** ExcelDetails 0.2.2 (source read in
   `~/workspace/details-meter-20261109/build-0.2.2/`) exposes no
   public segment API: `SegmentTracker` is an internal class, there
   is no service registration and no per-target/per-spell query
   surface another plugin can consume. Per the commission's
   instruction, no API was invented. Breakdown is therefore by
   source CATEGORY (Melee / Spells / Projectiles / Summons / Other)
   — what a damage event honestly proves (damager + cause). Spell
   NAMES are not carried by damage events; wiring the dummy into
   Details/Protect with a training stamp both meters agree on
   (DESIGN §3's "the two must agree") remains an integration item
   for a future Details/Protect packet — the dummy IS stamped for
   it (PDC keys `training`/`owner` + scoreboard tag
   `excelTrainingDummy`), and the damage event is deliberately NOT
   cancelled so the rest of the stack sees every hit.
2. **Virtual pool instead of a giant real health bar.** Vanilla's
   max-health attribute caps far below the 20,000 Seal pool; the
   virtual pool also IS the clean-by-construction mechanism (no
   death ⇒ no XP/loot/credit paths at all).
3. **Intake = `getFinalDamage()`** (after armor/resistance), the
   number a real target would have taken. The dummy mob's base
   armor is the zombie's 2 points (rig-proven: a 300 hit records
   295.2). Boss-armor preset is presentation scale only — it does
   not add a second armor layer to intake (DESIGN §4 describes a
   display-scale presentation, and that is what shipped).
4. **Hazard pulses target the owner only**, within 20 blocks and
   the same world — "pulses damage at the tester" (§5); other
   players standing nearby are not griefed by someone else's dummy.
5. **Admin training zone = admin claim + staff permission** (see
   above) — the conservative reading of §1; zone naming/flagging
   inside claims is a possible future refinement, not invented here.
6. **Compass-menu entry is a packet surface, not code** (ExcelTour
   precedent): the plugin ships the `/dummy` door; the draft
   MANIFEST entry carries the menu-entry design + headroom check.

## 3. PROOFS

- **Unit tests: 67 checks, 0 failed** (`TEST-RESULTS.txt`) — tiers,
  session/checkpoint/finish math, overkill capping, execute start +
  reset, endless laps, checkpoint toggle mid-test, ledgers, hazard
  clamp (lethal + non-lethal), zone verdicts (all five), spawn
  geometry, and ReportText rendering (duration/total/DPS line,
  category breakdown with percentages, hazard line, boss-scale
  x1000 rendering, checkpoint + status lines).
- **Rig (Paper 26.3, local): 11/11 PASS, SEVERE 0** — boot clean
  with Geyser + floodgate co-loaded; fake player spawns via the real
  command, stamp verified, virtual-pool intake proven event by
  event (295.2 for a 300 hit, single event, entity health unmoved),
  finish removes the dummy, 3-cluster + remove-all, console version
  line. Full account + limitations: `rig-1.0.0/RIG-RESULTS.md`.
  One first-boot anomaly (329.2 recorded once) did not reproduce
  across two instrumented runs and is reported there, not buried.
- **Double-build:** two clean compiles → two repacks →
  **byte-identical** jars (build.sh `cmp` gate).
- **Fingerprint gate:** `excel-build.txt` in the jar =
  `EXC-TDUMMY-1.0.0-WRV-20261112-01-178829` (see FINGERPRINTS.md).

## 4. ARTIFACT

- `ExcelTrainingDummy-1.0.0.jar` — **49,376 B**, SHA-256
  `b46250ed7a408a447749b8f62e10c432c235c9050a1e4dcff06a8138c516c098`,
  build id `EXC-TDUMMY-1.0.0-WRV-20261112-01-178829`.
- `ExcelTrainingDummy-1.0.0-source.zip` — **37,537 B**, SHA-256
  `85490d4aff4b333636960ec0add79a1e81bf5ec3883210514b18270b6813b829`.
- Fingerprints: Layer A — name ExcelTrainingDummy, author
  WinkReyesValley, version 1.0.0 real. Layer B — the build id in
  `BuildInfo` (fragment-assembled), `excel-build.txt`, the enable
  line, and `/dummy version`. Layer C — six markers, see
  FINGERPRINTS.md. Layer D — channel WRV.

## 5. NOT PROVEN HERE (named, not claimed)

- **Player proof owed after install:** Bedrock form pass (spawn +
  dummy settings forms on a real Bedrock client); Java menu clicks;
  tap-the-dummy with a compass (owner settings + non-owner readout);
  hazardous pulses felt in-game incl. the 1 HP stop and a Lethal
  Downed drill; checkpoint + report lines read in chat; endless lap
  reset; idle/logout despawn in real play; claim-gate against the
  live ExcelClaims (rig had no claims plugin).
- **Cross-plugin integration owed (future packets, not this one):**
  ExcelDetails segment agreement + ExcelProtect training-stamp
  logging (DESIGN §3/§7) — needs a Details/Protect-side API or
  stamp handling; this build only stamps and stays out of the way.
- **Compass menu surfaces owed:** ExcelMenu + ExcelBedrockMenu
  "Training Dummy" entries + What's New / Guide surfaces land with
  the install packet (same-pass surfaces rule) — designed in the
  draft MANIFEST entry; live layout headroom is GATE-VERIFY there.
- No live-server boot was run (desk rig only).
