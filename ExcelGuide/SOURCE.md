# ExcelGuide 0.3.0 - Source & Provenance

**Ownership:** ExcelGuide is original code written for the Wink Reyes
Valley family server (the "Valley Guide" chat guide). There is no
upstream project and no forked code. It follows the Excel-family build
conventions (Paper API, Java 21, SnakeYAML, plain javac + jar build,
no shading).

**0.3.0 SUPERSEDES 0.2.3 once installed** (built Oct 24, 2026): peer
learning rebuilt with a mandatory owner review queue (`/guide review`),
strict question/answer gates, 2+ player agreement or the asker's
confirmation, spoiler/name/profanity filters re-checked at serve time,
decay, "A player said: ..." serving with no names, and a first-use tip
that repeats until OK. New core classes: PeerFilter, PeerCandidates,
PeerTip; glue: Bedrock (Geyser/Floodgate forms by reflection). The
0.2.3 store and its AI adjudicator are retired. See CHANGELOG.md.
`knowledge.yml` is generated from the website's ai-knowledge.json
(source sha256 prefix 702e7bb3) and is not kept in the repo.
`intents.yml` is unchanged from 0.2.3 and is also kept off the public
repo (it holds owner-only details); the box copy is the build input.
Both files are needed to build and to run the tests.

