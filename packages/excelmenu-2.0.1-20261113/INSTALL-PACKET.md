# ExcelMenu 2.0.1 — INSTALL PACKET (DRAFT — not filed)

One restart. Gated jar swap ONLY: replaces the live ExcelMenu
2.0.0 jar with the 2.0.1 jar (Back button on grouped Bedrock
section forms, DESIGN Item 6). **No data placements** — the
2.0.1 jar's bundled entries/layouts/menus/config are
byte-identical to the live 2.0.0 jar's, so no live data file
changes and no data `.bak` set is needed. Rollback is the prior
jar. The FAILSAFE owns this install; the supervisor files it
into `~/workspace/wrv-install-queue/MANIFEST.md` after the G1
pulls below are receipted. This draft was written desk-side
with NO live contact (Nov 13, 2026).

## The artifact

    ExcelMenu-2.0.1.jar
    SHA-256  c6da29ba25f11c91f6e6a5c14932908a0bdaf192979241bd5e80e9bd3dea834a
    Size     287,906 bytes
    Built    twice byte-identical on the PROVEN 2.0.0 base
             (sources-zip snapshot, rebuilt byte-identical to
             the live jar first — BUILD-RECORD.md Part 1);
             suite 128 green; desk-rig boot verified, SEVERE 0
             (TEST-REPORT.md). Build record: BUILD-RECORD.md.

## G1 — live premises (SUPERVISOR TO CONFIRM at filing)

Desk pass made NO live-premise claim from a same-day pull. Pull
via the PC route (panel fallback per lane rules) and receipt
(path, size, SHA-256, timestamp) each of the following BEFORE
filing (DESIGN §5; expected values are the landed state of
record from `~/workspace/menu-grouped-20261112/` INSTALLED.txt /
OUTCOME.txt, Nov 12, 2026 — premises to re-prove, not receipts):

| Live path | Expected (landed state of record) |
|---|---|
| `plugins/` listing for the ExcelMenu jar (exact live filename) | one ExcelMenu jar; name as listed |
| `plugins/<live ExcelMenu jar>` | 286,829 B / `d1c6e77100d3a6f3ae342789d58b848922911f31142759c372fc50e70cd0bdf2` — STOP / re-base on mismatch |
| `plugins/ExcelMenu/layouts/server_menu.yml` | 6,959 B / `fceb14331f02036dd0d9e6283b061a7d4acbd3094d658bd1ab9f37425a76e510` |
| `plugins/ExcelMenu/layouts/commands_menu.yml` | 4,956 B / `146db1d91c4d34c49d7f43c1fd55f2eb80a6979215365b29442848ea47403bcb` |
| `plugins/ExcelMenu/config.yml` | 2,218 B / `d14d9cb14f3bf53552a57bdda25b5d13ad813ea3ce492bd31ef07e4958283eb7` |

## Filing-time rig (DESIGN §3, owed before/at filing — not done desk-side)

On the PC test-server mirror with the live-pulled companion
jars: (a) Bedrock client opens compass → picker, enters Me →
form ends with Back, taps Back → picker returns, enters a
second section → works; (b) same path in one commands_menu
group; (c) entry taps from a group form still dispatch exactly
as before (one screen-hook door — Ask or Skills — and one plain
door); (d) Java compass open unchanged; (e) SEVERE 0, registry /
layout reload counts unchanged vs the pre-swap live state.

## Gates (standing, at window time)

1. Fresh-pull hash gate: the outgoing live ExcelMenu jar hashes
   to `d1c6e771…` / 286,829 B (per G1). Anything else: STOP —
   the base moved; re-base before proceeding.
2. Emptiness gate ×2 (verified empty, twice) — this is a gated
   jar swap, NOT a text live-edit.
3. Panel discipline: create
   `~/workspace/wrv-install-queue/WINDOW-IN-PROGRESS.txt` at
   window start; delete it at window end; re-check the marker
   immediately before Shut Down and before Start (foreign
   marker = coordination abort).
4. No open battle state (seal governance: no jar swaps over an
   open battle).

## Step 0 — BACKUP FIRST + cap-at-6

Run ONE fresh Shockbyte backup for the window and confirm it
completed BEFORE any file moves; prune oldest manual backups
so the post-creation total is ≤6, naming prunees in the install
report (never delete the Daily Backup's newest copy or the two
newest manual restore points).

## Step 1 — swap the jar (the only file move)

Back up the live ExcelMenu jar in place
(`<live name>.bak-<window date>-pre201`), then place
`ExcelMenu-2.0.1.jar` under the SAME live filename confirmed in
G1. Hash-verify the placed file = `c6da29ba…` / 287,906 B.
No other file is placed, edited, or deleted.

## Step 2 — ONE restart

Panel Shut Down → Start (never a bare console stop).

## Step 3 — boot verification (all must hold; else rollback)

From the boot log:
- Plugin list carries ExcelMenu 2.0.1; ExcelBedrockMenu absent.
- Enable line shape: "ExcelMenu 2.0.1 enabled (Phase 2: unified
  …)" with registry entry count, 2 registry menus, and
  definition-menu count UNCHANGED from the pre-swap live boot
  (the desk rig, seeded with Nov 2 data, read 84 / 2 / 12; live
  counts are whatever the G1-era live state carries — the gate
  is unchanged, not the rig's numbers).
- `ExcelMenu platform bridge self-check: Geyser reflection
  surface PRESENT`.
- No SEVERE attributable to ExcelMenu.
- Seal-governance lines present as usual; TPS 20.0 after settle.

## Player proof owed post-install (DESIGN §7 — Mike, Bedrock)

1. Open the compass → section picker appears.
2. Tap **Me** → the section form's LAST button is **Back**.
3. Tap Back → back at the section picker (not out of the menu).
4. Tap another section (e.g. Travel), tap an entry — works.
5. Bonus: open Server Commands, enter any group, Back returns
   to the group picker.

Java: no proof owed for this item (chest code untouched).

## Rollback

Panel Shut Down. Restore the live ExcelMenu jar from
`<live name>.bak-<window date>-pre201`. Start. Verify the
2.0.0 enable line and unchanged registry counts. (The step-0
backup is the deeper fallback if anything else drifted.)

# ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
