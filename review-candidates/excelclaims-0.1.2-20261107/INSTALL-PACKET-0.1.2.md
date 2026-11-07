# ExcelClaims 0.1.2 — SWAP PACKET (0.1.1 → 0.1.2 + per-player expiration config)

RESTART-class, emptiness-gated jar swap PLUS one config rewrite,
in the same window, **over ExcelClaims 0.1.1 ONLY**. 0.1.2 is
Mike's design ruling of Nov 7, 2026 (DESIGN-0.1.2-per-player-
expiration.md): the three global expiration clocks and the
block-count exemptions are replaced by ONE inactivity threshold
settable per player — `expiration.default-days` (`never` =
infinite) plus `expiration.players` overrides — with the new
staff command `/excelclaims expire <player> [days|never]` to
report and set it live. The 0.1.1 safety floor is retained
verbatim: a claim whose owner's last-seen basis is unknown NEVER
expires under any setting. Evidence: BUILD-RECORD.md, 0.1.2
section (66/66 unit tests, link-check PASS, rig A persistence +
FACADE-SMOKE 14/14, rig B mixed-fixture soak on probe-verified
Bukkit playerdata stamps).

## The pre-window gate — read this first

**This packet installs ONLY over ExcelClaims 0.1.1.** Before
stopping anything, the live build MUST read
`EXC-CLAIMS-0.1.1-WRV-20261107-02-5a1e9c` (console:
`excelclaims status`). **Anything else — 0.1.0 still live, the
0.1.1 window still in flight, a different build id — = STOP and
report.** 0.1.2's safety story is layered on 0.1.1's importer
seeding and sweep floor; skipping a layer voids the packet.
Never run this swap in the same window as another jar swap.

## The config step — read this second

At 0.1.1 install the live expiration section was restored to
the settled GP clocks (chest 7 / unused 14 / all-claims 60).
0.1.2 retires those keys. This window's config step rewrites
the expiration section of `plugins/GriefPrevention/config.yml`
to the new shape, with **`default-days: never`** — Mike's
ruling: everyone infinite for now, per-player numbers available
whenever wanted. Steps:

1. Back the config up FIRST to
   `plugins/GriefPrevention/config.yml.bak-<install date>-pre-0.1.2`.
2. In the `expiration:` section: DELETE the five retired lines
   (`chest-claim-days`, `unused-claim-days`,
   `all-claims-days-inactive`,
   `except-when-owner-has-total-claim-blocks`,
   `except-when-owner-has-bonus-claim-blocks`) and ADD:
   ```yaml
     default-days: never
     players:
   ```
   KEEP `check-interval-minutes: 60`. Nothing else in the file
   changes — the live config carries WRV's tuned values; do NOT
   replace it with the jar's default config. (Leaving a retired
   key behind would be harmless — 0.1.2 never reads them — but
   the section is rewritten clean so the file tells the truth.)
3. READ THE FILE BACK before Start: the expiration section must
   show exactly `check-interval-minutes: 60`,
   `default-days: never`, and an empty `players:` — nothing else.

## Artifacts

- IN: `~/workspace/excelclaims-build/ExcelClaims-0.1.2.jar` —
  99,370 B, SHA-256
  b4663fa3d377bafa06803f8b4d21294ac1ea6b720614a263cc5fb3d7ff120c5d
  → `plugins/ExcelClaims-0.1.2.jar` (plugin NAME inside remains
  "GriefPrevention" — the facade contract).
- OUT: `plugins/ExcelClaims-0.1.1.jar` → rename aside to
  `ExcelClaims-0.1.1.jar.held-<install date>-pre-0.1.2`,
  hash-verified (ff5ea0810d1df5866a423ac126e3145354bca25d2600cfc42a5536362b540706).
  NEVER delete it in this window.
- CONFIG: as above. The config backup is part of the rollback.
- UNTOUCHED, always: `plugins/GriefPreventionData/` (the GP
  originals — still the data-level rollback), every other
  plugin, every other config, and the claim store itself
  (`plugins/GriefPrevention/ClaimData|PlayerData|player-extra.yml`
  — 0.1.2 reads the 0.1.1-written files as-is; no data migration).

## Gates (all standing rules apply)

- Verified-empty server (list = 0 twice) or weekend countdown
  protocol; WINDOW-IN-PROGRESS marker discipline; the failsafe
  re-checks the marker before Shut Down and before Start.
- **BACKUP FIRST (cap-at-6):** fresh Shockbyte backup before the
  swap, oldest manual pruned per the house rule, prunees named
  in the install report. **That backup's restore is rollback #1.**

## In-window procedure

