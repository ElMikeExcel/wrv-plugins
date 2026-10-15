# ExcelBuildMode: Supported-Actions Specification (Pilot)

Drafted October 14, 2026, from Moon's handoff (HANDOFF.md, REWARD-EXCLUSIONS.md, ACTION-BOUNDARY-REVIEW.md). Paper only. Nothing here is built or installed. This is the review document Mike signs off on before any implementation.

Decisions recorded October 14, 2026 (Mike): (1) WorldEdit is IN for pilot v1. (2) Clipboard copy/paste is IN: Mike trusts his builders, with an end-of-cycle audit expected (see Audit). (3) The two delayed-reward limitations are ACCEPTED with the clean-entry mitigation. (4) Marvin is the sole pilot builder for now; the role may expand later, only by Mike's decision.

## Purpose and scope

A construction-only pilot for one trusted builder, Marvin (GumGumJetPistol), so he can build the spawn hub in the existing world. Build Mode is Creative building with guardrails. Normal gameplay and all earning are paused while building. Nobody's Survival earnings change, in or out of Build Mode.

Hard limits for the whole pilot:

- No OP, ever. No wildcard permissions. No permanent WorldEdit group.
- Only Marvin is eligible, through a private allowlist. The shipped defaults are safe: empty allowlist. The role may expand to other builders later, only by Mike's decision.
- No new world, no WorldGuard, no hub region required. The hub location is still undecided and this spec does not need it.
- Blocks placed in Creative stay ordinary blocks that anyone can later mine. This is trusted building, not item-provenance enforcement.
- No changes to Jobs, mcMMO, or EliteMobs themselves, and no global config changes. All restrictions are session-only and come off at exit.

## Entering Build Mode (clean handoff)

Entry is refused unless all of these are true:

1. Marvin is on the private allowlist and has the `excelbuildmode.use` permission.
2. He is in Survival, standing on solid ground, not in a vehicle, cursor and crafting grid empty, inventories closed.
3. No active combat, no fishing cast out, no projectiles in flight, no pet engagements, no pending Elite party loot or votes unsettled.
4. He is not in an EliteMobs party. If he is, he leaves it himself while still in Survival, after his loot has settled. The plugin never forces a party leave.
5. No relevant Survival work is mid-completion under his name (cooking, brewing, smelting he started). Furnaces and brewers he already loaded keep running and finish normally; he just cannot start or feed new ones while building.

On entry: his Survival inventory, armor, offhand, ender chest, vanilla XP, selected slot, and flight settings are snapshotted. Build Mode starts from a cleared inventory and zeroed XP display. Session permissions attach: `jobs.use` denied for the session, temporary WorldEdit nodes only if WorldEdit is enabled for the pilot. Money, Jobs progress, mcMMO levels, and Elite progress are never part of the snapshot and are never rolled back.

## Supported actions (allowed while building)

- Place and break blocks in Creative, including decorative use of any block (for example, placing an already-stripped log instead of stripping one).
- Fly.
- Use the Creative inventory and his own crafting view to get building materials.
- Chat, help, list, and the Build / Play Normally toggle menu.
- WorldEdit, only in the restricted form below. WorldEdit is enabled for the pilot (Mike, Oct 14).

## Restricted: WorldEdit (narrow form only)

- Only the fixed temporary permission set (16 nodes): selection, fill, replace, walls, faces, clipboard copy/cut/paste and rotate/flip, self undo/redo.
- Cap of 5,000 changed blocks per operation and 10 history entries. No limit-override permission.
- No OP, no wildcards, no schematic or file loading, no scripts, no access to other players' history, no admin commands.
- Clipboard caution: copy/paste can carry container contents and entities with a structure. Clipboard is IN for the pilot (Mike, Oct 14: he trusts his builders). Builders are expected to paste responsibly; the end-of-cycle audit below is the check on that trust.
- All WorldEdit permissions are removed at exit, on disconnect, and after a restart. A changed-block cap is not a CPU limit; very large selections are still discouraged.

## Excluded actions (blocked at input while building)

Blocked as new inputs, at the earliest event point the stack allows:

