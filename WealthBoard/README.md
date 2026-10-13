# WealthBoard 1.3.0-mobile.1

A narrowly scoped mobile layout update to the original WealthBoard 1.3.0 plugin. The oversized join title and single-line richest-player subtitle are replaced by a short welcome message and separate chat entries containing complete player names and balances.

See [the player guide](PLAYER-GUIDE.md) for what appears in game and how to reopen the existing full leaderboards.

## Scope

- Short `Welcome!` title and `Wealth in chat` subtitle.
- One ranked name line and one balance line per player; no truncation.
- Empty-state messages stay compact.
- Same layout for Java and Bedrock clients.
- Ranking, currency formatting, join timing, Jobs enrollment and plugin identity remain unchanged.

This candidate is based on the original 1.3.0 behavior. It deliberately does not incorporate the separate, broader 1.3.1 repair work. Reconcile that work explicitly before combining branches.

Requires Vault with an enabled economy provider. Jobs is optional. The existing `/leaderboard` command is supplied by the separate Leaderboard plugin and invokes the server's existing wealth and mcMMO commands.

## Build and tests

Use JDK 21 and Maven 3.9+, or run the checksum-verified Maven 3.9.9 wrapper from this folder:

```
./mvnw verify
```

Output: `target/WealthBoard-1.3.0-mobile.1.jar`. Dependencies are compile-time/provided, not bundled.

Four automated JUnit tests cover fixed compact titles, preservation of complete names and formatted amounts, top-three ordering and separate chat lines, empty balances, and disconnect before the delayed popup. One test includes ten focused text assertions.

The candidate also passed three isolated Paper 26.2 boots with synthetic players and economy: normal startup/join behavior, absent economy provider, and absent Vault. Those checks do not establish actual phone rendering or behavior with every installed economy/Jobs/Geyser combination. Real client verification is still required after an authorized installation.

## Installation and rollback

This is an undeployed candidate. Back up the existing WealthBoard JAR before replacement. Stop the server through its normal controlled workflow, replace the single WealthBoard JAR, and start it again. Never keep two active WealthBoard versions in the plugins directory, and do not use a full-server hot reload.

Check startup logs, then follow the mobile checklist in the player guide. To roll back, stop the server, restore the saved original JAR, and start again. No player-data migration is introduced by this layout change.
