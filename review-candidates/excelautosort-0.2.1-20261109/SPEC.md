# SPEC — ExcelAutoSort 0.1.0

**Status:** Design of record, ready to build from. Written Nov 6, 2026 by
Iris (desk work only — no code yet). **Amended Nov 6, 2026, 11:32 PM CST:**
the three §14 open questions are DECIDED by Mike's ruling (see the
Decisions of record, item 4); §14 now records the decisions.
**Plugin:** ExcelAutoSort 0.1.0 — a brand-new house plugin.
**Package:** `com.winkreyes.excelautosort` · **Authors:** [Iris, Mike]
**Supersedes:** ExcelAutoSortBridge 0.1.0 (ours) **and** AutoSort 0.1.17
(third-party, by nothub) — both retire in the swap window (§10).

## Decisions of record (Mike, Nov 6, 2026, 11:02 PM CST)

1. **Not a bridge, not an edit.** A new house auto-sort plugin of our own.
   ExcelAutoSortBridge is retired, not extended; the third-party AutoSort
   engine is replaced by our own implementation, not wrapped.
2. **Filter-delete, BOTH modes, chosen per chest** — whitelist (keep only
   what's allowed; delete the rest) and blacklist (delete listed junk;
   keep everything else).
3. **Contents-seeded selector.** The filter picker is seeded from what is
   already in the chest — tap items present to put them on/off the list —
   not a master item list. Adding an item that is not in the chest must
   still be possible (hand / own-inventory paths, §7).

Iris's safety defaults from the Nov 6, 2026 pitch stand unoverridden and
are core features of this spec, not options: only explicitly marked
chests ever delete; marking requires an itemized confirm; every deletion
is logged to ExcelProtect; deleted items sit in a recovery buffer for a
configurable window (default 7 days / 168h per the Nov 6 ruling below)
before permanent deletion.

4. **The three §14 open questions — DECIDED (Mike, Nov 6, 2026,
   11:32 PM CST, his words in chat: "Go with all three
   recommendations"):** (1) recovery surface = the plugin-local buffer
   (§6.2), not Protect's Lost & Found; (2) buffer window default =
   **7 days / 168h**; (3) hopper filtering default = **ON** (§5.7).
   §14 records each decision in full.
5. **Targeted access only + compass entry (Mike, Nov 6, 2026,
   11:51 PM CST):** filter settings are reached only by standing at
   the chest — `/autosort chest` resolves the chest in the player's
   crosshair (no remote browsing or chest lists anywhere), and
   hitting/tapping a chest while holding a compass opens the same
   menu. Config posture reaffirmed the same ruling: a clean,
   sectioned, commented `config.yml` carrying every knob and every
   player-facing sentence. Full text in §7.1 and §9.
6. **Configure rights are a hard GP gate (Mike's explicit rule,
   Nov 6, 2026, 11:52 PM CST):** only a player with configure rights
   over a chest may touch its filter settings — claim owner,
   PermissionTrust, the recorded owner in unclaimed land, a
   Landmark-plot renter in their plot, staff in admin claims.
   **ContainerTrust / AccessTrust alone is never enough:** being
   allowed to open a chest does not grant the right to change its
   settings. Refusal is the plain no-permission line — no menu, no
   read-only view, no leak of the chest's settings. Full text in §6.4.

---

## 1. The incumbent — what runs on WRV today (inventory, sourced)

Auto-sort on WRV today is **two** plugins, and only one of them sorts:

| Piece | What it is | Source |
|---|---|---|
| **AutoSort 0.1.17** (nothub, MIT; installed Oct 24, 2026) | The actual sorting engine. One class, no config file. | Upstream source cloned at `menu-toggle-fix-20261103/work/autosort-upstream/` (tag `v0.1.17`, commit `5349a479…`); live jar `autosort-0.1.17+26.3-paper.jar`, 6,677 B, SHA-256 `448a5fff…` |
| **ExcelAutoSortBridge 0.1.0** (ours; installed Nov 3, 2026) | A read-only adapter that registers `%autosort_status%` (ON/OFF) for the menu, reading AutoSort's live in-memory `actives` set reflectively, state-file fallback. It sorts nothing and writes nothing. | `menu-toggle-fix-20261103/SOURCE.md`, `INSTALLED.txt`; record package `records-catchup-20261103/packages/excelautosortbridge-0.1.0-20261103/` |

**AutoSort 0.1.17's full behavior contract** (from its source — this is
the behavior players actually use):

- `/autosort` toggles the player (perm `autosort.toggle`, **default
  true**; alias **`/sort`**). State is set membership: in the set = ON,
  absent = **OFF (the default for everyone)**. Persisted synchronously
  on every toggle to `plugins/autosort/active.txt` (one UUID per line).
- Sorts on **InventoryOpen and InventoryClose**.
- Inventory types: **CHEST, BARREL, ENDER_CHEST, SHULKER_BOX**.
- Only **real world storage**: block containers, double chests, storage
  minecarts, chest boats (holder check); an ender-chest inventory counts
  only if it is the viewing player's own ender chest. Plugin GUIs that
  reuse those inventory types are never touched.
- **Skips the sort if any other viewer has the inventory open.**
- Sort order: material registry order → material key → item-meta string;
  stacks are **merged/compressed** on re-add. It tidies the opened
  inventory in place — it never moves items between containers.
- Toggle feedback: "Sorting enabled for: Barrel, Chest, Ender Chest,
  Shulker Box" (green) / "Sorting disabled" (red). No other commands, no
  config, no per-chest anything, no API, no placeholder of its own.

**Menu surface (ExcelMenu 2.0 registry, live):**

- Entry `autosort` (Settings group; **Java order 10, Bedrock order
  200**) — state-driven per the Nov 3 menu-toggle standard:
  `%autosort_status%` drives ON (LIME_DYE, "Auto-sort my chests: ON") /
  OFF (GRAY_DYE) / default ("unavailable", fail-closed) variants; action
  `[player] autosort` + `[refresh]` (tap flips and re-renders in place).
  `requires-plugin: AutoSort`, `requires-permission: autosort.toggle`.
  (`menu-toggle-fix-20261103/entries/autosort.yml`, 1,994 B.)
- Entry `autojoin` ("Auto-open on join") sits **last** in Settings
  (**Java order 70, Bedrock order 250**) per Mike's Nov 3, 2026 ruling
  (kept, moved to the bottom — superseding the Oct 18 removal note).
  Its behavior belongs to **ExcelMenuJoin** (`/menujoin`,
  `%menujoin_status%`), not to AutoSort or the bridge; the Nov 3 packet
  merely reordered it in the same window. The swap must not disturb it
  (§2 P11, §10).

**Publishing note:** the Nov 5, 2026 plugin audit lists
ExcelAutoSortBridge as NOT FOR SALE (a ~5 KB adapter on a third-party
private contract — too fragile to charge for). ExcelAutoSort removes
that fragility: the whole feature becomes our own code with our own
state (see §13).

---

