# ExcelDowned 0.1.1 — BUILD RECORD (Nov 8, 2026)

Commission: Mike Reyes, Nov 8, 2026, ~10:14 PM CST — "I want buttons.
You know it's our go to way." Player decisions on WRV happen through
button/menu UIs on both platforms, not typed commands alone. Design:
`~/workspace/downed-revive/DESIGN.md` (0.1.0 contract unchanged).
**Status: BUILT + RIG-PROVEN at desk. NOT installed — 0.1.0 is live.**

## Candidate

- `build/ExcelDowned/ExcelDowned-0.1.1.jar` — **47,199 B**,
  sha256 `331e3b103ef07da6f81455f212d56c849916e1105232d0bb0b5bd472ae871d2f`
  (copies also at `build/ExcelDowned-0.1.1.jar` + `-repro.jar`)
- Deterministic double-build byte-identical; fingerprint gate PASS
  (build **EXC-DOWNED-0.1.1-WRV-20261108-02-d0wn3d**, tag `d0wn3d`
  carried forward from 0.1.0 — Layer B in-jar, semantic marker lineage
  unbroken per the FINGERPRINT-STANDARD persistence law).
- Gates on the live jar: ExcelDowned-0.1.0.jar, sha256 `0347d1a0…`
  (see PACKET-NOTES.md).

## What changed (and what did not)

**No 0.1.0 gameplay values or paths changed** — bleed-out 300 s,
downed factor 0.25, two-step discipline with the same 10 s arm window
(`giveup-window-seconds`), exactly-once death latch, records stream,
action bar text: all untouched. `config.yml` is byte-identical to
0.1.0 (no new keys). The delta is the /giveup CONFIRM SURFACE:

- **First `/giveup` while downed now opens a confirm UI** instead of
  only arming in chat. Opening the UI arms the session
  (`DownedSession.armGiveUp`) — the SAME arm state the typed path
  uses, so menu state and typed-arm state are one state: a typed
  `/giveup` while the menu stands armed confirms, and a UI "Let go"
  after a chat arm confirms.
- **Java:** a 9-slot inventory menu — slot 2 red concrete
  "✝ LET GO — die now", slot 6 green concrete "✚ Keep fighting",
  black-glass filler, title "✝ Let go?".
- **Bedrock:** the Cumulus ModalForm via Geyser — title "✝ You are
  DOWN", body with the consequence + live bleed-out clock, buttons
  "✝ Let go" / "Keep fighting".
- **"Let go"** confirms through `DownedSession.confirmGiveUp` → the
  existing exactly-once death path (real drops, one death, `give_up`
  record). **"Keep fighting", closing the menu/form, or the arm
  window lapsing** cancels/disarm — play continues.
- **Stale UI is inert:** revive / bleed-out / give-up / disconnect
  invalidate the UI (ticket dropped, Java menu closed by
  `ExcelDowned.revive/finalizeDeath/noteExternalDeath` +
  PlayerQuit hooks). A late "Let go" finds no session (silent no-op)
  or a lapsed arm (`confirmGiveUp` refuses — and never re-arms, so a
  stale click can't prime a surprise typed death either).
- **Fallback:** if the Bedrock form API is unavailable at runtime
  (Geyser absent/disabled, connection null, send fails), the player
  gets the 0.1.0 chat arm/confirm verbatim — the typed path never
  breaks. Downed restrictions stay; opening/interacting with THIS
  menu is the one sanctioned inventory exception
  (`DownedListener.onOpen` exempts `GiveUpHolder` inventories).

## Sibling patterns followed

- **Java menu — ExcelMenu** (`excelmenu/src/.../bukkit/`): a custom
  `InventoryHolder` (`GiveUpHolder`, after `MenuHolder`) carries the
  slot→action truth; the click listener cancels every click/drag in
  the view and resolves the action from the holder's map, never from
  the ItemStack; bottom-inventory clicks ignored (`MenuListener`).
- **Bedrock form — ExcelBedrockMenu 1.5.29** (`GeyserForms.java`):
  reflection-only Cumulus access through Geyser's own classloader (no
  compile-time Geyser dependency, no loader-constraint risk),
  `GeyserApi.isBedrockPlayer` routing, `ModalForm` with
  `validResultHandler` + `closedOrInvalidResultHandler` (dismissal =
  cancel). Carried over as `BedrockForms.java`, minus ClickStats.
