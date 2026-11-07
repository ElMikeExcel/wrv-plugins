# ExcelProtect 0.1.7 "Parity" — changelog

The retirement-unblocking build: the DeathRecord import can now run
clean, and Protect's kill records reach field parity with
ExcelTelemetry's, so both predecessor tools can retire on schedule.
Two decided features only (Mike, Nov 6, 2026) — no recording-expansion
scope.

## Fixed

- **The DeathRecord importer no longer resurrects restored items.**
  DeathRecord keeps per-item restored marks (`restored-times`), and the
  old importer ignored them: an item a player had already gotten back
  from DeathRecord would import as a fresh Lost & Found claim —
  offerable, claimable, double-restorable. The census that forced this
  fix (`protect-rollout-item13a/CENSUS-20261106.md`) counted four such
  items, all one player's restored netherite gear. Now:
  - a burn DeathRecord already restored imports stamped
    `source_restored` AND pre-marked restored in Protect's own restore
    marks, under the exact id Lost & Found computes — it is never
    offered and can never be claimed twice;
  - death-snapshot items DeathRecord already restored are excluded
    from the imported snapshot (a staff restore hands over the whole
    snapshot, so inclusion would double-give), counted on the line as
    `source_restored_items`;
  - a death whose every item was restored is marked restored outright,
    and a death whose XP was already restored imports without
    `xp_level`, so its XP cannot be granted a second time.

## Added

- **Lost & Found player state migrates with the import.**
  DeathRecord's per-player state (free-recovery-used flag, paid
  recovery count, last recovery time) now merges into Protect's
  per-player state — by delta, against a small applied-values book, so
  importing the same folder twice changes nothing and a later import
  picks up only what DeathRecord recorded since. A merge can never
  un-use a free recovery, lower a count, or rewind a timestamp.
- **Kill-field parity on `entity_death` records.** Protect's kill lines
  now carry the fields ExcelTelemetry's kill records carry, from the
  same live signals, so the one-day field diff (retirement gate G3)
  compares mechanically:
  - `elite` / `elite_level` (or `elite_probe: "unavailable"`) from the
    EliteMobs EntityTracker probe the incident streams already use;
  - `weapon` — the killer player's main hand at death time;
  - `origin_class` (+ `rg_denied` when set) from RewardGuard's PDC
    provenance marks with the stashed spawn reason as fallback — a
    field-for-field port of Telemetry's origin reader; the existing
    lowercase `origin` field is unchanged;
  - `damage.fall_share` / `damage.explosion_share` computed from the
    fight's by-cause totals with Telemetry's definitions and rounding,
    plus always-present `fall_softened`, and Telemetry's
    `explosion_share_high` integrity flag (elite victims, share ≥ 0.60).
- The import console reply and the `type=import` system record now
  report restored-marked burns/items and migrated state files.

Everything 0.1.6 does is carried unchanged; all additions are additive
fields on existing records — no stream format breakage, no config
changes.
