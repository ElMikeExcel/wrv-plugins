# ExcelAttunement 0.1.3 (staged, not installed)

Built and tested Oct 24, 2026 (~12:15 AM CDT). Not live. Installs with the Magic (elmakers)
heal kit, before `magic load`, at a server restart (a jar swap cannot hot-reload).

## Why
The Magic heal kit adds new spell keys: mend, flashheal, ward, greaterheal, layonhands
(plus leveled forms such as mend|2). They were not in spell-classes.txt, so 0.1.2 treated
them as COMBAT (fail-closed) and a healer casting Mend out of combat was put in caster stance.

## Change
- spell-classes.txt regenerated with tools/gen_spell_table.py from the heal-kit stage configs:
  105 spells = COMBAT 36 / SUPPORT 17 / UTILITY 52 (was 100 = 36 / 12 / 52).
- The five heal-kit spells are SUPPORT, like heal and superheal. stormshackle stays COMBAT at
  every level. No existing spell changed class.
- Generator: ModifyMana counts as a support signal (eat_food is now a mana-restore spell in
  the stage configs; it was SUPPORT and stays SUPPORT).
- Tests: counts updated; new pins for every heal-kit key and its leveled forms, and for
  stormshackle|2..4 staying COMBAT.
- Java source unchanged from 0.1.2. plugin.yml 0.1.3. build.sh / run-tests.sh take
  JDK, LIBS, RT, MAGIC (and STAGES) from the environment.

## Results
- Plain-JVM suites: 309 passed, 0 failed. Display pins: 0 failed.
- Test server (Paper 26.3-214, Magic 11.2.5, heal kit configs): boots clean, "spell table 105
  spells", no unknown-key warnings. Bot checks: Mend out of combat = SUPPORT, no stance;
  Mend after taking a hit = SUPPORT, caster stance (by design); Storm Shackle = COMBAT.
- Jar: ExcelAttunement-0.1.3.jar, 26,458 bytes, two builds byte-identical. See SHA256SUMS-0.1.3.txt.
- The first commit on this branch is the 0.1.2 source exactly as deposited; rebuilding it gives
  the live 0.1.2 classes byte-for-byte (only the jar manifest's JDK build line differs).

## Rollback
Put the 0.1.2 jar back and restart. Heal-kit spells then fall back to COMBAT.
