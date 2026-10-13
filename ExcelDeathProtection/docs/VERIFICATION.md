# Two-tier verification checklist

**0.1.3 source candidate; 0.1.2 is the last verified installed version at 23:40:51 UTC. All real Java, Bedrock-phone, live-purchase, and gameplay acceptance below remains pending.** [Candidate evidence](VERIFICATION-RESULTS-0.1.3.md) separates automated results and current operations from [0.1.2 installation history](INSTALLATION-0.1.2.md). Checked evidence items below are narrowly scoped; unchecked real-client/provider cases have not passed merely because startup or synthetic tests passed.

## Evidence and release gates

- [x] Record exact 0.1.3 artifact: 58,521 bytes, SHA-256 `f0224d18a51e430b81ec8fb02f9408fd6038e64b893d84d738368fd288cb4093`; Java 21.0.12.1, Maven 3.9.9, pinned cached dependencies, clean offline builds.
- [x] Record 99 candidate tests, 107 including the same 99 plus eight independent additions, and 196 isolated Paper assertions. These are synthetic tests, not actual-provider/client acceptance.
- [x] Separate the tested implementation/resources/tests from the tested-doc snapshot and the reconciled publication docs in [the 0.1.3 manifest](SOURCE-MANIFEST-0.1.3.json).
- [x] Record unchanged dependency set, packaged 0.1.3 metadata/prices, Java-21 class targets, and absence of test/fixture classes.
- [x] Record later provider-reported full stopped-instance backup completion at 23:39:15 UTC and exposed Restore option. This is separate from the unsuccessful earlier 0.1.2 rollout backup attempt.
- [ ] Establish recovery integrity: no restore or archive-integrity/manifest test has been performed. Recheck that the selected recovery point covers later gameplay/economy activity before changing the installation.
- [ ] Verify a separately authorized 0.1.3 installation: loaded version, artifact hash, current live config/menu, economy provider, and relevant logs. Source publication is not deployment evidence.
- [ ] Obtain separate authorization before test purchases or any real-balance/playerdata correction.
- [ ] Reconcile the historical menu/generator with the live hand-integrated entries in a separately scoped update before future regeneration.
- [ ] Verify candidate exact-shortfall feedback on an actual phone; the reported 0.1.2 concern is not closed by automated checks.
- [ ] Run and record real Java player checks below.
- [ ] Run and record real Bedrock-mobile checks below, with Geyser/Floodgate/server versions as applicable.

## Preserved 0.1.2 evidence (historical, not 0.1.3 acceptance)

- [x] Record installed source commit `a2586bf17d2322f5e3b313b7e45bc0fe32fc56ac`, exact JAR SHA-256, and prior build/toolchain evidence in [installation history](INSTALLATION-0.1.2.md).
- [x] Preserve baseline PR #13 commit `ff224dea03d61e0f2227e65d8015d662c93c8d34` as historical input, not installed source or candidate identity.
- [x] Preserve 48 automated tests and 128 isolated Paper assertions for 0.1.2 in [its historical results](VERIFICATION-RESULTS-0.1.2.md).
- [x] Preserve authorized 0.1.2 installation, loaded version, exact files/prices, economy provider, and menu registration; 0.1.2 was reconfirmed loaded at 23:40:51 UTC.
- [x] Preserve the owner's narrow phone report that both 0.1.2 entries look good. The owner accepts fixed-one purchases without the ordinary quantity window. This is not purchase, gameplay, Java, full phone, or new-review acceptance.
- [x] Preserve installed slot 31/basic and slot 34/premium, integer/non-conflicting slots, YAML disclosures, one plugin command per left/right route, and all 20 unrelated items/non-item settings. These checks describe the recorded 0.1.2 snapshot, not a fresh 0.1.3 live read.

## Review-window release checks

