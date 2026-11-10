# ExcelDetails 0.1.0 — DESIGN

**Commission:** Mike Reyes, Nov 8, 2026, ~11:22 PM CST (backlog Item 40).
Faithful Details! copy first, adjust on feedback. **LIVE updating is the
core requirement.** Owner chain: board supervisor (build) → parent hand
window (install). Placement law:
`~/workspace/details-meter-20261109/DETAILS-PLACEMENT-DESK-VERIFICATION.md`
(verdict FEASIBLE, EliteMobs 10.9.12 bytecode-verified).

## 1. What it is

A per-player sidebar damage meter in the Details! mold:

- **Windows:** Damage Done (default), Damage Taken, Healing Done.
  `/details window <damage|taken|healing>` or `/details next` cycles.
- **Live:** the sidebar re-renders every 10 ticks (0.5 s) from live
  accumulators — numbers move while the fight is happening, not at the
  end of it. This is the requirement the whole build is shaped around.
- **Segments:** one shared encounter segment (Details' "current
  segment"). It opens on the first tracked combat event after idle and
  closes after `combat-timeout-seconds` (default 8) with no tracked
  event from anyone. On close it is archived as the last segment and
  merged into the overall totals. `/details clear` discards and
  restarts the current segment on the spot (the manual clear Mike
  asked for); `/details reset` also wipes the overall totals.
- **Per-player toggle, default ON:** `/details off|on|toggle`, saved
  per player (data.yml) across relogs. Off removes every meter line
  the player was shown and hands a standalone board back.
- **All combat from v1:** damage done (melee, projectiles by shooter,
  any `EntityDamageByEntityEvent` with a player behind it), damage
  taken (entity sources named; environment sources named by cause —
  fall, fire, drowning…), and healing (see §4 for the v1 attribution
  rule). Bosses need no special case: the Warden is a LivingEntity, so
  a Warden fight fills Damage Done (target "Warden") and the Damage
  Taken window (source "Warden") exactly like any other fight — the
  Item 39/40 requirement.

## 2. Placement — the three hard constraints (verification doc)

1. **Never `setScoreboard()` a private board while EM manages the
   player's board.** Detection: a player's current scoreboard that is
   neither the server main board nor our own standalone board, and
   that carries a SIDEBAR objective, is treated as EM-managed
   (piggyback mode). In piggyback mode we only write entries into
   that board's existing sidebar objective — the board assignment is
   never touched.
2. **Negative scores in piggyback mode.** EM owns scores 0..n−1;
   meter lines take −1, −2, … and render directly underneath EM's
   lowest line. No collision is possible, ever.
3. **Combined box self-caps.** Meter line budget in piggyback mode is
   `15 − (entries on the objective with score ≥ 0)`, floored at 0.
   At budget 0 the meter writes nothing (quest view owns the box).
   Lines are shed in a fixed order — footer first, then the lowest
   ranked rows — the header and the viewer's own row are shed last.
   In standalone mode the budget is the configured row count + header
   + footer (≤ 15 by construction: max-rows default 6, hard cap 12).

Mode transitions are re-evaluated every refresh: party forms → next
refresh piggybacks; party ends (EM restores the old board) → the
restored board is main-or-ours and the meter renders standalone
again. Stale meter entries are removed from whichever board they
were written to before the mode switch completes.

Entry mechanics (both modes): each meter line is a unique color-code
entry string owned by a team named `edt<i>` whose prefix carries the
line text (the ScoreboardUtil pattern EM itself uses, independently
implemented). Our entry strings are prefixed `§7§d` + two hex
color-code digits, a dialect EM's random entries do not use.

## 3. What a line looks like

```
(header — piggyback)  §6§lDetails! §7Damage Done
(row)                 §f1. Mike §e12.5k §7(45%)
(own row)             §a2. Kay §e9.8k §7(35%)
(footer)              §70:42 • you 232/s
```

Standalone mode puts the window title in the objective display name
(`§6§lDetails!`) and the header line names the window instead.
Row text: rank, name, compact amount (1.25k / 3.4M style), share of
the window total. Footer: segment clock + the viewer's own
per-second rate for the shown window.

## 4. Healing attribution (v1 rule, stated plainly)

`EntityRegainHealthEvent` names no healer, and Magic heals land
through it with reason CUSTOM. V1 therefore credits healing to the
player who received it when the regain reason is MAGIC,
MAGIC_REGEN, CUSTOM, or a splash/lingering potion lands on them via
`PotionSplashEvent` bookkeeping — i.e. the Healing window reads as
"healing landed on you, by you or for you." True healer attribution
for Magic spells needs a Magic-side hook and is a named v1.1
candidate, not silently faked here. Damage done + damage taken —
Mike's stated minimum — are exact.

## 5. Commands & permissions

- `/details` — status line (window, segment clock, your numbers).
- `/details on|off|toggle` — per-player, default ON.
- `/details window <damage|taken|healing>`, `/details next`.
- `/details clear` — manual segment clear (any player; clears the
  shared current segment, Details-style).
- `/details reset` — clear + overall wipe.
- `/details reload` — `exceldetails.admin` (op default).
- `exceldetails.use` — default true.

## 6. Fingerprints (FINGERPRINT-STANDARD)

- Layer A: ExcelDetails name/description/authors in plugin.yml;
  READY line names plugin, version, build.
- Layer B: build ID `EXC-DETAILS-0.1.0-WRV-20261109-01-a47f0c` in
  `excel-build.txt` + BuildInfo fragments; shown by `/details`
  status (support surface).
- Layer C (≥4): (1) default pair refresh 10 ticks × timeout 8 s;
  (2) config comment phrasing/section order; (3) team-name dialect
  `edt<i>` + entry prefix `§7§d`; (4) message voice ("Details! is
  already watching this fight." style lines); (5) shed order
  footer→lowest rows→header/own-row-last.
- Layer D: channel WRV (never distributed).

## 7. Non-goals for 0.1.0

No persistence of segments across restarts (toggles persist; meters
are live instruments). No death/overall windows beyond the three
above. No EM config change, no EM API dependency — placement is
scoreboard-state detection only, so an EM update that keeps the
PartySidebar shape keeps working.
