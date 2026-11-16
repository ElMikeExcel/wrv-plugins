# RIG RESULTS — ExcelDowned 0.1.5

Status: Installed, not Proven. The S7 live retest is still owed (see
below); nothing in this document discharges it.

## Unit suite (build tree)

- Full suite: 44/44 passed, 0 failed (39 carried + 5 new
  TemplateCheckPolicy tests: schedule shape, empty-registry defers
  and never WARNs early, deferred→loaded resolves sequence,
  missing-key-in-loaded-registry WARNs once, probe-unavailable stays
  distinct). See BUILD-RECORD.md.

## Mirror rig (Homebase wrv-testserver, full stack, real Magic 11.2.5)

- R1 (end state) — PASS. Cold mirror boot, candidate staged:
  deferred check logged `resolves` for BOTH `resurrection` and
  `grouprenewal`; template WARN count = 0; ExcelDowned WARN
  count = 0; SEVERE = 0.
- R2 (negative control) — PASS. Mirror spells.yml copy with the
  `resurrection:` block removed: the final-check WARN fired exactly
  once, naming `resurrection` only, with the corrected wording;
  `grouprenewal` logged `resolves`, no WARN; SEVERE = 0.
- R3 (timing evidence) — PARTIAL. The defective pre-0.1.5 ordering
  reproduced (Downed enabled ~5 s before Magic's spell load
  completed, so a synchronous onEnable probe would have read the
  empty registry in this very boot). The deferred-INFO branch did
  not fire on the mirror, correctly — Magic's load finished before
  Downed's first deferred attempt (+10 s); that branch is pinned by
  the unit suite.
- R4 (grant sanity) — PARTIAL. Post-load reflective probes (the
  identical resolver shape MagicGate's hook uses) resolve both keys;
  no skipped-key "Wand inventory sync" WARNING in the boot. Limits:
  the mirror's ExcelMagicGate is 0.3.1 (predates SpellTemplateGate,
  added 0.3.2; live carries 0.3.3), and no player joined the mirror,
  so no literal join-time wand sync ran. The remainder is readable
  off the S7 session.

## Install boot verification (live, Nov 16, 2026)

- Installed by wrv-install-failsafe, window marker 09:23:40 CST,
  Shut Down 09:38 CST, Start 09:39 CST, boot Done 09:39:53 CST
  (24.287s). Backup "ExcelDowned 0.1.5 pre-install 1116" (cap-at-6
  held). Placed jar pull-back EXACT 63,701 B / 50d75149…9710;
  Loading ExcelDowned v0.1.5 = 1; "ExcelDowned 0.1.5 ready" = 1;
  build EXC-DOWNED-0.1.5-WRV-20261116-01-d0wn3d = 1; custom Magic
  action 'Resurrect' registered = 2; SEVERE 0; guild OPEN 5 of 5
  unchanged. See INSTALLED.txt and INSTALL-PACKET.md.
- THE WARN LEG (the defect 0.1.5 fixes): "does NOT resolve" = 0;
  "grants of this spell will NPE" = 0 — both 0.1.4 false-positive
  boot WARNs are GONE. "spell template 'resurrection' resolves" = 1;
  "spell template 'grouprenewal' resolves" = 1 (deferred check after
  Magic loaded).

## Proof still owed — S7 LIVE RETEST

A real revive in game with real clients (tester class: Marvin or
Mike — a healer/caster + a party member who goes down): caster wand
carries Resurrection, cast lands, revive at 50%, revive record
written, party DOWN / revived alerts fire. Run BOTH doors if the
session allows (hub door via /excelparty, EM door via /em party);
the same session also carries the R4 remainder (a literal
ExcelMagicGate 0.3.3 join-time sync read). Until S7 lands,
ExcelDowned 0.1.5 is Installed, not Proven.
