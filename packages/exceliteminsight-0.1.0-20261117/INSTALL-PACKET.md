# ExcelItemInsight 0.1.0 — INSTALL PACKET

Fresh plugin, no config changes to anything else. Built and
rig-verified 2026-10-20 (BUILD.md). **Built is not installed:**
this packet runs only inside a standard install window (server
empty, quiet hours, failsafe rules) as its own manifest entry.

## Artifacts (verify hashes before upload)

| File | Size | SHA-256 |
|---|---|---|
| ExcelItemInsight-0.1.0.jar | 24,137 | 5f300b4be8317ab2e93889be9c30499c23e422bad972825b2953d67add9ccf13 |

Kit on Drive ("Minecraft WRV"): jar in Build files
(1GcohtKn8Dh7lJQ4CqTOJ8Q2N1_kByy3L), source zip + SOURCE.md in Source (1fjCzjh4mmN0Mhic28TIzKA_YQCi6uM4A /
1s_Pqmp8Q4bWQmaJAjf5adCdJAH5hwiAn), MD5-verified at build time.

Preconditions: none. No Vault, no economy, no other plugin is
touched; the plugin has no dependencies and writes only item
lore blocks on enchanted items.

## Install

1. Upload `ExcelItemInsight-0.1.0.jar` to `plugins/` (panel
   File Manager; Upload menu left open, element ref without
   '@').
2. Panel Shut Down, then Start.
3. Verify in console/log:
   - `ExcelItemInsight 0.1.0 enabled — dictionary: 43 enchantments, registry coverage 43/43.`
   - zero SEVERE lines mentioning ExcelItemInsight
   - `plugins/ExcelItemInsight/config.yml` generated
4. Smoke (console): `iteminsight status`
   → expect `v0.1.0 — enabled: true — dictionary covers 43/43 enchantments.`
5. First player proof (one, at the next natural moment): hold
   any enchanted item and open your inventory — the lore gains
   the gray block under any existing lore. Jester's Sharpness V
   diamond sword should read `Total hit: 10 (base 7 + 3 from
   Sharpness)`. That proof is Mike's or a player's, one test,
   not part of the install window itself.

## Rollback

Shut Down, delete `plugins/ExcelItemInsight-0.1.0.jar` and the
`plugins/ExcelItemInsight/` folder, Start. Complete removal:
nothing else references the plugin. Blocks already written into
item lore stay on those items as plain lore text (harmless —
they no longer refresh; a future reinstall would find them by
their footer and take over again). To strip them instead,
reinstall is the cleaner path; there is no mass-strip command
in 0.1.0 by design.

## Notes for the window

- First-join effect: every player's inventory gets a catch-up
  scan on join/open, so existing enchanted gear gains blocks
  the first time each player logs in after install — no action
  needed, nothing is announced.
- Config after install: `enabled` + the header line only.
  `/iteminsight reload` (exceliteminsight.admin, default op)
  re-reads it live; changing the header is safe (old blocks are
  found by their footer and replaced).

<!-- ExcelItemInsight — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel) -->