## 2. Parity set — everything players use today, carried over

Each item is a hard acceptance requirement (§11 tests them).

| # | Parity item | Source |
|---|---|---|
| P1 | Per-player sort toggle, **default OFF**; toggling is permission-gated with the permission **defaulting to true** (everyone may toggle) | Upstream `Plugin.java` (`actives` set), upstream `plugin.yml` (`autosort.toggle`, default true) |
| P2 | Command **`/autosort`** toggles, with alias **`/sort`**, and chat feedback naming the state ("Sorting enabled for: …" / "Sorting disabled", wording in config, §9) | Upstream `plugin.yml`; upstream toggle executor |
| P3 | Sort fires on **inventory open and on inventory close** | Upstream `onInventoryOpen` / `onInventoryClose` |
| P4 | Sortable types: **chest, barrel, ender chest, shulker box** | Upstream `inventories` set |
| P5 | Only real world storage is sorted — block containers, double chests, storage minecarts, chest boats; an ender chest sorts only for its owner; plugin GUIs are never sorted | Upstream `isWorldStorage` |
| P6 | **No sort while another viewer has the inventory open** | Upstream `hasViewers` check |
| P7 | Ordering: material registry order → material key → item-meta string; **stacks merged/compressed** by the sort | Upstream comparator + re-add |
| P8 | Toggle state **persists across restarts** | Upstream `active.txt` load/save |
| P9 | Menu toggle shows the player's **CURRENT state** (`%autosort_status%` → ON/OFF; unresolvable → "unavailable" fail-closed variant) | Bridge `AutoSortExpansion`; converted `entries/autosort.yml` |
| P10 | Menu placement: `autosort` entry in Settings at **Java order 10 / Bedrock order 200**, state-driven labels, tap = toggle + `[refresh]` | `entries/autosort.yml` (live) |
| P11 | **Auto-open on join stays last** in Settings (Java 70 / Bedrock 250). Its behavior is ExcelMenuJoin's and is not reimplemented; the parity obligation is that the swap window leaves the entry and its placement byte-identical | Mike's ruling Nov 3, 2026; `entries/autojoin.yml` (live) |

**Parity count: 11.**

### Deliberate drops (named, with reasons)

- **D1 — ExcelAutoSortBridge itself.** An adapter that exists only to
  read a third-party plugin's private state. ExcelAutoSort owns its
  state natively and registers the same placeholder itself (§8); the
  bridge has nothing left to bridge. Retired at swap.
- **D2 — The nothub AutoSort dependency.** The engine is reimplemented
  in-house to the contract in §1 (upstream is MIT and is neither
  modified nor redistributed by this project — same posture as the
  bridge packet). Its jar is removed at swap; our sort behavior is
  specified by §4, not by upstream's code.
- **D3 — The `plugins/autosort/active.txt` file format.** Imported once
  at first enable (§10) so nobody's ON/OFF state resets, then retired;
  ExcelAutoSort keeps its own `data/players.yml`.
- **D4 — Filter-marking for ender chests, shulker boxes, and entity
  containers** (a scope line on the *new* feature, not a behavior
  drop): these stay **sortable** (P4/P5) but can never be **marked**
  (§5.6). Ender chests are per-player with no fixed location; shulker
  boxes and minecart/boat chests are mobile — per-chest records keyed
  by world+coords cannot follow them, and a filter that can travel
  into someone else's base is a grief tool.

---

## 3. Architecture overview

One plugin, four parts, all event-driven:

1. **Sort engine** (§4) — the parity engine.
2. **Filter engine** (§5–§6) — per-chest white/blacklist deletion with
   confirm-arming, a recovery buffer, and Protect logging.
3. **Selector GUI** (§7) — Java inventory GUI + Bedrock (Geyser/Cumulus)
   form, contents-seeded.
4. **Integrations** — PlaceholderAPI (own expansion), ExcelMenu
   (registry data only), GriefPrevention (rights checks via its API),
   ExcelProtect (deletion audit via an isolation sink, §6.3),
   ExcelLandmarks (plot trust falls out of the GP checks, §6.4).

**Performance law:** no repeating task ever scans chests or the world.
Every action hangs off an event (open, close, click-completion,
hopper-move, block-break, claim-change). The only scheduled work is an
hourly buffer-expiry sweep over the in-memory buffer index (a list,
not the world) and it does no block access. A filter check is one map
lookup (chest key → record) plus set membership per stack — bounded by
one inventory's contents (≤54 stacks).

**Fail-safe law:** any doubt = no deletion. Unknown chest state, GP
rights unverifiable, Protect sink unbound, buffer write failure, or a
chest whose shape changed unexpectedly → the filter does nothing and
the reason is logged to console once per chest per hour (rate-limited).

---

## 4. Sort engine

- **Trigger model (parity):** sort on **open** and on **close** of a
  sortable inventory, for players whose toggle is ON. There is no
  deposit-into-other-chests routing (upstream never had it; "auto-sort"
  on WRV means *tidy the container you opened*, and player canon says
  exactly that — §12).
- **Algorithm (parity):** collect non-empty stacks; order by Material
  registry order, then material key, then item-meta string; clear;
  re-add (merging stacks). Port the upstream `SortTest` cases into our
  test suite as the parity baseline (§11).
- **Guards (parity):** real-storage holder check identical in effect
  to upstream's (block `Container`, `DoubleChest`, `StorageMinecart`,
  `ChestBoat`; ender chest only when it is the opener's own); skip if
  any *other* viewer has the inventory open.
- **Order of operations with the filter engine:** on **close**, the
  filter pass (§5.3) runs **first**, then the sort pass tidies what
  remains. On **open**, sort only — opening a chest must never delete
  anything (a player opening a chest to *rescue* an item must win).
- Sorting a marked chest is otherwise unremarkable: the sort may move
  stacks inside the same inventory; the close-time filter pass has
  already removed rejects.

## 5. Filter-delete engine

### 5.1 Chest records

A chest becomes a **filter chest** only by an explicit marking flow
(§6.1). A record holds: a stable `chestId` (UUID), the block key(s)
(world + x/y/z; two keys for a double chest, §5.5), `owner` (the UUID
with configure rights at marking time), `mode` (`off` | `whitelist` |
`blacklist`), `items` (a set of Materials — matching is **material-
level** in 0.1.0; see §5.4), `created` / `updated` timestamps, and a
`suspended` flag (§6.4). Only **placed chests and barrels** can hold a
record (D4).

Mode `off` keeps the record and its list but deletes nothing — the
player's way to pause a filter without losing its setup.

### 5.2 What the filter does

When enforcement runs on a filter chest with mode ≠ `off`:

- **Whitelist:** any stack whose material is **not** on the list is a
  reject.
- **Blacklist:** any stack whose material **is** on the list is a
  reject.
- A reject stack is removed **whole** (all units — §5.4) and moved to
  the recovery buffer (§6.2), credited per §6.2, and logged (§6.3).
  Nothing is ever deleted in place; "deletion" in this spec always
  means *removed to the buffer*, with permanent deletion happening
  only at buffer expiry.

