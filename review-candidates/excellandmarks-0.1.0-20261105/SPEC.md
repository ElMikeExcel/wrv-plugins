# ExcelLandmarks 0.1.0 — SPEC

**Status:** Build spec, written Nov 5, 2026 from the settled design contract
`~/workspace/village-identity/REMAP-DIRECTION-20261103.md`. Mike gave the GO
Nov 5, 2026 ("Yes to landmark") and adopted the Nov 3 price sketch as config
defaults the same day. Settled policies in the direction doc are NOT open
questions; this spec implements them as written.

**Provenance / decisions already settled (do not relitigate in build):**
- Spawn is a huge premade pasted build; players land there and teleport out.
  Landmarks give its places a purpose: naming rights, rent-vs-buy.
- The ADMIN SIGN is the whole admin interface. Placing a `[Landmark]` sign
  registers the spot; nobody sets up claims or registry rows by hand.
- Two product types: **LANDMARKS** (naming rights only — sign + registry, no
  build rights) and **PLOTS/AREAS** (naming + build rights via a
  GriefPrevention subdivision the plugin creates automatically).
- Prices are CONFIG-KEYED, never hardcoded (config-first fleet pattern).
  Defaults adopted by Mike Nov 5, 2026: rent minor spot $500/mo, landmark
  $1,500/mo, heart piece $3,000/mo; buy $10,000 / $25,000 / $50,000.
