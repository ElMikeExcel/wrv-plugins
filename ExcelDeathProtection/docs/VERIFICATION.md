# Two-tier verification checklist

**Status: 0.1.2 installed October 13, 2026; client and gameplay acceptance pending.** [Installation evidence](INSTALLATION-0.1.2.md) records exact deployed hashes, configured prices, startup, provider, and menu registration. [Automated results](VERIFICATION-RESULTS-0.1.2.md) are separate. The unchecked scenarios below remain pending for real Java and Bedrock-mobile clients and the actual provider; a passing startup does not mark them passed.

## Evidence and release gates

- [x] Record installed source commit `a2586bf17d2322f5e3b313b7e45bc0fe32fc56ac`, exact JAR SHA-256, and prior build/toolchain evidence.
- [x] Record 48 automated tests and 128 isolated Paper assertions separately from real-client acceptance.
- [x] Preserve baseline PR #13 commit `ff224dea03d61e0f2227e65d8015d662c93c8d34` as historical input, not installed source identity.
- [x] Record prior packaged-resource and class-target checks; this documentation update changes no implementation or dependencies.
- [x] Record separately authorized installation, loaded version, installed hashes, configured prices, economy provider, and menu registration.
- [ ] Obtain separate authorization before test purchases or any real-balance/playerdata correction.
- [ ] Establish a genuinely completed recoverable backup; the rollout's fresh full backup did not produce a verified completed entry. Retained file copies are not a complete recovery snapshot.
- [ ] Reconcile the historical menu/generator with the live hand-integrated entries before future regeneration.
- [x] Record the owner's October 13 phone visual report: both shop entries look good. This is limited appearance evidence, not full UI or gameplay acceptance.
- [ ] Investigate exact-shortfall feedback visibility. The owner accepts the lack of a quantity window for these single-item purchases.
- [ ] Run and record the Java player checks below.
- [ ] Run and record the Bedrock-mobile player checks below, with Geyser/Floodgate/server versions as applicable.

## Product and confirmation checks

- [ ] First basic purchase shows **Death Protection / $15,000** and makes no withdrawal. Same-product confirmation within 20 seconds charges once and immediately activates basic with no item delivered.
- [ ] First premium purchase shows **+Death Protection Potion / $30,000** and makes no withdrawal. Confirmation charges once and gives the real marked potion without activating protection.
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

- [x] Inspect the live menu; record basic slot 31 and premium slot 34. The historical manual generator remains an open overwrite risk.
- [x] Install integer, non-conflicting slots; the active file has no template placeholders.
- [x] Verify installed YAML names, prices, activation wording, XP disclosure, and premium benefits, including mining on land. This is a file check, not visual acceptance.
- [x] Verify each installed left/right click route calls exactly one tier-aware plugin buy command; neither route withdraws money or gives an item independently.
- [ ] Verify confirmation visibility while the shop is open, including Bedrock-mobile tap behavior and product changes.
- [ ] Confirm the deployed player-facing guide matches the installed implementation only after publication is authorized.
- [ ] Record remaining failures/blocked cases. Do not call the feature live-ready until the required gates are satisfied.

- [ ] Storage-fault rehearsal covers both a lost paid grant and a rollback that appears complete in memory but silently fails to persist, potentially restoring a refunded grant after restart.
