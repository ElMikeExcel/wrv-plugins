# G1 RECEIPTS - ExcelExpansions 0.1.1 filing prep (lane W5, run 335)

Pull date: Sat Nov 14, 2026, 05:20-05:22 CST (America/Chicago).
Lane: PC route (rclone copyto on the PC from shockbyte remote
"1. Michael's Server" into PC staging muse-temp\g1-w5-exp011,
then scp down to this folder). No fallback used. No panel touched,
no MANIFEST edit, no install.

## Probe log (PC route, mandatory invocation)

- Attempt 1 - 2026-11-14 05:20:56 CST:
  ssh -F ~/.ssh/config -o ConnectTimeout=20 mike-pc "hostname"
  -> DESKTOP-IL05DQR, exit 0. PASS on attempt 1 of 3.
  No further attempts needed. Last successful probe: this run.
- Pull run: 05:21:24-05:21:43 CST on the PC (rclone), 3/3 file
  pulls OK, 0 PULL-FAILED lines; 2 listings OK + 1 expected error
  (ExcelExpansions folder absent, below).
- Hash integrity: PC-side Get-FileHash values in HASHES.txt match
  the locally computed SHA-256 for every pulled file (byte-identical
  transport). One cosmetic glitch: HASHES.txt could not hash itself
  on the PC (file in use); its local hash is recorded below.
- PC cleanup (PC cleanup rule): staging dir g1-w5-exp011 and
  pull-w5.ps1 deleted from the PC after the pull; verified
  Test-Path = False x2.

## Receipt 1 - ExcelExpansions ABSENT live (NEW plugin premise)

- Census: plugins-top.txt (live plugins/ listing, this pull) =
  16,754 B / 6b66cd2cec6c50b702b56a57241e0d37857e009303c6bc1e43d
  243af6b0e959b - 476 entries. grep for "ExcelExpansions" (any
  case): 0 hits. No ExcelExpansions jar, no ExcelExpansions folder.
- Direct probe: rclone lsf "plugins/ExcelExpansions/" returned
  ERROR "error listing: directory not found" (rclone exit 3),
  retained verbatim in excelexpansions-lsf.txt (775 B /
  ee6d5b7d9f46a40cfec7f5e14a0119d827220c9def2d475b1f76740b4d78853b).
- latest.log plugin list (127,401 B /
  0764bb803e670f644ea04b8c0c3253da6bbecafccb6ed8767395528d637a410f)
  contains no ExcelExpansions entry. Verdict: ABSENT - proven.
  There is no live predecessor jar; no live-jar swap gate applies.

## Receipt 2 - Live plugin count

- Source: plugins-top.txt above. Entries: 476 lines total;
  loadable jars (filename ending exactly .jar): 87.
  ExcelExpansions would make 88 after install.

## Receipt 3 - EM player_data.db (level-store premise)

- Live path: plugins/EliteMobs/data/player_data.db
- Pulled copy: plugins__EliteMobs__data__player_data.db =
  28,672 B / 541cebeb2d72db1e316fb9dbb0fd6c8b6b5ef6b1ffca03ecf3c4
  5e1785edbe3e (PC hash = local hash).
- This is the POST-WIPE database: the hash equals the sealfreeze
  window record's post-swap live DB (541cebeb..., Nov 14 04:28 CST).
- Read-only sqlite check on the pulled copy (this lane): table
  PlayerData, 31 columns, 11 rows; SUM and MAX of
  HighestLevelKilled = 0, Score = 0, Kills = 0, CurrencyV2 = 0.0.
  Premise: ExcelExpansions' level store (config level-store.em-db-
  path "EliteMobs/data/player_data.db", band signal = the named EM
  field HighestLevelKilled + Score) seeds from post-wipe zeros.

## Receipt 4 - GuildGate state (guild LOCKED 4 of 5 premise)

- Live path: plugins/ExcelGuildGate/state.yml
- Pulled copy: plugins__ExcelGuildGate__state.yml = 1,015 B /
  79e4470255edc5522db555e6ee935c301a5b74776fed45f3aa1f45220879b4c0
  (PC hash = local hash).
- Content (verbatim key lines): "unlocked: false"; seals broken:
  ender_dragon true, wither true, elder_guardian true, raid true;
  warden broken: false. That is 4 of 5 broken, guild LOCKED.
- Folder listing retained: guildgate-top.txt (331 B /
  6a5f3f7d37626d14fabb98832cf0abdef9a7c3d37df857300839a580f5557ecc)
  - state.yml present at the path above.
- Corroboration from latest.log (this pull): "[ExcelGuildGate]
  ExcelGuildGate enabled: 4 of 5 seals broken, guild LOCKED" and
  "Enabling ExcelGuildGate v0.4.0".

## Files retained in this folder

- plugins-top.txt - live plugins/ census (Receipts 1-2).
- excelexpansions-lsf.txt - the directory-not-found error (Rec. 1).
- plugins__EliteMobs__data__player_data.db - live EM DB (Rec. 3).
- plugins__ExcelGuildGate__state.yml - live GuildGate state (Rec. 4).
- guildgate-top.txt - live ExcelGuildGate folder listing (Rec. 4).
- logs__latest.log - live boot log corroboration (Rec. 1 + 4).
- HASHES.txt - PC-side hashes (661 B; local SHA-256
  bd07a8d2411b10e83ce5c1fbb20a3b446640d446ed6a744e78153d43b0ff9c68).
