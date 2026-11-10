# BUILD RECORD — ExcelDowned 0.1.2 (console admin surface)

Built Nov 10, 2026 (desk build lane for the WRV board supervisor;
overnight console-admin burn-down, AUDIT.md §4 suggested order —
Downed second). Commission: the console-admin coverage audit
Nov 9, 2026 under Mike's Nov 3, 2026 console-admin rule.
**Not installed.** The install packet is `INSTALL-PACKET.md` in
this folder; the MANIFEST entry is drafted in
`MANIFEST-ENTRY-DRAFT.md` for the supervisor to file after
verification (this lane did NOT touch the MANIFEST, the
parent-backlog, or board state). Records staged at
`~/workspace/records-catchup-20261103/packages/exceldowned-0.1.2-20261110/`;
GitHub/Drive legs are the parent's (see `PUSH-NOTES.md`). The
0.1.1 trees at `~/workspace/downed-revive/` and in the 0.1.1
records package are untouched — the 0.1.1 jar still hashes to its
record value (verified at close-out).

## Base proof

- Authoritative base: the records-package 0.1.1 tree
  `~/workspace/records-catchup-20261103/packages/exceldowned-0.1.1-20261108/`,
  verified byte-identical to the build tree at
  `~/workspace/downed-revive/build/ExcelDowned/` (src, plugin.yml,
  local-build.sh diffed clean), whose jar
  `ExcelDowned-0.1.1.jar` — 47,199 B — SHA-256
  `331e3b103ef07da6f81455f212d56c849916e1105232d0bb0b5bd472ae871d2f`
  matches its BUILD-RECORD and the installed record
  (`~/workspace/downed-revive/INSTALLED-0.1.1.txt`, installed
  Nov 8, 2026).
- DELTA RULE, verified before the change: a clean-room copy of
  the untouched tree rebuilt ExcelDowned-0.1.1.jar
  **byte-identical** to the shipped jar (331e3b10…2f), 17/17
  tests green at the 0.1.1 totals. The 0.1.2 delta was applied
  to a copy of that verified base.

## Candidate

- `ExcelDowned-0.1.2.jar` — **56,778 B** — SHA-256
  `2320e812d6c33a21f179b8e64d3f37723f260c97dbefc2eadf73cef97c519396`
  — MD5 `d2bcc275d60cda833d0125a8d847d5ee`
- Deterministic double-build byte-identical, AND a clean-room
  rebuild and a rebuild from the shipped source zip each
  reproduced the same hash (three independent builds).
- Fingerprint gate PASS — build
  **EXC-DOWNED-0.1.2-WRV-20261110-01-d0wn3d** (Layer B in-jar;
  the `d0wn3d` tag and EXC-DOWNED semantic marker carried forward
  from 0.1.0/0.1.1 per the persistence law).
- `ExcelDowned-0.1.2-source.zip` — 42,200 B — SHA-256
  `3994ea7ace118c244d59a3ce0d8b5166d392b518685621256ba2ecd26f5ba519`
  (src/ + tests/ + plugin.yml + config.yml + local-build.sh +
  DESIGN-0.1.2.md; timestamps pinned 2026-11-10).

## What shipped (the whole change, nothing else)

Design of record: `DESIGN-0.1.2.md` in this folder and in the
source zip. Change-scope diff vs the verified 0.1.1 tree — only:
`AdminPolicy.java` (new), `DownedCommands.java` (admin routing +
handlers), `ExcelDowned.java` (`adminRevive`/`adminClear`/
`sessionByName`; `revive` refactored onto a shared internal path
with the spell record/messages byte-unchanged), `BuildInfo.java`
(version fragments), `plugin.yml` (version, usage, descriptions),
`local-build.sh` (version/date/build-id knobs), `DownedTest.java`
(10 new tests). `config.yml` and every other file are
byte-for-byte the verified 0.1.1 ones (diff-proven). No gameplay
value changed: bleed-out 300 s, downed factor 0.25, Resurrection
mana 100 / stage-4 unlock (spells packet, untouched), the 0.1.1
/giveup button menu — all as Mike ruled them.

