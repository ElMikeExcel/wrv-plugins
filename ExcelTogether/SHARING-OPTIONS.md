# ExcelTogether 0.2.1: party bonus, sharing, indicator, status (Oct 16, 2026 5:50 PM CDT)

## Why Mike saw nothing (diagnosis; live logs cannot confirm, UNCONFIRMED)
The newest server log copy (latest (5).log, Downloads) ends at the 5:03 PM boot, so it has no party or XP lines from 5:20-5:43 PM. From the code:
1. 0.2.0 only boosts the EARNER. The +25% needs an ExcelTogether party (not an mcMMO-only party) with the partner online, same world, spectator excluded, within 50 blocks. Nothing is ever shared to the partner, so "I got no XP for being near her" was correct 0.2.0 behavior: he only earns more when HE earns.
2. If they formed the party with mcMMO's `/party` (typed), 0.2.0 does not see it (the hand-join mirror is new in 0.2.1). No ExcelTogether party = no bonus.
3. Bedrock dot names and nicknames are NOT a cause: membership is by UUID (names only for display and invite matching).
4. 0.2.0 shows no indicator at all. Jobs shows the boosted number in its own action bar, but nothing says "party bonus".
5. mcMMO skill XP is NOT touched by 0.2.0 (it only hooks Jobs JobsPrePaymentEvent). The WRV mcMMO build has its OWN party Together bonus (x1.25, config default range 32, needs an mcMMO party and permission) and its own XP share (party must be level-unlocked and leader sets mode EQUAL; default NONE). Neither needed our party.
6. Also possible: ExcelBuildMode / ExcelRewardGuard suppress earning, or Mike simply had no Jobs earn action (movement alone earns nothing).

## What 0.2.1 does now
- Earner: +25% Jobs money and XP while a party member is in range (unchanged, multiplicative with Jobs boosts such as ExcelWeekendXP, which Jobs applies after).
- NEW Jobs sharing (share-mode full, share-percent 100): each OTHER party member within radius (same world, online, not spectator, not idle) gets 100% of the earner's boosted gain, in the SAME job, only if they have that job. Paid straight through Jobs (PlayerManager.addExperience so levels work, buffered economy for money). The copy is the earner's amount BEFORE any Jobs boost; the RECEIVER's own Jobs boosts (global/schedule, permission, item) are then applied through Jobs' own boost math (PlayerManager.getFinalBonus + Boost.getFinalAmount), so everything stacks and the earner's boost is never counted twice.
- NEW mcMMO skill XP sharing (share-mcmmo-xp true): on the earner's final XP gain event, we divide the earner's own mcMMO multipliers and Together bonus back out (leaving the base, which already includes RewardGuard's kill scaling), then each active nearby party member gets base x THEIR OWN mcMMO modifiers (global /xprate such as ExcelWeekendXP's 1.2 on weekends, perks, skill modifiers) x the party multiplier, applied as a party-member gain so mcMMO does not boost it again. If mcMMO's private modifyXpGain cannot be reached (reflection), it falls back to a plain copy of the earner's final amount. Skipped for a party where mcMMO's own XP share is EQUAL, so nothing pays twice. Reflection only; off with a plain reason if mcMMO differs.
- Loop guards: JobsPrePaymentEvent is not fired by our payout; a re-entry flag blocks sharing during any payout; the mcMMO side uses the same flag and the party-member source.
- Anti-leech: receiver must be within radius, same world, online, and not idle (no block move, click or block break for share-idle-seconds, default 120; 0 = off). Earner is never affected. RewardGuard farm scaling is on the earner's gain before our copy, so copies inherit it (UNCONFIRMED against RewardGuard's code, which is not on this box).
- NEW indicator: action bar "+25% party bonus (with Kailey)" on each boosted gain (3 s throttle), receivers see "+25% party bonus: shared <Job> gain from <name>" or "shared skill XP", and one chat line per 2 minutes: "Party bonus is on ... Type /excelparty status".
- NEW `/excelparty status` (also aliases bonus, check; also shown in My party): ON with who, or OFF with why (not in a party, partner offline, other world, spectator, N blocks away needs 50, alone).

## Options not done / choices for Mike
- Share percent: `share-percent: 50` for half, `share-mode: off` for 0.2.0-style (earner-only).
- Receiver credited in the earner's job only. Alternative (not built): credit the receiver's best job. Rejected: surprising and farmable.
- Radius default 50 BLOCKS (Mike said 50 feet; confirm). mcMMO's own Together bonus range is 32 and its share range 75 (config values from the scratch copy; LIVE values UNCONFIRMED). Matching live mcMMO Together range to 50 would make the mcMMO x1.25 on the earner's own gain agree with ours (needs an edit to mcMMO config.yml, Mike's go).
- mcMMO XP share by mcMMO itself (`/party xpshare equal`) splits instead of copying and needs party level 10; we stay out when it is on.
- Decided by Mike Oct 16 6:09 PM: all boosts stack for receivers (done, see above). ExcelWeekendXP itself is an mcMMO /xprate 1.2 (plus vanilla XP) switch, not a Jobs boost, so it reaches receivers through mcMMO's own rate, and Jobs-side weekend boosts (if any are configured in Jobs) through Jobs' boost math.
- The mcMMO Together range edit (Party.Together_Bonus.Range 32 to 50) is written as an install step in INSTALL-RECORD-0.2.1.md.
- UNCONFIRMED: ExcelRewardGuard's source is not on this box; its mcMMO kill scaling happens before multipliers (per its enable log), so it is inside the base we copy. Its Jobs-side limits, if any, act on the earner's event before our HIGHEST-priority listener only if its priority is lower than HIGHEST.
