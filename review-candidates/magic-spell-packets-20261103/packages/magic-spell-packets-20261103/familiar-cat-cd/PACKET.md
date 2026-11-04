# Familiar + Cat cooldown packet — DECIDED, FILED PENDING

**Prepared:** Nov 3, 2026 (prep); **decided and finalized:** Nov 3, 2026.  
**Requested change:** set familiar-related spell cooldowns to 4 minutes (240,000 ms).  
**Decision (Mike, Nov 3, 2026, ~9:39 AM CST, options-card pick):** **familiar and cat to exactly 240,000 ms**, chosen with full knowledge that both are currently 300,000 ms — i.e., this knowingly *shortens* both cooldowns by 60,000 ms. The picked option was, verbatim: "Familiar and cat to 4 min exactly, even though that's shorter than now."  
**Outcome:** The four prepared files in this folder now carry the real change: under the top-level `familiar` key and the top-level `cat` key, `cooldown: 300000` → `cooldown: 240000`, in stage 3, stage 4, stage 5, and the live file. Nothing else changed (wolf stays 60,000 ms, piggyback 20,000 ms, tame 30,000 ms). §6 install steps are **ACTIVE**; the packet is filed PENDING in the install MANIFEST and lands via the failsafe.

## 1. Source-of-record determination

### Winner: packet #3 install staging

Canonical files:

| Stage | Canonical source | SHA-256 | Bytes |
|---|---|---:|---:|
| Stage 3 / live content | `~/workspace/spell-logic-fixes/install-20261102-packet3/stage-s3/spells.yml` | `a7fcf51e342d62db74340b19e4ee4e92dc37e365ed79ecfdfdbdd48ebe0ad772` | 139,962 |
| Stage 4 | `~/workspace/spell-logic-fixes/install-20261102-packet3/stage-s4/spells.yml` | `9e4024a8113faf1812c3908daf02a2ee411df388aa1178db2ff95a237e699969` | 178,203 |
| Stage 5 | `~/workspace/spell-logic-fixes/install-20261102-packet3/stage-s5/spells.yml` | `e124568524103de8723f190a19efa1d9bea112dfe6a6e3456913bb594da7de6d` | 178,203 |

Why these win:

1. `install-20261102-packet3/INSTALL-PACKET.md` states that packet #3 was built from packet #2’s outputs, that packet #2 was installed and was the live base of record, and that packet #3’s stage-s3 file serves **both** the live `plugins/Magic/spells.yml` and the stage-3 MagicGate template. The same packet defines the stage-4 and stage-5 template destinations.
2. `install-20261102-packet3/INSTALLED.txt` records the Nov 2, 2026 failsafe install: all five then-current destination gates matched packet #2 exactly, packet #3 files were placed, the post-placement downloads were parent-hashed exact, and a post-boot live download-back was also exact to the stage-s3 hash above.
3. Their hashes are therefore not merely the newest staging copies; they are the recorded hashes of the files actually installed and read back from the server.

### Candidates rejected

| Candidate | Why it loses |
|---|---|
| `~/workspace/magic-scratch/stage-seal3/4/5/plugins/Magic/spells.yml` and the matching `seal3/4/5-packet` files | These are the original Oct 17, 2026 rollout snapshots documented by `~/workspace/magic-scratch/PACKETS-INDEX.md`. They predate spell packets #1–#3 and still carry the old pacing: familiar 30,000 ms, cat 60,000 ms, wolf 5,000 ms. They are historical build inputs, not the post-packet-3 live record. |
| `~/workspace/agents/0e8550a5-47fa-4ef4-bdde-ac0799ef0141/stage-mg/ExcelMagicGate/stages/stage3/4/5/spells.yml` | This agent-work-folder tree is byte-identical to the obsolete magic-scratch stage files: stage 3 SHA-256 prefix `820206e5d83d`, stage 4 `0322290d2d99`, stage 5 `9d1c2a01f49b`, with the same old familiar/cat/wolf values. Its directory shape looks like a MagicGate stage tree, but its content is the Oct 17/18 scratch generation and contains none of the Nov 2 packet #1–#3 retunes or fixes. Packet #3’s installed/read-back hashes beat directory shape. |
| `~/workspace/spell-logic-fixes/install-20261102/` and `install-20261102-packet2/` | Real predecessors in the chain, but superseded by packet #3. Packet #3 explicitly chains on packet #2 and its `INSTALLED.txt` is the latest install record. |

