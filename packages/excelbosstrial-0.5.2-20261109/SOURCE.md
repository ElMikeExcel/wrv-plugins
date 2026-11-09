# ExcelBossTrial 0.5.2 — SOURCE

Excel — Wink Reyes Valley. Authors: Iris / Mike.

- **What:** ExcelBossTrial is the server's governed boss-fight plugin
  (Wither / Warden / Dragon / Elder chapters: pool-space damage governing,
  verdicts, sprinkle adds, seal loot via GuildGate). Design of record for
  this build: the Item 35 reconciliation
  (`~/workspace/seal-fight-packet-b-20261109/RECONCILIATION.md`, sha256
  7068a663b7bd24082d53b49a01c8953b58da05f21cfe3248f3bf8aef708920bd), which
  rebases the boss-voice lane (`~/workspace/boss-voice-20261108/DESIGN.md`
  + `DECISIONS.md`, all four decisions ruled Nov 9, 2026, 1:00–1:21 AM)
  onto Packet B; backlog: Item 35 in
  `~/workspace/goals/family-minecraft-server-build/hidden_files/parent-backlog.md`.
- **Base:** the built, suite-green, rig-proven 0.5.1 tree
  (`~/workspace/seal-fight-packet-b-20261109/bosstrial/`), copied
  tree-identical into `~/workspace/bosstrial-052-build/bosstrial/` and
  proven before any edit by a pristine rebuild byte-identical to the
  0.5.1 jar (422,926 B / sha256 fff74246500fb13b7e1173ea29b24a7fefd350
  f4d0256562b93f3d1dc850a652) with its suite at 0 failures.
- **0.5.2 delta (voice, additive at every seam):** one new pure unit
  `core/BossVoice.java` (line→event resolution against the frozen pack
  contract `wrv:bosstrial.<boss>.<slug>` — the GuildGate narrator
  precedent, verified in that plugin's shipped source — index
  alignment, position-preserving slug sanitization, missing-clip
  fail-soft) + `core/VoiceSettings.java` (per-chapter seam slugs and
  the settled DESIGN §5 defaults: 32 wired clips) + `core/VoiceBook.java`
  (boom rate limit/alternation, open/last-door once guards,
  missing-clip log-once). Seams wired: Wither taunts by TauntBook pick
  index (chat unchanged as subtitle; `TauntBook.onEatPick` added,
  `onEat` delegates); milestone beats for elder/warden in the host and
  per-phase for the dragon in its chapter (phase-2 50% = beat50p2);
  dragon open at battle registration, flip inside the flip sequence
  adjacent to the re-light fence, chapter add waves at spawn; dragon
  sprinkle waves through a new fenced `SprinkleService` wave listener;
  elder relay inside `relayElderTrio` with its broadcast (plus the
  last-door line, staggered at progress 2 or spoken at the last
  elder's own opening); encounter open on first attributed damage;
  Warden sonic boom under the taunt cooldown; verdict at the death
  close for every chapter. Playback is positional AT THE BOSS
  (`Player.playSound(bossLocation, eventId, VOICE, 1.0, 1.0)`,
  participants only) — decision #2. The 6 raid clips are NOT wired:
  they ride the frozen packs inert until a later GuildGate packet
  (the narrator is the raid's speaker, decision #3; the Java narrator
  restore, decision #4, rides the merged pack in the install packet).
  Config: new keys only — `wither.taunt-sounds` (10, index-aligned),
  `dragon.voice`, `elder-guardian.voice`, `warden.voice`. No Packet B
  or Packet A key changes value.
- **Build:** JDK 25 (`~/workspace/.jdk25`), `build-test.sh` (suite:
  27,468 carried Packet B checks at 0 failures + 89 new voice checks
  = 27,557 by the Packet B tally method; 86 harnesses) then
  `package.sh` twice — deterministic, byte-identical: 438,791 B /
  sha256 1a866a54840dc510ad78cdbd384b75d49f82a25808a74724a0b4fc1e66f0944b. Build ID EXC-BOSSTRIAL-0.5.2-WRV-20261109-01-13447a (fresh
  tag; 0.5.1's a3aa16 never reused). Full record: `BUILD-RECORD.md` in
  `~/workspace/bosstrial-052-build/`; install packet: `PACKET-DRAFT.md`
  there. Ownership: Excel (Mike) — original work, no third-party code
  beyond the Paper/EliteMobs/Magic APIs in DEPENDENCIES.md.
