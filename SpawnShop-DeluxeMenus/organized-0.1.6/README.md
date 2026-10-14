# Organized Spawn Shop: ExcelShops 0.1.6

This is the exact six-menu, five-category layout installed from the verified 21-product server menu. Installation status is recorded separately in `INSTALLATION.md`; source publication is a draft, not approval to deploy.

## Players

Open the compass menu → Spawn Shop, the Spawn Shop villager, or `/shop`. Choose Food, Building, Travel & Animals, Enchanting, or Potions & Protection. Each 3-by-3 menu places products first, then balance and Back. Category Back returns to the chooser; its Back returns to the server menu.

All 21 products retain the same IDs, quantities, price text, lore and purchase commands. The 18 ordinary offers still use ExcelShops quantity → review → separate final confirmation. Veil Potion, Death Protection and +Death Protection Potion retain their own fixed-one review and separate confirmation. No browsing, category or Back action purchases anything.

ExcelShops 0.1.6 routes Java quantity Back through `/shop`. Typed-entry Cancel preserves the current amount. Bedrock native Cancel/close stays closed; final-confirmation Back returns to quantity. Chest-shop navigation and `/excelshop` standalone fallback are unchanged. If DeluxeMenus is unavailable, unhandled or throws, quantity Back safely falls back to the built-in catalog after retiring the old page and quote.

## Exact layout and registrations

`candidate/gui_menus/` contains the six exact menu files. Merge `registration-additions.yml` under the existing DeluxeMenus `gui_menus` key; preserve all existing unrelated settings and registrations. Only the root has `open_command: shop` and `register_command: true`. Children expose no extra command or permission requirement.

The nine-slot DISPENSER inventory type is supported by DeluxeMenus 1.14.1 and uses the native 3-by-3 container path rather than a tiny chest that Geyser pads with unused slots. This is a source/configuration compatibility finding, not real phone rendering acceptance.

`baseline/spawnshop.yml` is the pinned product/menu checkpoint. The public `baseline/deluxemenus-config.live.yml` and generated `candidate/config.yml` are registration-only reproduction fixtures, **not full live configuration exports and not standalone deployment files**. The live main-config hash belongs in the installation record, not these fixture files.

## Reproduce and verify

With Python 3 and PyYAML installed, run:

```
python build_candidate.py
python -m unittest discover -s tests -v
```

All 19 tests pass, checking exact product text except slots, product equivalence, unique IDs and slots, both click handlers, navigation targets, registration preservation and deterministic output. The public fixture reproduces all six installed menu candidates byte-for-byte. The original candidate also passed 273 assertions using the exact Paper 26.2 build 123 YAML API; `tests/VerifyPaperYaml.java` is the checker.

ExcelShops source changes and tests are in the adjacent `WRVShops` module. The original build passed 769 Java tests and 57 Python checks, with an independent clean rebuild producing the same JAR. Four exact-Paper boots passed 1,281 synthetic-player assertions. Only `ShopUi.class` changes behavior; plugin/Maven metadata identifies 0.1.6. Details are in `WRVShops/verification/navigation-0.1.6`.

Real Java/Bedrock screens, live purchases and complete production-plugin interactions remain unverified. No new player-testing request is outstanding for this publication. Historical evidence is retained as historical, not silently promoted to a pass.

Do not use the parent directory's older 18-product flat-menu price-retuning generator to overwrite this layout. This organization release changes no authoritative ExcelShops catalog, price or data file. Revalidate any future price change against the authoritative catalog and separately reviewed potion/protection plugins.

## Historical packaging boundary

The repository's older paired ChestShop-guard packaging tool and CI package are retained for reproducibility of that earlier review stack. They include a historical flat catalog and are **not the eight-file organized-shop rollout**. Do not reintroduce ChestShop or overwrite live catalog/config/data from that archive. For this release, the reviewed installation consists only of ExcelShops 0.1.6, the six menus, and the five appended registrations in the existing main config.
