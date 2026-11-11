# BUILD RECORD — ExcelPouch 0.1.1 (console admin)

Built Nov 10, 2026 (desk build lane for the WRV board supervisor).
Brief: `~/workspace/pouch-consoleadmin-20261110/BUILD-BRIEF.md`
(design of record: `DESIGN.md` in the same folder; decisions of
record: decision-log D10 for the wand-clear safety stack, brief
§4 for design-D2/D4/D5/D6, brief §5 for the design-D3 flag).
**Not installed.** The install packet is `INSTALL-PACKET.md` in
this folder; the MANIFEST entry is drafted in
`MANIFEST-ENTRY-DRAFT.md` for the supervisor to file after
verification (this lane did NOT touch the MANIFEST, the
parent-backlog, board state, or MEMORY.md). Records staged at
`~/workspace/records-catchup-20261103/packages/excelpouch-0.1.1-20261110/`;
GitHub/Drive legs are the parent's (see `PUSH-NOTES.md`). The
0.1.0 tree at `~/workspace/excelpouch/` is untouched — the 0.1.0
jar still hashes to its record value (verified at close-out).

## Base proof

- Base source of record: `~/workspace/excelpouch/src/ExcelPouch/`
  (the 0.1.0 tree, per the brief — no other Pouch lineage exists
  on disk). The live gate fingerprint from the brief was
  re-verified on disk at close-out: `ExcelPouch-0.1.0.jar` —
  28,574 B — SHA-256
  `ace560bfd315f3dd9b3ebc54562fb45a5ca82a5bae1750ec521293122a69d211`
  (matches `ExcelPouch-0.1.0-SOURCE.md`).
- The 0.1.1 delta was applied to a copy of that tree in
  `~/workspace/pouch-011-build/ExcelPouch/`; the 0.1.0 folder
  itself was never edited.

## Candidate

- `ExcelPouch-0.1.1.jar` — **45,109 B** — SHA-256
  `b4cdfa2057733a06fe39607bd018c571d8bb0a9cfbce06d753cc0e6cfff3f9c4`
- Deterministic double-build byte-identical (two consecutive
  `build.sh` runs, same hash).
- Build ID: this lineage carries no in-jar build-ID fingerprint
  (0.1.0 has no BuildInfo class and the brief's §2 file list adds
  none) — the artifact is identified by version 0.1.1 + the
  SHA-256 above.
- `ExcelPouch-0.1.1-source.zip` — 115,654 B — SHA-256
  `eac9be04bb2ca3968455b36698bb74a32efb71ec2b3d2214dd45730025a46bc9`
  (src/ + tests/ + plugin.yml + config.yml + build.sh +
  run-tests.sh; timestamps pinned 2026-11-10).

## What shipped (the whole change, nothing else)

Player-facing behaviour change: **none.** The bare `/pouch`
player path, the GUI, the allowlist, death-survival, and the
`PouchQuery` contract are behaviour-unchanged. Change-scope diff
vs the 0.1.0 tree — only the brief §2 files:

- `ExcelPouch.java` — the `admin` branch is dispatched in
  `onCommand` **before** the `instanceof Player` gate (the
  existing rejection line stays for the non-admin path). Gate:
  `sender instanceof ConsoleCommandSender ||
  sender.hasPermission("excelpouch.admin")`. Handlers: global +
  per-player `status`, `return`, `clear`, `reload`, help for
  bare/malformed admin forms. Shared target resolution (exact
  online name → hasPlayedBefore offline → literal UUID; a
  never-played non-UUID name is rejected in one line and creates
  no state). Shared selectors via AdminSelector. A non-retaining
  read path for offline targets (`loadForAdmin` +
  `evictWithoutSave`) so a pure read can never mint an empty
  `<uuid>.yml` via the autosave (design §4.5.2). An
  item-description helper that deserializes a **copy** of the
  pocket bytes and never writes back. Tab completion for the
  admin branch only. `reloadFromCommand()` wraps
  `reloadConfig()` + the existing private `reloadSettings()`.
