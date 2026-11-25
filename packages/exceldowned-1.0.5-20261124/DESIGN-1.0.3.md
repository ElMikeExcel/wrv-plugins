# ExcelDowned 1.0.3 — open Resurrection spell targeting

**Decision:** D140 (Mike, Nov 19, 2026, 9:20 PM CST) — the
Resurrection spell opens to ANYONE, matching the revive potion
(ExcelDowned 1.0.2 rule). HARD-REMOVE of the gate, no config flag
(supervisor decision D144, following the 1.0.2 potion precedent).

**What changes:** the spell's party-only restriction is REMOVED.
Party membership is no longer consulted at any of the spell path's
three consult points in `MagicBridge.java`:

1. Pre-cast gate — `onPreCast`: `findDownedParty` becomes the
   party-blind `findDowned` (same-world + in-range + downed +
   not-caster, NO membership test). Refusal wording: "no downed
   player nearby".
2. Aim resolution — `pickAimedTarget`: candidates come from
   `findDowned`. Refusal wording: "found no downed player in your
   aim". Cooldown-refund logic untouched.
3. Custom action — `ResurrectAction.perform`: the aimed-target
   membership test and the fallback-loop membership `continue`
   are deleted. Same-world + revive-range checks stay exactly as
   they are.

**Why:** the 1.0.2 potion change (Mike, Nov 19, 2026, 00:07 CST)
opened the potion to anyone; the spell kept its own party rule as
a separate code path. D140 closes that carve-out. The spell
definition in live `spells.yml` carries no party key, and
`only_friendly: true` is not a party gate (proven from the live
Magic jar, W7 §1d) — no `spells.yml` change is needed.

**What does NOT change:** 100 mana, 10-minute spell cooldown
(600000 ms), master path + 20 casts, 50% revive health, gate-range
/ revive-range 20.0, 300 s bleed-out, self-revive exclusion, the
downed-mage pre-cast gate, the cooldown refund on a failed cast,
the Cleric Raise Ally gate (separate path, `require-party: false`
default already opens it), config.yml (byte-identical to the 1.0.2
tree), the party system itself. `plugin.yml` description updated
one clause ("brings any downed player back") so copy stays
truthful. No dead code: the `findDownedParty` helper is renamed,
not left behind.

**Base:** builds on the 1.0.2 tree (open potion targeting). Installs
only after 1.0.2 is installed (MANIFEST sequencing). Rollback is a
jar swap + restart to the 1.0.2 jar.
