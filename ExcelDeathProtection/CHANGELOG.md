# ExcelDeathProtection changelog

## 0.1.2 — two-tier offline candidate, October 13, 2026

**Not published or deployed. Java and Bedrock-mobile player checks are pending.** Automated results belong in the candidate's verification evidence and are separate from live acceptance.

- Split the product into **Death Protection**, $15,000, activated immediately after a confirmed purchase with no extra abilities, and **+Death Protection Potion**, $30,000, stored and activated by drinking.
- Keep the premium Conduit Power benefits, including faster mining on land, and disclose them consistently.
- Use a shared one-death active slot. Reject duplicate basic purchases before charging; cancel a second potion drink while either tier is active without consuming the potion.
- Add the basic boss bar **Inventory protected: 1 death**, without boss fog, sky darkening, or music.
- Add explicit `buy basic` / `buy premium` command forms. Preserve the old buy alias as premium intent with confirmation of the new $30,000 price.
- Add `basic-price` / `premium-price`; ignore the old single `price` setting.
- Preserve legacy active protection and recognized old bottles as premium at no extra charge. Refresh known original bottle names/lore lazily while preserving custom names and persistent item data.
- Avoid blanket Conduit/Luck removal because legacy effect ownership cannot be proven. Basic adds no effects; premium only fills a missing Conduit effect.
- Add transaction-journal handling for ambiguous purchases. Vault and player/plugin persistence are separate, so crash-atomic charging and delivery are not guaranteed; unresolved transactions require operator review rather than automatic recharge/refund. Player-save calls also lack a durable-write acknowledgment, so silent save failures can require manual reconciliation even after a completed journal entry.
- Supply unassigned shop templates and integration instructions. The verified source menu has no Death Protection entry; live slots and the menu generator must be inspected before a future authorized installation.
- Keep real death, one-use inventory retention, and unchanged XP handling. Correct the earlier Curse of Vanishing assumption: an isolated native Paper 26.2 test retained a cursed sword during a protected death using keep-inventory. No gameplay change was made to force cursed items to vanish. The live server/plugin combination still needs protected and unprotected checks.

## 0.1.1 — historical revision, October 13, 2026

These notes describe the earlier single-product version, not the two-tier candidate.

- Switched the indicator from Luck to Conduit Power after the 0.1.0 Bedrock-mobile test showed no Luck indicator.
- Disclosed Conduit Power's underwater breathing, clearer underwater vision, and faster mining, including mining on land.
- Added on-screen titles for purchase confirmation, insufficient funds, and successful purchase because chat can sit behind the shop window on Bedrock.
- Raised the single potion price from $10,000 to $15,000.
- The baseline notes did not establish a completed Java player check. Historical build or player results do not validate 0.1.2.

## 0.1.0 — historical first build, October 13, 2026

- Introduced the single Death Protection Potion: drinking activated one-death inventory protection. Death was not prevented and XP was not saved.
- Stored active protection per player across relogs/restarts and initially used Luck as its indicator.
- Added two-step `/deathprotection buy`, status, and admin give commands; the original price was $10,000.
- The historical Bedrock-mobile test passed purchase messaging, drinking, status, item retention on death, XP behavior, and consuming protection. The Luck indicator failed to appear on Bedrock; the original expectation that it would display there was incorrect.
