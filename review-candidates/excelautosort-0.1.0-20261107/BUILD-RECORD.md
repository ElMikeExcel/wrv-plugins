# BUILD-RECORD — ExcelAutoSort 0.1.0

Built Nov 6–7, 2026 (Iris, subagent session for the Minecraft side
chat) to `SPEC.md` as amended. **Not installed.** Filed PENDING in
`~/workspace/wrv-install-queue/MANIFEST.md`; the swap packet is
`INSTALL-PACKET.md` in this folder.

## Artifacts

| Artifact | Size | SHA-256 |
|---|---|---|
| `ExcelAutoSort-0.1.0.jar` | 104,136 B | `00d01234b0a7d06b5c01dce3805f9f5736f0cf2f026512234e29c29c868dc36d` |
| `ExcelAutoSort-0.1.0-source.zip` | 61,659 B | `4081e1d1cf0f2ea97be121a6b38731f26263c4062d516e7439f74393f4d6d6d7` |

- Deterministic double-build byte-identical (`local-build.sh`:
  YAML validation → JUnit → pinned-timestamp jar, built twice,
  `cmp` clean).
- Fingerprint gate PASS on both Layer B homes. Build ID:
  **`EXC-AUTOSORT-0.1.0-WRV-20261106-04-f96f93`** (see
  `FINGERPRINTS.md`; seq -01 was a same-night pre-release build,
  superseded before install when Mike's 11:51/11:52 PM rulings were
  folded in; seq -02/-03 were superseded the same night as the rig
  caught and pinned down the double-chest defect below — only -04
  was ever filed).

## Tests

- **Unit: 19/19 PASS** (JUnit; the pure core — upstream sort-order
  cases ported as data, comparator + merge model, filter matching
  and reject itemization, buffer expiry/cap math, key roundtrips,
  active.txt import tolerance, the Protect §6.3 field tables
  exact incl. the null-actor hopper shape, the shared Java/Bedrock
  selector model, and the config-defaults test of §9 posture).
- **Rig: 50/50 PASS** (Paper 26.3, real GP 16.18.7 + Protect
  0.1.7 + PlaceholderAPI, clientless fake players on production
  event paths; final acceptance run `drive-run7b.log`, evidence in
  `rig-evidence/`): parity 12/12 (incl. import + restart
  persistence) · filter core 5/5 (itemized confirm, arm-sweep to
  the confirmer's buffer, cancel path, hopper filter,
  kill-switch) · double chest 7/7 (2-key arm, both-halves filter,
  break degrade ×2, span suspension ×3 incl. resume-by-owner) ·
  disaster 2/2 (netherite sword → buffer + Protect stream line;
  whole claim-back) · buffer lifecycle 5/5 (claim-overflow drops
  at feet, seeded expiry + purge stream line, 162-stack cap) ·
  shulker whole-buffering 2/2 (contents intact) · rights family
  10/10 (claim creation, ContainerTrust refused settings while
  still able to use the chest, PermissionTrust allowed on both
  entries, depositor credited, claim-delete suspension holds
  items, compass opens own chest, unrelated player refused on
  BOTH entries, untargeted command refused clean) ·
  Protect-absent 1/1 (filter works unbound; two seeded corrupt
  records skipped with log lines, valid record honored) · log
  verdicts 4/4 (import line, corrupt-source line, Protect
  UNBOUND/bound lines, clean boots, zero SEVERE).
- **Rig-caught product defect (fixed in this build):** the early
  acceptance runs exposed that a double chest armed from a block
  (not an open inventory) recorded ONE key — a block state's own
  inventory never reports a DoubleChest holder, so the pairing
  was invisible to `FilterEngine.keysOfBlock`, which would have
  broken break-degrade and span suspension in production too.
  Fixed by resolving the partner from the Chest block data using
  vanilla's own pairing rule (LEFT → clockwise of facing, RIGHT →
  counterclockwise; rig-verified against the game's behavior),
  builds -03/-04. Two further failure waves were rig artifacts,
  fixed in the harness only: fake-player aim geometry (a chest 2
  blocks ahead needs a −1.12 pitch drop) and the break steps'
  event order (production fires BlockBreakEvent while the block
  is still a chest). Run history: 42 → 45 → 48 → **50/50**.