- `core/AdminSelector.java` (new) — pure-logic selector
  resolution: `pocket:<n>` (0-based, includes config-hidden
  pockets) or kind id; zero = no-op report, one = act, many =
  refused with the matching pocket list; malformed / out-of-range
  outcomes. Bukkit-free, plain-JVM tested.
- `PouchStore.java` (additive only) — `isBroken()` accessor; a
  read-only pocket snapshot (`snapshot()` / `rawEntry(int)`)
  exposing index, kind, byte length, and the **verbatim stored
  Base64 entry** per pocket (a parallel `entries` list maintained
  at load and in `setPocket`, so a rescue copy is the stored
  entry itself, never a re-encode). No storage-format change;
  all mutations still go through `setPocket(int, byte[], String)`.
- `plugin.yml` — version 0.1.1; usage names the admin forms;
  new permission `excelpouch.admin` (default: op);
  `excelpouch.use` unchanged.
- `tests/AdminSelectorTest.java` (new) + one registration line
  in `tests/TestMain.java`; `build.sh` version/output knobs.
- **No change** (brief §2 no-touch list, honoured): PouchListener,
  PouchHolder, PouchService, api/PouchQuery, core/KindMatcher,
  core/WandInspector, core/Nbt, core/PouchLayout,
  core/PouchFileCodec, core/RefusalThrottle, config.yml (no new
  keys), and the whole ExcelMenuJoin tree.

### The designed set, as built

- `/pouch admin status` — global readout in the design §4.1
  shape (version/slots/allowlist/wand key; loaded stores vs data
  files + quarantined count; PouchQuery registration).
- `/pouch admin status <player>` — online or offline; one line
  per stored pocket **including empty and HIDDEN pockets**, in
  store order; UNREADABLE wording for undeserializable pockets;
  BROKEN line for quarantined stores; the no-file line
  (`No pouch on file for <name> — treated as empty; nothing was
  created.`) for a target with nothing on disk.
