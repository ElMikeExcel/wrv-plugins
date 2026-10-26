# ExcelWhatsNew (Wink Reyes Valley)

Server news on join, once per entry, until the player says OK. Bedrock gets a Floodgate form,
Java gets a chat box with clickable buttons. `/whatsnew` (alias `/news`) reads the news any time,
`/whatsnew list` shows the titles, `/whatsnew <n>` opens one entry. Since 0.1.1 the pages go both
ways (Newer news and Older news).

Build: `./build.sh` (JDK 25 javac --release 21 against the Paper API compile jars; deterministic
jar date). It also runs the unit checks in tests/ (TestMain).

News lives in `plugins/ExcelWhatsNew/changelog.yml`; edit it and run `/whatsnew reload` (no restart).
