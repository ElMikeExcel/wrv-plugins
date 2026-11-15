# VERIFY — Arrow family balance fix landing (W2, board supervisor run 390, Nov 15, 2026)

Method: primary evidence only. Every hash/size below was recomputed by this worker
from the on-disk files (sha256sum / stat), not copied from summaries. Sources:
MANIFEST.md tombstone, MANIFEST-ARCHIVE.md full record, packet folder
~/workspace/board-r360/r8-arrow-balance/build/ (+ install-20261115/).

## Verdict: LANDING VERIFIED — all substantive checks PASS (2 documented nuances, §N)

## 1. MANIFEST tombstone
- Expected: one-line tombstone in ~/workspace/wrv-install-queue/MANIFEST.md
- Actual: L1081 exactly: `## INSTALLED — Arrow family balance fix (Magic spells.yml, 5 destinations) — Nov 15, 2026 — full record in MANIFEST-ARCHIVE.md`
- **PASS**

## 2. MANIFEST-ARCHIVE full record
- Expected: full entry in ~/workspace/wrv-install-queue/MANIFEST-ARCHIVE.md
- Actual: entry at archive L3671–L3681 — original PENDING block (L3671–3676) plus
  appended `**Installed Nov 15, 2026** by wrv-install-failsafe (run scheduled
  2026-11-15 09:03 CST, full-push chain, whitelist on), Homebase lane, no fallback`
  paragraph at L3679. (Archive convention: heading keeps its PENDING wording;
  the Installed paragraph is the conversion record.)
- **PASS**

## 3. Packet identity (archive record vs on-disk)
| Item | Expected (archive L3672) | Actual (recomputed) | Result |
|---|---|---|---|
| INSTALL-PACKET.md size | 5,584 B | 5,584 B | PASS |
| INSTALL-PACKET.md SHA-256 | 63110e5b0e049b7e2192458f9e0f78b9d85d818824241661dd26e2b3c9c405c5 | 63110e5b0e049b7e2192458f9e0f78b9d85d818824241661dd26e2b3c9c405c5 | PASS |
| RECEIPTS.md SHA-256 | 03215e384071914b0d283a5b2f1f09a284d530c9ec1ea7dee8f455b44b1b5cbf | 03215e384071914b0d283a5b2f1f09a284d530c9ec1ea7dee8f455b44b1b5cbf (2,989 B) | PASS |

## 4. Candidate hashes/sizes (archive + packet SHA256SUMS vs staged files on disk)
| Destination | Expected size / SHA-256 | Actual staged file | Result |
|---|---|---|---|
| live | 188,297 / 3572fc5ef5978314bc39a5261318be2c4f5eb0803eeec26f183f5ee3d5510f36 | staged/live-spells.yml identical | PASS |
| stage2 | 89,223 / 9498948ba474c1868c4ef54a4339604f19b703bc9e6ee1c9f509656de99e4389 | staged/stage2-spells.yml identical | PASS |
| stage3 | 145,696 / 123334d4ce535d9b1ec59a629ee1f6ad93da5c2cc58ee49479b2cf4294ec1406 | staged/stage3-spells.yml identical | PASS |
| stage4 | = live hash (188,297 / 3572fc5e…) | staged/stage4-spells.yml identical | PASS |
| stage5 | 188,297 / 96c61a50a75bc41d1906680bedff2b80a09ffece85be7ab97548bf36ac5a8152 | staged/stage5-spells.yml identical | PASS |

`sha256sum -c` against build/SHA256SUMS: 5/5 OK. RECEIPTS.md candidate sizes agree.

## 5. G1 gate (fresh pulls at install vs receipts) — install-20261115/gate-*.yml recomputed
| Destination | Expected (RECEIPTS G1 / SCRIPT lp-* blocks) | Actual gate file | Result |
|---|---|---|---|
| live | 188,274 / 66ebb58cabb58b30a5cc611cf2b376b8c449c4511179c60805e769d33fbe44a7 (STAGE5 receipt — re-based, see §N1) | gate-live.yml identical | PASS |
| stage2 | 89,208 / 9be7eda6444471b526c0b20b28ee38a1efa0f25a0b474ff003cd21e9d78f97a5 | gate-stage2.yml identical | PASS |
| stage3 | 145,673 / 682ab22f57b99e78b6c668e92d1fa8f43893569bf92bae34d8602fde2fb08be7 | gate-stage3.yml identical | PASS |
| stage4 | 188,274 / 75877195b4991f48dd976698fbb53dca3800633dd8230d65a13022bfa2258abf | gate-stage4.yml identical | PASS |
| stage5 | 188,274 / 66ebb58cabb58b30a5cc611cf2b376b8c449c4511179c60805e769d33fbe44a7 | gate-stage5.yml identical | PASS |

## 6. Placed + post-boot pull-backs — install-20261115/placed-*.yml, post-*.yml recomputed
| Destination | Expected placed hash | placed-* | post-* | Result |
|---|---|---|---|---|
| live | 96c61a50… (stage5 candidate, per rebase) 188,297 B | EXACT | EXACT | PASS |
| stage2 | 9498948b… 89,223 B | EXACT | EXACT | PASS |
| stage3 | 123334d4… 145,696 B | EXACT | EXACT | PASS |
| stage4 | 3572fc5e… 188,297 B | EXACT | EXACT | PASS |
| stage5 | 96c61a50… 188,297 B | EXACT | EXACT | PASS |