## 2. Familiar-family membership verdict

### In scope: exactly one key

| Key | Evidence | Stages present |
|---|---|---|
| `familiar` | A single top-level key; `SpawnEntity` cast action; weighted animal table; mana 50; `undo: 20000`. | Stage 3, stage 4, stage 5 only; absent from stage 2. |

No other key belongs to the familiar family on the evidence:

- `frag_familiar_raw.yml` contains only the base `familiar` entry.
- `frag_variants.md` inventories 32 tiered families in stage-s3; **familiar is not one of them**.
- `FULL-AUDIT-20261102.md` §4 states expressly: “familiar and cat have no tiers at all.”
- A parsed sweep of each canonical packet #3 file finds exactly one top-level key containing `familiar`: `familiar`. There are no `familiar|2`, `familiar|3`, or other rank keys.
- The canonical `familiar` entry has no `alias`, `aliases`, or `inherits` field, and its only action block is `cast`; there is no alternate-gesture key carrying a separate cooldown.

### Adjacent spells — reported for context, NOT changed

| Key | Relationship | Current cooldown in stages 3/4/5 | Treatment |
|---|---|---:|---|
| `cat` | Separate persistent summon spell (`SpawnEntity`, tamed cat), paced alongside familiar by packet #1 but not a familiar rank/variant. | 300,000 ms | **Changed to 240,000 ms** — Mike's picked option named cat explicitly |
| `wolf` | Separate persistent summon spell using Magic’s built-in `WolfSpell` class; its concurrent cap is `max_wolves`, not familiar configuration. | 60,000 ms | **Unchanged** |
| `piggyback` | Separate transient `SpawnEntity` novelty/mount spell; spawned pair auto-undoes. | 20,000 ms | **Unchanged** |
| `tame` | Not a summon at all: a targeted `Tame` action that spawns nothing. Included here because the audit’s family section expressly separates it from familiar. | 30,000 ms | **Unchanged** |

## 3. Before / after cooldown table

Target decided: **exactly 240,000 ms** for `familiar` and `cat`, applied as an explicit reduction from 300,000 ms per Mike's Nov 3, 2026 pick.

| Destination content | Key | Before | Prepared after | Delta |
|---|---|---:|---:|---:|
| Stage 3 template | `familiar` | 300,000 ms | **240,000 ms** | −60,000 ms |
| Stage 3 template | `cat` | 300,000 ms | **240,000 ms** | −60,000 ms |
| Stage 4 template | `familiar` | 300,000 ms | **240,000 ms** | −60,000 ms |
| Stage 4 template | `cat` | 300,000 ms | **240,000 ms** | −60,000 ms |
| Stage 5 template | `familiar` | 300,000 ms | **240,000 ms** | −60,000 ms |
| Stage 5 template | `cat` | 300,000 ms | **240,000 ms** | −60,000 ms |
| Live file (stage-3 content) | `familiar` | 300,000 ms | **240,000 ms** | −60,000 ms |
| Live file (stage-3 content) | `cat` | 300,000 ms | **240,000 ms** | −60,000 ms |

Adjacent context, all unchanged in every prepared stage file: `wolf` 60,000 ms; `piggyback` 20,000 ms; `tame` 30,000 ms. Verified by diff: each prepared file differs from its canonical packet #3 source on **exactly two lines** (the familiar and cat cooldown lines), and by YAML parse assertions (familiar mana 50, `undo: 20000`, no `entity_parameters` key, cooldown 240,000; cat cooldown 240,000).

### Decision record

The prep pass flagged that the configured value was already 300,000 ms (five minutes) — longer than the requested four minutes — and held the change for reconciliation. Mike was presented the discrepancy with options and picked, Nov 3, 2026 ~9:39 AM CST: **"Familiar and cat to 4 min exactly, even though that's shorter than now."** That is possibility 4 from the original reconciliation note, chosen explicitly. The separate open question — whether the configured cooldown actually registers in live play post-packet-#3 (the M1 cooldown-registration proof, one Familiar cast + one Cat cast) — is unaffected by this packet and remains an owed player proof.

## 4. Prepared files in this folder

All four files parse cleanly as YAML. Parsed assertions (verified Nov 3, 2026): familiar — no `entity_parameters` key, mana 50, cooldown **240,000 ms**, `undo: 20000`; cat cooldown **240,000 ms**; wolf 60,000 ms, piggyback 20,000 ms, tame 30,000 ms unchanged. Stage key counts are 168 (stage 3/live), 209 (stage 4), and 209 (stage 5).

