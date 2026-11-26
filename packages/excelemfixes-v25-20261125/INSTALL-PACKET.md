# INSTALL PACKET — ExcelEMFixes v25 (bedrock-abilities module; ExcelEMAbilityBedrockBridge fold, Wave 4)

Build: ExcelEMFixes v25 (Wink Reyes Valley), built Nov 25, 2026.
Ruling: D245 (Mike, Nov 25, 2026) — consolidation programme
Wave 4: ExcelEMAbilityBedrockBridge 0.2.0 folds into
ExcelEMFixes as the toggleable `bedrock-abilities` module;
the standalone jar retires in the SAME window, only after
module proof (done — see RECEIPTS.md: unit suites 2,083/0
pack + 216/216 bridge checks incl. 26/26 mapping against the
live EM 10.9.16 jar; rig Phase A form [Divine Steed, Provoke,
Repulse, Close] with all three slots fired through the
module path, EM evidence 2 -> 5; Phases B/C probe + absent
stand-downs clean; Phase D toggle-off clean; SEVERE 0 in
every phase).
Lint: 4689 B / max line 104 / non-ASCII 63 (P3 targets met).
RECEIPTS.md: `~/workspace/excelemfixes-bedrock-abilities-20261125/packet-v25/RECEIPTS.md`
— sha256 `4df627c0fefc65f020174a6354969b34005ac4f24efe65b5c98403a0b85c6e50`.

## What changes

- ExcelEMFixes v24 → v25: adds ONE module,
  `bedrock-abilities` (27 modules total). Player-facing
  behavior is IDENTICAL to the live standalone 0.2.0: the
  `/abilities` command name, its `/classabilities` alias,
  its status shape, its
  exceleitemobsabilitybedrockbridge.admin permission node,
  and the Bedrock Cumulus form's shape.
- The standalone `ExcelEMAbilityBedrockBridge-0.2.0.jar`
  (the LIVE filename — see the gate below) is REMOVED in
  this window (kept in the backup set as rollback). Its two
  data folders hold only a config whose single key
  (debug-log-fires: false) equals the module's shipped
  default; the folders are left in place, untouched.
- No data migration: the module keeps no state and writes
  no files; EliteMobs holds all class data.
- EM compile-pin: the standalone's MD5 build pin is
  REPLACED by the pack's guarded-seam + boot-probe posture
  (programme Wave 4 decision; RECEIPTS.md has the receipt).

## Live-premise gate (verify BEFORE any step; abort on mismatch)

- `plugins/ExcelEMFixes-v24.jar` — 626,284 B — sha256
  `372c41031c8336a78d1113a1e9778c0fbcadd3afbb15c1840e16c56b0e042df9`
  (receipt Nov 25, 2026 ~11:17 PM CT, RECEIPTS.md; re-pull
  at the window).
- `plugins/ExcelEMFixes/config.yml` — 91,671 B — sha256
  `5a4a9468808a540409f1fee66ced8aab78c36acebeae5d25abd009ad59b63a01`
  (same receipt). deploy-config.yml in this packet is this
  file + only the bedrock-abilities section (+56 / -0).
- `plugins/ExcelEMAbilityBedrockBridge-0.2.0.jar` —
  31,990 B — sha256
  `9c4158212c7443f49be5371784aab229855381cc1f8ba46359a7637b7a32a4c1`
  (same receipt; this is the jar the window removes).
- `plugins/EliteMobs-10.9.10.jar` — 14,573,497 B — sha256
  `e38dad008037184e11e5b51dd6b60651419e9c1b29297f3372ec23d54e58705b`.
  Its plugin.yml reads version 10.9.16 (updated in place
  under the old filename) — gate on the hash, not the name.

## Candidate

- `packet-v25/ExcelEMFixes-v25.jar` — 657,130 B — sha256
  `7641a8acec174fffdfef07d6d5bd6bf9cbf49f28e73e41918b2ecbf725aaa676`
  (deterministic: two builds byte-identical).
- `packet-v25/deploy-config.yml` — 94,405 B — sha256
  `a34903709ba2dfb3a882a01ba17356b2bbc94b09d80411e19fbf673597e9e13e`.

## Window steps (standing discipline: marker, BACKUP FIRST
## + cap-at-6, emptiness gate, one panel actor)

1. Verify the live-premise gate above from a fresh pull.
2. Backup (jar + config + the standalone jar being
   removed), then swap: ExcelEMFixes-v25.jar in,
   ExcelEMFixes-v24.jar out, deploy-config.yml in,
   ExcelEMAbilityBedrockBridge-0.2.0.jar OUT (into the
   backup set as rollback).
3. Boot verify: "ExcelEMFixes v25 ready" with
   bedrock-abilities in the module list; standby line then
   "Bedrock ability bridge online" once ACS is up;
   `/abilities status` from console answers UP/READY;
   SEVERE 0 attributable.
4. Surfaces pass per SURFACES.md (What's New x2 + website
   row; Guide hibernated noted; tips/menus checked).

## Proof owed after install

- A live Bedrock ACS cast via the G1 harness (a real
  Bedrock client taps an ability in /abilities inside EM
  content and EM's evidence shows the cast) — the wave's
  closing proof per the programme. The Cumulus transport
  round-trip on a real Bedrock client is the one leg no rig
  can prove (headless players have no Geyser connection).
- Upstream: after that live proof, an upstream note to
  MagmaGuy naming ExcelEMFixes v25 (the Bedrock ACS input
  gap is a genuine EM platform defect; standing testing
  bar — live proof first, never rig-only).

## Marker on install

`~/workspace/excelemfixes-bedrock-abilities-20261125/packet-v25/INSTALLED.txt`
