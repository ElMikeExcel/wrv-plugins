# ExcelBedrockMenu 1.5.22: Bedrock compass parity with the Java compass (STAGED, NOT INSTALLED)

Built Oct 26, 2026 (12:14 to 12:55 AM CDT) for Mike's parity rule (12:10 AM CDT: "always do onto one as you
would do the other"). Live stays 1.5.21 (837424dd) until Mike says go.

* Jar: staged/ExcelBedrockMenu-1.5.22.jar, sha256 1d860fbb640d188a831b7da93595ce0a08ebcd83018a63fcc31b6201e960e2ab
* What changes: CHANGELOG.md. How to install: INSTALL.md (drift guard on 837424dd, whitelist only Mike,
  dated .bak, one restart). Undo: ROLLBACK.md. Proof: TEST-RESULTS.md (224 unit ok, rig 24/24).
* Mockups: map/bedrock-before.png (live 1.5.21) and map/bedrock-after.png (1.5.22), drawn from source.
* Optional Java text fix: java-optional/ (skills_perks.yml "Magic (Sealed)" -> "Magic (Awake)", reload only).
* Source: src/ExcelBedrockMenu (build.sh), base/ebm-1.5.21-src (byte-identical rebuild of the live jar),
  ExcelBedrockMenu-1.5.21-to-1.5.22.diff.

## Decisions taken (easy to change, flag to Mike)
1. Kept Mike's 1.5.21 order and slotted each new button next to its Java group mates, rather than
   re-sorting the whole list into Java row order.
2. Removed the separate mcMMO Stats button to match Java (Mike, Oct 25: Skills & Perks is enough) and put a
   "My mcMMO stats" button inside Skills & Perks. If Mike wants it back on the main list, it is one line.
3. One "Maker's mark and Gear stats" button that opens ExcelMakersMark's own Bedrock two-switch form,
   instead of two main-menu buttons. Same two settings, current state shown, one tap to flip.
4. Ask the Guide is a text box form; Send runs /ask. The Java button stays a chat hint (Java can type).

## Small follow-ups (not in this jar)
* ExcelMenuStats bedrock-map: add "Maker's mark and Gear stats" -> the Java makers_mark item id so both
  platforms count in one row (cosmetics and ask already line up).
* Server Commands catalogue (Bedrock) and commands_menu.yml (Java) both still lack /cosmetics, /mark,
  /ask, /lostfound, /trash. Do it as one paired change.
* Full plugin parity audit: /workspace/batch-next/parity-audit-20261026.md.
