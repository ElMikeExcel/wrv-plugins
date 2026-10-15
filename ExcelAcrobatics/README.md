# ExcelAcrobatics 1.0

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