- Combat of any kind: melee, and launching new projectiles (bow, trident, snowballs, etc.).
- Fishing: no new casts.
- EliteMobs class abilities and action-event starts.
- NPC interaction, both interaction events, including dialogue and quest NPCs.
- All containers and machine GUIs: chests, barrels, hoppers, furnaces, brewing stands, anvils, merchants, and shop inventories or menus (including the Spawn Shop).
- Item drops and item pickups.
- Right-click harvesting and collection: composter, berries, honey and honeycomb.
- Log, stem, and bamboo stripping by axe use.
- Starting campfire cooking, smelting, or brewing.
- Crafting, trading, repairing, and enchanting as earning inputs.
- Breeding and taming.
- Explosives.
- Every command except the allowlist: the Build Mode toggle, chat, help, list, and the WorldEdit subset when enabled. No /jobs, /em, homes, teleports, shop, or party commands.
- Party credit of any kind: while building, Marvin neither gives nor receives mcMMO party share, Together bonus credit, or EliteMobs party credit. This is enforced by the entry rules above, since party membership counting itself cannot be filtered by game mode.

## Money, XP, and delayed rewards (the honest rules)

- Already-earned Survival payouts are never cancelled for being in Build Mode. Buffered Jobs money that was earned in Survival settles normally during a build session.
- Two disclosed pilot limitations, both mitigated by the clean-entry rules:
  1. A plugin that delivers an item directly into his inventory mid-session (a delayed quest reward or shared-loot win, for example) can lose that item when the Survival inventory is restored at exit. Mitigation: entry requires outstanding deliveries to be settled first.
  2. A delayed Survival vanilla-XP delivery landing mid-session can be discarded by the XP snapshot/restore. Mitigation: entry requires no pending combat or XP activity.
- These limitations are stated here for Mike's explicit sign-off. They are not a design goal, and the pilot does not claim lossless transitions beyond them.

## Exiting Build Mode

- Exit restores the snapshot exactly: inventory, armor, offhand, ender chest, vanilla XP, selected slot, flight settings.
- Exit removes every session permission, including all temporary WorldEdit nodes.
- The same restore and cleanup must happen on death, disconnect and rejoin, and server restart mid-session. Recovery behavior reuses Moon's tested recovery core (217 corrected Paper checks).

## Acceptance tests (all must pass before live install)

On the exact current plugin stack, with a real Java client:

1. Ordinary block place and break in Build Mode produce zero Jobs money, points, XP, and quest progress, and zero mcMMO progression.
2. Forbidden collection, stripping, and campfire use are actually stopped, not just cancelled on screen.
3. Projectile launch and fishing cast are blocked; pre-entry Survival projectiles and payouts still resolve normally.
4. NPC interaction and shop GUI cannot be reached in either event order.
5. A legitimate queued Survival payout settles during Build Mode.
6. Nearby party behavior matches this spec for mcMMO share, Together, and EliteMobs credit.
7. Inventory restore is exact after normal exit, death, rejoin, and restart.
8. WorldEdit permissions are present only in session and gone afterward on every exit path.
9. Ordinary Survival play before and after a session is unchanged.

Any interaction that cannot pass its test gets explicitly excluded from Build Mode, not papered over.

## Audit (Mike, Oct 14: check at the end of build cycles)

Mike's expectation: after a builder's work cycle, review whether anything improper was done. What exists today:

- Server logs record commands, so WorldEdit operations issued in a session are visible in the console log with the player and time.
- The 5,000-block cap and 10-entry history bound how much a single operation can change.
- Placed blocks are ordinary world blocks, visible in the world itself.

The honest gap: WorldEdit does not keep its own permanent per-block record of who placed or removed what. A complete block-level audit trail would need a block-logging plugin (CoreProtect is the standard one). Mike decided October 14, 2026 to go without it: the audit for the pilot is command logs plus world inspection, which catches big moves but not every single block. That is the accepted setup.

## Decisions Mike needs to make

All four were decided October 14, 2026:

1. WorldEdit in pilot v1: YES.
2. Clipboard copy/paste: YES, trust-based, with the end-of-cycle audit above.
3. The two disclosed delayed-reward limitations: ACCEPTED with the clean-entry mitigation.
4. Marvin is the sole pilot builder; the role may expand later, only by Mike's decision.

CoreProtect follow-up: CLOSED October 14, 2026. Mike chose to go without a block-logging plugin; the pilot audit is command logs plus world inspection.

## Explicit non-goals

- No installation from the handoff archives. The pilot is a separate candidate built to this spec.
- No provider patches to Jobs, mcMMO, or EliteMobs.
- No spawn-point change. Activating the finished hub as the live spawn is a separate later approval.
