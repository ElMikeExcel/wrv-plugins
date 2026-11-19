# BUILD REPORT — ExcelDowned 1.0.2 (open Revive Potion targeting)

- Worker: build worker (subagent), board chain
- Date: Thu Nov 19, 2026, ~12:10–12:20 AM CST (America/Chicago)
- Decision implemented: **Mike, Nov 19, 2026, 00:07 CST — the
  Revive Potion's party-only targeting is REMOVED.** His words:
  "I like the idea of being able to just use it on anyone you come
  across to be helpful."
- Supersedes: the earlier same-night brief (a not-party hint on
  the silent refusal) — the hint was NOT built; the refusal it
  would have explained no longer exists. No hint code, no dead
  code.

## Verdict

```
BUILT + FILED — ExcelDowned 1.0.2 (open potion targeting)
  Candidate: ExcelDowned-1.0.2.jar — 139,867 B /
    8237327f781c6a63e78637153d4e1b75b52d34cc20169e3f30538bef9085609f
  Build ID: EXC-DOWNED-1.0.2-WRV-20261119-01-d0wn3d
  Tests: 79/79 PASS (77 carried from the 1.0.1 suite + 2 new),
    double-build byte-identical (jar == repro), fingerprint
    gate PASS.
  Load check (D122): VM-local Paper 26.2 rig PASS — Done
    (48.600s), SEVERE 0. Homebase mirror NOT used (found state
    under the idle-queue calibration lane); VM-local rig is the
    substitute M1's G-4 used. Log: work/vm-rig-boot.log.
  Status: Filed (MANIFEST PENDING, sequenced AFTER M1 /
    ExcelDowned 1.0.1). NOT installed.
```

## ⭐ Design supersession — stated prominently per the filing instruction

**1.0.2 supersedes design S-1's party-only potion targeting per
Mike's Nov 19, 2026 decision.** From this build forward, the
Revive Potion works on any downed player. The Resurrection spell
keeps its own party rule and the Cleric Raise Ally gate is
untouched — both are separate code paths, outside this build.

## Base

Built ON the 1.0.1 source tree (M1 ExcelItems migration,
`~/workspace/board-r559/m1-downed/work/build-1.0.1/`), copied to
`work/build-1.0.2/` in this folder. 1.0.1 is built + filed,
pending its D125 gate; 1.0.2's install premise is 1.0.1 installed
(the MANIFEST entry carries that sequencing gate).

## The change (three consult points, all cut)

1. **Prompt** — `RevivePotionPolicy.promptEligible`: the
   `membership` parameter and its `!= PARTY` refusal are removed.
   Any downed player in prompt range prompts a potion carrier.
2. **Interact** — `RevivePotionUi.onInteractEntity`: the silent
   `membership != PARTY` return is removed; the (now dead)
   `membership()` helper is removed with it.
3. **Confirm** — `RevivePotionPolicy.confirmVerdict`: the
   `membership` parameter and the NOT_PARTY refusal are removed;
   `ConfirmVerdict.NOT_PARTY` and the UI's NOT_PARTY message case
   are removed with them (no dead code).

Copy updated to stay truthful (no mechanics): plugin.yml
description ("revive any downed player"), DownedCommands chat-arm
fallback + give message ("a downed player"), the downed player's
own message ("unless someone revives you"). Version knobs bumped
in plugin.yml, BuildInfo fragments, local-build.sh
(1.0.2 / 2026-11-19 / EXC-DOWNED-1.0.2-WRV-20261119-01-d0wn3d);
the four test version pins updated to match.

## Untouched — verified, not assumed

- Cleric Raise Ally: `ClericRevivePolicy` / `ClericRevive` not
  edited; its NOT_PARTY gate stands (a source pin in the new
  tests asserts it survives — whether it should also open is a
  separate question for Mike, NOT part of 1.0.2).
- Resurrection spell: `MagicBridge` not edited; its party rule
  stands (source-pinned likewise).
- Economics/mechanics: $5,000 price, passive carry, consume-only-
  after-land, 50% HP, 10-min per-carrier cooldown, totem deferral,
  300 s bleed-out, self-revive exclusion, ranges, config.yml
  (byte-identical to the 1.0.1 tree — never edited), data files.

## Tests (79/79)

- The three potion matrices (prompt / self-exclusion / confirm)
  rewritten to the membership-free signatures — every remaining
  axis pinned exactly as before (the partied path is the same
  path; identical inputs, identical outcomes).
- NEW `potionOpenTargetingStrangerReviveChain`: a carrier with no
  party relationship walks the full chain — prompt eligible →
  confirm OK → shouldConsume(true) → the 10-minute cooldown
  starts → a second confirm is refused CARRIER_ON_COOLDOWN.
- NEW `potionOpenTargetingSourcePins`: the potion policy/UI
  contain no membership consult and no NOT_PARTY refusal; the
  Cleric policy and MagicBridge keep theirs.
- All prior suites pass unchanged otherwise (77 carried tests
  include the M1 registry pins, purchase, Cleric, session,
  TogetherPartyProvider suites).

## Load check detail (VM-local rig)

Throwaway Paper 26.2-123 server in /tmp (primed runtime copy;
rig discarded after), exactly three plugins co-resident with
fresh data folders: ExcelItems-0.1.0.jar (live pin 85dbb9ea…) +
Magic-11.2.5.jar + candidate ExcelDowned-1.0.2.jar.

- `Done (48.600s)`; **SEVERE count 0** (whole boot log).
- `Enabling ExcelItems v0.1.0` line 175 BEFORE
  `Enabling ExcelDowned v1.0.2` line 177 — hard-depend order
  proven; `ExcelItems enabled: 4 custom item(s) registered,
  next free id 5.`
- `ExcelDowned: registered revive behavior for ExcelItems ID 4
  (exceldowned:revive-potion).` present.
- Ready line: `ExcelDowned 1.0.2 ready — build
  EXC-DOWNED-1.0.2-WRV-20261119-01-d0wn3d; sessions restored: 0;
  bleed-out 300s; downed factor 0.25`.
- Rig-environment WARNs only (no Vault on the rig → the designed
  "purchases disabled" notice; Magic spell-template self-check
  notices because the bare rig carries no WRV spell data) — the
  same bare-rig notice class as M1's rig, not build defects.
- Log preserved: `work/vm-rig-boot.log`, SHA-256
  6ac29f483fb6549ff0a1528933691e0275d6257bde0d47bc90e3dfe78c9f0312.