Pre-boot (vh-*) and post-boot (ph-*) SCRIPT-OUTPUT blocks each EXPECTED == ACTUAL
for all 5 destinations. In-place backups named in OUTCOME/archive:
spells.yml.bak-20261115-prearrowvel at all 5 destinations (server-side; not
independently pullable from this folder — stated, not verified here).

## 7. Marker file — build/INSTALLED.txt (packet-named marker path)
- Expected: present, installed=2026-11-15, all 5 destination size/hash lines
- Actual: present, **677 B / SHA-256 5d715ae4ca82ebc15fd249ea77ce6412d94880f4f4776dc6d112051628de0330**, contents verbatim:
  - `plugin=Magic (config-only, no jar)`
  - `change=Arrow family balance fix (spells.yml x5 destinations)`
  - `installed=2026-11-15`
  - `window=wrv-install-failsafe scheduled 2026-11-15 09:03 CST (full-push chain, whitelist on)`
  - `live=plugins/Magic/spells.yml 188297 96c61a50a75bc41d1906680bedff2b80a09ffece85be7ab97548bf36ac5a8152 (stage5 candidate; live base had flipped to stage5)`
  - `stage2=89223 9498948b…`, `stage3=145696 123334d4…`, `stage4=188297 3572fc5e…`, `stage5=188297 96c61a50…` (full hashes in §4 — all match candidates/placed)
- SCRIPT-OUTPUT `verify-marker` block: RESULT PASS, ACTUAL text matches file contents.
- Note: the archive PENDING block defined the marker as "post-place pull-backs of
  all 5 destinations == candidate hashes" — that definition is also satisfied (§6).
- **PASS**

## 8. Script-output PASS counts
- Expected (archive): "26 verbatim script blocks all PASS"
- Actual: install-20261115/SCRIPT-OUTPUT.txt contains 26 blocks (`--- ` count 26),
  `RESULT: PASS` ×26, `RESULT: FAIL` ×0. Breakdown: backup_cap_check 1, G1 live-pulls
  (lp-*) 5, emptiness 3 + departure 1, verify-hash pre-boot (vh-*) 5, boot-log-scan 1,
  vc-loaded/done/severe/guild 4, post-boot verify-hash (ph-*) 5, verify-marker 1.
- **PASS**

## 9. Boot / SEVERE lines — install-20261115/latest.log recomputed by grep
| Check | Expected (archive/OUTCOME) | Actual | Result |
|---|---|---|---|
| Done line | Done (23.781s) ×1 | L805 `[16:26:43] Done (23.781s)! For help, type "help"` ×1 (vc-done PASS) | PASS |
| Loaded spells | Loaded 221 spells ×1 | L778 `[Magic] Loaded 221 spells` ×1 (vc-loaded PASS) | PASS |
| SEVERE | 0 | grep count 0 in latest.log (vc-severe PASS) | PASS |
| GuildGate | "5 of 5 seals broken, guild OPEN" ×1 unchanged | L342 ×1 (vc-guild PASS) | PASS |
| Resurrect WARN | pre-existing WARN present | L759 `[Magic] Error loading class Resurrect for spell Resurrection` present | PRESENT (see §N3) |

## N. Nuances / discrepancies (none block the landing)
- **N1 — Live-destination rebase (by design, packet CAUTION):** the stage-5 flip
  landed before this install, so live gated against the packet's stage5 receipt
  (188,274 / 66ebb58c…) instead of the original live receipt (75877195…, which is
  the stage4 gate), and live received the staged STAGE5 candidate (96c61a50…),
  not the staged live candidate (3572fc5e…). Consistent across packet CAUTION,
  archive, OUTCOME, INSTALLED.txt, SCRIPT-OUTPUT lp-live/vh-live/ph-live, and the
  retained gate/placed/post files. The staged live candidate (3572fc5e…) was
  placed only at stage4, its byte-twin destination.
- **N2 — Stale figures in the packet, correctly NOT used:** packet boot-verify said
  "Loaded 167 spells" and guild "LOCKED unchanged"; actual boot reads Loaded 221
  and guild OPEN 5/5. Archive/OUTCOME flag both as stale and the SCRIPT-OUTPUT
  blocks verify against the actual values. Also the packet header's worker lint
  line (5625/84/22) does not reproduce from the on-disk packet (5,584 B; my count
  max-line 76 chars, 66 non-ASCII bytes = 22 non-ASCII chars) — the archive
  already cites the on-disk size/hash, which verify EXACT (§3). Lint only; no
  candidate or gate value is affected.
- **N3 — Not independently verifiable from retained evidence:** the claim that the
  Resurrect WARN is pre-existing ("also in 09:48 + 10:07 boots") rests on
  OUTCOME/archive prose — those boots' logs are not retained in the packet folder,
  so this worker confirms only that the WARN exists in this boot, not its history.
  Similarly, server-side .bak-20261115-prearrowvel files (§6) and proof owed
  (Jester recast set) are outside this folder's evidence; proof remains owed per
  archive — landing verification does not discharge it.
- boot-log-scan block quirk: EXPECTED `guild_locked=any`, ACTUAL `guild_locked=missing`,
  RESULT PASS — correct behavior (guild is OPEN, not locked); vc-guild confirms the
  OPEN line separately.

## Missing evidence
None for the landing itself: tombstone, archive record, packet, receipts, staged
candidates, gate pulls, placed pull-backs, post-boot pull-backs, marker, script
output, and boot log are all present and mutually consistent. Only the two
out-of-folder claims in §N3 lack retained primary evidence.
