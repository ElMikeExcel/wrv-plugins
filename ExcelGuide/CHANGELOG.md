# ExcelGuide changelog

## 0.3.2 (built Oct 25, 2026, CDT) - delayed answers in plain chat

STAGED ON THE BOX TEST SERVER ONLY. Not installed live. Needs Mike's go
and one restart. Contains everything in 0.3.1 and 0.3.0 below.

Why: Mike's design (Oct 25): "a player asks a question in chat. It
reads it, does the research and then watches to see if another player
answers. If another player answers and someone follows up with thanks
or something of that sort it'll bank that answer for next time that
question is asked." Before 0.3.2, plain chat never got a researched
answer: only `/ask` and "Guide, ..." reached the knowledge search, the
AI cache, approved player answers and the AI.

### Players
- **Delayed plain-chat answers (new, ON by default).** When a player
  asks a real game question in plain chat and no quick answer fits,
  the Guide stays quiet and researches it: its own learned answers,
  the curated `/ask` answers (not personal stats), the knowledge,
  the AI cache, approved player answers, and the AI last. Then it
  waits 45 seconds.
  - If another player answers in that time (or the asker says thanks
    to someone), the Guide stays out of it. Peer learning watches that
    answer as before.
  - If nobody answers, the asker gets the Guide's answer privately
    (only they see it). Nothing found: the usual "I don't know that
    one yet" line, and the question is written down.
  - "nvm" / "never mind" / "found it" from the asker cancels it. So
    does asking the same thing with `/ask` or "Guide, ..." (that door
    answers at once), leaving, or `/guide off`.
  - Never for: statements, social chat, lines that name a player,
    profanity, seal-fight spoilers, or personal stat questions (the
    stat answers keep handling those).
  - Limits: one waiting question per player, 4 server-wide, 90 s per
    player between delayed questions, plus the normal chat cooldowns.
    The AI is asked only when nobody answered (`ai-timing: late`), and
    it counts against the same AI cooldown and daily budget. If the AI
    fails, the asker gets the standard non-answer and the console gets
    the 0.3.1 WARN line (kind and status only, never the key).
- **A third player's "thanks" now counts as one agreement** for a
  player answer (`peer-learning.others-thanks-count`, on).
- **New, OFF by default: `peer-learning.auto-approve-thanked`.** On =
  the asker's thanks alone approves a player answer (no `/guide
  review`), when every filter passed and nobody disputed it. Approved
  player answers are now also reused for the next plain-chat asker
  ("A player said: ...", privately, after the delay).
- If the Guide already answered a question, the asker's later "thanks"
  is not counted for a player answer (it may be for the Guide).
- The ambient hint (0.3.1, off) is not sent while a delayed answer is
  coming.

### Console and status
- Boot line: `Delayed chat answers: ON/off (delay, AI timing, reply
  channel, how player answers are approved)`.
- `/guide status` adds a `delayed chat answers` line (waiting now;
  seen / answered / left to players / cancelled this run).
- peer.log gets `delayed-*` events (scheduled, reply-seen, held,
  posted, cancelled, dropped, skipped). Question text and reason only,
  never names or ids. `thanks-ambiguous` when a thanks follows a Guide
  answer.
- The peer-learning timer now runs every second (was every 30 s) so a
  delayed answer lands on time. It is cheap: it returns at once when
  nothing is waiting.

### Config (all optional; a missing block uses these defaults)
```
delayed-answers:
  enabled: true
  delay-seconds: 45           # 15 to 300
  ai-timing: late             # late | early
  say-dont-know: true
  reply-channel: private      # private | public (knowledge answers only)
  per-player-cooldown-seconds: 90
  max-pending: 4
  hold-on-any-reply: true
  cancel-phrases: []
peer-learning:
  auto-approve-thanked: false
  others-thanks-count: true
