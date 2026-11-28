# SURFACES - ExcelEMFixes v32 (test-mode move-out)

Owed in the same pass as the chained install
(parent / surfaces lane owns these; the detailed
adjustment rides the D228 drop this lane fired,
because the v31 surfaces follow-on
P-EMFIXES-V31-SURFACES-20261127 was written for
v31 and is superseded by the move-out):

1. Website (winkreyesvalley): plugin table row
   ExcelEMFixes -> v32. (Checked by the build
   lane Nov 27, 2026: no v31 surfaces had gone
   out - no v31 surfaces record exists and the
   v31 follow-on sat gated on the install; the
   row moves from its last published state
   straight to v32.)
2. GitHub release (wrv-plugins / the
   ExcelEMFixes release home, per the v30
   pattern): title "ExcelEMFixes v32" (D247
   plain version, no star), detailed changelog
   leading with the move-out: the test-mode
   module added in v31 is REMOVED in v32 and now
   lives as the standalone ExcelTestMode v1
   plugin, per Mike's Nov 27, 2026 ruling
   (ExcelEMFixes is the EliteMobs fix pack; test
   mode fixes nothing - it is a fleet testing
   tool). Note in the changelog whether a v31
   release went out (checked by this lane: NO
   v31 release or surfaces went out before the
   move-out was ordered). Tests 2,237 / 1
   pre-existing failure disclosed; rig boots
   cited. Publication claim line required in the
   close-out record (G2: pr-exists check first;
   Homebase git lane default).
3. What's New BOTH surfaces (website section +
   in-game ExcelWhatsNew changelog), same words,
   reflecting v32 + ExcelTestMode v1 - drafted
   text rides the D228 surfaces drop.
4. Guide knowledge / intents: NO CHANGE - test
   mode was never a player-facing feature.
5. Broadcaster tips: NO CHANGE.
6. In-game menus (ExcelMenu + ExcelBedrockMenu):
   NO CHANGE - /testmode is an operator console
   command, deliberately not a menu.

Records: the wrv-plugins records package for
v32 (source + jar deposit per house pattern)
rides with the release leg.
