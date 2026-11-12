# FINGERPRINTS — ExcelDeathProtection 0.2.0

Per the house standard (`~/workspace/plugin-publishing/FINGERPRINT-STANDARD.md`):
passive provenance only — no phone-home, no telemetry, no sabotage.

## Build identity (Layer B)

- Build ID: `EXC-DPROTECT-0.2.0-WRV-20261112-01-d2b348`
  (SKU DPROTECT · version 0.2.0 · channel WRV · 2026-11-12 · seq 01 ·
  tag d2b348 = first 6 hex of SHA-256 of "EXC-DPROTECT-0.2.0-premium-credit").
  First build of the DPROTECT SKU at 0.2.0; the 0.1.3 base carried
  no build ID (Argus candidate line).
- Homes: (1) `excel-build.txt` at the jar root; (2) the `BuildInfo`
  constant, fragment-assembled in
  `src/com/winkreyes/exceldeathprotection/BuildInfo.java`;
  (3) read back at runtime by the enable line and `/dp version`.
  The standard's generated-file home does not apply — this plugin
  writes only the purchase journal, whose format is 0.1.3 record
  and was not altered.
- The build's fingerprint gate verifies home (1) in the shipped
  jar on every build; it passed for this artifact.
- Artifact identity: `ExcelDeathProtection-0.2.0.jar` — 59,390 B —
  SHA-256 ed819bb6547b5fd712e4b00d21d710bb0bae0885f23c69f9b53f493f6a230719,
  reproduced by the in-tree double-build (byte-identical).

## Semantic markers (Layer C — categories + where to look)

1. **Default config values with a signature shape:** the two-tier
   price pair 15000.0 / 30000.0 (carried from 0.1.3) paired with
   the 0.2.0 premium block defaults — per-call clamp 5 and bank
   cap 0 (uncapped) (config.yml).
2. **Default config phrasing:** the config.yml comment voice
   ("A banked premium credit is a service balance, not an item…").
3. **Schema dialect:** the player PDC key set — `active` / `tier`
   (0.1.3) plus the 0.2.0 `premium-credits` INTEGER count under
   this plugin's namespace — is how a leaked install's player
   data names this SKU.
4. **Message-string voice:** the credit/re-arm phrasing
   ("Your banked premium credit re-arms your protection…",
   "Banked premium credits: N — one re-arms automatically…")
   in ExcelDeathProtection.java.
5. **Algorithmic quirk:** a credit arriving while any tier is
   active banks in full and never overwrites; while inactive the
   first unit activates and only the remainder banks
   (PremiumCredits.applyCredit).
6. **Innocuous idiosyncrasy:** the admin revoke subcommand is
   spelled `uncredit` (not `revoke`/`remove`) in the command
   surface and tab-completion.

## Layer D

Channel WRV (our own server's build; never distributed). No other
channel variants minted for 0.2.0. Note the visible layer carries
the 0.1.3 lineage's `author: Argus` in plugin.yml, unchanged —
0.2.0 is a delta on that recovered tree, not a rebrand.
