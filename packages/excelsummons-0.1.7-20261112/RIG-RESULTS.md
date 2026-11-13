# RIG RESULTS — ExcelSummons 0.1.7 (Nov 12, 2026)

Rig: fresh copy of the 0.1.6 rig server (Paper 26.3-235 + Magic
11.2.5, JDK 25, offline mode) in `rig-0.1.7/server/` — a LOCAL VM
rig, not the PC test-server mirror (no contention with the
WandBars 0.3.17 builder) — with the plugin swapped to the
candidate ExcelSummons-0.1.7.jar (SHA-256 verified 2fc1a226…d56
before boot), the registry data file cleared, a fresh rig world,
and a new menu-focused driver (`rig-0.1.7/driver-src/`, the
0.1.6 NMS fake-player pattern). Full log: `rig-0.1.7/boot.log`.
The server reached Done, was exercised, and stopped cleanly.

## Run note (transparency)

The FIRST 0.1.7 rig run tallied 21 PASS / 1 FAIL:
`gui-rows-agree-with-list-lines` failed because the check
recomputed the model a few seconds after the GUI was filled and
the list lines' "Ns left" field had ticked (180s → ~177s) — a
check artifact, not a rendering difference. The check now
normalizes ONLY the seconds field; every other character of the
line (spell, branch, HP, hold clause) is still compared
byte-for-byte. The rig was reset to its pristine start state
(fresh world, data cleared, jars re-verified) and the FULL run
repeated. **Every number below is from that clean second run**
(first run preserved as `rig-0.1.7/boot-run1.log`).

## Boot

- "Enabling ExcelSummons v0.1.7" present.
- Enable line verbatim: "ExcelSummons 0.1.7 enabled (build
  EXC-SUMMONS-0.1.7-WRV-20261112-01-7e4a17); registry holds 0
  summon(s)."
- SEVERE: **0** in the whole boot + exercise + shutdown.
- Clean stop: "Disabling ExcelSummons v0.1.7".

## Behavior exercise — 22/22 PASS, 0 FAIL (RIGOBS tally in boot.log)

| Check | Result |
|---|---|
| platform-detect-splits-players | PASS — name-UUID player renders Java; Floodgate-prefix UUID renders Bedrock (heuristic tier) |
| model-fixture-shape | PASS — mixed fixture: 6 registered, 4 cast + 2 adopted, applied hold 55 (guardian 25 + archer 10 + wolf 10 + mount 10; adopted hold nothing) |
| menu-command-opens-java-gui | PASS — `/summons menu` through the real command path opens the Java GUI (MainMenu holder) |
| gui-rows-agree-with-list-lines | PASS — every GUI row's first lore line is the entry's list line (seconds normalized, see run note) |
| gui-adopted-button-count | PASS — slot 46 reads "Adopted pets (2)" |
| gui-info-book-held-percent | PASS — slot 48 reads "Mana held: 55% of your pool" |
| gui-main-page-has-no-adopted-rows | PASS — no main-page slot carries an adopted line |
| adopted-subpage-opens | PASS — Adopted button opens the AdoptedMenu view |
| adopted-rows-carry-indicators | PASS — both rows: list line + ADOPTED + holds no mana + tap-to-dismiss-confirm lore |
| adopted-back-returns-to-main | PASS — Back returns to MainMenu |
| adopted-row-opens-confirm-step | PASS — clicking the wolf row opens ConfirmDismissMenu for exactly that pet; button reads "Yes — remove my Wolf for good" |
| confirm-step-alone-dismisses-nothing | PASS — wolf still registered + alive with the confirm merely open |
| unconfirmed-dismiss-removes-nothing | PASS — after Keep: back on the sub-page, both adopted pets still registered |
| confirmed-dismiss-removes-exactly-one | PASS — confirmed wolf gone (record + entity); the cat beside it untouched; owner left with exactly 1 record |
| adopted-dismiss-audit-line-written | PASS — server log carries "Summons adopted dismiss by BystanderS (…): dismissed adopted wolf (pack), entity … — permanent removal of a tamed animal, confirm-gated menu flow." |
| ab-mount-scope-matches | PASS — GUI slot click vs bare `summons dismiss` on identical fixtures: both took ONLY the cast mount |
| ab-pets-scope-matches | PASS — GUI vs `summons dismiss pets`: cast wolf released on both; the ADOPTED wolf survives on both |
| ab-all-scope-matches | PASS — GUI vs `summons dismiss all`: after all three scopes both players keep exactly their 2 adopted pets — GUI == commands |
| adopted-page-one-full | PASS — 50-adopted registry: sub-page 1 renders 45 rows |
| adopted-page-two-rest | PASS — sub-page 2 renders the remaining 5 |
| bedrock-form-send-exercised | PASS (as far as the rig allows) — with no Floodgate/Geyser in the rig, BedrockForms.sendSimpleForm runs the full reflection path and honestly returns false |
| bedrock-menu-falls-back-to-chat | PASS — no Java GUI opened for the Bedrock-prefix player; the chat-list fallback ran instead of failing |

## Rig limitations, stated plainly

- **No real Bedrock client ran.** The Bedrock form SEND path was
  exercised to the point the rig allows (platform detection →
  form build → Floodgate/Geyser send attempts → honest false →
  chat fallback). A Cumulus form actually rendering on a Bedrock
  client, and its button callbacks, are NOT proven here — they
  are the post-install acceptance (Mike opens the compass page
  on Bedrock). Do not read this rig as a Bedrock client proof.
- GUI clicks were driven through SummonsMenus.handleClick — the
  exact method SummonsMenuListener delegates inventory clicks
  to (the DummyMenus pattern) — not through synthetic client
  click packets.
- Registry records were seeded directly (real spawned entities,
  real registry) rather than produced by Magic summon casts —
  the same limitation prior Summons rigs state; the paths this
  packet changes (menu rendering, dismiss routing, the adopted
  confirm flow) are the paths exercised.
