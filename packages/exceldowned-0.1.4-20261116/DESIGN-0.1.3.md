# DESIGN — ExcelDowned 0.1.3 (P-DOWNED-REVIVE-1 fix chain)

Base: verified 0.1.2 tree (`~/workspace/downed-012-build/ExcelDowned/`),
base gate passed run 364 (unmodified rebuild byte-identical to the
shipped 0.1.2 jar). Diagnosis of record:
`~/workspace/board-r363/downed-diagnosis/DIAGNOSIS.md` +
`EVIDENCE-PULL.md` (premise correction: the Nov 14 session ran its full
designed course — 246.84 s elapsed + 54.00 s damage-eaten = 300.84 s).

## Defect 3 — totem first (proven in 0.1.2 source: zero totem handling)
Standing lethal intercept now checks both hands first
(`FixPolicy.holdsTotem` / `entersDowned`): a lethal non-void hit on a
player holding a TOTEM_OF_UNDYING in main or off hand is not cancelled
and does not down — vanilla consumes the totem. Inventory-only totems
do not count (vanilla parity). Void stays a real death. A MONITOR
`EntityResurrectEvent` listener records `totem_save` rows.

## Defect 1 — window rule (evidence verdict, not the original framing)
Damage while downed eats ZERO bleed-out window by default
(`damage-eats-window: false`): the bleed-out is a pure 300 s
wall-clock window. Friendly/rescue-side damage (the incident's largest
consumer, 41 rows / 34.53 s incl. one 28.95 s hit) can no longer
shorten a rescue. The scaled eat mechanism remains only behind the
explicit operator opt-in. Hardening from the diagnosis: absorption now
also CANCELS the event (was: zero only), fall absorption cancels too,
an `EntityCombustEvent` guard prevents (re)ignition, fire ticks are
cleared on entry / every state tick / exit, a MONITOR audit logs any
downed damage event that still reaches MONITOR uncancelled and
non-zero, and the `bypassDeath` latch is removed on quit and join.
Deliberately NOT used: `setInvulnerable` (diagnosis open question 5 —
interactions with EliteMobs targeting / Protect records unproven).

## Defect 2 — Resurrection / grant NPE (ExcelDowned-side legs)
The NPE itself lives in Magic's wand build when a spell template is
null; the grant-side held-key audit belongs to ExcelMagicGate (a
separate plugin — named as an owed separate build in RESULT.md, not
claimed here). This build carries every ExcelDowned-side leg:
- `Resurrect` action registration also runs at `onLoad`, before Magic
  loads templates at enable, plus the existing onEnable attempt.
- Boot self-check probes the `resurrection` AND `grouprenewal`
  templates reflectively: a dead template is a boot WARNING naming the
  key; a probe that cannot run is itself a WARNING (never fail-open).
- Ranges aligned to ONE number: gate 20 / revive 20 in config (spell
  range 16 in Magic spells.yml is a packet file change, in the packet
  draft — not part of this jar).
- `ResurrectAction` direct target and fallback both now require
  same-world + within revive range (the 0.1.2 fallback checked
  neither). `FixPolicy.grantVerified` pins the verification rule
  (wand presence AND template resolves) for the MagicGate leg.

## Restore note
Install replaces the held live items: jar
`ExcelDowned-0.1.2.jar.held-20261114` → this candidate as
`ExcelDowned-0.1.3.jar`, data folder `ExcelDowned.held-20261114/`
restored to `ExcelDowned/`, restart, boot verify (self-check lines,
SEVERE 0). Packet draft carries the full sequence; supervisor files.
