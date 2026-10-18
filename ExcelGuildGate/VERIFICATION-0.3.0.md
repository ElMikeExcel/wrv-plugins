# ExcelGuildGate 0.3.0 — verification record

Built and verified Sat Oct 17, 2026 (local scratch only; NOT installed,
live untouched, the armed 0.2.0 install watch untouched). Scratch stack:
Paper 26.2-123 (the live build) + PlaceholderAPI 2.12.3, offline mode,
flat world, JDK 25 — the same harness as the 0.2.0 verification.

## Provenance and change scope

- 0.3.0 was built from the shipped 0.2.0 tree (whose own base was
  proven byte-identical to the live 0.1.0 jar in the 0.2.0 packet).
- Change-scope check (`diff -rq` of the 0.2.0 and 0.3.0 source trees):
  only `ExcelGuildGate.java`, `config.yml`, `plugin.yml`, `build.sh`,
  `run-tests.sh` differ. `SealState.java`, `CatchUpTracker.java`,
  `GateForms.java`, `GuildGateExpansion.java`, `GuildGateAPI.java` and
  both pre-existing test harnesses are byte-identical. New files:
  `NarrationGuide.java`, `NarrationGuideTest.java`.

## Automated tests — 246/246

`src/ExcelGuildGate/run-tests.sh`:

- SealStateTest 95/95 (untouched),
- CatchUpTrackerTest 56/56 (untouched),
- **NarrationGuideTest 80/80 (new)**: default sound + guide per stage
  (each guide ≤5 lines, names its beat, stage 1 carries the "you'll
  need a wand" line, every guide ends with the Skill Trees pointer);
  out-of-range stages silent; master toggle silences moment, guide,
  and replay; replay picks the HIGHEST missed beat (order-independent,
  null-tolerant); replay toggle off silences only the replay; sound
  override replaces, blank silences the voice but not the guide,
  null/short tables fall back per stage; guide override wins wholesale
  (an empty list means intentionally no guide); blank/null guide lines
  dropped; negative line delay clamps to 0. The plan is stateless —
  replay once-ness is CatchUpTracker's seen-stage advance, tested
  there.
- GuildGateAPITest 15/15 (unchanged).

Jar reproduction: two clean builds → identical SHA-256
`3c87c3ffbbc33163163ce054a744fad51c1c3bb47c0e0dc0302e3a90cf495699`
(38,447 bytes).

Resource-pack reproduction: `record-narration.sh --pack-only` run twice
→ identical SHA-1 `e1ef3b835d59280e613ae435f6efca760389045e`
(377,104 bytes). Pack format [88, 0] read from the 26.2 jar's own
`version.json` (`pack_version.resource_major: 88, resource_minor: 0`),
not from documentation. All five .ogg verified as Vorbis, mono, with
durations matching the recorded MP3s. The seal 1 recording is
byte-identical to the parent's audition MP3 (SHA-256 `022f0a89…`).

## Scratch boots (logs in scratch/)

1. **Boot A — 0.3.0 on genuine 0.1.0 data** (`bootA-0.3.0-on-0.1.0data.log`):
   state file produced by the 0.1.0 jar itself (grant→revoke round
   trip, no `seen:` section) + the 0.1.0 config (no `catchup:`, no
   `narration:`). Result: `Enabling ExcelGuildGate v0.3.0`; "enabled:
   0 of 5 seals broken, guild LOCKED" (**campaign state preserved on
   0.1.0 data**); "Join catch-up: on, 0 players tracked."; "Narration:
   on, catch-up replay on, guide delay 2s". Console exercise:
   `catchup ScratchSteve` answered correctly; **`narrate 1` dispatched
   `wrv:guildgate.seal1` (logged, 0 online targets) and sent guide
   lines 1–5 at 2-second intervals**; `narrate 9` → "Stage must be
   1-5."; final status 0 of 5 LOCKED.
