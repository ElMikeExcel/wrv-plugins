# BUILD RECORD — ExcelMenuJoin 1.4.4 (console admin)

Built Nov 10, 2026 (desk build lane for the WRV board supervisor).
Commission: the console-admin coverage audit
(`~/workspace/console-admin-audit-20261109/AUDIT.md` §2/§4 —
ExcelMenuJoin verdict FAIL, packet sized SMALL) under Mike's
Nov 3, 2026 console-admin rule: every Excel plugin ships
console-runnable admin commands to inspect and fix a player's
state. **Not installed.** The install packet is
`INSTALL-PACKET.md` in this folder; the MANIFEST entry is drafted
in `MANIFEST-ENTRY-DRAFT.md` for the supervisor to file after
verification (this lane did NOT touch the MANIFEST, the
parent-backlog, board state, or MEMORY.md). Records staged at
`~/workspace/records-catchup-20261103/packages/excelmenujoin-1.4.4-20261110/`;
GitHub/Drive legs are the parent's (see `PUSH-NOTES.md`). The
base packet folder (below) is untouched — its shipped jar still
hashes to its record value (verified at close-out). The build
folder keeps the commissioned name `menujoin-142-build` even
though the packet re-based (see next section).

## RE-BASE CORRECTION (read first — the version moved)

This packet was first built as "1.4.2" on the 1.4.1 tree,
because the commission and the Nov 9 audit both worked from
`~/workspace/excelmenujoin/src-1.4.1/`. That base was STALE.
The workspace's own records — the Nov 9 plugin-table canon
(`~/workspace/plugin-table-canon-20261109/`), the Nov 3
source-coverage index (B11 v9 partials), the Oct 27
jar-recovery prompt, and the Oct 30 daily log — establish:

- **1.4.2 shipped and installed Oct 24, 2026** (~11:37 AM CDT):
  the compass air-tap fix (canon; its jar appears in the
  Oct 27 installed-jar recovery list, SHA-256 877bf195…).
- **1.4.3 shipped and installed Oct 30, 2026** (3:35 PM CDT,
  directed push): Moon's reviewed packet — automatic menu
  dispatch routed through normal command-preprocess policy
  (BuildMode restrictions now apply), session-bound
  disabled/quit-safe join callbacks, CompassTap seam. Shipped
  jar on disk in its packet (below), hash-verified.

So the audit's FAIL verdict was read off a superseded tree —
but the verdict itself still holds at 1.4.3: the 1.4.3
plugin.yml declares no admin node and its handlers are the
same player-gated pair, verified directly from the 1.4.3
source during this rebuild. The first-pass "1.4.2" artifacts
(jar, source zip, rig, records package) were DISCARDED into
recoverable trash before anything was filed, installed, or
pushed — nothing outside this build folder ever carried them.
The packet was rebuilt from the verified 1.4.3 tree as
**1.4.4**, the next free version. Everything below describes
the 1.4.4 build only.

## Base proof

- Base source of record: the shipped 1.4.3 packet tree
  `~/workspace/moon-packets-audit/inspect/menujoin143/ExcelMenuJoin-1.4.3/`
  (plugin.yml 1.4.3, src/, tests/, RELEASE-NOTES.md; the
  requal rebuild of the same tree at `~/workspace/requal/menujoin143/`
  reproduces every shipped class byte-identically per the
  Nov 3 coverage index, which pins this packet as the source
  of record, PARTIAL → PINNED).
- Live gate fingerprint re-verified on disk at rebuild start
  and at close-out: `ExcelMenuJoin-1.4.3.jar` — 11,678 B —
  SHA-256 `178febcce9da81127e25dce07b99a985db63a91dbbfde5d7a434283794f9bfd7`
  (matches the coverage index and Moon's packet audit value;
  the packet's own SHA256SUMS verify 0 failures). Install
  receipt: `~/workspace/requal/menujoin143/INSTALLED.txt` +
  the Oct 30, 2026 daily log entry quoted in the coverage
  index (Done 20.791s, expansion menujoin [1.4.3], SEVERE 0).
- The 1.4.4 delta was applied to a copy of that tree in
  `~/workspace/menujoin-142-build/ExcelMenuJoin/` (copy verified
  byte-identical to the 1.4.3 packet tree — `diff -r` clean on
  src/ and tests/, `cmp` clean on plugin.yml — before any
  edit); the base packet folder itself was never edited. The
  packet's Moon tooling (build-and-test.py, evidence/,
  MANIFEST.json) was NOT carried into the build tree; the
  packet uses the house javac build/test scripts, as the
  Quartermaster/AimAssist packets do.
- Baseline in the new tree, before the delta: the carried
  suite ran **21/21**, matching the 1.4.3 packet's
  retained-tests.txt count exactly (14 pouch + 5 guard +
  2 CompassTap).

## Candidate

