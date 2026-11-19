# ExcelShops 1.3.10 revivepatch — RIG RESULTS

- Mirror rig: board run 510 W5 (combined Downed + Shops
  unit) — boots SEVERE 0, catalog + console legs PASS.
  The rig jar (269,780 B / 6245b502...) was packaged ad hoc
  and is marked RIG ONLY; it is NOT the shipped candidate.
  The shipped candidate was packaged at filing from the
  patch tree (see BUILD-RECORD.md).
- Release gate (D122): mirror loads clean + tests 443/443
  + live load in the install window = SHIP IT.
- Live install boot verify (failsafe run 20261118-1634):
  "Enabling ExcelShops v1.3.10" + "Economy: EssentialsX
  Economy"; NO protection-only / catalog-invariant error
  (the 22-product invariant is enforced at enable and held);
  ExcelMenu loaded spawnshop + spawnshop_potions_protection
  with no menu error; SEVERE 0; guild OPEN 5 of 5.

## Proof still owed (post-release, NOT gates — D122)

P4 per platform: one real purchase (balance -$5,000
exactly), one Cancel (no charge), one insufficient-funds
attempt (no charge); menu Back works. Status until then:
Installed + Released, unproven by design.
