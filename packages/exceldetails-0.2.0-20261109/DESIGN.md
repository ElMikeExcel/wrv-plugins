# ExcelDetails 0.2.0 — DESIGN PASS (the checkbox mix)

Base: shipped 0.1.0 source (build EXC-DETAILS-0.1.0-WRV-20261109-01-a47f0c).
Spec: Mike's mix ruling, Nov 9, 2026 (MENU-PACKET-DETAILS-SETTINGS.md §0).
Everything not mentioned here is 0.1.0 behavior, unchanged: Protect/final-
damage intake, shared segment engine, 8 s quiet close, piggyback placement
law (negative scores under EM's party box, never setScoreboard over a
foreign sidebar board), RAW final-damage numbers (no x1000 — that rescale
is BossTrial 0.5.2's feedback display only).

## 1. The mix model

Per-player display prefs become three independent booleans:

- showDamageDone — default ON
- showDamageTaken — default OFF
- showHealing — default OFF

The legacy single "window" pref survives as the player's FOCUS window
(status display + the anchor `/details next` cycles from). The shown set
is the display truth; the focus is bookkeeping.

Migration (data.yml): if any show-* key exists for a player, it wins.
Otherwise a legacy `window:` key migrates to shown = {that window}, so a
0.1.0 player who had switched to Damage Taken keeps exactly that view.

## 2. Section rendering + the row budget (the pinned rule)

The sidebar renders EVERY enabled metric that has data in the current
(or last) segment, stacked in canonical order — Damage Done, Damage
Taken, Healing Done — each as its own separated section:

```
§6§lDetails! §7Damage Done        <- section header (0.1.0 header format)
§a1. Mike §e16 §7(100%)           <- ranked rows
§6§lDetails! §7Healing Done
§f1. Kay §e20 §7(100%)
§7 0:08 • you 1.9/s               <- one shared footer
```

- A section with no rows in the segment renders NOTHING (no empty
  header). An enabled-but-quiet metric simply isn't in the box yet.
- **config `max-rows` is a PER-SECTION row cap** (default 6, as
  shipped). It is NOT the box total.
- **The box total is hard-capped at 15 lines in BOTH modes**
  (MeterMath.SIDEBAR_CEILING): piggyback budget = 15 − EM's lines;
  standalone budget = 15. This is the reading that keeps the combined
  sidebar ≤ 15 everywhere, per the placement law.
- Composition under pressure (LineBuilder.buildSections): start from
  each section's rows capped at max-rows; if header + rows + footer
  exceeds the budget, shed in the house order — footer first, then one
  row at a time from the section currently showing the most rows (ties:
  the LAST such section in canonical order sheds), never below 1 row
  per section while any section still shows 2+; then whole sections
  from the end of canonical order (Healing first); a lone remaining
  section degrades exactly like 0.1.0 (header + rows, header alone at
  budget 1, nothing at budget 0).
- Per-section own-row retention: the viewer's row, if ranked below a
  section's cut, takes that section's last shown slot (0.1.0 rule,
  applied per section).
- The shared footer shows the segment clock plus the viewer's
  per-second rate in the FIRST rendered section — identical to 0.1.0
  in the default single-section case.
- Worked cases (max-rows 6): standalone 1 section = 8 lines (as
  0.1.0); standalone 2 sections = 15 lines exact; standalone 3
  sections = 4 rows per section, no footer (15); piggyback with EM at
  7 lines (budget 8), 3 sections = headers 3 + rows 2/2/1, no footer.

## 3. All-off and no-data: no box at all

- All three metrics OFF (or meter toggled off): the renderer strips
  every meter line and, in standalone mode, hands the player's
  scoreboard back — no sidebar, no empty objective frame. Segments
  keep tracking; `/details` status still reports numbers.
- Enabled with ≥1 metric on but no segment data yet: also no box.
  The box appears with the first tracked event and disappears when
  the segment closes to empty — DESIGN.md §2's "auto-opens in combat"
  made literal (0.1.0 idled with a header+footer frame; 0.2.0 does
  not. Deliberate conformance change, flagged in the BUILD-RECORD).

## 4. Command semantics (exact)

- `/details` (bare) — status (unchanged): build, focus window, mix
  line ("Showing: Damage Done + Healing Done" / "nothing — all
  sections off"), segment clock/state, your current-segment numbers.
- `/details show damage|taken|healing` — TOGGLES that one metric in
  the shown set; the others are untouched. No argument: prints the
  current mix state. This is the menu checkbox command.
- `/details window damage|taken|healing` — quick path: shown set
  becomes EXACTLY that metric and it becomes the focus (0.1.0's
  single-window feel, preserved as an opt-in).
- `/details next` — quick path: focus advances to the next window
  cyclically and the shown set becomes exactly that window.
- `/details on|off|toggle` — the master switch (unchanged); `off`
  also strips the box immediately.
- `/details clear` — discards the shared current segment (unchanged).
- `/details reset` — wipes current segment + overall totals
  (unchanged) AND now persists the wipe immediately.
- `/details reload` — staff (unchanged). `/dmeter` alias (unchanged).

## 5. Placeholders (menu bridge)

PlaceholderAPI expansion, identifier `details`, registered only when
PlaceholderAPI is present (plugin.yml softdepend):

- `%details_show_damage%`, `%details_show_taken%`,
  `%details_show_healing%` → **"ON" / "OFF"**.

Value-format note: the build task text said true/false, but the
consumer spec (MENU-PACKET §0/§2) defines the menu's view-requirements
comparing against 'ON' — the house idiom of %menujoin_status%,
%silkwarnings_status%, %aimassist_status%. The placeholders return
ON/OFF so the drafted menu works as written; the discrepancy is
flagged in the BUILD-RECORD for the parent.

## 6. Empty-segment churn — investigation + fix

Evidence (reconcile/session-log-20261109-pre-restart.log): Mike joined
16:13:55 UTC; segment closes announced at 16:14:28 "(0:00)", 16:14:49
"(0:00)", 16:18:36 "(0:08)" (his real test), 16:19:13 "(0:02)".

Cause, proven from the 0.1.0 code: a segment's announced duration is
lastEventMs − segmentStartMs. Every record* path gates amount > 0
BEFORE touch(), so a literally zero-event segment can never open, and
the tick can only close a segment an attributed event opened. A
"(0:00)" close is therefore a MICRO-segment: one tracked event (or a
sub-second burst) followed by 8 s of quiet — around a login that is a
lone satiated/eating regen tick, a minor fall/settle tick, or one
stray hit. latest.log logs no intake, so which event fired at
16:14:20 / 16:14:41 is not recoverable; the class is certain.

Defect: 0.1.0's announcer prints EVERY close identically, so micro-
segments announce like fights (console noise, and the close line was
the only symptom Mike saw). A second 0-event close path exists:
`/details clear` on a live segment restarts an EMPTY segment that
then "closes" at the timeout with zero events.

Fix (lifecycle semantics untouched — merges, last-segment archival,
and totals stay byte-for-byte 0.1.0): the tracker counts tracked
events per segment and records, at each close, whether the close is
announce-worthy: **announceable = eventCount ≥ 2 OR span ≥ 1000 ms**.
The console announce fires only for announceable closes (config
announce-segments still gates it). Micro closes and post-clear empty
closes merge into overall silently. A 0-event segment now provably
never announces.

## 7. Overall-total persistence (added judgment call — flagged)

0.1.0 kept overall totals in memory only (its BUILD-RECORD names this
a v1 limit). 0.2.0 persists per-player overall totals into data.yml:

- Written by DataFile.save on: segment close, `/details reset`,
  toggle/window/show changes (the existing savePrefs points), and
  onDisable.
- Layout: `overall.<uuid>.{name, damage-done, damage-taken, healing,
  hits}` alongside the existing `players.<uuid>.*` pref block.
- Loaded at startup into the tracker before the first render.
- Totals only: the by-target / by-source breakdown maps stay
  session-scoped (they are drill-down detail, not totals; persisting
  them is unbounded growth for no ruled need).

## 8. Persistence layer

All data.yml reads/writes move into a pure `DataFile` class
(save/load against YamlConfiguration + File) so the full round-trip —
prefs, mix, migration, overall totals — is unit-testable without a
running server. ExcelDetails keeps only the file location + calls.
