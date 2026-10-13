# ExcelDeathProtection — spec (agreed with Mike, Oct 13, 2026)

Status: SPEC ONLY. Not built, not installed. Build starts on Mike's go-ahead.

## What it does
- A one-use buff that saves a player's ITEMS through one death.
- It does NOT stop the death: the player dies and respawns normally, keeps their items, and the buff is consumed.
- XP is NOT saved. Player levels/XP drop as usual. This must be stated plainly in the item description and player guide (Mike's explicit requirement).

## How players get it
- A custom potion sold at the spawn shop (ExcelShops / Spawn Shop).
- Drinking the potion activates the buff. The buff stays active until a death uses it (no timer planned; confirm at build time).
- Potion price: $10,000 (set by Mike, Oct 13, 2026). Note: that's above every current player balance (top is ~$6,481), so at launch it's a save-up item nobody can buy yet.

## Indicator (how players know it's active)
- The buff is a real status effect, so it shows as a small icon beside the HUD (like Speed/Strength) and is listed inside the inventory screen, on Java and Bedrock. Small and out of the way was Mike's requirement.
- Display text: "☠✔ Items protected" (Mike's pick).
- Build approach: first try registering a fully custom effect with that name/icon. Fallback: borrow a vanilla effect (Resistance shield icon) knowing its list name would read "Resistance".

## Item description (player-facing, draft)
- Potion name: Death Protection Potion (working name)
- Lore/description must say: "Drink to protect your items. If you die while this is active, you keep your items once. Does NOT save your XP levels."

## Behavior details to lock at build time
- One active buff per player; what happens if a second potion is drunk while active (expected: no stacking, potion not consumed or allowed as refresh — decide at build).
- Works in all dimensions and for all death causes, including void (items are kept on the player, nothing drops, so void is safe).
- On the death that uses it: buff clears, effect icon disappears, player gets a short message that their protection was used.
- Buff persists through logout/login (expected yes; store per-player).
- Cursed items (Curse of Vanishing) still vanish — vanilla behavior, note in guide.

## Verification gates (per WRV workflow)
- Built / installed live / Java player check / Bedrock-mobile player check are separate gates.
- Any test purchase with real in-game money needs Mike's agreement on item + cost first.
- Existing-solutions check (done Oct 13, 2026): KeepInventoryCost (auto pay-per-death toggle, not a one-use potion buff, no custom indicator), death-chest and grave plugins (against Mike's no-Graves death philosophy), DeathWarden (permission-based, old). None matches this design; a small custom Excel plugin is the fit.
