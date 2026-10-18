# ExcelBedrockMenu 1.5.8 — verification record

Built Sat Oct 17, 2026 (Argus). Status: BUILT + TESTED + SCRATCH-BOOTED.
NOT installed live. Bedrock tap check still requires a real device (below).

## Provenance (which source is the live 1.5.7 source)

- Live jar, per `~/workspace/wrv-records/VERSIONS-AND-ROLLBACK.md`:
  ExcelBedrockMenu 1.5.7, SHA-256 `0787c161…`, record = wrv-plugins PR #33
  (draft, branch `record/excelbedrockmenu-1.5.6-2026-10-16`).
- The source zip deposited Oct 17 (`~/workspace/drive-deposits-2026-10-17/
  ExcelBedrockMenu-1.5.7-source.zip`, pulled from the PR #33 branch) was
  extracted to `provenance/` and rebuilt with `src/build-157-local.sh`
  (Grok's `build-157.sh` with VM-local paths; Paper API build 123 in place
  of Grok's 133 — bytecode output unaffected).
- **The rebuild is byte-identical to the live jar:**
  SHA-256 `0787c1616d576795b1b50ef7be42bd0f5de0658450b7c859ec1f93f4e56fb7c7`.
  That is the source 1.5.8 was built from. No guessing involved.

## The change

Every menu action that immediately teleports the player now asks first,
via the Cumulus ModalForm helper (`GeyserForms.sendModalForm`) the plugin
already uses for teleport requests:

- Form: title `Teleport to <destination>?`, body line per destination,
  buttons `Yes, teleport` / `Cancel`. Closing the form without choosing
  never calls back (the helper only reports valid results) = cancel.
- Yes runs the exact 1.5.7 action, exactly once:
  - Spawn (main menu): the console-dispatch + village-fallback
    `teleportToSpawn` path, unchanged.
  - Adventurers Guild: `performCommand("ag")`, unchanged — and only
    while the guild is unlocked. While sealed, /ag opens the seals
    quest page (not a teleport), so no confirm is shown.
  - Catalogue `/spawn` and `/home`: the player's own command, unchanged.
- If the form cannot be sent at all, the button falls back to the
  pre-1.5.8 immediate action so it never dead-ends.

Covered buttons (confirm added):
- Main menu: **Spawn**; **Adventurers Guild** (unlocked state only).
- /servercommands catalogue entries: **/spawn**, **/home**, **/ag**
  (unlocked state only).

Teleport-capable, deliberately NOT covered:
- **Teleport to a player** (/tpmenu): opens a picker; the only teleport
  that can follow completes through the *target's* existing
  Accept/Decline prompt. The tap itself teleports nobody.
- **/tpaccept** (catalogue): answers a request another player started;
  that flow is the consent step, and the teleport usually moves the
  requester, not the tapper.
- **/trapped** (catalogue): GriefPrevention honors it only when the
  player is genuinely trapped in a claim — a stray tap anywhere else is
  a no-op — and it is an emergency escape.

Not teleports, untouched: Party, Excel Shop, Jobs & Money, mcMMO Stats,
Leaderboard, Server Info, Server Commands, Server Website,
Auto-open on join, and every other catalogue entry.

Java side: out of scope for this plugin (it serves Bedrock forms; Java
players get the DeluxeMenus chest menu). Mike's confirm standard
applies there via the separate Java menu config packet.

## Tests

`src/build-158.sh` compiles and runs `MenuLogicTest`: **38 checks, ALL
PASS** (15 pre-existing 1.5.7 checks + 23 new). The new checks pin the
routing decision (`teleportConfirmTitle` / `requiresConfirm` /
`confirmChoiceTeleports`): spawn always confirms; "ag" confirms only
while unlocked; "home" confirms; every non-teleport menu button and
catalogue entry (including tpaccept and trapped) never confirms; Yes
teleports, Cancel does nothing, closed form (null) does nothing; all
confirm texts plain ASCII; titles/labels exact; plugin.yml is 1.5.8.

## Rebuild reproduction

Two consecutive `build-158.sh` runs produce the identical jar:
SHA-256 `52766c5916b50dda597ad3782f414ad3950c15656018699ec177e7e0d3db255c`,
22,486 bytes.

## Change scope vs the live 1.5.7 jar

Entry sets identical (6 entries). Byte-changed entries only:
`plugin.yml` (version) and `ExcelBedrockMenu.class`. `GeyserForms.class`,
`TeleportMenu.class`, `TeleportMenu$PickHolder.class` and `config.yml`
are byte-identical to 1.5.7.

## Scratch boot

Fresh minimal Paper 26.2 (build 123 server set + cached Mojang jar from
`~/workspace/magic-scratch/stage-paper`), only ExcelBedrockMenu-1.5.8.jar
in plugins/, port 25601, offline mode. Log: `scratch-server/boot.log`.

- `[ExcelBedrockMenu] Loading server plugin ExcelBedrockMenu v1.5.8`
- `[ExcelBedrockMenu] Enabling ExcelBedrockMenu v1.5.8`
- `[ExcelBedrockMenu] ExcelBedrockMenu enabled: /menu uses native Bedrock dialogs.`
- Console `version ExcelBedrockMenu` → `ExcelBedrockMenu version 1.5.8`
- Console probes reach the executors: `Only players can use /menu.` /
  `/servercommands.` / `/serverwebsite.`; `tpmenu` → `Only players can use this.`
- No SEVERE and no warning/exception naming the plugin. (The log's SSL
  errors are the sandbox proxy blocking Mojang endpoints; offline mode,
  root-user and version-fetch warnings are environmental.)

## What only a real Bedrock tap can verify

- The confirm form renders on a Bedrock client (Floodgate/Geyser) with
  the right title/body/buttons.
- Cancel and form-close leave the player standing where they were.
- Yes teleports exactly once, for Spawn and for the Adventurers Guild
  button (guild currently sealed live, so the guild confirm can only be
  tap-tested after the first seal falls, or pre-checked on a scratch
  with GuildGate unlocked).
- Menu behavior with ExcelGuildGate present (the scratch boot ran
  without it; `isGuildLocked()` fails safe to "not locked" when the
  plugin is absent, as in 1.5.7).
