# RECEIPTS — BB-02 ExcelBrainGames 0.3.4 (Back-button pass, run 354)

All timestamps America/Chicago (CST), Sat Nov 14, 2026.

## G1 live-premise receipt (Homebase route, no fallback)

- Probe: `ssh -F ~/.ssh/config -o ConnectTimeout=20 mike-pc "hostname"`
  attempt 1 at 16:54 CST → `DESKTOP-IL05DQR`, exit 0. PASS on
  attempt 1 of 3. Last successful probe: this run, 16:54 CST.
- Pull: rclone on the PC from remote `shockbyte:1. Michael's Server`
  into PC staging `muse-temp\g1-r354-bb02`, then scp down to
  `pull-g1/` in this folder. Pull completed 16:55 CST.
- Live path: `plugins/ExcelBrainGames-0.3.3.jar`
- Pulled copy: `pull-g1/ExcelBrainGames-0.3.3.jar`
- Size: 135,705 B
- SHA-256: 64e93e17f4d3fdd14acd9284b874902001188f9eaedf3687d83ef328589d6de1
- Verdict: EXACT match to the audited 0.3.3 jar of record
  (brain-games/build-0.3.3 BUILD-RECORD.md). Live IS the audited
  version and tree — the §3 base-gate clears with no re-base.
- Census (pull-g1/plugins-braingames-lsf.txt, rclone lsf of the live
  plugins dir filtered to BrainGames): exactly ONE active jar,
  `ExcelBrainGames-0.3.3.jar`; held-asides
  `ExcelBrainGames-0.1.0.jar.bak-20261105-pre030`,
  `-0.3.0.jar.bak-20261105-pre031`, `-0.3.1.jar.bak-20261107-pre032`,
  `-0.3.2.jar.bak-20261107-pre033`; plus the `ExcelBrainGames/` data
  folder. No 0.3.4 jar exists live.
- PC cleanup (PC cleanup rule): staging dir `g1-r354-bb02` and
  `pull-bb02.ps1` deleted from the PC after the pull; verified
  Test-Path = False / False.

## Candidate

- Artifact: `~/workspace/brain-games/build-0.3.4/ExcelBrainGames-0.3.4.jar`
- Size: 136,140 B
- SHA-256: 52d498e720b00a65cd9de5878e046d870d1fe140db399055338a91d46dc329d3
- Build: EXC-BRAINGAMES-0.3.4-WRV-20261114-01-43bf6f
- Tests: 1,924,741 checks / 0 failures across two full build.sh runs
  (carried suite at exactly its 0.3.3 counts, 1,924,715; new
  BackNavTest 26). Jar byte-identical across both runs and across
  each run's internal double assembly.
- Packs (untouched by this build, regenerated + validated by
  build.sh): java zip 05942eed39e0eed0e7bfc9538641c033fda420c5019484fef768e81ef20bb0d4 /
  bedrock mcpack 486bcad6e1452ffa8e6e83e267112d6edad693c2de68f4e1057c3bc9c2221286
  — 0.3.3's recorded hashes.
- Build folder sums: `~/workspace/brain-games/build-0.3.4/SHA256SUMS`
  (1,296 entries, `sha256sum -c` all OK at staging time).

## Rig (mirror, this folder's rig/)

Two boots on Paper 26.3-235 from scratch copies of the 0.3.3 rig
servers with the 0.3.4 jar swapped in (old plugin data folder
removed, so each boot generates fresh defaults):

- Boot A (no Floodgate), rig/boot-034.log: enable line
  `ExcelBrainGames 0.3.4 enabled: ... platform-detection=unavailable
  (Java presentation for all), build=EXC-BRAINGAMES-0.3.4-WRV-20261114-01-43bf6f`;
  `Done (56.647s)`; SEVERE count 0 (grep -c).
- Boot B (Geyser + Floodgate), rig/boot-034-floodgate.log: bind line
  `floodgate detected`; enable line `platform-detection=floodgate`,
  same build id; `Done (33.976s)`; SEVERE count 0 (grep -c).

## Desk walk

The 13-row screen × platform walk (Back slot, action, landing,
mid-round save semantics per row) is tabulated in
`~/workspace/brain-games/build-0.3.4/TEST-REPORT.md` (Desk walk
section) and pinned by BackNavTest's 26 checks. Real-client tap
walks are the packet's player proof, not desk evidence.
