# MANIFEST Entry Draft — ExcelMagicGate 0.3.2 (PENDING filing)

File this entry as the 0.3.2 PENDING packet when the supervisor
files it. Status stays PENDING until the install lands. This
build did NOT edit the MANIFEST and ran NO live pulls: every
live-state claim below is marked GATE-VERIFY and must be re-based
from a fresh same-day live pull at filing/install time (G1
live-premise rule) — nothing here is asserted from disk lineage.

## ExcelMagicGate 0.3.2

- Status: PENDING
- Type: plugin (Bukkit/Paper, family server suite)
- Version: 0.3.2 (build EXC-MAGICGATE-0.3.2-WRV-20261112-01-9c032b)
- Jar: `ExcelMagicGate-0.3.2.jar`, 247078 bytes, SHA-256 `895cf91dfd28bcca0c493dc2ef69fcc903257001ea1790bb70fcae3b0a81fb68`
- Supersedes on install: the live ExcelMagicGate jar — GATE-VERIFY (identity not asserted by this build: pull the live plugin file fresh, record its filename, size and SHA-256 as the packet's live-premise receipt, and confirm it is the expected predecessor — 0.3.1, build EXC-MAGICGATE-0.3.1-WRV-20261110-01-7a0311, 243885 B, SHA-256 `454186c0dd145a57af08757d88b4de92cbd55551c5bdd71bc5677806e9bf42a2` if the 0.3.1 packet installed as filed, otherwise the amended 0.3.0 -02 jar — before swapping; any mismatch stops the swap)
- Live filename for the swap: GATE-VERIFY (the panel filename the predecessor runs under; place the candidate under that same filename per the carried convention)
- Stage packets: byte-identical to 0.2.1 / 0.3.0 / 0.3.1. Shipped config.yml byte-identical to 0.3.1 (no config merge at install; earned.yml untouched, no placement re-run)
- Depends: Magic 11.2.5, ExcelGuildGate 0.1.0, ExcelWandBars (soft)
- Build: double-built byte-identical; 293 unit tests, 0 failures
- Rig: Paper 26.3-214 + Magic 11.2.5, stage 4 — dead-key gate PROVEN (Boot C: config-mismatch grant candidate `zzdeadspell` skipped + named at WARNING, 35 valid spells granted + verified, accounting reports 1 skipped separately, zero getMode NPE lines, SEVERE 0); Mike mage-fixture replay clean (Boot B: step-up + 34-spell grant, SEVERE 0); forceAddSpell dead-key refusal probed (Boot A)
- Summary: P0 DEFENSIVE FIX (diagnosis Nov 12, 2026, §5 Fix 1). Magic's Wand.buildInventory() NPEs (SpellTemplate.getMode() on null) whenever a wand save/addSpell reaches a spell key that resolves to no SpellTemplate in Magic's registry. 0.3.2 gates ExcelMagicGate's wand inventory sync: each missing key is resolved against Magic's controller (getSpellTemplate, reflective, fail-open) before addSpell; a dead key is skipped, logged BY NAME at WARNING ("spell '<key>' … has no template in Magic — skipped, wand data/config mismatch"), and reported in the sync's completion accounting as skipped — never counted as granted — while every valid key syncs exactly as before (add-only, verify-by-re-read). The same guard sits on the addSpellQuietly seam. This ends the sync's NPE spam and names any dead grant candidate at sync time on the next join; it does NOT cure dead keys already sitting in wand data (that is the diagnosis's Fix 2 data packet), and it does not touch rank/reconcile behaviour, paths, or any config. The wand-name messages.yml rename (diagnosis §6) is a separate config packet, NOT in this build.
- Records: `records-catchup-20261103/packages/excelmagicgate-0.3.2-20261112/` (staged by this build; publication is the supervisor's leg)
- Design: diagnosis `magic-break-diagnosis-20261112/DIAGNOSIS.md` §1 + §5 Fix 1 (Mike's P0, Nov 12, 2026)
