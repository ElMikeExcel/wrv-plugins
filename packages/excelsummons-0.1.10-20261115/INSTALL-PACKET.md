# INSTALL PACKET - ExcelSummons 0.1.10 (BB-11 back-button fix)

Lint: 4530 B / max line 75 / non-ASCII 0
Unit: BB-11 - ExcelSummons main summons page Back fixes,
2 rows (0 FIX-1 / 2 FIX-2), per FIXBATCH-SPLIT-R348.md
section 2 BB-11 + section 3 base-gate, and the
ExcelSummons rows in back-button-audit-20261113/AUDIT.md.

## PENDING - ExcelSummons 0.1.10 (Nov 14, 2026) - GATED jar swap

- Packet: ~/workspace/back-button-wave3-r356/BB-11/packet/INSTALL-PACKET.md
- Receipts: BB-11/packet/RECEIPTS.md - 5,537 B /
  SHA-256 3e6b9816b593973b432bfb191873e36df962f956c6
  64970788e07eea5ce43413 (G1 pull, re-verdict, base
  gate, candidate + rig evidence live there, not here).

- Live-premise receipts (G1, Nov 14, 2026, Homebase):
  - Live jar is ExcelSummons 0.1.9 seq-02 - receipt:
    BB-11/pull-g1/plugins__ExcelSummons-0.1.9.jar =
    102,260 B / ead3beec553da8e37d255971c8e8e5f96a707
    835c0a8811a90df115759e6df46, pulled 18:26 CST,
    probes 3/3 PASS 18:22:17 / 18:23:01 / 18:23:48 CST.
    No fallback used; PC staging cleaned (False x3).
  - Census: exactly 1 loadable ExcelSummons jar live
    (0.1.9); older jars .bak / .held only - receipt:
    BB-11/pull-g1/plugins-top.txt (hash in RECEIPTS.md).
  - Guild LOCKED 4 of 5 - receipt:
    BB-11/pull-g1/plugins__ExcelGuildGate__state.yml
    (unlocked false, warden seal unbroken).

- Candidate: BB-11/packet/ExcelSummons-0.1.10.jar -
  102,597 B / SHA-256 97e442165b755fb7fac325e712f634
  c2607490bf7f0de0dd83b28510036ff7d0; build
  EXC-SUMMONS-0.1.10-WRV-20261114-01-bb1110; tests
  109/109 (107 carried + 2 new BackNav); deterministic
  double-build + second full run byte-identical;
  local rig boot PASS, SEVERE 0 (RECEIPTS.md).
- Live-jar gate (STOP on mismatch): live file
  plugins/ExcelSummons-0.1.9.jar = 102,260 B /
  ead3beec553da8e37d255971c8e8e5f96a707835c0a8811a90
  df115759e6df46 - from the G1 receipt above.
- Staged artifacts: BB-11/packet/ (candidate jar +
  ExcelSummons-0.1.10-sources.zip 141,825 B /
  d15a3594b08cb2fe7bd842cd1e03ce8bfbb60296ce80eae6a2
  859ccf9de9fe25, 47 files); full tree at
  BB-11/build-0.1.10/ExcelSummons/; SHA256SUMS at
  BB-11/SHA256SUMS.
- Scope: ONLY the main summons page gains Back to
  Server Menu. Java: ARROW item at slot 52
  (SLOT_BACK_TO_MENU; slot 49 is the main page's
  Dismiss-pets slot) whose click closes the view and
  runs performCommand("menu"). Bedrock: main form
  gains "Back to Server Menu" as its LAST button
  (MenuModel.mainFormButtons, MAIN_BACK_INDEX 4; all
  pre-0.1.10 indices unchanged) routed to the same
  opener, with a could-not-open message on failure.
  NOT changed: adopted pets sub-page and per-pet
  dismiss confirm (already pass, sources untouched),
  config.yml (byte-identical), caps, mana, dismiss
  scopes, rename, registry data, player data.
- Gates: marker discipline with re-checks before
  Shut Down and before Start; BACKUP FIRST + cap-at-6
  (name prunees; never the Daily newest or the two
  newest manuals); emptiness list = 0/10 twice.
  This is a jar swap - fully gated, NOT a text
  live-edit.
- Boot verify (script-named lines):
  - verify-live-jar.sh --path plugins/ExcelSummons-0.1.10.jar
    --size 102597 --sha256 97e44216...f7d0 (full hash
    above): placed jar matches the candidate EXACT.
  - counts-check on the boot log --pattern
    "EXC-SUMMONS-0.1.10-WRV-20261114-01-bb1110"
    --expected 1 --mode lines: the 0.1.10 build id in
    the enable line exactly once.
  - counts-check --pattern "Enabling ExcelSummons
    v0.1.10" --expected 1; guild LOCKED 4 of 5;
    SEVERE 0 attributable to ExcelSummons.
  Marker: BB-11/INSTALLED.txt (written on install).
- Same-pass surfaces: drafts staged in
  BB-11/packet/surfaces/ - WHATSNEW-DRAFT.md (website
  + in-game changelog texts, registry id
  2026-11-14-excelsummons-back-button, re-date at
  filing if needed) and GUIDE-NOTE-DRAFT.md (Guide:
  no new Q&A owed; optional one-liner). Tips check:
  NO CHANGE. Menus check: NO CHANGE (the ExcelMenu
  /summons entry is untouched). The supervisor places
  both What's New surfaces in the install pass.
- Proof owed after install: mirror walk - main page
  x platform: Java /summons menu, tap the slot-52
  Back, Server Menu shown; Bedrock /summons form, tap
  the last button (Back to Server Menu), Server Menu
  shown. Plus player spot taps (batch acceptance):
  Mike on Java + Kailey (.coolgirlkail) on Bedrock,
  one main-page Back tap each. A proof is only
  discharged by the tester's own report/screenshot.
- Marker on install: BB-11/INSTALLED.txt