2. **Boot B — 0.3.0 on 0.2.0 data** (`bootB-0.3.0-on-0.2.0data.log`):
   0.2.0-shaped state.yml (with a `seen:` record) + the shipped 0.3.0
   config. Result: campaign preserved; "Join catch-up: on, 1 players
   tracked." (**the 0.2.0 seen record survived the upgrade**);
   `catchup ScratchSteve` → "told through stage 0 of 0. Fully caught
   up."; `narrate 5` dispatched `wrv:guildgate.seal5` + its 5 guide
   lines.
3. **Boot C — narration off** (`bootC-narration-off.log`): same data,
   `narration.enabled: false`. Enable logs "Narration: off";
   `narrate 1` replies "Narration is off (narration.enabled is false);
   nothing to preview." — no dispatch, no error, no state change.

The only ERROR lines in any boot are the sandbox VM's broken outbound
TLS hitting Mojang / PlaceholderAPI / Paper version checks —
environmental, identical to the 0.2.0 verification, unrelated to
GuildGate. Scratch server left stopped.

A note on what the console boots prove about the fall path itself: the
celebrated path (`creditSeal`, fired by boss kills and raid victories)
calls the same `runNarrationMoment` the `narrate` preview exercises —
admin grant/revoke deliberately stay quiet, per the standing 0.1.0
design, so no console command can fake a celebrated fall; the hook is
one method call inside the existing celebrate block (see the
change-scope diff).

## Bedrock / Geyser finding (live build, bytecode + official docs)

Live Geyser: **Geyser-Spigot 2.11.3-SNAPSHOT** (the jar in the Oct 15
live backup). Three independent checks agree:

1. The jar's `JavaClientboundResourcePackPushPacket` translator
   (decompiled with `javap -c`) **never downloads, converts, or
   forwards** a Java server resource pack. If the pack is required it
   answers the Java server ACCEPTED → DOWNLOADED → SUCCESSFULLY_LOADED
   without the client receiving anything; if not required it answers
   DECLINED. The pack bytes never reach a Bedrock client.
2. The jar's sound path (`JavaSoundTranslator` →
   `SoundUtils.playSound`) passes an unknown custom sound through by
   raw name (only the `minecraft:` prefix is stripped) as a Bedrock
   PlaySoundPacket named `wrv:guildgate.seal<N>`. A Bedrock client
   plays a named sound only when a pack it has loaded defines it —
   and per (1) no such pack arrives. Net effect: **silence, no
   error**.
3. GeyserMC's official docs ("Using Resource Packs with Geyser"):
   "Geyser does not convert resource packs from the Java Edition
   format into the Bedrock edition format"; the FAQ repeats "There is
   currently no automatic Java-to-Bedrock resource pack conversion."
   Geyser serves Bedrock-edition packs from its own `packs` folder —
   the one future route to Bedrock narration (a hand-built .mcpack
   defining the same sound names, dropped in
   `plugins/Geyser-Spigot/packs/`). That route is unbuilt and
   unverified, and whether Bedrock sound definitions accept a
   Java-style namespaced event name is an open question within it.

Attempting an actual conversion in scratch was therefore moot: the
live Geyser contains no pack converter at all (no
`org.geysermc.pack.converter` classes in the jar), so there was no
machinery to attempt it with. The design fallback stands: Bedrock
players get the seal broadcasts, the full guide text, and the join
catch-up — everything but the voice.

## What only real players can prove

- Actually **hearing** the narrator on a Java client with the pack
  accepted (volume, the MASTER channel, whether the 2-second guide
  stagger reads well against a ~9-second voice line).
- The pack prompt flow on Java (accept/decline, rejoin behavior) —
  and that declining never locks anyone out (guaranteed by
  `require-resource-pack=false`, but the click-through is a client
  experience).
- A genuine in-play seal fall end to end: kill → broadcasts → voice
  → guide → seen-advance → silent rejoin, on the live server.
- Bedrock: confirming the predicted silence (and that the guide text
  + catch-up arrive intact) on a phone/tablet through Geyser.
