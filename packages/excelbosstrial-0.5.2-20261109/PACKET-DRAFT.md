# PACKET DRAFT — ExcelBossTrial 0.5.2, COMBINED (Item 35 Packet B + boss voice)

**For a PARENT hand window.** This packet SUPERSEDES Packet B's
0.5.1 PACKET-DRAFT and the voice DESIGN §6 as the window
document (RECONCILIATION.md §5 step 6). It is the single-window
merge of both lanes: the 0.5.1 fight-feel set and the boss
voice set land together, with the two frozen voice packs, in
one restart. **Neither 0.5.0 nor 0.5.1 installs separately —
0.5.0 was never built and 0.5.1 never lands; both are
superseded by this candidate.**

Candidate: `~/workspace/bosstrial-052-build/ExcelBossTrial-0.5.2.jar`
— **438,791 B / sha256 1a866a54840dc510ad78cdbd384b75d49f82a25808a74724a0b4fc1e66f0944b**, deterministic double-build verified
byte-identical. Build ID:
**EXC-BOSSTRIAL-0.5.2-WRV-20261109-01-13447a** (fresh tag; the
0.5.1 tag a3aa16 is never reused). Build record: `BUILD-RECORD.md`
beside it (carried Packet B suite at 0 failures + 89 new voice
checks; rig-proven on Packet B's items + the log-level voice
seams).

