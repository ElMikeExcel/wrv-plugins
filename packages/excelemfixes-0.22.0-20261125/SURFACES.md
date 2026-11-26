# SURFACES — ExcelEMFixes 0.22.0 (quest-xp fold, D244)

Player-facing behavior does not change in this release: `/questxp`,
its messages, its popup, grant amounts, target pins and backpay are
the live 1.1.0 behavior, moved inside ExcelEMFixes. Surfaces are
therefore mostly verify-no-change, with the fold itself noted where
players/owners look.

- **In-game What's New (ExcelWhatsNew changelog.yml)** — ADD one
  short entry at install: quest XP is now built into ExcelEMFixes;
  `/questxp` works exactly as before; nothing for players to do.
  Verify against the LIVE in-game file after placement (pull the
  file / count entries — never assume from the website pass).
- **Website What's New** — the matching short entry, same wording.
- **Guide (knowledge + intents)** — verify-no-change: everything the
  Guide teaches about quest XP (quests pay EliteMobs XP, `/questxp
  target`, the popup, Armor's 1/3 share) stays true word for word.
  No Guide edit needed; state the check in the install record.
- **GitHub (wrv-plugins)** — 0.22.0 release with a proper detailed
  changelog (no bare release): the fold, the migration mechanism,
  test + rig counts. Records PR stays an open draft per standing
  rule.
- **Rotation tips** — checked: no tip references ExcelEMQuestXP as a
  separate plugin; no change.
- **In-game menus (ExcelMenu + ExcelBedrockMenu)** — checked:
  `/questxp` is not a menu entry (it is a command + its own popup);
  no menu change. The quest-xp popup itself ships inside the jar
  (Java chest GUI + Bedrock form via the pack's shared renderer).
- **README + config comments** — shipped IN the build (the quest-xp
  README section and the `modules.quest-xp` config REASON block
  carry D136 + D159 + D244), not an install-pass surface.
