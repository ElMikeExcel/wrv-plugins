# Changelog - ExcelWealthBoard (WealthBoard)

## 1.3.0-mobile.7 - unreleased candidate

- Remove intermediate 5-tick fade-outs and extend intermediate stays to 70 ticks,
  overlapping the unchanged 60-tick ranking updates. Keep the first fade-in and
  final 55-tick stay / 15-tick fade-out unchanged.
- Preserve the welcome title, one-ranking-per-subtitle layout, text, sorting,
  currency formatting, 10-second join delay and Jobs behavior.
- Add a pinned standalone build and eleven scheduler/API regression cases.
  Identical tests against mobile.6 fail three timing checks as expected.
- Not installed. Real Java and Bedrock/Geyser rendering checks remain pending.

## 1.3.0-mobile.6 - 2026-10-13 (live)

- Popup start delayed to 10 seconds after joining (was 6s in mobile.5).
  With 6s, the owner still missed 1st place behind the loading screen;
  with 10s the full cycle is visible. Bedrock/mobile player check passed.

## 1.3.0-mobile.5 - 2026-10-13

- Popup start delayed to 6 seconds after joining (was 2s). Owner saw only
  2nd and 3rd place; 1st still played during loading.

## 1.3.0-mobile.4 - 2026-10-13

- Each ranking now shows for 3 seconds (was about 1.75s in mobile.3, too
  fast to read). Owner then saw only 3rd place, which led to the start
  delay changes in mobile.5 and mobile.6.

## 1.3.0-mobile.3 - 2026-10-13

- Rankings cycle one entry at a time under the Wink Reyes Valley title.
  Same repeated-title pattern the restart countdown already uses.

## 1.3.0-mobile.2 - 2026-10-13 (superseded, never accepted)

- Restored the popup with one ranking per line inside the subtitle.
  Failed on Bedrock: lines after the first never rendered.

## 1.3.0-mobile.1 - 2026-10-13 (rejected)

- Changed the popup to a large "Welcome!" title and moved the rankings
  into chat lines. Rejected by the owner: the Wink Reyes Valley popup and
  the on-screen leaderboard must stay, with rankings fitting on screen.

## 1.3.0 - 2026-10-11

- Original design: Wink Reyes Valley title popup with the top 3 richest
  on one subtitle line, plus automatic Jobs enrollment on join. The
  one-line subtitle clipped on phone screens, which started the mobile
  fix series above.