| Prepared file | Source stage | SHA-256 | Bytes | Difference from canonical |
|---|---|---:|---:|---|
| `stage3-spells.yml` | Packet #3 stage-s3 | `5c340235a002134c9a9e051f1a3dc4580cc328bdba33432d9a6d5fb617022b69` | 139,962 | Exactly 2 lines: familiar + cat `cooldown: 300000` → `240000` (lines 4460, 4574) |
| `stage4-spells.yml` | Packet #3 stage-s4 | `14fc6112332c92d08b3bd54db8f21a6ca59a8836f5cf1ce672a49f4fde4725ea` | 178,203 | Exactly 2 lines: familiar + cat `cooldown: 300000` → `240000` (lines 4460, 4574) |
| `stage5-spells.yml` | Packet #3 stage-s5 | `bf30449cd4ecd1afbc9e7db0f7d70c9b7174b64e2760f648f29c33480005b03f` | 178,203 | Exactly 2 lines: familiar + cat `cooldown: 300000` → `240000` (lines 4548, 4662) |
| `live-spells.yml` | Packet #3 stage-s3 content | `5c340235a002134c9a9e051f1a3dc4580cc328bdba33432d9a6d5fb617022b69` | 139,962 | Exactly 2 lines, identical to `stage3-spells.yml` |

Supporting harness outputs saved alongside this packet:

- `harness-baseline-packet3.txt`
- `harness-prepared-noop.txt` (prep-stage no-op run, superseded)
- `harness-verdict-diff.txt` (prep-stage no-op verdict, superseded)
- `harness-prepared-240000.txt` (the decided files)
- `harness-verdict-diff-240000.txt` (baseline vs decided — see §5)

## 5. Logic-test harness results

Command shape:

```bash
python3 ~/workspace/spell-audit/logic-tests/spell_logic_tests.py \
  <stage-s3 file> <stage-s4 file> <stage-s5 file>
```

The baseline was the three canonical packet #3 files. The prepared run used the three stage files in this folder. Both runs exited 0.

| File content | Findings | CRITICAL | HIGH | MEDIUM | LOW |
|---|---:|---:|---:|---:|---:|
| Stage 3 baseline | 224 | 0 | 7 | 80 | 137 |
| Stage 3 prepared | 224 | 0 | 7 | 80 | 137 |
| Stage 4 baseline | 272 | 0 | 8 | 89 | 175 |
| Stage 4 prepared | 272 | 0 | 8 | 89 | 175 |
| Stage 5 baseline | 272 | 0 | 8 | 89 | 175 |
| Stage 5 prepared | 272 | 0 | 8 | 89 | 175 |
| **Total baseline** | **768** | **0** | — | — | — |
| **Total prepared** | **768** | **0** | — | — | — |

After normalizing only the file-heading paths and the run-exit labels, the verdict diff is empty: **zero new failures and zero changed findings**. Familiar produces the same two rows in each stage in both runs:

- Check 2, MEDIUM: persistent entities accumulate with no concurrent cap in config; mana 50, cooldown 300,000 ms, `undo=20000`.
- Check 1, LOW: mana 50, cooldown 300,000 ms → drain 0.2/s, at or below regen at pool 60.

### Decided-files run (240,000 ms) — Nov 3, 2026

The harness was re-run on the three edited stage files (`harness-prepared-240000.txt`, exit 0) and diffed against the packet #3 baseline with the same normalization (file-heading paths and run-exit labels only). **Verdict: zero new failures; finding counts and severities identical to baseline** (768 findings: stage 3 = 224, stage 4 = 272, stage 5 = 272; 0 CRITICAL). Exactly **30 content lines changed, all confined to familiar/cat rows** — per stage, 10 lines:

- `familiar` check 2 (MEDIUM, unchanged severity): cooldown text 300,000 → 240,000 ms.
- `familiar` check 1 (LOW, unchanged): cooldown 300,000 → 240,000 ms (drain still rounds to 0.2/s).
- `cat` check 1 (LOW, unchanged): cooldown 300,000 → 240,000 ms (drain still 0.1/s).
- `cat` check 2 (LOW, unchanged): cooldown text 300,000 → 240,000 ms.
- Check-2 detail lines for `familiar` and `cat`: cooldown 300,000 → 240,000 ms.
- Pacing lines for `familiar` and `cat`: cooldown 300,000 → 240,000 ms; sustained rate 0.20 → 0.25 spawns/min.
- Value-touching detail lines for `familiar` and `cat`: cooldown 300,000 → 240,000 ms.

