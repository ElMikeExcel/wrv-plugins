# excelcalmbars-0.1.0-20261124 — RIG RESULTS

Rig: Paper 26.1.2 behavioral rig with a Mineflayer bot ("CalmBot") —
minecraft-data (latest published, 3.119.0) carries no 26.3 dataset, so
Mineflayer cannot speak protocol 777; the fleet's skills-menu lane hit
the same cap and proved on 26.1.2. Mitigations: (i) the bridge's
reflection targets were javap-verified field-for-field identical on
the 26.3 and 26.1.2 server jars; (ii) the plugin was also booted on a
Paper 26.3 rig with the exact live plugin set — "ExcelCalmBars 0.1.0
ready — quiet period 5s, max hold 120s, bridge ok", Done, SEVERE 0.
All other rig plugins were the LIVE jars (mcMMO, Jobs,
ExcelDeathProtection 0.2.2, ExcelItems, CMILib, Vault, EssentialsX).
Rig config: quiet-period-seconds 5, debug-logging true. Full detail:
BUILD-RECORD.md (included in this package).

Five cases, all PASS:

- **(a) Out of combat, bars arrive promptly.** Jobs Digger bar created
  ~4 s after digging (Jobs' own Batched delay; zero gate holds).
  mcMMO "Learning a new skill..." bar + updates flowed; Mining 132 XP
  on /mcstats. Repeat on the final build: bar created the same second
  as the dig, gate "boss packets seen: 4".
- **(b) In combat, XP bars held, released after the quiet period.**
  Two rounds against a tanked zombie with verified damage both ways:
  mcMMO and Jobs Hunter ADD packets held server-side, nothing reached
  the bot mid-fight; the Hunter bar was released ~5 s after the
  killing blow ended combat (the rig's 5 s quiet period). Retraction:
  a visible Digger bar was withdrawn the moment combat started
  (server "retracted", bot BOSS_DELETED), ahead of Jobs' own timer.
  Honest note: mcMMO hides its own bar ~3 s after creation even while
  held — its REMOVE passes and drops the held state, so a short-lived
  mcMMO bar gained mid-fight may never display. The XP is unaffected;
  only the pop-up is gone, which is the player's ask. Jobs bars
  (longer owner timer) survive the hold and release normally.
- **(c) Real boss bars never held.** Vanilla /bossbar bars delivered
  promptly three times, including "Test Boss Three" created DURING
  verified combat, in the same window where an mcMMO bar was held.
- **(d) ExcelDeathProtection bar never held.** Bytecode first: EDP
  0.2.2's standing bar belongs to the BASIC tier ("Inventory
  protected: 1 death"). The bot bought BASIC through the real
  purchase GUI ($15,000); across both combat rounds the count of
  BOSS_DELETED for that title is 0 — never held, never retracted.
  The title also matches no XP pattern and sits on the never-hold
  list (unit-tested).
- **(e) Clean enable/disable, no leak across relog.** Bot quit
  mid-hold and rejoined: only the four legitimate persistent bars,
  fresh gate state (held: 0). Boots with the jar: ready line + Done +
  SEVERE 0; without the jar: Done, SEVERE 0, zero ExcelCalmBars
  mentions.

Pattern unit test: 22/22 checks on the final sources ("ALL PATTERN
CHECKS PASSED") — real titles from the reporter's screenshot
identified; the EDP title, Wither/Ender Dragon, boss and quest
titles, and near-misses all rejected.

Deterministic build: two consecutive build.sh runs byte-identical.

The rig also caught two real defects before filing (see
BUILD-RECORD.md): a missing setAccessible on a package-private
reflection target (every hold silently failed open), and the
injection anchor/observability rebuild. A third fix covers join-time
sync: bars added before injection are treated as client-held state
on first contact, so combat-start retraction covers them too.
