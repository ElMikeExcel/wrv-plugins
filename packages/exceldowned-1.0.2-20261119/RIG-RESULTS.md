# RIG RESULTS - ExcelDowned 1.0.2

Load check (D122): VM-local Paper 26.2 rig PASS, Nov 19, 2026
(build worker; full detail in BUILD-RECORD.md). The Homebase
mirror was NOT used: it sat in found state under the
idle-queue calibration lane that night; the VM-local rig is
the same substitute M1's G-4 used. Live boot-verify at the
install window also PASS (below).

## VM-local rig (pre-install)

Throwaway Paper 26.2-123 server (primed runtime copy; rig
discarded after), exactly three plugins co-resident with
fresh data folders: ExcelItems-0.1.0.jar (live pin 85dbb9ea…)
+ Magic-11.2.5.jar + candidate ExcelDowned-1.0.2.jar.

- Done (48.600s); SEVERE count 0 (whole boot log).
- Enabling ExcelItems v0.1.0 line 175 BEFORE Enabling
  ExcelDowned v1.0.2 line 177 - hard-depend order proven;
  "ExcelItems enabled: 4 custom item(s) registered, next
  free id 5."
- "ExcelDowned: registered revive behavior for ExcelItems
  ID 4 (exceldowned:revive-potion)." present.
- Ready line: "ExcelDowned 1.0.2 ready - build
  EXC-DOWNED-1.0.2-WRV-20261119-01-d0wn3d; sessions restored:
  0; bleed-out 300s; downed factor 0.25".
- Rig-environment WARNs only (no Vault on the rig -> the
  designed "purchases disabled" notice; Magic spell-template
  self-check notices on the bare rig) - the same bare-rig
  notice class as M1's rig, not build defects.
- Log preserved: work/vm-rig-boot.log in the build folder,
  SHA-256 6ac29f483fb6549ff0a1528933691e0275d6257bde0d47bc90e3dfe78c9f0312.

## Live boot-verify (install window, Nov 19, 2026)

wrv-install-failsafe-v2 run d94d7abf-efa1-48f5-b8c4-cf7a1981630c,
panel Start 2:37:08 AM CST:

- Done (24.305s) at 2:37:35 AM; SEVERE 0.
- [ExcelItems] Enabling ExcelItems v0.1.0 x1 (2:37:23);
  "ExcelItems enabled: 4 custom item(s) registered, next
  free id 5." x1.
- [ExcelDowned] Enabling ExcelDowned v1.0.2 x1 (2:37:27,
  AFTER ExcelItems); "ExcelDowned: registered revive
  behavior for ExcelItems ID 4 (exceldowned:revive-potion)."
  x1; ready line carries build
  EXC-DOWNED-1.0.2-WRV-20261119-01-d0wn3d.
- "purchase journal unavailable" ABSENT (count 0).
- [ExcelGuildGate] "ExcelGuildGate enabled: 5 of 5 seals
  broken, guild OPEN".
- Console: excelitems status -> "ExcelItems: 4 item(s)
  registered, next free id 5."; list -> 0 of 10 players.
- One [Error] line present, pre-existing and unrelated:
  [Essentials] "You are running an unsupported server
  version!" (recorded as observed in the run record, not a
  gate input).
- Placed bytes verified by pull-back before boot:
  ExcelDowned-1.0.2.jar 139,867 B / 8237327f… EXACT.
- Run record: ~/workspace/wrv-install-queue/runs/20261119-0204-failsafe/RUN-RECORD.md
