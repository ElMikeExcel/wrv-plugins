# BB-18 fix build — BossTrial 0.5.7 (board supervisor run 425, worker W6)

- Run: 425 (W6 — BB-18 fix build). Date: Mon Nov 16, 2026 (America/Chicago).
- Scope: build only. No installs, no MANIFEST writes, no publications,
  no edits to the tree of record (all work on a copy, §2).
- Premise: W5 verification (`~/workspace/board-r425/bb18-verify/REPORT.md`)
  verdict (b) — the installed BossTrial 0.5.6 claims chest
  (`/bossrewards`, "Your earned rewards", 54 slots) renders only
  45 Prev / 49 Refresh / 53 Next and has no Back. This build adds the
  fleet-standard Back (BB-14 BackNav pattern) as BossTrial 0.5.7.

## 1. Candidate jar (FINAL)

- `~/workspace/board-r425/bb18-fix-build/ExcelBossTrial-0.5.7.jar`
  — **472,525 B / SHA-256
  `4b737c584dae9d9d6ebcffe85dc0899fbc6918d06f7c3818b9e8a5bd9bfc1e01`**
- Deterministic: `package.sh` run twice on the fixed tree — both jars
  byte-identical at `4b737c58…` (`cmp` clean).
- Version inside the jar: `plugin.yml` reads `version: 0.5.7`;
  `excel-build.txt` reads build-id
  `EXC-BOSSTRIAL-0.5.7-WRV-20261116-01-bb18`, version 0.5.7.
- Jar-entry diff vs the 0.5.6 candidate of record (471,888 B /
  `005bd005…`): 244 → 244 entries, none added or removed;
  content-changed entries are exactly the `PersonalClaimsUi` class
  family (`PersonalClaimsUi.class`, `PersonalClaimsUi$1.class`,
  `PersonalClaimsUi$View.class` — synthetic/inner classes of the one
  edited source file) plus `plugin.yml` and `excel-build.txt`.

## 2. Base and work area

- Tree of record (never edited):
  `~/workspace/bosstrial-056-fsync2-rebuild-20261114/bosstrial/`
  (the fsync2 0.5.6 tree per W5 §2/§5).
