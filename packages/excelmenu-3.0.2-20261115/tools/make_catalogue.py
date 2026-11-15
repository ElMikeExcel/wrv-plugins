#!/usr/bin/env python3
"""ExcelMenu 2.0 Phase 1 — commands-catalogue converter of record.

Turns the two old catalogue columns into registry entry files
(menu: commands_menu), per DESIGN-2.0 §4 Phase 1 + census Amendment B:

  - Java column:    fixtures/commands_menu.yml + commands_menu_page2.yml
                    (the ExcelMenu 0.1.8 preserved set, e230a10b0a29 /
                    4fe2562ff321) — item ids, icons, labels, lore and
                    click lines are the canonical action dialect.
  - Bedrock column: fixtures/bedrock-arrays.json (machine-extracted
                    from ExcelBedrockMenu 1.5.27's CATALOG arrays) —
                    displays, run strings, descriptions, group order.

The conversion is a UNION: a row that exists on only one platform
joins both (parity law), with its icon/description taken from the
side that has it (icons minted for Bedrock-only rows are named in
PHASE-1-REPORT). Where the two columns' WORDS differ for the same
row, the converter picks per the drift table below and PRINTS every
pick — nothing is resolved silently; the picks are the C3 acceptance
in the catalogue harness.

Rows are matched across columns by their command display string.
"""
import json
import pathlib
import sys

import yaml

ROOT = pathlib.Path(__file__).resolve().parent.parent
FIX = ROOT / "fixtures"
OUT = ROOT / "src/main/resources/entries"
MENU = "commands_menu"

GROUPS = [  # (key, display name) — registry order = old group order,
            # Fixes & Help inserted before Your Items & Help (its Java
            # page-1 position relative to the page-2 group).
    ("getting_around", "Getting Around"),
    ("money_jobs", "Money & Jobs"),
    ("skills_levels", "Skills & Levels"),
    ("shops_trading", "Shops & Trading"),
    ("claims_land", "Claims & Land"),
    ("guild_quests", "Guild & Quests"),
    ("server_info", "Server Info"),
    ("fixes_help", "Fixes & Help"),
    ("your_items_help", "Your Items & Help"),
]
GROUP_KEY = {name: key for key, name in GROUPS}

# Bedrock-only rows: (display, group, minted entry id, minted icon).
# Icons are the only invented data in the conversion (Java needs one);
# both rows' descriptions/actions come from the Bedrock column itself.
BEDROCK_ONLY = {
    "/childproofmenu": ("cl_childproofmenu", "IRON_DOOR"),
    "/unchildproof <player>": ("cl_unchildproof_player", "OAK_DOOR"),
    "/whatsnew": ("si_whatsnew", "NETHER_STAR"),
}

# Wording-drift picks for shared rows (name -> (winner, loser)) —
# printed at the end; mirrored by the harness's C3 acceptance.
DRIFT_PICKS = []  # filled dynamically: (row, field, kept, dropped)

# SETTLED TEXT FIXES (INSTALL-PACKET §7 + the Oct 31, 2026 addendum —
# Mike's decisions): where a settled decision supersedes the verbatim
# Java text, the conversion applies the settled text and prints it.
# Baked into the converter at the Nov 1, 2026 re-base so the shipped
# data carries them (before that they were install-time edits).
# Keyed by entry id; each pair must actually fire — a missing stale
# string aborts the conversion loudly rather than going stale.
SETTLED_TEXT_FIXES = {
    "st_shop": [("&7Open the Spawn Shop", "&7Open the Excel Shop")],
    "yi_lostfound": [("&7the void or a despawn in the last 7 days",
                      "&7the void or a despawn in the last 14 days")],
}


def q(s):
    return "'" + str(s).replace("'", "''") + "'"


def load_java_rows():
    """display -> dict(id, icon, label, lore, action, group, seq)."""
    rows = {}
    seq = 0
    for fname in ("commands_menu.yml", "commands_menu_page2.yml"):
        doc = yaml.safe_load((FIX / fname).read_text(encoding="utf-8"))
        group = None
        for item_id, item in doc["items"].items():
            name = item.get("name", "")
            lore = item.get("lore") or []
            if lore and lore[-1] == "&8Command group":
                group = name.replace("&6&l", "")
                continue
            if item_id in ("nav_more", "back"):
                continue  # pagination chrome -> layout fixed items
            clicks = (item.get("clicks") or {}).get("left") or []
            display = name.replace("&a&l", "")
            rows[display] = dict(
                id=item_id, icon=item["material"], label=name,
                lore=lore, action=clicks, group=group, seq=seq)
            seq += 1
    return rows


def load_bedrock_rows():
    """display -> dict(run, description, group, seq)."""
    data = json.loads((FIX / "bedrock-arrays.json").read_text(encoding="utf-8"))
    rows = {}
    seq = 0
    for gi, group_rows in enumerate(data["catalog"]):
        for r in group_rows:
            rows[r["display"]] = dict(
                run=r["run"], description=r["description"],
                group=data["catalog_groups"][gi], seq=seq)
            seq += 1
    return rows


def hint(display):
    return f"[message] &7Type it in chat: &e{display}"