- **Bedrock (honest scope):** the Cumulus form is unit-tested at
  the model level and its construction is exercised on every rig
  boot (the hook binds); the live-client tap flow (a real Bedrock
  player driving the form) is a named post-install player proof
  in the packet, per the house pattern — it is NOT claimed as
  rig-proven.

## SWAP PACKET (summary — full packet in INSTALL-PACKET.md)

- **Removes TWO jars:** `ExcelAutoSortBridge` + nothub `AutoSort`
  (renamed aside as `.held-20261106-pre-autosort`, never deleted).
- **Places:** `ExcelAutoSort-0.1.0.jar`. Plugin count 77 → 76.
- **active.txt import verification:** first-boot console line
  `imported N auto-sort states` must equal the pre-swap line count
  of `plugins/autosort/active.txt`; the import is read-only and its
  one-time guard is the existence of
  `plugins/ExcelAutoSort/data/players.yml`.
- **Menu diff:** `autosort.yml` changes EXACTLY two fields
  (`requires-plugin: AutoSort` → `ExcelAutoSort`;
  `requires-permission: autosort.toggle` → `excelautosort.toggle`)
  + two NEW entries (`packet/chestfilter.yml`,
  `packet/filterrecover.yml`).
- **Rollback:** remove the new jar, restore both held jars and the
  pre-swap `autosort.yml`, delete the two new entries, restart;
  `plugins/ExcelAutoSort/` data folder left in place (inert, holds
  the only copy of any records created since the swap).
- **Gates:** BACKUP-FIRST with cap-at-6 pruning; verified-empty
  (`list` 0 twice); panel marker discipline.
- **Seal:** BossTrial/GuildGate jars are untouched by this packet,
  so chapter acceptance does NOT re-open; boot-verify expects the
  seal line unchanged at LOCKED 3 of 5.

## Checked properties (named in the task / rulings)

- **Config posture (Mike, Nov 6, 2026, 11:51 PM): CHECKED.**
  `config.yml` is sectioned (`sort` / `filter` / `recovery` /
  `selector` / `messages`), every knob carries a comment saying what
  it does and what changing it means, every player-facing sentence
  lives under `messages:`, and the header states the growth rule
  (new knobs join their section; new sentences join `messages:` —
  no restructuring). The unit suite's config test loads an empty
  live config over the shipped defaults and asserts the decided
  values (168h / hoppers ON / cap 162 / page 28).
- **Targeted access only (§7.1):** the settings command resolves
  the crosshair block (`getTargetBlockExact(6)`, chest/trapped
  chest/barrel); no chest list or remote selection exists anywhere
  in the plugin. Untargeted use answers with the plain
  `look-at-chest` line. Rig case T12.
- **Compass entry (§7.1):** main-hand compass hit/tap on a
  chest/barrel routes through the identical resolution + §6.4
  rights check; rig cases T11 + T6 (both directions).
- **GP configure-rights rule (§6.4, Mike's explicit rule, Nov 6,
  2026, 11:52 PM):** configure = claim owner / PermissionTrust /
  recorded owner in unclaimed land / Landmark-plot renter
  (subdivision manager) / `excelautosort.admin` in admin claims.
  ContainerTrust/AccessTrust alone never suffices. **The pre-ruling
  read-only selector view was REMOVED from the code entirely**
  (session flag, both renderers, both message keys): a refused
  player gets the plain `no-permission` line and no view of the
  chest's mode or list, on every entry point. Rig cases T6/T11.
- **Max Organization (§15): SPEC ONLY.** Designed into the SPEC as
  the 0.2.0 fast-follow (rent $1,000/30 days or buy $15,000, per
  Mike's Nov 7, 2026, 12:01 AM pricing ruling). Nothing of it is in
  this build: no `max-org` config section, no code, no menu entry.

## Implementer's readings (disclosed)

- Trapped chest is accepted wherever the SPEC says "chest" for the
  settings flow (command target check + listener container set) —
  it is a chest for every gameplay purpose that matters here.
- Plugin data lives in `plugins/ExcelAutoSort/data/`
  (`players.yml`, `chests.yml`, `buffers.yml`), per SPEC §9.
- The Bedrock selector is rig-tested at the model level (unit test:
  one SelectorModel drives both skins) + form construction; the
  live-client Bedrock flow is a named post-install player proof in
  the packet, not a rig claim.
