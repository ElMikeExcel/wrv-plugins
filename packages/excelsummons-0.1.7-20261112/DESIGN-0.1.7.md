# ExcelSummons 0.1.7 — the compass Summons page (design summary)

Commissioned by Mike, Nov 12, 2026, 5:55 PM CST ("Yes, add a
Summons page to the compass menu"), refined the same evening
(~6:01 PM): adopted pets get their OWN sub-page. Full commission:
`~/workspace/summons-menu-017-build/COMMISSION.md`.

## What 0.1.7 adds (and nothing else)

- `/summons menu` for players. Java: inventory GUI. Bedrock:
  Cumulus simple form (house reflection pattern — PlatformDetect
  + BedrockForms carried from ExcelTrainingDummy; a form failure
  falls back to the chat list, never to silence).
- MAIN page (MenuModel): CAST summons only, one row per record
  with the same facts as `/summons list` — the row's line is
  Nameplate.listLine over the identical inputs listLineFor
  computes (spell, branch, time left / persistent, HP or
  "not loaded", the APPLIED hold from ReservePolicy) — plus the
  total applied held percent (ReservePolicy.appliedPercent, the
  list's own total) and buttons for the three EXISTING dismiss
  scopes (mount only / cast pets / all), routed through
  SummonsListener.dismiss and reported with the command's own
  result text (MenuModel.resultMessage).
- ADOPTED sub-page: reached by the main page's "Adopted pets (N)"
  button. Every adopted row carries the indicators — ADOPTED
  badge, "holds no mana" — plus type/HP facts. PER-PET DISMISS,
  CONFIRM-GATED (Mike's ruling, Nov 12, 2026, ~6:05 PM,
  superseding the view-only default of an hour earlier): each
  pet's own dismiss button opens a confirm step naming that pet
  and warning that dismissal permanently removes the real
  tamed/bred animal (not re-summonable); only the confirm action
  dismisses, and it dismisses exactly that one record through
  the new narrow path `SummonsListener.dismissOneAdopted`
  (owner + entityId, origin-checked by AdoptedDismissPolicy,
  INFO audit line naming player + pet spell/type key). There is
  NO "dismiss all adopted" control anywhere. The command scopes
  are unchanged — AdminPolicy.dismissTargets is byte-identical
  to 0.1.6 and still never selects an adopted pet — so this
  confirm-gated menu flow is the only adopted-dismiss door.
- Pagination: Java pages of 45 entries (54-slot view, bottom row
  is controls) on BOTH views, index clamped; Bedrock form bodies
  carry every line of their view (no pagination in text).
- Compass door: ExcelMenu `entries/summons.yml` (staged with the
  packet; group `me`, after Wand controls; requires-plugin
  ExcelSummons; action closes the menu and runs `summons menu`).

## What 0.1.7 does NOT change

- Zero change to 0.1.6 mana/reserve behavior: ReservePolicy,
  CapPolicy, AdminPolicy, LifetimePolicy, TargetRules are
  byte-identical to 0.1.6. config.yml is byte-identical (no menu
  title setting was needed — titles are constants, as in
  TrainingDummy). `/summons list`, `/summons dismiss`, and the
  admin surface behave exactly as before; the only command-
  surface change is the added `menu` subcommand and its name in
  the two usage lines.
- No per-entity dismissal is invented; the menu's buttons are
  the three scopes the commands have always had.

## Shape

- `MenuModel` (pure, harness-tested): the page split, rendering,
  pagination, button/scope table, result text. Its only
  Bukkit-dependent input is each entity's HP text, passed in by
  `ExcelSummons.menuPageFor` using listLineFor's own expression.
- `SummonsMenus` + `SummonsMenuListener`: Bukkit mechanics only
  (DummyMenus/DummyListener pattern).
