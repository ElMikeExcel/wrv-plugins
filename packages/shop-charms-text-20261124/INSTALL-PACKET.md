# INSTALL PACKET — ExcelMenu Travel & Animals menu: add Mount Charm + Ghast Charm (TEXT — LIVE-EDIT EXEMPTION)

- Backlog ID: P-SHOP-JAVA-GUI-CHARMS-20261124. Filed Nov 24, 2026.
- Lint: 5184 B / max line 110 / non-ASCII 20 (measured pre-lint-line; the line itself is ASCII and shorter than the max)
- Type: TEXT live-edit. One menu file replaced in place + `/excelmenu reload`.
  No jar swap, no restart, no emptiness gate, no BACKUP FIRST (standing TEXT rules).

## Live-premise receipts
- All live-state claims cite `~/workspace/shop-java-charms-fix-20261124/packet/RECEIPTS.md`
  (same-day Homebase pulls, Nov 24, 2026): the live menu file's bytes/hash, the ID 7
  wiring reference, the live configs, and the mirror-proof receipts.

## Candidate
- `~/workspace/shop-java-charms-fix-20261124/packet/spawnshop_travel_animals.yml`
  — 4,993 B — SHA-256 `f4499be4b4de64afa9c961bc23822460f2f63bd12e058b7987490d8eafc4433f`
  (SHA256SUMS in the packet folder; identical copy staged at
  `~/workspace/shop-java-charms-fix-20261124/staged/spawnshop_travel_animals.yml`).
- What changed vs live (diff-verified, nothing else): header comment noting the fix;
  `size: 9` → `18` and the `type: dispenser` line removed (the live file is a full
  9-slot dispenser — no free slots existed; chest-by-omission at size 18 is the
  proven pattern of live spawnshop_cosmetics_colors.yml); slots 0–8 byte-identical;
  NEW slot 9 `mount_charm` (ECHO_SHARD, $15,000 lore + $11,250 guild-vendor line,
  clicks left+right `[console] mountcharm buy %player_name%`); NEW slot 10
  `ghast_charm` (GHAST_TEAR, $50,000 lore + $37,500 vendor line + quest-gate lore,
  clicks left+right `[console] ghastcharm buy %player_name%`). The click wiring
  mirrors the live ID 7 Strong Healing Potion entry exactly (same review-then-confirm
  buy-command pattern), so the D204 quest gate — enforced inside the ExcelEMFixes
  buy path — applies at arm AND confirm for the Ghast Charm.

## Live-file gate (STOP on mismatch)
- Live `plugins/ExcelMenu/menus/spawnshop_travel_animals.yml` must hash to
  3,377 B / `b694f26bcd4603a84aedfa126df60465385e2f87887c3540e1eb214cac4aebae`
  (receipt in RECEIPTS.md). Any other bytes = STOP, report, re-base.

## Placement steps
1. In-place safety copy: copy the live file to
   `plugins/ExcelMenu/menus/spawnshop_travel_animals.yml.bak-charms-20261124`
   in the same folder (the TEXT net; never overwrite an existing .bak of that name —
   if one exists, STOP and report).
2. Place the candidate over `plugins/ExcelMenu/menus/spawnshop_travel_animals.yml`.
3. Verify: `sha256sum` of the placed file =
   `f4499be4b4de64afa9c961bc23822460f2f63bd12e058b7987490d8eafc4433f`.
4. Run console command: `excelmenu reload`.
5. Verify in latest.log after the reload: ExcelMenu self-check line lists
   spawnshop_travel_animals among the loaded menus with NO parse error for it;
   SEVERE 0 attributable to the reload.

## Post-place verification (Iris lane, same pass)
- Bot (Mineflayer, live bot discipline) or PC-Java as MikeExcel: `/shop` →
  Travel & Animals — Mount Charm (slot 9, ECHO_SHARD, "Price: $15,000") and
  Ghast Charm (slot 10, GHAST_TEAR, "Price: $50,000" + quest-gate lore) both visible.
- Mount Charm GUI buy, fund-and-revert: fund the test profile, click Mount Charm,
  `/mountcharm confirm`, confirm exactly $15,000 charged and an echo_shard carrying
  ExcelItems ID 5 delivered; restore the profile balance to its pre-test value
  (MikeExcel baseline $0.28 if MikeExcel is used).
- Ghast Charm click as an unqualified profile: expect the gate refusal quoting
  quest progress (e.g. "0 of 105 quests in primis — 53 needed"), no charge, no item.
  (All three already proven on the mirror rig — bot/charmbot.log, 10/10 RESULT PASS;
  the live pass re-checks visibility + the Mount buy only if a live bot session is
  already running for another proof; otherwise visibility + reload log lines suffice
  and the buy path stands on the rig proof + Lane B's live command-buy proof.)

## Scope
- Changes: ONLY `plugins/ExcelMenu/menus/spawnshop_travel_animals.yml` on the live
  server (plus its in-place .bak). No config, data, jar, or earned file is touched.
  Rollback: restore the .bak over the file + `excelmenu reload`.

## Same-pass surfaces
- In-game menus: this packet IS the missing menu surface. The other two menu copies
  (DeluxeMenus gui_menus travel file + the ExcelShops jar's bundled mobile-menus)
  already carry both charms — verified in the filing (parent-backlog
  P-SHOP-JAVA-GUI-CHARMS-20261124), not re-verified here.
- What's New: both charms already have entries (2026-11-24-mount-charm-shop,
  2026-11-24-ghast-charm-shop, placed Nov 24, 2026) — nothing owed.
- Website: charms already listed (mounts lane follow-ups) — nothing owed.
- Guide: hibernated since Nov 16, 2026 — nothing owed. Broadcaster tips: n/a.

## Proof owed after install
- None player-bound. Post-place verification is Iris-lane (above). Player feedback
  in natural play is the standing feedback-watch thereafter.

## Marker on install
- `~/workspace/shop-java-charms-fix-20261124/packet/INSTALLED.txt` — written by the
  executing lane with the placed hash, the reload log line, and the post-place
  verification result.
