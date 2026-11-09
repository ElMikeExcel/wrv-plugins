# ExcelDetails 0.2.0 — BUILD RECORD (Nov 9, 2026)

Design: `~/workspace/details-meter-20261109/build-0.2.0/DESIGN.md`.
Commission: Mike's checkbox-mix ruling, Nov 9, 2026, ~10:25–10:28 AM CST
(MENU-PACKET-DETAILS-SETTINGS.md §0). Base: the shipped 0.1.0 source
tree, copied and delta'd — intake, segment lifecycle semantics, and
placement law are 0.1.0's, untouched except where this record says so.

**Status: BUILT + UNIT-PROVEN at desk. NOT installed. MANIFEST PENDING
entry filed at build close (see PACKET-DRAFT.md); install is a parent
hand window.**

## Candidate

- `ExcelDetails/ExcelDetails-0.2.0.jar` — 37,130 B,
  sha256 `f216f81eefd8d09cc6abfc14efbd2b884803f6c3717d9d9946766b836cd538cb`
- Deterministic double-build byte-identical, AND a second full build
  run reproduced the same sha256. Fingerprint gate PASS
  (build EXC-DETAILS-0.2.0-WRV-20261109-01-c84f19, Layer B in-jar:
  excel-build.txt + BuildInfo fragments).
- Unit tests: **28/28** (13 carried from 0.1.0, unmodified + passing;
  15 new): mix defaults (damage only), independent toggles + all-off
  legality, showOnly exclusivity, section composition at 1/2/3
  sections, the 15-line ceiling worked case (3 headers + 4 rows each,
  footer shed), the fat-EM worked case (budget 8 → 3 headers + rows
  2/2/1), no-data/all-off = zero lines, per-section own-row retention,
  zero-event segment never closes/announces, micro segment merges
  silently, real fight announces, post-clear empty close is silent,
  full data.yml round-trip (prefs + mix + overall totals survive a
  simulated restart), 0.1.0 legacy window-key migration, snapshot
  isolation.

## What 0.2.0 changes

1. **The mix (the ruling).** PlayerPrefs gains three independent
   shown booleans (defaults: damage ON, taken OFF, healing OFF); the
   0.1.0 single window survives as the focus. LineBuilder.buildSections
   stacks every ticked metric as its own separated section (0.1.0
   header format per section) in one box; a metric with no data
   renders nothing. **Budget rule (pinned): config max-rows is a
   PER-SECTION cap; the box total is hard-capped at 15 lines in both
   modes** (piggyback budget = 15 − EM lines; standalone = 15), shed
   order: footer → rows from the fullest section → whole sections
   from the end of canonical order. All-off (or no data) = NO box at
   all: standalone hands the scoreboard back, piggyback strips its
   entries; segments keep tracking regardless.
2. **Commands.** `/details show damage|taken|healing` toggles one
   checkbox (no arg = mix readout). `/details window <x>` = show
   EXACTLY that metric + focus it; `/details next` = focus advances,
   shown becomes exactly that window (documented quick paths to the
   0.1.0 feel). Bare `/details` status now leads with the mix line.
   on/off/toggle/clear/reset/reload and the `/dmeter` alias unchanged.
3. **Menu bridge.** PlaceholderAPI expansion (softdepend, registered
   only when PAPI is present): %details_show_damage%,
   %details_show_taken%, %details_show_healing%.
   ⚠️ **FLAG — value format:** these return **ON/OFF**, not the
   true/false the build task text mentioned. The consumer spec
   (MENU-PACKET §0/§2) drafts the Details Settings menu's
   view-requirements comparing against 'ON' — the house idiom of
   %menujoin_status% / %silkwarnings_status% / %aimassist_status% —
   so ON/OFF is what makes the drafted menu work as written. If the
   parent wants true/false instead, it is a one-line change plus a
   menu-packet edit; decide before the menu packet places.
4. **⚠️ ADDED JUDGMENT CALL — overall-total persistence (beyond the
   mix ruling, per the build task's own scope item 8).** 0.1.0 kept
   overall totals in memory only (its BUILD-RECORD names this a v1
   limit: "segment numbers do not survive a restart" — which is why
   Mike's Nov 9 session totals could not be re-read after the Packet
   A restart). 0.2.0 persists per-player overall totals (damage
   done/taken, healing, hits, display name) into data.yml on segment
   close, on /details reset, at every prefs save point, and on
   disable; loaded at startup. Totals only — the by-target/by-source
   breakdown maps stay session-scoped by design (unbounded growth,
   no ruled need). All data.yml I/O moved into a pure DataFile class
   (round-trip unit-tested incl. the 0.1.0 → 0.2.0 migration: a
   legacy `window:` key with no show-* keys loads as shown = {that
   window}). Downgrade-safe: 0.1.0 ignores the new keys.
5. **Empty-segment churn — investigated + fixed.** Evidence:
   reconcile/session-log-20261109-pre-restart.log — closes announced
   "(0:00)" at 16:14:28 and 16:14:49 UTC, minutes after Mike's
   16:13:55 join, around his real (0:08)/(0:02) test segments.
   **Cause (proven from the 0.1.0 code):** announced duration =
   lastEvent − segmentStart; every record path gates amount > 0
   BEFORE opening, so a zero-event segment can never open — a
   "(0:00)" close is a MICRO segment: exactly one tracked event (or
   a sub-second burst) followed by 8 s of quiet. Around a login that
   is a lone satiated/eating regen tick, minor fall/settle damage,
   or one stray hit. latest.log logs no intake, so the specific
   events at 16:14:20/16:14:41 UTC are not recoverable — the class
   is certain. A second 0-event close path existed: /details clear
   restarts an EMPTY segment whose timeout close then announced.
   **Fix (lifecycle semantics byte-for-byte 0.1.0 — merges, archival,
   totals unchanged):** the tracker counts tracked events per
   segment; a close is announce-worthy only at ≥2 events or a ≥1 s
   span. Micro closes and post-clear empty closes merge into overall
   silently. A 0-event segment now provably never announces
   (unit-tested all three shapes).
6. **Deliberate conformance change (flagged):** an enabled player
   with no segment data now sees NO box (0.1.0 idled with a
   header+footer frame). This is DESIGN.md §2's "auto-opens in
   combat" made literal, and it is what makes the all-off rule's
   "no stray empty frame" hold in every state.

## NOT re-run / unverified (stated plainly)

- The 0.1.0 Paper rig was NOT re-run for 0.2.0: it predates the
  section renderer, and this build's gates were the unit suite +
  deterministic double-build per the build task. Bukkit-level proof
  (piggyback under the REAL EliteMobs PartySidebar, menu placeholder
  reads, Bedrock rendering) rides the install window's player proofs
  in PACKET-DRAFT.md — including the still-owed 0.1.0 EM-party proof,
  which the mix makes more important, not less.
- Healing attribution is unchanged from 0.1.0: the regain event
  names no healer, so healing credits the player it lands on
  (DESIGN §4). Not a 0.2.0 scope item.
- The placeholder registration path (PAPI present → register) is
  compile-verified against PlaceholderAPI 2.12.3 (the live version);
  its first live read is the Details Settings menu placement proof.

## Files

- Source + tests + build script: `build-0.2.0/ExcelDetails/`
  (local-build.sh: JUnit first, then the pinned double-build +
  fingerprint gate; PAPI jar is compile-only via softdepend).
- Packet: `build-0.2.0/PACKET-DRAFT.md`. MANIFEST PENDING entry
  filed Nov 9, 2026 at build close.
