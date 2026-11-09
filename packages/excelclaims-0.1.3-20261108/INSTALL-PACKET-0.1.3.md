# ExcelClaims 0.1.3 — SWAP PACKET (0.1.2 → 0.1.3, corner pillars)

RESTART-class, emptiness-gated jar swap, **over ExcelClaims
0.1.2 ONLY**. No config step (see below). 0.1.3 is Mike's
ruling of Nov 8, 2026 (~7:38 PM CST, tap + typed words: "Make
claim corners taller and more visible"), commissioned after
Marv's player-proof report the same evening: visualizing a
claim, he saw only 3 of its 4 corners — 0.1.2 sends ONE
packet-only glowstone block per corner at surface+1, and a
single ground-level block disappears behind slopes and bumps
from many viewing angles. 0.1.3 makes each corner a short
pillar: 4 blocks total (glowstone body, sea-lantern cap),
still packet-only per viewer (the world is never touched),
still 45 seconds. Evidence: BUILD-RECORD.md, 0.1.3 section
(79/79 unit tests, link-check PASS, rig boot over the 0.1.2
rig store with FACADE-SMOKE 14/14).

## The pre-window gate — read this first

**This packet installs ONLY over ExcelClaims 0.1.2.** Before
stopping anything, the live build MUST read
`EXC-CLAIMS-0.1.2-WRV-20261107-03-3b8f2d` (console:
`excelclaims status`) AND a fresh panel download of the live
jar MUST hash to the 0.1.2 gate below. **Anything else =
STOP and report.** Never run this swap in the same window as
another jar swap.

- LIVE GATE: `plugins/ExcelClaims-0.1.2.jar` — 99,370 B,
  SHA-256
  b4663fa3d377bafa06803f8b4d21294ac1ea6b720614a263cc5fb3d7ff120c5d.

## The config step — there is none (read this second)

0.1.3 adds a `visualization:` config section
(`corner-pillar-height`, `corner-block-material`,
`corner-cap-material`), but the ClaimsConfig FIELD DEFAULTS
are the shipped behavior (4 / GLOWSTONE / SEA_LANTERN) — the
live config.yml carries WRV's tuned values and MUST NOT be
replaced or edited by this window. The pillars work at the
shipped shape the moment the jar boots. (Adding the section
later for tuning is a hand edit anyone can make; the values
take effect on `/excelclaims reload`'s config reload or the
next boot.)

## Artifacts

- IN: `~/workspace/excelclaims-build/ExcelClaims-0.1.3.jar` —
  102,334 B, SHA-256
  21f3f7999c36259b6db9f3cc79208501b4ebbf47372a06db567ce2d3a406fbd7
  → `plugins/ExcelClaims-0.1.3.jar` (plugin NAME inside remains
  "GriefPrevention" — the facade contract).
- OUT: `plugins/ExcelClaims-0.1.2.jar` → rename aside to
  `ExcelClaims-0.1.2.jar.held-<install date>-pre-0.1.3`,
  hash-verified (b4663fa3…). NEVER delete it in this window.
- CONFIG: untouched (see above).
- UNTOUCHED, always: `plugins/GriefPreventionData/` (the GP
  originals — still the data-level rollback), every other
  plugin, every other config, and the claim store itself
  (`plugins/GriefPrevention/ClaimData|PlayerData|player-extra.yml`
  — 0.1.3 reads the 0.1.2-written files as-is; no data
  migration; the visualization change carries no state).

## Gates (all standing rules apply)

- Verified-empty server (list = 0 twice) or weekend countdown
  protocol; WINDOW-IN-PROGRESS marker discipline; the failsafe
  re-checks the marker before Shut Down and before Start.
- **BACKUP FIRST (cap-at-6):** fresh Shockbyte backup before
  the swap, oldest manual pruned per the house rule, prunees
  named in the install report. **That backup's restore is
  rollback #1.**

## In-window procedure

1. **Pre-window census (BEFORE stopping anything).** Console:
   `excelclaims status` — build MUST read
   **EXC-CLAIMS-0.1.2-WRV-20261107-03-3b8f2d** (the gate
   above). Record the claims / player-accounts counts exactly
   as read (last known: 10 claims (1 admin, 0 subdivisions) /
   11 accounts at the 0.1.2 install, Nov 7 — the live read
   governs, counts may legitimately have grown since).
   Fresh-download the live jar and hash it against the gate.
   Any mismatch = STOP and report.
2. BACKUP FIRST per the gate above; verify the backup
   completed.
3. Stop the server. Rename the 0.1.2 jar aside (hash-verify
   the aside). Place `ExcelClaims-0.1.3.jar` (panel Compute
   checksum MUST equal 21f3f799…).
4. Start the server. **Boot-verify:** Done; plugin count 76
   UNCHANGED (1-for-1 swap); the enable line reads
   `ExcelClaims 0.1.3 enabled: <N> claims, <M> player
   accounts, build EXC-CLAIMS-0.1.3-WRV-20261108-04-bd9ff4`
   with N / M EXACTLY the step-1 census; SEVERE 0
   attributable; guildgate LOCKED count unchanged from the
   pre-window read; Landmarks / Protect / AuditLog adapters
   engage (no adapter-off or class-link errors).
5. **POST-INSTALL PROOF — census (hard gate).** At least 5
   minutes past the boot, console `excelclaims status` MUST
   still read the step-1 census (claims + accounts) on build
   0.1.3-04-bd9ff4. Any drift = STOP and report.
6. Write the marker `~/workspace/excelclaims-build/INSTALLED-0.1.3.txt`
   (census numbers pre + post, backup name + prunees, 0.1.2
   jar hash + aside name, boot-verify results, the step-5
   status read). Leave `INSTALLED-0.1.2.txt` and the earlier
   markers untouched.

## Player proof (post-install, named — owed, does not block the window)

Any player (Marv/GumGumJetPistol is the natural pick — this
fixes HIS report) visualizes a claim the usual ways (golden
shovel flow, stick investigation, or a claim command that
shows corners), on Java AND on Bedrock:

- every corner is a 4-tall pillar: 3 glowstone with a pale
  sea-lantern block on top;
- all FOUR corners are findable, including the corner behind
  the slope that hid from Marv in 0.1.2;
- after ~45 seconds the pillars vanish and the underlying
  blocks are exactly as they were (the markers never touched
  the world);
- a second player standing beside the viewer sees NO pillars
  (the markers are per-viewer packets).

## Rollback (pre-written, in order)

1. **Jar only:** stop → remove `ExcelClaims-0.1.3.jar` →
   rename the 0.1.2 aside back to `ExcelClaims-0.1.2.jar` →
   start → verify the 0.1.2 enable line and `excelclaims
   status` = the step-1 census. No config or data was
   changed by this window, so nothing else unwinds.
2. If anything beyond the swap went wrong (it should not —
   nothing else is touched), restore the Step-2 backup.
3. Data safety is structural: the store format is unchanged
   between 0.1.2 and 0.1.3; `plugins/GriefPreventionData/`
   remains the untouched data-level rollback.

## Same-pass surfaces (parent, at install time)

0.1.3 changes one piece of player-visible truth: claim
corners now show as glowing pillars (glowstone + sea-lantern
cap, 4 tall) instead of a single glowstone block. At install
time the parent lands, in the same pass: Guide knowledge (any
"glowstone corner" wording updated to the pillar shape),
an ExcelWhatsNew entry on BOTH surfaces (website section +
in-game changelog.yml — text-only, a live-edit under the
Nov 2 rule), the broadcaster tips check, and the records
close-out (Route Sheet + Big Brain + draft record PR +
Drive deposit per PUSH-NOTES-0.1.3.md). ORDERING: installs
only over 0.1.2 (gate above); never same window as another
jar swap. The 0.1.0 packet's Step-8 player proofs remain
owed and are NOT discharged by this window.
