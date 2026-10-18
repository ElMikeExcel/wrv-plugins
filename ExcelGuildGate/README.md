# ExcelGuildGate 0.3.0 — narration

Built Sat Oct 17, 2026 for Wink Reyes Valley (Paper 26.2). Mike's ask:
"for guild gate can we add a narrator? Like text to speech even is
fine. Basically the voice line gets played, then a small guide onto
what was released. Like you'll need a wand etc etc." **Not installed** —
this packet is built, tested, scratch-verified, and deposited; the
install packet (INSTALL-PACKET.md) covers the jar swap, the pack
hosting step, and the server.properties settings, and it installs over
live 0.1.0 directly or over 0.2.0, whichever is live by then.

## Provenance (base proven before any change)

- 0.3.0 is built on the shipped **0.2.0** tree (join catch-up), whose
  base was rebuilt from the 0.1.0 source and reproduced the live jar
  byte-identically (SHA-256 `7ada5c62…`, see the 0.2.0 packet).
- Change scope vs 0.2.0 is exactly: `ExcelGuildGate.java` (the
  narration wiring), `config.yml` (a `narration:` section),
  `plugin.yml` (0.3.0), `build.sh`/`run-tests.sh`, plus the new
  `NarrationGuide.java` and its test. `SealState`, `CatchUpTracker`,
  `GateForms`, `GuildGateAPI` are byte-identical files.

## What 0.3.0 adds (the whole change)

A narrator for the seal falls, in three parts:

1. **The moment.** When a seal falls in play (the celebrated path —
   boss kill or raid victory, the same place 0.2.0 advances
   seen-stage), the stage's **recorded voice line** plays for
   everyone online, then a short **guide sequence** runs: staggered
   chat lines, at most 5 per stage, 2 seconds apart by default, saying
   what opened, the practical need, and where to read the full ladder:
   - **Seal 1, The Healer** — healing magic is back; "**you'll need a
     wand**"; spells are learned with SP, ranks Beginner to Master;
     the Skill Trees pointer.
   - **Seal 2, The Pathfinder** — light, sight, getting-around arts;
     Blink waits at Master.
   - **Seal 3, The Wonder** — Lightning and Chain Lightning at Master.
   - **Seal 4, The Masons** — building magic, honestly flagged as the
     **builder's handout** (masons wand + builder group), not a ladder
     rank yet; it joins Master at Seal 5.
   - **Seal 5, The Guild Opens** — /ag is open, the last schools
     return, the Masons take their Master-rank place.
   Guide wording mirrors the Magic packets' LADDER.md placement
   decisions and is **DRAFT for Mike** — most importantly the
   wand-acquisition line stays deliberately generic, because how a
   first wand is earned is not decided yet. All wording is editable in
   `config.yml` (`narration.guides`) with no rebuild.
2. **The voice.** Five TTS lines (voice `avocado_v2:magnus`, scripts
   in NARRATION-LINES.md; Seal 1's script is the fixed, auditioned
   one; its shipped take is byte-identical to the audition) ship in a
   Java resource pack as custom sounds `wrv:guildgate.seal1..seal5`.
   Editing a line or changing the voice and re-running
   `record-narration.sh` re-records everything and rebuilds the pack
   + its SHA-1 in one command. **Voice approval is Mike's, pending.**