- [ ] Both tiers display quantity 1, price, balance, projected remainder or positive shortfall clearly on Java and a real Bedrock phone.
- [ ] Open, repeated buy commands, refresh, Back, close, quit and stale callbacks never charge or grant.
- [ ] Only current owner inventory plain Confirm can transact; shifted, double, creative, number-key, offhand, drop, bottom and outside clicks plus drags are blocked.
- [ ] Insufficient funds, funds changed between click and execution, new active protection, full potion inventory, changed prices and expiry all fail/requote safely.
- [ ] Opening from DeluxeMenus safely replaces the shop; Back restores its existing /shop menu. Confirmation survives no close/reopen boundary.
- [x] Offline candidate testing used no new dependencies, new legal acceptance, production server changes, or real balances/player data. The existing isolated fixture agreement was retained unchanged.

## Product and confirmation checks

- [ ] Opening basic review shows **Death Protection / quantity 1 / $15,000**, current balance and projected remainder/shortfall, and makes no withdrawal. Its Confirm within 20 seconds charges once and immediately activates basic with no item delivered.
- [ ] Opening premium review shows **+Death Protection Potion / quantity 1 / $30,000**, current balance and projected remainder/shortfall, and makes no withdrawal. Confirm charges once and gives the real marked potion without activating protection.
- [ ] Switching products, changed effective price, expired confirmation, and rapid/repeated clicks cannot reuse the wrong quote or double charge. Verify the one-second successful-purchase cooldown and clearing of unused quotes on logout.
- [ ] Bare player `/dp buy` and old console `buy <player>` are premium aliases. They show/confirm $30,000, never an unconfirmed legacy $15,000 charge or unintended basic purchase.
- [ ] Explicit player/admin/console forms resolve the intended player and reject malformed/unauthorized usage.
- [ ] Insufficient balance, unavailable economy provider, invalid configuration price, offline target, and premium inventory-capacity failures make no successful charge or delivery claim.
- [ ] Basic can activate without an empty inventory slot, because it delivers no item.
- [ ] Premium bottle storage alone leaves `/dp status` inactive. Drinking activates premium once.

## Shared active slot and death checks

Run each relevant case with basic active and again with premium active.

- [ ] Another basic purchase is rejected before charging, including a purchase armed before protection became active.
- [ ] An externally cancelled or replaced consume event cannot grant premium, apply an effect, save an entitlement, or report success; next-tick persistence sees the actually consumed bottle. Test immediate damage/death and save failure at this boundary.
- [ ] A premium potion drink is cancelled and the original potion/stack remains unconsumed; active tier does not change or refresh.
- [ ] A real death happens and respawn is normal. Inventory contents, armor, and offhand are retained exactly once, with no duplicate world drops.
- [ ] Protection is then inactive; a second ordinary death receives no protection from the consumed entitlement.
- [ ] XP event handling is unchanged, tested against the configured normal death rules; the plugin neither saves XP nor overrides another policy.
- [ ] Record actual Curse of Vanishing outcomes for basic-protected, premium-protected, and unprotected deaths, including relevant equipment slots. An isolated native Paper 26.2 test retained a protected cursed sword under keep-inventory; do not use “protected cursed items must vanish” as an acceptance criterion. Verify the current server/plugin combination without modifying gameplay to fit the old assumption.
- [ ] Test representative death causes/dimensions, including void death, and interactions with the server's other death/inventory plugins, particularly competing `HIGHEST` handlers. Event mutation by other plugins at `MONITOR` is outside the supported contract; identify such plugins explicitly.
- [ ] Repeated events, join/respawn timing, relogs, ordinary restarts, and watchdog ticks cannot restore a consumed entitlement.

## Basic UI and effect isolation

- [ ] Basic bar reads exactly **Inventory protected: 1 death** on Java and Bedrock mobile.
- [ ] No boss fog, sky darkening, or boss music occurs.
- [ ] Basic grants no Conduit, Luck, water-breathing, vision, mining, resistance, or other extra ability.
- [ ] Basic indicator appears/restores at the correct times and is removed when protection is consumed, on player disconnect, or plugin shutdown as appropriate.
- [ ] External Conduit/Luck effects remain untouched by basic activation, migration, and plugin lifecycle operations. Account separately for vanilla effect removal on death.

## Premium benefits and item metadata

