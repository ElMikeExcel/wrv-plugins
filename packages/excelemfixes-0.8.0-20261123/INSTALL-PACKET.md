# INSTALL PACKET — ExcelEMFixes 0.8.0: Ghast flying mount (D204)

- Filed: Nov 23, 2026 (parent seat, per D212 — build lane: ~/workspace/ghast-mount-20261123/, BUILD-RECORD.md).
- STATUS: **HELD — do NOT install until Mike's explicit go** (target ~Nov 30, 2026). The failsafe must not run this entry.
- SECRECY (Mike's instruction): NO What's New, website, or Guide surfaces with this install. The tier goes in unannounced; surfaces are a separate later decision.

## What it is
ExcelEMFixes 0.7.1 → 0.8.0. The summon-mount module gains a second mount: a flying happy ghast, the premium tier above the Mount Charm horse. Carry-and-toggle charm item (right-click summons/despawns, never consumed), ExcelItems ID 9 (`ghast_charm`). Purchase gate: player must be halfway through the quests of Primis OR any single expansion — census at build time: Primis = 105 enabled quests, threshold 53 completions (counted at runtime from EM's own quest files; generic per-package config). Prices: $30,000 list (ExcelShops) / $22,500 guild-vendor NPC (standing 25% discount, D200). Flight speed: the ghast spawns with its flying-speed attribute set from config (`ghast.flying-speed: 0.097` in the deploy config) — rig-measured cruise ≈13.6 blocks/s, slightly faster than the horse's ≈12.2 (Mike's ruling, Nov 23, 2026; see BUILD-RECORD.md speed section). All numbers are config knobs.

## Provenance gates (verify fresh at install; hashes govern)
- Live jar must be `plugins/ExcelEMFixes-0.7.1.jar` — 389,957 bytes, SHA-256 `7b3fcdede8435447c7c4dcf3aef29a62d60044dbceba6a2b4481e17c9c84e895` (installed Nov 23, 2026, hand window `hand-20261123-210408-emfixes071`). 0.8.0 was RE-BASED onto the 0.7.1 tree Nov 23, 2026 (the `emfixes reload` NPE fix is in this jar — verified at source level: the tree's ExcelEMFixes.java differs from the 0.7.1 lane's only in the VERSION constant); if the live jar differs, STOP and re-base.
- Live ExcelEMFixes config: pull fresh and diff against the lane's base before touching it. The lane's deploy config is exactly the 0.7.1 live config (SHA-256 `b1c4b791…` at filing) PLUS the `ghast:` subsection — including food-heal `debug: false` (the TP-26 flip that went live with 0.7.1). The ONLY config change in this packet is the insertion of that `ghast:` subsection from `build-root/deploy/config.yml`. Pure insertion; anything else in the diff = STOP.

## Artifacts
- Jar: `~/workspace/ghast-mount-20261123/build-root/ExcelEMFixes-0.8.0.jar` — 423,069 bytes — SHA-256 `5c9d6ac15b8eb0e49624a71f84792442fbeb911da8594090f555416788117dd7` (deterministic build, two builds byte-identical; supersedes the earlier 0.7.0-based builds `a5cb921d…` / `aa9fd230…` — do NOT install those).
- Deploy config: `~/workspace/ghast-mount-20261123/build-root/deploy/config.yml` (55,237 bytes, SHA-256 `f15c811980c82d32f20a26cb277d5449eef309a9a365e5cffcf07cf0383e68e8`) — source of the `ghast:` subsection only; do NOT wholesale-replace the live config.

## Steps (standard pack-swap window: emptiness gate ×2, snapshot first, marker discipline)
1. Snapshot (D203 naming). Rename the live 0.7.1 jar aside as the rollback (`ExcelEMFixes-0.7.1.jar.bak-ghast-080`), upload the 0.8.0 jar, verify by fresh download (size + SHA-256 exact).
2. Config: back up the live config (`config.yml.bak-0.7.1-<date>`), insert the `ghast:` subsection, verify by fresh download + diff (insertion only).
3. **ExcelItems: NO change. WARNING — do NOT add a `9: ghast_charm` line to ExcelItems' config.yml.** On ExcelItems 0.1.0 that line disables ExcelItems at its own enable (unit-proven both directions). ID 9 registers through the API at module enable.
4. Vendor NPC wiring (compose at install, mirroring the Mount Charm exactly): the guild mount vendor (shepherd stall) gains a Ghast Charm good at $22,500 whose interaction is a COMMAND interaction running the console form `ghastcharm buy %player_name% npc`.
5. ExcelShops listing (compose at install, mirroring the Mount Charm listing): Ghast Charm at $30,000 wired to the player-form buy (`ghastcharm buy` as the player).
6. Start; boot-verify (below); write INSTALLED.txt in the lane folder; convert this MANIFEST entry to its INSTALLED tombstone; append the timing ledger.

## Post-install verification
- Boot log: ExcelEMFixes 0.8.0 ready, 17 modules incl. summon-mount; the ghast tier line (registry id 9, $30000/$22500, gate on); the census line reads `ghast quest-gate census — primis=105 quests`; SEVERE 0.
- `emfixes reload` after boot: zero "reload failed" lines (the 0.7.1 fix this build is based on — acceptance per the 0.7.1 install).
- `/ghastcharm` exists. A below-gate account is refused with its real progress quoted ("52 of 105 … 53 needed" shape) and charged nothing.
- A summoned ghast's flying-speed attribute reads 0.097 (rig-proven; a live spot-check is welcome but not gating).
- Rig proof already on record (BUILD-RECORD.md): boot ON 30/0 SEVERE 0 — gate refuse at 52/105 with zero charge, pass at 53/105 charging exactly $30,000 list and $22,500 NPC, funds-checked refusal, summon via the real item event (HAPPY_GHAST, CUSTOM spawn, RED_HARNESS in the BODY slot, owner-only boarding, toggle despawn, combat mark bars both charms, CUSTOM spawn survives in em_primis while a NATURAL control is cancelled); ghastoff / modoff / ExcelItems-absent boots all SEVERE 0. Speed proof (same record): vanilla ghast measured 3.611 blocks/s, module ghast at flying-speed 0.097 measured 13.590 blocks/s; horse unchanged (attributes 0.29/0.85 read back).
- Live proof owed after install (never gates): actual flight feel/rider steering, Bedrock rendering of the ghast + harness, idle drift over time.

## Rollback
Restore the 0.7.1 jar from the `.bak-ghast-080` rename and the config from its `.bak-0.7.1-*` copy; remove the shepherd-stall Ghast Charm good and the ExcelShops Ghast Charm listing. No data migration exists; ID 9 simply stops registering.
