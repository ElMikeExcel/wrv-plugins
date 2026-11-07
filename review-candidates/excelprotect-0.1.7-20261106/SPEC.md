# ExcelProtect 0.1.5 — SPEC: "Clarity" (delivery outcomes, item chains,
# inspect pairing, live inventory, player-first timelines)

**Commission:** Mike, Nov 5, 2026, 11:05 PM CST — the clarity pass is
pulled AHEAD of the Elites build after the Nov 5 mending-book
investigation. **Scope addition:** Mike, Nov 5, 2026, ~11:10 PM CST —
player-first timelines (§7). Base: the live ExcelProtect 0.1.4 tree
(`~/workspace/protect-where-build/`; live jar 184,594 B, SHA-256
`2b471e7b…`). 0.1.5 is cumulative: 0.1.2 + 0.1.3 + 0.1.4 whole, plus the
six clarity features below. Same house discipline: daily JSONL per
stream, one async writer family, 30-day retention + gzip, MONITOR
listeners, no IO on the event thread.

**Staged-line renumbering (this build):** Clarity takes 0.1.5. The
coverage map's staged plan shifts by one: Elites & entities →
**0.1.6**, Machines & making → **0.1.7**, server self-knowledge →
**0.1.8**, casts & economy causes → **0.1.9**. The coverage map is
updated to match.

## §0 Why — the Nov 5 mending-book investigation

Kailey bought two mending books from a villager (20:43 and 20:47 CST).
One trade's book ended up on the ground; she picked it up at 20:52:39.
Every fact was in Protect's records — two `villager_trade` economy
records, an `item_drop`, an `item_pickup` — yet the investigator drew
the wrong conclusion **twice**, because:

1. the trade record did not say where the book **landed** (inventory
   and ground look identical in the economy stream);
2. the drop and the pickup sat in a **separate stream, unlinked** —
   which drop belonged to which trade was inference, not data;
3. `inspect player` listed item events **without pairing** a drop to
   its pickup, so "lost vs recovered" needed manual stitching;
4. there was **no way to count a player's current inventory while
   they were online** (snapshots are join/leave/world-change only),
   so "how many does she have right now" was unanswerable mid-session;
