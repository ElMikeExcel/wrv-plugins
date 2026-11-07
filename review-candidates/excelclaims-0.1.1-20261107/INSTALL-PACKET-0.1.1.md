# ExcelClaims 0.1.1 — SWAP PACKET (0.1.0 → 0.1.1 + expiration clock restore)

RESTART-class, emptiness-gated jar swap PLUS one config restore,
in the same window. 0.1.1 is the fix for the Nov 7, 2026
expiration incident (BUILD-RECORD.md, 0.1.1 section): the importer
now seeds the dates GriefPrevention's files never carried, and
the expiration sweep can never expire a claim whose owner's
last-seen basis is unknown. Evidence: 55/55 unit tests, link-check
PASS, rig A (persistence-across-reboot + FACADE-SMOKE 14/14 on the
real staged set), rig B (probe-verified Bukkit last-played
records; the genuinely-absent fixture expired on schedule, the
active fixture survived, and the all-zero "recovered store"
fixture survived with the skipped-unknown note).

## The config restore — read this first

The Nov 7 recovery froze the live expiration clocks at 99999
(chest / unused / all-claims) in `plugins/GriefPrevention/config.yml`
to stop the 0.1.0 sweep from re-expiring the recovered claims.
This packet restores the settled live values — **chest 7 /
unused 14 / all-claims 60** — in the same window as the jar swap.

**The restore is safe ONLY because the 0.1.1 jar carries fixes
A+B. NEVER restore finite clocks while the 0.1.0 jar is live, and
never boot 0.1.0 against finite clocks in any rollback — that
combination is exactly the incident.** Jar and clocks move
together, in this window, in the order below.

## Artifacts

- IN: `~/workspace/excelclaims-build/ExcelClaims-0.1.1.jar` —
  97,503 B, SHA-256
  ff5ea0810d1df5866a423ac126e3145354bca25d2600cfc42a5536362b540706
  → `plugins/ExcelClaims-0.1.1.jar` (plugin NAME inside remains
  "GriefPrevention" — the facade contract).
- OUT: `plugins/ExcelClaims-0.1.0.jar` → rename aside to
  `ExcelClaims-0.1.0.jar.held-<install date>-pre-0.1.1`,
  hash-verified. NEVER delete it in this window.
- CONFIG: `plugins/GriefPrevention/config.yml` — back it up FIRST
  to `plugins/GriefPrevention/config.yml.bak-<install date>-pre-0.1.1`,
  then change ONLY the three expiration values:
  `chest-claim-days: 99999` → `7`, `unused-claim-days: 99999` →
  `14`, `all-claims-days-inactive: 99999` → `60`. Nothing else in
  the file changes (the live config carries WRV's tuned values;
  do NOT replace it with the jar's default config).
- UNTOUCHED, always: `plugins/GriefPreventionData/` (the GP
  originals — still the data-level rollback), every other plugin,
  every other config, and the claim store itself
  (`plugins/GriefPrevention/ClaimData|PlayerData|player-extra.yml` —
  0.1.1 reads the 0.1.0-written files as-is).

## Gates (all standing rules apply)

- Verified-empty server (list = 0 twice) or weekend countdown
  protocol; WINDOW-IN-PROGRESS marker discipline; the failsafe
  re-checks the marker before Shut Down and before Start.
- **BACKUP FIRST (cap-at-6):** fresh Shockbyte backup before the
  swap, oldest manual pruned per the house rule, prunees named in
  the install report. **That backup's restore is rollback #1.**

## In-window procedure

1. **Pre-window census (BEFORE stopping anything).** Console:
   `excelclaims status` — MUST read the recovered live state:
   **Claims: 10 (1 admin, 0 subdivisions) · Player accounts: 11**,
   build EXC-CLAIMS-0.1.0-WRV-20261107-01-7c1a3f. Read the three
   live expiration values from `plugins/GriefPrevention/config.yml`
   — MUST be 99999 / 99999 / 99999 (the recovery freeze). Any
   other values = STOP and report; the window's assumptions are
   wrong.
2. BACKUP FIRST per the gate above; verify the backup completed.
3. Stop the server. Rename the 0.1.0 jar aside (hash-verify the
   aside). Place `ExcelClaims-0.1.1.jar` (panel Compute checksum
   MUST equal ff5ea081…). Back up config.yml (name above), apply
   the three-value clock restore, and READ THE FILE BACK — the
   expiration section must show 7 / 14 / 60 before the server
   starts.
4. Start the server. **Boot-verify:** Done; plugin count 76
   UNCHANGED (1-for-1 swap); the enable line reads
   `ExcelClaims 0.1.1 enabled: 10 claims, 11 player accounts,
   build EXC-CLAIMS-0.1.1-WRV-20261107-02-5a1e9c`; SEVERE 0
   attributable; guildgate LOCKED 3 of 5 unchanged; Landmarks /
   Protect / AuditLog adapters engage (no adapter-off or
   class-link errors).
5. **POST-INSTALL PROOF — the sweep tick (hard gate).** The
   first expiration tick fires ~1 minute after enable. Wait at
   least 5 minutes past the boot, then:
   - Console `excelclaims status` — MUST still read **Claims: 10
     (1 admin, 0 subdivisions) · Player accounts: 11**.
   - Read the boot log from the enable line forward: NO
     `expiring claim` line may name any live claim. A single
     `expiration check skipped N claim(s) — owner last-seen
     unknown` line is EXPECTED and acceptable (it is the guard
     working for any account Bukkit has no record of).
   - If any claim HAS expired: STOP and report before anything
     else. Distinguish before concluding: with 0.1.1 an expiring
     claim always has a positive last-seen basis, so the executor
     names the claim, its owner, and the owner's Bukkit
     last-played (panel: the player's .dat under
     `world/players/data/`, or a Guide/Protect session record).
     A genuine >60-day absence expiring on the long clock is the
     feature working (report it, don't roll back); ANY expiry
     whose basis cannot be shown genuine = treat as the defect,
     roll back per below.
6. Write the marker `~/workspace/excelclaims-build/INSTALLED-0.1.1.txt`
   (census numbers, backup name + prunees, 0.1.0 jar hash + aside
   name, config .bak name, boot-verify results, the step-5 status
   pair + log read). Leave `INSTALLED.txt` (the 0.1.0 marker)
   untouched.

## Rollback (pre-written, in order)

1. **Jar + clocks move together:** stop → remove
   `ExcelClaims-0.1.1.jar` → rename the 0.1.0 aside back to
   `ExcelClaims-0.1.0.jar` → restore the frozen config from
   `config.yml.bak-<install date>-pre-0.1.1` (clocks back to
   99999 BEFORE the 0.1.0 jar ever boots) → start → verify the
   0.1.0 enable line and `excelclaims status` = 10 / 11.
2. If anything beyond the swap went wrong (it should not —
   nothing else is touched), restore the Step-2 backup.
3. Data safety is structural, as before: the store was never
   modified by the swap; `plugins/GriefPreventionData/` remains
   the untouched data-level rollback.

## Same-pass surfaces (parent, at install time)

No player-facing behavior changes (the fix is invisible except
that expiration is safe again), so the surfaces reduce to the
records close-out: Route Sheet + Big Brain note the incident fix
and the restored clocks. The 0.1.0 packet's Step-8 player proofs
(own-claim build/container + neighbor denial, /claimslist,
shovel resize, spawn admin protection, Landmark renter case)
remain owed from the 0.1.0 install and are NOT discharged by
this window. ORDERING: independent of every other queue entry;
runs in any empty/stopped window.