- Rental-lapse policy (settled Nov 4, 2026): 1 week of grace, then
  (a) container contents + loose items packed into a redemption box,
  (b) placed-block structures forfeit and the plot clears,
  (c) block MATERIALS tallied from ExcelProtect placement attribution and
  returned as item stacks in the same box. Pre-tracking placements are
  indistinguishable — accepted limit. **The redemption box NEVER expires**
  (explicitly different from ExcelProtect Lost & Found's 14-day window).
- Console-admin rule (fleet standard): every state-changing support
  operation has a console-runnable admin command (§8).
- Menu standard (fleet): every player-facing action is reachable from the
  compass menu system (§7); the command is the twin, never the only door.
- Platform parity: Java + Bedrock get the same menus, prices, and rules.

**Mike-directed addition (Nov 5, 2026, ~4:59 AM CST):** tapping a landmark
sign must POP UP A MENU presented through the server compass menu system —
the ExcelMenu 2.0 dual-renderer presentation (native Floodgate form on
Bedrock, inventory menu on Java), the same presentation players get from
the compass. NOT chat text, NOT a bespoke one-off GUI. The purchase /
renew / release flows run through this menu, with a confirm step before
any Vault charge (§6).

---

## 1. Spots and tiers

A **spot** is one registered sign location. Fields: id, world + sign block
coords, display name, type (`LANDMARK` | `PLOT`), tier (`SPOT` | `LANDMARK`
| `HEART` — the price ladder, independent of type: a heart-tier LANDMARK is
a naming-only heart piece), rent price (cents), buy price (cents), plot
half-extent (PLOT only), state, owner UUID + last-known name, mode
(`RENTED` | `OWNED`), paid-through timestamp (rented), GP claim id (PLOT,
nullable), created timestamp.

Prices resolve at registration: tier defaults from config, overridable per
spot afterwards via admin command (never by re-placing the sign). "Month"
is the config rent period: `rent-period-days: 30` by default.

## 2. Registration — the admin sign

An admin (permission `landmarks.admin`) places any sign and writes:

```
Line 1: [Landmark]
Line 2: <display name>            (blank → "Unnamed Spot")
Line 3: <TYPE>:<TIER>             (e.g. PLOT:HEART, LANDMARK:SPOT; blank → config defaults LANDMARK:SPOT)
Line 4: <plot half-extent>        (PLOT only, integer 1–25; blank → tier default size)
```

On `SignChangeEvent` the plugin validates line 1 + permission, registers
the spot, persists it, and rewrites the sign in its live format (§3). A
`[Landmark]` sign by a non-admin is not registered and the marker line is
stripped (the sign stays a plain sign). Breaking a registered sign is
blocked for everyone; an admin breaking one with `landmarks.admin` gets a
chat warning and must run `/landmarks admin remove <id>` — deregistration
is always deliberate, never a side effect of a punch. Registered signs
also refuse edits (SignChangeEvent on an existing registered sign is
cancelled for non-admins; admins edit via commands, not by re-typing).

## 3. The sign face (live format)

The plugin owns the sign text after registration:

```
Line 1: [Landmark]            (gold)
Line 2: <display name>
Line 3: Available             — or the owner's name (rented / owned)
Line 4: Rent $500/mo · Buy $10,000     — or "Rented til Nov 20" / "Owned"
```

State changes (purchase, renew, lapse, release) rewrite lines 3–4 in the
same tick. The sign is the at-a-glance state board; the menu (§6) is the
full detail.

## 4. Purchase / renew / release

All flows run through the spot menu (§6) and charge Vault **only on the
confirm step**:

- **Rent** (available spot): charge rent price → owner set, mode RENTED,
  paid-through = now + rent period. PLOT: GP subdivision created (§5).
- **Buy** (available spot, or own rented spot — buying out a rental is
  allowed and simply converts it): charge buy price → mode OWNED, no
  clock. A renter who buys out keeps their subdivision; no refund of the
  current period (stated on the confirm screen).
- **Renew** (own rented spot, active or in grace): charge rent price →
  paid-through extends from `max(now, paid-through)` — renewing early
  stacks time, never shortens it (ExcelCosmetics rental precedent:
  rentals keep their time).
- **Release** (own spot, rented or owned): confirm screen warns that
  anything left in a PLOT is packed into the owner's redemption box.
  On confirm, a PLOT runs the same pack-and-clear as a lapse finalize
  (§5 sweep steps 1–3, minus grace) so there is exactly one sweep code
  path; a LANDMARK simply frees. GP subdivision deleted. Spot returns to
  Available.

Insufficient balance or a failed withdrawal changes nothing and the menu
re-opens with the failure line shown.

## 5. Rent clock, grace, lapse finalize

- The clock is checked on a repeating task (`clock-tick-minutes`, default
  15) and on enable (catch-up after downtime), and via
  `/landmarks admin sweep`.
- `now > paid-through` → **GRACE**: spot stays flagged as the renter's,
  sign line 4 shows the grace deadline, subdivision stays. Renewing in
  grace restores ACTIVE with time stacked from the old paid-through.
- `now > paid-through + grace-days (7)` → **FINALIZE**, PLOT spots:
  1. **Pack**: every container (TileState inventory) inside the region is
     emptied into the redemption box; loose item entities inside the
     region are collected the same way.
  2. **Materials tally**: ExcelProtect's blocks stream
     (`plugins/ExcelProtect/blocks-*.jsonl[.gz]` — action / block /
     player_uuid / world / x / y / z per line) is reduced to the latest
     action per position inside the region; positions whose latest action
     is `place` by the renter and whose live block still matches are
     tallied as material stacks into the same box. Unattributed blocks —
     the original build, other players', pre-tracking placements — are
     never tallied and never cleared (the accepted limit).
  3. **Clear**: attributed blocks are set to air (drops suppressed;
     containers already emptied in step 1). The plot is clean for the
     next renter; their structure is forfeit, its materials are in the
     box (step 2).
  4. GP subdivision deleted (§5 adapter; absence is a no-op).
  5. Spot → Available, owner cleared, sign rewritten.
  LANDMARK spots skip steps 1–3 (nothing to pack or clear).
- **Redemption box**: a chest placed at the first free cell beside the
  sign (spiral search, config `redemption.search-radius`), filled with
  the packed items; overflow stacks go to further chests stacked above.
  The box is recorded in `redemptions.yml` (owner, spot, chest locations,
  creation time). It **NEVER expires**: no sweep ever removes or empties
  a redemption box, and non-owners cannot open or break one. If no free
  cell exists, the items are held in virtual storage instead and the
  owner claims them from the My Landmarks menu (§7) — still with no
  expiry. One code path serializes the item list; only the delivery
  differs.

### GriefPrevention adapter (fail-soft)
Compiled against GriefPrevention 16.18.7 (pinned hash), `softdepend`.
At purchase of a PLOT: find the claim at the sign location
(`DataStore.getClaimAt`); if it exists, `createClaim` with that parent
and the region corners → a subdivision. **Rights model (rig-proven
Nov 5, 2026):** GP subdivisions cannot change owners —
`DataStore.changeClaimOwner` throws `NoTransferException` for them —
so title stays with the admin claim and the buyer receives GP trust on
the subdivision: `ClaimPermission.Build` + manager membership, which
is exactly what building (and trusting friends) takes in GP. The grant
is verified by re-reading the claim; a grant that doesn't take deletes
the subdivision and degrades to naming-only. If GP is absent, or no
parent claim covers the sign, or creation fails: naming still works,
the spot records `claim-id: null` and the enable/status lines say so.
Nothing about GP is ever set up by hand; the one big admin claim over
the spawn build is assumed to exist (its absence degrades PLOTs to
naming-only, loudly in `/landmarks admin status`, never silently).

### ExcelProtect attribution (fail-soft)
Read-only file integration (Protect has no query API for this; its
inspect index is in-memory). If Protect is absent or its blocks files are
missing/unreadable, the tally is empty, the finalize logs one line
(`attribution unavailable — materials tally empty`), and the rest of the
sweep proceeds. Region = cuboid centred on the sign X/Z with the spot's
half-extent, Y from `signY − plot-y-below (3)` to `signY + plot-y-above
(12)`, both config-keyed.

## 6. The sign menu (Mike-directed, Nov 5, 2026)

**Clarification pinned by Mike (Nov 5, 2026, ~5:06 AM CST), following
the server's ExcelShops tap-on-sign→menu precedent:** the sign tap
itself opens the menu — no compass item in hand, no command. The menu
presents only the options available for that spot and that viewer, and
the viewer's choice drives the flow (with the confirm step before any
charge). Options a viewer can't take never appear as dead buttons.
This matches the design below exactly; no adjustment was needed.

