# SOURCE — ExcelBedrockMenu 1.5.29 (teleport direction choice)

Built Nov 2, 2026 (Iris, local work only — no live-server contact, nothing
installed, nothing published).

## Why

Jester asked (Nov 2, 2026) for a `/tpahere` command — "sends a tpa request
but requests the person to teleport to you". The investigation found
Essentials 2.22 already ships exactly that command; the only missing piece
was the LuckPerms node, granted to all players the same day
(`essentials.tpahere`, read back true). Mike then asked for the Bedrock
menu to offer it the way it offers teleports: tap a player in the picker,
then choose **"Teleport to player"** or **"Ask player to come to me"**
(his exact wording, picked on the options card Nov 2, 2026). Per the
platform-parity rule, the Java side carries the same choice step in the
same build.

## Base + base gate

Base tree: `~/workspace/excelbedrockmenu-1.5.28/` (the live 1.5.28 source
of record). Per the delta-build law, the unmodified base was rebuilt
BEFORE any edit and proven byte-identical to the shipped 1.5.28 jar:

- Rebuild and shipped jar both: SHA-256
  `b9a8abd44fbb82b4a05923e362df6d8e0e9cc8b80a9fdf1d6dcbd47808cb1dc9`, 41,839 B.
- Toolchain: Temurin 25.0.4.1 (`~/workspace/.jdk25`), Paper libraries
  `~/workspace/paper231-rerun/bootstrap/libraries`, the tree's own
  `build-offline.sh`. Baseline AdapterLogicTest on the base: 185 checks,
  PASS (matches 1.5.28's SOURCE.md).

## The delta

All functional changes are in
`src/com/winkreyes/excelbedrockmenu/TeleportMenu.java`, plus the
`plugin.yml` version stamp (1.5.28 → 1.5.29), the build script's jar
name/date, and test-only additions to `AdapterLogicTest.java`.

1. **A `Direction` enum + two pure helpers.** `requestCommand(direction,
   name)` returns `"tpa <name>"` or `"tpahere <name>"`;
   `promptText(direction, who)` returns `"<who> wants to teleport to
   you."` or `"<who> is asking you to teleport to them."`. The settled
   labels are constants: `LABEL_TELEPORT_TO = "Teleport to player"`,
   `LABEL_COME_TO_ME = "Ask player to come to me"`,
   `CHOICE_BODY = "What do you want to do?"`.
2. **Bedrock choice step.** The picker form's tap no longer dispatches;
   it opens a two-button modal (the existing `GeyserForms.sendModalForm`)
   titled with the picked player's (stripped) display name, body
   `CHOICE_BODY`, buttons in the settled order. Button 1 →
   `TELEPORT_TO`, button 2 → `COME_TO_ME`. If the modal can't be sent,
   it fails soft to the pre-1.5.29 behavior (a plain `/tpa`). A closed
   form fires no callback, so closing sends nothing.
3. **Java choice step.** Clicking a head now opens a 9-slot choice chest
   (new `ChoiceHolder`, following the `PickHolder` pattern) titled with
   the picked player's name: slot 2 = Ender Pearl "Teleport to player",
   slot 6 = Ender Eye "Ask player to come to me", each with a one-line
   lore saying who travels and that they can accept or decline. Clicks
   are cancelled (items can't be taken), drags are cancelled for both
   holders, and closing the chest sends nothing. If the picked player
   logged off between the list and the click, it routes through
   `sendRequest`, which owns the existing "just left the game" message.
4. **Generalized dispatch.** `sendRequest` takes a `Direction` and runs
   `from.performCommand(requestCommand(direction, to.getName()))` — still
   AS the picker, real username, so Essentials' cooldowns, safe-teleport
   checks and timeouts apply unchanged, and a rejected command still
   skips the answer prompt.
5. **Direction-aware answer prompt.** `askTarget` uses `promptText` for
   both the Bedrock modal (title "Teleport request", buttons
   Accept/Decline — unchanged) and the Java clickable chat. The Java
   [Accept] hover now mirrors the direction ("Let them come to you" for
   TELEPORT_TO, "Teleport to them" for COME_TO_ME); [Decline] and its
   hover are unchanged. `/tpaccept` + `/tpdeny` answer both directions
   in Essentials, so `answer()` is untouched.

Everything else is preserved exactly: self excluded from the picker,
vanished players hidden, the nobody-online message, the
Essentials-missing message, the logged-off messages, and the 2-tick delay
before the answer prompt.

## Judgment calls (flagged, per the build brief)

- **Strings stay hardcoded — no new config keys.** The adjustability law
  says new retunable strings go in config.yml, but `TeleportMenu` has NO
  config pattern: every string in the class is hardcoded and the bundled
  `config.yml` carries only the Spawn fallback coordinates. Inventing a
  config section for one class would diverge from the file's own pattern
  and would change the bundled config (breaking the jar-entry scope).
  Instead the settled labels live in named constants pinned byte-exact
  by tests — a retune is a one-line change in one obvious place.
- **Java [Accept] hover text** for the new direction ("Teleport to
  them") is new wording written to mirror the existing hover; the brief
  pinned the prompt sentence, and the hover follows from it. Flagged
  here so it's a known, reversible choice.
- **Choice-chest materials** (Ender Pearl / Ender Eye) and slots (2 / 6)
  are presentation choices, not balance values; either is a one-line
  change if Mike dislikes them in game.

## Candidate of record

| Artifact | Size | SHA-256 |
|---|---|---|
| `ExcelBedrockMenu-1.5.29.jar` | 44,735 B | `15c3ed773c2867c90a5ecd36cdf9aa6587e4d1fd1de800edecb46ec2e6366ae0` |
| `ExcelBedrockMenu-1.5.29-source.zip` | 35,989 B | `e28fc51f979f9cf18dad783a9dbbad01dbc04dced2e974298fb1eca414b5d7c1` |

- **Deterministic build**: built three times from the filed tree →
  byte-identical every time. Build: `build-offline.sh` in this folder
  (JDK `~/workspace/.jdk25`, Paper libraries as above, jar date pinned
  2026-11-02T00:00:00Z).
- **Change scope (jar-entry diff vs the base-proven 1.5.28 jar)**:
  `TeleportMenu.class` changed, `plugin.yml` changed (version line
  only), two new entries (`TeleportMenu$Direction.class`,
  `TeleportMenu$ChoiceHolder.class`); `TeleportMenu$PickHolder.class`
  differs only in InnerClasses metadata (disassembly identical). Every
  other entry, including the bundled `config.yml`, is byte-identical.
- **Tests**: AdapterLogicTest 193 checks PASS (185 carried + 8 new);
  Paper 26.3-235 rig boot smoke PASS, SEVERE 0. Full detail:
  `TEST-REPORT.md`.

— Excel (Iris), for Wink Reyes Valley
