# LIVE-PROPOSAL AUDIT — run 335 continuation (Nov 14, 2026, ~06:34–06:37 CST)

Worker audit for the board supervisor. Desk + PC-route pulls only.
No MANIFEST edit, no panel, no install, no live write.
Evidence start: `filing/INSTALLED.txt` (06:03 CST chained window) —
(1) PROPOSAL M-02 at 12:19:04 UTC where the packet predicted none;
(2) `/excelexpansions status` = "1 proposed, 0 due, 1 released"
against a pulled release-state.txt with no released row.

## Pull receipts (this audit, PC route)

- Probe: attempt 1, 2026-11-14 06:34:49 CST —
  `ssh -F ~/.ssh/config -o ConnectTimeout=20 mike-pc "hostname"`
  → `DESKTOP-IL05DQR`, exit 0. PASS on attempt 1 of ≤3. No fallback.
- Pull run: 06:35:05–06:35:28 CST on the PC, rclone copyto from
  `shockbyte:1. Michael's Server` into PC staging
  `muse-temp\live-audit-r335\`, then scp down to
  `filing/live-audit-r335/`. 6/6 PULL lines, 0 PULL-FAILED.
  PC-side Get-FileHash (HASHES.txt) = local SHA-256 for all 6 files
  (byte-identical transport; HASHES.txt self-hash glitch on the PC
  is the known cosmetic file-in-use case, local hash recorded).
- PC cleanup: staging dir + pull script deleted from the PC,
  verified `Test-Path = False ×2` at 06:36 CST.

| Live path | Bytes | SHA-256 |
|---|---|---|
| plugins/ExcelExpansions/release-state.txt | 207 | 5299c98123aa6bbeb18a04472d463e455cde5577b3b930d52573240ca3715bd8 |
| plugins/ExcelExpansions/level-store.txt | 1,436 | d325c8a57e9923aa755de4e4d1851c36c39940cf00b0a0ba831717037e96f99e |
| plugins/EliteMobs/content_packages/primis_meta_pack.yml | 567 | 4162e2f16ab1ab7830e61cedc405ce88dcb937b11fd8d20e1faf3492cd119842 |
| plugins/EliteMobs/content_packages/primis_adventure.yml | 708 | cc1141bd1855dd8319165ebe5418fe7987fd317be906bc0145700bbfe13c0d49 |
| plugins/EliteMobs/content_packages/primis_blood_temple_sanctum.yml | 866 | 3e08b9a94d60a5e014aa18ce2906e375fce9ccab663dca24d53eb67973140dbf |
| plugins/EliteMobs/content_packages/primis_gladius_invasion_dungeon.yml | 986 | f481333292c53bf1d0616f428386499a662e969f32153059f17f9d95037d93b5 |

Pulled contents (verbatim essentials):
- release-state.txt — ONE data row only:
  `M-02|proposed|1794658744894|1794658744894|||81c98d9f4d|proposed: 6 of 6 active in band`
  (dueSince = proposedAt = 2026-11-14T12:19:04.894Z, the logged tick;
  releasedAt and releasedBy EMPTY; token matches INSTALLED.txt.)
- level-store.txt — 11 rows, every row HighestLevelKilled 0 / Score 0
  (post-wipe zeros, as INSTALLED.txt recorded).

## A. "6 of 6 active in band" — VERDICT: CORRECT. Packet-expectation error, NOT a plugin defect.

The packet (INSTALL-PACKET.md, Boot verify) said: "With the
post-wipe zero roster, NO proposal is expected at this tick (nobody
in band yet); a PROPOSAL line for M-02 at first tick would be a
defect." That expectation misreads the locked law at three points:

1. **M-02's band bottom is 0, and IN BAND is `>= band_min`.**
   FINAL manifest, M-02 row: band "0–20 fixed (meta + adventure
   name, pack-read)". DESIGN §2.2: "IN BAND = the player's EM
   combat level >= the pack's band_min"; DESIGN §1.1 worked
   example for Primis: `band_min: 0`. Plugin:
   `releases.yml` L31–L40 (M-02: band_min 0, band_max 20,
   band_kind fixed, real pack-read band_source, so evaluable);
   `Watcher.java` L30–L32 — non-dynamic floor = `r.bandMin()`,
   in-band test `p.combatLevel().getAsInt() >= floor`, DUE iff
   `inBand*2 > active.size()`. A HighestLevelKilled of 0 therefore
   satisfies 0 >= 0. The build's own test spells this out:
   `WatcherTest.java` L23–L24 — "Primis bandMin 0 so all >=0".
   "Post-wipe zero" is the one roster state in which Primis —
   and only a band_min-0 row — is automatically in band for
   everyone; the packet assumed zero meant below-band, which is
   true for every other fixed row and false for this one.
2. **The band signal really is present at 0 (not missing).**
   `StoreCombatLevelSource.java` L15–L19 returns the stored
   HighestLevelKilled whenever non-null (0 is a value, distinct
   from the empty/absent case, which correctly counts not-in-band
   per DESIGN §2.2). The fresh level-store pull shows all 11 rows
   carrying an explicit `0`, seeded by the EM read
   (ExcelExpansions.java L86–L88), not signal-less touch entries.
3. **ACTIVE = 6 is exactly reproducible from the pulled store.**
   DESIGN §2.1: ACTIVE = played within rolling 336 h, floor of 3;
   `Watcher.java` L10–L11 implements 336 h; `Watcher.java` L21
   enforces the floor of 3. Recomputing from the fresh pull at
   the tick instant (2026-11-14T12:19:04.894Z, threshold
   2026-10-31T12:19:04.894Z): exactly 6 of the 11 store rows have
   lastActive at/after the threshold (the other 5 fall on
   Oct 22–30), so ACTIVE = 6, all 6 have HLK 0 >= 0, 6×2 > 6 —
   strict majority met. "6 of 6 active in band" is arithmetically
   exact, not an approximation. Phase order does not block it:
   `Watcher.java` L23–L24 requires every earlier row released,
   and M-01 carries bundled initial state "released"
   (`releases.yml` L24), which is also the FINAL manifest's
   design (M-01 = live hub baseline; M-01+M-02 = the DECIDED
   opening wave).
   Rig corroboration: run-335 rig §5.1 produced the identical
   mechanism — PROPOSAL M-02, "7 of 7 active in band" — from its
   12-player store (7 inside the 336 h window), and the desk
   harness reproduced it (RIG-REPORT.md §5.1, §5.7, harness
   section). Same code path, same verdict shape, different
   roster size. This was known-good behaviour before install;
   the packet's boot-verify line is the outlier.

## B. Status "1 released" vs release-state.txt — VERDICT: no bug, no rotated row, no misread of the file. The count is M-01, sourced from the bundled initial state, not the overlay.

- Status counting code: `ExcelExpansions.java` L186–L189 counts
  rows by `releaseState.stateOf(r)` — proposed (incl.
  retirement-proposed), due, released.
- `ReleaseState.java` L21–L22: `stateOf` = the overlay entry when
  present, ELSE the row's bundled initial `releasedState` from
  releases.yml. The overlay file only ever stores rows that have
  LEFT their initial state (ReleaseState.java L3–L9 header).
- `releases.yml` L24: M-01 `released_state: "released"` (Phase 0
  Guild Hub baseline — DESIGN §1.4: "Phase 0 … is recorded as a
  released baseline row"; FINAL manifest M-01: LIVE-INSTALLED,
  only validated live world). Every other row starts
  "unreleased" (e.g. M-02, L46).
- Therefore on a fresh install with an EMPTY overlay the status
  already reads "0 proposed, 0 due, 1 released" — the 1 is M-01
  by design. The rig proves the sequence: RIG-REPORT.md §5.7,
  BEFORE any tap: status "1 proposed, 0 due, 1 released" with
  only M-02 proposed; §5.3, AFTER the M-02 tap: "0 proposed,
  0 due, 2 released" (M-01 baseline + M-02). No row rotated out
  of the live overlay: the fresh pull above still holds exactly
  the one M-02 proposed row INSTALLED.txt pulled, byte-consistent
  token and timestamps.
- **Was anything actually released live? NO.** Fresh
  release-state.txt: no released row, releasedAt/releasedBy
  empty, M-02 still `proposed` awaiting the tap token
  81c98d9f4d. The 4 Primis content files all read
  `isEnabled: true` in the fresh pull — but that is their
  PRE-EXISTING live state, not a flip: FINAL manifest M-02
  standby state = "all 4 packages REGISTERED live,
  isEnabled=true", parked_state "registered-not-set-up", and
  the 4 pulled hashes are byte-identical to the rig report's
  PRISTINE pre-rig hashes (RIG-REPORT.md §5.3: meta 4162e2f1…,
  adventure cc1141bd…, blood-temple 3e08b9a9…, gladius
  f4813332…). A tap flip would have written .bak-excelexpansions
  backups and changed the state row (Releaser.java L37,
  ReleaseService path); neither happened. Nothing was flipped,
  enabled, or released by the proposal.

## C. Recommendation (one paragraph)

No build fix is needed before Mike's first tap: both anomalies
are the packet's/status-reader's expectations being wrong, not
the plugin — the M-02 proposal is valid under the locked watcher
law (band 0–20, HLK 0 in band, 6 of 6 actives, strict majority,
phase order satisfied by the released M-01 baseline), its token
is live and untapped, and the "1 released" is simply M-01's
bundled baseline state, so Mike may tap
`/excelexpansions tap M-02 81c98d9f4d` whenever he chooses; the
only cautions are sequencing facts for his tap decision, not
defects — per the FINAL manifest M-02's opening wave was tied to
the Prologue gate (Warden kill to 5/5; guild still 4 of 5 at
install) plus the S-7 em_primis world setup, which is still
owed (no em_primis world exists and all 4 files are already
isEnabled:true, so a tap now would succeed as an ALREADY_SET
no-op flip and consume the terminal "released" transition
without making Primis playable), and a future cosmetic build
could label the status baseline ("1 released (M-01 baseline)")
to prevent this exact misreading recurring.
