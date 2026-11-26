# excelemfixes-0.20.0-20261125 — RIG RESULTS

Rig proof (local test rig only, Nov 25, 2026). Evidence:
~/workspace/excelemfixes-caster-xp-toggle-20261125/rig/
(boot-0200.log, boot-0200-final.log, bot/casterbot-out.log);
full detail in the packet RECEIPTS.md. VERDICT: PASS on
every leg.

Rig: Paper 26.1.2, EliteMobs live data set; bot "CasterBot"
staged Jester-shaped via /em skill set: WANDS Lv 18 =
198,972 XP, STAVES Lv 12 = 37,023 XP. All XP reads are
EliteMobs' own player_data.db.

- BOOT: "Enabling ExcelEMFixes v0.20.0", all 23 modules
  incl. "Caster XP Sharing ready — per-player WANDS/STAVES
  sharing toggle (default mode STOCK, catch-up on, 0 stored
  player record(s))", Done, SEVERE 0. Restart boot: same,
  "1 stored player record(s)".
- STOCK: wand kill of a level-12 test boss: WANDS
  198,972 -> 199,173 (+201), STAVES +0 exactly. Negative
  controls: bare-fist boss kills pay no skill XP; a level-1
  boss pays 0 to a class-12 player (EM level-difference
  scaling).
- CATCH-UP: /casterxp shared -> "Caster catch-up: your
  Staves XP was raised from 37,023 to 199,173 to match your
  Wands"; DB 199173|199173 exactly; STAVES level 12 -> 18.
  Audit: catch-up-done true, track STAVES, amount 162,150,
  both before-totals + timestamp. Re-toggle did not refire
  — fires exactly once.
- SHARED: wand kill: WANDS +101, STAVES +100 (sum 201 = the
  STOCK award; XP-neutral ceil/floor split).
- CARRY: gap re-opened in STOCK (+201 / +0, gap 202), then
  /casterxp carry + wand kill: WANDS +201, STAVES +201
  (full mirror; gap constant at 202).
- MENU: /skillmenu Wands page carries the "Caster XP
  Sharing" row -> module view (3 mode buttons, current mode
  highlighted) -> menu click set SHARED -> Back returns to
  the Wands page. Bedrock parity rides the same
  Views/MenuRenderer surface as all 0.15.0+ modules.
- PERSISTENCE: mode + catch-up record survive a full
  restart; /casterxp status after restart: "Caster XP
  sharing: Shared pool".

Unit suites: baseline pristine 0.19.0 tree 1,193 passed /
0 failed; final tree TOTAL 1,259 passed, 0 failed on
repeated full runs (66 new caster-xp checks). One
intermittent failure can appear on either tree:
"Jester-shaped: armor named first on the tie"
(SkillNudgeMathTest) — the documented pre-existing
tie-break flake (salted Map.of iteration), unrelated to
this module.

Live acceptance (install window
wrv-install-failsafe-20261125-1752): boot Done (37.536s)
17:57:23 CT, "ExcelEMFixes 0.20.0 ready" with 23 modules,
emfixes reload ZERO reload-failed, SEVERE 0, guild OPEN
5 of 5 unchanged. Confirmatory live proof owed: Jester's
first non-STOCK toggle fires his one-time catch-up —
verify in EM player data, then the report-back fires
(separate drop).
