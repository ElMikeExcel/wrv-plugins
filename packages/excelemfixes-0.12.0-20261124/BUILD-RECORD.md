# BUILD RECORD — ExcelEMFixes 0.12.0 (EM-world Acrobatics ceiling)

Built Nov 24, 2026 by the Acrobatics ceiling build lane.
Decision: D220. Diagnosis: DIAGNOSIS.md (this folder).
Sizing: CEILING-PLAN.md (this folder). Staged design:
staged-packet/ (this folder) — implemented as staged, with the
small deviations listed at the bottom of this record.

## What shipped in the build

The mcmmo-xp-connector module gains part 5, the EM-world gain
ceiling, exactly per the staged packet:

- `EmWorlds.java` (new, copied verbatim from the staged
  packet): EliteMobs world classifier — `em_` prefix (incl.
  instanced copies) + `GoblinKingdom_vf` exact / instance
  prefix, case-insensitive, plus config extra names/prefixes;
  fail-open (unmatched = not an EM world); null/empty never
  matches.
- `XpPort.Hooks` gains `emWorldHeadroom(player, skill,
  worldName)` (-1 = not covered; >= 0 = remaining budget).
- `KseaEngine`: Settings gains the five emCeiling fields read
  from `em-world-ceiling.*` (defaults: enabled true, skills
  {ACROBATICS} when the list is empty, hourly-xp 100,000);
  a second WatchdogLedger persisted as
  `mcmmo-emceiling-ledger.txt` (loaded beside the watchdog
  ledger, saved beside it in saveState + on a 30 s throttle
  from the feed, via the engine's writeAtomically); the
  MONITOR observed-gain feed restructured so the EM feed has
  its own gate and runs even with the watchdog off; the
  crossing gain writes an EMCEILING flag record (shared
  append path with the watchdog's flags; its log line states
  that further in-world gains ARE trimmed, unlike a watchdog
  flag's wording).
- `McMMOBridge.onXpGain` (LOWEST): after the watchdog-cap
  block, before the grant-window block — headroom 0 zeroes +
  cancels; a gain above headroom is set to exactly the
  headroom (fit/trim semantics); class javadoc gains the
  ceiling paragraph.
- Config: the staged `em-world-ceiling:` block inserted into
  the shipped config.yml immediately after the watchdog
  block, carrying the full REASON comment (Jester's 90,646
  XP/hr hour, Mike's Nov 24 ruling, ~10% headroom sizing).
- README: the staged "EM-world gain ceiling (part 5 of
  mcmmo-xp-connector)" section appended to the module's
  README section, incl. its Knobs paragraph.
- plugin.yml version 0.11.0 -> 0.12.0. Module count stays 19.

## Artifacts

- ExcelEMFixes-0.12.0.jar — 459,581 bytes
  SHA-256 e71620292afe95b58c90650c2d65d85f1a5da8163ecb279d6de861619be4a9a2
  Deterministic double-build byte-identical (two consecutive
  build.sh runs, cmp clean). Build tree: build-root/ in this
  folder (a copy of the live 0.11.0 tree plus this change;
  the copied 0.11.0 jar inside it still hashes 98269ab6...).
- packet/deploy-config.yml — 64,032 bytes
  SHA-256 0c694bfa3d0bf9165f451c797637cbfe87767882de621f6fd55139971ba25cff
  Built from the verified live config (the 0.11.0 packet's
  placed deploy config, f076a351...) by inserting ONLY the
  em-world-ceiling block; verified mechanically — removing
  the inserted 49-line block reproduces the live config
  byte-for-byte.

## Unit suites

run-tests.sh: **1,055 checks, 0 failed** on clean runs
(1,029 pre-existing + 26 new). New class EmCeilingTest
(registered in TestMain; EmWorlds.java added to the
run-tests.sh compile list):

- EMWORLDS (17 checks): em_primis, em_adventurers_guild,
  em_id_primis_wolf_den_1, GoblinKingdom_vf, goblinkingdom_vf_2,
  EM_PRIMIS (case) match; world, world_nether, em, emx_world,
  some_em_world, "", null do not; config extras match
  case-insensitively, exact names do not prefix-match,
  prefixes anchor at the start.
- EMHEADROOM (9 checks): fresh ledger = full budget; 1,000 +
  1,000 vs a 2,500 cap leaves 500; non-EM world and unlisted
  skill return -1; spent and over-spent budgets floor at 0;
  serialize/deserialize round-trips the totals; malformed
  lines skipped; the rolling hour frees the budget.

Flake note: across three suite runs, one run showed a single
failure in the pre-existing SkillNudgeMathTest tie-break
check ("armor named first on the tie") — the documented
pre-existing intermittent flake, in code this change does
not touch; the other two runs were fully clean (1,055/0).

## Rig proof (scratch rig in this folder's rig/, never live)

Pattern: the connector lane's rig (gates-deleted mcMMO fork,
pipeline grants through beginXpGain, deltas measured on the
profile; expectations computed as headroom read live from
the engine + mcMMO's stock early-game boost read from its
own API — the CEILING-FIT precedent). Driver extended with
emceil scenarios (KseaRig), harness run-emceil.py. Receipts:
receipts/rig-emceil-*.json. SEVERE was 0 in every boot, and
every boot's BOOT checks passed (pack present, all 19 modules
enabled incl. mcmmo-xp-connector, engine live).