Right-clicking a registered sign opens that spot's menu. One **menu
model** (core, pure Java) is built per open — title, content lines,
ordered buttons — and rendered two ways, the ExcelMenu 2.0 dual-render
pattern:

- **Bedrock**: a native Floodgate **SimpleForm** (Cumulus — the same
  form stack ExcelMenu's Bedrock renderer uses, reached through the
  Floodgate API), buttons in model order, response mapped back to
  button ids.
- **Java**: a chest inventory menu in the ExcelMenu visual conventions
  (same title, button items with label + lore from the model), clicks
  mapped back to the same button ids.

ExcelMenu's public API (verified against the phase-2 source,
`ExcelMenuApi.java`) is entry-registration only — it deliberately offers
no dynamic-screen hooks — so the spot menu renders on the same dual
stack ExcelMenu itself uses, from the one shared model; the compass
door (§7) is a true ExcelMenu registry entry. Content of the spot menu:

- Title: spot name. Lines: type + tier; state — `Available`,
  `Rented by <name> until <date>`, `Owned by <name>`, for the owner in
  grace `Rented by you — renew by <date>`; prices for this spot
  (rent/mo + buy); plot size for PLOTs.
- Buttons by viewer and state:
  - Available, any viewer: **Rent**, **Buy** — each opens a confirm
    screen (`Rent <name> for $X/mo?` → **Confirm** / **Back**); the
    Vault charge happens only on Confirm.
  - Own rented spot: **Renew** (confirm), **Release** (confirm with the
    pack warning).
  - Own owned spot: **Release** (confirm).
  - Own rented spot in grace: **Renew** is emphasised (first button,
    labelled with the grace deadline).
  - **Someone else's spot: view-only** — state and prices, no action
    buttons (§9 tests pin this).
  - **Close** on every screen; **Back** on confirm screens.

