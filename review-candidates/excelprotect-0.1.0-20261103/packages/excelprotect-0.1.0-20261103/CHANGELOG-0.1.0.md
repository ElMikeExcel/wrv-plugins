# ExcelProtect 0.1.0 — CHANGELOG

First build, 2026-11-01 (America/Chicago). **Not yet installed** — the install packet is a SHADOW install alongside CoreProtect, DeathRecord, Telemetry and AuditLog; nothing is removed by it.

ExcelProtect is the suite's single record + recovery system (SPEC v2 + Amendment 3): append-only daily JSONL per stream, byte-exact item payloads, one async writer, no database.

## Streams (each its own daily JSONL; gzip at rollover; retention-days default 30)

blocks · containers · entities · items · players · economy · audit · combat · deaths · system · snapshots (samples) · admin. Every line: ISO ts with ms + Chicago offset, ts_ms, world/x/y/z when located, claim owner when inside a GriefPrevention claim (fail-soft adapter).

## Coverage

- **blocks** — player place/break with before/after blockstate strings and sign text; natural decay; burn; explosions (one line per block, shared event id); form/fade/grow, liquid flow, pistons (toggles, default off).
- **containers** — open/close diff transactions for chests/barrels/shulkers/hoppers + ender chests, each storing the full resulting contents (fingerprints + payloads).
- **entities** — every death with fight-window damage attribution (by_attacker/by_cause, killer, fight_s) + full equipment snapshots with payloads; armor-stand/item-frame change events; tames; origin-stamped spawns.
- **items** — drop + pickup for ALL items; owner attribution stamped into the item entity's PDC at drop (+ drop memory); despawn/burn/void into the burn ledger.
- **players** — join/leave/teleport/command, chat (OFF by default), deaths, mcMMO level-ups (adapter); inventory snapshots at join/leave/world-change and mcMMO skill snapshots in the samples stream.
- **deaths** — full byte-exact death snapshots (inventory/armor/ender chest/XP) + staff restore with once-only marks and force override; the burn ledger + **Lost & Found** (/lostfound, /deathrecord): config price/free-first chain/window/cooldown/tell-owner (defaults are the WRV-settled values, marked as defaults in config).
- **economy** — Vault money deltas with recent-command tagging; shop purchases, reward adjustments (incl. blocked at paid=0), service charges/casts via the emitter contract; villager trades; Jobs payouts (adapter).
- **audit** — XP levels, enchants, ender-chest markers, seal verdicts (via the BossTrial fight-table publish).
- **combat** — player-involved damage ledger, healing ledger (spell attribution via the Magic CastEvent feed), per-fight summaries with config gap window; BossTrial publishes its full per-player table at verdict.
- **claims/system** — GriefPrevention claim create/delete/change; imports; rollback ops; overload shed markers; **watch alerts** (burst breaks in a claim, valuable destruction, non-owner container access → staff in-game + webhook outbox).
- **admin** — every staff export/inspect/lookup/rollback/restore/import is itself a record.

## Query + recovery

`/excelprotect export` (time/stream/type/player/match/world/radius/encounter filters → timestamped export file + one console line), `inspect` (click a block → its latest event), `rollback` with **preview** (dry-run counts) and **undo** (every op captures its pre-state), `restore-stand` from equipment snapshots, `deaths` lookup/restore, `status` (writer queues/drops + adapter states), `import deathrecord <folder>` (idempotent; payloads preserved; original timestamps honored for the L&F window).

## Item provenance

Fingerprint-class items are stamped with a Protect record ID (PDC) at first sight (drop/craft); crafts are recorded; `export match=<rid>` is the per-item timeline.

## Publish API

`com.winkreyes.excelprotect.api`: RecordEvent + ExcelProtectAPI.publish (general door), FightTableEvent (boss/seal tables), EconomyEvent (emitters). All fail-soft for the emitter: no Protect, no error.

## Compaction + overload

Retention rolls expiring day files into per-type **aggregates kept indefinitely** before deleting raw lines. At writer saturation, streams shed in config order (cheapest first) with marker records + status counters — never silently.

## Configuration

Every family/sub-family toggle and every threshold is a config key; config.yml is self-documenting (what each toggle records and what it costs). No dependency on CoreProtect (it is being replaced) or on any other Excel plugin; GriefPrevention/Vault/mcMMO/Magic/Jobs are optional adapters.