5. the records are organized **stream-first**, but investigations are
   **player-first** ("where are Kailey's books", "what did Jester
   do") — answering meant scanning fifteen files and merging by hand.

The recording layer worked. The **clarity layer** is what this build
adds. Every change is ADDITIVE: no existing stream, record type, or
field changes shape; new fields and one new record type ride along.

## §1 Delivery outcome — `item_delivery` (economy stream)

When an acquisition is recorded — a `villager_trade`
(`PlayerPurchaseEvent`, Adapters) or an API `shop_purchase`
(`EconomyEvent`, when player + item are extractable from the emitter's
fields and the player is online) — Protect schedules a **next-tick**
check and emits a follow-up record in the economy stream:

```
{"type":"item_delivery","player":…,"player_uuid":…,
 "chain":"ch-20261105-0007","source":"villager_trade"|"shop_purchase",
 "item":{…fingerprint…},"delivered":"inventory"|"ground"|"unknown"}
```

Classification (pure, unit-tested — `Chains.classify`):
- **inventory** — the acquired signature (id + enchants + custom name,
  the ItemFact fingerprint minus count) is present in the player's
  inventory with total count ≥ the acquired count. Inventory wins
  over ground when both match: the delivery provably arrived.
- **ground** — not in inventory, and a **fresh** item entity
  (ticksLived ≤ 100, i.e. spawned within ~5 s) with the same signature
  exists within `clarity.ground-radius` blocks (default 5) of the
  player. The freshness bound is what keeps a pre-existing identical
  item lying nearby from being misread as this delivery.
- **unknown** — neither provable (player logged off before the check,
  signature extraction failed, inventory changed within the tick).
  Unknown is a stated answer, never a guess.

The ledger is append-only, so the outcome is a **linked follow-up
record**, never an edit of the trade record; the link is the chain id
(§2), minted on the trade record itself so either record finds the
other. Acquisitions whose toggles are off record nothing, exactly as
before — delivery follows the acquisition record's own gate.

## §2 Chain linking — `Chains`

An in-memory tracker (no new stream; chain state is derivable from
the records it stamps). **Mint:** every recorded acquisition mints
`ch-<chicago-day>-<seq>` (seq zero-padded, per-day counter) and the
acquisition record carries `"chain"`. **Signature:** id + enchants +
custom name (count excluded — chains track an item's flow, counts are
per-event facts).

**Stamping:** subsequent records for the same player + signature
carry `"chain"` and `"link"`:
- items stream: `item_drop`, `item_pickup`, `item_despawn`,
  `item_destroy` (ItemEvents);
- containers stream: the matching fact inside a transaction's
  `deposits`/`withdrawals` arrays gains a `"chain"` field
  (Containers).

**Matching is state-filtered and conservative** — a wrong `exact` is
worse than a `probable`:
- drop candidates: chains for that player+signature in state OPEN
  (delivery pending) or HELD (believed in inventory);
- pickup candidates: chains in state GROUND (or OPEN — a trade whose
  delivery check hasn't landed yet);
- despawn/destroy candidates: GROUND chains owned by the item's
  recorded owner (falling back to the dropper-of-record);
- container deposit candidates: HELD/OPEN chains of the depositor;
  withdrawal candidates: chains in state STORED for that container
  kind+position… (containers carry no persistent identity across
  opens; withdrawal matching uses the depositor's STORED chains —
  see lifecycle — keyed by player+signature only).
- exactly one candidate → `link":"exact"`; more than one →
  the OLDEST candidate with `link":"probable"`; none → no chain
  fields (the record is exactly its 0.1.4 shape).

**Lifecycle:** mint → OPEN → (delivery) HELD (inventory) or GROUND;
drop → GROUND; pickup → HELD; container deposit → STORED;
withdrawal → HELD; despawn/destroy → CLOSED (terminal). Chains also
expire silently after `clarity.chain-expiry-seconds` (default 1800 —
the Nov 5 pickup came 9 minutes after the drop; 30 minutes covers a
play session's errands without letting stale chains swallow later
unrelated events). Expiry is lazy (checked on access); an expired
chain simply stops matching. Cross-player pickups match the PICKER's
chains first, then the item owner's — a stolen drop still resolves
against the victim's chain when the thief has none.

**Honest limit:** vanilla items are not unique instances. Two
identical books bought minutes apart are two chains over one
signature; the state filter is what keeps the common case exact
(the ground chain is the only pickup candidate). When signatures and
states truly collide, the record says `probable` and the item query
(§3) prints the ambiguity rather than hiding it.

## §3 Item chain query — `/excelprotect item <player> <item> [hours]`

Assembles a chain of custody from the economy, items, containers and
deaths streams (or the player's timeline, §7 — same shapes), oldest
first, one line per event, plain language:

```
20:43:32 TRADE villager @ world -1266,68,-247 paid 14x emerald -> delivered GROUND [ch-20261105-0007]
20:43:32 DROP enchanted_book#1 [mending] @ world -1266,69,-247
20:47:48 TRADE villager @ world -1267,68,-248 paid 14x emerald -> delivered INVENTORY [ch-20261105-0008]
20:52:39 PICKUP enchanted_book#1 [mending] by .coolgirlkail @ world -1269,68,-248 -> chain in inventory
```

Event vocabulary: TRADE (villager / shop), DELIVERY (only when its
trade falls outside the window), DROP, PICKUP, DESPAWN / DESTROYED
("-> chain closed: lost"), DEPOSIT / WITHDRAW (containers), CRAFT,
DESTROYED (deaths stream), RECOVERED (Lost & Found). The item
argument matches by id substring or full signature, case-insensitive,
as the other queries match. Window: default **24 h**, clamp
0.1–720 (DamageReport's parser, reused). No matching records → the
command says so plainly ("no records for X / item in the last 24h").
Header names the event count and the source (timeline vs streams).
Self-logged (`item_query`), any sender. Pure assembly
(`ItemChainReport`) over scanned lines — unit-tested, rig-proven.

## §4 Inspect pairing — the [items] section resolves drops

In `inspect player`'s per-stream sections (0.1.4), the items section
now **pairs each drop with its resolution** before rendering:
drops and resolutions (pickups by the dropper or of the dropper's
items, despawns, destroys) are matched per signature in timestamp
order (chain ids, when present, take precedence over signature
matching). The drop's line gains its outcome:

```
21:14:05 drop minecraft:enchanted_book#1 [mending] by Kai @ … -> picked up 21:23:41 by Kai
21:31:02 drop minecraft:diamond#3 by Kai @ … -> UNRECOVERED (2h14m ago)
```

A drop with no resolution in the scanned records is flagged
**UNRECOVERED** with its age (relative to the query's `now`, passed
in — the pairing is pure and testable). Resolution lines keep their
own entries (they are separate events); pairing annotates, never
hides. `summarize()` (single-line, inspect-at and the merged build
path) is unchanged — pairing is a sections feature, where the whole
player picture is in scope.

Companion clarity fix in the same pass: `villager_trade`,
`shop_purchase`, `item_delivery` and `jobs_payout` economy records
become inspect-reportable (0.1.4's economy section knew only money
and service charges — a trade was invisible to inspect even though
the stream held it).

## §5 Live inventory read — inspect on an ONLINE player

When `inspect player <name>` targets a player who is **online**, the
answer gains a LIVE section, read in real time on the main thread
before the async scan:

```
[LIVE] .coolgirlkail is ONLINE — inventory right now: 33 stacks (41 items), ender chest 12 stacks
  enchanted_book x2 [minecraft:mending]
  …
  (last snapshot: 2026-11-05 19:33:00, join — LIVE supersedes it)
```

Contents render in the snapshot fingerprint style, aggregated by
signature (id + count, plus name/enchant markers), **clearly labeled
LIVE**, with the latest snapshot's timestamp + reason shown for
contrast (found in the same scan). Aggregation is a pure function
(`LiveInventory.summarize`) over ItemFacts — unit-tested. **Offline**
players keep the snapshot-based behavior, with one added line:
"offline — counts are as of the last snapshot (<ts>, <reason>)"
(or "no snapshot recorded"). Payloads are never read or printed —
fingerprints only, as everywhere in inspect.

## §6 Config + lines

- `log.player-timelines` — new toggle, default **true** (§7). OFF =
  no projection is written; queries read the streams.
- `clarity.chain-expiry-seconds` — default 1800, clamp 60–86400.
- `clarity.ground-radius` — default 5, clamp 1–16 (delivery check).
- The shipped config documents all three; **the live config is NOT
  replaced at install** — absent keys default to ON/1800/5, the
  desired state.
- Awake lines:
  `[ExcelProtect] clarity: ON delivery=next-tick chains=expiry 1800s ground-radius=5`
  `[ExcelProtect] player timelines: ON (projection of the streams; streams stay canonical)`
  (OFF variant names the key.) READY/status report `version=0.1.5`;
  status gains the timeline failure count when nonzero.

## §7 Player-first timelines — a projection, not a restructure

**Why streams stay canonical:** the daily per-stream files are the
source of truth and stay exactly as they are. Three record classes
make per-player-only storage wrong: **two-player events** (a dealt
incident, a heal — one fact, two subjects; duplicating it into a
per-player store as the ONLY copy forks the truth), **location-first
queries** (`inspect at`, rollback, radius export — keyed by place and
stream, not person), and **non-player events** (natural block change,
mob deaths, system markers — no player key exists). Retention,
gzip, aggregates and every existing query already speak streams.

**The projection:** at write time, every canonical record that names
a player is ALSO appended to `players/<player-key>-<day>.jsonl`
under the Protect data folder, with two added fields: `"role"` and
`"stream"` (the origin stream, so consumers can re-section). A
two-player record lands in BOTH players' files, each copy carrying
that player's role. Records with no player are never projected.

- **Player key:** the player's name as the streams carry it,
  sanitized for the filesystem — every character outside
  `[A-Za-z0-9_-]` becomes `_` (Bedrock's `.coolgirlkail` →
  `_coolgirlkail`; the leading underscore also keeps the file from
  being dot-hidden, and keeps a Java `coolgirlkail` distinct). When a
  record names only a UUID, the key is the last known name for that
  UUID, else the UUID itself.
- **Roles** (the streams' own vocabulary): `player` → **actor**;
  `attacker` → **actor**; `healer` → **healer**; `target` →
  **target**; `victim` → **victim**; `owner` (items/DeathRecord) →
  **owner**. One copy per player per record: a self-heal (healer ==
  target) yields ONE copy with the stronger role (priority actor >
  healer > target > victim > owner).
- **Who counts as a player:** identities resolve against a
  known-player registry — seeded at enable from the server's
  offline-player list, self-learning from every record's primary
  `player`/`player_uuid` pair, extended on join. A counterparty
  field whose UUID is not a known player (a zombie victim in a dealt
  incident, a mob attacker) is never projected — mob timelines would
  be junk files. Raw `combat` lines name counterparties by entity
  key, not player identity; they project via their primary fields
  only, and their incident families (damage/dealt/healing) carry the
  full two-player projection. `fight_summary` is not projected
  per-player (its rows' actors are covered by combat/incident
  records).
- **Failure isolation:** projection writes go through their own
  async writers, enqueued AFTER the canonical write, inside a
  catch-everything guard. A projection failure can never lose, delay
  or block a canonical record: the first failure latches the
  projector "broken" (further copies are counted, not attempted),
  writes ONE marker to the system stream, and surfaces in
  `/excelprotect status`. The canonical path does not know the
  projector exists beyond a single guarded call.
- **Retention:** timeline files ride the same housekeeping as
  streams (gzip after their day, deleted past retention) — minus
  aggregation, which stays a per-stream concept.
- **Queries read player-first:** `/excelprotect item` and
  `inspect player` assemble per DAY: a day with a timeline file for
  the player is read from the timeline (one file instead of
  fifteen); a day without one (pre-0.1.5 history, toggle-off gaps)
  is read from the streams. No day is ever read from both, so the
  merge cannot double-count. The item query's header names its
  source per the days actually used.
- **Rebuild:** timelines are derived and rebuildable —
  `/excelprotect timelines rebuild [yyyy-MM-dd]` (default: yesterday,
  Chicago) regenerates one day's timeline files from that day's
  canonical stream files: identities are learned in a first pass,
  records projected in a second, existing files for the day replaced.
  Self-logged (`timelines_rebuild`), any sender, runs async.

## §8 What 0.1.5 deliberately is not

- **No storage restructure** — §7: streams stay canonical; timelines
  are a derived, rebuildable projection.
- **No per-instance item identity** — vanilla stacks carry no unique
  id (the 0.1.0 `rid` stamp covers fingerprint-class items at craft
  time only); chains are signature-level with stated confidence,
  never false precision.
- **No movement of the staged line's content** — Elites (0.1.6),
  Machines (0.1.7), self-knowledge (0.1.8) and casts/causes (0.1.9)
  keep their scopes; clarity only renumbers them.
- **No change to Lost & Found, rollback, or restore paths** — the
  deaths stream gains readers (§3), not writers.