No row outside familiar/cat changed; no severity moved. Full normalized diff: `harness-verdict-diff-240000.txt`.

## 6. Exact install steps for the failsafe window — ACTIVE

**Status: ACTIVE.** Mike decided Nov 3, 2026 (see header and §3 decision record): familiar + cat to exactly 240,000 ms. The prepared files carry the real change and are hashed in §4. This packet is filed PENDING in `~/workspace/wrv-install-queue/MANIFEST.md`; the failsafe executes the steps below in a verified-empty window.

1. **Decision — DONE.** End state: `familiar` and `cat` at exactly 240,000 ms in stages 3/4/5 and the live file. Prepared-file hashes of record are in §4; if any prepared file changes after filing, re-hash and update §4 and the MANIFEST entry before installing.
2. **BACKUP FIRST.** Before any jar/config/data swap, create a fresh Shockbyte backup and verify it completed. Do not proceed on an unverified backup.
3. **Fresh live gate.** Immediately before the window, pull all four destinations and hash them. The fresh hashes **must equal the packet #3 read-back hashes** — these are the gates:
   - `plugins/Magic/spells.yml` — gate: `a7fcf51e342d62db74340b19e4ee4e92dc37e365ed79ecfdfdbdd48ebe0ad772`
   - `plugins/ExcelMagicGate/stages/stage3/spells.yml` — gate: `a7fcf51e342d62db74340b19e4ee4e92dc37e365ed79ecfdfdbdd48ebe0ad772`
   - `plugins/ExcelMagicGate/stages/stage4/spells.yml` — gate: `9e4024a8113faf1812c3908daf02a2ee411df388aa1178db2ff95a237e699969`
   - `plugins/ExcelMagicGate/stages/stage5/spells.yml` — gate: `e124568524103de8723f190a19efa1d9bea112dfe6a6e3456913bb594da7de6d`

   Any fresh mismatch is a **STOP**, not an overwrite authorization: the live state has moved since packet #3 and must be reconciled first.
4. **Emptiness gate.** In the panel console, run `list` twice and require **0 of 10 online both times**. Do not proceed on a single reading. (No stage-2 destination is in this packet: stage 2 is an earlier stage under the stage-flip law and `familiar` is absent there.)
5. **In-place destination backups.** At each of the four destinations, copy the current file to `spells.yml.bak-20261103-prefamiliar-cd` and verify each backup exists before replacing anything.
6. **Place all four files as one set.** Place `live-spells.yml` at `plugins/Magic/spells.yml`, `stage3-spells.yml` at `plugins/ExcelMagicGate/stages/stage3/spells.yml`, `stage4-spells.yml` at the stage-4 path, and `stage5-spells.yml` at the stage-5 path. Do not leave live and template destinations split across windows.
7. **Read-back verification.** Download or otherwise hash every placed destination and require exact equality with the authorized prepared-file hashes. A size-only match is not sufficient.
8. **Full restart.** Use panel **Shut Down → Start**, not a Magic reload, so the boot is verified against the fresh files.
9. **Boot verification.** Require: server reaches Done; TPS 20.0; Magic reports **Loaded 167 spells** with no spell-load errors; ExcelGuildGate still reports **3 of 5 seals broken, guild LOCKED**; attributable SEVERE count is 0.
10. **Post-boot file verification.** Parse the live file and every stage template and assert **familiar cooldown 240,000 and cat cooldown 240,000** at all four destinations. Record the window, backup identity, gate hashes, placed hashes, and boot lines in the completion marker `~/workspace/spell-familiar-cd-20261103/INSTALLED.txt`.

## 7. Rollback notes

- Restore each destination’s `spells.yml.bak-20261103-prefamiliar-cd`, verify the restored hashes return to the pre-install gate hashes (the packet #3 read-back hashes in §1/§6.3), then perform a full panel Shut Down → Start and repeat the boot checks in §6.9.
- Rollback returns familiar **and cat** to the pre-window value of **300,000 ms** at all four destinations.
- Stage 2 is untouched by this packet and needs no rollback.
