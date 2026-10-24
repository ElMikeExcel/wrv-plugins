# ExcelMenu changelog

## 0.1.4
- Java players now get a confirm screen before a paid ExcelMenu click. This covers any item with a `bedrock: mode: modal` block and any `excelcosmetics buy` line. The screen shows a green Confirm (with the chosen option and price), a red Cancel, and a preview of the item.
- Through 0.1.3, a Java chest click ran `excelcosmetics buy` and charged right away. Bedrock already got a detail form first.
- The confirm only fires once. Only plain left or right clicks from the same player count, and it expires after 30 s. Cancel or closing the window charges nothing.
- No menu YAML changes.
