# ExcelClaims 0.1.0 — SWAP PACKET (GriefPrevention → ExcelClaims)

RESTART-class, emptiness-gated jar swap (weekend protocol applies).
This packet replaces the third-party GriefPrevention 16.18.7 plugin
with ExcelClaims 0.1.0, our own clean-room claims plugin. ExcelClaims
registers under the SAME Bukkit plugin name ("GriefPrevention") and
ships a GP-API-compatible facade, so **no other plugin is rebuilt,
re-configured, or restarted beyond the normal server restart** —
Landmarks, AutoSort, Protect, AuditLog, TreeCleanup, Guide,
Leaderboard, Shops, BossTrial, the menus, and Magic all keep working
against the facade. Evidence: BUILD-RECORD.md (45/45 unit tests,
link-check PASS, rig FACADE-SMOKE 14/14 on Paper 26.3-231 with the
real Oct 15 claim set).

## Artifacts

- IN: `~/workspace/excelclaims-build/ExcelClaims-0.1.0.jar` —
  96,545 B, SHA-256
  838a85b717e5a5617d3dbd48f151efd03ded3269ce4d458dec070c4a2edfbee8
  → `plugins/ExcelClaims-0.1.0.jar` (the filename is ours; the
  plugin NAME inside is "GriefPrevention" — that is the facade
  contract, not a mistake).
- OUT: the live GriefPrevention jar (identify it in the window: the
  jar in `plugins/` whose plugin.yml declares
  `name: GriefPrevention` and version 16.18.7 — there is exactly
  one). Rename aside to `<live filename>.held-<install date>-pre-excelclaims`,
  hash-verified. NEVER delete it in this window.
- UNTOUCHED, always: `plugins/GriefPreventionData/` (GP's data —
  the import source AND the data-level rollback), every other
  plugin, every config. ExcelClaims writes only its own new folder
  `plugins/GriefPrevention/`.

## Gates (all standing rules apply)

- Verified-empty server (list = 0 twice) or weekend countdown
  protocol; WINDOW-IN-PROGRESS marker discipline; the failsafe
  re-checks the marker before Shut Down and before Start.
- **BACKUP FIRST (cap-at-6):** fresh Shockbyte backup before the
  swap, oldest manual pruned per the house rule, prunees named in
  the install report. **That backup's restore is rollback #1.**

## In-window procedure

1. **Live census (BEFORE stopping anything).** From the panel file
   manager, record: count of `plugins/GriefPreventionData/ClaimData/*.yml`
   (= live claims), count of `plugins/GriefPreventionData/PlayerData/*`
   (= player accounts), the value of `ClaimData/_nextClaimID`, and
   the split: files with empty `Owner:` = admin claims; files with
   `Parent Claim ID:` other than -1 = subdivisions. Write all five
   numbers in the marker. (At filing, the last known census was
   Oct 18: 10 claims / 6 players, plus the Nov 3 spawn admin claim
   and any plots sold since — the in-window census governs, never
   the filing numbers.)
2. Capture the live GP jar's SHA-256 (panel Compute checksum) into
   the marker.
3. BACKUP FIRST per the gate above; verify the backup completed.
4. Stop the server. Rename the GP jar aside (hash-verify the aside).
   Place `ExcelClaims-0.1.0.jar` (panel Compute checksum MUST equal
   838a85b7…). Start the server.
5. **Boot-verify:** Done; plugin count 77 UNCHANGED; the enable line
   reads `ExcelClaims 0.1.0 enabled: 0 claims, 0 player accounts,
   build EXC-CLAIMS-0.1.0-WRV-20261107-01-7c1a3f`; SEVERE 0
   attributable; guildgate LOCKED with seal count unchanged from
   the pre-window read; Landmarks/Protect/AuditLog adapters engage
   (no adapter-off or class-link errors in the boot log — their GP
   hooks load only when the facade resolves).
6. **LIVE RECONCILIATION GATE (hard stop).** Console:
   `excelclaims import`, then `excelclaims status`. The import line
   and the status counts MUST match the Step-1 census exactly:
   claims (total, admin, subdivisions) and player accounts. **Any
   mismatch = STOP: do not proceed, roll back per below, and
   report the two count sets.** (The importer also reports
   re-rooted orphan subdivisions; any orphan on live data = STOP
   and report — the rig set had none.)
7. **Post-swap verification (console + log):**
   - `excelclaims status` build ID matches the packet.
   - Protect's claim-event path is live (its GpAdapter registered;
     the import itself fires no fake history — the first natural
     claim event after the swap must appear in Protect's system
     stream; check after step 8's player proof).
   - `/claimslist` parity is a player proof (step 8).
8. **Player proofs (owed post-install, named — the install is not
   proof-complete until they land):** (a) any family member stands
   in their own claim — container opens, build works; stands in a
   neighbor's claim — build denied with the ExcelClaims denial
   line; (b) `/claimslist` shows the player's claims and block
   counts; (c) golden-shovel corner nudge on their own claim
   (resize visualization shows, then abandon the resize); (d) at
   spawn: the admin claim protects (a non-staff player cannot
   build); (e) if any Landmark plot is currently sold: the renter
   can build in their plot and — once ExcelAutoSort is installed —
   can configure a filter chest in it while a ContainerTrust-only
   family member cannot (AutoSort §6.4).
9. Write the marker `~/workspace/excelclaims-build/INSTALLED.txt`
   (census numbers, backup name + prunees, GP jar hash + aside
   name, import/status counts, boot-verify results).

## Rollback (pre-written, in order)

1. **Data safety is structural:** the import only READS
   `GriefPreventionData/`; GP's data was never modified. Any failure
   before or at the reconciliation gate rolls back by jar alone:
   stop → remove `ExcelClaims-0.1.0.jar` → rename the GP jar aside
   back to its live name → start → verify GP's enable line and one
   known claim (console `claim` checks by a player proof or the
   boot log). `plugins/GriefPrevention/` (ExcelClaims' store) may
   be left in place — GP ignores it — or deleted; name the choice
   in the marker.
2. If anything beyond the jar swap went wrong (it should not —
   nothing else is touched), restore the Step-3 backup.
3. Tertiary/data-level: ExcelClaims can export its store back to
   pure GP format (`excelclaims export` →
   `plugins/GriefPreventionData-restore/`) if a rollback is ever
   needed AFTER live play has created new claims under ExcelClaims;
   those files replace `GriefPreventionData/` contents only under
   a stopped server, with the originals renamed aside first.

## Same-pass surfaces (parent, at install time)

Commands, tools, and behavior are parity, so Guide claims answers
stay accurate; the parent still: (1) adds the What's New entry
(website + in-game ExcelWhatsNew changelog) announcing ExcelClaims
as our own claims plugin; (2) checks the rotation tips for any
GriefPrevention-named wording (behavior references stay true; brand
references update); (3) updates the records (Route Sheet + Big
Brain) and the plugin inventory counts (77 plugins, name
"GriefPrevention" now ours). ORDERING: file order governs — the
ExcelAutoSort 0.1.0 entry installs first; the two never share a
window.
