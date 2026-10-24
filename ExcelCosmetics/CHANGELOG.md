# ExcelCosmetics changelog

## 0.1.2
- **Owned check before charging.** A purchase of something the player already owns forever is refused with `ALREADY_OWNED` before any money moves. This applies to rent, 4 weeks and own. Under 0.1.1, re-buying an owned color charged $15,000 for nothing.
- **Auto-equip and buyer message.** After a successful `buy` (color, effect, set or emote pack, at any tier), the item is worn right away. If a set was worn, buying a color or effect takes the set off so the purchase shows; the set stays owned. The buyer is told in chat using only Bedrock-exact colors, for example: "You bought Pink (1 week)! It's now on. Switch anytime with /cosmetics, or /cosmetics equip color pink." Failed buys (already owned, not enough money, retired, payment problem) also tell the buyer that nothing was charged. Hand `grant`s keep the old fill-an-empty-slot rule.
- **Chat colors v2 palette.** 21 colors, each hex the exact RGB of one Bedrock chat code (16 classic plus the material codes g h i n p q s u v), so Geyser maps each to that code and Java matches Bedrock. Dropped as too dark to read: &0 &1 &4 &8 &j &m &t.
- **`for-sale: false`** on a color retires it: owners keep wearing and equipping it, but `buy` refuses it (`NOT_FOR_SALE`). Old ids bubblegum, magenta, lavender, sky, mint, peach and coral are retired and now use the hex of their closest v2 color, so no purchase is lost.
- Set colors moved to exact Bedrock codes. Effects and prices are unchanged.
- Readability floor lowered from 0.18 to 0.10 luminance (about 3:1 against black), so Blue (&9) and Grape (&5) qualify.
- Tests: 285 passed, 0 failed. New tests cover the exact-code/unique-code palette checks, a config.yml-vs-fixture cross-check, owned and retired refusals that never touch the wallet.
