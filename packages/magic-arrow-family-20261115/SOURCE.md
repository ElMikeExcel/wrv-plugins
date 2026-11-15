# SOURCE — Arrow family balance fix (Magic spells.yml, config-only)

Config-only fix, no jar, no code, no source zip. Installed
Nov 15, 2026 by wrv-install-failsafe (scheduled 2026-11-15
09:03 CST run, full-push chain, whitelist on, Homebase lane,
no fallback) as Install 3 of 13. Landing verified PASS in
board run 390 (VERIFY.md in this package: 26/26 script
blocks, 5/5 destinations EXACT, SEVERE 0).

## What the change is

In each of the 5 destination Magic spells.yml files, only
`arrow_damage:` lines inside the real-arrow blocks changed:
new = old / projectile speed, rounded to 2 dp (42 changed
lines total: live 9, stage2 6, stage3 9, stage4 9, stage5 9).
Magic's ProjectileAction writes arrow_damage as vanilla BASE
damage, which vanilla multiplies by impact velocity, so the
arrow ladder landed at ~3–5x config intent (arrow|6 ~58/cast
measured vs the config's balanced 23). Post-fix arrow|6 =
7 physical + 16 payload = 23/cast, Power V bow parity.
arrowrain unchanged (speed 1, value already true).
Diagnosis of record:
~/workspace/spell-audit/ARROW-DOUBLING-INVESTIGATION-20261109.md
(outside this package; cited by INSTALL-PACKET.md).

## Placed files (placed-files/ in this package)

Byte-copies of the installer's post-place pull-backs
(~/workspace/board-r360/r8-arrow-balance/build/
install-20261115/placed-*.yml), renamed by destination.
Placed state per destination (size / SHA-256):

- live-spells.yml   188,297 B /
  96c61a50a75bc41d1906680bedff2b80a09ffece85be7ab97548bf36ac5a8152
- stage2-spells.yml  89,223 B /
  9498948ba474c1868c4ef54a4339604f19b703bc9e6ee1c9f509656de99e4389
- stage3-spells.yml 145,696 B /
  123334d4ce535d9b1ec59a629ee1f6ad93da5c2cc58ee49479b2cf4294ec1406
- stage4-spells.yml 188,297 B /
  3572fc5ef5978314bc39a5261318be2c4f5eb0803eeec26f183f5ee3d5510f36
- stage5-spells.yml 188,297 B /
  96c61a50a75bc41d1906680bedff2b80a09ffece85be7ab97548bf36ac5a8152

Rebase note (VERIFY.md §N1, OUTCOME.txt): the stage-5 flip
landed before this install, so the live destination gated
against the packet's stage5 G1 receipt and received the
STAGED STAGE5 candidate (96c61a50…); stage4 received the
staged live candidate (3572fc5e…). The staged candidates
themselves stay in the build folder
(~/workspace/board-r360/r8-arrow-balance/build/staged/,
hashes in RECEIPTS.md and the build SHA256SUMS); the placed
pull-backs above are the canonical live state.

## Rest of the package

- INSTALL-PACKET.md — the install packet of record
  (5,584 B / 63110e5b…).
- RECEIPTS.md — G1 live-premise receipts (Homebase pulls
  2026-11-14 21:25–21:27 CST) + candidate hashes.
- RESULT.md — the build record (run 360, worker R8):
  premise, derivation, tests.
- build.py / model_test.py — the scripts that produced the
  candidates and the 39-row damage model test.
- diffs/ — 5 per-destination diffs vs the G1 bases; every
  changed line is an arrow_damage line.
- evidence/ — spell logic-test harness outputs (base vs
  staged findings identical: 278, CRITICAL 0), model-test
  output (PASS), local rig boot log (Loaded 591 spells rig
  count, SEVERE 0).
- INSTALLED.txt — the packet-named install marker.
- OUTCOME.txt + SCRIPT-OUTPUT.txt — the failsafe install
  record (26/26 script blocks PASS).
- VERIFY.md — run-390 landing verification (W2).

Not copied into this package (retained at the build folder
path above): the installer's raw working files —
gate-*.yml, post-*.yml, lp-/ph-/vh-*.txt, emptiness and
list captures, latest.log. They are summarised by
SCRIPT-OUTPUT.txt, OUTCOME.txt, and VERIFY.md, and the
placed pull-backs (the load-bearing bytes) ARE included.

## Drive deposit

NOT APPLICABLE — no jar. Drive deposits are keyed to jars
(upkeep B14); a config-only package carries no Drive leg
(SpellBook PR #216 precedent: records package, no deposit).

## Sanitization

No redactions. The copied set was grepped this prep for
IPs / emails / credential patterns: the only IP-shaped
hits are 127.0.0.1 (localhost, rig-boot.log) and a
Temurin version string fragment (25.0.4.1) — both false
positives of the class already dispositioned in the
records-catchup census. No SANITIZATION.diff is needed.
