# MANIFEST ENTRY DRAFT — ExcelMenu 3.0.1 (Bedrock Server Menu section colours)

**For the board supervisor to file** (desk build does not file).
Packet: `~/workspace/menu-colors-20261113/packet/INSTALL-PACKET.md`.

```markdown
## PENDING — ExcelMenu 3.0.1 (Bedrock Server Menu section colours)

- Jar: `packet/ExcelMenu-3.0.1.jar` — 330,214 B /
  `5bb1c1a3c45ce4b54e75a5e5b6e5d03b50f06c5708afd3904c3cf85e031b2939`
  (double-build byte-identical; suite OK 169 tests).
- Live filename at swap: `plugins/ExcelMenu-2.0.0.jar` (same name as
  the 3.0.0 content it carries); outgoing jar kept in place as
  `ExcelMenu-2.0.0.jar.bak-20261113-pre301`.
- Place `packet/filing-data/config.yml` — 3,215 B /
  `c0467b5f04a2c77424eab776e02b3076caa4d2f20be62eff3aa949692050eef7`
  → `plugins/ExcelMenu/config.yml` (in-place .bak first).
  Palette only: Travel `&a`, Me `&d`, Shops `&6`, Help `&c`,
  Settings `&e`, Chest Organization `&8`; new `pin-color: "&1"`.
- Premise (re-pull fresh at filing; STOP on mismatch): live jar
  3.0.0, 329,425 B / `306607ba…`; live config.yml 2,564 B /
  `910c2f83…`. No layout or entries change.
- ONE backup (cap-at-6) + ONE restart. Emptiness `list` 0/10 twice.
- Boot verify: "ExcelMenu 3.0.1 enabled … 93 entries / 2 registry
  menus / 13 definition menus"; SEVERE 0; TPS 20.0; placed files
  pull-back EXACT; guild LOCKED 4 of 5 unchanged.
- Rollback: restore .bak jar + .bak config, restart, verify the
  3.0.0 enable line.
- Player proofs owed after install (Mike, Bedrock): picker colours
  (Travel green / Me purple / Shops gold / Help red / Settings
  yellow / Chest Organization dark gray), pins still blue, section
  forms coloured, Back returns to the picker.
- Avoid the Sat Nov 14 Warden-attempt evening (~8:30 PM, flexible
  9–10 PM); weekend protocol governs a weekend landing.
```
