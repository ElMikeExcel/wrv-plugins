# RECORD — Primis Adventure v54 update preparation (issue #242)

Lane: `~/workspace/primis-v54-update-20261124/` · Run: 2026-11-24, ~06:18–06:45 CT ·
Mode: prepare only. Nothing was applied to live; live was read-only throughout; nobody was
messaged; the draft reply was not posted.

## Outcome

**SUPERSEDED Nov 24, 2026 ~11:20 CT — v54 was acquired, merged, and FILED the same
morning after Mike approved the link flow. See the addendum at the bottom of this
record.** The prep-lane outcome below is the state as of 06:45 CT.

Everything that does not depend on having the v54 bytes is **done and receipted**: the live
Primis set is pulled and hash-verified, every local WRV edit inside it is inventoried and
confirmed present on live, the merge rules are written, and the packet is staged
(`INSTALL-PACKET.md`, marked NOT FILED). The one missing piece is the v54 content set itself:
EliteMobs' content download is gated on a Nightbreak account link, and completing that link
needs Mike's nightbreak.io sign-in (Patreon) in a browser — outside this lane's reach. Exact
unblock steps are in §Blocker. The MANIFEST pointer stub was **not** filed, because the filing
condition (packet complete, merged set proven complete against the diff) is not met.

## What was verified

- **Live Primis set pulled, complete.** 1,604 primis files from `D:\muse-servers\wrv\plugins\EliteMobs`,
  archive SHA-256 matched on both ends (`9b12d96b…`), census == extraction == 1,604.
  Full receipts: `RECEIPTS.md`; census: `diff/live-census.tsv`; tree: `live/`.