### Interact routing (Oct 15 ExcelShops lesson)
`PlayerInteractEvent` at `EventPriority.HIGH`, main hand only
(`EquipmentSlot.HAND`), `RIGHT_CLICK_BLOCK` only. A registered sign:
cancel the event (`setCancelled` + `setUseInteractedBlock(DENY)`) and
open exactly one menu; a per-player 300 ms debounce kills the duplicate
fire some clients produce. Unregistered signs and all other blocks are
ignored completely — never cancelled, never a menu — so MenuJoin, shop
signs, and vanilla sign editing are untouched. The routing decision is a
pure core function (`TapRouter`) the listener delegates to, and the
tests pin every branch.

## 7. Compass door — My Landmarks

`menu-entry/landmarks.yml` (ExcelMenu 2.0 registry entry, `server_menu`,
compass group `me`) runs `[player] landmarks`, opening **My Landmarks**
through the same dual-render path: the viewer's spots as buttons (name,
state line, renew-by date) each opening that spot's menu (§6), plus a
**Claim redemption** button when — and only when — the viewer holds
unclaimed virtual-fallback items. `/landmarks` with no arguments is the
command twin of the door. Every player-facing action (rent, buy, renew,
release, claim) is reachable from a menu; no action is chat-only.

## 8. Commands and permissions

- `/landmarks` — open My Landmarks (players). Alias `/plots`.
- `/landmarks open <id>` — open one spot's menu (used by the sign tap
  internally; also the support door when a sign is unreachable).
- `/landmarks admin status` — spot counts by state, adapter states
  (GP / Protect / Floodgate), clock state. Console OK.
- `/landmarks admin inspect <id>` — full spot record + claim id +
  paid-through / grace math. Console OK.
- `/landmarks admin register <name> <TYPE> <TIER> [size]` — register the
  spot at the sign the admin is looking at (the command twin of the
  sign flow, for support). In-game admin only (needs a location).
- `/landmarks admin remove <id>` — deregister (finalizes a rented PLOT
  first, same sweep as release). Console OK.
- `/landmarks admin release <id>` — force-release a spot (same finalize
  path as lapse). Console OK.
- `/landmarks admin setprice <id> <rent|buy> <dollars>` — per-spot price
  override. Console OK.
- `/landmarks admin setpaid <id> <yyyy-MM-dd>` — support tool: move a
  rental's paid-through date (e.g. comping a player). Console OK.
- `/landmarks admin sweep` — run the rent-clock check now. Console OK.
- `/landmarks admin reload` — reload config. Console OK.

Permissions: `landmarks.play` (default true), `landmarks.admin`
(default op; the server console can always run admin commands).

## 9. Data, export, config

- `spots.yml` — the registry (source of truth), saved on every change.
- `redemptions.yml` — redemption boxes + virtual holdings; never pruned.
- `registry-export.json` — rewritten on every registry change in the
  house export pattern (id, name, type, tier, world/coords, state,
  owner name, paid-through) so the website + Guide can consume it later;
  0.1.0 ships the export only, no surface work.
- `config.yml` — tiers (rent/buy per tier), default type/tier, tier plot
  sizes, rent period days, grace days, clock tick minutes, region Y
  offsets, redemption search radius, Protect logs dir, all player-facing
  messages. Nothing player-visible is hardcoded.

## 10. Enable line and verification shape

`ExcelLandmarks 0.1.0 enabled: spots=<n>, gp=ON|OFF (subdivisions),
protect-attribution=ON|OFF, floodgate-forms=ON|OFF, rent-period=30d,
grace=7d` — the smoke/rig greps assert this line's shape plus zero
attributable SEVERE, per fleet practice.
