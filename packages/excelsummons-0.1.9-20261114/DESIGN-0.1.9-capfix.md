# ExcelSummons 0.1.9 (build seq 02) — total cap counts CAST summons only

Commission: P-SUMMONS-CAP-1 (Mike, Nov 14, 2026, ~12:48 AM CST).
This CHANGES his locked Nov 10, 2026 cap rule. Base: the
built-but-unfiled 0.1.9 tree (pet names + rename, seq 01);
this fix folds into the 0.1.9 line before filing — version
stays 0.1.9, build seq rotates 01 → 02 per the fingerprint
convention (seq/hex rotate per distributed build).

## The defect

The rank-scaled total cap (Beginner 1 / Student 2 /
Apprentice 3 / Master 5) counted every registry record in
COMBAT + COMPANION + PACK, regardless of origin. Mike's 8
adopted (tamed) wolves against an Apprentice cap of 3 blocked
every cast — and the counted set could never be released by
the player, because typed dismiss scopes never touch adopted
pets (correctly: "Nothing to dismiss"). The counted set and
the dismissible set disagreed.

## The rule (Mike, Nov 14, 2026)

The total cap counts CAST summons only. Adopted pets
(tamed/bred originals in the registry) hold no mana
(0.1.6 ruling, unchanged) AND occupy no cap slot.

## The fix

- `SummonRegistry.countsTowardCap(rec)` — NEW single
  predicate: counted branch AND `Origin.CAST`.
  `countCounted` now counts through it. This is the only
  counting path the total cap uses (CapPolicy.check +
  refusalMessage), so PreCast and capture-backstop agree.
- `CapPolicy.countedNames` / `anyCountedReleasable` now read
  the same predicate: the refusal message counts and names
  CAST summons only (adopted pets are never blamed), and
  every counted record is releasable, so the message's
  dismiss advice is always true. The legacy
  "can only leave by death" text branch is retained
  defensively in `totalCapMessage` but is unreachable
  through `check()` under the cast-only count.
- Unchanged: per-spell SPELL_CAP (`countSpell`), the combat
  SERVER_CEILING, all dismiss scopes (typed dismiss still
  never touches adopted; the menu per-pet confirm dismiss
  remains the only adopted release), the mana/reserve model,
  the rank ladder and thresholds, config values.

## Acceptance (tests)

(a) cast summons at cap still blocked — `castSummonsAtCapStillBlocked`;
(b) adopted pets over cap do not block — `adoptedPetsOverCapDoNotBlockCasts`
    (the incident inverted: 8 adopted wolves, cap 3, casts 1–3 legal, 4th CAST refused);
(c) adopted pets consume no mana — `adoptedPetsConsumeNoMana`.
Carried cap tests that asserted counted totals were migrated
from the ADOPTED-default `rec()` helper to `recCast()` so
they assert the cast set they always meant; the one message
test whose premise the new law retired (adopted-only
TOTAL_CAP variant) now pins `check() == NONE` for an
adopted-only set and pins the legacy text shape directly on
`totalCapMessage`.
