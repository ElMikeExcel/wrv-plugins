# ㉚.9 BUILD — POST-FIGHT STATS SURFACE (ExcelBossTrial 0.4.1)

Built Nov 8, 2026 under Mike's "Fix everything as best as you can" —
the follow-up build the ㉚ code phase named: ㉚.9's healing column was
hard-gated on ㉚.10's controlled-heal proof, and that proof PASSED in
the code phase (HoT heal credited 17.0 effective; splash credited as
POTION; Protect's fight_summary carries nonzero healing_done — see
`build/BUILD-RECORD.md`, item ㉚.10). **The gate is open; the healing
column is IN this build.** Build/test/stage only: no installs, no
panel, no sends, no GitHub writes were performed by this pass.

Design of record: `~/workspace/bosstrial-fix-20261108/DESIGN.md` §㉚.9
(+ PM-4's space-naming rule, PM-8's records, PM-9's split).

## Base and candidate

| Artifact | Size | SHA-256 | Base |
|---|---|---|---|
| `ExcelBossTrial-0.4.1.jar` | **411,565 B** | **`f7376dc93f86e866d7e1fa5553d5e66a09e41fdd0ca383682f48d32900da20f1`** | the ㉚ code phase's staged **0.4.0** tree (`build/bosstrial/`), copied byte-identical to `build-stats/bosstrial/` and built ON it — never on 0.3.7 — **REBASED Nov 8, 2026 onto the REBUILT 0.4.0 (see REBASE section at the end); the original candidate `8eb5a004…` / 404,260 B is VOID** |

Base jar verified before the copy: `build/ExcelBossTrial-0.4.0.jar`
hashes to `0638c15832d2b6b818f9f6dd2f04b63b703ce2764acd2378b2b92537a1bf4d00`
(the staged candidate of record at THIS build's start; superseded the
same evening by the rebuilt 0.4.0 `043c1558…` — the rebase below), and
the copied tree diffed clean against it (`diff -r`, 165 Java files). Toolchain identical to the
code phase: JDK 25.0.4.1, Paper-26.3 classpath from
`seal-tuning/build-0.3.6/classpath.txt`; packaged with package.sh
(which re-runs the whole suite; jar dates pinned 2026-10-28, so the
candidate hash is reproducible).

## What built

1. **Post-fight pop-up (every close).** `snapshotCloseRecord` — the
   one PM-8 close-record writer — now calls `presentFightStats(rec)`
   after the record is written and published. Every ending that
   writes a record (verdict, elder progress, give-up, prune,
   admin reset, seal grant, server stop) shows each still-online
   participant their own line. Java: chat. Bedrock: a Floodgate
   (Cumulus) SimpleForm carrying the same text, reflective and
   fail-soft to chat (the PM-11 seam shape). A player who set
   `/bosstrial feedback off` is not popped up at — automatic
   surfaces obey the display opt-out; the command below, being
   asked for, does not.
2. **`/bosstrial stats` (public).** Re-shows the caller's most
   recent CLOSED fight record: the full per-player table. The
   lookup scans `fights/*.yml` (the `-open-` mid-fight snapshots
   are excluded — they are a crash net, not an ending), pre-filters
   on the caller's UUID, and parses candidates with the new
   `FightTracker.parseRecord` — the exact inverse of
   `FightRecord.toYaml`, so the command re-shows the record the
   close wrote, never a second accounting. `CommandAccess` gains
   `isStatsCall` (public, like status/feedback: a player reads
   only their own record — the lookup is keyed to the caller).
3. **The PM-9 split is in both renderings.** Party line and table
   split line always carry players / other / boss-healed / net of
   pool; the table adds the verdict's own share figure (player
   damage vs the pool the gate judges on — it can pass 100 when
   the boss healed damage back and the party dealt it again,
   e.g. the spike's 103.1%).
4. **Space naming (PM-4).** Both renderings carry the header
   sentence "Every damage number is pool damage - the numbers
   the boss bar showed." No entity-space number appears anywhere
   on the surface.

### The delta (the rebase unit — see PACKET-DRAFT)

- NEW `core/FightStatsFormat.java` — pure rendering + readout math
  (shareOfParty, outcome words, durations, plain-text for forms).
- EDIT `core/FightTracker.java` — `parseRecord` + parse helpers
  only; no writer behavior changed (FightTrackerTest untouched and
  green at its 0.4.0 count).
- EDIT `core/CommandAccess.java` — `isStatsCall` + allowed().
- EDIT `ExcelBossTrial.java` — two config leaves read in
  loadSettings; the pop-up call in snapshotCloseRecord; the stats
  branch + usage line in onCommand; handleStats /
  mostRecentRecordFor + the Floodgate adapter methods.
- EDIT `config.yml` — `fight-tracking.stats-popup: true`,
  `fight-tracking.stats-command: true` (code-defaulted true; the
  keys exist for visibility).
- EDIT `plugin.yml` — version 0.4.1, usage gains `stats`, the
  0.4.1 description sentence appended (history kept whole).
- Tests: NEW `FightStatsFormatTest`; pins added in
  `CommandAccessTest` (+7) and `PluginYmlParseTest` (+3, version
  pin moved to 0.4.1).

## Exact formats

Pop-up (chat form; values from test fixture A — a 4-player CREDIT
Wither, shown to the healer, .MikeExcel):

```
&6⚔ The Wither &7- Seal earned
&7Every damage number is pool damage - the numbers the boss bar showed.
&fYou dealt &a1580.0 &7(7.0% of the party's damage) &7- healed &a17.0 &7- took &c412.0
&7Party: net &f21900.0 &7of &f21900.0 &7pool - players &f22680.0 &7- other &f470.0 &7- boss healed &f1250.0 &7- 23m 31s
&7Full table: &f/bosstrial stats
```

Table (`/bosstrial stats`, same fight, top dealer's view; the date
renders local, `MMM d, h:mm a`):

```
&6- The Wither fight record &7- Seal earned &6-
&7Every damage number is pool damage - the numbers the boss bar showed &7- Nov 14, 10:36 PM &7- lasted 23m 31s
&7Killer: &fJester_Turtle &7- standing at the close: &fJester_Turtle, .coolgirlkail, GumGumJetPistol, .MikeExcel
&e» #1 &fJester_Turtle &7- &a11000.0 dmg &7(48.5%) &7- &a0.0 healed &7- &a17.0 received &7- &c1204.0 taken
&7#2 &f.coolgirlkail &7- &a5800.0 dmg &7(25.6%) &7- &a0.0 healed &7- &a5.0 received &7- &c860.0 taken
&7#3 &fGumGumJetPistol &7- &a4300.0 dmg &7(19.0%) &7- &a0.0 healed &7- &a4.0 received &7- &c977.0 taken
&7#4 &f.MikeExcel &7- &a1580.0 dmg &7(7.0%) &7- &a17.0 healed &7- &a0.0 received &7- &c412.0 taken
&7The split: players &f22680.0 &7- other &f470.0 &7- boss healed &f1250.0 &7- net &f21900.0 &7of &f21900.0 &7pool - players dealt &f103.6% &7of the pool by their own hands
&7Healing with no named source: &f9.0 &7(spell 4.0 - other 5.0)
```

Bedrock form: title `<Boss> - fight record`, content = the same
lines with colour codes stripped (`FightStatsFormat.plainText`),
one Close button.

## Tests

- Baseline in the staging copy (pristine 0.4.0 tree): **27,225
  checks / 0 failures** — reproduces the code phase's count exactly
  (tally = Σ "N passed" + Σ "N checks" across the harnesses).
- After the build: **27,328 checks / 0 failures** (+103):
  - `FightStatsFormatTest` NEW — **93 checks**: readout math
    (shares incl. zero-party and sum-to-100, durations), outcome
    words (incl. unknown labels rendering as themselves), the
    pop-up and table against three PM-8 fixtures — a CREDIT close
    with a claim-attributed healer (17.0, the ㉚.10 rig figure at
    desk scale), a CLOSED_NO_VERDICT close (no killer recorded,
    partial net), and the healed-pool zombie shape (dealt 1,079,
    healed back 1,024, net 55) — the PM-9 split asserted whole on
    both renderings, the space-naming header asserted, the
    unattributed-healing line asserted present-with / absent-
    without, and the **round trip**: record → toYaml →
    parseRecord → field equality (at the file format's own
    one-decimal precision) and a byte-identical re-rendered table.
    Malformed input (foreign text, unparseable boss UUID, null)
    is refused, never guessed.
  - `CommandAccessTest` 34 → **41** (stats cells of the matrix).
  - `PluginYmlParseTest` 10 → **13** (version 0.4.1, the 0.4.1
    description sentence, both new config keys parse true).
  - `FightTrackerTest` unchanged at 68 — the writer was not
    touched.
- package.sh re-ran the suite green while packaging the
  candidate (exit 0).

## Design points the records could not support (with evidence)

1. **Data source is the PM-8 fight record, NOT Protect's
   fight_summary.** DESIGN §㉚.9 names the source: "the PM-8
   fight records (dealt, taken, player/other split, outcome
   label)". The record's healing-done column is BossTrial's own
   claim-matched attribution (Magic CastEvent claim + CUSTOM
   regain, effective-capped — the 0.3.1 mechanism FightTracker
   documents). Protect's fight_summary is ExcelProtect's
   entity-space accounting of the same fight; PM-4 forbids
   presenting the two spaces blended without naming both, so
   the surface deliberately reads ONE accounting. The ㉚.10
   proof that opened the gate was Protect-side (its readback
   convicted and fixed Protect's own feed); BossTrial's column
   never depended on that feed.
2. **Champions-standing UUIDs are not in the record files.**
   `FightRecord.toYaml` persists standing NAMES and a count
   only, so a parsed record's `championsStanding` UUID list is
   empty (documented on parseRecord). The surface renders
   standing from the names list — no display loss; anything
   needing standing UUIDs must read the live record, not the
   file.
3. **The Bedrock form path is compile-verified, not
   live-proven** — the same recorded limitation as PM-11: no
   Floodgate/Cumulus jar exists in any rig asset on this
   machine, so the Cumulus seam cannot execute here. The seam
   is the PM-11 reflective shape (fail-soft to chat at every
   step), and the live proof is owed on the family rig: a
   Bedrock participant at a scripted close receives the form;
   a Bedrock `/bosstrial stats` returns the form. Until then
   the Bedrock pop-up may in the worst case arrive as chat —
   degraded, never absent.
4. **Raid has no stats surface** — there is no BossTrial raid
   fight record (PM-8's own note: the raid lives in GuildGate;
   its claims path persists claims, not a ledger). The design
   matrix's "records permitting" is the operative word; nothing
   was fabricated for the raid.
5. **Stats answers closed fights only.** The `-open-` mid-fight
   snapshots are excluded from the lookup by name: they are
   PM-8's crash net, and their label (IN_PROGRESS) has an
   outcome word only so a leaked file still renders honestly.
   A live-fight readout is a possible small follow-up, not
   part of the design.
6. **One-decimal precision is the record format's own.** Files
   have written `%.1f` numbers since 0.3.1; the re-shown table
   is byte-identical to the close-time table because both
   render from the same precision (pinned by the round-trip
   test). No format change was made to chase precision the
   bar itself never showed.

## REBASE — Nov 8, 2026, evening (onto the REBUILT 0.4.0)

The 0.4.0 base was rebuilt the same evening (clamp raise 48 → 140 +
Wither taunts + loud sanitize; `build/BUILD-RECORD.md`, REBUILD
section), which changed the staged 0.4.0 hash from `0638c158…` to
**`043c15588ed844fa94ba3e93149101bca45e8e1366d8e71e433fbe436c1bafc3`**
(401,269 B) and voided this build's staged-hash gate. This tree was
rebased onto the rebuilt 0.4.0 the same evening:

- **Copied verbatim** (rebuild-only files, untouched by the 0.4.1
  delta): `core/SprinkleSettings.java`, `core/TauntBook.java` (new),
  `core/TauntSettings.java` (new) + tests `SprinkleSettingsTest`,
  `TauntBookTest` (new), `TauntSettingsTest` (new).
- **Ported by hand** into this tree's `ExcelBossTrial.java` (a
  both-sides-changed file): the taunt fields/imports, the taunt
  settings parse, the `tauntSummary()` boot line + helper, the
  loud-sanitize adjustments logging in the sprinkle parse, the
  regain-handler taunt hook, `maybeTaunt`, and the prune-sweep
  `tauntBook.retainOnly(...)` line — every hunk anchored on text the
  0.4.1 delta never touched, and verified after the port: the
  host-file diff between the two trees now contains ZERO
  taunt/rebuild lines (the remaining diff is purely the 0.4.1 stats
  delta, exactly the 9 files of record).
- **Config:** the `wither.taunts` + `taunt-cooldown-seconds` keys
  added to this tree's bundled config (same block as the 0.4.0
  tree's).
- **Suite:** pre-rebase baseline in this tree 2,078 checks / 73
  harnesses / 0 failures → post-rebase **2,109 checks / 75 harnesses
  / 0 failures** (same harness-summary-line tally as the 0.4.0
  rebuild record; the +31 is exactly the rebuild's own delta:
  SprinkleSettingsTest +7, TauntBookTest 15, TauntSettingsTest 9 —
  the 0.4.1 stats checks are untouched). As with the 0.4.0 record,
  this lane's tally method differs from the original lanes' 27,328
  figure; the reproducible facts are 0 failures and the exact delta.
- **Candidate:** double-built via package.sh, byte-identical:
  **`ExcelBossTrial-0.4.1.jar` = 411,565 B / sha256
  `f7376dc93f86e866d7e1fa5553d5e66a09e41fdd0ca383682f48d32900da20f1`**.
  The old candidate `8eb5a004…` / 404,260 B is **VOID** (it gates on
  the superseded `0638c158…`); never install it. This packet's gate
  now names the rebuilt 0.4.0 hash `043c1558…` (PACKET-DRAFT updated).
