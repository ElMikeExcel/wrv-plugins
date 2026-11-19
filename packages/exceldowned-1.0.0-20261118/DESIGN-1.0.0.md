# ExcelDowned 1.0.0 — Revive Potion (design seed + build notes)

Seed: ~/workspace/board-r506/revive/A-DOWNED-FEATURE-DESIGN.md (spec A,
§9 build plan) + C-SHOP-SURFACES-PLAN.md (Track-2 shop seam), defaults
adopted under DECIDE-AND-LOG D106.

## SUPERSESSIONS — Mike, Nov 17, 2026, 7:46–7:56 PM CST (daily log)
These OVERRIDE the run-506 specs / D106 where they differ:
1. VERSION: ships as ExcelDowned **1.0.0**, not 0.1.6 (7:56 PM) —
   the revive system is the plugin's first full release.
2. POTION COOLDOWN: **10 minutes per player** after a successful
   potion revive, on top of consuming the potion (7:47 PM) —
   supersedes spec A's 2 s anti-double-click guard. SEPARATE from
   the Cleric ability cooldown: parallel, each can fire once
   back-to-back.
3. CLERIC COOLDOWN: **10 minutes (600 s)**, not the spec's 120 s
   (7:46–7:47 PM). Piece B's build; the shared keys live in this
   config under `cleric-revive:`.
4. REVIVE HP: 50% — unchanged, confirmed (`revive-health-fraction`).
5. CONFIG RULE (hard requirement, 7:48 PM): EVERY tunable is
   config-editable, no hardcoding — potion price ($5,000), potion
   cooldown (600 s), Cleric cooldown (600 s), revive HP (0.5),
   prompt/pop-up behavior (prompt-enabled, ranges), Cleric unlock
   point (unlock-em-level: 10) all live in config.yml.

## What 1.0.0 adds (over 0.1.5)
- RevivePotionItem: PDC-tagged Material.POTION mint factory
  (exceldowned:revive_potion BYTE=1 + lore-version int; tag is the
  only identity), storage+offhand count scan, consume-exactly-one.
- RevivePotionPolicy: pure policy — prompt/confirm verdicts
  (party-only, self excluded, ranges, ticket, 10-min cooldown),
  ordering invariant (consume iff revive landed), hint throttle,
  give/shop-buy routing (AdminPolicy gate shape, denial-first).
- RevivePotionUi: walk-up prompt (1 s tick on the state task),
  PlayerInteractEntityEvent → confirm UI in the GiveUpUi pattern
  (Java menu via RevivePotionHolder, Bedrock ModalForm via
  BedrockForms, chat arm/confirm fallback), drink-block, ticket
  lifecycle + invalidation on every session exit.
- RevivePotionHolder: slot→action truth for the Java menu
  (slot 2 CONFIRM, slot 4 picture, slot 6 Back).
- ExcelDowned: reviveByPotion (third reviveInternal shape; record
  type potion_revive, method potion, potions_remaining), the mint
  seam giveRevivePotions (record potion_mint), revive-potion config.
- DownedCommands: /downed revivepotion give <player> [amount]
  (admin-gated mint — the console command piece C's shop slot 5
  calls), /downed revive buy <player> [amount] alias (piece C's
  placeholder dispatch shape), /downed revivepotion confirm (chat
  fallback), status line "Revive Potions: <x>".
- Money: Downed takes NO money (spec A §8). The $5,000 charge is
  the Excel Shop leg; config revive-potion.price carries the
  launch price for display/hint text.

## Cleric "Raise Ally" — BUILT into this tree (run 508 W5)
Landed after the run-507 GRACE spike (D107: SPEND ROUTE EXISTS) under
Mike's amended baseline D108 (cooldown 600 s success-only, revive
50% HP via the shared revive path, EM level 10 / Cleric root unlock,
20-block range, every tunable config-editable):
- ClericGate: the EM seam (house types only); EmClericGate is the
  ONLY EM-importing class — public-static reads (lineage root,
  GRACE snapshot, ClassIdentity level) + the D107 spend:
  AdvancedCombatModule.get() -> reflective "resources" field ->
  ClassResourceController.trySpend, all Throwable-guarded behind
  the READY / NOT_INITIALIZED / UNAVAILABLE probe.
- ClericRevivePolicy: pure gate order (design B section 5.1),
  aimed-then-nearest picking, success-only charge/cooldown.
- ClericRevive: the cast path — revive via reviveByCleric (the
  potion build's exactly-once reviveInternal path, cleric_revive
  record, source "cleric-ability" per D-B8), GRACE charged only
  after the revive lands, cooldown persisted in cleric-revive.yml.
- /raiseally (alias /raise) command door; EliteMobs softdepend.
- Compile pins the EliteMobs 10.9.14 jar by SHA-256 in
  local-build.sh (bridge discipline).
- Still owed pre-filing: desk-rig runtime GRACE proof R1 (snapshot
  -> trySpend -> snapshot = -85.0) and design B proofs R2–R9.

## Not in this build
- Shop menu files / surfaces (piece C ship chain).
- Rig R1–R7 + live proofs P1–P4 (post-build lanes, spec A §11).
