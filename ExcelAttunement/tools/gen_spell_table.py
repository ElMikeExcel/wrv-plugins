#!/usr/bin/env python3
"""Regenerate spell-classes.txt (+ the test fixture spell-signals.txt)
from the ExcelMagicGate stage configs — the ONLY source of spell truth
on this server (Magic runs with load_default_spells: false; a spell
that is not in a stage file does not exist).

Inputs (defaults point at the MagicGate 0.1.0 project tree):
  <stages>/stage5/spells.yml   full spell definitions (categories,
                               cast action classes, inheritance)
  <stages>/stage*/grants.txt   the issuable spell universe per stage
                               (stage 5 is the cumulative superset)

The CLASS ASSIGNMENTS below are the curated design table (Combat
Attunement, Oct 19, 2026). They were derived from the signals in the
real definitions and are cross-checked by this script:
  * any spell whose resolved cast actions contain Damage -> COMBAT
  * any spell with Heal/Feed and no Damage -> never COMBAT
  * SUPPORT entries must carry a Heal/Feed/PotionEffect signal
The Java test suite re-runs the same cross-checks against
spell-signals.txt, and asserts every granted spell in every stage is
classified. If MagicGate ever ships a new stage, re-run this script:
it fails loudly on any granted spell missing from the table.
"""
import re
import sys
import yaml

COMBAT = set("""arrow arrow_ice arrow_light arrow_regular arrowrain arrow_shock
chainlightning lightning smite missile frost stun wave stream earth fury
masterattack powerstone sweep whip boomerang throwsword anvil holylight
stormshackle dash shell walls grow counterspell shrink tendril push pull
force critical""".split())

SUPPORT = set("""heal healing superheal cure feed eat_food blessing boon
gills invincible cloak tamepet""".split())

RATIONALE = {
    # COMBAT by control/debuff where no Damage action exists
    "shell": "Magic's own category is combat; encases a target in blocks (fight control)",
    "walls": "Magic's own category is combat; battlefield control",
    "grow": "Magic's own category is combat; GrowEntity debuff on a target",
    "counterspell": "Magic's own category is combat; anti-caster Undo",
    "shrink": "Magic's own category is combat; ShrinkEntity debuff",
    "tendril": "Magic's own category is combat; battlefield control",
    "push": "enemy-directed velocity control (knockback class)",
    "pull": "enemy-directed velocity control (reposition class)",
    "force": "enemy-directed projectile velocity control",
    "critical": "passive crit-damage proc (Damage action in its definition)",
    "holylight": "carries BOTH Heal and Damage - Damage wins: casting it is fighting",
    # SUPPORT judgment calls
    "blessing": "beneficial AoE buff (PotionEffect on allies)",
    "boon": "beneficial self buff (PotionEffect)",
    "gills": "beneficial self buff (water breathing)",
    "invincible": "beneficial self buff (resistance)",
    "cloak": "beneficial self buff (vanish/invisibility)",
    "tamepet": "heals the caster's pet (Heal action, no Damage)",
    # UTILITY judgment calls
    "sparkle": "random-outcome novelty spell; no reliable combat function",
    "bubble": "kit's own category is utility; no damage/heal signal",
    "lift": "kit's own category is utility; block lifting, no damage signal",
    "gather": "party/admin teleport utility",
    "extinguish": "fire removal; the stage file's own utility group",
}


def actions_of(node, acc):
    if isinstance(node, dict):
        c = node.get("class")
        if c:
            acc.add(c)
        for v in node.values():
            actions_of(v, acc)
    elif isinstance(node, list):
        for v in node:
            actions_of(v, acc)


def main(stages):
    grants = set()
    for s in range(6):
        txt = open(f"{stages}/stage{s}/grants.txt").read()
        grants |= set(re.findall(r"permission set magic\.cast\.([a-z_0-9]+)", txt))
    data = yaml.safe_load(open(f"{stages}/stage5/spells.yml"))

    def resolved_actions(key, seen=None):
        seen = seen or set()
        if key in seen or key not in data:
            return set()
        seen.add(key)
        spec = data[key]
        acc = set()
        if isinstance(spec, dict):
            if "actions" in spec:
                actions_of(spec["actions"], acc)
            inh = spec.get("inherit")
            if inh:
                acc |= resolved_actions(inh, seen)
        return acc

    table = {}
    problems = []
    for g in sorted(grants):
        acts = resolved_actions(g)
        if g in COMBAT:
            cls = "COMBAT"
        elif g in SUPPORT:
            cls = "SUPPORT"
        else:
            cls = "UTILITY"
        if "Damage" in acts and cls != "COMBAT":
            problems.append(f"{g}: has Damage action but classified {cls}")
        if ("Heal" in acts or "Feed" in acts) and "Damage" not in acts and cls == "COMBAT":
            problems.append(f"{g}: heal/feed signal but classified COMBAT")
        if cls == "SUPPORT" and not (acts & {"Heal", "Feed", "PotionEffect"}):
            problems.append(f"{g}: SUPPORT without a heal/feed/buff signal")
        spec = data.get(g) or {}
        table[g] = (cls, spec.get("category"), sorted(acts))
    if problems:
        print("SIGNAL PROBLEMS:")
        for p in problems:
            print(" -", p)
        sys.exit(1)

    counts = {"COMBAT": 0, "SUPPORT": 0, "UTILITY": 0}
    lines = [
        "# ExcelAttunement spell classification table.",
        "# Generated by tools/gen_spell_table.py from the ExcelMagicGate",
        "# stage configs (stage5/spells.yml definitions + stage grants).",
        "# Do not hand-edit: change the assignment sets in the generator,",
        "# re-run it, and let the signal cross-checks + tests judge.",
        "# Format: <spell key> = <COMBAT|SUPPORT|UTILITY>",
        "# Counts: COMBAT 36 / SUPPORT 12 / UTILITY 52 (100 issuable spells).",
    ]
    sig = ["# key hasDamage hasHealOrFeed magicCategory"]
    for g in sorted(table):
        cls, cat, acts = table[g]
        counts[cls] += 1
        lines.append(f"{g} = {cls}")
        sig.append(f"{g} {'1' if 'Damage' in acts else '0'} "
                   f"{'1' if ('Heal' in acts or 'Feed' in acts) else '0'} {cat or '-'}")
    assert counts == {"COMBAT": 36, "SUPPORT": 12, "UTILITY": 52}, counts
    open("spell-classes.txt", "w").write("\n".join(lines) + "\n")
    open("tests/spell-signals.txt", "w").write("\n".join(sig) + "\n")
    print(f"wrote spell-classes.txt + tests/spell-signals.txt: {counts}")


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else
         "/home/hatch/workspace/excelmagicgate-0.1.0/src/ExcelMagicGate/stages")
