# ExcelSummons 0.1.8 — RIG RESULTS

Desk rig: `rig-0.1.8/` (`run-rig.sh` reproduces both phases
from the 0.1.7 rig template). Real Paper 26.3 + Magic 11.2.5,
NMS fake players with real spawned entities registered
directly in the bridge registry. One driver
(`driver-src/summonsrig/SummonsRig.java`) runs both phases;
the phase is chosen by whether the Geyser jar is present.
Fresh ExcelSummons data dir and fresh world per phase (the
registry starts empty — the enable line reads "registry holds
0 summon(s)").

**Phase A (no Geyser/Floodgate): 24/24 PASS, SEVERE 0.**
**Phase B (+ live-line Geyser + Floodgate): 30/30 PASS, SEVERE 0.**
Full logs: `rig-0.1.8/boot-a.log`, `rig-0.1.8/boot-b.log`;
tallies: `phase-a-results.txt` / `phase-b-results.txt`.

## Phase A — the 0.1.7 sequence carried + the counts

All 22 of 0.1.7's checks pass verbatim (platform tiers;
command opens the Java GUI; GUI rows agree with the list
lines; adopted button count; info book held %; no adopted
leak; sub-page + indicators + Back; confirm step opens for
exactly the clicked pet, dismisses nothing alone; Keep
removes nothing; Confirm removes exactly one, audit line
written; A/B GUI-vs-command equality over mount / pets / all
scopes; pagination 45 + 5; Bedrock send honestly false; chat
fallback). Plus the two 0.1.8 counts checks:

- `model-out-counts-fixture` — the standard fixture (cast
  guardian + archer + wolf + mount; adopted wolf + cat)
  derives OutCounts(1 mount, 1 cast pet, 2 combat, 4 total)
  and the body line reads `Out now: 1 mount · 1 cast pet ·
  2 combat summons (4 total)`.
- `gui-info-book-shows-out-counts` — the Java info book's lore
  leads with that same line, then `2 adopted pet(s)
  registered.`, then the adopted-hold-no-mana note; the name
  still reads `Mana held: 55% of your pool`.

Boot evidence: `Enabling ExcelSummons v0.1.8`; enable line
"ExcelSummons 0.1.8 enabled (build
EXC-SUMMONS-0.1.8-WRV-20261112-01-f8d6ba); registry holds
0 summon(s)."; Done in 49.980s; **SEVERE 0**.

## Phase B — the live plugin mix + THE FORM RESPONSES

Jars added: **Geyser-Spigot 2.12.1-b1292** (reports
2.12.1-SNAPSHOT; the staged jar from `~/workspace/geyser-update/`
— the same 2.12.1 line the live server runs at b1298) and
**Floodgate 2.2.5-SNAPSHOT (b141-81b65cc)** (the live build).
`geyser-and-floodgate-enabled` passes; the full phase A
sequence re-passes in the mix (24 shared checks), with the
platform tier re-proven as `platform-detect-with-floodgate-live`:
with the Floodgate API authoritative, the prefix fake is NOT
a Floodgate player, so it correctly takes the Java GUI
(`bedrock-menu-takes-java-path-with-floodgate-live`) — a real
Bedrock join IS a Floodgate player and takes the form path.

The form section (the proof 0.1.7 never had — it proved forms
SEND; 0.1.8 proves taps ARRIVE):

- `bedrock-send-honest-false-without-connection` — with Geyser
  live, sendSimpleForm to a connection-less fake player
  answers false and logs the WARNING ("no Geyser connection
  for BedrockRig…") instead of throwing or pretending. The
  no-silence requirement is visible in both boot logs.
- `bedrock-form-builds-against-live-geyser` — the production
  `BedrockForms.buildForm`, fed the real main-page contents
  for the fixture player, produces a genuine Cumulus
  `SimpleFormImpl` titled "Your Summons" with buttons exactly
  [Dismiss mount, Dismiss cast pets, Dismiss all (mount +
  combat), Adopted pets (2)].
- `bedrock-response-main-form-every-button` — synthetic
  responses driven through the ATTACHED handler via
  `FormImpl.callResultHandler` (Geyser's own FormCache
  dispatch): 0→0, 1→1, 2→2, **3→3 (the Adopted door whose tap
  died in 0.1.7)**, closed→−1.
- `bedrock-response-raw-payload-path` — the same form through
  Geyser's raw entry (`SimpleFormDefinition.handleFormResponse`
  with the client's raw strings): "0"→0 … "3"→3, "null"→−1.
- `bedrock-response-adopted-form-every-button` — [Dismiss
  Wolf, Dismiss Cat, Back to your summons]: 0→0, 1→1, 2→2
  (Back), closed→−1.
- `bedrock-response-confirm-form-buttons` — [Yes — remove my
  Wolf for good, No — keep my pet]: 0→0, **1→1 (Keep)**,
  closed→−1.

Boot evidence: same 0.1.8 enable line and build; Done in
53.399s; **SEVERE 0**.

## The honest limits (unchanged in kind from 0.1.7)

- A fake player is not a Floodgate player, so the rig cannot
  exercise a form SEND to a real Geyser connection, nor the
  one lambda hop from the delivered index into SummonsMenus'
  routing (that routing is the same code the Java click path
  and the A/B dismissal checks exercise, and the wiring shape
  is WandBars' live-proven one). What IS proven, in-server
  against the real Cumulus classes: the production build
  attaches a handler that receives every tap index on both
  of Geyser's real dispatch paths.
- A real Bedrock client's tap remains **Mike's post-install
  live proof** — the packet's Acceptance #1 states it plainly.

Excel — Wink Reyes Valley (Iris / Mike).
