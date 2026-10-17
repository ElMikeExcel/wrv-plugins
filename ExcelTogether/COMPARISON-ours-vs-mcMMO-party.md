# ExcelTogether party vs mcMMO party (WRV-063), Oct 16, 2026
Sources: mcMMO upstream source at commit 058279c (2.3.003-SNAPSHOT) in /workspace/mmo-test/upstream, its default party.yml and config.yml, and Big Brain. The live mcMMO jar and live party.yml/config.yml are NOT in the workspace, so the live version, MaxSize and party feature levels are UNCONFIRMED. Big Brain and the server command list confirm mcMMO is installed live and owns /party.

| | ExcelTogether party | mcMMO party |
|---|---|---|
| Purpose | Shares the +25% Jobs money and XP bonus when members are within 50 blocks | Shares mcMMO skill XP, plus party chat, teleport, item share |
| Size | 5 (config max-party-size) | MaxSize default -1 (unlimited); live value UNCONFIRMED. Bypass permission mcmmo.bypass.partylimit |
| Create | Automatic when the first invite is accepted | /party create <name> [password] by hand |
| Invite | Menu button, compass click (confirm first), or /excelparty invite. 60 second Accept/Decline popup | /party invite <name> then target types /party accept. No popup, no clickable buttons |
| Bedrock | Native forms for menu, picker, invite popup, leave confirm. Dot names handled | Typed commands only, no forms. Party names have dots stripped. Hard to use on a phone |
| Chat | None | Party chat (/p or /pc), unlocked at party level 1 by default |
| XP sharing | No sharing; each member keeps their own XP, all get +25% when close | mcMMO skill XP only. /party xpshare equal, unlocks at party level 10, near members, bonus 1.1 up to 1.5 |
| Item sharing | None | Item share modes, unlock at party level 8 |
| Teleport | None (by design) | /ptp to members, unlock at party level 2 |
| Other | Leave asks first, leader kick, leader auto handoff, 5 minute offline grace, memory only (restart clears) | Password and lock, alliances (level 5), rename, change owner, friendly fire off, auto-kick after 7 offline days, saved to disk, party levels up to 10 |
| Jobs bonus | Yes, this is its whole point | No Jobs link |

Why both: mcMMO has no invite popup, no menu and no Bedrock UI, and has nothing to do with the Jobs bonus. Ours has the friendly front end and the Jobs bonus but no chat or XP share. The link makes ours drive mcMMO's party in the background.

## mcMMO API findings (2.3.003-SNAPSHOT)
- PartyAPI (public) has NO invite or accept method. It has inParty, inSameParty, getPartyName, getMaxPartySize, isPartySystemEnabled, addToParty (joins directly, creates the party if missing; the 3 arg bypassLimit version ignores the flag) and removeFromParty.
- The invite flow lives in mcMMO internals, used by /party invite and /party accept: McMMOPlayer.setPartyInvite(Party), PartyManager.createParty(mmoPlayer, name, password), PartyManager.joinInvitedParty(mmoPlayer) (the real accept path, checks the size limit), PartyManager.isPartyFull, canInvite (locked party), removeFromParty. These are public but not a promised API, so the link calls them by reflection and turns itself off if anything is missing.
- One invite per player: a new setPartyInvite replaces the old one.
- /party works on Bedrock only as typed text. Nothing in mcMMO shows a form.

## How the link behaves (config mcmmo-party-link: true)
- Soft dependency only (plugin.yml softdepend). mcMMO absent, party system disabled, or any method missing: link is OFF, ExcelTogether works alone, nothing is shown to players.
- Our invite accepted by the rules: also creates the inviter's mcMMO party if they have none (name from their name, next free name if taken) and sets the mcMMO invite on the target.
- Invitee accepts ours: mcMMO joinInvitedParty runs, so they are in the mcMMO party too (recreated if it was disbanded meanwhile).
- Decline or expiry: the mcMMO invite we set is cleared, and an mcMMO party we created that stayed solo is removed.
- Leave, kick, ours ending, or offline too long: the player is removed from the mcMMO party, ONLY if we put them there and they are still in that same party. Anyone who joined mcMMO on their own is never touched.
- mcMMO party full (its MaxSize, below our 5), locked by a leader who is not the inviter, or the target in a different mcMMO party: the ExcelTogether invite and party still work and the player gets one plain note. Bonus is unaffected.
- mcMMO limit bigger than 5 or unlimited: ours still caps at 5.
- Not covered: someone typing /party accept themselves joins only mcMMO, not ours. mcMMO party leader changes are not mirrored. Parties made in mcMMO first are reused when the inviter is already in one (the invitee is invited into that mcMMO party).
