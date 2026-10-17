# ExcelAcrobatics 1.1

Jar: `ExcelAcrobatics-1.1.jar`
SHA-256: `8d64f9323f6763d29e1d8874b55336b7ec4477b777cde164adc20bb6721d13b3`

Source tarball: `ExcelAcrobatics-1.1-source.tgz`
SHA-256: `d05cd085672b505614809ec0444782aecdaaa24cb98fd3ea9fc8efb536abeaae`

That tarball is in this folder, and its source is extracted here (`build.sh` and `src/`). The 1.1 fix is in `CHANGES-1.1.md`. The scratch boot log is `scratch-boot-1.1.log`.

In-game: UNTESTED. Built: yes. Unit tests: pass. Scratch boot: partial. Installed live: yes, 7:57 PM CDT Oct 16, 2026. Java check: UNCONFIRMED. Bedrock check: UNCONFIRMED.

`CHANGES-1.1.md` was written as not installed live. This record says installed live at 7:57 PM CDT Oct 16, 2026.

Mike's rule (Oct 14, 2026): falling from a high place and landing in water
should pay mcMMO Acrobatics XP. Vanilla mcMMO pays Acrobatics XP only
through its fall-damage hook, and water cancels fall damage entirely, so
water landings paid nothing.

This plugin watches falls; when one ends in water it awards, through
mcMMO's own API, what the same fall would have paid on land (the fall's
damage points x the Fall rate from mcMMO's experience.yml, 600 here,
including the Feather Falling damage reduction and mcMMO's 2.0x Feather
Falling XP compensation), PLUS a style bonus for height (+5% per block
past 10, capped at +100%), with same-spot repeats inside 10 minutes
paying less (75/50/25/10%) instead of stopping, so new spots pay best.

No XP while flying, gliding, riding, slow-falling, in Creative/Spectate,
or in an ExcelBuildMode session. Per-landing cap 25,000 XP.

Fatal falls: mcMMO pays nothing when a fall kills. Per Mike's rule
(clarified Oct 14, 2026), a death fall pays Acrobatics XP (final damage
x rate, no style bonus) under ONE condition: the death actually cost a
decent amount of vanilla XP, at least 200 points lost after counting
the recoverable dropped orbs. Build Mode deaths never pay.

Install checklist: one calibration jump on land first (known height,
read the Acrobatics XP gained) to confirm xp-per-damage-point against
the exact live mcMMO build, then a test water landing.

Build: ./build.sh

## Dependencies (what this plugin relies on)

- Paper (or a compatible fork) 1.21+. Required.
- mcMMO: required for awards. XP is granted through mcMMO's own
  Experience API, probed at startup against the exact installed build.
  If mcMMO is missing or no known API signature verifies, awards stay
  off, no fake XP is tracked anywhere, and the server log says why.
- ExcelBuildMode: optional companion. Players inside a Build Mode
  session earn nothing from this plugin (checked via ExcelBuildMode's
  active-session index).
