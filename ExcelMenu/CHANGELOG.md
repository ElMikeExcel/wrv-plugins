# ExcelMenu changelog

## 0.1.5
- Bedrock forms now show hex colors properly. Geyser sends form text to Bedrock untouched, and Bedrock has no hex, so a `&#RRGGBB` color used to show up as a scrambled legacy code. Each hex is now swapped for the single Bedrock code Geyser itself would choose in chat (same palette, same HSV distance), so the shop preview matches real chat. This covers titles, button labels and content.
- New `{sample}` lore marker. A lore line that starts with `{sample}` is a color sample: Java shows it as written (the marker is removed), and Bedrock keeps its real color instead of the usual darkening of pale lore on light buttons.
- Chat colors v2 menu (spawnshop_cosmetics_colors, 36 slots): 21 colors, each hex the exact RGB of a Bedrock chat code, each name in its own color, plus a sample chat line. Needs ExcelCosmetics 0.1.2. Prices unchanged.
- Build scripts take JDK / LIBS / RT / PAPI / ANNOT overrides. Defaults are unchanged.
- Tests: 517 passed, 0 failed.

## 0.1.4
- Java players now get a confirm screen before a paid ExcelMenu click. This covers any item with a `bedrock: mode: modal` block and any `excelcosmetics buy` line. The screen shows a green Confirm (with the chosen option and price), a red Cancel, and a preview of the item.
- Through 0.1.3, a Java chest click ran `excelcosmetics buy` and charged right away. Bedrock already got a detail form first.
- The confirm only fires once. Only plain left or right clicks from the same player count, and it expires after 30 s. Cancel or closing the window charges nothing.
- No menu YAML changes.