Packs (FROZEN artifacts — do not rebuild; a rebuild mints new
Bedrock UUIDs and new hashes and must be re-recorded):
- Merged Java pack: `~/workspace/boss-voice-20261108/pack-build/wrv-merged-java-pack.zip`
  — 1,822,176 B / sha256 8e7d50e379b359670d965c144f2ba7dac230ce
  5257b65d1b3cd6235ec6fc5c54 (BrainGames tiles + narrator
  `guildgate.seal1-5` restored + 38 boss events; the narrator
  Java restore, decision #4, rides this file).
- Bedrock pack: `~/workspace/boss-voice-20261108/pack-build/wrv-bossvoice-bedrock.mcpack`
  — 1,631,924 B / sha256 b8a042abd679fc6fea44d8466bc6409d91f59f
  d2df4104da884e501da1780862 (same 43 events; boss `is3D: true`,
  raid `is3D: false`).

## Gate

1. **Marker discipline:** create
   `~/workspace/wrv-install-queue/WINDOW-IN-PROGRESS.txt` at
   window start; delete at finish. Before opening, confirm no
   other window/failsafe run is in flight.
2. **Live jar identity (the reconciliation replacement gate):**
   the panel file `plugins/ExcelBossTrial-0.2.9-team-r2.jar`
   must hash to the installed **0.4.1: 411,565 B / sha256
   f7376dc93f86e866d7e1fa5553d5e66a09e41fdd0ca383682f48d32900
   da20f1**. Any other hash = STOP and re-reconcile (the
   Packet B draft's "0.5.0 landed" STOP is retired by the
   reconciliation — 0.5.0 will never land).
3. **Packet A first:** pull the live `plugins/ExcelBossTrial/
   config.yml` and confirm Packet A's keys are present as
   filed (dragon theatre block, elder sprinkle arm, raid
   `pulse-hot-seconds` 45). If Packet A has NOT landed, land
   it first (its own packet), then this one — never merge the
   two config sets by hand in one edit.
4. **Pack hashes:** re-hash both pack files at the window;
   they must match the frozen hashes above exactly. Any other
   hash = STOP (someone rebuilt the packs; that is a new pack
   build, re-record before proceeding).
5. **BACKUP FIRST + CAP-AT-6:** fresh Shockbyte backup before
   the swap; prune oldest manual backups first so the
   post-creation total is ≤ 6; never delete the Daily Backup's
   newest copy or the two newest manual restore points; name
   prunees in the install report.
6. **Emptiness gate:** install only on a verified-empty server
   (`list` = 0 twice) or under the weekend/Mike-directed push
   protocol. (The pack swap touches every player's client —
   a fresh ~1.8 MB join-time download and a new prompt on
   Java; `require-resource-pack` stays **false**, standing
   house law, so declining degrades to subtitles, never a
   lockout.)

## The swap

- Replace `plugins/ExcelBossTrial-0.2.9-team-r2.jar` (same-name
  swap) with the candidate; keep the replaced 0.4.1 jar as
  `ExcelBossTrial-0.2.9-team-r2.jar.bak-0.4.1` in the same
  folder until the window closes clean.
- **Config merge — MERGE, NEVER REPLACE.** Touch ONLY the keys
  below in the live config; every other key (including ALL of
  Packet A's keys and all live tuning) stays byte-for-byte.
  Keep a pre-merge copy as `config.yml.bak-052` in the plugin
  folder. The merge list is ONE list in two parts:
  - **Part 1 — Packet B's merge, unchanged.** GATE-VERIFY the
    old values first (Packet A's interim dragon adds):
    `dragon.adds.per-wave` (expect **5**), `dragon.adds.max-alive`
    (expect **15**), `dragon.adds.wave-thresholds` (expect
    Packet A's six values). Then set `dragon.adds.enabled:
    false`; replace the `dragon.sprinkle` block with the armed
    design values (enabled true, cadence 180, stamp-multipliers
    [1.0, 1.0, 1.0], levels [18, 20, 22], max-alive 10,
    max-per-fight 40, leash-radius 48.0, waves = one ENDERMAN ×4
    ring); add `dragon.flip-crystals: {count: 4, max-live: 6}`;
    add `feedback.display-scale: 1000`; add
    `elder-guardian.relay.enabled: true`; add
    `messages.elder-relay: "&3The monument answers &7- its
    remaining guardians swell with rage."` (Full YAML in the
    superseded 0.5.1 PACKET-DRAFT, whose Part-1 text this draft
    incorporates by that reference.)
  - **Part 2 — the voice keys (new in this window).** Add
    exactly the bundled-config blocks (they are also the code
    defaults, so a key the merge misses degrades to the same
    value, never to silence):
    - `wither.taunt-sounds`: the 10 slugs `eat_01` … `eat_10`,
      index-aligned with the live `wither.taunts` list
      (GATE-VERIFY the live taunts are the shipped 10, in
      order — alignment is by index).
    - `dragon.voice`: open `open`, flip `flip`, beat-75
      `beat75`, beat-50 `beat50`, beat-25 `beat25`,
      beat-50-phase2 `beat50p2`, addwave `addwave`, verdict
      `verdict`.
    - `elder-guardian.voice`: open `open`, beat-75 `beat75`,
      beat-50 `beat50`, beat-25 `beat25`, relay `relay`,
      lastdoor `lastdoor`, verdict `verdict`.
    - `warden.voice`: open `open`, boom `boom`, boom-alt
      `boom_alt`, beat-75 `beat75`, beat-50 `beat50`, beat-25
      `beat25`, verdict `verdict`.
    - **No raid keys** — the raid's speaker is the GuildGate
      narrator and its playback is a later GuildGate packet;
      the 6 raid clips ride the packs inert in this window.
- **Java pack leg:** upload the merged Java pack to the
  website `packs/` hosting (same route as the tiles pack);
  compute its SHA-1; update `server.properties` in the same
  window — `resource-pack` = the hosted URL, `resource-pack-sha1`
  = that SHA-1, `resource-pack-prompt` gains the voices
  (bosses speak; declining still changes nothing but the
  sound), `require-resource-pack` stays **false**. Keep the
  pre-edit `server.properties` as a `.bak-052` copy.
- **Bedrock pack leg:** place `wrv-bossvoice-bedrock.mcpack`
  into `plugins/Geyser-Spigot/packs/`; CONFIRM the narrator
  mcpack (`wrv-guildgate-narrator-bedrock.mcpack`) is still in
  that folder (desk records say placed Oct 18 — confirm live,
  do not assume).
- One restart for the whole window.

## Boot verification (console, in order)

- `Enabling ExcelBossTrial v0.5.2`
- `Build: EXC-BOSSTRIAL-0.5.2-WRV-20261109-01-13447a`
- Damage feedback line contains `display scale x1000.0
  (feedback.display-scale)`.
- Elite sprinkle line contains `ender_dragon ARMED (cadence
  180s, cap 40) (phase 2 only)`; wither/elder/warden arms
  unchanged from the 0.4.1 boot.
- Boss taunts line unchanged (Wither ARMED, 10 lines, 25s).
- **Boss voice line:** `Boss voice: The Wither ARMED (10
  clips), An Elder Guardian ARMED (7 clips), The Warden ARMED
  (7 clips), The Ender Dragon ARMED (8 clips) - positional at
  the boss, participants only; raid clips ride the packs
  inert (GuildGate packet, later).`
- NO `max-per-fight … clamped` warning for the dragon
  (wither-only clamp warning is pre-existing).
- `SEVERE` count 0 attributable to ExcelBossTrial; Geyser
  boots clean with the new mcpack in its packs folder.
- Dragon chapter awake line unchanged (two-phase, caps);
  GuildGate LOCKED state unchanged; dragon `state.yml` build
  stamp advances to 0.5.2 on its next save.

## Post-install proofs (player-proof, scheduled with the family)

- **Packet B proofs (unchanged):** dragon flip re-lights 4
  crystals (never >6 trial crystals); phase-2 fountain
  enderman waves (milestone + 180 s cadence), none in phase 1;
  elder relay in play; V-1 spot checks per the design table.
- **Voice proofs (DESIGN §8):** Java hears the Wither at the
  boss (positional, subtitle-identical, cooldown-silent second
  eat); **Bedrock hears the same clip on the same eat**,
  direction/distance reading as at-the-boss — failure fires
  the DESIGN §2.2 fallback (at-player on both platforms) as a
  decision, not a shrug; volume/distance sanity at arena edge
  vs point-blank; a declined-pack Java client joins clean,
  hears nothing, still gets subtitles.
- **Pack-merge regression:** narrator seal line audible on
  Java again (closes the Nov 5 defect); BrainGames letter
  tiles still render on both platforms.
- **Chapter re-acceptance re-fights** (already owed): dragon
  flip skyline + flip line, phase-2 adds attacking + addwave
  line, elder relay in play + its line, last-door line on the
  third elder, Warden open/boom/beats in its first
  post-packet attempt (voice never gates the Sat Nov 14
  attempt), gear-ladder spot checks per chapter.
- **V-1 proper** (five real hits per chapter) remains its own
  parent-dispatched step — not closed by this window.

## Rollback

Restore `ExcelBossTrial-0.2.9-team-r2.jar.bak-0.4.1` over the
swapped name, `config.yml.bak-052` over the config, and the
`server.properties` `.bak-052` over the properties (the pack
pointer returns to the tiles-only pack); remove
`wrv-bossvoice-bedrock.mcpack` from Geyser's packs folder;
restart; confirm the 0.4.1 boot lines (no Build line, no
display scale, no Boss voice line). The fresh backup from the
gate is the second net. (The hosted merged pack file may stay
on the website — unpointed, it is inert.)

## Same-pass surfaces owed at install close-out

Guide knowledge + intents (dragon re-light, phase-2-only adds,
elder relay, ×1000 display numbers, boss voice + how the
packs/subtitles work), website pair, What's New BOTH surfaces
(website section AND in-game ExcelWhatsNew changelog.yml),
rotation tips, ExcelMenu + ExcelBedrockMenu if any entry text
names the old behavior — plus daily log, Route Sheet row, Big
Brain local + Drive, and the records publication for this
build. Records package staged at
`~/workspace/records-catchup-20261103/packages/excelbosstrial-0.5.2-20261109/`
(jar NOT included, per house rule). The 0.5.1 records package
is superseded-uninstalled: kept, marked, never published as a
shipped version.

## Explicitly NOT in this packet

- Raid voice PLAYBACK (the Narrator's 6 clips ride the packs
  inert; playback is a later GuildGate packet gated on the
  GuildGate 0.4.0 source deposit).
- Packet A's own config merges (its packet, its window —
  lands FIRST).
- V-1 live verification (separate parent dispatch,
  post-install).
- Any pack rebuild (the packs are frozen at the hashes above).
