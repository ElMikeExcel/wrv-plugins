# ExcelNoPhantoms 1.2.0 — Source & Provenance

**Ownership:** ExcelNoPhantoms is original code written for the Wink
Reyes Valley family server (Argus, for Mike). There is no upstream
project. Version 1.2.0 extends our own 1.1.0 (Oct 16, 2026 renames
bundle) in place: same package, same main class, LegacyDataMigration
carried over byte-untouched (SHA-256
2bc0e7d4c5705edd088ca52bbd4ad0c1ac7b289950f61d6f4e0e1bdf3ae380d1 in both
the 1.1.0 and 1.2.0 trees).

**Purpose:** the Phantom Debt event mode (Mike's design, approved
Oct 18, 2026 ~8:01 PM CDT; story-rotation upgrade ~8:04 PM CDT).
Phantoms hunt on America/Chicago Mon–Thu nights while the event is
active and Seal 1 (the Ender Dragon) stands; the guild's wards suppress
them Fri–Sun; when the dragon seal breaks (read from ExcelGuildGate by
reflection — softdepend, no compile-time dependency) or an operator
ends the event, suppression is permanent. Haunted nights carry a
rotating in-chat story (the reason, in 5 lines); ward evenings get one
ward line at first nightfall. The bundled default config
(mode: always-off) reproduces 1.1.0 exactly.

**Compile-time dependencies (not redistributed in the source zip):**
- Paper API (compileOnly), via the house harness: ExcelShops
  build-libs paper-api.jar + runtime-server libraries, JDK 25 javac
  with --release 21, plain javac + deterministic jar packaging
  (make-jar.py, sorted entries, pinned timestamps).

**Layout:**
- `src/ExcelNoPhantoms/src/com/winkreyes/excelnophantoms/` — 6 sources:
  ExcelNoPhantoms (Bukkit glue: listener, config, GuildGate reflection,
  broadcast checks, state.yml persistence, /nophantoms), PhantomGate +
  GateConfig (the pure gate and fail-soft config parsing), StoryRotator
  + WardLineTracker (the pure broadcast state machines),
  LegacyDataMigration (1.1.0, untouched).
- `src/ExcelNoPhantoms/` — plugin.yml (version 1.2.0, softdepend
  ExcelGuildGate, /nophantoms + excelnophantoms.admin) and config.yml
  (bundled default, mode: always-off).
- Project root of the zip — build.sh, run-tests.sh, make-jar.py,
  BUILD-1.2.0.md (semantics, test totals, rig evidence + its honest
  limit), INSTALL-PACKET.md, packet-config/config.yml (the live event
  config the packet places).
- `tests/` — 4 JUnit 5 + Mockito suites, 35 tests, 0 failures.

**Checksums:**
- ExcelNoPhantoms-1.2.0.jar — 40,631 bytes
  SHA-256 f705ac0e657645c09ed20427289c738dd25b5eb6c3983f3a383c825a057d94f8
  MD5 2848050ef92e19866bc9af3a98fed1fa
  (a second clean build reproduced the jar byte-identically)
- ExcelNoPhantoms-1.2.0-source.zip — 88,665 bytes
  SHA-256 9b6dbd8926963715143c097c89bc5c36b1fe336b7132d49341d39df9a025beee
  MD5 7dee6d4d182afd670ec50f2602249d91

**Drive deposit (deposit-at-build, Oct 18, 2026):** see BUILD-1.2.0.md's
deposit note and the build report; file IDs + MD5 verification were
confirmed against Drive metadata at deposit time and are recorded in
the project BUILD doc's final amendment.

**Status at deposit (Oct 18, 2026):** BUILT, tested 35/35, rig-proven
on scratch Paper 26.2-123 alongside ExcelGuildGate 0.3.0 (both
always-off and event boots; seal read via reflection returned
"not broken"). NOT installed — rides the pending install window per
INSTALL-PACKET.md. The ALLOW path's live proof is owed on the first
haunted night after install (today is a Sunday, a wards day; the rig
cannot honestly show it — see BUILD-1.2.0.md).
