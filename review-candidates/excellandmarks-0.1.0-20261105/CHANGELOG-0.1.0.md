# ExcelLandmarks 0.1.0 — spawn landmarks: sign-tap rent/buy (Nov 5, 2026)

First release. Mike's GO Nov 5, 2026 ("Yes to landmark") on the design in
`~/workspace/village-identity/REMAP-DIRECTION-20261103.md`, with the
rental-lapse policy settled Nov 4, 2026 and two menu directives added by
Mike the same morning. There is no previous version; everything below is
new.

## What it does

- **Registration is a sign.** An admin places any sign, writes
  `[Landmark]` + the spot name + `TYPE:TIER` on it, and the spot
  registers. Nobody hand-builds claims or registry rows. Prices resolve
  at registration from config tier defaults (overridable per spot later
  via admin command, never by re-placing the sign).
- **Prices are config, never hardcoded** (config-first fleet pattern).
  Defaults (Mike's adopted sketch): rent SPOT $500/mo, LANDMARK
  $1,500/mo, HEART $3,000/mo; buy $10,000 / $25,000 / $50,000. "Month" is
  the config rent period (30 days default).
- **Two product types.** LANDMARKS sell naming rights only (sign +
  registry). PLOTS/AREAS sell naming + build rights: at purchase the
  plugin auto-creates a GriefPrevention subdivision for the plot.
- **The sign tap IS the menu** (Mike's directives, SPEC §6). Tapping a
  registered sign pops the spot menu through the server menu system —
  one core model, native Floodgate form on Bedrock, chest inventory on
  Java. No compass or command needed. The menu lists ONLY the options
  available for that spot + viewer:
  - available → Rent / Buy
  - own rental → Renew / Buy Outright / Release (grace: Renew first,
    labelled with the deadline)
  - own owned → Release
  - someone else's → info view only; Close is the ONLY button
  Every charge (rent, renew, buy, buy-out) sits behind a confirm screen
  naming the exact amount; the balance is untouched until Confirm.
- **GP rights are TRUST, not title** (rig-proven): GriefPrevention
  forbids subdivision title transfer (`changeClaimOwner` throws
  NoTransferException), so the buyer gets Build permission + manager on
  their subdivision; the subdivision is deleted wholesale when a rental
  finalizes. A LANDMARK (naming-only) creates no subdivision.
- **Rental clock:** 30-day period, 7-day grace. Renewals stack from
  paid-through when early, from now when in grace. In grace the spot
  holds (sign shows "Grace til …") and nothing is packed.
- **Lapse finalize** (after grace, settled Nov 4 policy): (a) container
  contents + loose items packed into a redemption box beside the sign;
  (b) placed-block structures forfeit and the plot clears — exactly the
  blocks ExcelProtect attribution credits to the renter (pre-tracking
  placements are indistinguishable; accepted limit), whose MATERIALS
  are tallied and returned as item stacks in the same box; (c) the GP
  subdivision is deleted. **The redemption box NEVER expires** —
  deliberately unlike ExcelProtect Lost & Found's 14-day window. If the
  physical box cannot be placed, the redemption is held virtually and
  the owner claims it via the menu/command.
- **Compass door:** "My Landmarks" is a true ExcelMenu registry entry
  (registry 85 → 86 at install) listing the player's own spots.
- **Console admin for every state-changing op** (fleet standard):
  `/landmarks admin status|inspect|register|remove|release|setprice|setpaid|sweep|reload`.
- **Fail-soft hooks:** with GriefPrevention and/or ExcelProtect absent,
  the plugin runs naming-only — purchases still work, subdivisions and
  material tally simply report OFF (rig Boot B). Vault is the economy
  rail for every charge.

## Tests

101,018 checks, 0 failures (TEST-REPORT.md): rent clock/grace to the
millisecond + a 20,000-case randomized sweep; Protect attribution
parser/tally incl. file-level reads and missing-dir fail-soft; tap
routing incl. unregistered signs and the 299/300/301 ms debounce edges;
the menu model incl. view-only pinning on someone else's spot; Mike's
adopted prices pinned as the config defaults. Rig on Paper 26.3-235
(NMS fake players firing real events): Boot A (Vault + EssentialsX +
GP 16.18.7 + Protect 0.1.1) 28/28 PASS; Boot B (GP + Protect absent)
9/9 PASS fail-soft; SEVERE 0 on both boots.

## Not yet live in play

Installed Nov 5, 2026 — but the plugin is inert at spots=0 until
ACTIVATION: the one-time in-game admin sign-placement pass over the
spawn build + the spawn-landmark tier inventory (packet carries the
sign how-to + the one-big-GP-claim-over-spawn precondition). Player
proofs owed after activation: the Bedrock form send path (Mike,
sign-tap + rent), the Java chest menu + view-only (Jester), and a
redemption-box owner-only open.
