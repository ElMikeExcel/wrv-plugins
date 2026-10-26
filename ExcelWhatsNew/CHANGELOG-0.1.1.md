# ExcelWhatsNew 0.1.1 (Oct 26, 2026): Newer news button

Approved by Mike Oct 26, 2026, 9:15 AM CDT ("Yes, add a Newer news button").
Base: ExcelWhatsNew 0.1.0, live jar sha256 691ee97a25f7c603ecb057eab9f60dca04f399268e50236b76a7c01ea0ebe979
(26,858 B). The box source (src-0.1.0/) rebuilds that jar byte for byte, so the diff below is the only change.

## Player-facing
- **Newer news button.** When a player is reading any entry other than the newest one, a Newer news
  button now shows. It opens the next newer entry (`/whatsnew <n-1>`). Before, pages only went
  one way (Older news), so going back to the newest meant closing and reopening.
  - Java: clickable chat row, left to right: `[OK]` (only on the entry still waiting for OK),
    `[Newer news]`, `[Older news]`. Hover text: "Read the news after this".
  - Bedrock form buttons, top to bottom: OK (only on the entry waiting for OK), Newer news,
    Older news, Close (only when there is no OK button, same rule as 0.1.0).
  - On the newest entry nothing changes except that Close moved below Older news when both show.
  - On the oldest entry players can reach (show-count, live = 10), only Newer news (plus OK or Close) shows.
- **Typed fallback line** (used when the Bedrock form cannot open and the news falls back to chat)
  now names both directions, for example "Newer news: /whatsnew 3. Older news: /whatsnew 5." It no
  longer prints an empty grey line when there is nothing to say.
- **Bedrock `/whatsnew list` hint** used to hardcode "Type /whatsnew 2 (or 3) to read an older one."
  It now follows the real count: "Type /whatsnew 2 to /whatsnew 10 to read an older one." With 2
  entries it says "Type /whatsnew 2 to read the older one." With 1 entry it says nothing.
- Command description in plugin.yml mentions "/whatsnew 1 goes back to the newest".

## Config
- New key `text.newer-button`, default `Newer news`. The code falls back to `Newer news` when the
  key is missing, so the live config.yml (62b95a5e, which does not have the key) keeps working
  untouched. Do not edit the live config for this update.
- The default config.yml inside the jar has the new key (only used if config.yml is ever deleted).
- Java [OK] now uses `text.ok-button` (was a fixed "OK"), matching Bedrock. Live value is 'OK', so
  the text is the same.

## Code
- New `core/Nav.java`: pure paging logic (which buttons, in what order, what number each opens,
  the typed hint, the list hint). Both platforms use it, so Java and Bedrock cannot drift.
- Bedrock form callbacks now map the clicked button id through the same `Nav.Act` list that built
  the form, because buttons are conditional (0.1.0 used fixed ids 0 and 1).
- `open()`, `chat()` and `openForm()` no longer take a `more` flag; they ask Nav.
- Version 0.1.1 in plugin.yml, config header and class doc. Jar date 2026-10-26T00:00:00Z
  (deterministic build, same toolchain as 0.1.0: JDK 25 javac --release 21, Paper API compile libs).

## Not changed
- Join announcement, calm-moment logic, OK/seen storage (players.yml format), reload/check/status/
  reset/push, changelog.yml format and validation, max-age, show-count handling.
