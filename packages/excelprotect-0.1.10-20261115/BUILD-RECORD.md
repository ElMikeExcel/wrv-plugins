# RESULT - P-PROTECT-WATCH-1: ExcelProtect audit watch (board supervisor run 364, worker W11)

BUILT. ExcelProtect 0.1.10 "Audit Watch" is built, tested, deterministic and
local-rig-proven. NOT installed, NOT filed to MANIFEST (packet is a draft in
this folder, per the tasking). Live server untouched.

## Base + version (explicit, per tasking)

- Base: ExcelProtect 0.1.9, tree of record
  ~/workspace/protect-i4-fix-20261114/ (src + tests + plugin.yml + config.yml
  + local-build.sh). Live-premise jar: 269,208 B / SHA-256
  893892889318ae2d5ffc08846a55b92d0b83bab4140d9a6f46d9f91f4cf6b626,
  fingerprint EXC-PROTECT-0.1.9-WRV-20261114-01-0af02b, installed Nov 14,
  2026 12:43 CST (that folder's INSTALLED.txt).
- P-PROTECT-I4-1 relationship: the 0.1.9 build IS the I4 item-break fix
  build. This watch build adds NO new I4 work and does not re-open the I4
  item; the hasMaxDamage fix rides only as base identity inside 0.1.9.
  (The backlog's P-PROTECT-I4-1 entry still reads QUEUED; its own folder's
  INSTALLED.txt + BUILD-RECORD.md show it built and installed Nov 14 -
  flagged here as a stale-state note for the supervisor, not acted on.)
- New version: 0.1.10 (one patch bump, house pattern).

## Base gate

- Unmodified 0.1.9 sources rebuilt in work/base-gate/: byte-identical -
  269,208 B / 893892889318ae2d5ffc08846a55b92d0b83bab4140d9a6f46d9f91f4cf6b626,
  EXACT match to the tree-of-record jar. Base suite 99/99.
  Evidence: evidence/base-gate-build.log.

## Candidate (structured evidence)

- Jar: ~/workspace/board-r364/protect-watch-build/ExcelProtect-0.1.10.jar
  295,195 B / SHA-256 dd78718e789492865aec621dda6f5ccefdcce57a4df95b0b4bc360c15de1be61
- Build ID: EXC-PROTECT-0.1.10-WRV-20261114-01-0d1a10 (verified inside the
  jar: excel-build.txt + plugin.yml version 0.1.10).
- Deterministic double-build: builds A and B produced the identical hash.
  Evidence: evidence/build-0110-a.log, evidence/build-0110-b.log,
  evidence/candidate-a.jar.
- Source deposit (for the records leg):
  excelprotect-0.1.10-source.zip 203,205 B / SHA-256
  470f3590121d4e45de715ba1ea1f1c4462f870c6a70b9a6e2f7c825c5ea15680
  (full 0.1.10 tree also kept at build/ in this folder).

## Tests

- Suite: 107 found / 107 successful / 0 failed, on both builds.
  99 base cases unchanged + 8 new audit-watch cases:
  auditReserveAdoptedHoldsNothing (adopted = 0 hold, Mike's Nov 12 ruling),
  auditReserveClampWalkSkipsOverCapAndFitsSmallerLater,
  auditReserveFloorBindsBelowCeiling, auditReservePercentFoldsTiersAndUnits,
  auditLogRoutesPerPlayer (two players, two files, no cross-contamination,
  no-player record dropped + counted, Bedrock name sanitized to
  _coolgirlkail), auditReportBuildsPerPlayerStory (latest-first, window,
  header skipped), auditWatchReasonMapping, gameplayEventCarriesKindAndFields.

## What the watch does (per the backlog spec + per-player law)

- Append-only audit log, daily files under plugins/ExcelProtect/audit/,
  ONE FILE PER PLAYER PER DAY (audit/<player>-<day>.jsonl) - one player's
  casts, summons and mana history in a single pull, per the Nov 14
  per-player law. Every record is ALSO emitted onto the canonical audit
  stream, so /excelprotect inspect, export and the players/ timeline
  projection keep carrying it (the player's whole story stays in one pull
  there too). Retention/gzip housekeeping extended to the audit/ folder.
- spell_cast: Protect's own reflective Magic CastEvent feed (the
  CombatCore pattern), completed casts only, with spell, spell_base,
  mana_cost (Spell.getRequiredCost().getMana()), mana_after, mana_max,
  effective_mana_max.
- summon_spawn / summon_despawn: observed through the ExcelSummonsAPI
  attribution facade, reflectively (no compile-time dependency) - spawn
  fast path (+3t/+20t attribution checks), a 5 s proximity sweep (sees
  adoptions, which register long after spawn), EntityDeathEvent
  (reason=died), EntityRemoveEvent (reason mapped from Cause: unloaded,
  removed_plugin, despawned, discarded, out_of_world, owner_quit, ...),
  plus a registry-drop reconciliation in the sweep. Reasons are the
  OBSERVED removal causes; the watch never claims "expired" it did not
  see - the bridge's own reason arrives via the emitter door.
- mana_reserve: after every observed spawn/despawn, Protect replays the
  derivation (new pure core AuditReserve) over its observed registry for
  that owner - CAST-origin holds only, the ceiling/floor clamp walk
  (Summons 0.1.6 law), percent table + ceiling + floor read from the LIVE
  ExcelSummons config at derivation time (never baked in, so the
  P-SUMMONS-TUNING-1 raised holds will flow through automatically) -
  stamped source=derived, with trigger, this summon's hold_percent,
  held_total_percent, held_total and usable_mana (the mage's live
  effective max) when Magic answers.
- Refusals, two honest grades: (a) the emitter door - new
  api/GameplayEvent + ExcelProtectAPI.gameplay(kind, fields), the house
  EconomyEvent/FightTableEvent pattern - carries summon_refusal with the
  exact reason and the same numbers shown to the player; the recorder
  half is live in this build, ExcelSummons' adoption is a FOLLOW-ON
  build (named below, not hidden); (b) observed PreCast cancellations of
  summon-family spells at MONITOR, stamped source=observed_precast with
  the derived held percent and an explicit note that the refusing plugin
  and reason are not visible to Protect.
- Read side: /excelprotect audit <player> [hours] renders the player's
  story latest-first (AuditReport). Status gains an audit-watch health
  line. Config: log.gameplay section, five toggles, all default on.
- Existing protection/rollback records are untouched, exactly as scoped.

## Rig verdict

- LOCAL RIG BOOT: PASS. Paper 26.3 (pre-patched rig directory - a fresh
  paperclip download is blocked by this VM's network, so the 0.1.0 rig's
  patched server directory was reused) + Magic 11.2.5 + ExcelSummons
  0.1.9 + candidate 0.1.10: boot Done (40.115s), SEVERE 0, enabling lines
  present verbatim - "audit watch: Magic CastEvent + PreCastEvent feeds
  hooked" and "audit watch: ON dir=plugins/ExcelProtect/audit per-player
  daily files magic=on summons=on", READY version=0.1.10, audit/ folder
  created, /excelprotect status answered version=0.1.10, clean disable.
  Evidence: evidence/rig-boot.log (86b913bc...). The log's authlib ERROR
  lines are Mojang session-service failures from the VM's blocked
  network in an offline-mode rig - pre-boot, unrelated to any plugin;
  SEVERE count is 0.
- NOT proven by this rig (no client exists in this VM): a real in-game
  cast / summon / dismiss producing audit lines end to end. That proof
  is player/mirror-class and is written into the packet draft as the
  post-install proof owed (Mike) + the mirror rig leg at filing.

## Packet draft (not filed)

- INSTALL-PACKET.md (this folder): 5,175 B / SHA-256
  16c6a6b59bb9dc30243250d9051a5ee6274da03d18b07b9eda76605a6a77dbe7,
  lint 5175 / max line 95 / non-ASCII 0 (targets met).
- RECEIPTS.md (this folder): 3,047 B / SHA-256
  bafc7b7c08b17fd6e56b64f96768a88fd5f228a306d4e71567b62c341c87567f -
  build-side facts filled, G1 slots PULL-01..05 BLANK by design for
  filing-day same-day pulls. MANIFEST.md was NOT touched.

## Same-pass surfaces owed (named, per the standing rule)

At the install pass: What's New BOTH surfaces (website + in-game
ExcelWhatsNew changelog), Guide knowledge + intents (a "where can I see
my casts / summons / mana holds" chunk), website pair, broadcaster tips
check, in-game menus check (no change expected - admin command only),
and the records publication (wrv-plugins draft PR + Drive jar/source
deposit, G2 duplicate guard first). All are named in the packet draft.

## Follow-on owed (one item, explicit)

- ExcelSummons emitter adoption: a small ExcelSummons build that fires
  GameplayEvent for summon_refusal (exact reason + player-shown
  numbers), mana_reserve (its own bookkeeping numbers), and
  spawn/despawn with the bridge's true despawn reason (expired /
  dismissed). Until it lands, refusals are covered by the observed
  PreCast sightings and reserve lines are Protect-derived - both
  labeled as such in every record. Suggested owner: board supervisor
  chain as a sibling build item next to P-SUMMONS-TUNING-1 (same plugin,
  same area; the tuning build already touches the refusal messages).

## Files in this folder

- RESULT.md (this file), INSTALL-PACKET.md, RECEIPTS.md, SHA256SUMS
- ExcelProtect-0.1.10.jar (candidate), excelprotect-0.1.10-source.zip
- build/ (full 0.1.10 build tree), work/base-gate/ (base-gate rebuild)
- evidence/ (base-gate-build.log, build-0110-a.log, build-0110-b.log,
  candidate-a.jar, rig-boot.log)
