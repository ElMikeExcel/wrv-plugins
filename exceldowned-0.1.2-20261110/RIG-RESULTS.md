# RIG RESULTS — ExcelDowned 0.1.2 (Nov 10, 2026)

Rig: copy of the 0.1.1 rig server (`~/workspace/downed-revive/build/rig/server/`)
with the jar swapped to ExcelDowned-0.1.2.jar (SHA-256 verified
2320e812…9396 before boot) and the driver swapped to the new
AdminRig (driver-src in this folder). Paper 26.3-235 + Magic
11.2.5, JDK 25, offline mode, rig world; the rig's ExcelDowned
config is the 0.1.1 rig config (incl. its rig-only
`party.fallback-all-online: true` — production config is untouched
and byte-identical to 0.1.1). Full log: `boot.log` in this folder.
Server reached Done (32.8 s), was exercised from the console, and
stopped cleanly ("Disabling ExcelDowned v0.1.2").

## Boot

- "Enabling ExcelDowned v0.1.2" present; "custom Magic action
  'Resurrect' registered." present.
- Ready line verbatim: "ExcelDowned 0.1.2 ready — build
  EXC-DOWNED-0.1.2-WRV-20261110-01-d0wn3d; sessions restored: 0;
  bleed-out 300s; downed factor 0.25" — the Mike-ruled values are
  unchanged on the boot surface.
- SEVERE: 0 in the whole boot + exercise + shutdown.

## Behavior exercise — 17/17 PASS, 0 FAIL (RIGOBS tally in boot.log)

All admin acts were dispatched through Bukkit's REAL console
sender (`Bukkit.dispatchCommand(Bukkit.getConsoleSender(), …)`)
against NMS fake players driven into the downed state by lethal
zombie damage, exactly the 0.1.1 rig recipe.

| Check | Result |
|---|---|
| admin-revive-setup-downed | PASS — AdmPatA downed by lethal hits, no death |
| console-revive-clears-state | PASS — `downed revive AdmPatA` from console: no longer downed |
| console-revive-health-50pct | PASS — health = 10.0 (50% of 20, the spell path's fraction) |
| console-revive-no-death | PASS — deaths = 0, slowness cleared: a revive, not a death |
| admin-clear-setup-downed | PASS — AdmPatB downed |
| console-clear-clears-state | PASS — `downed clear AdmPatB` from console: no longer downed |
| console-clear-restores-nothing | PASS — health still 1.0, deaths = 0, slowness cleared (clear heals nothing, kills nothing) |
| redundant-and-unknown-are-noops | PASS — revive/clear on a standing player, revive/clear on `NobodyHereXYZ`, bare `downed revive` / `downed clear`, `downed revive AdmPatB extra`: target untouched at 20.0 |
| offline-setup-downed + offline-session-is-durable | PASS — AdmPatC's session persists in the registry after logout |
| offline-revive-refuses-and-changes-nothing | PASS — console revive of the offline session leaves it standing (message names the remaining 4:42 clock and points at clear) |
| offline-clear-removes-the-record | PASS — console clear removes the durable session |
| offline-cleared-player-returns-standing | PASS — AdmPatC rejoins not downed |
| denial-setup-downed | PASS — AdmPatE downed |
| nonadmin-player-revive-denied | PASS — a non-op fake player (no `exceldowned.admin`) dispatches `downed revive AdmPatE`: AdmPatE stays downed |
| nonadmin-player-clear-denied | PASS — same for `downed clear AdmPatE` |
| console-revive-after-denials | PASS — console revive then resolves AdmPatE at 50% |

Console message lines verified verbatim in boot.log: the legacy
status line ("AdmPatA: DOWNED, bleed out in 4:56 (cause
entity_attack)"), the revive line ("Downed admin: AdmPatA revived
— downed state resolved at 50% health, no death."), the clear
lines (online + "offline record" forms), the not-downed lines, the
unknown-name line, the usage line ×3, the offline-revive refusal
line, the INFO audit lines ("Downed admin revive by CONSOLE for
…", "Downed admin clear by CONSOLE for …"), and "Only a player can
give up." — the player path still rejects the console (the
allowance is admin-branch only). The plugin's records stream
carries `admin_revive` ×2 and `admin_clear` ×2 with
`"actor":"CONSOLE"` and no fake spell/death records.

## Rig limitations, stated plainly

- The non-admin denial is proven by STATE (the target stayed
  downed across both attempts): a fake player's chat messages are
  not echoed to the server log, so the denial message text itself
  is proven at the policy layer by DownedTest (denial routing) and
  read in DownedCommands, not observed in boot.log.
- No Bedrock form round-trip (the rig's fake players are
  Java-edition NMS players) — unchanged from 0.1.1; 0.1.2 does not
  touch the /giveup UI surface at all.
- The rig config's `party.fallback-all-online: true` is rig-only
  (inherited from the 0.1.1 rig); the shipped config.yml is
  byte-identical to 0.1.1 with the flag false.
