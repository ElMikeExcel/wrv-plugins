# ExcelDowned 0.1.5 — DESIGN (self-check timing fix)

Scope: DIAGNOSIS.md §5 (board-r427/downed-template-diagnosis) exactly.
One production file changes (`ExcelDowned.java`), plus one new pure
policy class (`TemplateCheckPolicy.java`, same pattern as FixPolicy),
version/fingerprint bumps, and tests.

## Defect

`checkSpellTemplates()` ran synchronously in `onEnable`. Magic
11.2.5 loads its spell registry in a deferred phase completing
~7 s later, so `getSpellTemplate(key)` returned null for ANY key at
probe time and the check emitted a false template WARN for both
`resurrection` and `grouprenewal` on every boot (5/5 boots).

## Fix

- The check is deferred to a scheduled task: attempts at +10 s,
  +20 s, +40 s after enable (bounded, 3 attempts).
- Each attempt probes the control key `heal` (known-good builtin)
  alongside the two target keys, decided by TemplateCheckPolicy:
  control false -> registry not loaded yet -> one INFO deferred
  line + retry, never a template WARN; control true + target false
  -> genuinely missing -> WARN once per missing key; probe null ->
  the distinct UNPROVEN branch (retry, final-attempt WARNING).
- WARN wording corrected: the stale "grants of this spell will NPE
  in Magic's wand build" claim is dropped (since MagicGate 0.3.2,
  grant-time verification runs at wand-sync time against the
  populated registry). The new wording states the key did not
  resolve in Magic's loaded spell registry at the final self-check.
- Fail-loud preserved: a genuinely missing template still surfaces
  as a boot-visible WARN (rig R2 negative control).

## NOT in this packet

- The optional stage5 template range rider (DIAGNOSIS §5: stage5
  resurrection `range: 16` -> `20`) is a separate template-side
  call — deliberately excluded.
- No change to the revive/PreCast path, MagicBridge, provider,
  config, or spells data. S7 live retest remains owed separately.