Rig ceiling 2,500 (config transform), world em_rig:

- EMCEIL-1 PASS: grants of 1,000 + 1,000 ACROBATICS paid
  1,051.0 and 1,000.0 (grant + early-game boost 51, then 0 —
  the boost falls away as the level rises); engine headroom
  after = 449.0 = 2,500 - paid. The ledger counts final
  written amounts, boost included, as designed.
- EMCEIL-TRIM PASS: the crossing grant of 1,000 with
  headroom 449.0 wrote EXACTLY 449.0; headroom after = 0.0.
  **The partial-set trim survives the pipeline — the staged
  plan's hard-zero fallback was NOT taken.** (Boost was 0.0
  for this grant, so the written amount is the headroom
  itself, not headroom + boost.)
- EMCEIL-2 PASS: a further 1,000 grant at the spent budget
  wrote 0.0, and mcmmo-watchdog-flags.txt contains an
  EMCEILING record naming the player.
- EMCEIL-3 PASS: the same player in the overworld — a 5,000
  ACROBATICS grant paid 5,000.0 in full; EM headroom
  unchanged (0.0 -> 0.0). Overworld gains are never counted
  and never touched.
- EMCEIL-4 PASS: a 5,000 SWORDS grant in em_rig paid in
  full; emWorldHeadroom(SWORDS, em_rig) = -1 and
  emWorldHeadroom(ACROBATICS, world) = -1 through the real
  engine.
- EMCEIL-5a PASS (separate boot, plain restart, state NOT
  parked): headroom read 0.0 and a 1,000 grant wrote 0.0 —
  the spent budget survived the restart on disk.
- EMCEIL-5b PASS (third boot, after the harness aged every
  ledger timestamp 3,700,000 ms): headroom read the full
  2,500.0 and a 1,000 grant paid in full again.

Shipped ceiling 100,000 (shipped config, fresh ledgers):

- EMCEIL-6 PASS: 3 x 30,000 grants paid in full (90,000.0
  total); the 4th trimmed to exactly the 10,000.0 headroom
  (wrote 10,000.0); the 5th wrote 0.0; EMCEILING flag
  present. The watchdog's rolling-hour total reached exactly
  100,000 — under its own 120,000 cap — so the zeroing is
  attributable to the EM ceiling alone.

Ceiling disabled (em-world-ceiling.enabled: false):