3. **The catch-up replay.** A player who was away gets the 0.2.0 text
   catch-up as before, and additionally hears the narrator line for
   their **highest missed stage**, privately, once (the once-ness is
   the catch-up's own seen-stage advance — narration keeps no state).
   Toggle: `narration.catchupReplay` (default on). Master toggle:
   `narration.enabled` (default on). Admin preview:
   `/guildgate narrate <1-5>` fires a stage's moment for everyone
   online without touching the campaign.

Admin `grant`/`open` stay quiet, exactly as before: no narration, no
marks. Narration is exception-guarded end to end — a missing pack or
an unknown sound can never break a seal fall or a join; the string
playsound is a client-side lookup and players without the pack simply
hear nothing while still getting every text line.

## The Bedrock finding (read this before promising anyone audio)

**Bedrock players (Geyser) will NOT hear the narrator from this
pack.** Verified three ways against the live Geyser build
(2.11.3-SNAPSHOT, from the Oct 15 live backup): the jar's resource
pack push translator never downloads, converts, or forwards a Java
server pack to Bedrock clients (it fake-accepts a required pack so
nobody is locked out, and declines an optional one); the sound
translator does forward our playsound by name, but a Bedrock client
plays a named sound only if a loaded Bedrock pack defines it, and no
pack arrives; GeyserMC's docs state plainly that Geyser does not
convert Java packs to Bedrock format. Full evidence in
VERIFICATION-0.3.0.md.

So Bedrock players get everything except the voice: the seal
broadcasts, the full guide text, and the join catch-up. The one
future route is a hand-built Bedrock-format pack defining the same
sound names, placed in Geyser's own `packs` folder — unbuilt,
unverified, and with an open question about namespaced sound names
on Bedrock. It is not part of this packet and nobody should be told
it works until it is proven on a real device.

## Data compatibility (mandatory, verified)

- state.yml is untouched by 0.3.0: no new keys, no migration. The
  narration layer keeps no per-player state.
- 0.3.0 boots cleanly on the genuine 0.1.0 state file + 0.1.0 config
  (campaign preserved at 0 of 5 LOCKED, catch-up and narration on
  built-in defaults) and on 0.2.0 data (the `seen:` record survives).
  Both proven in scratch — see VERIFICATION-0.3.0.md.
- Rollback is a plain jar swap: 0.1.0 and 0.2.0 both read the same
  state.yml and config; the resource pack may stay hosted or be
  unlinked in server.properties (see INSTALL-PACKET.md).

## Tests

`src/ExcelGuildGate/run-tests.sh` — **246 checks, all passing**:
SealStateTest 95/95 and CatchUpTrackerTest 56/56 (both untouched),
GuildGateAPITest 15/15, and the new NarrationGuideTest 80/80 (guide
selection per stage, highest-beat replay choice, toggle semantics,
override/fallback rules, garbage tolerance). The jar and the resource
pack both reproduce byte-identically on rebuild (SHA256SUMS.txt,
PACK-SHA1.txt).

## Dependencies (disclosure)

- Required: none beyond Paper. GuildGate works with every softdepend
  absent.
- Optional (unchanged softdepends): PlaceholderAPI (placeholders),
  Geyser-Spigot/floodgate (Bedrock forms + player detection),
  EliteMobs (the guild itself; the gate never touches it).
- The narrator voice additionally needs the **server resource pack**
  (this packet's zip, hosted per INSTALL-PACKET.md) and a Java client
  that accepts it. Without the pack, the plugin is fully functional —
  text only, which is the pre-0.3.0 experience plus the guide lines.

## What only real players can prove

- Hearing the narrator on a Java client (volume, timing against the
  guide stagger), the pack prompt flow, and a genuine in-play seal
  fall end to end.
- Bedrock: confirming the predicted audio silence (and intact text)
  on a real phone/tablet through Geyser.

## Files in this packet

- `ExcelGuildGate-0.3.0.jar` — the build (38,447 bytes, SHA-256
  `3c87c3ffbbc33163163ce054a744fad51c1c3bb47c0e0dc0302e3a90cf495699`).
- `wrv-guildgate-narrator-pack.zip` — the resource pack (377,104
  bytes, SHA-1 `e1ef3b835d59280e613ae435f6efca760389045e`), with
  `PACK-SHA1.txt`, `pack-src/` (its full source), `narration/mp3/`
  (the recorded takes), `NARRATION-LINES.md` (the scripts), and
  `record-narration.sh` (one-command re-record/rebuild).
- `src/ExcelGuildGate/` — full source tree (with `build.sh`,
  `run-tests.sh`).
- `SHA256SUMS.txt` — jar + source zip + pack hashes.
- `INSTALL-PACKET.md` — install/rollback/publishing steps, including
  the web hosting step and the exact server.properties values.
- `CHANGELOG-0.3.0.md`, `VERIFICATION-0.3.0.md`.
- `scratch/` — the scratch-server verification logs and the genuine
  0.1.0/0.2.0 state shapes used.