- **Confirmation lifecycle — ExcelBedrockMenu's
  `TeleportConfirmationGate`**: a per-player identity ticket; a form
  answer is honored only while its ticket is still the player's
  current ticket; state changes invalidate. (`GiveUpUi.formTickets`.)
- **Platform routing** is a pure function (`GiveUpRoute.route`):
  Java → menu; Bedrock + API → form; Bedrock − API → chat fallback.

## Tests

**Unit (JUnit, `DownedTest`): 17/17** — the 8 original 0.1.0
state-machine tests (deadline math, scaled window eating, TW
composition, typed two-step incl. stale-arm re-arm, exactly-once
latch, persistence round-trip) plus 9 new: UI-open arms without
confirming; Let-go in-window confirms (death latch still consumed
exactly once); Let-go after lapse is stale and does NOT re-arm;
Let-go unarmed is stale; Keep-fighting disarm cancels (later confirm
and single typed /giveup both safe); menu-arm → typed /giveup
confirms; typed-arm → UI confirm confirms; disarm never un-confirms;
the 4-cell routing matrix.

**Desk rig (Paper 26.3 + Magic 11.2.5, NMS fake players, fresh
world): 43 PASS / 0 FAIL** (`build/rig/rig-console8.log`) — all 26
original 0.1.0 checks (gate, down state, deaggro, window eating,
restrictions, CastEvent revive, custom action, typed give-up death +
drops, bleed-out, relog persistence) plus 17 new menu checks:

- menu-open-arms — /giveup opens the menu, session armed, no death
- menu-keep-fighting-cancels — still downed, disarmed, menu closed
- menu-letgo-dies-once / menu-letgo-drops-real — exactly 1 death,
  drops on the ground, `give_up` record written for the menu path
- menu-close-cancels — closeInventory without choosing disarms
- menu-reopen-after-cancel — a fresh /giveup re-arms/reopens, no kill
- revive-closes-menu / stale-letgo-after-revive-noop — revive closes
  the menu via invalidate; the stale click on the saved view: no
  death, health still 10.0, no error
- bleedout-with-menu-open-one-death — 8 s window, menu open: exactly
  1 death, `bleed_out` record, state cleared
- Bonus this run: the real Magic cast attempt returned true and
  revived end-to-end (healer cast() -> true, health 10.0).

Rig hygiene note: run 7 (`rig-console7.log`, 40 PASS / 3 FAIL) failed
only the three party-dependent revive checks — my fresh-state reset
had wiped the rig's ExcelDowned config, losing its rig-only
`party.fallback-all-online: true` (the healer counts as party only
with it; there is no ExcelTogether on the rig). Restored for run 8;
all menu checks had already passed in run 7. The shipped jar's
default config keeps `fallback-all-online: false` — production is
unaffected.

**Not desk-provable:** a live Bedrock form round-trip (the rig's fake
players are Java-edition NMS players; no Geyser on the rig). The form
code is byte-pattern-identical to the live-proven ExcelBedrockMenu
1.5.29 route and compiles/runs with Geyser absent (fallback proven by
the routing unit tests); the Bedrock form proof is a player proof in
the install packet, as specced.

## Boot-verify lines (for the install packet)

- `ExcelDowned: custom Magic action 'Resurrect' registered.`
- `ExcelDowned 0.1.1 ready — build EXC-DOWNED-0.1.1-WRV-20261108-02-d0wn3d; sessions restored: N; bleed-out 300s; downed factor 0.25`
  (both seen verbatim in the rig boot, rig-console8.log)
