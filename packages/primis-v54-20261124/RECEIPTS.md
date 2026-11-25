# RECEIPTS — live-premise receipts (G1) for the Primis v54 update lane

Rule: every live-file claim in this lane carries a fresh pull receipt — path, size, SHA-256,
timestamp. Hashes are the gate; sizes are corroboration only.

## The pull

- Source: live server tree `D:\muse-servers\wrv\plugins\EliteMobs` on Mike's PC
  (DESKTOP-IL05DQR), read-only. Live java (pid 29724) was running throughout; no live file was
  written, and no EliteMobs command was run on live.
- Method: PC-side census of every file whose path contains `primis` (excluding `.bak-*`),
  packed with `tar.exe -czf -T <list>` on the PC, downloaded over the Homebase SSH route,
  SHA-256 verified on arrival, then extracted to this lane's `live/`.
- Census count on the PC: **1,604 files**. Extracted count here: **1,604 files**. The tar run
  logged one "Couldn't visit directory" error (a directory entry); the file counts match and
  the archive hash matches on both ends, so the set is complete.
- Archive: `primis-live-20261124.tgz` in this lane folder.
  - PC-side SHA-256: `9B12D96BF20ED5708D80DF9D120B7CBEA9EDBC5A5A22B2B1B0F8E418176C24D2`
  - Downloaded SHA-256: `9b12d96bf20ed5708d80df9d120b7cbea9edbc5a5a22b2b1b0f8e418176c24d2` — match.
  - Download verified: 2026-11-24 06:24:45 CT (archive mtime on arrival 12:24:45 UTC).
- Full per-file census (path, size, SHA-256 for all 1,604 files): `diff/live-census.tsv`.
  File list: `diff/live-file-list.txt`. Extracted tree: `live/`.

## Decision-relevant receipts (all from the verified pull above)

| live path (under `plugins/EliteMobs/`) | size (B) | SHA-256 |
|---|---|---|
| powers/primis/primis_sleep.yml | 818 | bfefef97cd916eaa5df38c69c602371fc653936499397e875d1039e5ad4b9cc2 |
| customtreasurechests/primis/primis_gladius_sunken_ship_chest_1.yml | 647 | 875038d9e4018d948628be2366b1c2d549e5d297f93ba5f09464af910c013d13 |
| customtreasurechests/primis/primis_gladius_sunken_ship_chest_2.yml | 647 | b8385b73528ca8c1bd5552245654a88a593f535a05801dfd0a960863e6ca3b03 |
| customtreasurechests/primis/primis_gladius_sunken_ship_chest_3.yml | 644 | 450fb6db63ae5d5991d20b32c59b773f71361043ff37cccc475faade503bb7a3 |
| content_packages/primis_meta_pack.yml | 1,623 | 525b528c1c8fa069c30911b29918bf32f34578d13109f556b43e7fecba705477 |

- The sleep file's hash equals the file placed by the Nov 21 wake patch, byte for byte — the
  patch is what is live today.
- The three chest hashes equal the D219 staged hashes exactly — the documents fix is what is
  live today.
- All 23 D197 boss files: live hashes equal the post-edit hashes in
  `~/workspace/em-density-c-packet-20261122/hashes.tsv`, 23 of 23 (rows in `diff/live-census.tsv`).
- All 16 Primis `content_packages/*.yml` files read `dungeonLockoutMinutes: 120` in this pull
  (grep over the extracted tree, 2026-11-24); `primis_meta_pack.yml` reads `dungeonVersion: 51`.

## v54 harvest (2026-11-24, ~10:50–11:00 CT — acquisition run after Mike's approval)

- Pre-session snapshot of the mirror's whole `plugins\EliteMobs` folder (this run):
  `C:\Users\MikeExcel\muse-tools\mirror-em-presnap-20261124-v54.zip`, 13,776,354 B,
  SHA-256 `602F120AF0583ECB3C9D6ED47048F77E9D8A383CB57FB34B2A302F67893D787C`;
  primis census 1,059 files (`diff/mirror-primis-census-v54-pre.txt`).
