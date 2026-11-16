# RECEIPTS - ExcelProtect 0.1.12 G1 live-premise pulls (run 432)

Desk work only: read-only pulls, no installs, no live changes,
no MANIFEST edits. Pull date: Mon Nov 16, 2026 (America/Chicago).

## Homebase probe (retry pattern, pinned form only)

- Attempt 1: 2026-11-16 12:03:01 CST -
  ssh -F ~/.ssh/config mike-pc hostname
  -> DESKTOP-IL05DQR, exit 0. PASS on attempt 1
  (attempts 2-3 not needed).
- Last-successful-probe: 2026-11-16 12:03 CST.
- Fallback used: NO - Homebase route healthy. No receipt
  below is a FALLBACK (panel) receipt.

## Route

- Homebase - SSH mike-pc (DESKTOP-IL05DQR, -F ~/.ssh/config
  on every ssh/scp call), on-PC rclone copyto from the
  shockbyte: remote into PC staging
  C:\Users\MikeExcel\muse-temp\r432-protect\, hashed on PC
  (Get-FileHash SHA-256), SCP'd to pulled/ here and
  re-hashed locally. PC and local hashes AGREE on both files.
- Pull window (PC clock): lsf exit 0; copyto exits 0 x2;
  PC timestamp 2026-11-16 12:03:38 CST (-06:00); local
  re-hash verified 2026-11-16 12:03:57 CST.
- PC cleanup: staging folder + muse-temp-pull-r432.ps1
  deleted, verified gone (Test-Path = False x2).
- Pull script retained VM-side: work/pull-r432.ps1;
  pull output: work/pull-output.txt.

## Receipts (per file)

### PULL-01 live jar - ExcelProtect (THE gated file)
- Live path: plugins/ExcelProtect-0.1.11.jar
  (remote: shockbyte:1. Michael's Server/plugins/
  ExcelProtect-0.1.11.jar)
- Local copy: pulled/ExcelProtect-0.1.11.jar
- Size: 303,587 B
- SHA-256: ba10bbcd84de50f199f78b1e09d354ee189f8919ba9732d654ed2a4bcc94260f
- Pull timestamp: 2026-11-16 12:03 CST
- Expected (task premise): 303,587 B / ba10bbcd... (0.1.11)
- Verdict: EXACT - live jar hash match YES. No STOP.

### PULL-02 live config - ExcelProtect (attempts toggle state)
- Live path: plugins/ExcelProtect/config.yml
- Local copy: pulled/config.yml
- Size: 16,002 B
- SHA-256: c17204010170fe09d67bfeea348e89f50da071e3526bc834ee1897af69c98a9b
- Pull timestamp: 2026-11-16 12:03 CST
- Attempts state (grep on pulled file, script output):
  line 112 "enabled: true" under log.attempts (line 104);
  line 116 coalesce-ms: 2000; line 122 world-prefix: "em_";
  line 123 worlds: []. So attempts is ON live, scope em_.
- Note: this hash equals the 0.1.12 candidate config hash
  (BUILD-RECORD: config byte-identical 0.1.11 to 0.1.12,
  c1720401... both trees). No config change in this packet.

### PULL-03 live plugins listing - ExcelProtect census
- Source: rclone lsf of plugins/ at 2026-11-16 12:03 CST,
  Homebase, exit 0. Local copy: pulled/plugins-lsf.txt
  (19,021 B / 446169bc991c276f1894d63ebf3cd8bd30fc161b39838339a10b9a71fa50a0b6).
- ExcelProtect entries (13 lines): folder ExcelProtect/
  plus exactly ONE loadable jar, ExcelProtect-0.1.11.jar;
  all other ExcelProtect files are inert .held/.bak copies
  (0.1.0 .held-20261104-pre011, 0.1.1 .held-20261105-pre012,
  0.1.2 .held-20261105-pre013, 0.1.3 .held-20261105-pre014,
  0.1.4 .held-20261106-pre015, 0.1.5 .held-20261106-pre016,
  0.1.6 .bak-20261106-pre017, 0.1.7 .bak-20261108-prebosspair,
  0.1.8 .bak-20261114-pre019, 0.1.9 .bak-20261115-pre0110,
  0.1.10 .bak-20261115-pre0111).
- Active-jar count (regex ^ExcelProtect-[0-9].*\.jar$) = 1.
- Verdict: EXACT vs premise - no second active jar.

## Candidate (desk-side, not a live claim)

- ExcelProtect-0.1.12.jar at
  ~/workspace/board-r432/protect-build/ExcelProtect-0.1.12.jar
  - 304,634 B / SHA-256
  6e3cf3981f38f92d3489bdde60b21f1c44ab202c26a55b70b57f9b5200d51e4b
  (re-hashed locally this run, matches BUILD-RECORD).
- Build: EXC-PROTECT-0.1.12-WRV-20261116-01-0d1a12;
  tests 125/125; double-build byte-identical.
- Rig: RIG PASS (~/workspace/board-r432/protect-rig/RIG.md);
  instance em_id_* leg NOTRUN on mirror, rides live re-proof.
