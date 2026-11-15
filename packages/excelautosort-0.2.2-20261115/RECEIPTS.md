# BB-04 ExcelAutoSort 0.2.2 — RECEIPTS

## G1 live-premise receipt — BLOCKED (gate OPEN, not satisfied)

Claim needing a receipt: live jar is ExcelAutoSort 0.2.1
(plugins/ExcelAutoSort-0.2.1.jar), the lineage this build rebases.

- Homebase probes this run (SSH alias mike-pc, via the runtime
  Tailscale relay): 7 attempts, all failed with
  "kex_exchange_identification: read: Connection reset by peer /
  Connection reset by 198.19.0.1 port 3128", exit 255.
  Round 1: 2026-11-14 16:51:04, 16:51:53, 16:52:42 CST (3 attempts
  over ~2 min, the lane's retry pattern). Round 2: 16:54:05 CST.
  Round 3: 17:03:01, 17:03:42, 17:04:22 CST.
- Per the Nov 11 alert-clarity rule: this is evidence about the
  relay path only; the PC's own state is unknowable while the
  probes fail. Last successful Homebase probe on record:
  ~4:40 PM CST, Sat Nov 14, 2026 (board-manager run 353, W2
  attempt-1 PASS).
- FALLBACK (panel) receipt: NOT taken. The panel route needs a
  live-browser session, which this desk worker cannot operate;
  the parent/supervisor lane must take the labeled FALLBACK
  (panel) receipt (panel File Manager download of
  plugins/ExcelAutoSort-0.2.1.jar + local SHA-256) or re-run the
  Homebase pull when the relay recovers, BEFORE this packet is
  filed in MANIFEST.md.
- Lineage (NOT G1 evidence, recorded for the re-pull to confirm):
  build-0.2.1/INSTALLED.txt (Nov 10, 2026) records the live swap
  to ExcelAutoSort-0.2.1.jar, 159,240 B / SHA-256
  48a5c0fed431fc9853e0e92ff199b24887ab18485a5febb4132735759617e266,
  panel Compute checksum EXACT pre-boot. The three GUI files this
  unit edits are byte-identical between the audited src/ tree
  (0.2.0 lineage) and the build-0.2.1 tree (diff -rq, this run).

## Candidate build receipt

- Tree: ~/workspace/back-button-wave1-r354/BB-04/build/ExcelAutoSort-0.2.2/
  (copy of autosort-plugin/build-0.2.1; only SelectorGui.java,
  MaxOrgGui.java, BedrockFormsHook.java, BuildInfo.java,
  config.yml, plugin.yml, local-build.sh, AutoSortTest.java differ)
- Build: bash local-build.sh — JUnit 35 found / 35 successful /
  0 failed; double-build byte-identical; fingerprint gate PASS.
- Build ID: EXC-AUTOSORT-0.2.2-WRV-20261114-01-8fdbf2
- Candidate jar: ExcelAutoSort-0.2.2.jar — 160,197 B / SHA-256
  8b3bf5ab3b07119909e8f371361a21f32235b17039eb0f53b987f514fa18ad28
- Source zip: ExcelAutoSort-0.2.2-source.zip — 100,207 B / SHA-256
  c2ff6c453264c810e5fc340ade2fac0a32a940f30b958fa0427565733ca288fe
- Candidate config.yml: SHA-256
  0eba2b583cb2be54bfa74f344c1a0c5917877e72ce1b91b423902efb16988ed2

## Rig receipt (local Paper rig, BB-04/rig/server, port 25654)

- Boot log: BB-04/rig/server/boot.log
- "[ExcelAutoSort] Enabling ExcelAutoSort v0.2.2" present.
- Awake line verbatim: "ExcelAutoSort 0.2.2 awake: sort parity
  engine on (Barrel, Chest, Ender Chest, Shulker Box), filter
  chests 0, recovery entries 0, protect sink BOUND, gp rights
  BOUND, max-org on (economy UNBOUND, entitlements 0), build
  EXC-AUTOSORT-0.2.2-WRV-20261114-01-8fdbf2"
- "Done (86.512s)" present; SEVERE count in boot.log = 0.
- Console-command smoke not usable on this rig: piped console
  commands threw a Paper-internal NullPointerException in
  net.minecraft.commands (no ExcelAutoSort frames) — noted, not
  counted against the candidate.
- Vault absent on the rig (expected): MaxOrg purchases refuse,
  network view works for owners — same line as the 0.2.1 rig.

## Mirror walk receipt (code-trace, scripted)

- Script: BB-04/rig/walk-check.sh; output: BB-04/rig/WALK-OUTPUT.txt
- Result: WALK-TOTAL pass=25 fail=0 (25 render/handler/landing
  anchors across the 12 audit rows, all present in the 0.2.2 tree)
- A live mirror walk (Homebase test-server mirror, Java + Bedrock
  clients) was NOT run: the Homebase relay was down for this
  whole run (see G1 above). Player proof remains owed after
  install per the packet.

## G1 RECEIPT (supervisor re-dispatch)

Re-dispatch context: board-manager run 354. The supervisor
reported a Homebase probe PASS at ~17:08 CST Nov 14, 2026
(attempt 1, DESKTOP-IL05DQR), after the relay was down
16:51-17:06 CST. This worker re-ran the pull immediately after.

- Route attempted: Homebase (SSH alias mike-pc,
  HostName 100.72.156.1, ProxyCommand ts-connect.py via the
  runtime Tailscale relay; on-PC rclone remote shockbyte:,
  root "1. Michael's Server", target
  plugins/ExcelAutoSort-0.2.1.jar).
- Probe result: FAIL — relay down again. Full retry pattern
  run (5 attempts over ~4 min, exceeding the required
  3-probe/~2-min pattern):
  - 2026-11-14 17:09:25 CST — "kex_exchange_identification:
    read: Connection reset by peer / Connection reset by
    198.19.0.1 port 3128", exit 255
  - 2026-11-14 17:10:17 CST — same reset, exit 255
  - 2026-11-14 17:11:18 CST — same reset, exit 255
  - 2026-11-14 17:12:29 CST — "Connection closed by
    198.19.0.1 port 3128", exit 255
  - 2026-11-14 17:13:30 CST — same reset as attempt 1, exit 255
- Live path: plugins/ExcelAutoSort-0.2.1.jar (attempted; NOT pulled)
- Size: NOT OBTAINED (no connection; no bytes transferred)
- SHA-256: NOT OBTAINED
- Pull timestamp: NONE — no pull completed; last probe
  2026-11-14 17:13:30 CST
- plugin.yml version: NOT CONFIRMED (jar not obtained)
- Per the Nov 11 alert-clarity rule: these failures are
  evidence about the relay path only; the PC's own state is
  unknowable while the probes fail. The supervisor's ~17:08
  CST PASS shows the relay is flapping, not stably back.
- PC-side temp files: none created (never connected); nothing
  to clean up.
- Retained jar: NONE — ~/workspace/back-button-wave1-r354/BB-04/g1/
  created empty; no live jar retained.
- Gate state: G1 remains OPEN / NOT SATISFIED. Lineage
  expectation (159,240 B / 48a5c0fe...) is neither confirmed
  nor contradicted by this attempt. Filing remains blocked
  until a fresh Homebase pull (on a stable probe) or a labeled
  FALLBACK (panel) receipt is taken.

## G1 FALLBACK (panel) RECEIPT — supervisor leg, Nov 14, 2026, 5:15–5:16 PM CST

Route: Shockbyte panel File Manager (read-only, live browser), Home > plugins. Homebase route was flapping (probes reset 17:09–17:13 CST) so this leg used the labeled panel fallback per PACKET-TEMPLATE.md.
- Live path: plugins/ExcelAutoSort-0.2.1.jar
- Size as displayed: 155.51 KB (consistent with lineage 159,240 B; panel display is rounded — byte-exact size NOT obtained)
- Last modified as displayed: Nov 10, 12:31 AM
- SHA-256: NOT OBTAINED via panel listing (Download action exists in the panel; not exercised)
- Gate state: live premise CORROBORATED (filename + version + size class + mtime all match lineage 0.2.1 / 159,240 B / 48a5c0fe…); byte-exact size + SHA-256 remain owed at the filing gate from a fresh Homebase pull when the relay is stable. Filing waits for BB-01 regardless.

## G1 RECEIPT (supervisor G1 dispatch, Nov 14)

Re-dispatch context: board-manager run 354, combined BB-01 + BB-04
G1 pull (second re-dispatch for BB-04). Supervisor reported a
Homebase probe PASS at ~17:21 CST Nov 14, 2026. This worker
probed from 17:23:32 CST.

- Route attempted: Homebase (SSH alias mike-pc, HostName
  100.72.156.1, ProxyCommand ts-connect.py via the runtime
  Tailscale relay; on-PC rclone remote shockbyte:, root
  "1. Michael's Server", target plugins/ExcelAutoSort-0.2.1.jar).
- Probe outcome: FAIL — relay down for the whole window.
  33 probes, 2026-11-14 17:23:32–17:33:55 CST, all exit 255:
  32x "kex_exchange_identification: read: Connection reset by
  peer / Connection reset by 198.19.0.1 port 3128", 1x
  "Connection closed by 198.19.0.1 port 3128" (17:28:24 CST).
  The 3-probe/~2-min pattern was run and exceeded many times
  over. Full probe log: ../G1-PULLS-RESULT.md.
- Live path: plugins/ExcelAutoSort-0.2.1.jar (attempted; NOT pulled)
- Size: NOT OBTAINED (no connection; no bytes transferred)
- SHA-256: NOT OBTAINED
- Pull timestamp: NONE — no pull completed; last probe
  2026-11-14 17:33:55 CST
- Lineage comparison: expectation 159,240 B / SHA-256
  48a5c0fed431fc9853e0e92ff199b24887ab18485a5febb4132735759617e266
  — **does NOT match-check: neither confirmed nor contradicted**;
  no live bytes were obtained, so no exact-match statement can
  be made. The labeled FALLBACK (panel) receipt above (155.51 KB
  displayed, mtime Nov 10 12:31 AM) remains the standing
  corroboration.
- Retained pull: NONE —
  ~/workspace/back-button-wave1-r354/g1-pulls/excelautosort-jar/
  is empty. PC-side temp files: none created (never
  connected); nothing to clean up.
- Per the Nov 11 alert-clarity rule: relay-path evidence only;
  the PC's own state is unknowable while the probes fail.
- Gate state: BB-04 byte-exact G1 remains OPEN / NOT SATISFIED.

## G1 BYTE-CLOSED — FALLBACK (panel) download, supervisor leg, Nov 14, 2026, 5:35–5:37 PM CST

Downloaded via the Shockbyte panel (live browser) after the Homebase relay stayed down: plugins/ExcelAutoSort-0.2.1.jar — 159,240 B / SHA-256 48a5c0fed431fc9853e0e92ff199b24887ab18485a5febb4132735759617e266 — EXACT match to the lineage expectation. Retained: g1-pulls/autosort/ExcelAutoSort-0.2.1.jar. BB-04 G1 gate is CLOSED (labeled FALLBACK (panel) receipt per PACKET-TEMPLATE.md).
