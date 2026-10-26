# ExcelWhatsNew 0.1.1: test results (box, Oct 26, 2026, about 9:18 to 9:22 AM CDT)

Jar: ExcelWhatsNew-0.1.1.jar sha256 ca35e86d473913d005a66ccc8b7a45be2be65478c9ebaecd85f5d2d24ac15cd5 (30,471 B).
Built twice from src/: byte-identical both times. plugin.yml and config.yml inside the jar YAML-parse
(version 0.1.1, text.newer-button 'Newer news').

## Base check (before building)
- Live jar per Big Brain: ExcelWhatsNew-0.1.0.jar 691ee97a... (P4 readback Oct 25, 10:47 AM CDT). No
  later ExcelWhatsNew jar change in Big Brain or any Oct 26 packet; the Oct 26 1 AM plugin audit
  lists ExcelWhatsNew-0.1.0.jar.
- `src-0.1.0/build.sh` rebuilds 691ee97a byte for byte (cmp identical). 0.1.0 unit checks 52/52.

## Unit checks: 86 passed, 0 failed (`src/build.sh`, log evidence/unit-tests-0.1.1.out)
- All 52 old checks still pass.
- 34 new: Newer only when idx > 0; Newer opens /whatsnew idx; Older still idx+2; Bedrock button
  order for newest pending, newest after OK, middle, oldest, pending middle, single entry; click id
  mapping (0/1/2 per layout, -1 and out of range do nothing; pending-newest ids unchanged from 0.1.0);
  Java row order; exhaustive Java/Bedrock parity over show-count 1..10, every idx, pending yes/no;
  typed hint both directions; list hint for 1, 2, 10 entries and no "(or 3)"; no dashes; default
  config has the key; live config 62b95a5e has no key and show-count 10; code default present.

## Box rig (Paper 26.3-214, Geyser + Floodgate, /workspace/whatsnew-rig)
Loaded with the LIVE config.yml (62b95a5e, no newer-button key) and the LIVE changelog.yml (0091bc72,
8 entries). Boot: "ExcelWhatsNew 0.1.1: 8 news entries (newest 2026-10-26-mend-mana)", 0 SEVERE, no
ExcelWhatsNew warnings, config.yml still 62b95a5e after boot (not rewritten).

Java (mineflayer, tests/rig/javanewer.js): 12/12 (evidence/java-newer.out)
- Newest: [OK] + [Older news] (/whatsnew 2), no Newer.
- Entry 3: [Newer news] runs /whatsnew 2, [Older news] runs /whatsnew 4, Newer first.
- Following Newer twice from 3 lands on the newest (Mend news).
- Entry 8: Newer only (/whatsnew 7). /whatsnew 99 clamps to entry 8.
- Java list: 8 [Read] buttons, no Bedrock hint. After /whatsnew ok: entry 2 has Newer, no OK.

Bedrock (bedrock-protocol client via Geyser, tests/rig/bedrocknewer.js): 12/12 (evidence/bedrock-newer.out)
- /whatsnew 3: Newer news, Older news, Close.
- Tap Newer: entry 2 (different text, same 3 buttons). Older from 2 returns to 3 (same text).
- Newer twice reaches the newest: OK, Older news. OK still says "Got it".
- Entry 8: Newer news, Close. Close opens nothing.
- Newest after OK: Older news, Close.
- /whatsnew list hint: "Type /whatsnew 2 to /whatsnew 8 to read an older one."

Rig restored afterwards (0.1.0 jar and its data folder back, rig stopped).

## NOT tested (check on real devices after install)
- How the 3 to 4 button form looks on a real phone or console (the client reads form JSON only).
- Real Java client colors/hover (bot reads text and click events).
- The live players.yml and other live plugins together with 0.1.1 (rig has an older plugin set).
- The draft What's New entry text in game (it parses with 0 warnings against the live changelog).