def emit(e):
    lines = ["# ExcelMenu 2.0 registry entry — commands catalogue, converted in",
             "# Phase 1 from the ExcelMenu 0.1.8 commands pages (Java) and the",
             "# ExcelBedrockMenu 1.5.27 CATALOG arrays (Bedrock). See",
             "# tools/make_catalogue.py — the converter of record."]
    lines.append(f"id: {e['id']}")
    lines.append(f"menu: {MENU}")
    lines.append(f"group: {e['group']}")
    lines.append(f"order: {e['order']}")
    lines.append(f"order-bedrock: {e['order_bedrock']}")
    lines.append(f"icon: {e['icon']}")
    lines.append(f"label: {q(e['label'])}")
    if e.get("lore"):
        lines.append("lore:")
        for l in e["lore"]:
            lines.append(f"  - {q(l)}")
    if e.get("action"):
        lines.append("action:")
        for a in e["action"]:
            lines.append(f"  - {q(a)}")
    (OUT / f"{e['id']}.yml").write_text("\n".join(lines) + "\n", encoding="utf-8")


def main():
    java = load_java_rows()
    bedrock = load_bedrock_rows()
    displays = list(dict.fromkeys(list(java) + list(bedrock)))

    # Union sequence: sort by (group index, bedrock seq if present else
    # java seq + 0.5 bias) — Bedrock order wins ties because the Java
    # page order is a two-page artefact; per-group picks printed below.
    def group_index(d):
        g = (java.get(d) or bedrock.get(d))["group"]
        return [k for k, _ in GROUPS].index(GROUP_KEY[g])

    def sort_key(d):
        gi = group_index(d)
        b = bedrock.get(d)
        j = java.get(d)
        if b is not None:
            within = b["seq"]
        else:
            within = 100 + j["seq"]  # Java-only rows follow their group's
            # Bedrock rows unless the drift table says otherwise
        return (gi, within)

    # Java-only rows that must sit INSIDE their group (not after it):
    # buildmode closes Shops & Trading; emotes closes Server Info;
    # the Fixes & Help rows form their whole group in Java order.
    MANUAL_WITHIN = {
        "/buildmode": 60,        # after mobveil (bedrock seq 23-ish band)
        "/emotes": 95,           # after whatsnew in Server Info
    }

    def sort_key2(d):
        gi = group_index(d)
        b = bedrock.get(d)
        j = java.get(d)
        if d in MANUAL_WITHIN:
            return (gi, MANUAL_WITHIN[d])
        if b is not None:
            return (gi, b["seq"])
        return (gi, 100 + j["seq"])

    displays.sort(key=sort_key2)

    per_group_order = {}
    entries = []
    for d in displays:
        j = java.get(d)
        b = bedrock.get(d)
        group_name = (j or b)["group"]
        gkey = GROUP_KEY[group_name]
        per_group_order.setdefault(gkey, 0)
        per_group_order[gkey] += 1
        order = per_group_order[gkey] * 10
        if j is not None:
            eid, icon, label, lore = j["id"], j["icon"], j["label"], list(j["lore"])
            action = list(j["action"])
        else:
            eid, icon = BEDROCK_ONLY[d]
            label = f"&a&l{d}"
            lore = [f"&7{b['description']}", f"&8{group_name}"]
            action = [f"[player] {b['run']}"] if b["run"] else [hint(d)]
        # Lore: Java lore wins where present (it carries the group
        # trailer); record wording drift vs the Bedrock description.
        if j is not None and b is not None:
            java_desc = j["lore"][0].replace("&7", "") if j["lore"] else ""
            if java_desc != b["description"]:
                DRIFT_PICKS.append((d, "description", java_desc, b["description"]))
            # Action dialect check: Bedrock run vs Java [player] line.
            if b["run"] is not None:
                expected = f"[player] {b['run']}"
                if expected not in action:
                    DRIFT_PICKS.append((d, "action", str(action),
                                        f"bedrock run '{b['run']}'"))
            elif not (len(action) == 1 and action[0].startswith("[message]")):
                DRIFT_PICKS.append((d, "action", str(action),
                                    "bedrock display-only (run=null)"))
        # Settled text fixes (see SETTLED_TEXT_FIXES): applied to the
        # lore of the named entries, printed — never silent.
        for stale, settled in SETTLED_TEXT_FIXES.get(eid, []):
            assert any(stale in line for line in lore), \
                f"settled fix for {eid}: stale text not found: {stale!r}"
            lore = [line.replace(stale, settled) for line in lore]
            print(f"settled text fix [{eid}]: {stale!r} -> {settled!r}")
        entries.append(dict(id=eid, group=gkey, order=order,
                            order_bedrock=0, icon=icon, label=label,
                            lore=lore, action=action))

    # order-bedrock: flat weight across the whole catalogue in union
    # sequence (the resolver sorts Bedrock by the flat weight; the
    # grouped form builder then groups in that sequence).
    for i, e in enumerate(entries):
        e["order_bedrock"] = i * 10
        emit(e)

    print(f"wrote {len(entries)} catalogue entries")
    print(f"java rows: {len(java)}, bedrock rows: {len(bedrock)}, "
          f"union: {len(entries)}")
    only_j = [d for d in displays if d not in bedrock]
    only_b = [d for d in displays if d not in java]
    print(f"java-only rows ({len(only_j)}): {only_j}")
    print(f"bedrock-only rows ({len(only_b)}): {only_b}")
    print("drift picks (kept | dropped):")
    for row, field, kept, dropped in DRIFT_PICKS:
        print(f"  {row} [{field}]\n    kept:    {kept}\n    dropped: {dropped}")


if __name__ == "__main__":
    sys.exit(main())
