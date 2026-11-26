# RECEIPTS — ExcelEMFixes v27 packet

## Candidate artifacts (build lane, Nov 26, 2026)

- ExcelEMFixes-v27.jar — 682,311 B — SHA-256
  6c7726b450f579ea88db5626f960fe71ef543b120cac8776b49447ff5442f00e
  Deterministic: two consecutive ./build.sh runs
  produced this identical hash.
- deploy-config.yml — 101,105 B — SHA-256
  0226f22904b5d928ff49421a1671eaae21aefd6654d667d6d5bc0d504a9602f5
- SHA256SUMS in this folder matches both lines above
  (sha256sum run in the packet folder at staging).

## Base tree

Copied Nov 26, 2026 from the v26 (FIX-LIST B1) build
tree ~/workspace/excelemfixes-fetch-derivation-20261126
/build-root (plugin.yml read version: v26 at copy
time). Post-copy diff of src/ vs the base tree shows
ONLY the v27 change set (listed in BUILD-RECORD.md).

## Specimen data receipts (offline, from the re-audit
## live pull — content files, not live-server state)

- Quest file:
  quest-guidance-reaudit-20261126/live-pull/customquests
  /primis/primis_main_quest_14.yml — turnInNPC:
  primis_pontis_mille_shogun.yml; 5 KILL_CUSTOM
  objectives (Storm Temple instance bosses).
- NPC file:
  quest-guidance-reaudit-20261126/live-pull/npcs/primis
  /primis_pontis_mille_shogun.yml — name Navita;
  spawnLocation em_primis,595.5,72.0,771.7 (rounds to
  the census/FIX-LIST specimen 596, 72, 772).
- Census row (AUDIT/census.csv line 161):
  primis_main_quest_14 TURN-IN -> Navita,
  "em_primis 596,72,772", class turn-in-far, HOLE.

## Live-premise receipts

NONE TAKEN BY THIS LANE (no-live-action limit). The
filing lane takes the same-day live jar + config pulls
named in INSTALL-PACKET.md §Live-premise receipts and
appends them here before the MANIFEST entry is added.

## Live-premise receipts (G1) — filing lane, Nov 26, 2026

Taken Nov 26, 2026, 02:24 AM CT by the filing worker
(board supervisor chain) via the Homebase SSH route
(`ssh -F ~/.ssh/config mike-pc` + scp, attempt 1 PASS).
Pull folder: `packet-v27/live-pull/` in this packet.

- Live jar `plugins/ExcelEMFixes-v25.jar` — 657,130 B —
  SHA-256
  `7641a8acec174fffdfef07d6d5bd6bf9cbf49f28e73e41918b2ecbf725aaa676`
  (byte-identical to the v25 candidate in the v25
  install record, MANIFEST-ARCHIVE.md). This is the
  expected predecessor: v26 (B1) was never filed and
  never installed (no MANIFEST entry, no packet), so
  the live-jar gate reads v25 and PASSES. v27 remains
  a superset of v26 per the packet ORDERING rule.
- Live config `plugins/ExcelEMFixes/config.yml` —
  94,405 B — SHA-256
  `a34903709ba2dfb3a882a01ba17356b2bbc94b09d80411e19fbf673597e9e13e`
  (byte-identical to the v25 deploy config).

## CONFIG DIFF WARNING (read before the swap)

The staged deploy-config.yml is SRC-BUILT, not
live-derived. Diffed against the live pull above, its
ONLY intended additions are the two quest-destinations
toggles (derive-fetch-destinations: true — the v26 B1
addition — and derive-turn-in-destinations: true).
Every other value diff is a live-only WRV value the
deploy config would REGRESS if placed as-is:
- arcane-bolt ability-damage-scale: live 1.5, deploy 1.0
- progress-tracker kill-records-folder: live
  "ExcelProtect", deploy ""
- food-heal worlds: live [em_primis,
  em_adventurers_guild], deploy []
- leash debug: live true, deploy false
- summon-mount horse price/npc-price: live
  15000/11250, deploy 7500/5625
- summon-mount ghast price/npc-price: live
  50000/37500, deploy 30000/22500
- summon-mount ghast flying-speed: live 0.097,
  deploy 0.0
- summon-mount ghast gate packages: live [primis],
  deploy []
GATE FOR THE INSTALL WINDOW: do NOT place
deploy-config.yml as-is. Build the placed config from
the LIVE config + the two derive-* toggle blocks only
(the 0.9.0 merged-config precedent), and verify the
eight values above survive in the placed file.
