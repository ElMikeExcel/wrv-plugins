# ExcelProtect 0.1.5 — TEST REPORT

Build Nov 6, 2026. Jar `ExcelProtect-0.1.5.jar`, 225,181 B, SHA-256
`c9b5dc1f58cd67de7db60e67e82b8e1cd8cb83b6ed0207ae9597bca0fa3ed5cb`.

## Unit suite — 66 tests, 0 failures

`local-build.sh` runs JUnit first (console standalone 1.11.4); the jar
is only produced on a green suite. **49 carried from 0.1.4 stay
green** (one ledger test updated with an explicit 0.1.5 SUPERSESSION
marker — fresh day files now open with the provenance `file_header`
record, so the emitted envelope is the second line). 17 new:

| Test | Proves |
|---|---|
| deliveryClassification | the delivery truth table: inventory found → INVENTORY (and wins over ground), fresh ground entity only → GROUND, neither → UNKNOWN |
| sigFromItemJsonParsesEnchants | chain signatures rebuild from the streams' own item JSON (id + enchants + custom name; count excluded); junk input → null, never a crash |
| chainMintLinkAndClose | full lifecycle: mint → drop (exact, pre-delivery order tolerated) → ground delivery holds state → pickup → deposit → withdraw → drop → despawn closes; a closed chain never matches again; another player's events never match |
| chainAmbiguousDegradesToProbable | two identical books: first drop probable (oldest chosen), second drop exact, pickup across two ground chains probable, final despawn exact |
| chainExpiryStopsMatching | a chain past `clarity.chain-expiry-seconds` matches nothing and is pruned |
| itemChainQueryAssemblesMendingScenario | the Nov 5 case rebuilt as fixtures: two trades (deliveries folded into their trade lines: GROUND, INVENTORY), the drop, the later pickup — 4 lines in causal order, 2 chains; a different item filter sees none of it |
| itemChainQueryEmptyAndStandaloneDelivery | empty input → total 0, no lines; a delivery whose trade fell outside the window renders as a standalone DELIVERY line |
| inspectPairingResolvesAndFlags | drop+pickup pair renders "picked up"; an unresolved drop renders UNRECOVERED with its age (1h59m at the fixture's now) |
| inspectPairingChainIdDecides | with two identical drops, the pickup's chain id resolves the SECOND drop and flags the first UNRECOVERED — chains decide over signature guessing |
| inspectEconomySummariesIncludeTrades | `villager_trade` and `item_delivery` summarize in the 0.1.4 section style |
| liveInventoryAggregates | LIVE summary aggregates by signature: 3 stacks / 66 items counted, mending books x2, arrows x64, ender chest separate |
| timelineSanitizeKeys | Bedrock dotted names sanitize (`.coolgirlkail` → `_coolgirlkail`); plain names pass through |
| timelineProjectionRoles | single-player record → one actor copy; healing → healer + target copies; mob-attacker damage → victim only; no-player record → none; self-heal → one copy with the stronger role; `fight_summary` never projected |
| timelineLedgerRoundTrip | through the real Ledger: canonical lines land AND the timeline file holds the trade copy with role+stream; a no-player system record is absent from timelines |
| timelineFailureIsolation | timeline root under a regular file: projection latches broken, canonical economy write still lands, nothing throws |
| timelineRebuildRegenerates | `rebuildDay` from stream pairs regenerates the day's file: join + trade + drop + pickup, 4 copies in order |
| claritySettingsDefaultsAndClamps | timelines default ON; chain expiry default 1800 (clamp floor 60 proven); ground radius default 5 (clamp cap 16 proven) |

Determinism: built twice via `local-build.sh`; both jars
byte-identical (SHA-256 above; repro jar kept alongside). The build
also gate-verifies the Layer B fingerprint marker inside the jar
(`excel-build.txt` = `EXC-PROTECT-0.1.5-WRV-20261106-01-c01500`).

## Rig — Paper 26.3 + REAL EliteMobs 10.9.12 + REAL Magic 11.2.5

Sandbox pattern as prior Protect rigs (fake NMS player `RigKailey`;
shipped 0.1.5 config). Evidence in `rig/evidence/` (boot-0.1.5.log,
economy/items/players day files, `players/RigKailey-2026-11-05.jsonl`).
The scenario IS the Nov 5 investigation: trade 1 with a full inventory
(the book entity on the ground), trade 2 into an empty inventory, the
ground book collected minutes later, then the console family —
`item`, `inspect player`, `status`, `timelines rebuild <today>`.

Boot lines verbatim:

```
[ExcelProtect] clarity: ON delivery=next-tick chains=expiry 1800s ground-radius=5
[ExcelProtect] player timelines: ON (projection of the streams; streams stay canonical)
[ExcelProtect] READY version=0.1.5 dir=plugins/ExcelProtect gp=off vault=off mcmmo=off retention-days=30
[ExcelProtect] timelines: files=1 projected=10 dropped=0 failures=0   (status, mid-run)
```

**SEVERE 0** in the whole boot — none attributable to Protect, none
at all. Every driver analysis check PASS:

| Branch | Evidence |
|---|---|
| Two trades mint chains | economy stream: `villager_trade` ×2 carrying `ch-20261105-0001` / `ch-20261105-0002` |
| Delivery stamps | `item_delivery` for chain 0001 = `ground`; for chain 0002 = `inventory` — the two Nov 5 outcomes, distinguished |
| Drop links to the trade | items stream `item_drop` carries chain 0001, `link=exact` |
| Later pickup closes the loop | `item_pickup` carries chain 0001, `link=exact`; the rig's own inventory count ends at 2 mending books |
| Timeline holds the story | the player's timeline file lists villager_trade → item_drop → villager_trade → item_pickup in timestamp order, every copy with `role` + `stream` |
| Item query tells it in one read | console: `RigKailey — chain of custody for mending, last 24h: 4 event(s), 2 chain(s) (source: timeline+streams)` then TRADE … paid minecraft:emerald#14 -> delivered GROUND / DROP [exact] / TRADE -> delivered INVENTORY / PICKUP -> chain in inventory |
| Empty query is plain | `no records for RigKailey / diamond in the last 24h` |
| Inspect LIVE + pairing | `[LIVE] RigKailey is ONLINE — inventory right now: …` with the last-snapshot contrast line; the [items] drop renders `-> picked up …` (in the pre-pickup run of this rig it correctly rendered `-> UNRECOVERED (1m ago)`) |
| Rebuild fidelity | `timelines rebuild 2026-11-05` ran from the console; the evidence timeline file above is the REGENERATED one and still holds the full chain in order |
| Query path is player-first | the item query read the day's records from the player's timeline file (16 lines) — the legacy path scans the four chain streams' full day files for every player (8 lines in this toy corpus; thousands per day live) |

## Volume note

Timelines duplicate (a subset of) the day's records: worst case the
data folder's daily growth roughly doubles for player-named records,
inside the same gzip + 30-day retention envelope the storage evidence
already measured (Protect's whole live footprint was 12.47 MB / 4
days against CoreProtect's 1.54 GB database). The projection is one
toggle (`log.player-timelines`) if volume ever argues otherwise; the
streams never depend on it.

## Honest limits

- **The trade is event-driven, not GUI-driven.** A fake player has no
  client to work a real merchant GUI, so the rig fires
  `PlayerPurchaseEvent` — the exact event Protect consumes — with the
  result entity placed on the ground (trade 1) or into the inventory
  the same tick (trade 2, what the GUI does). Everything downstream
  (mint, next-tick delivery resolution, stamping, records, timelines,
  queries) is the production path.
- **The pickup touch is driven through the NMS entry.** The
  collision-driven pickup never fires for a clientless player in this
  sandbox (two scripted walk-overs produced no touch — the fake
  player floats at teleport height, clear of the resting item's
  pickup box). The rig therefore calls `ItemEntity.playerTouch` —
  the exact method the collision tick calls — and the Bukkit
  `PlayerPickupItemEvent`, the inventory add and Protect's record all
  ran their real code. A live player's pickup is owed as a
  post-install proof (INSTALL-PACKET).
- The rig's EliteMobs/Magic are present and booted clean, but no
  elite or cast participates in the clarity scenario by design; the
  combat families are covered by the 0.1.3/0.1.4 rigs and the unit
  suite.
- `delivered=unknown` is exercised by the unit truth table, not the
  rig (the rig's two landings are the two provable ones — by design:
  unknown is what the code says when it cannot prove, and the rig
  proves the provable paths end-to-end instead).