- **`/downed revive <player>`** — force-resolve an ONLINE downed
  session without death, through the EXISTING revive path (the
  same exactly-once session consumption the Resurrection spell
  uses; target returns at the configured 50% fraction,
  presentation cleared, DownedReviveEvent fired). Recorded as
  `admin_revive` naming the operator actor — never as a fake
  spell cast — with an INFO audit log line.
- **`/downed clear <player>`** — remove a downed session outright
  (state surgery for a stuck session): online (presentation
  cleared) or OFFLINE (the durable downed.yml record is dropped;
  the player returns standing at next login). No health restored,
  no death run, nothing drops. Recorded as `admin_clear` with
  actor + online flag.
- **Gate:** the EXISTING `exceldowned.admin`, plugin.yml default
  **op** (the audit's explicit condition — no new node created).
  Console explicitly allowed for the revive/clear branch only
  (`console || hasPermission`, the ExcelCosmetics/BrainGames
  shape). Denial precedes all target handling. Player-facing
  paths (/giveup incl. its console rejection, self status, the
  gated `<player>` inspection and `reload`) are
  behavior-unchanged.
- **Deliberate limits:** revive requires the target ONLINE (it
  restores live health through the spell's path) — an
  offline-durable session is reported with its remaining
  bleed-out clock and left untouched, with clear named as the
  offline tool; no tab-completion surface added (0.1.1 registered
  none; the admin verbs are operator surface, typed in full).

## Tests

- **DownedTest 27/27** via `bash local-build.sh`: the 17 carried
  0.1.0/0.1.1 state-machine tests unmodified at their totals
  (deadline math, scaled window eating, TW composition, typed
  two-step incl. stale-arm re-arm, exactly-once latch,
  persistence round-trip, the 0.1.1 arm/confirm/disarm sharing,
  routing matrix) + **10 new for the admin surface**: gate matrix
  (console allowed; operator allowed; non-admin denied; the node
  is the existing `exceldowned.admin`), routing for both verbs
  (targets carried, case-insensitive, console + operator),
  denial preceding usage/targets, usage errors (missing/extra/
  blank target), legacy-surface regression pins (every pre-0.1.2
  shape routes LEGACY for console, plain player, and operator),
  target classification incl. unknown, the revive decision
  matrix (proceed / not-downed / offline-refusal / unknown), the
  clear decision matrix (proceed for online AND offline), admin
  acts exercised against a real session registry (revive
  consumes exactly once and never touches the death latch; clear
  removes a persistence-restored offline session), and
  plugin.yml/BuildInfo pins (version 0.1.2; admin default op;
  usage names both forms; fingerprint lineage).
- **Rig** (Paper 26.3-235 + Magic 11.2.5, NMS fake players,
  `rig-0.1.2/`): clean 0.1.2 enable, Resurrect action registered,
  ready line with bleed-out 300 s / factor 0.25, **SEVERE 0**,
  clean stop, and **17/17 behavior checks** dispatched through
  the real console sender — console revive at 50% with no death,
  console clear restoring nothing, redundant/unknown/usage
  no-ops, the offline pair (revive refuses + changes nothing;
  clear removes the record; player returns standing), a
  non-admin player's revive AND clear both denied (proven by
  state), and the legacy console status + /giveup rejection
  unchanged. Records stream carries admin_revive ×2 /
  admin_clear ×2 with actor CONSOLE and no fake spell/death
  records. Full results: `rig-0.1.2/RIG-RESULTS.md`; log:
  `rig-0.1.2/boot.log`. Rig limitation, stated plainly: the
  denial MESSAGE text is not observable in the rig (fake-player
  chat is not echoed to the log) — denial itself is proven by
  state there and by routing in DownedTest; no Bedrock form
  round-trip (unchanged from 0.1.1, and 0.1.2 does not touch the
  /giveup UI surface).
