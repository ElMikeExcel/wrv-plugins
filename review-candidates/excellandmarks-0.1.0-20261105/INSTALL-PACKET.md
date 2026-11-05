# INSTALL PACKET — ExcelLandmarks 0.1.0 (NEW PLUGIN)

**Status: PENDING** — filed Nov 5, 2026. Build record: `~/workspace/landmarks-build/`
(SPEC.md, TEST-REPORT.md, source under `src/`, tests under `tests/`).

**Class:** RESTART install, **emptiness-gated** (weekend countdown protocol
applies Fri/Sat). One NEW jar + one NEW ExcelMenu entry file. No existing
file is modified, no config surgery on any live plugin.

## What it is

Spawn-build naming / rent / buy, per `~/workspace/village-identity/REMAP-DIRECTION-20261103.md`
(Mike's GO Nov 5, 2026; prices adopted as config defaults the same day):

- An admin writes `[Landmark]` on a sign at a spawn spot → the spot registers
  (name, TYPE:TIER from the sign; prices from config).
- Players tap the sign → the spot menu pops up (native Floodgate form on
  Bedrock, chest inventory on Java — one model, dual render): available spots
  offer **Rent / Buy**, your own rental offers **Renew / Buy Outright /
  Release**, someone else's spot is view-only. Every charge has a confirm
  step. Vault charges happen only on Confirm.
- PLOT spots also grant build rights: the plugin auto-creates a
  GriefPrevention **subdivision** of the big spawn admin claim at purchase
  (no manual claim setup). If GP is ever absent, naming still works.
- Rentals run a 30-day clock with a **7-day grace**; after grace the plot
  finalizes — container contents + loose items are packed, the renter's
  placed-block **materials are tallied from ExcelProtect attribution** and
  returned as stacks, exactly those blocks are cleared, the subdivision is
  deleted, and everything lands in a **redemption box beside the sign that
  NEVER expires** (unlike Lost & Found's 14 days).
- Compass door: "My Landmarks" (ExcelMenu registry entry) — your spots,
  renew/release, redemption claims. Console admin surface:
  `/landmarks admin status|inspect|register|remove|release|setprice|setpaid|sweep|reload`.

## Gates (STOP and report on any miss)

1. Candidate jar must hash EXACTLY:
   `ExcelLandmarks-0.1.0.jar` — 77,996 B, SHA-256
   `466c14c4a4002b87fb72e399411355d4df442b8d19b6fcbc58122073521246dc`.
2. BACKUP-FIRST per the standing rule (fresh Shockbyte backup; cap-at-6
   prune discipline applies, prunees named in the install report).
3. Server verified empty (list = 0 twice) or weekend countdown protocol.
4. Live plugin census before: **78 plugins**; after: **79**.

## Steps

1. Upload `ExcelLandmarks-0.1.0.jar` → `plugins/ExcelLandmarks-0.1.0.jar`.
2. Upload `menu-entry/landmarks.yml` (from the build folder) →
   `plugins/ExcelMenu/entries/landmarks.yml`.
3. Restart the server.
4. From console: `excelmenu reload` (picks up the new door), then
   `landmarks admin status` (must answer; console surface proof).

## Boot-verify (all must hold)

- Done line reached; plugin count 79.
- Enable line (exact shape):
  `ExcelLandmarks 0.1.0 enabled: spots=0, gp=ON (subdivisions), protect-attribution=ON, floodgate-forms=ON, rent-period=30d, grace=7d`
  (spots=0 on first boot is CORRECT — no spots exist until the activation
  sign pass below. gp / protect-attribution / floodgate-forms must all read
  ON on the live server; any OFF = report, do not fail the install —
  naming still works — but the parent must know before the sign pass.)
- SEVERE 0 attributable to the install.
- GuildGate LOCKED count UNCHANGED from the pre-install read (3 of 5 at
  filing — never hardcode; read it in the window).
- ExcelMenu reload reports the registry at **86 entries** (85 live at
  filing + this door) with 0 failed.
- TPS 20.0 nominal.
- `plugins/ExcelLandmarks/` generated: config.yml (defaults = the adopted
  prices), spots.yml, redemptions.yml, registry-export.json.

## Rollback

Remove `plugins/ExcelLandmarks-0.1.0.jar` + `plugins/ExcelMenu/entries/landmarks.yml`,
`excelmenu reload`, restart. Nothing else was touched; player money moves
only happen through the plugin's own menu flows after activation. If spots
were already registered when a rollback is needed, keep the
`plugins/ExcelLandmarks/` folder — it is the registry of record.

## ACTIVATION — later in-game steps (need a person in the world; NOT part of this install)

The plugin is inert until spots exist. Activation is a one-time in-game
admin pass, done by Mike (or an op) in the world:

### Sign-placement how-to (for the admin)

1. Go to the spot at spawn. Place any sign so it faces arriving players.
2. Write the four lines:
   - Line 1: `[Landmark]`  ← the marker; this is what registers the spot
   - Line 2: the spot's name (e.g. `Fountain Corner`)
   - Line 3: `TYPE:TIER` — TYPE is `LANDMARK` (naming only) or `PLOT`
     (naming + build rights); TIER is `SPOT` ($500/mo · $10,000),
     `LANDMARK` ($1,500/mo · $25,000) or `HEART` ($3,000/mo · $50,000).
     Blank = LANDMARK:SPOT.
   - Line 4 (PLOTs only, optional): the plot half-size in blocks
     (1–25; blank = tier default 3/5/8). The plot is centred on the sign.
3. Finish the sign. The plugin rewrites it gold with the live state
   (Available + prices) and confirms in chat with the spot id (`lm-1`, …).
4. One sign per spot. Mistakes: `/landmarks admin remove <id>` frees the
   registry entry (the sign block itself stays — break it by hand after).
   Prices for a single spot can be overridden with
   `/landmarks admin setprice <id> rent|buy <dollars>`.
5. BEFORE the pass: the spawn build must sit inside ONE big GriefPrevention
   admin claim (the plugin subdivides it automatically; a PLOT whose sign
   is outside any claim still rents/buys for naming, but grants no build
   rights until the claim covers it — the boot log warns once per attempt).

### The spawn-landmark inventory (owed with the pass)

The list of which spawn builds become which tier (which are LANDMARKs vs
PLOTs, and the heart pieces) is Mike's in-game call, made standing in the
village — it is deliberately NOT in this packet. Suggested pass order:
heart pieces first, then named landmarks, then minor plots.

## Player proofs owed after activation

- Mike (Bedrock): tap a registered sign → native form pops (not a chest
  inventory); rent an available spot through form + confirm; balance drops
  by the rent; sign face shows his name; My Landmarks door lists it.
- Jester (Java): same through the chest menu; tap ANOTHER player's spot →
  view-only (state + prices, Close only).
- Rylie: redemption path — after any lapse/release, the box beside the
  sign opens for the owner only and never expires.
- /ask retest: the Guide chunk for Landmarks lands with the same-pass
  surfaces pass (parent, at install time).

## Same-pass surfaces (parent, at install time — NOT this packet)

Guide knowledge + intents, website (index.html + ai-knowledge.json),
What's New BOTH surfaces, broadcaster tip check, menu count 85→86 —
per the standing same-pass rule. This packet covers the jar + door only.