- [ ] New potion name is **+Death Protection Potion** and lore discloses one real death, XP not saved, Conduit benefits, and faster mining on land.
- [ ] Premium grants missing Conduit Power, shows the expected effect UI, and restores the effect after ordinary clearing while protection remains active.
- [ ] Existing stronger/externally applied Conduit effects are not replaced; unrelated Luck is not removed.
- [ ] Source attribution is not inferred from a matching effect duration/amplifier. No blanket cleanup of old infinite effects occurs.
- [ ] Verify underwater breathing, clearer underwater vision, and mining both underwater and on land in the actual Java and Bedrock-mobile server setup.
- [ ] `/dp status` reports entitlement accurately even when a real conduit or another plugin gives Conduit to an unprotected player.

## Legacy compatibility

- [ ] Legacy active byte-1 fixtures with absent and unknown tier markers are recognized as premium with no charge, downgrade, reset, or lost protection across relog/restart.
- [ ] Recognized old potion-marker fixtures remain drinkable premium potions with no migration charge.
- [ ] Known original names/lore refresh at enable/join and on the five-second online-inventory check; custom renamed bottles keep their custom display name.
- [ ] Preserve all persistent item data and other unrelated metadata; do not replace the whole bottle or change counts.
- [ ] Ordinary unmarked potions and items merely named “Death Protection Potion” are not accepted as premium entitlements.
- [ ] Containers/offline inventories are not reported as fully migrated merely because an online inventory pass ran.
- [ ] Legacy `price` cannot silently override either new tier price; an old config still receives clearly documented effective defaults.

## Economy and crash boundaries

Use isolated test data and controlled interruption points, never an unapproved experiment with real in-game balances.

- [ ] Journal creation failure blocks the purchase before withdrawal.
- [ ] Known withdrawal rejection does not deliver or claim a successful purchase.
- [ ] Interrupt before withdrawal, during/after economy response, during entitlement/bottle persistence, and before journal completion; record each outcome.
- [ ] Ambiguous transaction records in `purchases.journal` survive restart and block further purchases for the affected player. An unreadable/corrupt journal disables all purchases and is never replaced with an empty audit.
- [ ] No ambiguous outcome triggers an automatic re-charge, refund, duplicate grant, or record deletion.
- [ ] Operator review can correlate UUID, product, amount, time, economy history, server save warnings/settings, and delivery evidence.
- [ ] Inspect player-data-save settings. Simulate silent save failure and disabled saving in a controlled server: `Player.saveData()` returning normally cannot be treated as proof of durable delivery. Record the residual paid-entitlement-loss risk, including completed-journal cases, and the manual reconciliation evidence.
- [ ] Review and test a controlled, authorized operator-resolution procedure; the candidate has no automatic recovery command and journal edits/deletion must not be improvised as an unlock.
- [ ] Document what cannot be made crash-atomic across Vault/player/plugin storage. Do not mark a mocked single-process test as proof of cross-system crash safety.

## Menu integration and final acceptance

- [ ] Freshly inspect the live menu before 0.1.3 rollout, preserving recorded slots 31/34, all unrelated products/settings and any later edits. The historical repository menu/generator remains an overwrite risk.
- [ ] Change only the two reviewed instructions to “Tap to review 1. No charge yet.” Do not load the unassigned template as a complete menu.
- [ ] Reverify names, prices, activation wording, XP disclosure, premium benefits including land mining, and matching effective config.
- [ ] Reverify each click route calls exactly one tier-aware buy command with no independent withdrawal/delivery or additional commands dismissing the review.
- [ ] Verify confirmation visibility while the shop is open, including Bedrock-mobile tap behavior, Back, close, expiry/refresh, price/funds changes, repeated commands and rejected inventory actions.
- [ ] Confirm the deployed player-facing guide matches the installed implementation only after publication is authorized.
- [ ] Record remaining failures/blocked cases. Do not call the feature live-ready until the required gates are satisfied.

- [ ] Storage-fault rehearsal covers both a lost paid grant and a rollback that appears complete in memory but silently fails to persist, potentially restoring a refunded grant after restart.
