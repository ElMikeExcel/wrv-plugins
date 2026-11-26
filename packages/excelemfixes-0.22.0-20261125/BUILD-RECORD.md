# BUILD RECORD — ExcelEMFixes 0.22.0, quest-xp module (D244 fold of ExcelEMQuestXP 1.1.0)

Built Nov 25, 2026 (evening, CT) by the fold lane, working only
inside `~/workspace/excelemfixes-questxp-fold-20261125/`.
Ruling: **D244** (Mike, Nov 25, 2026, 8:07 PM CT) — fold
ExcelEMQuestXP into ExcelEMFixes. Reasons carried in the module's
README section + config comments: **D136** (Nov 19, 2026 — Jester's
ask: quests felt pointless paying no XP; Mike approved quests
paying EliteMobs XP) and **D159** (per-player XP target choice).
Not installed by this lane; no live contact; no player messages.

## Artifact

- `packet-0.22.0/ExcelEMFixes-0.22.0.jar` — 596,100 B — sha256
  `49eae192df49fe029c7528e76769552d4faacc9bfedb3ce76a716e213aca9b04`
- Deterministic: two consecutive builds byte-identical.
- Version bumped in `ExcelEMFixes.VERSION`, `plugin.yml`,
  `build.sh` (output name + jar date 2026-11-25). ModuleRegistry
  registers `QuestXpModule` last — 24 modules total.

## What was ported (1.1.0 source → `excelemfixes.questxp`, verified from source, never memory)

Verbatim ports (package line only): `QuestXpTable`,
`GrantPolicy` (armor split, highest resolution, session
ClaimLedger), `BackpayTable`, `BackpayLedger`, `TargetPolicy`,
`TargetStore` — plus the two bundled data files byte-identical
(quest-xp.yml `821e7103…`, backpay.yml `b7e544b3…`; 112 quests,
130,355 total XP). One cosmetic change: TargetStore's save-header
comment now names the module (file shape unchanged).

Adapted ports:
- `SkillGrant` → **`EmQuestXpBridge`** — the ONLY quest-xp class
  touching EliteMobs types. Grant mechanics verbatim: raw
  `PlayerData.addSkillXP`, display refresh (CombatLevelDisplay +
  ArmorSkillHealthBonus), the grant/level-up player messages,
  target resolution, the `Audit` callback into grants.log. Probe
  set = the same 7 classes 1.1.0 pinned; EM_PIN "10.9.14".
- `QuestXpListener` — 1.1.0's listener unchanged in behavior
  (NORMAL priority, ignoreCancelled, ClaimLedger tryClaim /
  release-on-failure).
- `ExcelQuestXP` (main) → **`QuestXpModule`** (FixModule +
  CommandExecutor): onCommand (target / menu / reload / backpay /
  bare / console status), applyTargetChoice with 1.1.0's exact
  messages, runBackpay with ALL 1.1.0 guards (entry exists → ledger
  readable → player ONLINE with data loaded → live QuestsCompleted
  counter == staged snapshot; write-through ledger save after
  EVERY grant; configured-zero quests skipped), grants.log audit
  (America/Chicago ISO stamps, BACKPAY tag). `/questxp` is
  registered dynamically via `ctx.registerCommand` — with the
  module off, the command does not exist.
- plugin.yml declares `excelquestxp.admin` (op) +
  `excelquestxp.target` (true) — 1.1.0's node names kept so any
  existing permission grants carry over; undeclared they would
  default to op and lock players out of target/menu.

## The exactly-once mechanism (the hard core)

- **Storage:** everything lives in
  `plugins/ExcelEMFixes/quest-xp/` — the same five filenames as the
  standalone's folder.
- **Migration (install packet performs it):** copy the ENTIRE live
  `plugins/ExcelEMQuestXP/` state set into that folder BEFORE the
  first 0.22.0 boot; sha256-verify every file against its source;
  the standalone jar is removed BEFORE the boot, so the two grant
  listeners never coexist at a boot (both hear
  QuestCompleteEvent and own `/questxp` — coexistence = double
  grants).
- **Why a paid quest can never re-pay:** the module loads the paid
  ledger ONLY from its own folder; backpay re-reads the ledger
  from disk at the start of every run and writes through to disk
  after EVERY grant; a ledger file that exists but cannot be
  parsed sets the broken flag and backpay REFUSES (never read as
  "nothing paid" — 1.1.0 behavior, kept). Live grants carry the
  session ClaimLedger on top.
- **Why an owed quest still pays exactly once:** the staged
  backpay table migrates with the ledger; the counter guard,
  online guard and per-grant write-through are 1.1.0's, unchanged.
- **Boot read-back proof:** the module's ready line reports
  "backpay already paid: N" from the migrated ledger (live
  expectation: 46); the packet also requires the ledger file's
  sha256 to be unchanged after the boot.
