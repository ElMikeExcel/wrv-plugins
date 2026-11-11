# RIG RESULTS — ExcelBuildMode 1.3.4 (Nov 10, 2026)

Rig: copy of the 1.3.3 rig server (`~/workspace/buildmode-133-build/rig-1.3.3/server/`)
with the jar swapped to ExcelBuildMode-1.3.4.jar (SHA-256 verified
39d86f3b…6d3 in place before boot). Paper 26.2-123 + WorldEdit 7.4.5,
JDK 25, offline mode, flat world, rig config unchanged (synthetic
eligible builder 00000000-0000-0000-0000-000000000043,
temporary-worldedit on). Full log: `boot.log` in this folder. Server
reached Done, was exercised from the console, and stopped cleanly
(exit 0).

## Boot

- "Enabling ExcelBuildMode v1.3.4" present; plugin list shows
  ExcelBuildMode (1.3.4).
- The 1.3.4 PILOT banner printed in full, including BOTH delta lines:
  the 1.3.3 line ("console admin - /buildmode admin status <player>
  and /buildmode admin off <player> (excelbuildmode.admin, console
  allowed)…") and the 1.3.4 line ("combined rebuild - the
  reviewed-builder exemption rides alongside the console admin
  surface: a holder of excelbuildmode.reviewed keeps standing
  WorldEdit permissions and still uses Build Mode (the
  outside-WorldEdit entry refusal and the exit residual-permission
  probe are skipped)").
- SEVERE: 0 in the whole boot + exercise + shutdown.
- The only ERROR lines are Paper's own sandbox-network noise
  (yggdrasil public-key fetch, PaperVersionFetcher) — the same lines
  the 1.3.2/1.3.3 rigs produced; none is attributable to
  ExcelBuildMode.
- Clean stop: "Disabling ExcelBuildMode v1.3.4", exit code 0.

## Console exercise (the 1.3.3 set, re-run verbatim — all from the
## server console)

| Console input | Observed reply (boot.log) |
|---|---|
| `buildmode admin status NobodyHereXYZ` | "Build Mode admin: no player named 'NobodyHereXYZ' is online and no Build Mode record exists for that name." |
| `buildmode admin off NobodyHereXYZ` | "Build Mode admin: no player named 'NobodyHereXYZ' is online and no Build Mode session record exists for that name. Nothing was changed." |
| `buildmode admin` | "Usage: /buildmode admin status <player> \| /buildmode admin off <player>" |
| `buildmode admin status` (no target) | same usage line |
| `buildmode admin bogus Marvin` | same usage line |
| `buildmode admin off` (no target) | same usage line |
| `buildmode status` (player path from console) | "Use /buildmode in game. It only changes your own mode." — the pre-1.3.3 rejection, unchanged: the console allowance covers the admin branch ONLY |

Result: 7/7 console checks behaved exactly as specified — the console
admin surface survived the combined rebuild unchanged.

## Rig limitations, stated plainly

- The rig has no real Minecraft client, so no live player session
  exists to exercise the reviewed entry/exit behavior or a live
  force-restore. Those legs are proven by: (1) ReviewedTests' truth
  tables for the recovered policy helpers; (2) the control-build
  bytecode proof that the recovered helpers + call sites compile to
  the preserved reviewed jar's exact instruction streams
  (REVIEWED-DELTA-RECOVERY.md); (3) AdminTests' exercised restore
  through the real SessionCoordinator.recover(); and (4) the install
  packet's post-install player proofs on the live server (reviewed
  entry/exit with standing WorldEdit + the console rescue).
- Permission denial for a non-admin PLAYER sender is proven at the
  policy layer by AdminTests (gate + routing matrix); no client
  exists in the rig to send as one.
