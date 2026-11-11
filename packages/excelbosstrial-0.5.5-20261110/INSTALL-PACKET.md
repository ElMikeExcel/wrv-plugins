# PACKET — ExcelBossTrial 0.5.5, WARDEN FIRST-BREAK LAYER

**For the FAILSAFE lane (gated jar swap + config merge).**
Commission: Mike, Nov 10, 2026, APPROVED — the Warden
first-break package: the repeat slate at first-break values
PLUS first-break-only extras, installed before the Sat
Nov 14 Warden attempt (~10 PM CT). Design of record:
`~/workspace/warden-firstbreak-20261110/DESIGN-AND-BUILDSCOPE.md`.
Cash: decision-log D6 — **$10,000 per champion TOTAL**
(replaces the $5,000 repeat rate on this kill, NOT additive).

Candidate: `~/workspace/bosstrial-055-build/ExcelBossTrial-0.5.5.jar`
— **455,307 B / sha256 eade52aa49ab6eb46acc09dd5deed1c23b601be0b1aa5092f947f3cb89aaf0cb**,
deterministic double-build verified byte-identical.
Build ID: **EXC-BOSSTRIAL-0.5.5-WRV-20261110-01-887e6f**
(fresh tag; 0.5.4's 5371d5 never reused). Build record:
`~/workspace/bosstrial-055-build/BUILD-RECORD.md` (base proof
byte-identical to the shipped 0.5.4 jar before any edit;
full suite 2,588 checks / 0 failures / 92 harnesses by the
lane tally in TEST-RESULTS.txt; new FirstBreakRewardsTest
56/56 + FirstBreakConfigTest 21/21; desk rig boot clean,
first-break boot line exact, SEVERE 0).

## What changes for players

- **The first breaking of the Warden seal** (and only that
  kill) now pays, per champion: **$10,000 cash** (double the
  repeat rate, as the kill's total cash — a first break never
  paid the $5,000 row before and does not stack it now),
  **+2 jackpot rolls** over the bonus + jackpot tables,
  **5,000 mcMMO XP (SWORDS) + 250 MagicLevel XP**
  (Underdog-scaled, online champions), the **war chest**
  (death-protection premium + 2 h claim fly, online
  champions), **healer recognition** (Support Commendation to
  the support champion), and the **ECHO_SHARD "Warden
  Tribute"** cosmetic — the full repeat slate at first-break
  values, on the same /bossrewards claims rails.
- **First-break-only extras**, one per roster champion,
  claim-stored: the party title **"Sealbreakers"** (a name
  tag), a **"Heart of the Deep — Warden Trophy"** (ECHO_SHARD
  stamped with the earner's name + the date + the party
  line), and an **EliteMobs starter kit** of vanilla
  consumables (16 golden carrots, 1 enchanted golden apple,
  4 golden apples, 1 totem, 4 splash Healing II, 2 Strength
  II, 2 Swiftness II, 2 milk buckets, 16 XP bottles — no
  gear).
- A server-wide headline announces the first break.
- **Nothing else moves.** Repeat breaks pay exactly the
  0.5.4 slate; every other chapter's first break is
  untouched; the signature pool (Hushwalkers set, generation
  0) and the 24 h repeat window / clear-#1 bookkeeping are
  exactly as before. No damage number anywhere changes.

## Mechanism

- Trigger: verdict CREDIT on the Warden with the layer
  enabled and the once-flag unpaid. By construction of
  CreditJudge, a Warden CREDIT exists only while the seal
  stands unbroken — this is the break itself, never a repeat.
- Exactly-once: a persisted flag in BossTrial `state.yml`
  (`first-break-paid.warden: true`) plus the plaque record
  (`first-break.warden.date` / `party-uuids` / `party-names`)
  is written and flushed **before any first-break reward
  moves** — jackpot branch included. A crash can underpay,
  never double-pay; an admin revoke + re-break stays silent.
- All values are config (`first-break.warden.*`, mirroring
  the `repeat:` names); the EM kit is a config list, so a
  morning-review trim is a config edit, not a rebuild.
- No data migration: state.yml only gains keys; a rollback
  to 0.5.4 ignores them harmlessly.

## Gates (STOP on any mismatch)

- **Live jar** = the panel file `plugins/ExcelBossTrial-0.4.1.jar`
  (the standing live filename — the 0.5.4 jar lives under it)
  at **443,874 B / aa9db99da0ce22c5bc0af15b01906cbbce5f97e89432af111cb979cb75b169c2**
  (the 0.5.4 candidate, installed Nov 10, 2026 — see
  `~/workspace/elder-sprinkle-melee-fix-20261110/build-054/INSTALLED.txt`),
  with `ExcelBossTrial-0.4.1.jar.bak-0.5.3` beside it.
- **Live config** = `plugins/ExcelBossTrial/config.yml` at
  **23,232 B** (the post-0.5.4 merged file;
  `config.yml.bak-054` beside it), elder spot-checks per
  `~/workspace/bosstrial-055-build/live-config-merge/MERGE.md`,
  and NO `first-break:` key anywhere yet.
- **Candidate jar** = 455,307 B /
  eade52aa49ab6eb46acc09dd5deed1c23b601be0b1aa5092f947f3cb89aaf0cb
  (re-hash the staged file fresh at the window — the
  provenance gate always re-hashes).
- BACKUP FIRST with cap-at-6 (backup "BossTrial 0.5.5
  pre-install <date>"; prune OLDEST manuals, never the
  Daily Backup's newest copy or the two newest manual
  restore points; prunees named in the install report;
  re-read the backup list — the panel error banner can
  lie). Emptiness gate (0/10 twice) or the weekend /
  Mike-directed push protocol. Marker discipline: window
  marker at start, re-checked immediately before Shut Down
  and again before Start; a foreign marker under 3 hours
  old = coordination abort.

## Install steps (failsafe lane)

1. Gates above. Take the backup; apply cap-at-6.
2. Config safety copy: live config →
   `config.yml.bak-055` beside it.
3. Jar swap: rename the live jar aside to
   `ExcelBossTrial-0.4.1.jar.bak-0.5.4`; place the candidate
   under the SAME live filename `ExcelBossTrial-0.4.1.jar`;
   panel-verify 455,307 B / eade52aa… after placement.
4. Config merge (MERGE ONLY, per
   `live-config-merge/MERGE.md`): insert the headline
   snippet (3 lines, 240 B / d05a1f46…) after the
   `messages.repeat-credit-headline` line, and the
   first-break block (1,825 B / 9bd6f774…) immediately
   before the Village Standard comment block. Read back
   and byte-diff vs the pre-merge copy: the diff must be
   exactly those two snippets; post-merge size 25,297 B if
   the gate size held. The bundled config in the jar is
   NEVER uploaded.
5. ONE restart. Boot verification below.
6. Marker file on success:
   `~/workspace/bosstrial-055-build/INSTALLED.txt`
   (window times, backup name + prunees, placed hashes,
   post-merge config size, boot lines).

## Boot verification (all must hold)

- `Enabling ExcelBossTrial v0.5.5`
- `Build: EXC-BOSSTRIAL-0.5.5-WRV-20261110-01-887e6f`
- `Repeat layer: ON (window 24.0h base, signature generations
  on, cash $5000/champion, jackpot +2 rolls).` — unchanged.
- `First-break layer: ON (warden only, cash $10000/champion
  replacing the repeat rate, jackpot +2 rolls, title
  "Sealbreakers", EM kit 9 rows; warden unpaid).` — verbatim
  in substance (the desk rig produced this line exactly).
- Display-scale line x1000.0; boss voice line all four
  ARMED; Guild LOCKED 4 of 5; Done ~20 s; **SEVERE 0**.

## Proof plan (the Nov 14 Warden fight is the behavior proof)

The pure-logic half is unit-proven (eligibility matrix,
exactly-once sequence, slate + kit exact) and the boot half
is rig-proven; the payment path rides the repeat layer's
proven rails and has no verdict-driving desk rig (see
BUILD-RECORD). The family's first Warden break proves it,
with these PASS lines:

1. **Once-flag first:** at the kill, the console logs
   `FIRST-BREAK WARDEN: party=[…] date=…` BEFORE the
   first-break layer line, and `state.yml` carries
   `first-break-paid.warden: true` with the party + date.
2. **Cash:** each champion standing at the fall gains
   exactly $10,000 (not $5,000, not $15,000).
3. **Claims:** every roster champion's /bossrewards holds
   the Warden Tribute, the "Sealbreakers" name tag, the
   Heart of the Deep trophy (their own name on it, the full
   party on the party line), and all 9 kit rows; the support
   champion also holds the Support Commendation.
4. **Headline:** the first-break headline broadcasts once.
5. **No-regression controls:** the signature set
   (Hushwalkers etc.) also lands; a second Warden kill
   inside 24 h judges DENY_REPEAT_WINDOW; the first kill
   after the window pays the ORDINARY repeat slate
   ($5,000, no title/trophy/kit).

File the proof result on this packet's board item and in
FEEDBACK-TRACKER with the named testers = the Warden party.

## Post-fight step (after the first break, parent/supervisor)

**Guild-hall plaque (design §2.3.2):** read the party +
date from the `FIRST-BREAK WARDEN` record (console line /
`state.yml` `first-break.warden.*`). In-game, at the
guild-hall entrance wall (the hall opens at 5/5 — the
Warden break unlocks it; the wall choice is the family's,
no coordinates exist on record): place a spruce sign —
line 1 "✦ THE WARDEN ✦", line 2 "First broken by",
line 3 the party names (truncated to sign width; the full
party lives in the state record + What's New), line 4 the
date — optionally with one Heart of the Deep trophy in an
item frame beside it.

## Rollback

Restore `ExcelBossTrial-0.4.1.jar.bak-0.5.4` to the live
filename; one restart. The merged config may stay (0.5.4
ignores the `first-break.*` keys); restore
`config.yml.bak-055` only if a clean file is wanted. If a
rollback happens AFTER a first break was paid, the
`first-break-paid.warden` state keys must be KEPT (any
state.yml restore from before the break would re-arm the
package — flag it in the install report if it ever comes
up).

## Surfaces owed at install close-out (same pass, parent lane)

- What's New BOTH surfaces (website section + in-game
  `ExcelWhatsNew/changelog.yml`): one entry — the Warden
  first-break package (cash/title/trophy/kit).
- Guide knowledge + intents and website canon: the Warden
  rewards entry gains the first-break package.
- Rotation tips: check (a Warden-weekend tip naming the
  first-break spoils is appropriate if the rotation has a
  boss slot).
- Menus check: ExcelMenu + ExcelBedrockMenu — the
  /bossrewards entry already exists; verify no menu text
  states repeat-only rewards; expected no change, stated
  either way.
- Records: record package + Drive jar/source deposit +
  draft PR in wrv-plugins (stays an open draft), daily-log
  line, backlog/Route Sheet row updates.
- Post-fight (Nov 14): the plaque step above + a What's
  New/Guide line naming the actual party + date.
