# PACKET DRAFT — ExcelWandBars 0.3.13 (cooldown accuracy) — NOT FILED

Status: staged candidate, desk-proven. NOT in the MANIFEST. Files
through the board supervisor like the other WandBars packets when the
parent green-lights the sequence.

## Artifact

- `ExcelWandBars-0.3.13.jar` (this folder) — 544,853 B, sha256
  `7278f09ed160236b54c02d9dfaa590feeb1ba25027c901b3a17217848a1327e3`,
  build id `EXC-WANDBARS-0.3.13-WRV-20261108-01-41cd07`.
- Contains ALL of 0.3.12 (pager LIST-axis default, SlotGuard,
  PresentedSweep) PLUS the cooldown-accuracy change (CooldownDisplayPlan,
  NameTagHook accuracy mode, MagicHook.baseCooldownMs, config
  `cooldown-name-tag.accuracy`).

## Sequence vs the in-flight 0.3.12

0.3.13 is built ON the 0.3.12 tree and is a superset jar. Two legal
sequences:

1. **0.3.12 installs first** (its own packet, live-config already
   carries its one key), then 0.3.13 replaces it in a later window —
   one extra jar swap, each step independently proven.
2. **0.3.13 installs INSTEAD of 0.3.12** — one jar swap total; the
   0.3.12 packet is retired as superseded. The 0.3.12 player proofs
   (tap-Next / two-tap) remain owed either way and are unaffected.

Recommendation: sequence 2 (one window, one swap) unless the parent
wants 0.3.12's pager fix live ahead of this packet for other reasons.

## Gates (install window)

- **Live jar gate:** the live WandBars jar is the 0.3.11-content jar
  (`ExcelWandBars-0.3.4.jar` filename on the panel). Swap is JAR-ONLY
  plus the config step below; BACKUP-FIRST + cap-at-6 + emptiness
  gate per standing rules (this packet mixes jar + config = fully
  gated, NOT a text live-edit).
- **Config step:** live config gains ONE key under the existing
  `cooldown-name-tag:` section: `accuracy: true` (the code default is
  also true when the key is absent, so the swap is safe even if the
  config edit lags; the key is added for visibility/rollback clarity).
  No other config changes. (Note the separately-flagged broader
  WandBars config refresh — 0.2.0-era live config — is NOT part of
  this packet.)
- **Rollback:** restore the pre-install jar + remove the one key;
  annotated names self-clean via the hook's sweeps on disable/quit.

## Post-install player proofs (the deciding evidence for the render)

1. **Heal (the specimen):** cast Heal, watch the icon: name tag counts
   the TRUE cooldown (8 s at base rank) in step with Magic's "wait"
   reminder on a deliberate early recast — they must agree. Before
   this fix the number flatlined at ~1 s.
2. **Mana-starved shape:** drain mana, cast Flash Heal dry — NO
   cooldown tag may appear for the mana wait itself (cooldown is 0);
   the mana bar alone tells that story.
3. **Familiar (Bedrock + Java):** cast Familiar — tag counts 4:00 →
   0:01 in the name on both platforms (Bedrock's number caps at 64;
   the tag is the truth there).
4. **Short truthful spell:** any sub-10 s cooldown spell with full
   mana — no tag; Magic's number alone, unchanged from today.
5. 0.3.12's owed proofs ride along unchanged (pager tap-Next; Java
   two-tap list→hotbar; Bedrock dialect verdict).

## Surfaces owed on install (same pass, per standing rule)

- Guide knowledge/intents: cooldown display explanation (tag = true
  cooldown; number = time until castable, can include mana wait).
- What's New: website + in-game changelog entry (registry-generated).
- Broadcaster tips: check — a "the name tag is the real cooldown" tip
  fits the rotation.
- Menus: no change (no new commands/settings surface; the toggle is
  config-only — flag if Mike wants it in the WandBars settings menu).