- EMCEIL-OFF PASS: headroom = -1 (not covered); two 5,000
  grants in em_rig paid 5,000.0 each without limit.

Module disabled entirely:

- EMCEIL-MODOFF PASS (plain boot): pack enables clean, no
  "mcMMO XP Connector ready" line, SEVERE 0.

Rig anomaly, recorded honestly: the first emceilship boot
was stopped by the runner ~5 s into the scenario with a
"done-marker" status but no EMCEIL-6 result line in its log
(the scenario never ran; the boot's receipt showed only the
3 BOOT checks). The scenario was re-run from a fresh park
and completed fully (real KSEARIG DONE, failures=0); the
receipt on file is the re-run's. No product code is
implicated — the module behaved identically in every other
boot — but the first attempt is NOT counted as proof of
anything.

Honest limit (from RIG-PROOF.md): real Dodge procs cannot be
produced headlessly. The ceiling counts written gains
regardless of source at the same MONITOR feed the watchdog
already proves against, and the trim acts on the same gain
event the per-damage veto proves against — a gain's source
cannot change the ceiling's behaviour. Post-install, the
first real bite (or 7 quiet days) is watched via the
EMCEILING flag line.

## Filing (D212)

- Packet: packet/INSTALL-PACKET.md (8,150 B, SHA-256
  bab6451bf5ba22d4613a9be3f53312226c5bde1d585ecdb69b99c6d1c5004ffd,
  lint 8150/74/0), packet/SURFACES.md (What's New TEXT entry
  id 2026-11-24-acrobatics-em-ceiling + website row text),
  packet jar + deploy-config as above.
- MANIFEST: PENDING entry added Nov 24, 2026 (backup
  MANIFEST.md.bak-20261124-emceiling taken first). Provenance
  gates: live 0.11.0 jar 98269ab6... (456,032 B) + live
  config f076a351... (61,436 B); config mismatch falls back
  to a section-merge, never a wholesale replace.
- Decision logged: D220.
- NOT installed by this lane (the failsafe owns installs);
  no player messages sent; no GitHub posts made. The
  upstream notice to MagmaGuy naming 0.12.0 is owed by the
  parent lane AFTER live proof, per Mike's Nov 24 testing
  bar.

## Deviations from the staged plan

1. Trim semantics: NONE — the rig proved the partial-set
   trim exact on the first attempt, so the shipped build
   keeps fit semantics (the plan's hard-zero fallback was
   not needed and not taken).
2. saveEmCeiling uses the engine's writeAtomically (tmp +
   atomic move), matching saveWatchdog's real shape in the
   tree — EDITS.md's sketch showed a plain writeString but
   instructed to match saveWatchdog; followed the instruction.
3. writeFlag refactor: the flags-file append was extracted
   into one shared appendFlagLine used by both writeFlag and
   the new writeEmCeilingFlag — EDITS.md explicitly offered
   this shape ("pick whichever shape keeps one file-writing
   code path").
4. Settings.from reads the skills list via a small helper
   (emCeilingSkills) instead of EDITS.md's inline ternary —
   same semantics (empty list -> ACROBATICS), and Set.of /
   HashSet via imports rather than fully-qualified names.
5. Documentation additions beyond the staged text (same
   pass, per the task's public-distribution requirement):
   one sentence in the config's hourly-xp comment and one
   clause in the README Knobs paragraph stating the shipped
   100,000 is WRV's sizing from its own data and other
   servers should size from theirs; the README data-files
   sentence now also names the EM-world ceiling ledger.
   No staged wording was removed or altered otherwise.
6. Rig: run on a scratch copy of the connector lane's rig
   in this folder (the Homebase mirror was busy with other
   lanes, and the plan allowed either). EMCEIL-5's restart
   half ran as two boots (emceil2 pre-age, emceil3
   post-age) with the harness ageing the ledger file
   between them, per RIG-PROOF's "rig clock/ledger edit".
