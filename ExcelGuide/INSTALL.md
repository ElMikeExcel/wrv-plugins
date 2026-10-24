# ExcelGuide 0.3.0 install

Needs Mike's go. One restart. Install only when nobody is online (or in
the 2 AM maintenance window). Give the reason, then `restartwarn`.

Jar: `ExcelGuide-0.3.0.jar` (not in this repo). Check its sha256
against the PR body before copying.

## Steps
1. Back up (never overwrite an existing backup; add a new suffix if the
   name is taken):
   - `plugins/ExcelGuide-0.2.3*.jar` -> keep a copy as `.bak-<date>-030`.
   - The whole data folder `plugins/ExcelGuide/` -> `plugins/ExcelGuide.bak-<date>-030/`.
2. Remove the 0.2.3 jar from `plugins/` and copy `ExcelGuide-0.3.0.jar`
   in. Only one ExcelGuide jar may be in `plugins/`.
3. Do NOT replace `plugins/ExcelGuide/config.yml` (it holds the live AI
   key). 0.3.0 runs on the 0.2.3 config with safe defaults. Optional,
   by hand on the server only:
   - Under `peer-learning:` add `review-file: peer-review.yml`,
     `min-agreements: 2`, `candidate-expire-days: 14`,
     `auto-approve: false`, `first-use-tip: true`. These equal the
     defaults, so skipping them changes nothing.
   - `blocked-names:` names of offline players that must never appear in
     answers. This list lives ONLY in the live config. Never commit it.
4. Leave `peer-answers.yml` (`[]`) and `learned.yml` (`{}`) as they are.
5. Restart (reason, then `restartwarn`).
6. After a restart, Bedrock players rejoin.

## Verify
- Console: `Valley Guide awake: ... 0 approved player answers, 0 waiting for /guide review.`
  No `[ExcelGuide]` WARN or ERROR lines, no "not a map".
- `/guide status` shows `peer learning: ON`.
- `/guide review` says 0 waiting.
- `/ask how do i claim land` still answers normally.
- Over the next days: when an answer waits, admins see
  "a player answer is waiting for review (#id)". Approve or reject
  with `/guide review`.
- Still owed on a real device: the Bedrock `/guide review` pop-up and
  the Bedrock tip OK pop-up (Java bots can't open Geyser forms).