### 5.3 Enforcement points (decided)

1. **Interaction completion** — after any click/drag sequence that
   changes a marked chest's contents (deposit, shift-click, hotbar
   swap, double-click collect), a **1-tick-delayed check** of that one
   inventory runs. Checks coalesce: at most one pending check per
   chest. This catches a deposit even if the player keeps the chest
   open, dies, or disconnects with it open.
2. **Close sweep** — on inventory close, the full contents are checked
   (this is also where the sort interplays, §4).
3. **Sort-arrival check** — if the sort engine ever runs on an
   inventory whose contents include items just placed there by a
   player action covered above, points 1–2 already cover it; there is
   no separate container-to-container sort in this design (§4), so no
   additional point exists. *(Stated explicitly because the design
   brief asked: there is no cross-chest sorting to hook.)*
4. **Hopper feed** — §5.7 (in scope, config-gated, default on).
5. **Arm sweep** — the one-time sweep at confirm time (§5.8).

Not enforcement points: opening a chest (§4), chunk load, server
start, timers.

### 5.4 Deletion mechanics and matching

- **Whole stack, always.** Partial-stack filtering (keep 10, delete
  54) is not in 0.1.0: it surprises players and complicates the buffer.
  A stack passes or goes, entire.
- **Matching is by Material only.** A whitelist entry for
  `DIAMOND_SWORD` passes every diamond sword, plain or enchanted.
  Exact-fingerprint entries (this specific named sword) are a possible
  0.2 feature, not 0.1.0.
- **No "special item" exemption — considered and rejected.** A guard
  that never deletes enchanted/renamed items was considered; it would
  silently break whitelist semantics for gear-storage chests (the
  chest would quietly keep everything valuable forever, and the player
  would think the filter broken). The safety net for the disaster
  case is the system Mike approved: itemized arm-confirm (§5.8/§6.1)
  + recovery buffer (§6.2) + Protect log (§6.3). A netherite sword
  deposited into a whitelist chest that doesn't allow it **is**
  removed — to the buffer, recoverable for the full window, and
  Protect-logged (§11, case T1).

### 5.5 Double chests

