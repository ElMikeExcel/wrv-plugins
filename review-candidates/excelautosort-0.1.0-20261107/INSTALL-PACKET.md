# INSTALL PACKET — ExcelAutoSort 0.1.0 (the AutoSort swap)

**Status: STAGED, NOT INSTALLED.** Filed as PENDING in
`~/workspace/wrv-install-queue/MANIFEST.md`. Executor: the install
failsafe / a hand window under the standing gates. Build record:
`~/workspace/autosort-plugin/BUILD-RECORD.md`. Spec:
`~/workspace/autosort-plugin/SPEC.md`.

## What this swap does

ExcelAutoSort 0.1.0 is a brand-new house plugin (Mike's decision,
Nov 6, 2026: "we aren't going to bridge we will make our own auto
sort plugin"). One window replaces **two** live jars with it:

| Out (rename aside, never delete) | In |
|---|---|
| `ExcelAutoSortBridge` jar (the %autosort_status% bridge) | `ExcelAutoSort-0.1.0.jar` → `plugins/` |
| nothub `AutoSort` jar (the third-party sort engine, 0.1.17) | |

Expected plugin count after the swap: **76** (77 live − 2 out + 1 in).

## Artifacts

- `~/workspace/autosort-plugin/ExcelAutoSort-0.1.0.jar` — 104,136 B,
  SHA-256 `00d01234b0a7d06b5c01dce3805f9f5736f0cf2f026512234e29c29c868dc36d`
  (deterministic double-build byte-identical; fingerprint gate PASS;
  build ID `EXC-AUTOSORT-0.1.0-WRV-20261106-04-f96f93`).
- Menu entry files in `~/workspace/autosort-plugin/packet/`
  (`autosort.yml`, `chestfilter.yml`, `filterrecover.yml`).

## Steps

1. **Gates first** (below). Do not start the swap without them.
2. Count the UUID lines in `plugins/autosort/active.txt` and note
   the number — step 6 verifies against it.
3. Rename the two outgoing jars aside in place:
   `ExcelAutoSortBridge….jar` → append `.held-20261106-pre-autosort`,
   the nothub `AutoSort….jar` → same suffix. (Identify them by the
   `/plugins` list names `ExcelAutoSortBridge` and `AutoSort`, not by
   guessing filenames.)
4. Place `ExcelAutoSort-0.1.0.jar` in `plugins/`. Verify its panel
   Compute checksum equals the SHA-256 above before booting.
5. Menu registry (ExcelMenu entries directory):
   - Replace `autosort.yml` with `packet/autosort.yml` — the diff is
     EXACTLY two fields and nothing else:
     `requires-plugin: AutoSort` → `ExcelAutoSort`;
     `requires-permission: autosort.toggle` → `excelautosort.toggle`.
   - Add `packet/chestfilter.yml` (new; order 15 / bedrock 205).
   - Add `packet/filterrecover.yml` (new; place beside the Lost &
     Found entry; the file's proposed order is 16 / bedrock 206 —
     renumber if it collides, keeping the two recovery entries
     adjacent).
6. **Import verification (first boot):** the console must show
   `ExcelAutoSort: imported N auto-sort states from AutoSort
   (plugins/autosort/active.txt)` with N equal to the count from
   step 2. The import is read-only — `active.txt` is never modified;
   the one-time guard is the existence of
   `plugins/ExcelAutoSort/data/players.yml`, which the import
   creates. If the line is absent or N mismatches, STOP and report —
   do not re-run the import by hand.
7. Boot-verify lines (all must appear):
   - the awake line naming build `EXC-AUTOSORT-0.1.0-WRV-20261106-04-f96f93`;
   - `ExcelProtect audit sink BOUND (stream: items)`;
   - `gp rights BOUND` in the awake line;
   - plugin count 76; SEVERE 0 attributable to ExcelAutoSort;
   - guild seal state unchanged: **LOCKED 3 of 5**.
8. Same-pass surfaces in the install window (standing rule): Guide
   knowledge + intents for the filter/recovery/compass flows, the
   website, What's New (website section AND the in-game
   ExcelWhatsNew changelog), and rotation tips — content per SPEC
   §12's canon drafts (buffer window wording: 7 days).

## Gates (standing house discipline)

- **BACKUP-FIRST:** fresh Shockbyte backup before the swap;
  **cap-at-6** — prune the oldest manual backups first so the total
  never exceeds 6; never delete the Daily Backup's newest copy or
  the two newest manual restore points; name prunees in the install
  report.
- **Emptiness:** verified empty (`list` = 0 twice) before shutdown;
  panel marker discipline — create
  `~/workspace/wrv-install-queue/WINDOW-IN-PROGRESS.txt` for a hand
  window, re-check foreign markers before Shut Down and before
  Start (coordination abort on a foreign marker < 3h old).
- **Seal note:** this packet touches **neither BossTrial nor
  GuildGate jars**, and no guild content — chapter acceptance does
  NOT re-open. The seal line in boot-verify is a state check only
  (LOCKED 3 of 5 expected).

## Rollback

Remove `ExcelAutoSort-0.1.0.jar`; restore both held jars to their
original names; restore the pre-swap `autosort.yml` (reverse the two
fields); delete the two new entry files (`chestfilter.yml`,
`filterrecover.yml`); restart. Leave `plugins/ExcelAutoSort/` (the
new data folder) in place — it is inert without the jar and holds
the only copy of any filter records/buffers created since the swap;
note its presence in the rollback report. Players who had sorting
ON keep it: the incumbent reads the untouched
`plugins/autosort/active.txt` exactly as before the swap.

## Player proofs owed after install (not install gates)

- One Bedrock player (Mike) runs the full selector flow from the
  menu AND by compass tap, arms a chest, and claims from the
  recovery box (the Bedrock form flow is rig-tested at model level
  only — the live-client pass is this proof).
- One Java player does the same via the inventory GUI.
- The live menu shows the Auto-sort entry's state from
  `%autosort_status%` (now served by ExcelAutoSort itself) and the
  two new entries render in both menus.
