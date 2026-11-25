# INSTALL PACKET — ExcelBossTrial 0.5.8 (grantclaim emloot form)

- Lint: 4,360 B / max line 73 / non-ASCII 45
- **Packet:** ~/workspace/bosstrial-emloot-20261124/INSTALL-PACKET.md
- **Build record:** ~/workspace/bosstrial-emloot-20261124/BUILD-RECORD.md
  (base gate, change list, suite, full rig proof, the
  soulbind-on-offline-builds note)
- **Surfaces:** ~/workspace/bosstrial-emloot-20261124/SURFACES.md
  (What's New TEXT entry draft, filed as a companion
  placement at install)

## Live-premise receipts

- Live jar `plugins/ExcelBossTrial-0.4.1.jar` = 472,617 B /
  `94cf076da9cbb489fa4ded38cd9b7599b4d72e36724f88017e0540ef7b01f899`
  — byte-identical to the 0.5.7.1 source of record
  (~/workspace/bosstrial0571-drive/), verified from the live
  pull by the dagger lane Nov 24, 2026, and reproduced
  byte-identically from the unmodified source tree at this
  build's base gate.
- Live config already carries `commands.grantclaim-enabled:
  true` (dagger lane, live config read Nov 24, 2026). This
  packet changes NO config.

## Candidate

- `~/workspace/bosstrial-emloot-20261124/ExcelBossTrial-0.5.8.jar`
  — 481,720 B / SHA-256
  `119ecbad189376e12f235c6c9c2a381033579ce0dfbc0497c4e3aa91c89f995b`;
  build `EXC-BOSSTRIAL-0.5.8-WRV-20261124-01-2c5e05`;
  suite 2,890 checks / 104 harnesses, 0 failures;
  deterministic double-build byte-identical; scratch-rig
  proof at the full level (offline credit → ledger
  read-back → bot join → claim → delivered, plus an
  EM-absent refusal boot) — BUILD-RECORD.md.

## Live-jar gate (STOP on mismatch)

- Live file `plugins/ExcelBossTrial-0.4.1.jar` must hash to
  472,617 B / `94cf076d…` at install or STOP and re-base.

## Scope

- GATED jar swap only, in place at the live filename
  `plugins/ExcelBossTrial-0.4.1.jar` (standing filename
  convention; content becomes 0.5.8). Outgoing jar MOVED
  aside as `ExcelBossTrial-0.4.1.jar.bak-20261124-pre058`
  (never co-resident). No config placed or edited; no data
  files touched — the claims ledger format is unchanged and
  existing claims carry over untouched.
- What changes: ONE new console/admin form,
  `/bosstrial grantclaim <player> emloot <loot-file>
  [count]`, crediting a genuine EliteMobs custom item
  (built by EM's own CustomItem construction, the
  `em loot give` calls) into a player's /bossrewards
  claims through the existing TeamClaims.grantClaim path —
  works for offline players. Refusals (EliteMobs absent,
  unknown loot file, bad count/file word) store nothing.
- What does NOT change: every existing grantclaim form,
  fight/verdict/reward behaviour, the ledger, the UI.
  Rollback: restore the held-aside jar at the same filename
  and restart; claims written by 0.5.8 are ordinary claims
  and stay valid under 0.5.7.1.

## Gates

- Marker discipline with re-checks before Shut Down and
  before Start; BACKUP FIRST + cap-at-6 (name prunees;
  never the Daily newest or the two newest manuals);
  emptiness `list` = 0/10 twice.

## Boot-verify

- `Enabling ExcelBossTrial v0.5.8` + Build line
  `EXC-BOSSTRIAL-0.5.8-WRV-20261124-01-2c5e05`; SEVERE 0;
  guild state unchanged.
- Form smoke (stores nothing): console
  `bosstrial grantclaim NobodyHere emloot
  no_such_loot_file.yml` → refusal "No such EliteMobs loot
  file" + the REFUSED (UNKNOWN_LOOT) audit line. This
  proves the form and the EM registry path are live.
- Write the marker on success:
  ~/workspace/bosstrial-emloot-20261124/INSTALLED.txt

## First use (parent/Mike step, NOT part of the swap)

- The Jester dagger credit this build exists for:
  `bosstrial grantclaim Jester_Turtle emloot
  primis_ice_village_alanus_dagger.yml` — then the prepared
  Discord DM goes out (dagger lane has the text) and the
  login alert hook covers the in-game half. Two notes:
  (1) if Jester is ONLINE when the command runs, EM builds
  with him as recipient and the dagger is soulbound to him
  exactly as EM's own give; if he is offline, the dagger is
  genuine but unbound ("Not Soulbound!" lore) — see
  BUILD-RECORD.md. (2) The alert hook's credit guard
  cannot currently see real claim payloads (gzip — see
  BUILD-RECORD.md); repair it before relying on the alert,
  or verify the credit by ledger read-back instead.
- Proof owed after first use: Jester claims the dagger at
  his next login (ledger claim flips to DELIVERED).
