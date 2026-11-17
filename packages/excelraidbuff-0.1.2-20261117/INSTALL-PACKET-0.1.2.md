# ExcelRaidBuff 0.1.2 — Install Packet ("Raid Expiry")

**Status:** BUILT + TESTED + RIG-PROVEN Oct 23, 2026. **NOT
installed.** This packet is the whole install: one jar swap and an
additive-only config merge (append the `expiry:` block to the live
config — NEVER replace the live config; its tuning columns and the
0.1.1 reveal block are the settled live values and must survive
byte-identical). Staged in
`~/workspace/wrv-install-queue/MANIFEST.md` for the install
failsafe / nightly window. **Do not install outside a sanctioned
window** (server empty twice-verified, per the standing rule).

**What it does (Mike's order, Oct 23, 2026, 7:28 AM CDT — "the
raid is still going; set a limit of after 6 hours it dies down
and they have to start over again"):** every raid gets a lifetime
cap (`expiry.hours`, default 6). The clock starts when the raid
triggers (or at first sight, for a raid already in progress) and
is persisted to `plugins/ExcelRaidBuff/expiry-starts.yml`, so the
daily 5 AM restart does NOT reset it — the hours the server
spends down count like any other hours, and fighting does NOT
extend the cap. At the cap the raid DIES DOWN: the raid state is
retired and its living members are removed where they stand —
no drops, no XP, no EliteMobs coins, no Hero of the Village;
the raid is neither won nor lost, and has to be started over.
One line to the players within 128 blocks of the raid center
("The raid has died down — the raiders have given up and gone.")
and a console line (`RaidBuff expiry: raid <key> died down …`)
for the nightly combat analysis. No damage path is touched:
every class except the adapter, the config record and the new
`ExpiryTracker` is byte-identical to 0.1.1, the compensation
API included — Rylie's 0.25 absolute is untouched, and GuildGate
sees nothing new.

**One honest bound:** a raid first tracked mid-progress starts
its clock at first sight — the plugin cannot know when it truly
began. (A raid in unloaded chunks is still on the world's raid
list, so its clock also starts at first sight; the rig expired
two such raids at their first-sight caps. That is the feature
working as documented: a stuck raid dies down instead of living
forever.)

## Artifacts

- `~/workspace/excelraidbuff/ExcelRaidBuff-0.1.2.jar`
  (42,438 B, SHA-256
  `009284dfcc91acbdcb237562d45fb79666f1c5c12cca15bc11507814d3e3149d`,
  MD5 `bbc6e7e71ee04131034f7d05c35283ad`)
- Source: `~/workspace/excelraidbuff/ExcelRaidBuff-0.1.2-source.zip`
  (56,552 B, SHA-256
  `ca8b8333f0e03361523d7d830cc19f35a17d7e212fa40a9741ce7dd324214c36`)
  + `~/workspace/excelraidbuff/ExcelRaidBuff-0.1.2-SOURCE.md`
- Drive-deposited, MD5 read-back verified Oct 23, 2026: jar in
  Minecraft WRV / Build files
  (`1cU_JH2oESmzHAKRu3dryHG7ls45Q-CqS`); source zip
  (`1kN91lQfA0E3dYugdbAiVBC_iLXtXX5r1`) + SOURCE.md
  (`1wQAkNJAsynM2TguTcqCRmhl2fTpzDYB1`) in Minecraft WRV / Source.
- Base discipline: the unmodified 0.1.1 tree rebuilt
  BYTE-IDENTICAL to the shipped 0.1.1 jar before the delta
  (`build-0.1.2/BASE-VERIFICATION.txt`, snapshot
  `build-0.1.2/base-0.1.1-tree-verified/`). **316/316 JVM
  checks** (275 carried + 41 new). A/B in-tree builds and a
  clean-room rebuild from the deposited source zip all
  byte-identical.

## Pre-install checks (ALL must hold)

1. Server empty (`list` = 0 players, twice).
2. Live jar is ExcelRaidBuff 0.1.1 (36,597 B, SHA-256
   `ab330c277b8c747b9947352609df29c36eb9df0ac97cfdd205b16ec2c8a19773`).
   If the live jar hash differs, STOP and report — the base moved.
3. Live config is the Oct 23 tuned config + the reveal block
   (5,029 B): HP x1.00/1.50/1.90/2.40/3.00, DMG
   x1.00/1.15/1.25/1.35/1.45, late-wave x1.25, share
   0%/8%/15%/22%/28%, level bonus +0/+0/+5/+10/+15, reveal ON
   quiet 180 s. If the live config differs, STOP and report —
   the merge base moved.
4. `/guildgate status` reads LOCKED 0 of 5.

## Install steps

1. **Back up, in `plugins/`:**
   - Rename `ExcelRaidBuff-0.1.1.jar` →
     `ExcelRaidBuff-0.1.1.jar.bak-<install date>` (the 0.1.1
     backup convention: `.bak-20261023`; use the install
     night's date).
   - Copy `plugins/ExcelRaidBuff/config.yml` →
     `plugins/ExcelRaidBuff/config.yml.bak-<install date>-pre012`.
2. **Swap:** place `ExcelRaidBuff-0.1.2.jar` in `plugins/`.
   Verify its hash on the server copy (SHA-256 above) before
   starting.
3. **Config merge (additive ONLY):** append this block to the END
   of the live `plugins/ExcelRaidBuff/config.yml`, changing
   nothing else in the file:

   ```yaml

   expiry:
     # Raid expiry (0.1.2, Mike's order Oct 23, 2026): a raid is a
     # siege, not a siege forever — after this many hours the raid
     # DIES DOWN and has to be started over. The clock is a hard
     # lifetime limit: fighting does NOT reset it (that is the
     # reveal's quiet clock above, a different thing), and it is
     # persisted to disk (expiry-starts.yml in this folder), so
     # the daily 5 AM restart does not reset it either — the
     # hours the server spends down count like any other hours.
     # When the cap lands, the remaining members give up and
     # leave: removed where they stand, no drops, no XP, no
     # coins, and no Hero of the Village — the raid is neither
     # won nor lost, it is over. One line to the players near it.
     # A raid first seen mid-progress (say, one whose chunk was
     # unloaded at a restart) starts its clock at first sight —
     # the honest bound of what the plugin can know.
     enabled: true
     # Lifetime cap in hours.
     hours: 6
     # The one line to players near the raid when it dies down.
     line: "&eThe raid has died down — the raiders have given up and gone."
   ```

   (The feature is also code-defaulted ON at 6 h without the
   block — the merge is for visibility and tuning, never a
   blind full-file replacement.)
4. **Start the server** (panel Start; verified-empty → straight
   Shut Down/Start, no countdown).
5. **Boot-verify** (see below).

## Boot verify (ALL must hold, else roll back)

- Zero SEVERE in the boot log; no ExcelRaidBuff error lines.
- The awake line reads the full tuned table and ends
  `; reveal ON quiet 180s; expiry ON cap 6h)`:
  `ExcelRaidBuff 0.1.2 awake: ENABLED (HP x1.00/1.50/1.90/2.40/3.00, DMG x1.00/1.15/1.25/1.35/1.45, late-wave HP x1.25 on final 2; elites ON share 0%/8%/15%/22%/28%, EM seam AVAILABLE, level cap 150; reveal ON quiet 180s; expiry ON cap 6h)`
- `/raidbuff status` shows the added line:
  `Raid expiry: ON, cap 6h, raids tracked 0` (a nonzero tracked
  count is legitimate ONLY if a raid is genuinely in progress
  in the world at boot — its clock started at first sight; the
  0.1.1 install showed the same shape for the reveal with a
  pre-existing world raid).
- The TrainingWheels raid-seam BOUND line still reads bound
  (the compensation API is byte-unchanged, but confirm).
- `/guildgate status` still reads LOCKED 0 of 5.
- `plugins/ExcelRaidBuff/expiry-starts.yml` exists once a raid
  has been seen (created on the first clock start / first
  save); absence before any raid is normal, not a failure.

## Player proof owed after install (one at a time, never batched)

1. **Event-driven (nobody stages this):** a real raid that
   crosses 6 hours dies down — the line is seen by whoever is
   near it, the remaining raiders leave with no loot, and no
   Hero of the Village is granted. The nightly combat analysis
   reads the `RaidBuff expiry:` console lines, so the proof
   lands in the record even if nobody is watching.

## Ship surfaces

No new command (nothing for ExcelMenu). The in-game announce
line is the player surface. **Owed to the install pass (parent):**
a website/Guide note that raids die down after 6 hours (no
loot, no victor, start over), per the three-surface rule.

**FLAGGED for Mike's call — rotation tip:** this build judges
that a rotation tip DOES fit the expiry (same family as the
raid-glow tip Mike approved Oct 23: it teaches a raid behavior
players will otherwise experience as a mystery — your raid
"ended" with no victory and no loot). The build does not
decide it; proposed text for approval: *"A raid that drags on
for 6 hours dies down on its own — the raiders give up and
leave with no loot, and it has to be started over."* (No
command in it, so no gold formatting applies.)

## Rollback

1. Stop the server.
2. Remove `ExcelRaidBuff-0.1.2.jar`; rename
   `ExcelRaidBuff-0.1.1.jar.bak-<install date>` back to
   `ExcelRaidBuff-0.1.1.jar`.
3. Restore `plugins/ExcelRaidBuff/config.yml` from
   `config.yml.bak-<install date>-pre012`.
4. Start; verify the 0.1.1 awake line (ends `; reveal ON quiet
   180s)`, no expiry suffix) and zero SEVERE.
   (`expiry-starts.yml` may be left in the data folder — 0.1.1
   never reads it; deleting it is cleaner but not required.)

## Notes for the executor

- `/raidbuff reload` re-reads the expiry block safely mid-raid.
  Turning expiry (or the plugin) OFF in a reload retires every
  tracked clock, persisted file included; turning it back on
  starts clocks fresh at first sight — the same semantics as
  the reveal's reload retirement.
- The TrainingWheels hard gate does NOT re-apply to this swap:
  0.1.2 touches no damage path (every damage-math class and
  the compensation API are byte-identical to 0.1.1). Rylie's
  0.25 absolute is untouched.
- While testing, the cap accepts fractional hours (0.02 = 72 s)
  — that is how the rig proved it; the shipped default is 6.
- Marker when installed: `~/workspace/excelraidbuff/INSTALLED-0.1.2.txt`
  (date/time, boot line, verify results).