- **Rollback guard (in the packet):** if any module-era backpay
  ran, copy the module folder's backpay-paid.yml (and targets.yml
  if pins changed) back over the standalone's before booting the
  standalone again.

## Deviations from the standalone (by necessity, stated plainly)

1. **Failure posture:** a probe/table failure makes the module
   IDLE + log; the pack never disables (1.1.0 disabled itself).
2. **Popup:** 1.1.0's popup was its OWN code (Java chest GUI in
   the Downed holder pattern + its own Cumulus reflection
   bridge) — it was NOT the pack's shared renderer. The fold
   re-expresses its content and order (Highest first, one row per
   weapon skill with level + XP, current choice marked, Close
   last; same title and intro) through the pack's shared Views
   renderer as `QuestXpView`, so Java/Bedrock parity comes from
   the renderer; a pick applies + confirms in chat + closes,
   exactly as 1.1.0's popup behaved.
3. **Console status line** names the module's new home
   ("ExcelEMFixes quest-xp module (formerly ExcelEMQuestXP
   1.1.0)"); all player-facing text is 1.1.0's verbatim.
4. **Rig method:** the brief said mineflayer bots; the mineflayer
   infra lives outside this lane's authorized folder, so the rig
   used the house recipe proven for this exact plugin in 1.0.1
   and 1.1.0 (NMS fake players + real CustomQuest objects + real
   QuestCompleteEvents), with every assertion read from EliteMobs'
   own PlayerData / player_data.db. Same evidence strength; the
   deviation is noted here rather than hidden.

## Tests

- Unit suite on the 0.22.0 tree: **1,908 passed / 0 failed**
  (0.21.0 baseline 1,843 + 65 new `QuestXpModuleTest` checks). The
  documented SkillNudgeMathTest tie flake did not appear.
- During test authoring, 4 initial failures were the TEST's own
  wrong expectations (a max-index arithmetic slip ×3; a quest
  reward value guessed from memory ×1 — the real table row is 20),
  each verified against the source/table and corrected. No product
  code changed for tests.

## Rig verdicts (Paper 26.3 + EM 10.9.10/internal 10.9.14; packet steps performed on the rig first; migration hashes MATCH ×5)

- **Phase A — 33/0:** module present (24 modules); no-double-pay
  for both already-paid fixture players (console backpay paid 0,
  totals + audit lines unchanged); fresh completion paid exactly
  20 → highest + 6 Armor (`target=highest`); pin set as a non-op
  player persisted; invalid skill refused; pinned completion paid
  20 → Wands + 6 (`target=pinned:WANDS`); owed backpay paid
  exactly 80 → pinned Wands + 26 Armor, BACKPAY audit line, ledger
  write-through on disk, re-run paid 0; menu + bare `/questxp`
  dispatched true.
- **Phase B — 12/0:** restart persistence (totals + pin), pinned
  routing continues (480 → Wands, +160 Armor), unpin restores
  HIGHEST (50 → Wands as highest, +16 Armor).
- **Phase C — 6/0:** toggle off → module absent, `/questxp`
  nonexistent, a real fired completion granted nothing, other 23
  modules enabled.
- **Boot SEVERE 0 in all phases.** Ready line on the rig:
  "quests in table: 112; total XP across table: 130355; backpay
  players: 5; backpay already paid: 3" (rig fixture counts; live
  will read 46 paid).
- Rig honesty notes (full detail in packet RECEIPTS.md): phase A
  needed one fixture reset (a stray phase-B boot from the
  reference server's leftover marker contaminated the fresh
  player's totals; the module's grants were proven correct from
  the audit lines even in the spoiled run); phase B needed an
  instrumented re-run after one stalled attempt; phase C needed a
  quest the fresh player could actually complete-event (two
  earlier picks returned no quest object from EM — completed /
  chain-gated — so no event fired and those attempts' no-grant
  checks were vacuous; the final run fired a real event).

## Packet

`packet-0.22.0/` — jar, deploy-config.yml (pure delta vs live
0.21.0 config: +57 lines, −0; the quest-xp section only),
INSTALL-PACKET.md (live-premise gate on 0.21.0 jar `4ecf2709…` /
552,041 B + config `949ff6d2…` / 81,482 B — both verified against
the in-folder 0.21.0 copies; sequence backup → migration →
jar swap → boot verify → ledger read-back), RECEIPTS.md,
SURFACES.md, SHA256SUMS (verified OK ×5).

## Filing + follow-ons

- MANIFEST PENDING pointer entry filed under
  `~/workspace/wrv-install-queue/MANIFEST.md` (D212) and read back.
- D228 drop(s) written for the install-pass surfaces (SURFACES.md:
  What's New ×2, Guide verify, GitHub release + records, tips/menu
  checks) — the install lane fires them at window close per the
  standing pattern; the drop is filed by this lane so the debt is
  owned either way.
