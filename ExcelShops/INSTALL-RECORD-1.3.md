# ExcelShops 1.3 (clearer shop wording): INSTALLED LIVE Oct 16, 2026, 2:16 PM CDT
| Status | State |
|---|---|
| Built | Yes, Oct 16, 2026 (JDK 25, release 21), source = v1.2 (PR #25) plus wording changes |
| Automated tests | 374/374 (363 updated + 11 new wording edge-case tests) |
| Test server (scratch boot) | Passed, Paper 26.2 build 133 |
| Installed live | YES, Oct 16, 2026, 2:16 PM CDT |
| Java check | UNCONFIRMED |
| Bedrock check | UNCONFIRMED (longest button "Buy 36 stacks (2,304 Torches) for $1,440" on a phone not seen) |
SHA-256 ExcelShops-1.3.jar: a83a4d79ad28ac415768d52d26f84fe416dd748730f5c2581c916d39c9d20d68
Source zip SHA-256: 729995d300f692df1e04cf50e873eb8051ee925fcad00de86299eed124c40d0c
Drive: Minecraft WRV, Build files (jar) and Source (zip).
What changed: shop wording only. Prices, quantities, limits, and purchase logic unchanged.
- "Buy 1 stack (64 Torches) for $40", "Buy 16 Bread for $40", "Buy 3 packs (48 Bread) for $120", "Buy 1 Golden Apple for $300"
- whole-dollar prices lose ".00" (balance lines still show cents); "Open guarded review" is "Buy for $X"; "Enter exact amount" is "Type a number"
- final screen "Confirm your purchase" with "Confirm: pay $X"; browse screens "Close shop"
Install: Oct 16, 2026, 2:16 PM CDT, 1.2 jar kept as ExcelShops-1.2.jar.bak-20261016c (made 2:15 PM).
Rollback: stop via panel Shut Down, restore ExcelShops-1.2.jar from the .bak, remove 1.3 jar (one active jar), start.
Unconfirmed: byte-identity of PR #25 source to the live 1.2 jar (live 1.2 SHA f72f9f81...56a53 not rebuilt).