1. **Pre-window census (BEFORE stopping anything).** Console:
   `excelclaims status` — MUST read **Claims: 10 (1 admin, 0
   subdivisions) · Player accounts: 11** on build
   **EXC-CLAIMS-0.1.1-WRV-20261107-02-5a1e9c** (the gate above).
   Read the live expiration section of
   `plugins/GriefPrevention/config.yml` — expected: the 0.1.1
   packet's restored clocks (7 / 14 / 60 + the two exemption
   keys). Any other build or census = STOP and report; the
   window's assumptions are wrong.
2. BACKUP FIRST per the gate above; verify the backup completed.
3. Stop the server. Rename the 0.1.1 jar aside (hash-verify the
   aside). Place `ExcelClaims-0.1.2.jar` (panel Compute checksum
   MUST equal b4663fa3…). Back up config.yml (name above), apply
   the config step, and READ THE FILE BACK — the expiration
   section must show the new shape (60 / never / empty players)
   before the server starts.
4. Start the server. **Boot-verify:** Done; plugin count 76
   UNCHANGED (1-for-1 swap); the enable line reads
   `ExcelClaims 0.1.2 enabled: 10 claims, 11 player accounts,
   build EXC-CLAIMS-0.1.2-WRV-20261107-03-3b8f2d`; SEVERE 0
   attributable; guildgate LOCKED 3 of 5 unchanged; Landmarks /
   Protect / AuditLog adapters engage (no adapter-off or
   class-link errors).
5. **POST-INSTALL PROOF — census + the sweep tick + the new
   command (hard gate).** The first expiration tick fires ~1
   minute after enable. Wait at least 5 minutes past the boot,
   then:
   - Console `excelclaims status` — MUST still read **Claims: 10
     (1 admin, 0 subdivisions) · Player accounts: 11**.
   - Read the boot log from the enable line forward: NO
     `expiring claim` line may appear AT ALL — with
     `default-days: never` and no per-player overrides, the
     sweep has nothing it is allowed to expire. (A
     `expiration check skipped N claim(s)` line should not
     appear either: `never` owners are skipped by setting
     before the floor is consulted. If one DOES appear, it
     means a finite setting exists somewhere — STOP and report
     what the config actually holds.)
   - **Command spot-reports:** console
     `excelclaims expire 00000000-0000-0000-0009-01f40ccd59de`
     (Kailey) and `excelclaims expire 8399c623-786f-448c-8b11-f54504721819`
     (Marvin) — each MUST answer `never (server default)`. These
     are report-only; they write nothing.
   - If any claim HAS expired or a spot-report shows anything
     but `never`: STOP and report before anything else.
6. Write the marker `~/workspace/excelclaims-build/INSTALLED-0.1.2.txt`
   (census numbers, backup name + prunees, 0.1.1 jar hash + aside
   name, config .bak name, the rewritten expiration section as
   read back, boot-verify results, the step-5 status pair + log
   read + both spot-report lines verbatim). Leave
   `INSTALLED-0.1.1.txt` and `INSTALLED.txt` untouched.

## Rollback (pre-written, in order)

1. **Jar + config move together:** stop → remove
   `ExcelClaims-0.1.2.jar` → rename the 0.1.1 aside back to
   `ExcelClaims-0.1.1.jar` → restore the config from
   `config.yml.bak-<install date>-pre-0.1.2` (the 7/14/60
   section returns WITH the 0.1.1 jar that understands it —
   never boot 0.1.2's `default-days: never` section under a
   jar that can't read it and reach the opposite conclusion;
   0.1.1 ignores the new keys and would run its 7/14/60 field
   defaults, which is NOT the pre-window state) → start →
   verify the 0.1.1 enable line and `excelclaims status` =
   10 / 11.
2. If anything beyond the swap went wrong (it should not —
   nothing else is touched), restore the Step-2 backup.
3. Data safety is structural: the store was never modified by
   the swap; `plugins/GriefPreventionData/` remains the
   untouched data-level rollback. Any per-player entries a
   staff member set via `/excelclaims expire` while 0.1.2 was
   live live only in config.yml under `expiration.players` —
   the rollback's config restore removes them with the section;
   if a rollback happens, note in the install report which
   entries existed so they can be re-set after a re-install.

## Same-pass surfaces (parent, at install time)

0.1.2 changes one piece of player-visible truth: claims no
longer run on global expiration clocks — expiration is
per-player policy, defaulting to never. At install time the
parent sweeps the surfaces for the old truth and lands, in the
same pass: Guide knowledge + website canon (any "claims expire
after N days" wording — TWO artifacts: index.html AND
ai-knowledge.json via the full regen), an ExcelWhatsNew entry
on BOTH surfaces (website section + in-game changelog.yml —
text-only, a live-edit under the Nov 2 rule), the broadcaster
tips check, and the records close-out (Route Sheet + Big
Brain). The `/excelclaims expire` command itself is staff-only
(console / adminclaims permission) and needs no menu surface.
The 0.1.0 packet's Step-8 player proofs remain owed and are
NOT discharged by this window. ORDERING: installs only over
0.1.1 (gate above); never same window as another jar swap.
