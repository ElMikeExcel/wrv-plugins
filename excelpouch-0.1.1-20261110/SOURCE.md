# ExcelPouch 0.1.1 — SOURCE record

Excel — Wink Reyes Valley. Authors: Iris / Mike. November 10, 2026.
Commission: the console-admin coverage audit (Nov 9, 2026) under
Mike's Nov 3, 2026 console-admin rule; build brief
`~/workspace/pouch-consoleadmin-20261110/BUILD-BRIEF.md` (design
of record: `DESIGN.md` alongside it).

## Provenance

- Project tree: `~/workspace/pouch-011-build/ExcelPouch/`
  (plugin.yml version 0.1.1, main
  com.winkreyes.excelpouch.ExcelPouch, api-version '1.13').
- Base: the 0.1.0 tree `~/workspace/excelpouch/src/ExcelPouch/`
  (the brief's source of record; no other Pouch lineage exists).
  Shipped 0.1.0 jar SHA-256
  `ace560bfd315f3dd9b3ebc54562fb45a5ca82a5bae1750ec521293122a69d211`
  (28,574 B) re-verified on disk Nov 10, 2026; the 0.1.0 folder
  is untouched by this build.
- Built by `build.sh` (the house deterministic recipe from
  0.1.0: manual javac, JDK 25 at `~/workspace/.jdk25`,
  `--release 21`, house Paper-API + kyori 4.20 classpath, jar
  entry timestamps pinned). Runtime target is the live Paper
  server; every API used is long-stable Bukkit API.
- Ownership: all code is ours (Iris / Mike lineage). NO runtime
  dependencies: no Magic, DeluxeMenus, or PlaceholderAPI
  linkage — Magic interaction stays purely through the item
  data Magic itself writes, exactly as in 0.1.0.

## What 0.1.1 changes vs 0.1.0 (the whole delta)

CONSOLE ADMIN. Design + root cause: DESIGN.md in
`~/workspace/pouch-consoleadmin-20261110/`. In short:

- NEW `core/AdminSelector`: pure-logic selector resolution
  (`pocket:<n>` incl. config-hidden pockets, or a kind id;
  zero/one/many semantics; many = refused with the pocket list).
- `ExcelPouch`: the `admin` branch routed BEFORE the player
  gate; gate = console or the new `excelpouch.admin` node;
  status (global + per-player, online or offline, non-retaining
  reads), return (online only, full-inventory safe), clear
  (the D10 safety stack: confirm gate → close-GUI-first with
  post-sync identity revalidation → verified verbatim rescue
  copy → delete via setPocket → audit line), reload; admin-only
  tab completion; item description from a deserialized copy.
- `PouchStore`: additive only — `isBroken()` accessor and a
  read-only pocket snapshot exposing the verbatim stored Base64
  entry (parallel `entries` list). Storage format unchanged.
- `plugin.yml`: version 0.1.1, admin usage line, new
  `excelpouch.admin` permission (default op).
- Tests: new `AdminSelectorTest` (23 checks) + TestMain
  registration. NOTHING else: PouchListener, PouchHolder,
  PouchService, api/PouchQuery, the other core classes, and
  config.yml are byte-for-byte the 0.1.0 ones.
- Explicitly NOT built (brief §4/§5): any grant/park-on-behalf
  op, any offline-return destination, any broken-store
  reset/recovery op, any rescue-copy pruning.

## Artifacts

- ExcelPouch-0.1.1.jar — 45,109 bytes
  SHA-256 b4cdfa2057733a06fe39607bd018c571d8bb0a9cfbce06d753cc0e6cfff3f9c4
  Deterministic double-build byte-identical. (No in-jar build-ID
  fingerprint exists in this lineage; version + hash identify it.)
- ExcelPouch-0.1.1-source.zip — 115,654 bytes
  SHA-256 eac9be04bb2ca3968455b36698bb74a32efb71ec2b3d2214dd45730025a46bc9
  (src/ + tests/ + plugin.yml + config.yml + build.sh +
  run-tests.sh; timestamps pinned 2026-11-10.)

## Verification summary

Desk 95/95 (72 carried regression checks unchanged + 23 new
AdminSelector checks). Rig on Paper 26.2 + Magic 11.2.5 +
ExcelMenuJoin 1.4.0 with NMS fake players (rig-0.1.1 in the
build folder): clean 0.1.1 enable, zero SEVERE, clean stop,
44/44 behavior checks dispatched through the real console
sender — status (online/offline/never-joined/fresh-UUID, data
dir byte-identical), reload after a slots edit, return
(kind/full-inventory/repeat), clear (preview/confirm/rescue
byte-equality), the duplicate-wand worked example, the
GUI-open race for return AND clear, broken-store refusal,
sealed-pocket read-refuse/clear-succeed, non-op denial,
ExcelMenuJoin regression, both D10 negative tests, and the
offline return-refusal / clear-success pair. One deviation
from the brief, recorded in BUILD-RECORD.md: the §3 step-2
post-close revalidation compares the §4.1 item identity rather
than raw bytes (Magic's volatile wand fields make byte-strict
comparison abort every legitimate wand op; a different item
still aborts).
NOT installed — built + staged only.