- **v54 exists and is current.** The mirror's EliteMobs boot (06:33:59 CT) parsed the
  Nightbreak API and reported: `The Primis Adventure! … Your version: 0 / remote version: 54`.
  (Live's own installed marker: `primis_meta_pack.yml` → `dungeonVersion: 51`.)
- **Local edits all present on live** (details + merge rules: `diff/LOCAL-EDITS-INVENTORY.md`):
  the Nov 21 `primis_sleep.yml` WakeUpOnDamage patch (hash identical to the placed file);
  the D219 Sunken Ship chest fix (all 3 hashes identical to staged); all 23 D197 density files
  (live hashes == post-edit hashes, 23/23); `dungeonLockoutMinutes: 120` in all 16 Primis
  package files. Records sweep found no other hand edits in the Primis tree.
- **Questions the task asked the diff to answer** — three of the four cannot be answered until
  v54 bytes exist, and are queued as the first checks of the diff pass (see §Remaining):
  (i) v54's `primis_sleep.yml` vs MagmaGuy's description; (ii) whether v54 touches the three
  Sunken Ship chest files; (iii) whether v54 touches the D197 files; (iv) whether v54 touches
  the lockout package files. What CAN be said now: our live sleep file already contains a
  wake-on-damage block equivalent to the described fix (ours, from Nov 21) — v54's file will
  replace it after an equivalence read, per the merge rule.

## Blocker — v54 acquisition

- Mirror route attempted end to end: mirror EliteMobs folder snapshotted
  (`mirror-em-presnap-20261124.zip`, SHA-256 `433E4A40…`), mirror booted, then via RCON:
  - `/em updateallcontent` → `[EliteMobs] This server is not linked to a Nightbreak account.
    Run /nightbreaklogin first.`
  - `/nightbreaklogin` → minted `https://nightbreak.io/server/link/GTFU-Z3XE`
    (06:34:56 CT, 30-minute life, so expired ~07:04 CT unused — Mike is asleep; re-minting is
    one RCON command and costs nothing).
- No other local source exists: a PC-wide search found no Primis zip in Downloads,
  muse-tools, the mirror tree, or `D:\muse-servers`; the mirror has no usable imports content.
  The alternate distribution is the updated itch zip, which likewise needs Mike's account in a
  browser.
- **Unblock (any one of these, ~2 minutes of Mike's browser):**
  1. Preferred: with the mirror booted, run `/nightbreaklogin` via RCON to mint a fresh link;
     Mike opens it, signs in to nightbreak.io (Patreon sign-in), approves the server. Then
     `/em updateallcontent` on the mirror downloads v54; harvest the mirror's primis files
     into this lane's `v54/`, stop the mirror, restore it from the snapshot, verify.
  2. Or: Mike downloads the updated Primis Adventure zip from itch and drops it in this lane;
     unpack to `v54/` and proceed — no mirror involved.
- Note: `/em updateallcontent` on the mirror will also pull the Adventurers Guild Hub update
  (26 → 27) the boot log flagged. Out of this lane's scope — harvest Primis files only.

## Mirror state

Restored and verified: stopped cleanly (SERVER-EXIT 0), EliteMobs folder replaced from the
pre-session snapshot, post-restore census byte-identical to the snapshot across all 1,059
primis files (0 missing / 0 extra / 0 mismatched). Mirror left stopped, as found. (A sibling
lane's DungeonRig session used the mirror during this run's first boot and shut it down
itself at 06:32 CT; the snapshot predates that session too, so the restore also unwinds the
rig's runtime churn in the EliteMobs folder. World folders the rig created are outside this
lane's snapshot scope and were left alone.)

## Remaining (in order, once v54 lands in `v54/`) — ALL DONE Nov 24, 2026 (see addendum)

1. Semantic diff v54 vs `live/` (YAML-aware; regional-boss files and chest files judged on
   stable lines — EM rewrites respawn timestamps / restock timers at runtime).
2. Answer the four queued questions from the diff; write `diff/DIFF-CLASSIFICATION.md` with
   every differing file classified (a)/(b)/(c) and every class-(c) merge shown.
3. Build `packet/files/` (merged set) + `packet/rollback/` (live bytes from `live/`).
4. Add harvest receipts to `RECEIPTS.md`; run the P3 lint; file the MANIFEST pointer stub.
5. Hand the packet to an install window; run the proof below; post the #242 reply
   (`DRAFT-REPLY-242.md`, updated with the result) after Mike's review.

## Addendum — acquisition + merge + filing (2026-11-24, ~10:50–11:20 CT, Mike-approved run)

Mike approved the Nightbreak link flow at 10:48 AM CT ("go ahead and let's do it open up
what you need try it on my PC so that way it downloads directly onto it"). Executed:

- **Link flow: SUCCESS, no credentials needed.** Mirror snapshotted fresh
  (`mirror-em-presnap-20261124-v54.zip`, SHA-256 `602F120A…`, census 1,059), booted,
  `/nightbreaklogin` minted code `GEXM-AE5G` (10:52:22 CT). Opened in Mike's PC Chrome
  via CDP: already signed in as Michael Reyes (Patreon session), page named the server
  "WRV TEST - NOT LIVE" with the matching code; clicked "Link this server" → "Server
  connected". `/em updateallcontent` imported v54 (meta pack on disk:
  `dungeonVersion: 54 # Sleeping bosses now wake up when hit`). Full receipts in
  RECEIPTS.md §v54 harvest.
- **Harvest complete: 1,603 files, 1,603/1,603 hash-verified** against the PC census
  (one file, `content_packages/primis_adventure.yml`, was skipped by tar over a BOM in
  the file list and recovered by a second boot/update + direct copy — hash matches the
  census exactly). Archive: `primis-v54-20261124.tgz` in this lane.
- **Mirror restored byte-exact, twice** (after each boot): post-restore census
  identical to pre-session (1,059 files, 0 diffs). Mirror left stopped, as found.
  Notable: the Nightbreak account token persists outside the EliteMobs folder — the
  restore did not unlink the mirror; a future mirror EM update needs no new link.
- **Diff + classification: DONE** (`diff/DIFF-CLASSIFICATION.md`). The four questions:
  (1) v54's sleep file carries the wake-on-damage block, functionally identical to our
  Nov 21 patch — ours is superseded, v54's ships. (2) Sunken Ship chests: v54 does not
  touch their content; merged == live, D219 safe. (3) D197 files: v54 is byte-identical
  to the pre-edit upstream hashes (23/23) — upstream never touched them; merged ==
  live. (4) Package files: v54 changes only the lockout line (stock now 4320) + the
  meta version line; lockout merged back to 120 everywhere, meta ships at version 54.
  204 differing files → 113 ship, 41 merge to live-identical, 27 restock churn, 24
  mirror-conversion artifacts (blueprint paper-world.yml CRLF-only + game_rules.dat).
- **Packet built + filed.** `packet/files/` = 113 merged files (225,249 B; hashes in
  `packet/packet-files.tsv`), `packet/rollback/` = live bytes of the 112 replaced
  files. All shipped YAML parses clean. INSTALL-PACKET.md rewritten to FILED state
  (5,954 B, lint 5954/272/0, SHA-256 `de55ecaf…`) with provenance gates on the live
  sleep/meta/chest hashes. MANIFEST PENDING stub filed Nov 24, 2026 — the install
  window (failsafe/hand) owns placement; this lane did not touch live.
- **Proof: NOT run in this lane.** The §Proof mirror repro needs a bot-driven
  leash-return/re-sleep/hit session against a mirror carrying the merged set — a
  dedicated test-lane session, beyond this lane's acquisition+filing scope. It stays
  owed (mirror repro after install, or first natural live case), and
  `DRAFT-REPLY-242.md` says so honestly; the draft is updated to the true state
  (acquired, merged, filed; install + proof pending) for Mike's review before posting.

## Proof — how the fleet verifies bosses now wake (the answer owed on #242)

**Mirror repro (primary, do this on the mirror before or alongside the live install):**
the exact scenario of Investigation B (`~/workspace/magmaguy-upstream-20261121/INVESTIGATION-2.md`):

1. On the mirror with the merged v54 set placed, spawn a Primis boss whose file carries
   `primis_sleep` (e.g. a Blood Temple phase-1 boss or the Terravilla alpha razorback).
2. Let it fall asleep (Sleep block: AI off on spawn). Walk the test player into its wake zone
   so it wakes and engages; then pull the player out of leash range until the boss
   leash-returns to its spawn point and falls back asleep (Reset + Sleep: teleported home,
   AI off) **while the player re-enters and stands inside the 8-block wake zone before the
   boss re-sleeps** — the geometry that defeated the zone-enter trigger.
3. With the boss asleep and the player already inside the zone, the player lands one damaging
   hit. **Pass:** the boss wakes (AI on), turns on the player, and fights. **Fail:** the hit
   registers damage but the boss stays AI-off until the player exits and re-enters the zone.
4. Record which boss file, the merged sleep-file hash in place, and the observed result.

**Live observation (the report-back to MagmaGuy):** the first natural live case after install —
a player fighting any sleeping-spawn Primis boss that leash-returns mid-fight; if it re-sleeps
with the player in the zone, the player's next hit must wake it. ExcelProtect logs + the
player's account are the evidence. (This same observation is the proof that has been owed on
our Nov 21 interim patch since it landed; the v54 install closes both.)

## Files in this lane

- `RECORD.md` (this file) · `RECEIPTS.md` · `INSTALL-PACKET.md` · `DRAFT-REPLY-242.md`
- `live/` — verified live pull (1,604 files) · `primis-live-20261124.tgz` — the pull archive
- `diff/live-census.tsv`, `diff/live-file-list.txt` — per-file receipts for the pull
- `diff/LOCAL-EDITS-INVENTORY.md` — local edits + merge rules
- `diff/mirror-primis-census-pre.txt`, `diff/mirror-primis-census-postrestore.txt` — mirror proof
- `v54/`, `merged/`, `packet/` — empty, awaiting the harvest (packet fills per INSTALL-PACKET.md)

## Correction (Nov 24, 2026) — primis_adventure.yml gate re-based

The failsafe window wrv-install-failsafe-20261124-1104 STOPPED at the
provenance gate: INSTALL-PACKET.md required
content_packages/primis_adventure.yml to be ABSENT on live. It is not
absent. Verified firsthand via the Panel API (read-only download) on
Nov 24, 2026:

- live plugins/EliteMobs/content_packages/primis_adventure.yml: 777 B,
  SHA-256 46c175feb4bac07b5bf7dca0e6871551cf25257868d0782f73d8bb4d510e8e31
- cmp against the packet candidate: BYTE-IDENTICAL
- the live file already reads `dungeonLockoutMinutes: 120` (line 21) and
  `dungeonVersion: 20` — the D190 lockout was already in place on live
  (file present since Nov 20, 2026; last modified Nov 22, 2026)

Case: NO PLACEMENT NEEDED for this file. The gate is re-based to the
verified live pre-state (present, 777 B, 46c175fe...); placement is
skip-if-identical (place only if live differs from the candidate at
placement time).

Cause: the "absent / NEW in v54" premise came from the mirror census,
not live — and this lane's own live pull missed the file too:
diff/live-census.tsv and diff/live-file-list.txt contain NO row for
content_packages/primis_adventure.yml (the Nov 24 06:24 CT pull's
file-list lost the line at pack time, the same skip class documented
for the v54 harvest tar). The file sat outside the harvest's verified
scope: the mirror census could not see it, and the live census
silently dropped it. Lesson: an "absent on live" gate needs a positive
live listing check, not census silence.

Changes made:
- INSTALL-PACKET.md: gate corrected to the verified live pre-state;
  placement for this file conditional (skip-if-identical); rollback
  DELETE instruction struck; re-base note added at the top. Corrected
  packet: 8,227 B, SHA-256
  a939029c2f31416ff4b93ef1ea9d7407f10c9914d228131c775f45ed2beb6cab,
  lint 8227/272/0. Every other gate byte-identical.
- packet/rollback/content_packages/primis_adventure.yml: added, holding
  the verified live bytes (46c175fe...); rollback now holds 113 files.
- packet/packet-files.tsv: adventure row reclassed new-package ->
  already-at-target, live_sha256_rollback None -> 46c175fe...; TSV
  SHA-256 now 0de068e555efdd3fbfd2945e2ca8a13fcace16e8912dece6c4ee14ba3add1794
  (113 rows, 225,249 B total, unchanged).
- MANIFEST v54 entry updated to the corrected packet + gate; the
  failsafe's STOPPED note superseded by a RE-BASED note.

Other gates re-verified in the same pass, read-only via Panel API —
all still at gate: powers/primis/primis_sleep.yml bfefef97... (818 B);
content_packages/primis_meta_pack.yml 525b528c... (1,623 B, reads
dungeonVersion: 51, dungeonLockoutMinutes: 120); Sunken Ship chests
875038d9... (647 B) / b8385b73... (647 B) / 450fb6db... (644 B).
