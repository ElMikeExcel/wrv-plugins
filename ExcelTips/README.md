# ExcelTips 0.1.0 (draft, not installed)

Short first-use tips for Wink Reyes Valley (Paper 26.3, Geyser + Floodgate).
A tip shows the first few times a player uses a feature and repeats until they
say OK: a clickable [OK] on Java, a calm-moment OK pop-up on Bedrock (never in
a fight or a menu), or `/tips ok` for anyone.

- `CHANGELOG.md` everything in this release
- `INSTALL.md` / `ROLLBACK.md` live server steps (need Mike's go)
- `INTEGRATIONS.md` how ExcelTrash 0.2.0, ExcelWandBars, ExcelMakersMark,
  the 0.2.2 Spell Book and Lost & Found fit (no duplicate tips)
- `TESTS.md` test server results
- `SHA256SUMS` hash of the built jar (the jar itself is not in the repo)

Build: `./build.sh` (JDK 25, `--release 21`, Paper API on the classpath; set
`LIBS`/`RT`/`JDK` for your machine). Unit tests: `./run-tests.sh` (set `FG` to
a floodgate.jar; the private word list for the name check is passed with
`BANNED=` and is never committed).
