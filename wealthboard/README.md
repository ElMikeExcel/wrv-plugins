# WealthBoard

**Version:** 1.3.0

Wealth info without a permanent sidebar. On every join, players see a brief title popup naming the top-3 richest players. Full boards live in `/leaderboard`.

Also auto-enrolls players in all Jobs (via the Jobs API) so they earn money through ordinary play with zero friction — no manual `/jobs join` needed.

Requires Vault (economy). Soft-depends on Jobs.

## Build

```bash
javac -cp paper-api.jar:Vault.jar:Jobs.jar -d classes $(find src -name "*.java")
cp plugin.yml classes/
jar cf WealthBoard-1.3.0.jar -C classes .
```
