# WRVWandDiagnostic 0.1.0 — BUILD RECORD (as deposited Oct 26, 2026)

From the candidate package's own records (`build-result.txt`,
`test-result.txt`, `README-CANDIDATE.md`, `smoke-result.json`):

- Build: full compilation passed (one deprecation note). Output jar
  `WRVWandDiagnostic-0.1.0-offline.jar` SHA-256
  `a234c934c4e7da1a1f8788d7336081c59705b955d4f9139ac95aea11967d34f1`.
- Tests: 21 diagnostic assertions passed + 19 real packet assertions
  passed (synthetic session, not device) = 40 total. See `test-result.txt`.
- Disposable boot: isolated loopback Paper 26.3-214 with exact Geyser
  b1269, Magic 11.2.5, WandBars 0.3.2 — plugin enabled, blank-owner
  console start refused, `show` stayed OFF. Registry integration probe
  FAILED in the sandbox (Geyser encryption init could not resolve
  Minecraft discovery endpoints — sandbox DNS), explicitly NOT passed.
  See `smoke.log` / `smoke-result.json`.
- Nature: console-only, owner-filtered, capture OFF by default,
  60-second hard-limited gesture diagnostic; observes only, changes no
  gameplay. `BACKUP-STATUS.txt` is the candidate's own backup note.
