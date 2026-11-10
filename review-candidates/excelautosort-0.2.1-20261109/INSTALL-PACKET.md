# INSTALL PACKET — ExcelAutoSort 0.2.1 (admin per-player surface)

**Status: STAGED, NOT INSTALLED.** The board supervisor files this
packet as PENDING in `~/workspace/wrv-install-queue/MANIFEST.md`
from `MANIFEST-ENTRY-DRAFT.md` in this folder after verification.
Executor: a parent hand window under the standing gates (installs
are parent-run; the failsafe is disabled). Build record:
`~/workspace/autosort-plugin/build-0.2.1/BUILD-RECORD.md`. Spec:
`SPEC.md` §16 in this folder is the design of record.

## What this swap does

A 1-for-1 version increment of the live ExcelAutoSort: 0.2.0 out
(held aside), 0.2.1 in. Everything 0.2.0 does is unchanged (sort
parity engine, per-chest whitelist/blacklist filters, recovery
buffer, Max Organization rent/buy + network view). 0.2.1 adds ONLY
the per-player admin surface Mike's Nov 3, 2026 console admin rule
requires (SPEC §16):

- `/autosort admin status <player>` — sort ON/OFF, Max Organization
  OWNED / RENTED (exact expiry) / LAPSED / NONE, chests, buffer.
- `/autosort admin revoke <player>` — removes the buy entitlement
  AND cancels any rent, at once, persisted in `data/maxorg.yml`.
- `/autosort admin grant <player> max` — no-charge permanent grant
  (ExcelCosmetics grant|revoke symmetry).

All three are console-runnable under `excelautosort.admin`
(default op), take effect live, and never move money.

Expected plugin count after the swap: **UNCHANGED** (1-for-1;
76 at the 0.2.0 install — the executor records the actual count).

## Artifacts

- `~/workspace/autosort-plugin/build-0.2.1/ExcelAutoSort-0.2.1.jar`
  — 159,240 B, SHA-256
  `48a5c0fed431fc9853e0e92ff199b24887ab18485a5febb4132735759617e266`
  (deterministic double-build byte-identical; fingerprint gate
  PASS; build ID `EXC-AUTOSORT-0.2.1-WRV-20261109-01-c36331`).
- Config: the jar's bundled `config.yml` differs from 0.2.0's in
  exactly ONE line — the `messages.usage` admin line now reads
  `admin prune|reload|status|revoke|grant`. **Merge that one line**
  into the live config (live values win; in-place backup first:
  `config.yml.bak-20261109-pre021`). If the merge is skipped the
  plugin still runs (the code fallback carries the same wording);
  the merge is the packet step — do it. Data folder untouched:
  `players.yml`, `chests.yml`, `buffers.yml`, `maxorg.yml` carry
  over as-is; no new data files.
- Menu registry: NO changes. No packet/ files ship with 0.2.1 (the
  0.2.0 `packet/maxorg.yml` entry is already live and untouched).

## Steps

1. **Gates first** (below). Do not start the swap without them.
2. Rename the live jar aside in place:
   `ExcelAutoSort-0.2.0.jar` → append `.held-20261109-pre021`
   (identify it by the `/plugins` list name `ExcelAutoSort`,
   version 0.2.0 — not by guessing; live gate: 154,773 B,
   SHA-256 `6164d943cfaeef498e8966978d9c3255d2352ab81ccc0b36875064d4d0013138`).
3. Place `ExcelAutoSort-0.2.1.jar` in `plugins/`. Verify its panel
   Compute checksum equals the SHA-256 above before booting.
4. Merge the one usage line into the live
   `plugins/ExcelAutoSort/config.yml` per above.
5. Boot-verify lines (all must appear):
   - the awake line naming version 0.2.1 + build
     `EXC-AUTOSORT-0.2.1-WRV-20261109-01-c36331`, with
     `protect sink BOUND`, `gp rights BOUND`, and the segment
     `max-org on (economy BOUND, entitlements N)` — N equals the
     live entitlement count carried over from 0.2.0 (NOT reset);
   - `ExcelProtect audit sink BOUND (stream: items)`;
   - plugin count unchanged from pre-swap; SEVERE 0 attributable
     to ExcelAutoSort;
   - guild seal state unchanged (record the actual line).
6. Console smoke (no state change): `/autosort status` from
   console prints version 0.2.1 + the build ID; then
   `/autosort admin status <a known player>` prints that player's
   sort state + Max Organization state. Do NOT grant or revoke a
   real player's entitlement as a smoke test — the first real use
   is a staff need, and the rig already proved both live (see
   BUILD-RECORD.md).
7. Same-pass surfaces in the install window (standing rule): this
   is a staff-only surface — no player-facing Guide/website copy
   changes are owed. What's New: a staff-facing line is OPTIONAL
   and the executor's call; the broadcaster-tips check is a
   no-change check (nothing player-facing changed). Menus: no
   changes (checked — no new player surface exists to add).

## Gates (standing house discipline)

- **BACKUP-FIRST:** fresh Shockbyte backup before the swap;
  **cap-at-6** — prune the oldest manual backups first so the total
  never exceeds 6; never delete the Daily Backup's newest copy or
  the two newest manual restore points; name prunees in the
  install report.
- **Emptiness:** verified empty (`list` = 0 twice) before shutdown,
  or the maintenance window; panel marker discipline — create
  `~/workspace/wrv-install-queue/WINDOW-IN-PROGRESS.txt` for the
  hand window, delete it at finish, re-check foreign markers
  before Shut Down and before Start (coordination abort on a
  foreign marker < 3h old).
- **Seal note:** this packet touches **neither BossTrial nor
  GuildGate jars**, and no guild content — chapter acceptance does
  NOT re-open. The seal line in boot-verify is a state check only.

## Rollback

Remove `ExcelAutoSort-0.2.1.jar`; restore the held 0.2.0 jar to its
original name; restore `config.yml.bak-20261109-pre021`; restart.
`plugins/ExcelAutoSort/data/maxorg.yml` stays exactly as it is —
0.2.0 reads the same `owned` / `rent-until` dialect (0.2.1 writes
no new fields), so any grant/revoke made under 0.2.1 remains in
force under 0.2.0 and stays visible in Protect's stream
(`autosort_maxorg_admin_grant` / `autosort_maxorg_admin_revoke`).

## Player proofs owed after install (not install gates)

- **Staff proof (Mike, console):** one real
  `/autosort admin status <player>` against a family account
  showing the correct Max Organization state, matching what the
  player sees in `/autosort max`. (The grant/revoke mechanics
  themselves are rig-proven live; no family entitlement should be
  revoked as a "test".)