```
`delayed-answers.enabled: false` + `/guide reload` = 0.3.1 behavior.

### Bundled files
- Bundled config.yml: 0.3.1 plus the blocks above (no player names, no
  key). Bundled knowledge.yml unchanged from 0.3.1 (0b48d6c5...).
  A jar swap never replaces the live data-folder files.

### Tests
- New harness DelayedAnswerTest (122 checks): timing (not before 45 s,
  once), knowledge / curated / AI / cache / approved player answer
  sources, hold on a player answer or the asker's thanks, banking with
  review (default) and auto-approve-thanked, reuse for the next asker,
  AI late vs early, async AI, AI failure (non-answer, WARN without the
  key, written down), budget, say-dont-know off, never-scheduled lines,
  limits, cancel paths, kill switch, disputes, ambiguous thanks,
  third-party thanks, two questions waiting, no names in peer.log.
- Every 0.3.1 harness unchanged and passing as before.

## 0.3.1 (built Oct 25, 2026, CDT) - say when the AI fails, and help players find /ask

STAGED ON THE BOX TEST SERVER ONLY. Not installed live. Needs Mike's go
and one restart. Contains everything in 0.3.0 below.

Why: on live 0.2.3, ai.log stopped growing after Oct 20 (158 B) while
the AI was switched on. Cause: since Oct 20 almost every question was
asked in plain chat, and plain chat never reaches the AI (only `/ask`
and "Guide, ..." do). The one `/ask` (Oct 21) left no ai.log line,
and the reason cannot be read from the logs, because 0.2.3 hides AI
call failures and ai.log write failures completely. 0.3.1 makes both visible and adds an optional
pointer to `/ask`.

### Console
- **AI call failures are now logged** as one WARN line: the failure
  kind and HTTP status only (for example `HTTP (HTTP 401)`, `TIMEOUT`,
  `IO`, `RATE_LIMIT`). Never the key, never the question. At most one
  line per 10 minutes; the rest are counted.
- **ai.log write failures are now logged** (one WARN per 10 minutes,
  with the log-file setting). AI answers keep working.
- **Boot line for the AI switch:** `AI fallback: ON/off (provider,
  model, log file, ambient hint on/off)`. Never the key.
- `/guide status` now also shows `ai failed this run` (resets on
  `/guide reload`, like the other "this run" counters).

### Players
- **New, OFF by default: `ai.ambient-hint`.** When a plain-chat
  question gets no answer and the AI is on, the asker gets ONE private
  line: "I might know that one. Type /ask and your question, and I
  will look it up for you." At most once per player per
  `ai.ambient-hint-hours` (default 24). It does not call the AI and
  spends no budget. Turn it on by adding `ambient-hint: true` under
  `ai:` in the live config and running `/guide reload`.
- The bundled default cooldown line no longer has an em dash ("One
  moment, I can answer again in about N seconds."). The live config
  keeps its own text until edited by hand.

### Bundled files
- Bundled knowledge.yml is the Oct 25 wrv.duckdns.org build (sha256
  0b48d6c5..., 155 chunks). This only matters if the data-folder copy
  is missing; a jar swap never replaces the live data folder files.
- Bundled config.yml: same as 0.3.0 (no player names) plus the two
  ambient-hint keys (off).

### Tests
- New harness AiObservabilityTest (33 checks): failure WARN content
  (no key, no question), throttling, ai.log failure WARN, a throwing
  logger never breaks chat, success stays quiet, ambient hint once per
  window, per player, only with AI on, off by default.

## 0.3.0 (built Oct 24, 2026, CDT) - smarter player-taught answers

STAGED ON THE BOX TEST SERVER ONLY. Not installed live. Needs Mike's go
and one restart.

Why: 0.2.3 "learned" 65 answers from family chat and almost all of
them were chatter (a name call, "my brain is fried", answers to a
different question). On Oct 24 at 1:25 PM CDT the live learned data was
cleared (`peer-answers.yml` set to `[]`, `learned.yml` to `{}`,
backups kept as `*.bak-20261024-clear`). 0.3.0 rebuilds peer learning
so that cannot happen again.

### Player answers now have to earn their place
- **The answer must match the question.** An answer has to share a
  meaningful word with the question or name game words. Questions only
  open a listening window when they are real game questions (they name
  a game word, are not social chat like "do you wanna...", "have you
  seen...", and name no player).
- **Answers must look like answers.** At least 20 characters and 4
  words, not a question, not chit-chat or a hedge ("lol", "idk",
  "maybe", "come back", "i was..."), and from a different player than
  the asker.
- **One chat line answers one question.** A line can no longer be
  learned as the answer to several questions at once.
- **Real backing is required.** 2 or more different players must agree
  (not the asker, not the author), OR the asker confirms ("thanks",
  "that worked"). A lone "yeah" no longer counts. Any "no / actually /
  thats wrong" kills the candidate.
- **Answers expire when stale.** A waiting answer nobody asks about
  again within 14 days is dropped (`candidate-expire-days`). Approved
  answers can expire too if you set `approved-expire-days` (off by
  default).

### Owner review queue: nothing is served until approved
- New `/guide review` (admin only):
  - `/guide review` or `/guide review list`: waiting answers with ids.
  - `/guide review show <id>`: one answer in full, with why it is
    there and whether the filters would block it now.
  - `/guide review approve <id>` / `reject <id>`.
  - `/guide review approved` and `/guide review remove <id>`.
- On Bedrock, `/guide review` opens a pop-up list with Approve, Reject
  and Back buttons. Chat commands work on every client.
- Online admins get one line when a new answer is waiting.
- `auto-approve` exists but is OFF by default.

### Filters
- **Seal-fight spoiler filter** (learn-by-dying rule). Questions about
  seal-fight mechanics never open a window, and answers that describe
  fight mechanics, enemies or what to expect are never stored or
  served. Built-in phrase list plus boss words combined with mechanic
  words; `spoiler-terms` in the config adds more.
- **Name filter.** No player names in stored or served answers: every
  online player and anyone seen in chat is blocked automatically;
  `blocked-names` in the live config can add names of players who are
  offline. Names never go in the repo.
- **Profanity filter**, built in, plus `blocked-words`.
- **Re-checked at serve time.** All filters run again every time an
  approved answer is served, so a word added later also blocks older
  answers.
- The 0.2.3 privacy screens (no one's stats, no other players'
  numbers, no links) still apply.

### How learned answers look
- Approved player answers are served as **"A player said: ..."**, never
  with a player name (0.2.3 said "<name> figured this one out").
- They rank below the Guide's own answers, the knowledge base and the
  AI cache, exactly as before.
- Answers are only served privately to the asker.

### First-use tip (new player-facing piece)
- When a player gets an "A player said" answer, the Guide adds a short
  tip: these answers came from other players and were checked by the
  owner, and the website's Ask page is the official word.
- The tip repeats on every player answer until the player says OK:
  - Java: click **[OK]** in chat (runs `/guide ok`).
  - Bedrock: tap **OK, got it!** on the pop-up (sent at most once per
    session), or type `/guide ok`.
- Admin: `/guide tipreset <player>` shows the tip again.
- Only the OK flag (player UUID) is stored, in `peer-tip.yml` in the
  plugin's data folder on the server.
- Config: `peer-learning.first-use-tip: true`, `tip-file: peer-tip.yml`.

### Files and logs
- New data files (data folder only): `peer-review.yml` (the queue and
  approved answers, no player names or UUIDs) and `peer-tip.yml`.
- `peer.log` lines carry no player names or UUIDs.
- The 0.2.3 store `peer-answers.yml` is no longer read or written. It
  stays on disk as it is.
- **Empty files load cleanly.** An empty, `[]` or `{}` `peer-review.yml`,
  `peer-tip.yml` or `learned.yml` means "nothing yet" with no warning.
  (0.2.3 warned "peer file root is not a map" on the cleared `[]` file;
  0.3.0 does not read that file at all.)
- The 0.2.3 AI adjudicator for disputed answers is removed. Disputes now
  simply drop the candidate.

### Replay of the 65 old answers
The 65 answers 0.2.3 learned (plus 28 it left unresolved) were replayed
from the box copy of the old data through the 0.3.0 rules:
- 64 of 65 are rejected. 1 would reach the review queue (backed by the
  asker saying it worked). **0 would be served** without Mike's approval.
- All 28 unresolved ones are rejected.
- Top reasons: question had no game word (11), social question (10),
  chatter answer (10), question too short (8), off-topic answer (7),
  no backing (5), answer too short (5), profanity (3), one line reused
  for another question (2), disputed (2), private `/ask` (1).

### Unchanged
- `knowledge.yml`, `intents.yml` and the bundled `learned.yml` are
  byte-identical to 0.2.3. Intents, retrieval, stats answers, AI
  fallback, notices and toggles behave as in 0.2.3.
- The live `config.yml` is never replaced by a jar swap. Every new
  setting has a safe built-in default, so the 0.2.3 config works as is.

### Tests
- Core harnesses: PeerReviewTest 109/109 (gates, corroboration,
  queue, serving, decay, persistence, kill switch) and the new
  PeerTipTest 61/61 (tip repeats until OK, persistence, Bedrock pop-up
  once per session, empty `[]`/`{}` files, 0.2.3 store untouched).
  Other harnesses unchanged from 0.2.3 (see TESTS in the PR body).
- Box test server end to end, run twice: review flow, spoiler and name
  answers not queued, "A player said" serving, tip repeat and OK,
  restart persistence, rollback to 0.2.3, and forward again.
