# ExcelGuide 0.3.2 (source record)

Wink Reyes Valley's in-game Guide for Paper 26.3 (Java and Bedrock via
Geyser/Floodgate). This folder records the 0.3.2 source: delayed
plain-chat answers (research quietly, wait, let players answer first,
bank a thanked player answer and reuse it right away, answer in
public chat if nobody did, with the asker's private details kept out
of public replies), on top
of 0.3.1 (AI failure logging, optional /ask hint) and 0.3.0 (peer
learning with an owner review queue, strict filters, "A player said:
..." serving with no names, a first-use tip).

- CHANGELOG.md - full change list, replay results and tests
- INSTALL.md - backups, jar swap, verify steps
- ROLLBACK.md - going back to the 0.2.3 jar
- SOURCE.md - build inputs and history
- run-tests.sh / build.sh - plain javac build and test harnesses
- rig-tests/ - Mineflayer scripts used on a local test server
  (tip, restart persistence, rollback to 0.2.3, forward again;
  0.3.2: delayed-answers.js, public-replies.js)

0.3.2 defaults (Mike, Oct 25, 2026 5:18 PM CDT): delayed replies are
public (`delayed-answers.reply-channel: public`) and a thanked player
answer is approved at once (`peer-learning.auto-approve-thanked: true`),
still behind every filter and the dispute block. For a public reply the
AI gets only the website knowledge, the public facts and the question:
no asker name, UUID or fact sheet. Any answer that names the asker, or
an AI answer that quotes one of their own numbers, goes to the asker
privately instead. See CHANGELOG.md, "Privacy in public replies".

Not in this repo: jars, `knowledge.yml` and `intents.yml` (see
SOURCE.md), live data files, and any player names or UUIDs. The UUIDs
in the tests are fixed placeholders (all 1s, 2s, 0...a, and so on).
