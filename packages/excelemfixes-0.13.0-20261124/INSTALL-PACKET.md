# INSTALL PACKET -- ExcelEMFixes 0.13.0 (skill-bonus Claim route + nudge reword)

Lane: ~/workspace/em-menu-replacement-20261124/
Build record: ~/workspace/em-menu-replacement-20261124/BUILD-RECORD.md
Design context: ~/workspace/em-menu-replacement-20261124/FEASIBILITY-DESIGN.md
Origin: Jester's report, Nov 24, 2026, 2:38 PM CT (FEEDBACK-TRACKER
row 89) + the P-SKILLNUDGE-WORDING-20261124 filing's FIX UPDATE.
This packet is the no-design-choice part of the menu work: it
re-points OUR OWN surfaces (the /emguide route and the nudge)
at the EliteMobs screens that work. It changes nothing in
EliteMobs' files and nothing about /em itself. (The bigger
menu question — flipping EliteMobs' own style switch and/or a
native skills menu — is Mike's decision in FEASIBILITY-DESIGN.md
and is NOT part of this packet.)

## What this is

1. /emguide's Skills page gains a working "Claim skill
   bonuses" button: a new clarity-menus route kind (em-menu)
   that opens EliteMobs' own skill-bonus claim screens
   directly through EliteMobs' public menu opener. The old
   route ran bare /em, whose dialog Skills page is a
   view-only text list — the exact dead end Jester reported.
   The step-by-step text is reworded to match and now also
   teaches the /elitemobs alt path honestly.
2. The skill-bonus-nudge stops pointing at the broken page:
   its click target becomes /emguide skills and its message
   leads with the Claim button and names /elitemobs alt.
3. Rider: the pack's boot ready line now prints the true
   version (the VERSION literal was stale at 0.11.0).

## Artifacts (in this folder's packet/)

- ExcelEMFixes-0.13.0.jar -- 461,532 bytes
  SHA-256 eeec91c1ea0496401544e477a2f170d46df53e80ca5c3772366c900298d65fd5
  Deterministic double-build byte-identical (build.sh in the
  lane's build-root/).
- deploy-config.yml -- 64,115 bytes
  SHA-256 a749aa7d5607c27c8fd0bb177cd0423f06f7d25bec7a27ff8fcfd5ca2892d3ea
  The live config with EXACTLY two lines changed (the nudge
  click-command and message line 2). Verified by diff
  against the gated live config: 2 lines differ, nothing else.

## Provenance gates (STOP if any fails)

- Live plugins/ExcelEMFixes-0.12.0.jar must hash
  e71620292afe95b58c90650c2d65d85f1a5da8163ecb279d6de861619be4a9a2
  (459,581 bytes -- the 0.12.0 build installed Nov 24, 2026;
  re-verified read-only by the build lane the same day).
- Live plugins/ExcelEMFixes/config.yml must hash
  0c694bfa3d0bf9165f451c797637cbfe87767882de621f6fd55139971ba25cff
  (64,032 bytes).
- Live plugins/ExcelEMFixes/ must contain NO clarity-menus.yml
  (positive listing check, spec v2.1 A6). If one has appeared,
  STOP: it overrides the bundled text, and the window must
  merge the packet's skills-page changes into it as well.
If the jar differs, STOP and re-base. If the config differs,
do NOT place deploy-config.yml: hand-edit only the two nudge
lines in the live config instead (click-command ->
"/emguide skills"; the message's second line -> the packet
config's second message line), backing the live file up first.

## Placement

1. Snapshot first (fleet standard).
2. Stop the server (verified empty, fleet standard).
3. Jar: MOVE plugins/ExcelEMFixes-0.12.0.jar aside as
   ExcelEMFixes-0.12.0.jar.bak-<install date>-pre0130
   (a move, never a copy -- two pack jars must never be
   co-resident), then place the packet jar as
   plugins/ExcelEMFixes-0.13.0.jar.
4. Config: back the live config up as
   config.yml.bak-<install date>-pre0130, then place
   deploy-config.yml as plugins/ExcelEMFixes/config.yml
   (or the two-line hand-edit above if the gate said so).
5. Start the server.

## Acceptance

- Boot log carries "ExcelEMFixes 0.13.0 ready" with 19
  modules (the ready line's version literal now matches
  plugin.yml -- spec v2.1 A11 cross-check) and the exact
  line "Clarity Menus ready — 5 pages, 10 routes."
- `emfixes reload` reports ZERO reload-failed.
- SEVERE count 0 for the boot.
- Exactly-one-ExcelEMFixes-jar census in plugins/ after the
  swap (spec v2.1 A4/A5).

## Proof (pre-install)

- Unit suites: 1,059 checks, 0 failed, two consecutive clean
  runs (new checks: the claim route's shape; loader rejects
  a route with both kinds, neither kind, or an unknown
  em-menu target). One intermittent failure of the
  pre-existing skill-nudge tie-break check observed once in
  an earlier run -- the documented pre-existing flake,
  unrelated to this change.
- Rig boot smoke (Paper 26.1.2 rig clone with the live
  EliteMobs jar): pack enables, "Clarity Menus ready —
  5 pages, 10 routes.", ready line prints 0.13.0, SEVERE 0.
- The menu opener is EliteMobs' own public static, verified
  in the live jar's bytecode (SkillBonusMenu.openWeaponSelectMenu
  is what EliteMobs' chest-style status menu calls for its
  Skills item; the activation click path behind it was also
  read: PlayerSkillSelection add/remove + SkillBonus
  onActivate/onDeactivate + the configured message).

## Proof owed after install

- Player click-through (Java): /emguide -> Skills -> Claim
  skill bonuses -> EliteMobs' skill-select screen opens ->
  pick a skill -> click an [UNLOCKED] bonus -> it activates.
  Jester is the natural prover (he reported it); his confirm
  closes FEEDBACK-TRACKER row 89's workaround half.
- Player click-through (Bedrock): Marvin taps the same path
  through the ExcelMenu door.
- Nudge: next natural nudge recipient confirms the click
  lands on /emguide Skills (or read it off the next bot-free
  join evaluation in the log with nudge debug off — player
  word is the proof).
- Upstream: the EliteMobs dialog Skills gap itself (row 89)
  is EliteMobs' defect; an upstream report naming this
  workaround is owed AFTER the live proof above, per Mike's
  Nov 24 testing bar (parent lane drafts it for Mike's eyes;
  this packet does not send anything).

## Surfaces (same pass)

See SURFACES.md in this folder: the in-game What's New TEXT
entry (id 2026-11-24-skill-bonus-claim) rides as a companion
live-edit placement; the website plugin-row bump
0.12.0 -> 0.13.0 is a parent follow-up; a Discord note to
Jester (he reported it) goes out from the parent seat after
install with the proof ask.

## Rollback

Stop the server; remove ExcelEMFixes-0.13.0.jar; move
ExcelEMFixes-0.12.0.jar.bak-<install date>-pre0130 back to
ExcelEMFixes-0.12.0.jar; restore
config.yml.bak-<install date>-pre0130 over config.yml; start.
No data migration either way: no new data files, and the
clarity text is bundled in the jar.
