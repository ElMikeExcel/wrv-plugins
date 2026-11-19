# INSTALL PACKET - ExcelDowned 1.0.0 (Revive system) + ExcelShops
# 1.3.10 presentation patch + shop menus (Nov 18, 2026)
- Lint: 10452 B / max line 67 / non-ASCII 0
- Status: FILED as one gated unit under D122 (ship-it rule,
  Nov 18, 2026). Gate basis: mirror rig run 510 W5 (boots
  SEVERE 0, catalog + console legs PASS) + automated tests
  (Downed 71/71, Shops 443/443). Player-flow proofs are
  post-release per D122 and do NOT gate this packet.
- Packet: ~/workspace/downed-100-filing/INSTALL-PACKET.md
  (this file). Receipts: RECEIPTS.md in this folder (G1 pulls
  Nov 18, 2026, Homebase route, 9/9 PASS). Staged artifacts:
  staged/ in this folder; SHA256SUMS in this folder.
- Marker on install: ~/workspace/downed-100-filing/INSTALLED.txt

## What this install is
ExcelDowned 1.0.0 ships the revive system Mike designed with
Marvin's idea (Nov 17, 2026): (A) the Revive Potion - a passive
carry item sold ONLY in the Excel Shop for $5,000 (launch price);
walk up to a downed teammate, use the Revive pop-up, it consumes
1 potion and revives at 50% HP; per-player use cooldown 600 s.
(B) the Cleric ability Raise Ally - same walk-up rescue, Cleric
class active + EliteMobs level 10, real 85 GRACE via EM's own
ClassResourceController.trySpend (D107), cooldown 600 s,
success-only. (C) the shop charge leg lives in ExcelDowned
(D109): the buy verb is 'downed revivepotion buy' (the 'give'
spelling is the FREE admin mint and is never wired to a menu).
All tunables are config-editable (Mike's config rule): price,
both cooldowns, revive HP fraction, Cleric unlock, prompt
behavior. The companion ExcelShops patch is presentation-only:
it adds the REVIVE_POTION route/product (catalog invariant
21 -> 22 products) dispatching the canonical command, plus the
Revive shelf entries in both menu formats (D112 slot split:
live Java menu slot 6, jar-bundled Bedrock-validation slot 5).
One unit, one window, one boot: no intermediate state is safe
(menus without the patch break the Bedrock catalog; a menu
without Downed 1.0.0 points at a missing verb; see run-510
W6 S2.1). Internal order: Downed, then Shops, menus LAST.

## Live-premise receipts (G1) - ALL TAKEN, ALL PASS
Full table in RECEIPTS.md. Summary (Nov 18, 2026 pulls):
- Live ExcelDowned-0.1.5.jar = 63,701 B / 50d75149... (G1-1)
- Live ExcelShops-1.3.10.jar = 267,490 B / 1d7f7733... (G1-2)
- Live menu spawnshop_potions_protection.yml = 3,319 B /
  93de37b7... (G1-3); spawnshop.yml = 3,436 B / f205625a...
  (G1-4) - both EXACTLY the files the patches were built on.
- Live ExcelDowned/config.yml = 1,727 B / 8f3671e7... (G1-5);
  candidate config is a pure superset (RECEIPTS.md diff:
  one addition hunk, zero live lines changed).
- Do-not-touch premises verified: ExcelShops/config.yml
  443 B / 4eac2df5... (G1-6); spawn-catalog.yml 5,963 B /
  7f9a41cc... with zero revive_potion (G1-7).
- TEXT-leg gates: ExcelWhatsNew/changelog.yml 116,980 B /
  86c4118c..., 131 entries (G1-8); AutoBroadcaster/config.yml
  10,529 B / be71c9f1..., 37 tips (G1-9).

## Candidates (staged in this folder, staged/)
1. ExcelDowned-1.0.0.jar - 140,683 B /
   fcfa96d2036926b0eee5eaf338196ceee55680ca56e9df415563e35b1990e57c
   Build EXC-DOWNED-1.0.0-WRV-20261117-01-d0wn3d; tests 71/71;
   double-build byte-identical (repro jar staged beside it).
   Source: board-r510/f1-fix (F1 journal fix on the rig-proven
   run-509 candidate).
2. ExcelDowned-config-1.0.0.yml - 4,916 B /
   94eda94a6ff9b9c23d11936315703199fdf8aabd165283266a035d4cbc87a3bf
   Places as plugins/ExcelDowned/config.yml.
3. ExcelShops-1.3.10-revivepatch.jar - 268,988 B /
   0893dd4350eb541d3c465f2aeea3d2e7e4ee97b6e345f24c00cca373daafb40a
   Packaged at filing from the run-509 patch tree (tree hashes
   re-verified EXACT at filing); tests 443/443 in two
   independent runs; double-build byte-identical. Ships as
   rebuilt 1.3.10 (no version bump, F-1 call). Places under the
   LIVE filename plugins/ExcelShops-1.3.10.jar. The bundled
   mobile-menus ride inside it (Bedrock-validation set, slot 5).
4. spawnshop_potions_protection.yml - 3,937 B /
   c6341f4337ebbdb0076a7a3cab58e94bb1a5feebdd875d4796317c70dab21ad4
   Live ExcelMenu format; Revive at slot 6 (D112); Lost & Found
   at slot 5 untouched. Places LAST (step 6).
5. spawnshop.yml - 3,452 B /
   40c3c65c1183a93abdc8b3aca38d29e004fdd33a105caff64a86e9f4d33e3842
   Category lore + '4 products'. Places LAST (step 6).

## Live gates (STOP on mismatch; re-pull at the window)
- plugins/ExcelDowned-0.1.5.jar must equal G1-1 values.
- plugins/ExcelShops-1.3.10.jar must equal G1-2 values.
- plugins/ExcelMenu/menus/spawnshop_potions_protection.yml
  must equal G1-3; spawnshop.yml must equal G1-4.
- plugins/ExcelDowned/config.yml must equal G1-5.
Any mismatch: STOP, no swap, entry stays PENDING, report.

## Install steps (one gated window, in order)
0. Marker v2 discipline + emptiness: RCON list = 0 twice.
1. BACKUP FIRST: one Shockbyte backup for the whole window;
   cap-at-6 (prune oldest manual first; never the Daily
   newest or the two newest manuals).
2. Downed jar swap: rename plugins/ExcelDowned-0.1.5.jar to
   ExcelDowned-0.1.5.jar.bak-pre100; place candidate 1 as
   plugins/ExcelDowned-1.0.0.jar.
3. Downed config: copy live config.yml to
   config.yml.bak-pre100; place candidate 2 as
   plugins/ExcelDowned/config.yml.
4. Shops jar swap: rename plugins/ExcelShops-1.3.10.jar to
   ExcelShops-1.3.10.jar.bak-prerevive; place candidate 3 as
   plugins/ExcelShops-1.3.10.jar (same filename).
5. Marker re-check before Start (foreign marker = abort per
   protocol).
6. Menus LAST: copy each live menu file to <name>.bak-pre100;
   place candidates 4 and 5 into plugins/ExcelMenu/menus/.
7. Start (single boot for the whole unit).
8. Boot verify (below), then the same-pass surfaces (below).
Partial-window fallback (only if the window cannot take the
whole unit): steps 2+3 ONLY (Downed jar + config). Shops jar
and menus stay untouched; the entry stays PENDING for the
remainder. Never menus-first, never Shops without Downed.

## Boot verify (all must hold; else rollback)
- Boot log: ExcelDowned enable line names v1.0.0 and the
  ready line names build EXC-DOWNED-1.0.0-WRV-20261117-01-d0wn3d.
- NO 'purchase journal unavailable' WARN from ExcelDowned
  (the F1 line) on this first boot with a fresh journal.
- ExcelShops enable line + 'Economy: EssentialsX Economy';
  NO protection-only / catalog-invariant error line.
- ExcelMenu loads spawnshop and spawnshop_potions_protection
  with no menu error; guild OPEN 5 of 5 unchanged.
- SEVERE count attributable to this window = 0.
- Console spot checks: 'downed revivepotion buy' resolves as a
  command (unknown-player refusal is a PASS shape); the shop
  catalog loads 22 products.

## Rollback (written in advance; window backup is the net)
Trigger: any boot-verify failure, or a bad player report after
release (D122: the backup is the rollback mechanism).
1. Stop the server (or use the next empty moment for a
   jar-only revert window; same emptiness discipline).
2. Remove plugins/ExcelDowned-1.0.0.jar; rename
   ExcelDowned-0.1.5.jar.bak-pre100 back to
   ExcelDowned-0.1.5.jar.
3. Restore plugins/ExcelDowned/config.yml from
   config.yml.bak-pre100.
4. Remove the placed ExcelShops-1.3.10.jar; rename
   ExcelShops-1.3.10.jar.bak-prerevive back to
   ExcelShops-1.3.10.jar.
5. Restore both menu files from their .bak-pre100 copies.
6. plugins/ExcelDowned/purchases.journal is a 1.0.0-only file:
   leave it in place (0.1.5 ignores it; it is the audit trail
   of any live purchases) - do NOT delete.
7. Start; verify the 0.1.5 / 1.3.10 boot lines return, SEVERE
   0, guild OPEN 5 of 5. If file-level rollback fails for any
   reason, restore the step-1 window backup instead and roll
   the whole window back.

## Scope - what does NOT change
- Live plugins/ExcelShops/config.yml and spawn-catalog.yml:
  UNTOUCHED (premises G1-6/G1-7).
- ExcelDowned data and records: untouched; the plugin creates
  plugins/ExcelDowned/purchases.journal itself at first boot.
- ExcelMenu / ExcelBedrockMenu jars: untouched. No other menu
  file is touched. The Lost & Found signpost (live slot 5) is
  never overwritten by this packet.
- Guide files: untouched (held, see surfaces).

## Same-pass surfaces (landing pass, all named)
1. Website pair (website lane, Homebase native git): shop
   price table gains Revive Potion $5,000 on the Potions &
   Protection shelf + a Downed/revive Q&A entry in index.html;
   ai-knowledge.json regenerated. Wording truth: review first,
   then Confirm - browsing never charges; $5,000 launch price;
   shop-only; carry, do not drink; revives at half health;
   works in EliteMobs worlds; Resurrection unchanged.
2. What's New - website section: Revive launch record in the
   same publish as row 1.
3. What's New - in-game (TEXT live-edit, same pass): add the
   pinned record in whatsnew-entry.yml (this folder) to the
   registry, regenerate, place changelog.yml; gate G1-8;
   verify count 131 -> 132 and the new entry on top, wording
   compared against row 2 (two surfaces, not one).
4. Rotation tip (TEXT live-edit, same pass): append the one
   pinned tip line from whatsnew-entry.yml to
   plugins/AutoBroadcaster/config.yml interval messages;
   gate G1-9; verify 37 -> 38 tips.
5. Menus: rows are the gated steps 6 above (Java slot 6) +
   the bundled Bedrock set inside the Shops jar (slot 5);
   parity = product, price, flow.
6. Guide: HELD under hibernation (Mike, Nov 16, 2026). The
   close-out record carries this line verbatim: "Guide leg
   held under hibernation; owed on wake: Revive Potion shop
   + Downed Q&A chunks from website canon."
7. Records (close-out, pre-approved lane): ExcelDowned 1.0.0
   record package + ExcelShops patch record package as OPEN
   DRAFT PRs in wrv-plugins (pr-exists.sh guard first) + Drive
   jar/source deposits; publication claim lines in the
   close-out.

## Proof owed after install (post-release, NOT a gate - D122)
- P4 per platform (Java + Bedrock): one real purchase each
  (balance -$5,000 exactly), one Cancel (no charge), one
  insufficient-funds attempt (no charge); menu Back works.
- The bought potion is used in a live revive proof; Cleric
  Raise Ally proven live by a Cleric-active player (Marvin's
  post-install live proofs are this item, per D120/D122).
- Installed without these = released and unproven, by design;
  player feedback is the test loop now.