- `ExcelMenuJoin-1.4.4.jar` — **14,080 B** — SHA-256
  `4df80550fa4ecf653534c32c5eacd597ad4f24fd0fd8d1af0956cf71ee7c5068`
- Deterministic double-build byte-identical (two consecutive
  `build.sh` runs, same hash).
- Build ID: this lineage carries no in-jar build-ID fingerprint
  — the artifact is identified by version 1.4.4 + the SHA-256
  above.
- `ExcelMenuJoin-1.4.4-source.zip` — 20,624 B — SHA-256
  `29e63d8cf7bb89e0944945f55afd7dfba47f3a1f3459721ddac8b64f74f05fc4`
  (the whole ExcelMenuJoin/ tree; timestamps pinned 2026-11-10).
- Jar-level delta vs the SHIPPED 1.4.3 jar: `AdminCommands.class`
  added, `ExcelMenuJoin.class` and `plugin.yml` changed,
  MANIFEST.MF Created-By stamp differs (JDK 25.0.4.1 here vs
  25.0.4 in Moon's build). CompassTap, ExcelMenuJoinExpansion
  and PouchCompassCheck classes are **byte-identical to the
  shipped jar** under the house recipe. One disclosure:
  `ExcelMenuJoin$JoinTicket.class` also differs from the
  shipped jar despite its source being unchanged — a record
  classfile sensitive to the JDK patch difference above (the
  same artifact class the 1.4.3 requal documented for its own
  rebuild); its source is byte-identical to the base tree and
  the class is exercised by rig checks 4–5 (the session-bound
  join give works).

## What shipped (the whole change, nothing else)

Player-facing behaviour change: **none.** The join/respawn
compass give (1.4.3 session-bound form), the 1.4.2 air-tap and
1.4.3 preprocess-policy menu open, drop/death protection, the
self toggle, /compass, and %menujoin_status% are
behaviour-unchanged. Change-scope diff vs the 1.4.3 tree —
only these files:

- `src/.../AdminCommands.java` (new) — Bukkit-free home of the
  admin surface: the gate decision (`allowed(isConsole,
  hasPermission)` — console explicitly allowed, the
  ExcelCosmetics pattern; anyone else needs the permission),
  and the exact reply texts for status, toggle, compass
  given / not-given, the three not-found rejections, the three
  usage lines, and the permission denial. Status formats state
  the caller already read and is read-only by construction.
- `src/.../ExcelMenuJoin.java` — an `admin` dispatch in the
  toggle command routed BEFORE the player-only gate, plus a
  private `handleAdmin` (gate check; status/toggle/compass
  dispatch; the target resolved by `getPlayerExact` to an
  ONLINE player only) and `resolveAdminTarget`. Status reads
  through the existing `isDisabled` / `hasMenuCompass`;
  toggle flips through the existing `setDisabled` (dual-key
  write, exactly as the self toggle writes); compass gives
  through the single existing `giveMenuCompass` path, so the
  1.4.0 Utility Pouch rule applies to an admin give too.
  Toggle and compass outcomes are logged. Class javadoc gains
  the 1.4.4 paragraph. Everything below the new branch — the
  self-service toggle and /compass bodies, the 1.4.2/1.4.3
  tap, join-session and preprocess code — is byte-for-byte
  the 1.4.3 code.
- `plugin.yml` — version 1.4.4; the excelmenujoin description
  and usage now name the admin forms; a NEW permission
  `excelmenujoin.admin` (default: op — the console satisfies
  op). The command-level `permission: excelmenujoin.toggle`
  (default true) and both player nodes are unchanged, and the
  admin gate is enforced in code on the admin branch only, so
  the self-service forms stay open to every player. The
  harness pins all of this.
- `tests/.../AdminCommandsTest.java` (new, 23 checks), 7 new
  admin-wiring checks in `PluginYmlGuardTest` (its no-depend
  regression checks carried, version expectation updated),
  one registration line in `TestMain.java`;
  `build.sh` / `run-tests.sh` house scripts (see Base proof).
- **No change:** there is no config file in this plugin (none
  existed in 1.4.3, none is added — nothing to keep identical),
  CompassTap, ExcelMenuJoinExpansion, PouchCompassCheck, and
  every other 1.4.3 source and test file (verified by the
  tree diff).

### The commands, as built

- `/excelmenujoin admin status <player>` — one line, e.g.
  `MenuJoin status for RigJava (<uuid>): join auto-open ON; menu compass held.`
  (auto-open is the inverse of the stored disabled flag,
  spelled out as ON/OFF; compass held reads the live
  inventory through the plugin's own hasMenuCompass).
- `/excelmenujoin admin toggle <player>` — flips the target's
  auto-open flag through the same setDisabled call the self
  toggle makes, e.g. `MenuJoin auto-open for RigJava (<uuid>) set to OFF (was ON).`
  Also written to the server log by the plugin.
- `/excelmenujoin admin compass <player>` — gives through the
  single give path: `Gave a Server Menu compass to <name> (<uuid>).`,
  or, when the player already holds one in the inventory or
  the Utility Pouch, the not-given line and nothing is handed
  over (never a duplicate). Also logged.
- Malformed admin forms print the three usage lines; an
  unresolvable target prints the per-form "Nothing was
  looked up / changed / given." rejection; a non-console
  sender without the permission gets the denial naming the
  node. Self-service `/excelmenujoin` and `/compass` behave
  exactly as in 1.4.3 (rig checks 22–26).

### State-model honesty (nothing invented)

- All three forms operate on the plugin's real state: the
  auto-open flag is the player's own PDC entry — the same
  entry the self toggle, the join handler and
  %menujoin_status% read — so an admin status can never
  disagree with what the player experiences, and an admin
  toggle takes effect at the target's next join with no
  relog needed for the flag itself.
- The one limit of the existing model is reported, not
  papered over: that flag lives ON the player, not in any
  plugin-owned file or map, and Bukkit can only read or write
  it while the player is online; this plugin keeps no
  name→UUID record of its own. So — unlike the AimAssist /
  Quartermaster packets, whose file-backed stores allow a raw
  UUID form — all three admin forms here take an ONLINE
  player by exact name only, and the rejection lines say why.
  No offline form exists and none was invented. Compass
  requires the target online by definition in any case.
- Out of scope by the audit's sizing, and NOT built: any
  change to the give/join/tap behaviour, any config key
  (the plugin has none), any menu or placeholder change, and
  any offline name→UUID lookup.

## Tests

- **Desk: 51/51** via `run-tests.sh` — the existing
  **21-check** 1.4.3 suite (PouchCompassCheckTest 14 +
  PluginYmlGuardTest 5 + CompassTapTest 2) carried, with
  PluginYmlGuardTest extended by 7 admin-wiring checks
  (12 total: version 1.4.4, the no-hard-depend regression set
  unchanged, the admin node declared and defaulting to op,
  all three usage forms named, the command-level permission
  and compass node unchanged), plus the new
  **AdminCommandsTest: 23 checks** (the gate matrix — console
  allowed, permission allowed, non-admin denied; exact status
  replies for all four state shapes; toggle confirmations in
  both directions naming the was-state; compass given /
  not-given texts; all three not-found rejections; usage /
  denial texts).
- **Deterministic double-build byte-identical** at the shipped
  hash (see Candidate).
- **Rig** (minimal Paper 26.2-123, the 1.4.4 jar + the live
  PlaceholderAPI 2.12.3 + a rig-only fake-player driver,
  commands dispatched through the real console sender,
  `rig-1.4.4/`): clean 1.4.4 enable with the expansion
  registered, SEVERE 0, clean stop, **27/27 behavior checks**
  — the 1.4.3 session-bound join compass + default flag
  (existing behaviour), status in both flag states, both
  admin toggle directions with independent PDC verification
  (both namespaces, both shapes), the admin compass give +
  no-duplicate refusal, all three bad-name rejections,
  bare/bogus usage forms, the console self-form gate lines
  byte-for-byte the 1.4.3 ones, and the player's own toggle
  + /compass regressions. Full table + limitations in
  `rig-1.4.4/RIG-RESULTS.md`; log: `rig-1.4.4/boot.log`. No
  plugin.yml or other defect surfaced in rig run 1 — the run
  passed as built, so there is nothing to disclose under the
  fix-and-pin rule.

## Surfaces checked (expected no-change, confirmed)

- Server Menu (ExcelMenu + the Bedrock form path): reads the
  same %menujoin_status% contract — untouched, and the rig
  proves the placeholder still registers at 1.4.4. No menu
  change owed.
- Guide knowledge/intents: operator-only commands — no player
  teaching owed; nothing added.
- Website pair, What's New BOTH surfaces, broadcaster tips:
  no player-facing claim about MenuJoin admin exists to
  correct; no changes made. (One stale-record note for the
  supervisor, not a surface change: the Nov 9 console-admin
  audit's §1b/§2 rows name ExcelMenuJoin "1.4.1" — the live
  version has been 1.4.3 since Oct 30; the audit FAIL verdict
  itself stands, verified against the 1.4.3 source.)
- The one documentation surface owed — an operator-facing note
  that console inspection/fix for a player's join-menu state
  now exists — is this build record plus the install packet's
  console-smoke section.

## Live proof owed after install (not part of the build)

The console smoke in the install packet (status/toggle/compass
against a real family member while they are online, read back
in the server log), plus the compass tap/click player proof
carried since 1.4.2/1.4.3 (air tap opens the menu; click opens
the Server Menu). No other player-visible check is owed — no
player surface changes.
