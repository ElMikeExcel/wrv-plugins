# ExcelSummons 0.1.10 - main page Back to Server Menu (BB-11)

Date: Sat Nov 14, 2026. Unit BB-11 of the back-button fix
batch (FIXBATCH-SPLIT-R348.md section 2): 2 rows, both FIX-2.
Base: the LIVE tree, ExcelSummons 0.1.9 seq-02 (build
EXC-SUMMONS-0.1.9-WRV-20261114-02-c9a114, jar 102,260 B /
ead3beec...), confirmed by a fresh same-day G1 pull and a
byte-identical base-gate rebuild before any edit.

## The defect (audit rows, re-verdicted on the live tree)

Back-button audit (Nov 13, 2026, against 0.1.8): the main
summons page (Your Summons) had no return to the Server Menu
on either platform. SummonsMenus.java and MenuModel.java are
byte-identical between the audited lineage and the live
0.1.9 seq-02 tree, so both rows are STILL BROKEN live:

- Java: fillMain set only Prev/Next, Adopted, Info and the
  three Dismiss items; the MainMenu click branch handled
  only those.
- Bedrock: the main form's buttons were only the three
  Dismiss labels plus Adopted pets; closing went nowhere.

## The fix (main page only)

- Java: new SLOT_BACK_TO_MENU = 52 (slot 49 is both the
  adopted page's Back slot and the main page's Dismiss-pets
  slot, so it cannot serve; 52 is free in every main-page
  state). fillMain places an ARROW item labelled
  MenuModel.MAIN_BACK_LABEL ("Back to Server Menu") there;
  its click closes the view and opens the Server Menu.
- Bedrock: MenuModel.mainFormButtons(page) builds the main
  form's buttons - dismiss x3, adopted door, then Back LAST
  at MAIN_BACK_INDEX = 4. Every pre-0.1.10 index is
  unchanged, and scopeForButton(4) stays null (Back is not
  a dismiss). The handler routes index 4 to the same opener.
- Opener: SummonsMenus.openServerMenu runs the fleet
  pattern player.performCommand("menu") - the opener the
  ExcelMenu spawn-shop Back and ExcelShops mobile root use -
  and sends "The Server Menu could not open." if it fails.
- ESC / form-close behaviour is unchanged (bare close).

## Out of scope (untouched, proven by tests + tree diff)

The adopted pets sub-page (already has Back on both
platforms) and the per-pet dismiss confirm (already returns
via "No - keep my pet") are not modified. No config, cap,
mana, dismiss-scope, rename or registry behaviour changes.

## Acceptance

Unit suite green incl. 2 new BackNav tests; deterministic
double-build; local rig boot PASS SEVERE 0. Post-install
proof owed: mirror walk of the main page on Java + Bedrock
(Back lands on the Server Menu) + player spot taps.
