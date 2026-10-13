# Death Protection

**0.1.3 review-window preview. Version 0.1.2 is the last verified installed version (October 13, 2026, 23:40:51 UTC).** The owner reported that the two 0.1.2 shop entries look good on his phone and accepted buying one at a time without a quantity dialog. Exact-shortfall visibility motivated this candidate. All 0.1.3 Java, Bedrock-phone, real-purchase, and gameplay checks remain pending. Prices below are in-game money.

## Death Protection — $15,000

Buy it, confirm the price, and your protection starts immediately. There is no potion to drink.

A bar reading **Inventory protected: 1 death** shows that your inventory is protected. This option gives no extra abilities.

## +Death Protection Potion — $30,000

Buy and confirm to receive a potion. You can keep it for later. **You are not protected until you drink it.**

While its protection is active, you also get **Conduit Power**: underwater breathing, clearer underwater vision, and faster mining. The mining boost also works on land. Conduit Power has a blue shell icon, but that effect can come from other sources too. Use `/dp status` to check your protection.

## What both options do

- Your next death keeps your inventory once. You still die and respawn normally.
- Protection is used up by that death.
- Protection does **not save your XP**. The server's normal XP rules still apply.
- In an isolated server test, protection also kept a sword with **Curse of Vanishing**. The live server still needs checking.
- Active protection survives logging out and ordinary server restarts.

## One active protection at a time

You can have only one active protection, basic or premium.

- If either is already active, buying basic protection is blocked before you are charged.
- If either is already active, drinking another protection potion is cancelled and you keep that potion.
- A potion sitting in your inventory does not count as active protection.

Check your current protection with **/dp status**.

## Last verified installed flow: 0.1.2

In 0.1.2, choose the same shop option or repeat the same buy command within 20 seconds to confirm the product and price. It buys one at a time and does not open an ordinary-item quantity window. The earlier phone report checked the appearance of the shop entries only; it did not establish purchase, activation, or death/respawn acceptance.

## Candidate 0.1.3: buying one at a time

Tap either product in Spawn Shop, or use **/dp buy basic** or **/dp buy premium**. The candidate opens a review with **quantity 1**, the price, your balance, and the money you would have left. If you cannot afford it, it shows **Need $… more** instead and disables purchasing.

Opening, refreshing, closing, and going Back never charge you. Select **Confirm** to purchase. Your balance and eligibility are checked again then. A review expires after 20 seconds; tap its disabled button to refresh, then Confirm again. If your balance changed from too low to enough, a fresh quote may be required before purchasing. Back requests the existing Spawn Shop `/shop` command; actual Java and phone navigation through DeluxeMenus still needs checking.

The older **/dp buy** command still opens the **$30,000 premium potion** review. Repeating any buy command opens another review; it never buys automatically.

## If you already have protection or a potion

Your existing active protection and old Death Protection Potions stay **premium**. You do not pay again to keep or use them. Older bottles receive updated descriptions when checked in your online inventory; a custom name you gave your potion is kept.