- `/pouch admin return <player> <selector>` — online only
  (offline refuses with the design's one-liner); `addItem` on a
  clone, leftovers = pocket left exactly as-was with the
  `Inventory full — …` line; already-empty is a no-op success;
  sealed pockets refuse and point at clear.
- `/pouch admin clear <player> <selector> confirm` — the only
  deleting op, under the D10 stack below; works offline; broken
  stores refuse; sealed pockets may be cleared under the stack.
- `/pouch admin reload` — re-reads config.yml, reports the
  effective values in the global-status format; loaded stores
  untouched, a shrink deletes nothing.
- Every mutating op logs the §4.0 audit line at INFO:
  `Pouch admin <return|clear>: actor=<CONSOLE|player-name>
  target=<name> (<uuid>) selector=<…> kind=<…> bytes=<n>`.
- No destructive defaults: no `all` target, no clear-everything
  form, no force flag. Bare/malformed admin forms print help and
  mutate nothing.

### The D10 safety stack, as built (brief §3 — order load-bearing)

1. **Confirm literal gate first** — without trailing `confirm`,
  clear prints what *would* be removed (full §4.1 description)
  plus `Re-run with 'confirm' to delete it — or use return to
  hand it back instead.` and changes nothing: no close, no
  rescue file, no mutation.
2. **Close-GUI-first** — for an online target with the pouch GUI
  open: `closeInventory()` first (the close-sync lands), then
  the pocket is re-read and re-validated against the post-sync
  state; a different item there aborts the op with a report.
  Never mutate-first/close-second. The same sequencing applies
  to `return`.
3. **Prior rescue copy, written and verified BEFORE deletion** —
  the pocket's raw Base64 entry verbatim to
  `plugins/ExcelPouch/data/rescue/<uuid>-pocket<n>-<yyyyMMdd-HHmmss>.b64`;
  the written file is read back and byte-compared; any write or
  verify failure aborts the clear. The success line names the
  rescue file.
4. **Delete only through `PouchStore.setPocket(n, null, null)`**,
  then the audit log line.

### Carried points, as built (brief §4)

- **Design-D2 — grant / park-on-behalf: EXCLUDED.** No grant op
  exists in 0.1.1.
- **Design-D4 — selector primacy:** `pocket:<n>` canonical; kind
  is a convenience that refuses on multiple matches with the
  pocket list.
- **Design-D5 — offline return: online-only.** No offline
  destination (no drop, mail, holding chest, or pending store)
  was built; offline rescue is status + clear.
- **Design-D6 — rescue retention: no expiry, no auto-prune.**
  Nothing in the plugin deletes, rotates, or caps rescue files.

### Design-D3 flag (brief §5) — honoured

**No reset/recovery op is built.** A reset that discards,
overwrites, or deletes a `<uuid>.yml.corrupt` quarantine would
be an irreversible deletion of player data across the hard
line; it is not rescued by a `confirm` literal. What 0.1.1
ships for broken stores is the non-destructive half only:
`isBroken()` visibility, the BROKEN status line, and
return/clear refusal on broken stores. The brief's reversible
preserve-first `admin recover` shape is designed in brief §5
and deliberately NOT built here.

## Tests

- **Desk: 95/95** via `run-tests.sh` — the existing **72-check**
  suite (NbtReader, WandInspector, KindMatcher, PouchLayout 17,
  StoreCodec 10, RefusalThrottle) passes unchanged, plus the new
  **AdminSelectorTest: 23 checks** (kind 0/1/many with the MANY
  pocket list; `pocket:<n>` in range, out of range, negative,
  and addressing a config-hidden pocket; malformed forms: empty,
  blank, null, `pocket:` with no/invalid/decimal number, unknown
  kind, unknown prefixed form; case-insensitivity).
- **Deterministic double-build byte-identical** at the shipped
  hash (see Candidate).
- **Rig** (Paper 26.2 + Magic 11.2.5 + ExcelMenuJoin 1.4.0,
  NMS fake players, `rig-0.1.1/`): clean 0.1.1 enable, SEVERE 0,
  clean stop, **44/44 behavior checks** through the real console
  sender covering brief §6.2 (a)–(k) plus the offline
  return/clear segment — full table, verbatim console lines,
  iteration history, and stated limitations in
  `rig-0.1.1/RIG-RESULTS.md`; log: `rig-0.1.1/boot.log`.

## Deviation from the brief (one, with reason)

**§3 step 2 revalidation compares item identity, not raw
bytes.** As first built, the post-close revalidation compared
the pocket's raw bytes to the pre-close bytes. Rig run 1 proved
that unworkable for wands: the GUI round-trip re-serializes the
item and Magic refreshes volatile wand fields
(mana / mana_timestamp — the 0.1.0 rig's own recorded finding),
so EVERY wand return/clear under an open GUI aborted even
though the pocket held the very item the operator confirmed.
The shipped revalidation compares what step 1 actually
describes — the §4.1 item identity: kind, material, amount,
display name, and wand verdict + owner_id. A genuinely
different item in the pocket (rig k2: wand swapped to a compass
inside the GUI) still aborts, and the rescue copy is always
taken from the post-sync bytes, so the exact final state is
what gets preserved. Everything else in the brief was built as
written.

## Surfaces checked (brief §7 — expected no-change, confirmed)

- Guide knowledge/intents: operator-only commands — no player
  teaching owed; nothing added.
- Website pair, What's New BOTH surfaces, broadcaster tips,
  in-game menus (ExcelMenu / ExcelBedrockMenu): no player-facing
  claim about pouch admin exists to correct; no changes made.
- The one documentation surface owed — an operator-facing note
  that console rescue for the pouch now exists — is this build
  record plus the install packet's console-smoke section.

## Live proof owed after install (brief §6.3, not part of the build)

One console `pouch admin status` against a real family member's
pouch, read back in the server log. No player-visible check is
owed — no player surface changes.