- Work copy: `~/workspace/board-r425/bb18-fix-build/work/bosstrial-057/`
  (verbatim copy; its pre-edit `PersonalClaimsUi.java` hashed
  `c18074a6…`, matching W5's record).
- BASE GATE (house pattern — PASSED before any edit): `package.sh`
  on the unmodified copy produced a jar **byte-identical to the
  installed 0.5.6 candidate** — 471,888 B /
  `005bd005ff015dc7b7c1f9e271a3606e3601a9e2732cfc5ce603d8caea53449a`;
  suite 2,792 checks / 101 harnesses, 0 failures — exactly the
  fsync2 rebuild's tally. Log: `work/base-gate.log`.

## 3. The change

Full unified diff: `PersonalClaimsUi-0.5.7.diff` in this folder.
Tree diff vs the tree of record = exactly 5 paths:

| Path | Change |
|---|---|
| `src/com/winkreyes/teamrewards/ui/PersonalClaimsUi.java` | **The fix** (166 → 213 lines; +47 net) |
| `plugin.yml` | version 0.5.6 → 0.5.7 + the 0.5.7 description sentence |
| `excel-build.txt` | version / build-id / build-date bumped to 0.5.7 |
| `tests/com/winkreyes/excelbosstrial/core/PluginYmlParseTest.java` | version pin 0.5.6 → 0.5.7 + one added description-sentence check (the house companion pin every version bump updates; no production behaviour) |
| `tests/com/winkreyes/teamrewards/ui/PersonalClaimsUiBackNavTest.java` | **New** — the BB-18 rig harness (§5) |

Production-source files changed: **1** (`PersonalClaimsUi.java` only),
as commissioned. The fix inside it:

1. **Contract constants** on the class, verbatim from BB-14's
   `BackNav`: `BACK_SLOT = 48`, `MENU_COMMAND = "menu"`,
   `BACK_LABEL = "§eBack to Server Menu"`,
   `BACK_LORE = "§7Return to the Server Menu"`,
   `BACK_FAILURE_LINE = "§cThe Server Menu could not open."`,
   plus `isBackSlot(int)`. (Constants live on `PersonalClaimsUi`
   itself rather than a new `BackNav` file, per the one-source-file
   commission; the values are the fleet contract unchanged.)
2. **Render**: `setItem(BACK_SLOT, backIcon())` in `open(...)`
   between the 45 and 49 placements. `backIcon()` builds an ARROW
   whose name/lore are deserialized through
   `LegacyComponentSerializer.legacySection()` (the serializer
   `FeedbackDisplay` already uses in this codebase), so the § codes
   render as colour — the neighbouring `icon(...)` helper is plain
   text by design and was left untouched.
3. **Click**: the navigation branch now reads
   `slot==45 || isBackSlot(slot) || slot==49 || slot==53`. A Back
   tap schedules next tick (never inside the click transaction,
   same discipline as the claim path), then — if the player is
   still online — `player.closeInventory()` followed by
   `player.performCommand(MENU_COMMAND)`; on `false` the player is
   sent `BACK_FAILURE_LINE`. The close fires the existing
   `InventoryCloseEvent` handler, so session-close semantics
   (`notifyClosed`, AUTO-origin review frames) are exactly the ESC
   path's — unchanged. Paging and claim clicks are untouched.

Slot choice: 48 is unwritten in 0.5.6; claim entries fill slots
0–44 only (`ClaimSession.visible()` caps at 45 entries), paging
sits at 45/49/53 — 48 collides with nothing and keeps Refresh
centred at 49 (W5 §5's scoping).

## 4. Compile (verbatim)

Build method: the tree's own `package.sh` → `build-test.sh`
(JDK 25 at `~/workspace/.jdk25`, `--release 21`, dependency
classpath per the original build's `work/classpath.txt`, copied to
`work/classpath.txt`). Final build log: `work/build-057.log`.

```
Note: Some input files use or override a deprecated API.
Note: Recompile with -Xlint:deprecation for details.
Note: src/com/winkreyes/excelbosstrial/ExcelBossTrial.java uses unchecked or unsafe operations.
Note: Recompile with -Xlint:unchecked for details.
PluginYmlParseTest: 61 passed, 0 failed
PersonalClaimsUiBackNavTest: 63 passed, 0 failed
PASS: compiled all source and ran all main-method harnesses. Build output: ephemeral local directory.
```

(The two `Note:` lines are pre-existing 0.5.6 warnings, also present
in the base-gate build.) First fixed-tree build attempt failed on
exactly one check — `FAIL: plugin.yml version is 0.5.6` from
`PluginYmlParseTest` — which is the descriptor's companion pin;
updating that pin (§3) made the suite green. No other failure
occurred at any point.

Full-suite tally (house `tally.py` over the final log):
**2,856 checks / 102 harnesses, 0 failures** — base 2,792 / 101,
delta +64 checks / +1 harness = `PersonalClaimsUiBackNavTest`
(63) + the one added `PluginYmlParseTest` sentence check (+1).

## 5. Rig evidence

Two layers (full output: `RIG-EVIDENCE.txt` in this folder):

1. **Plain-JVM harness** (the W5-scoped test; the tree has a harness
   — `build-test.sh` runs every main-method test under `tests/`):
   `PersonalClaimsUiBackNavTest` — **63 passed, 0 failed**. It pins
   the fleet contract verbatim (command `"menu"`, label, lore,
   failure line) and the slot geometry: `BACK_SLOT == 48`, bottom
   row, no collision with 45/49/53, `isBackSlot` true only at 48
   across −1…54.
2. **Bytecode/strings verification** of the built jar's
   `com/winkreyes/teamrewards/ui/PersonalClaimsUi.class`:
   - `strings`: `§eBack to Server Menu`, `§7Return to the Server
     Menu`, `menu`, `§cThe Server Menu could not open.` all present
     (alongside the pre-existing `Your earned rewards` /
     `Previous page` / `Refresh rewards` / `Next page`).
   - `javap -constants`: `BACK_SLOT = 48`,
     `MENU_COMMAND = "menu"`, `BACK_LABEL = "\u00a7eBack to Server Menu"`,
     `BACK_LORE = "\u00a77Return to the Server Menu"`,
     `BACK_FAILURE_LINE = "\u00a7cThe Server Menu could not open."`.
   - `javap -c`: render writes `bipush 45` (Prev), `bipush 48` →
     `backIcon()`, `bipush 49` (Refresh), `bipush 53` (Next);
     the click handler tests 45 / `isBackSlot` / 49 / 53, and the
     Back lambda invokes `Player.closeInventory()` and
     `Player.performCommand(String)`.
   - Slot-collision check: placements are exactly {45, 48, 49, 53}
     — pairwise distinct, and 48 ≥ 45 keeps it clear of the claim
     range 0–44.

In-game rig proof is still owed after install (§6) — no player/bot
tap can be run against a jar that is not installed.

## 6. Remaining chain (exact)

1. **G1 live-premise pull — OWED at filing.** A same-day live pull
   receipt for the live BossTrial jar (path, size, SHA-256,
   timestamp) must be taken before the packet is filed; nothing in
   this build asserts a live premise. Route note: the Homebase
   relay path was failing its probes this morning (W5: 3/3 fail,
   resets at relay `198.19.0.1:3128`; last supervisor-path success
   2026-11-16 05:24:09 CST) — re-probe with the 3-attempt/~2-minute
   pattern at filing; on continued failure use the labelled
   `FALLBACK (panel)` File Manager receipt per PACKET-TEMPLATE.md
   and give the one-line fallback notice.
2. **Packet filing** (supervisor): one pointer-stub MANIFEST entry
   per PACKET-TEMPLATE.md for `ExcelBossTrial-0.5.7.jar` —
   472,525 B / `4b737c58…` — carrying the G1 receipt, this folder's
   REPORT/diff as the packet record, and the proof-owed line.
   Expected live filename stays `ExcelBossTrial-0.4.1.jar`
   (the 0.5.6 install's retained name) unless the G1 pull says
   otherwise. No config merge (the claims menu has no config
   surface), no data change; rollback = restore the 0.5.6 jar.
3. **Failsafe install** under the standing gates (backup-first,
   cap-at-6, emptiness gate).
4. **Proofs after install**: Java rig — open `/bossrewards`, Back
   arrow visible at slot 48, tap → claims chest closes and the
   ExcelMenu Server Menu opens; one Bedrock spot-open of the same
   inventory (Bedrock sees the same chest UI by construction).
   Then BB-18 closes as Installed; Mike's batch spot-tap acceptance
   remains the separate player-proof remainder W3 recorded.
5. **Same-pass surfaces at filing/install**: What's New entry for
   the fix (both surfaces) + menus check; Guide/website/tips
   checked and named in the packet.

## 7. Deliverables in this folder

- `ExcelBossTrial-0.5.7.jar` — the candidate (472,525 B / `4b737c58…`)
- `PersonalClaimsUi-0.5.7.diff` — unified source diff (5 paths, §3)
- `RIG-EVIDENCE.txt` — strings + javap verification output (§5.2)
- `REPORT.md` — this report
- `SHA256SUMS` — hashes over the above, verified with `sha256sum -c`
- `work/` — the full 0.5.7 work tree (`work/bosstrial-057/`), build
  logs (`base-gate.log`, `build-057.log`, `build-057-second.log`),
  the base-gate jar, and the dependency classpath used