**0.2.3 SUPERSEDES 0.2.2 once installed** (built Oct 18, 2026): peer
learning (Variant A - live-learn with guardrails, Mike's idea and
pick; design doc PEER-LEARNING-IDEA.md). When a question goes
unanswered (either door) or is asked in ambient chat, the Guide
watches the next 5 minutes of chat; a DIFFERENT player's
answer-shaped reply becomes a candidate in the data-folder
`peer-answers.yml` (separate from ai-cache.yml and learned.yml),
guarded by PeerGuard (profanity filter, length caps, no
first-person-stat answers, no other-player numbers, asker cannot
teach their own question). Uncontested candidates are learned when
the window closes and served with attribution ("<Player> figured
this one out: ..."), ranking BELOW intents, promoted/learned
answers, confident retrieval and the AI cache, and invalidated by a
knowledge-stamp change exactly like the cache. A disagreement in
the window marks the candidate CONTESTED (never served);
adjudication runs knowledge-base → corroboration → asker
confirmation → budgeted AI weighing (the shared AiGate, a fixed
system identity), winners served as "<Player> was right: ...",
unresolved entries stay unlearned for the weekly report. New core
classes: PeerGuard, PeerAnswers, PeerLog; engine seam PeerDecider.
0.2.3 builds ON TOP of the uninstalled 0.2.2 and its install packet
(deploy-0.2.3/PACKET.md) SUPERSEDES the 0.2.2 packet - one jar, and
the same mandatory data-folder intents.yml swap.

**0.2.2 SUPERSEDES 0.2.1 once installed** (built Oct 18, 2026): two
player-facing changes from Mike's live tests the same evening.
(1) The combined claim-blocks answer: first-person claim-amount
questions ("how much land can i claim") are a stat now (new intent
my-claim-blocks, 29 intents total) serving the asker's OWN live
GriefPrevention numbers + the general rule + the /claimslist
self-check, privately; impersonal questions keep the generic
claim-amounts answer (which gained the /claimslist pointer). The GP
reflection moved verbatim from BukkitFacts into a shared GpClaims
class both layers read through. (2) Answer acknowledgments (Mike's
rule: a direct question is never met with silence): direct-door
questions blocked by a cooldown get one whispered notice quoting
the real remaining wait (RateLimiter.remainingMillis /
AiGate.cooldownRemainingMillis), and AI engagements return a
"thinking" line synchronously; ambient stays silent when blocked;
notices are config-gated (notices block, fail-soft OFF when
malformed) and self-throttled per player. 0.2.2 installs directly
over the live 0.2.1 (jar swap + data-folder intents.yml swap at a
restart window; see deploy-0.2.2/PACKET.md).

**0.2.1 SUPERSEDES 0.2.0 once installed** (built Oct 18, 2026):
0.2.0 was built and deposited but not yet installed when 0.2.1 was
built on top of it the same evening; 0.2.1 installs AFTER 0.2.0 is
live (its install packet assumes the 0.2.0 jar + AI config are in
place). Everything 0.2.0 added is included unchanged. 0.2.0 in turn
superseded 0.1.1 (built, deposited, never installed). Live remains
0.1.0 until the 0.2.0 install.

**Knowledge content:** `knowledge.yml` is GENERATED at build time from
the family's own website content (`~/workspace/site-ai/ai-knowledge.json`,
the same corpus that powers the website's Ask page) by
`~/workspace/site-ai/make-guide-knowledge.py`. The generated file
carries the source SHA-256 stamp inside itself; the converter refuses
to run if the JSON's chunk count doesn't match the yml's. No knowledge
text was written by hand or copied from anywhere else.

**Compile-time dependencies (not redistributed in the source zip):**
- Paper API 26.2 build 123 (dev bundle) - compileOnly
- SnakeYAML 2.2 (bundled with Paper at runtime)
- Gson 2.11 - compileOnly (Paper provides it at runtime); used only
  by the AI provider client for the chat-completions JSON shape
- mcMMO fork 2.3.003-WRV-XP-2 jar - compileOnly (ExperienceAPI)
- Jobs 5.2.6.3 + CMILib 1.6.0.1 jars - compileOnly (Jobs API)
- Vault 2.0.0 (VaultAPI) - compileOnly
- ExcelGuildGate 0.3.0 jar - compileOnly (GuildGateAPI)
- Magic (by NathanWolf) 11.2.5 - **not** a compile dependency: the
  Guide reads Magic state by reflection at runtime so a Magic API
  change can never break the Guide's enable.
- GriefPrevention 16.18.7 - **not** a compile dependency either: the
  AI fact sheet reads claim blocks by reflection (the same field
  pattern as ExcelLeaderboard's SoftAdapters), so the Guide builds
  and runs with or without it.

**Layout:**
- `src/com/winkreyes/excelguide/core/` - pure logic, no Bukkit
  imports: text normalization, knowledge loading, retrieval, intent
  matching, config, rate limiter, player toggles, unanswered log,
  the GuideEngine state machine, and the 0.2.0 AI layer
  (AiProvider interface, AiGate budget/cooldown, AiLog, AiPrompts
  grounding + fact-sheet assembly, FactsView), plus the 0.2.1 learn
  layer (LearnCache storage, LearnedAnswers promoted answers,
  LearnPrivacy gate), plus the 0.2.2 additions (ClaimText combined
  claim-blocks wording; the acknowledgment notices in GuideEngine,
  RateLimiter.remainingMillis, AiGate.cooldownRemainingMillis),
  plus the 0.2.3 peer layer (PeerGuard guardrails, PeerAnswers
  store, PeerLog report log, the capture/settle/adjudicate state
  machine inside GuideEngine). Unit-tested directly
  with a mock provider - no real API key exists in tests.
- `src/com/winkreyes/excelguide/` - Bukkit glue only: plugin main,
  `/ask` + `/guide` commands, Paper chat listener, BukkitStats
  (mcMMO/Jobs/Vault/playtime/GuildGate/Magic bridges), HttpAiProvider
  (java.net.http client), BukkitFacts (GriefPrevention reflection +
  stat bridges feeding the AI fact sheet), GpClaims (0.2.2: the one
  shared GriefPrevention claim-blocks read both layers use).
- Project root - plugin.yml, config.yml, intents.yml (29 curated
  intents), knowledge.yml (generated), learned.yml (0.2.1: promoted
  answers, ships empty); `build.sh` packs them into the jar root.
- `tests/...` + `run-tests.sh` - 13 test harnesses, 979 checks,
  plain-Java mains with a tiny check helper (no JUnit on the box).
  `build.sh` builds the jar reproducibly (sorted entries, fixed
  timestamps).

**Spec:** `~/workspace/excelguide/SPEC.md` (in this workspace; the
three Oct 18 decisions are recorded in its §12). Provider research:
`~/workspace/excelguide/AI-PROVIDER-RESEARCH.md`.

**0.2.0 changes (Oct 18, 2026):** an AI fallback for questions the
deterministic engine cannot answer (Mike approved the hybrid design
the same evening). The deterministic engine stays first - intents,
clarify, personal stats, confident knowledge answers are unchanged,
and personal-stat answers never route to the AI. Only a would-be
non-answer through `/ask` or the "Guide," summon may engage the AI
(ambient chat never does): the plugin retrieves the top-5 knowledge
excerpts, injects a fenced fact sheet with the ASKER's own live
numbers (GriefPrevention claim blocks, mcMMO, Jobs, balance,
playtime, Magic standing) plus public facts (seal progress, the Land
Claimed board leaders), and calls the provider async behind an 8 s
timeout, a 60 s per-player AI cooldown, and a 50/day budget. The
system prompt fences the model to the provided material with an
exact don't-know line; output is stripped of markdown/links and
capped; any failure serves the standard non-answer and logs as
today. Provider: Groq (llama-3.3-70b-versatile) per
AI-PROVIDER-RESEARCH.md; endpoint + key + model are config only. The
API key ships EMPTY in this source and in the jar - the real key
lives only in the live server's data-folder config.yml and is never
in source, deposits, git, or logs. Also new: the curated
`land-leaderboard` intent (pointer-first: compass → Server Menu →
Leaderboard). Test tally: 528 checks, 525 passing - RetrievalTest
keeps exactly the 3 pre-existing corpus-drift failures of the
73-chunk knowledge baseline (mcmmo-party winner, claims winner,
football adversarial), unchanged since 0.1.0's re-stamp.

**0.2.1 changes (Oct 18, 2026):** the learn-cache (Mike's idea: the
Guide should learn from the AI's answers so a repeat question costs
no AI usage). LEVEL 1 - CACHE: a successful AI answer is stored in
the data-folder `ai-cache.yml` against its normalized question with
the knowledge stamp at answer time; a repeat serves instantly with
no provider call and no budget spent. Entries whose stamp no longer
matches the live knowledge are stale - dropped on sight (logged to
ai.log as cache=stale-dropped) and re-asked fresh. PRIVACY GATE
(hard, tested): cached answers are served to OTHER players, so an
answer is stored only when (a) the question is not first-person
under the stat layer's own personal-token pattern (my/mine/i/me -
conservative: any "how do i..." phrasing never caches) and (b) the
answer quotes none of the asker's injected fact-sheet numbers,
including the coincidence case; numbers appearing only in public
facts do not block. Cap 500 entries (config), LRU by last hit,
write-through YAML saves, fail-soft loads. Cache serves ride the
asked doors only, respect /guide off, and stop when the AI kill
switch is off. LEVEL 2 - PROMOTION: human-reviewed answers written
into the data-folder `learned.yml` load as a deterministic source at
startup/reload (exact word-set matching, stemmed + sorted), outrank
cache + AI, never stale-expire, and serve even with the AI off;
malformed files fail soft with a console warning. `/guide status`
and `/guide learned` report learned count + cache size for the
weekly report. Test tally: 635 checks, 632 passing - the same 3
pre-existing RetrievalTest corpus-drift failures, unchanged.
Rig-verified on the scratch Paper 26.2 stack (boot, status, reload
picking up learned.yml edits). Full record: BUILD-0.2.1.md.

**0.2.3 changes (Oct 18, 2026):** peer learning, Variant A (design:
PEER-LEARNING-IDEA.md in this workspace; Mike's brief, his Variant A
pick, and his debate refinement). CAPTURE: a capture window (config
`peer-learning.capture-window-minutes`, default 5) opens when a
direct-door question ends in the don't-know non-answer or an
ambient question goes unanswered; ambient chatter is then watched
for a different player's answer. GUARDRAILS (PeerGuard, all must
pass): profanity token filter (the LearnPrivacy token approach,
plus fuck/shit/bitch/asshole/damn); shape (>=15 chars cleaned, >=3
distinct words after stemming, not question-shaped, normalized
length <= 3x the answer cap so a copied question can't pass as an
answer); personal content (first-person token + any number or stat
word; number + stat word even without first person; number +
another online player's name with the speaker exempt). PERSONAL
QUESTIONS (first-person token + stat word in the question itself)
never open a window at all. The asker cannot teach their own
question; a question holds ONE entry ever (pending, contested,
learned or unresolved) until an operator culls it. SETTLE (at
window close, driven by a 30 s glue tick plus a settle pass at the
top of every handle()): no answer -> logged peer=no-answer;
uncontested -> learned ("<Player> figured this one out",
resolution single, or confirmed when a third player backed it);
contested -> adjudication in the design's evidence order: (1) the
knowledge base, when it clears the confident bar and one side's
terms overlap the retrieved answer at least 1.5x the other's (and
a retrieved answer contradicting BOTH sides retires the question
unresolved); (2) third-player corroboration counted from window
backing, side authors excluded; (3) the asker's own in-window
backing; (4) the AI weighing both answers with the question and
the thread, charged to the SAME AiGate daily budget under a fixed
system UUID (async via the PeerDecider seam; strict ANSWER 1 /
ANSWER 2 parsing; anything else is undecided). Winners are learned
with verdict was-right; losers stay in the entry's sides for the
record. SERVING: learned peer answers rank below intents,
promoted/learned, confident retrieval and the AI cache; they serve
on both doors including ambient (a family fact, not a personal
one), carry their author's name, whisper on the ambient door like
every ambient answer, and drop on knowledge-stamp change
(peer=stale-dropped in peer.log). REPORTING: every transition is
one line in the data-folder peer.log (peer=captured / learned /
contested / unresolved / served / stale-dropped / rejected /
no-answer); the exact shapes the Monday weekly report should count
are in WEEKLY-REPORT-PEER-EXTENSION.md. Kill switch
peer-learning.enabled (default true); off = 0.2.2 behavior exactly
(proven by harness). Fail-soft everywhere: corrupt peer file =
empty store + console warning; peer logging failures never escape.
Test tally: 979 checks, 976 passing - PeerLearningTest 135/135 new
(capture matrix, every guardrail, attribution, stamp invalidation,
contested detection + all four adjudication steps with a mock
decider, self-teach refusal, privacy, kill switch, store
round-trip); the same 3 pre-existing RetrievalTest corpus-drift
failures, unchanged since 0.2.0. Full record: BUILD-0.2.3.md.