A double chest is **one logical filter chest**. Marking either half
marks the whole; the record stores both block keys (canonical order:
the `DoubleChest` holder's left half first). Enforcement always looks
up *either* half's key and finds the one record.

- **One half broken:** the record downgrades to the remaining single
  chest (key updated, list/mode kept). Logged (§6.3, `autosort_unmark`
  cause `half-broken` is *not* emitted — instead an
  `autosort_chest_reshaped` record; the filter keeps working).
- **A single marked chest becomes double** (someone places a chest
  beside it): if the placer holds configure rights (§6.4), the new
  half joins the record. If not, the filter **pauses** (`suspended:
  true`, reason `span-changed`) until a rights-holder opens the
  selector and re-confirms — the filter never silently extends to a
  chest half it wasn't armed on.

### 5.6 Shulker boxes, ender chests, barrels, entity containers

- **Barrels:** fully markable, same as chests (they are fixed storage
  and upstream already sorts them).
- **Shulker boxes:** **not markable** (D4). A shulker box *inside* a
  filter chest is an ordinary item: its material (`SHULKER_BOX` and
  color variants) can be on a list, and if filtered it goes to the
  buffer **with its contents untouched inside it**. Its contents are
  never inspected.
- **Ender chests:** **not markable** (per-player, no fixed location).
  Sorting parity only.
- **Storage minecarts / chest boats:** sortable (P5), **not
  markable** (mobile; no stable key).

### 5.7 Hoppers (decided: in scope)

Hoppers are the main farm use case (a mob-farm drop chest that voids
rotten flesh but keeps the bones is the feature's headline), so:

- `InventoryMoveItemEvent` with a **marked chest/barrel as
  destination**: the moved stack is checked against the filter before
  it settles (implemented as: allow the move, then run the same
  1-tick coalesced check as §5.3.1 — one code path, no event
  cancellation races). Rejects go to the buffer credited to the
  **chest owner** (no human actor), Protect-logged with `via: hopper`.
- Hoppers pulling **out of** a filter chest: untouched — normal
  vanilla behavior.
- Config kill-switch `filter.hoppers: true` (default **on** —
  DECIDED Nov 6, 2026, §14 D3). The check is a map lookup + set test per moved stack —
  event-driven, no polling — but a pathological redstone farm can
  turn the switch off per-server without a rebuild.

### 5.8 Retroactivity (decided: itemized confirm sweep — not silent, not forward-only)

When a player sets or changes a mode/list on a chest that **already
holds items which would be rejects**, nothing is deleted at edit
time. Instead the GUI's arm step (§7.4) computes the exact reject set
from the chest's *current* contents and shows it itemized — material
+ count per line, total stacks — with plain words: *"These don't pass
the filter. Move them to your recovery box, or cancel."*

- **Confirm** → the arm sweep runs once: those stacks go to the
  confirmer's buffer, Protect-logged with `cause: arm-sweep`, and the
  mode/list takes effect.
- **Cancel** → nothing changes: the previous mode/list stays exactly
  as it was (or the chest stays unmarked).

Why not forward-only: a whitelist chest that keeps yesterday's junk
forever isn't a whitelist chest — the player would have to hand-empty
it once anyway, which is exactly the chore the feature exists to
kill. Why not silent-retroactive: on a family server, experimenting
on the main storage chest must never be able to vaporize its contents
on a tap. The itemized confirm is the honest middle: the player sees
precisely what will go, and the buffer makes even a confirmed sweep
undoable for the full window. The same rule applies to later list
edits that would make *current* contents into rejects (the edit
re-arms through the same confirm screen; edits that create no new
rejects among current contents apply silently).

---

## 6. Safety system

### 6.1 Marking requires an itemized confirm

A chest starts deleting only after the configure flow's final confirm
screen, which always states — in config-driven plain language (§9) —
the mode, the list size, and, when current contents contain rejects,
the itemized sweep list (§5.8). There is no path to an armed filter
that skips this screen, including staff commands (§8: staff get a
text version of the same confirm, `--confirm` with the sweep printed
to chat first — no silent arm).

### 6.2 Recovery buffer (core feature)

- Every removed stack lands in a **per-player recovery buffer** of
  serialized ItemStacks (full payloads — a recovered sword is the same
  sword, enchants, name and all).
- **Credit rule:** a removal caused by a player's own deposit credits
  **that player's** buffer (recovery is about undoing your own
  mistake — including depositing into *someone else's* filter chest).
  A removal with no human actor (hopper feed, arm sweep is the
  confirmer's own) credits the **chest owner** for hopper feed, and
  the **confirmer** for the arm sweep.
- **Window:** each entry expires `recovery.hours` after removal —
  **default 7 days / 168h** (config; DECIDED Nov 6, 2026, §14 D2).
  Expiry is permanent deletion,
  logged (§6.3, `autosort_buffer_purge`).
- **Cap:** `recovery.max-stacks` per player, **default 162** (three
  double-chests' worth). Overflow purges the *oldest* entries first,
  each purge logged the same way.
- **Claiming:** `/autosort recover` (and the menu entry, §8) opens the
  buffer as a paged GUI (Java) / form list (Bedrock): each entry shows
  item, count, source chest location, and time left. Tap = claim that
  stack into the player's inventory; overflow drops at the player's
  feet. "Claim all" button included. Claiming is logged
  (`autosort_recover_claim`).
- Buffer contents survive restarts (persisted, §9) and survive the
  chest being broken or unmarked.

### 6.3 ExcelProtect integration — every deletion is auditable

ExcelAutoSort never keeps the only copy of a deletion record. It
publishes to ExcelProtect through an **isolation sink** — the
BossTrial 0.3.4 pattern (`ProtectEmitSink`): ExcelAutoSort's own code
talks only to a plain interface in JDK types; the one adapter class
that touches Protect's publish API is bound only when ExcelProtect is
present and enabled; the sink **never throws and never blocks** the
filter action; when unbound it is a NOOP and ExcelAutoSort's own
console log + buffer record remains (Protect's absence must never
stop a family cleanup — but see §3 fail-safe law: if Protect is
*expected* and its bind fails at startup, the console says so loudly,
once).

Event shape — Protect stream **`items`**, envelope per Protect's
standard (`ts`, `ts_ms`, `type`, `world`/`x`/`y`/`z`), with fields:

| Field | Meaning |
|---|---|
| `actor` | UUID + name of the depositing player, or `null` for automation |
| `actor_kind` | `player` \| `hopper` \| `arm-sweep` \| `sort-arrival` |
| `chest_owner` | UUID + name of the record owner |
| `chest_key` | canonical block key of the chest (left half for doubles) |
| `mode` | `whitelist` \| `blacklist` |
| `item` | material key + full serialized payload of the removed stack |
| `count` | stack size removed |
| `buffer_id` | the buffer entry id it went to |
| `buffer_owner` | UUID the buffer entry was credited to |
| `expires_ms` | when the buffer entry becomes permanent |

Event types: `autosort_filter_delete` (the one above),
`autosort_filter_arm` (mode/list armed, with the sweep summary),
`autosort_unmark` (record removed — causes: `owner`, `block-break`,
`claim-change`), `autosort_chest_reshaped` (§5.5),
`autosort_filter_suspend` / `autosort_filter_resume` (§6.4),
`autosort_recover_claim`, `autosort_buffer_purge`. Because Protect
records carry the full payload, chain-of-custody works end to end: a
parent asking "where did my sword go" gets chest, time, mode, who
deposited it, and where it was recovered to or purged.

### 6.4 Who may mark and configure — anti-grief rule

Rights are checked through **GriefPrevention**, the same checks that
govern the chest itself:

- **Configure** (mark, unmark, change mode, edit list, re-confirm a
  suspended record) requires one of:
  - the player is the **owner of the GP claim** the chest stands in;
  - the player holds **PermissionTrust** in that claim (GP's
    trust-management level — if you can manage the claim, you can
    manage its filters);
  - the chest stands in a **Landmarks plot subdivision** and the
    player is the plot's renter (the renter holds build+manager trust
    in the subdivision — treat the renter as claim-manager *for that
    subdivision*; the GP admin-claim owner keeps override);
  - the chest stands in **unclaimed** land and the player is the
    record's `owner` (for a new mark: any player may mark an
    unclaimed chest and becomes its owner);
  - the player holds `excelautosort.admin` (staff override; required
    in **admin claims**, e.g. spawn, where no player is claim owner).
- **ContainerTrust and AccessTrust are NOT configure rights**
  (Mike's explicit rule, Nov 6, 2026, 11:52 PM CST — this distinction
  is the point of the rule): being allowed to *open* or *use* a chest
  never grants the right to change its filter settings or to arm
  deletion on it. Settings access and chest use are separate trusts
  and are checked separately; a container-trusted family member
  deposits and withdraws exactly as before, and their own removed
  rejects still credit their own buffer — they simply cannot open
  the settings.
- **Refusal shape (both entry points, §7.1):** a player without
  configure rights gets the plain no-permission line
  (`messages.no-permission`) — no selector, no form, and nothing
  about the chest's current mode or list. There is **no read-only
  view** (this supersedes the read-only paragraph that stood in
  §7.1 before this ruling).
- **Deposit/use** needs no ExcelAutoSort right at all: anyone GP
  already lets open the chest can put things in it (deletions of
  their rejects credit *their* buffer, §6.2).
- If GP is absent or the claim lookup fails, configure is **denied**
  (fail closed, §3).
- **Chest broken:** breaking the container block removes the record
  once the last half is gone (§5.5), whoever breaks it — GP already
  decided they may break it. Buffer entries are unaffected. Cause is
  logged (`autosort_unmark`, cause `block-break`). Explosions count
  the same way. Chests can't be piston-moved, so no piston rule is
  needed.
- **Claim changes hands** (GP claim deleted, transferred, or a plot's
  renter changes): every record inside becomes **suspended** — the
  filter does nothing — until a player with configure rights under
  the *new* ownership opens the selector and re-confirms. Suspension
  and resumption are Protect-logged. A filter must never keep
  deleting under an owner who never agreed to it.

---

## 7. Selector GUI — contents-seeded, Java + Bedrock parity

### 7.1 Opening the selector

**Targeted access ONLY (Mike, Nov 6, 2026, 11:51 PM CST).** Filter
settings are configured by standing at the chest and targeting it.
There is no remote browsing, no chest list, and no way to select a
chest from a distance anywhere in the plugin — every path below
resolves to the one chest the player is physically at. If the
command runs with no chest in the crosshair, it fails with the plain
`look-at-chest` line ("Look at a chest or barrel and try again") and
nothing opens. (The plain `/autosort` sort toggle is not a settings
path and is unchanged.)

Four paths, all landing on the same screen for that one chest, all
running the identical §6.4 configure-rights check:

- **`/autosort chest`** while looking at a chest/barrel (target block,
  ≤ 6 blocks) — the main path, and what the menu entry runs (§8).
- **Compass tap (Mike, Nov 6, 2026, 11:51 PM CST):** hitting or
  tapping a chest/barrel while holding a **compass in the main hand**
  opens that chest's filter menu — the house compass interaction
  pattern. Same chest resolution and same configure-rights check as
  the command path; a chest the player may not configure gives the
  plain no-permission response, never the menu.
- **Sneak + right-click** a chest/barrel with an empty hand — the
  discoverable path (only fires for players with configure rights;
  everyone else gets normal chest opening).
- The **ExcelMenu "Chest filter" entry** (§8) — runs the same command;
  if the player isn't looking at a container, a plain chat line says
  what to do ("Look at a chest and tap again").

A player who lacks configure rights for the targeted chest gets the
plain no-permission line (`messages.no-permission`) and **nothing
else** — no menu, no form, no read-only view, and no information
about the chest's current mode or list (§6.4, Mike's rule of Nov 6,
2026, 11:52 PM CST; this replaces the read-only view described in
earlier drafts of this section).

### 7.2 Layout (Java inventory GUI, 54 slots)

- **Row 1 — mode control:** three buttons, **Filter off** /
  **Keep only these (whitelist)** / **Throw away these (blacklist)**.
  The CURRENT mode is shown per the menu-toggle standard: its button
  carries the state in its label and icon ("✔ Filter: KEEP ONLY
  THESE") — a player can always see which mode is live before
  touching anything. Switching to **off** applies immediately.
  Switching to a filtering mode starts the arm flow (§7.4).
- **Rows 2–5 — the contents-seeded grid (28 slots/page):** one slot
  per **distinct material currently in the chest**, icon = that item,
  count = how many are in the chest. State is visual and in the lore:
  **on the list** = glint + "ON the list — tap to take off";
  **not on the list** = plain + "Not on the list — tap to add".
  Below the chest's own contents, **list entries not present in the
  chest** are shown too (dimmed, "not in this chest right now — tap
  to take off"), so a list can be pruned without fetching the item.
- **Row 6 — actions:** **[Add from my hand]** (captures the material
  of the item in the player's main hand), **[Add from my
  inventory]** (opens a picker page of the distinct materials in the
  player's own inventory; tap adds and returns), **page arrows** when
  contents + list exceed 28 entries, **[Done]** (closes; list edits
  are already live, §7.3), and a **buffer shortcut** ("Recovery box —
  N stacks waiting", opens §6.2's GUI).
- All player-facing strings come from config (§9).

### 7.3 Save model (decided: list edits live; arming is confirm-gated)

**List toggles apply the moment they're tapped** (and persist) — the
same tap-and-see-it house UX as the menu toggles. There is no save
button for the list. What is *not* live is **arming**: the first
switch into a filtering mode, and any later edit combination that
would make the chest's current contents contain rejects, routes
through the §7.4 confirm before anything deletes. Toggling items on
the list of a chest in mode `off` deletes nothing and needs no
confirm until the mode is armed.

### 7.4 The arm/confirm screen

A second page (Java) / modal form (Bedrock) showing, in plain words:

- the mode in words: *"Keep only the 6 kinds on the list. Anything
  else put in this chest will be removed to the recovery box."* (or
  the blacklist equivalent);
- if current contents contain rejects, the **itemized list**:
  material + count per line, and the total;
- **[Move those to my recovery box and turn the filter on]** /
  **[Not yet]** — nothing is pre-selected; closing the screen =
  "Not yet".

### 7.5 Bedrock parity (Geyser/Cumulus)

The same flow as a Cumulus form set, same order, same words:

- Mode as three radio options with the **current mode pre-selected
  and named** ("Filter mode now: Keep only these").
- The contents-seeded list as **toggle switches**, one per distinct
  material, **starting position = current list state**, labeled with
  the in-chest count ("Iron Ingot ×34 — on the list").
- List entries absent from the chest appear after, labeled as such.
- Buttons for **Add the item in my hand**, then **Save** (form submit
  applies the toggle set at once — the form model differs from the
  Java live-tap, but the arm/confirm modal that follows is identical,
  so the safety semantics match exactly).
- The confirm is a modal with the itemized rejects and the same two
  choices. Bedrock players must never get a weaker confirm than Java
  players: the itemized list is in the modal text, not behind a tap.

---

## 8. Commands, permissions, placeholders, menu registry

### Commands (all under `/autosort`, alias `/sort` — P2)

| Command | Does | Permission |
|---|---|---|
| `/autosort` | Toggle my auto-sort on/off (parity P1/P2) | `excelautosort.toggle` (default true) |
| `/autosort status` | Tell me my state in chat (ON/OFF; marked chests I own; buffer count) | `excelautosort.toggle` |
| `/autosort chest` | Open the selector for the chest/barrel I'm looking at (§7) | `excelautosort.chest` (default true; GP rights still gate editing, §6.4) |
| `/autosort chest add <material>` / `… remove <material>` | Text-path list edit for the looked-at chest (tab-completed; runs the same arm/confirm rules, printed to chat) | `excelautosort.chest` |
| `/autosort recover` | Open my recovery buffer (§6.2) | `excelautosort.recover` (default true) |
| `/autosort admin prune` | Drop chest records whose block is no longer a container (§9) and print the count | `excelautosort.admin` (default op) |
| `/autosort admin reload` | Reload config + data | `excelautosort.admin` |

Every command has a console-safe refusal line for non-players (parity
with upstream's "not available for non-players"), except `admin`
subcommands, which console may run.

### Placeholders (own PlaceholderAPI expansion, identifier `autosort`)

- **`%autosort_status%`** → `ON` / `OFF` — same contract the bridge
  served (P9), now read from ExcelAutoSort's own live state. It can
  only be null (→ "unavailable") when the plugin itself is absent —
  which is exactly the menu's fail-closed case.
- **`%autosort_buffer%`** → the count of stacks waiting in the
  player's recovery buffer (for the recovery menu entry's label).

### ExcelMenu registry changes (data files, applied in the swap window)

1. **Entry `autosort` — updated, two fields only.** `requires-plugin:
   AutoSort` → `ExcelAutoSort`; `requires-permission: autosort.toggle`
   → `excelautosort.toggle`. Everything else — id, Settings group,
   **order 10 / order-bedrock 200**, icons, state labels, lore,
   `[player] autosort` + `[refresh]` actions, `status-placeholder:
   '%autosort_status%'` — is byte-unchanged. (The command name and the
   placeholder survive the swap by design; that's what makes the
   two-field diff sufficient.)
2. **NEW entry `chestfilter`** — Settings group, **Java order 15,
   Bedrock order 205** (immediately after Auto-sort). Label: "Chest
   filter: set up a chest" with lore in plain words ("Choose what a
   chest keeps — or throws away"); action `[player] autosort chest`.
   No state variants (it's a launcher, not a toggle).
3. **NEW entry `filterrecover`** — "Your Items & Help" group, placed
   beside Lost & Found / Trash Bin. Label carries the live count via
   `%autosort_buffer%`: "Filter recovery box (N waiting)"; action
   `[player] autosort recover`. This keeps the recovery path one tap
   from the same menu neighborhood players already use for lost
   things.
4. **Entry `autojoin` — untouched** (P11; verified byte-identical in
   the swap packet, §10).

Per the Nov 3 lesson, these entry files are also lifted into the next
ExcelMenu jar's bundled `entries/` verbatim at its next build, so a
fresh data folder can never regress them.

## 9. Config and data storage

### Config — `plugins/ExcelAutoSort/config.yml` (config-first)

All player-facing text lives here (toggle feedback, GUI labels, mode
names in plain words, confirm-screen wording, notices), following the
house config-first pattern. **Config posture (reaffirmed by Mike,
Nov 6, 2026, 11:51 PM CST):** the file must stay easy to change and
easy to ADD to — clean sections, a comment on every knob saying what
it does and what changing it means, all behavioral knobs and all
player-facing sentences in this one file; a future knob joins the
section it belongs to without restructuring anything. Behavioral keys:

```yaml
sort:
  types: [CHEST, BARREL, ENDER_CHEST, SHULKER_BOX]   # parity P4; display names for feedback in messages:
filter:
  hoppers: true            # §5.7 (DECIDED Nov 6, 2026, §14 D3: default ON)
  hopper-worlds: []        # optional per-world override; empty = all worlds
recovery:
  hours: 168               # buffer window — 7 days (DECIDED Nov 6, 2026, §14 D2)
  max-stacks: 162          # per-player buffer cap
selector:
  page-size: 28            # contents-grid slots per page
messages: { … }            # every player-facing string
```

### Data — `plugins/ExcelAutoSort/data/`

| File | Holds |
|---|---|
| `players.yml` | the ON set (UUID list) — P8's successor to `active.txt` |
| `chests.yml` | chest records keyed by `chestId`; each record carries its block key(s) as `world:x:y:z` strings plus a reverse index by block key for O(1) event lookup |
| `buffers.yml` | per-player buffer entries: id, serialized ItemStack (Bukkit serialization, Base64), removed-at, expires-at, chest key, cause |

Rules: saves are **atomic** (write temp file, rename) and happen on
every mutation plus on disable; loads are fail-soft per record (a
corrupt record is skipped and logged, never fatal). Records are pure
data — **chunk unload/load touches nothing**; a record whose block
was WorldEdited away simply never matches an event, and is cleaned
lazily when any flow touches its key (validation: block must still be
a chest/barrel, else prune + `autosort_unmark` cause `block-gone`) or
by `/autosort admin prune`. World deletion is handled the same lazy
way. Double-chest records store both keys; lookup by either key
resolves to the one record.

## 10. Migration and the swap packet

### Migration (automatic, first enable)

- **Toggle state:** if `data/players.yml` doesn't exist yet and
  `plugins/autosort/active.txt` does, import its UUIDs as the ON set
  and log the count ("imported N auto-sort states from AutoSort").
  Nobody's toggle resets on swap day (P8).
- **Filter chests:** none can exist — every chest starts unmarked.
- **Buffers:** empty by definition on day one.

### Swap packet shape (one window, house install discipline)

BACKUP FIRST + cap-at-6, emptiness gate (`list` 0/10 twice + pre-swap
check), marker discipline — the standard gates, with packet-specific
steps:

1. Pre-swap gates (exact, from the Nov 3 install record): live
   `autosort-0.1.17+26.3-paper.jar` present; `ExcelAutoSortBridge-
   0.1.0.jar` present; live `entries/autosort.yml` = 1,994 B /
   `36276320…`; live `entries/autojoin.yml` = 1,071 B / `57269d2d…`.
2. Place `ExcelAutoSort-0.1.0.jar`; update `entries/autosort.yml`
   (two fields, §8); add `entries/chestfilter.yml` +
   `entries/filterrecover.yml`; retire both old jars to `.held`
   names (not deleted) in the same window.
3. Panel Shut Down → Start (never console `stop`).
4. Boot verification: ExcelAutoSort enable line incl. the import
   count and the Protect-bind line; ExcelMenu registry count grows
   by exactly 2 with 0 failed; `%autosort_status%` resolvable;
   guild LOCKED 3 of 5 unchanged; SEVERE 0; server left ONLINE.
5. **Rollback** = remove the ExcelAutoSort jar, restore the two
   `.held` jars and the pre-swap `autosort.yml` (kept as
   `.bak-<date>`), restart. The `plugins/ExcelAutoSort/` data folder
   is **left in place** by rollback — buffer contents in it stay
   recoverable if ExcelAutoSort returns; nothing a player was owed
   is destroyed by rolling back.

## 11. Testing — acceptance cases

Unit tests (pure core: filter matching, comparator, buffer
expiry/cap math, record key handling) plus a local Paper 26.3 rig
with GriefPrevention, ExcelProtect, PlaceholderAPI and ExcelMenu
loaded. Parity cases first, then the filter cases.

**Parity regression (P1–P11):** toggle default OFF → `/autosort` →
menu label flips ON with `[refresh]`, `/papi parse` spot-check; sort
on open and on close across chest/barrel/ender/shulker; no sort with
a second viewer; upstream `SortTest` ordering cases pass verbatim;
state survives restart; autojoin entry byte-identical post-swap.

**Filter cases:**

- **T1 (the disaster case):** a netherite sword deposited into an
  armed whitelist chest whose list excludes swords → sword removed to
  the depositor's buffer within a tick, claimable whole via
  `/autosort recover`, and a complete `autosort_filter_delete` record
  (actor, chest, mode, payload) exists in Protect's `items` stream.
- **T2 (arm with contents):** chest holding cobblestone + diamonds;
  arm blacklist with cobblestone on the list → itemized confirm names
  cobblestone ×N only → confirm: cobble to buffer, diamonds
  untouched, filter live. Repeat with **cancel**: nothing moves, mode
  stays off.
- **T3 (hopper feed):** hopper feeding an armed blacklist chest:
  listed junk is buffered (credited to chest owner, `via: hopper`),
  keepers accumulate; with `filter.hoppers: false`, junk accumulates
  instead.
- **T4 (double chest):** mark by tapping one half → deposits in
  *either* half are filtered; break one half → record downgrades,
  filter keeps working on the single chest; break the last half →
  record gone, buffer intact.
- **T5 (span change):** untrusted player places a chest beside a
  marked single chest → filter suspends; owner re-confirms in the
  selector → resumes across both halves.
- **T6 (rights):** a player without GP rights at the chest gets the
  read-only selector and cannot arm; a ContainerTrust depositor's
  rejects credit the *depositor's* buffer; the claim owner can
  configure; after the claim is deleted, the record suspends and
  deletes nothing until re-confirmed.
- **T7 (Bedrock selector):** full flow on a Bedrock client — open
  from the menu, mode shows current state, toggle a contents item,
  arm with an itemized modal, deposit a reject, claim it back from
  the recovery form.
- **T8 (buffer lifecycle):** expiry purges at the configured window
  with a purge record; cap overflow purges oldest first; claim-all
  with a full inventory drops the overflow at the player's feet.
- **T9 (shulker discipline):** a filled shulker box deposited into a
  whitelist chest that doesn't list it → the box is buffered **with
  contents intact**; a shulker's contents are never filtered even
  when the box sits in a marked chest it passes.
- **T10 (fail-safe):** ExcelProtect disabled → filter still works,
  console logs the unbound sink loudly once, buffer remains the
  record; corrupt one `chests.yml` record → plugin enables, bad
  record skipped and logged.
- **T11 (no-scan audit):** code review line — no repeating task
  touches blocks or inventories; the only timer is the hourly buffer
  sweep over the in-memory index.

**Player proofs owed after install** (not rig-substitutable): one
Java and one Bedrock player each arm a real chest and recover a real
item; Mike (Bedrock) confirms the two new menu entries and the
Auto-sort entry's state display.

## 12. Player-facing canon (drafts for the same-pass surfaces)

**Guide / website paragraph (draft):**

> **Auto-sort and chest filters (ExcelAutoSort, made by us).** Turn
> auto-sort on in the menu (Settings → Auto-sort) and your chests,
> barrels, shulker boxes and ender chest tidy themselves whenever you
> open them. For maximum organization, a chest or barrel can also get
> a **filter**: look at it and choose **Chest filter** from the menu.
> Pick "Keep only these" and the chest holds just the kinds you tap
> from what's already inside it — anything else put in is removed.
> Pick "Throw away these" and the chest removes just the junk you
> pick, like rotten flesh. Removed items are never gone right away:
> they wait in your **Filter recovery box** (menu → Your Items) for
> 7 days, and every removal is written down, so nothing is ever
> lost by accident. Only chests you set up this way ever remove
> anything, and setting one up always shows you exactly what will be
> removed first.

**What's New line (draft):**

> **New: chest filters!** Our own ExcelAutoSort replaces the old
> auto-sort: chests can now keep only what you choose — or throw away
> the junk — with a 7-day recovery box so nothing is ever lost.
> Settings → Auto-sort and Chest filter.

## 13. Build and release standards

Paper 26.3 API, JDK 25, deterministic double-build (byte-identical)
per house practice; authors `[Iris, Mike]`; console-help present
(`/autosort` with no valid target prints usage — house console-help
rule); build fingerprint injected at release-build time per
`~/workspace/plugin-publishing/FINGERPRINT-STANDARD.md`, with marker
verification as a release gate. SOURCE.md at build time records repo,
version, checksums, ownership split (100% ours — the MIT upstream is
a behavior reference only; no upstream code is copied into the tree:
the sort comparator's *ordering contract* is reimplemented from this
spec, and the spec is the provenance).

## 14. DECIDED questions (were OPEN; ruled by Mike, Nov 6, 2026, 11:32 PM CST)

Mike's ruling, in his own words in chat: **"Go with all three
recommendations"** — all three recommendations below are adopted as
written and are binding on the 0.1.0 build.

- **D1 — Recovery surface: plugin-local buffer. DECIDED.**
  Removals go to ExcelAutoSort's own recovery buffer (§6.2), NOT
  through ExcelProtect's Lost & Found. (Recommendation adopted:
  Protect's Lost & Found is death-scoped — its claims, pricing and
  windows are built around deaths — and bolting filter recovery onto
  it couples two release trains; the audit trail already lives in
  Protect via §6.3, which is where the "one place to ask" value
  actually sits. Revisit only if the family finds two recovery boxes
  confusing in play.)
- **D2 — Buffer window default: 7 days / 168h. DECIDED.**
  The shipped config default for `recovery.hours` is **168** (§6.2,
  §9). (Recommendation adopted: the family plays heaviest on
  weekends; an item filtered on Friday night and missed would be
  unrecoverable by Monday at 24h; the buffer cap (§6.2) already
  bounds storage, and a longer window costs nothing but patience.)
- **D3 — Hopper filtering default: ON. DECIDED.**
  `filter.hoppers` ships **true** (§5.7, §9). (Recommendation
  adopted: every mob farm's drop chest can void junk hands-free,
  which is the feature's headline use; the config kill-switch and
  `via: hopper` attribution keep it honest and auditable.)

---

## 15. Max Organization — ExcelAutoSort 0.2.0 fast-follow (SPEC ONLY — not in the 0.1.0 build)

Designed Nov 6, 2026, 11:54 PM CST (Mike's feature idea; parameters
settled the same night in Mike + Iris design calls, stated as such).
This section is a complete design for the release that lands **right
after the 0.1.0 swap**. Nothing in this section is built, shipped, or
implied by 0.1.0; it exists so the 0.2.0 build starts from settled
decisions, not a fresh pitch.

**Status amendment (Nov 7, 2026):** §15 was implemented as
ExcelAutoSort 0.2.0 the same morning Mike commissioned it
(07:48 AM CST) — see BUILD-RECORD.md (0.2.0) for what shipped, the
implementer's readings (§15.2 rental renewal extends an ACTIVE
rental from its current expiry; §15.3 radius measured from the
player's block center to the chest block's center — the rig's
boundary case proved that measuring from the feet point silently
shrinks a same-floor reach to radius−1 — and re-verified per move
from the player's current position), and the rig evidence. The
paragraphs below remain the design of record as written the night
before.

### 15.1 Concept

In Mike's words, in substance: a player rents or buys access to
"max organization" — from where they stand they can open and manage
**all the chests around them that they own, all at once**, moving
stuff around to get organized without walking chest to chest. One
standing spot, one view, the whole storage corner.

### 15.2 Unlock — rent or buy (the house duality, same shape as ExcelLandmarks)

Mike's pricing ruling (Nov 7, 2026, 12:01 AM CST): *"$1,000 one-time
is too cheap — rental or high price to purchase."* The unlock is
therefore rent-or-buy:

- **RENT: $1,000 per 30 days.** Config knobs `max-org.rent-price`
  (default `1000`) and `max-org.rent-days` (default `30`) — Mike's
  original $1,000 becomes the monthly rate. Renting is a fresh
  confirm purchase every time: **no auto-charge, ever** (house rule:
  confirms before charges). On expiry the network ability simply
  **lapses** — and because the feature stores nothing (§15.4: the
  view is a window, never a warehouse), a lapse carries **zero item
  risk**; nothing is held, stranded, or lost. **Re-renting restores
  the ability instantly**, from the moment the renewal confirms.
- **BUY: $15,000 one-time, permanent.** Config knob
  `max-org.buy-price` (default `15000`). No expiry, no renewals.
- **Rent-then-buy:** a player who rents and later buys keeps the
  feature permanently from the buy. **No credit for rent already
  paid** — rent buys time, not equity — and the player-facing copy
  says so plainly before any charge, so nobody expects proration.
- Both purchases flow through the menu/command with a
  **confirm-before-charge step** (house menu standard: the confirm
  names the option, its price, and its duration or permanence — and,
  for rent, the lapse behavior; no charge happens on the first tap).
- **Player-facing copy states both prices and the lapse behavior up
  front**, on the unlock screen itself, not buried in a confirm:
  rent $X per N days (lapses when it runs out; re-rent anytime, no
  auto-charge), buy $Y once (yours forever; rent already paid does
  not count toward it).
- Access state **persists per player** (stored with the plugin's
  player data: a renter-until timestamp or the permanent flag;
  survives restarts; not transferable; refunds are a manual staff
  economy action, never a plugin action).
- **Economy integration** goes through the server's existing economy
  path — the same one ExcelProtect already audits — and **every
  purchase (rent, renewal, buy) is Protect-logged** (actor, kind,
  amount, expiry-after where applicable) like every other money
  event on the server.
- An insufficient-funds attempt charges nothing, changes no access
  state, and says so plainly (§15.7 tests).

### 15.3 Reach and rights — the network

- **Radius:** config knob `max-org.radius`, default **16 blocks**
  from the player's position.
- **The network** = every chest/barrel in radius where the player
  holds **CONFIGURE rights under the exact §6.4 rule** (claim owner /
  PermissionTrust / recorded owner in unclaimed land / Landmark-plot
  renter in their plot / staff in admin claims). **Same rule as
  configuration — no second trust model.** A chest the player may
  merely open (ContainerTrust, AccessTrust) does **not** appear; a
  chest one block past the radius does not appear.
- The network is resolved when the view opens and re-checked for
  each move (a claim change mid-session suspends the affected chest
  from the network, consistent with §6.4's suspension rule).

### 15.4 The view — a window, never a warehouse

- A **paged virtual inventory** (54-slot pages, page behavior in
  config) presenting the network's contents as one browsable space.
- **TOP INVARIANT — the view is a WINDOW onto the real chests, never
  a storage of its own.** Chest contents are the only storage;
  nothing ever lives only in the GUI. Closing the view, disconnecting,
  or a server crash leaves every item in its real chest. **Fail-closed:
  a move that cannot complete leaves the item where it was — no
  dupes, no voids, ever.** Every move is executed as a transfer
  between two real inventories (source chest ⇄ player, player ⇄
  destination chest), verified slot-by-slot, and rolled back to the
  source if the destination cannot take the whole stack.

### 15.5 Moves

- **Take:** tap/click a stack in the network view to move it to the
  player's inventory (whole stack, or what fits — a partial fit moves
  what fits and leaves the rest, never the reverse).
- **Smart deposit:** from the player's inventory, a smart-deposit
  action routes items into network chests, **respecting each marked
  chest's filter**: whitelist chests receive only listed items;
  blacklist chests refuse listed items; unmarked chests accept
  everything. An item no network chest will take stays in the
  player's inventory and is reported as unplaced, never voided.
- **Every move is Protect-logged** (actor, source chest key,
  destination, material, count) so chain-of-custody holds end to end,
  on the same §6.3 audit discipline as filter removals.

### 15.6 Bedrock parity

The same view via **Geyser chest-GUI emulation** with **tap-only
gestures** — no drag-dependent actions anywhere in the design, per
the platform-parity rule (every Java capability has a Bedrock path
that works with taps alone).

### 15.7 Config and tests

- **Config-first:** `max-org.rent-price`, `max-org.rent-days`,
  `max-org.buy-price`, `max-org.radius`, page behavior, and every
  player-facing sentence (unlock pitch with both prices and the
  lapse behavior, confirm wording, insufficient-funds line,
  unplaced-items report) live in `config.yml` under a `max-org:`
  section, added per the §9 posture without restructuring the file.
- **Acceptance tests named for the 0.2.0 build:**
  - *Fail-closed set:* kill/disconnect mid-move leaves every item in
    a real inventory; a move into a full destination leaves the item
    in its source; a move during chunk unload completes or does not
    happen at all — no dupes, no voids in any of the three.
  - *Filter-respecting smart deposit:* a listed item never lands in a
    blacklist chest that refuses it and always prefers a whitelist
    chest that lists it; unplaceable items stay with the player.
  - *Radius/rights boundary:* a chest 1 block past the radius never
    appears; a chest 1 trust level short (ContainerTrust only) never
    appears; both appear the moment the boundary is crossed the
    other way.
  - *Purchase set:* rent confirm charges exactly once and sets the
    expiry; **rent expiry lapses** the ability (the network view
    refuses, plainly) with zero item movement anywhere, and
    **re-rent restores it instantly**; **buy is permanent** across
    restarts and past any would-be rent expiry; **rent-then-buy**
    ends permanent, with no proration applied or promised; the
    insufficient-funds path charges nothing and changes nothing;
    every purchase is in the Protect stream.

---

## 16. Admin per-player surface — ExcelAutoSort 0.2.1 (Nov 9, 2026)

Commissioned by Mike Nov 9, 2026 (~8:07 PM CST) after the 0.2.0
admin surface was found to be only `prune|reload` — no
player-targeted status or revoke — violating Mike's Nov 3, 2026
console admin rule (every player-targeted state an admin may need
to inspect or correct is reachable from the console,
permission-gated, live-effect, persisted through the plugin's own
data path). 0.2.1 changes NOTHING else: every 0.1.0/0.2.0 behavior
carries over unchanged and is re-proven by the unchanged unit
tests plus the rig regression phases.

### 16.1 Commands (all under `/autosort admin`, permission `excelautosort.admin`, default op; console may run all of them)

| Command | Does |
|---|---|
| `/autosort admin status <player>` | Prints the target's auto-sort ON/OFF, Max Organization state — OWNED (permanent) / RENTED with the exact rent expiry instant + time left / LAPSED with the expiry instant / NONE — plus filter chests owned and recovery-box stacks. Read-only. |
| `/autosort admin revoke <player>` | Removes the Max Organization entitlement ENTIRELY in one action: the permanent buy flag AND any rent (active or lapsed) are cleared; the record leaves `data/maxorg.yml` in the same pass, so live access and post-restart state agree. No money moves — a refund, if one is owed, is a manual staff economy action (§15.2), never a plugin action. Revoking a player who holds nothing is a clean no-op that reports "nothing held". Protect-logged (`autosort_maxorg_admin_revoke`, before-state carried). |
| `/autosort admin grant <player> max` | No-charge permanent grant of Max Organization. INCLUDED by symmetry with ExcelCosmetics' admin surface (`grant|buy|revoke|status|setexpiry|reload`, where grant is the no-charge hand grant and revoke its mirror): a revoke without a grant leaves staff unable to repair a wrongly-revoked or lost entitlement except by faking a purchase through the economy. The grant lands in exactly the state a buy lands in (owned, rent timestamp spent), persists through the same data path, and is Protect-logged as an admin grant (`autosort_maxorg_admin_grant`, `via: admin`) — never as a purchase. Granting an existing owner changes nothing and says so. |

### 16.2 Target resolution (the ExcelCosmetics rule)

Exact online name first, then the stored record of a player who
has actually played on the server. A name that has never joined is
refused ("has never joined this server — refused"), so a typo can
never create or touch a phantom account. Offline targets work:
state is keyed by UUID in the plugin's own store, so status,
revoke, and grant all land on offline players and take effect on
their next join (and immediately, if they are online).

### 16.3 Deliberate non-changes

- No `setexpiry` analogue: Cosmetics has one as a rig/support tool;
  AutoSort's rent is a single timestamp already correctable by
  revoke + grant, and no WRV support case names a need to move an
  expiry by hand. Not built; revisit only on a named case.
- The player-facing `/autosort max rent|buy` flow, prices, and
  confirm-before-charge are untouched. Admin grant/revoke never
  charge and never refund.
