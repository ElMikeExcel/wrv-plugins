# ExcelDowned 1.0.0 — RIG RESULTS

- Mirror rig: board run 510 W5 on the pre-F1 jar — boots
  SEVERE 0, catalog + console legs PASS, Vault-absent
  fail-soft proven. The F1 fix is a one-file change on top
  of that rig-proven jar with its own pinning test.
- Release gate (D122 ship-it rule, Nov 18, 2026): mirror
  loads clean + automated tests pass (71/71) + live load in
  the install window = SHIP IT. Player-flow proofs are
  post-release and did NOT gate this release.
- Live install boot verify (failsafe run 20261118-1634,
  RUN-RECORD.md): boot Done (23.794s); "ExcelDowned 1.0.0
  ready — build EXC-DOWNED-1.0.0-WRV-20261117-01-d0wn3d";
  'purchase journal unavailable' count = 0 (the F1 line,
  PASS on first boot with a fresh journal);
  purchases.journal created by the plugin at first boot;
  SEVERE 0; guild OPEN 5 of 5.
- Console spot check: 'downed revivepotion buy' resolves
  (unknown-player refusal/guidance shape = PASS).

## Proof still owed (post-release, NOT gates — D122)

- P4 per platform (Java + Bedrock): one real purchase each
  (balance -$5,000 exactly), one Cancel (no charge), one
  insufficient-funds attempt (no charge); menu Back works.
- A bought potion used in a live revive proof; Cleric
  Raise Ally proven live by a Cleric-active player.
Status until then: Installed + Released, unproven by design.
