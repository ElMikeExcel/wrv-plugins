# ExcelGuildGate 0.3.0 — changelog (narration)

Built Sat Oct 17, 2026. Base: 0.2.0 (join catch-up), unchanged except
where listed. **Not installed.**

## Added

- **The narrator.** On the celebrated seal-fall path (the same hook
  where 0.2.0 advances seen-stage), the stage's recorded voice line
  now plays for everyone online (custom sounds
  `wrv:guildgate.seal1..seal5` from the new server resource pack),
  followed by a short staggered **guide sequence** for the stage that
  just opened: what opened, the practical need (a wand, a rank, the
  builder's handout), and the pointer to the website's Skill Trees
  page. At most 5 lines per stage, 2 seconds apart by default.
- **Catch-up replay.** The 0.2.0 join catch-up additionally plays the
  narrator line for the returner's **highest missed stage**, to that
  player only, once, alongside the existing combined text message.
  Toggle: `narration.catchupReplay` (default on).
- **`/guildgate narrate <1-5>`** (admin): previews a stage's voice
  line + guide for everyone online without touching campaign state —
  the audition and wording-check tool. The real moment fires only on
  genuine falls.
- **Config**: new optional `narration:` section (`enabled`,
  `catchupReplay`, `guideLineDelaySeconds`, per-stage `guides` with
  optional per-stage `sound` overrides; a blank sound silences that
  stage's voice). A config without the section (e.g. the live 0.1.0
  config) runs the full feature on built-in defaults.
- **Resource pack** `wrv-guildgate-narrator-pack.zip` (377,104 bytes,
  SHA-1 `e1ef3b835d59280e613ae435f6efca760389045e`): five TTS narrator
  lines (voice avocado_v2:magnus) as mono Vorbis .ogg, pack format
  88.0 for Minecraft 26.2. `record-narration.sh` re-records all five
  lines (or swaps the voice) and rebuilds the pack + SHA-1 in one
  command.

## Unchanged

- Everything from 0.1.0/0.2.0: the gate, the seals, the broadcasts,
  the quest page/forms, PlaceholderAPI placeholders, and the join
  catch-up rules. `SealState`, `CatchUpTracker`, `GateForms`,
  `GuildGateAPI` are byte-identical files; state.yml gains nothing
  new (narration keeps no per-player state — replay once-ness rides
  the catch-up's seen-stage advance).

## Safety

- Narration is garnish, never load-bearing: the whole moment and every
  per-player dispatch are exception-guarded, so a missing pack or an
  unknown sound can never break a seal fall or a join. Players who
  decline or never load the pack get the full text (broadcasts + guide
  lines) exactly as before.

## Known limitation (verified, not papered over)

- **Bedrock players hear no narrator audio through this pack.** The
  live Geyser build (2.11.3-SNAPSHOT) never delivers a Java server
  resource pack to Bedrock clients — bytecode-verified in the live
  jar and confirmed by GeyserMC's own docs. Geyser forwards the
  playsound by name, but with no Bedrock-side definition the client
  stays silent. Bedrock players still get the seal broadcasts, the
  guide text, and the join catch-up. See README.md for the full
  finding and the one future route (a Bedrock-format pack in Geyser's
  own packs folder — unbuilt, unverified).
