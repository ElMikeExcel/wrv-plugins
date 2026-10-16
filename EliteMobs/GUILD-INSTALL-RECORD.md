# EliteMobs — Adventurer's Guild Hub: install record (Phase 2)

**Plugin:** EliteMobs 10.9.8 (third-party, by MagmaGuy / Nightbreak) —
**configured by us**, not created or modified by us. This record covers a
content installation and the first-time setup of the plugin on Wink Reyes
Valley. It is the Phase 2 companion to the Phase 1 configuration record
(local record: `~/workspace/wrv-006/staged/`; website changelog:
winkreyesvalley PR #18, merged Oct 15, 2026).

**Status: INSTALLED LIVE — Oct 16, 2026, ~12:12 AM CDT.**
Statuses stay separate, per house rule:

- Installed live: **yes** (Oct 16, 2026)
- Bedrock/mobile player check: **passed** (owner, in game, Oct 16, 2026)
- Java player check: **pending**
- Claim-safety test: **pending**
- Mossback (first roaming boss) spot check: **pending**

## What was installed

- The official free **Adventurer's Guild Hub** content package
  (`em-free-content-the-adventurers-guild.zip`, placed in
  `plugins/EliteMobs/imports` on Oct 15, 2026, inert until this window).
- The guild is its **own separate world**, `em_adventurers_guild`. The
  family world, player data, builds and balances were not modified.
- First-time plugin setup: `/em initialize` was run for the first time on
  this server, with the **Recommended** preset (natural elites on, skills
  on, loot on, action + timed events on). That preset matches the settings
  the server was already playing with, so initialize changed no gameplay
  behavior; it marks setup done.

## How it was done (Phase 2, in game, Oct 16, 2026)

Precondition (Mike's rule): the verified pre-guild full-server backup on
Google Drive (completed Oct 15, 2026). Phase 1 configuration was already
live from Oct 15 (`onlyUseBedrockMenus: true`,
`enableHighCompatibilityMode: true`, `doRegenerateContainers: false`, with
`.bak-20261015` rollback copies on the server).

1. **Temporary operator window.** The owner plays deopped by design
   (ops.json empty). A console `op` was granted to the owner
   (`.MikeExcel`, level 4) at ~12:06 AM CDT and verified by reading
   ops.json. Setup commands were run; `deop` followed at ~12:18 AM CDT and
   ops.json was read back empty (`[]`). No restart was involved.
2. **`/em initialize` → Recommended** (~12:08 AM CDT). The server log shows
   the plugin's shutdown/reload sequence ending in "EliteMobs fully
   initialized!", with **zero errors**. The only WARN was the known
   Vault-preference note for this server's chosen Vault economy mode.
   "No account token found" also appears and is expected: the manual
   import route (Route A) needs **no Nightbreak account or token**.
3. **The initialize reload processed the imports zip** and installed the
   guild hub on its own. No `/em setup` install step was needed. (During
   the `/em setup` menu visit, a stray tap by the owner in the chest-style
   menu installed and downloaded **nothing** — verified against the server
   log, which shows no install or download lines.)
4. **`/ag` travel test** by the owner right after.

## Verification evidence (Bedrock, owner in game, Oct 16, 2026)

- `/ag` landed the owner at **208, 88, 236** in `em_adventurers_guild` —
  the package catalog's documented landing point (208.5, 88, 236.5).
- NPCs present and interactable, including **Hermes** (transporter) and
  **Casus** (guide).
- **Transporter round trip works:** Hermes returned the owner to the exact
  location he had left. Note the known behavior: EliteMobs teleports
  channel over a few seconds and **cancel on any movement**
  ("Teleport interrupted!"). Two attempts were interrupted by movement;
  the third, standing still, completed. This is plugin behavior, not an
  install defect.
- **Guild shop opens and responds on Bedrock:** the weapon shop's
  "Select Weapon Type" screen rendered its categories on the owner's
  phone. The red crossed-out slots are categories locked behind guild
  ranks — normal for a brand-new guild member, not a fault.
- Server log for the whole window: initialize and reload lines clean, no
  SEVERE/ERROR entries, EliteMobs reporting version 10.9.8 as the latest.

## Still pending (recorded honestly, not claimed)

- **Java player check** of the guild (`/ag`, NPCs, transporter, a shop) —
  not yet done by any Java player.
- **Claim-safety test** — confirming EliteMobs content respects
  GriefPrevention claims on this stack.
- **Mossback spot check** — the staged roaming-boss encounter test.
- Player-experience spot checks from the runbook §5 that need ordinary
  family play (elite conversion rate near spawners, non-admin permissions
  in the hub).

## Rollback

Three levels, mildest first, in
`~/workspace/wrv-006/GUILD-INSTALL-RUNBOOK.md` §6 (local record):

1. Disable/uninstall the guild package from `/em setup` (plus the
   command-rerouting fix that becomes necessary again without a hub).
2. Full file removal of the `em_adventurers_guild` world folder and the
   package's installed files, restoring
   `plugins/EliteMobs/config.yml.bak-20261015`.
3. Disaster only: the verified pre-guild Drive backup (costs all progress
   made after it; owner call only).

## Secrecy check

No credentials, tokens, server addresses, player UUIDs, or private player
data appear in this record. The Nightbreak token file was never created:
no token exists on this server.
