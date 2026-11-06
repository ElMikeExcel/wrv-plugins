# ExcelProtect 0.1.5 — changelog

Released as a build Nov 6, 2026 (install gated; see INSTALL-PACKET.md).
"Clarity": the pass Mike pulled ahead of the Elites build on Nov 5,
2026, after the mending-book investigation — Protect's records were
complete but unreadable as a story, and the investigator drew the
wrong conclusion twice from complete data. 0.1.5 makes acquisitions
say where the item went, links the records of one item's life into a
chain, answers "where are Kailey's books" in one command, pairs every
drop with its resolution in inspect, reads a live player's inventory
in real time, and keeps a per-player timeline file so investigations
read player-first. All changes are ADDITIVE — no existing stream
format changed. The staged line renumbers: Elites → 0.1.6, Machines
& making → 0.1.7, server self-knowledge → 0.1.8, casts & economy
causes → 0.1.9.

## Added

- **Delivery outcomes** (`item_delivery` records on the economy
  stream). One tick after a villager trade — and after a shop purchase
  whose event fields resolve — Protect checks where the item landed
  and records `delivered: inventory` (the full item signature is in
  the buyer's inventory), `delivered: ground` (a fresh matching item
  entity, ≤5 s old, lies within `clarity.ground-radius` blocks), or
  `delivered: unknown`. Unknown is a real answer, never a guess.
- **Chain linking.** Every acquisition mints a chain id
  (`ch-<yyyymmdd>-<seq>`). Drops, pickups, despawns, destroys and
  container deposits/withdrawals for the same player + item signature
  carry the chain id with `link=exact` (single candidate) or
  `link=probable` (several — the oldest is chosen and labeled). The
  matcher is state-filtered (a pickup can only continue a chain whose
  item is on the ground) and deliberately conservative: a wrong exact
  is worse than a probable. Chains close on despawn/destroy and lapse
  after `clarity.chain-expiry-seconds` (default 1800) without an event.
- **`/excelprotect item <player> <item> [hours]`** — the
  chain-of-custody query. One plain-language line per event, oldest
  first, across the economy, items, containers and deaths streams:
  TRADE … paid … -> delivered GROUND, DROP, PICKUP -> chain in
  inventory, DEPOSIT / WITHDRAW, DESPAWN / DESTROYED -> chain closed:
  lost, RECOVERED. Items match by id substring or signature detail
  (e.g. `mending`). No records says so plainly. Default window 24 h.
- **Per-player timeline files** (`players/<player>-<day>.jsonl` in
  the data folder). Every record that names a known player is ALSO
  appended to that player's daily timeline with `role` (actor /
  healer / target / victim / owner) and `stream` fields; two-player
  records land in both files. The per-stream files stay the canonical
  truth — timelines are a derived, rebuildable projection
  (`/excelprotect timelines rebuild [yyyy-MM-dd]`), failure-isolated
  from the canonical writes, toggle `log.player-timelines` (default
  ON). `item` and `inspect player` read player-first where a timeline
  file exists, streams otherwise.
- **LIVE inventory read in `inspect player`.** For an ONLINE player,
  inspect opens with a [LIVE] section: the real inventory + ender
  chest right now (stacks, item totals, per-signature counts in the
  snapshot fingerprint style), with the latest snapshot's timestamp
  shown for contrast — LIVE supersedes it. Offline players keep the
  snapshot behavior plus an explicit as-of line.
- **Config:** `log.player-timelines` (default ON), `clarity:` section
  (`chain-expiry-seconds` 1800, clamp 60–86400; `ground-radius` 5,
  clamp 1–16). New awake lines report both features' state.
- **Provenance file header:** fresh day files now open with a
  `file_header` JSON record (generator + version + brand) — closes
  the null-header gap from the fingerprint bake list. The jar also
  carries the Layer B build marker `excel-build.txt`, injected at
  build time and gate-verified (`EXC-PROTECT-0.1.5-WRV-20261106-01-c01500`);
  `plugin.yml` gains the `website:` line.

## Changed

- **`inspect player` [items] pairs drops with resolutions.** Each
  drop line now ends with its outcome — `-> picked up HH:MM:SS by X`,
  `-> despawned …`, `-> destroyed (reason) …` — or is flagged
  `-> UNRECOVERED (age ago)` while it has none. Pairing prefers chain
  ids, then signature + ownership (a bystander picking up your drop
  still resolves it). All 0.1.4 sections, the where/playtime output
  and the merged summary path are unchanged.
- **`inspect` economy summaries** now render `villager_trade`,
  `shop_purchase`, `item_delivery` and `jobs_payout` records (0.1.4
  showed them as raw type names in section listings).
- **Status** reports version 0.1.5 and gains a timelines health line
  (files / projected / dropped / failures) when the projection is on.

## By design (read before calling them gaps)

- Delivery resolves on the NEXT tick; a buyer who logs off in between
  reads `unknown`, as does any landing neither proof reaches.
- `probable` links appear exactly where two live chains share a
  signature — two identical plain books are genuinely ambiguous, and
  the record says so instead of picking silently.
- Shop purchases chain only when the emitter's event carries the
  player + item in a resolvable shape and the buyer is online;
  anything else records exactly as 0.1.4 did.
- `fight_summary` records are not projected per-player (they name a
  whole fight, not a person); their combat incident families are.
