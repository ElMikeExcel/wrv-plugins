# RECEIPTS - ExcelDowned 1.0.5 + ExcelMagicGate 0.3.5 (wand-loss hardening pair)

Filed Nov 24, 2026 with INSTALL-PACKET.md. Lane record:
~/workspace/wand-hardening-20261124/RECORD.md (mechanism s.1 + s.6,
mirror evidence s.7, net limitation s.8, restoration s.10).

## Candidates (hash-verified at filing)

- ExcelDowned-1.0.5.jar - 143,021 B
  SHA-256 233d20b8fd6c099f0ee22fa9c3974aca837c1ca37fa0889354e8ece0dbf17f09
  Build EXC-DOWNED-1.0.5-WRV-20261124-03-deathdefer; tests 87/87;
  double-build byte-identical; fingerprint gate PASS.
- ExcelMagicGate-0.3.5.jar - 255,175 B
  SHA-256 1e26c13ea6995bc490524763efceb3980a24357f0379df6844844d08909dfdf1
  Build EXC-MAGICGATE-0.3.5-WRV-20261124-02-wandnet; tests 325/325
  with fixtures (302 without); double-build byte-identical.
- ExcelDowned-1.0.5-source.zip
  SHA-256 ddcbfdc96d0fc537f33c9f0947c925d9d6ab21c512572d491c66091a9b29a034
- ExcelMagicGate-0.3.5-source.zip
  SHA-256 b0ad8627da07b3df58505fbcb5e188cc751d3376fd268c2bf020a89da1205c3b
- Superseded, do NOT install: ExcelDowned 1.0.5 -02-marklast build
  (SHA-256 50bd6b890ce5089a0bf79eca64c03d80678c4a6a1ba0f5c2458e0378d952d174)
  - mirror run B2 proved mark hygiene alone still loses the wand.

## Live baselines (verified by this lane Nov 24, 2026, ~04:37 CT)

- ExcelDowned-1.0.4.jar - 141,173 B
  SHA-256 C4C90D62... (prefix recorded at lane verification; the
  0.3.4 packet's receipts carry the same gate convention)
- ExcelMagicGate-0.2.0.jar (internal 0.3.4) - 252,773 B
  SHA-256 624f6f00193dcdda36d92c5bcb1c510ddcc4e8993a706dfaf9ece89b016e1eae
- Magic fork Magic-11.2.5-wrvnpe1.jar - 7,662,464 B
  SHA-256 prefix B78F03B8 (copy kept at
  ~/workspace/wand-hardening-20261124/evidence/Magic-11.2.5-wrvnpe1-live.jar)
- Live Downed config: bleed-out 300, wand-owner-guard.unmark-on-quit
  true.

## Mirror proof receipts

- Evidence root: ~/workspace/wand-hardening-20261124/evidence/
  - repro-unpatched/ - run B: after-kill playerdata (wand present,
    owner clean) + after-rejoin playerdata (wand gone), bot logs,
    server-log-full.txt, downed-after-kill.yml.
  - repro-patched/ - run B2 (mark-hygiene build): identical loss.
  - repro-patched-v2/ - run B3 (final pair): playerdata-after-kill,
    session-b3-botlog.txt, server-log.txt - wand in every dump,
    death at +5 s after join.
  - session-net2-botlog.txt + server-log-net2.txt - the NET attempt:
    "Failed to load bound wand" rejections (substrate limit, s.8);
    also captures a wand given while downed being stamped with the
    marked owner name, and a deferred death carrying a full 25-item
    inventory (0 dropped, 25 carried).
  - substrate/ + substrate-net/ - the transplanted playerdata/mage
    files used (bot UUID 1d200ff8-56d4-3c74-8502-d6cd33a86548).
- Mirror restored to as-found state and left stopped (RECORD.md s.10).

## The two losses (why this exists)

- Incident 1, Nov 23, 2026: downed, logged out downed; mage save held
  the stored wand copy with the downed-marked owner. Return login:
  session expired, finalizeDeath ~1 s after login; death snapshot
  (7 dropped, 8 carried) had no stick. Restored by surgery Nov 23.
- Incident 2, Nov 24, 2026: downed, node hard-killed; rejoin
  re-entered downed, bleed-out finalized 1.7 s after join; wand
  absent from the first dump and the death snapshot. Mage record
  CLEAN (no marked owner) - the loss the mark theory could not
  explain. Restored by surgery Nov 24 (~04:09-04:11 CT).