- Mirror booted 10:50 CT; RCON `/nightbreaklogin` (via the mirror's own
  `rcon-mirror.py`, port 25511) minted `https://nightbreak.io/server/link/GEXM-AE5G`
  at 10:52:22 CT. Opened in Mike's PC Chrome (CDP): the page was already signed in
  as Michael Reyes (Patreon session), showed server "WRV TEST - NOT LIVE" and the
  matching code GEXM-AE5G; "Link this server" clicked → page: "Server connected —
  Your server has its token and can download your content now." No credentials
  were entered anywhere; no 2FA appeared.
- RCON `/em updateallcontent` ~10:54 CT: EM reloaded with "Account token loaded
  successfully!" and imported the content. On-disk proof:
  `content_packages/primis_meta_pack.yml` → `dungeonVersion: 54 # Sleeping bosses
  now wake up when hit`. (The mirror's DungeonRig auto-session stopped the server
  cleanly at 10:54:59, SERVER-EXIT 0, after the import.)
- Harvest: PC-side census of primis files (1,603 incl. one recovered below),
  packed `primis-v54-20261124.tgz` on the PC — PC SHA-256
  `0473AB2280DC65E1B977960B9E289C2796CFB5CD7840FCD3D9E2A5AA168FD317`,
  downloaded SHA-256 identical, 27,045,617 B; extracted to `v54/` (archive kept in
  this lane). Per-file census: `diff/v54-harvest-census.tsv`.
- One-file recovery: the tar file-list's first line carried a UTF-8 BOM, so
  `content_packages/primis_adventure.yml` (778 B) was skipped by tar. Recovered by
  a second mirror boot + `/em updateallcontent` (the Nightbreak account token
  persists outside the EliteMobs folder — no re-link needed) and a direct copy;
  SHA-256 `DD3A885FC520174AFF801D29F3047732E489387B7C672C85DB0F7846C108E976` —
  exactly the harvest-census value. Final v54 set: 1,603 files, every file
  hash-verified against the harvest census (1,603/1,603).
- Mirror restoration (done twice, after each boot): stopped cleanly (SERVER-EXIT
  0), EliteMobs folder replaced from the pre-session snapshot zip (hash
  re-verified `602F120A…` at restore), post-restore census 1,059 files —
  **byte-identical to the pre-session census both times**
  (`diff/mirror-primis-census-v54-postrestore.txt`, `…-postrestore2.txt`).
  Mirror left stopped, as found. Live was never touched by this run.

## Mirror receipts (v54 acquisition attempt — prep lane, superseded by the harvest above)

- Mirror: `C:\Users\MikeExcel\wrv-testserver\server` on the same PC (Paper 26.3-245, port
  25566, RCON 25511). Pre-session snapshot of its whole `plugins\EliteMobs` folder:
  `C:\Users\MikeExcel\muse-tools\mirror-em-presnap-20261124.zip`, 13,686,908 B,
  SHA-256 `433E4A40057AACA321FA3EA164F1B1610A0A9838CE83B3062C5544CD57D90E83`
  (hash re-verified at restore time — same value).
- Mirror boot log, 2026-11-24 06:33:59 CT: `[EliteMobs] Parsed 60 content versions from
  Nightbreak API` and `Content [000-020] The Primis Adventure! is outdated! … Your version: 0 /
  remote version: 54` — independent confirmation that v54 is the current remote Primis version.
- `/em updateallcontent` via mirror RCON, 2026-11-24 ~06:34 CT, verbatim response:
  `[EliteMobs] This server is not linked to a Nightbreak account. Run /nightbreaklogin first.`
- `/nightbreaklogin` via mirror RCON minted a one-time link (log, 06:34:56 CT):
  `https://nightbreak.io/server/link/GTFU-Z3XE`, code `GTFU-Z3XE`, "expires in 30 minutes"
  (≈07:04 CT). Not completed — completing it needs a nightbreak.io sign-in in a browser
  (Mike's account signs in via Patreon). Codes are cheap to re-mint (one RCON command after a
  mirror boot), so the expiry costs nothing.
- Mirror restoration: mirror stopped cleanly (SERVER-EXIT 0, 06:36:22 CT), EliteMobs folder
  replaced from the snapshot zip, and a post-restore census (`diff/mirror-primis-census-postrestore.txt`,
  1,059 primis files) compared against the snapshot's own contents: **0 missing, 0 extra,
  0 hash mismatches** (3 zip directory entries account for the raw count difference). Mirror
  left stopped, exactly as found. Pre-update mirror census kept at
  `diff/mirror-primis-census-pre.txt`.
