# ExcelShops 1.3.10 revivepatch — BUILD RECORD

- Assembled at records close-out, Nov 18, 2026, from board
  run 509 W6 (patch build) and the filing-day packaging
  receipt in ~/workspace/downed-100-filing/RECEIPTS.md.
- Tree of record: ~/workspace/board-r509/
  excelshops-patch-tree/. Packaging of record:
  ~/workspace/downed-100-filing/shops-build/ (logs in this
  package under shops-build/).

## Lineage (G1)

Base is the verified live 1.3.10 tree: the packaged base
jar is byte-identical to the live jar — 267,490 B /
1d7f773399d540e30afa990d8183f9cf49f9f469fd18f329de78cd8c0e173b2b
(live pull Nov 18, 2026, Homebase route, EXACT).

## Patch scope (8 artifacts vs base)

1. MobileCatalog.java — REVIVE_POTION route; EXPECTED gains
   revive_potion; canonical command '[console] downed
   revivepotion buy %player_name%'; invariant 21 -> 22.
2. ShopUi.java — mobileSpecialReview REVIVE_POTION dispatch;
   dependency -> ExcelDowned.
3. mobile-menus/spawnshop_potions_protection.yml (bundled,
   inside the jar) — Revive at slot 5 (D112 Bedrock set).
4. mobile-menus/spawnshop.yml (bundled) — category lore +
   '3 products' -> '4 products'.
5. live-menu-patches/spawnshop_potions_protection.yml —
   live ExcelMenu format, Revive at slot 6 (D112).
6. live-menu-patches/spawnshop.yml — same category edit.
7. MobileCatalogTest.java — 22-product pins + Revive pin.
8. MobileCatalogDerivedTest.java — derived counts +1.
Tree hashes re-verified EXACT at filing (RECEIPTS.md).
Version call F-1: ship as rebuilt 1.3.10, no bump;
plugin.yml in the jar reads version: '1.3.10'.

## Packaging + tests

Two independent full runs of the corrected test script
(--scan-class-path over the tree's actual 12 test classes;
the base script's stale class list is a pre-existing base
defect, disclosed in run 509 W6): 443 tests found, 443
successful, 0 failed — BOTH runs (shops-build/run1-tests.log,
run2-tests.log in this package).
Deterministic packaging (house recipe: staged classes,
entries touched to 2026-11-18 00:00:00 UTC, sorted list,
jar --date=2026-11-18T00:00:00Z): run 1 jar and run 2 jar
BYTE-IDENTICAL — 268,988 B /
0893dd4350eb541d3c465f2aeea3d2e7e4ee97b6e345f24c00cca373daafb40a.
Entry-set vs live jar: file entries identical; the
candidate omits the live jar's 7 explicit directory
entries (cosmetic zip shape only).
