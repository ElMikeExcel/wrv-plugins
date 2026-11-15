#!/usr/bin/env python3
"""Generate the Phase 0 fixture entry files (one per registry entry) from the
two sources of record:
  - Java column: the live merged server_menu.yml (fixtures/server_menu.yml,
    SHA-256 132442bf... since the Nov 1, 2026 re-base; originally
    86b51650...) — icons, labels, lore, actions, slot order.
  - Bedrock column: ExcelBedrockMenu 1.5.27 BUTTONS/COMMANDS — flat order
    (order-bedrock = index * 10, a GLOBAL flat weight; see PROGRESS.md).
Groups follow the Java rows / Bedrock groupColor table (identical grouping).
ExcelMenu 2.0 Phase 0 — fixture generator (build tooling, not shipped).
"""
import pathlib

OUT = pathlib.Path(__file__).resolve().parent.parent / "src/main/resources/entries"

HDR = ("# ExcelMenu 2.0 registry entry — production data converted from the live\n"
       "# server_menu.yml (Java) + ExcelBedrockMenu 1.5.27 BUTTONS (Bedrock order).\n")

def q(s):
    return "'" + s.replace("'", "''") + "'"

def emit(e):
    lines = [HDR.rstrip("\n")]
    lines.append(f"id: {e['id']}")
    lines.append(f"menu: {e.get('menu', 'server_menu')}")
    lines.append(f"group: {e['group']}")
    lines.append(f"order: {e['order']}")
    if 'order_bedrock' in e:
        lines.append(f"order-bedrock: {e['order_bedrock']}")
    lines.append(f"icon: {e['icon']}")
    lines.append(f"label: {q(e['label'])}")
    if e.get('lore'):
        lines.append("lore:")
        for l in e['lore']:
            lines.append(f"  - {q(l)}")
    if e.get('action'):
        lines.append("action:")
        for a in e['action']:
            lines.append(f"  - {q(a)}")
    if e.get('platforms'):
        lines.append("platforms: [" + ", ".join(e['platforms']) + "]")
    if e.get('requires_plugin'):
        lines.append(f"requires-plugin: {e['requires_plugin']}")
    if e.get('requires_permission'):
        lines.append(f"requires-permission: {e['requires_permission']}")
    if e.get('status_placeholder'):
        lines.append(f"status-placeholder: {q(e['status_placeholder'])}")
    if e.get('states'):
        lines.append("states:")
        for key, v in e['states'].items():
            # State keys are QUOTED: unquoted ON/OFF/true/false are YAML
            # booleans, not text (the loader refuses them loudly).
            lines.append(f"  '{key}':")
            lines.append(f"    icon: {v['icon']}")
            lines.append(f"    label: {q(v['label'])}")
            if v.get('lore'):
                lines.append("    lore:")
                for l in v['lore']:
                    lines.append(f"      - {q(l)}")
            if v.get('action'):
                lines.append("    action:")
                for a in v['action']:
                    lines.append(f"      - {q(a)}")
    if e.get('visible_from'):
        lines.append(f"visible-from: {e['visible_from']}")
    if e.get('visible_until'):
        lines.append(f"visible-until: {e['visible_until']}")
    (OUT / f"{e['id']}.yml").write_text("\n".join(lines) + "\n", encoding="utf-8")

E = []
# ---- Travel (Java slots 1-4; Bedrock idx 0,1,2,7) ----
E.append(dict(id="spawn", group="travel", order=10, order_bedrock=0, icon="COMPASS",
    label="&a&lSpawn", lore=["&7Teleport to spawn", "&7(asks Yes or Cancel first)"],
    action=["[openguimenu] teleport_confirm_spawn"]))
E.append(dict(id="guild", group="travel", order=20, order_bedrock=70, icon="BELL",
    label="&a&lAdventurers Guild",
    lore=["&7Teleport to the EliteMobs", "&7guild hall"],
    action=["[openguimenu] teleport_confirm_guild"],
    requires_plugin="ExcelGuildGate", status_placeholder="%guildgate_unlocked%",
    states={
      "false": dict(icon="BELL", label="&8&lAdventurers Guild",
                    lore=["&7Sealed by the five challenges", "&7Tap to see the Road to the Guild"],
                    action=["[player] seals"]),
      "true": dict(icon="BELL", label="&a&lAdventurers Guild",
                    lore=["&7Teleport to the EliteMobs", "&7guild hall"],
                    action=["[openguimenu] teleport_confirm_guild"]),
      "default": dict(icon="BELL", label="&8&lAdventurers Guild",
                    lore=["&7Sealed by the five challenges", "&7Tap to see the Road to the Guild"],
                    action=["[player] seals"]),
    }))
E.append(dict(id="tpmenu", group="travel", order=30, order_bedrock=10, icon="ENDER_PEARL",
    label="&a&lTeleport to a player", lore=["&7Pick a player and send", "&7them a teleport request"],
    action=["[close]", "[player] tpmenu"]))
E.append(dict(id="party", group="travel", order=40, order_bedrock=20, icon="PLAYER_HEAD",
    label="&a&lParty", lore=["&7Invite family to a party", "&7and see who is in yours"],
    action=["[player] excelparty"]))
# ---- Me (Java slots 10-17; Bedrock idx 3,4,8,9,10,11,12,13) ----
E.append(dict(id="skills", group="me", order=10, order_bedrock=90, icon="ENCHANTED_BOOK",
    label="&a&lSkills & Perks",
    lore=["&7Your mcMMO stats and every", "&7perk ladder, in game and on the website"],
    action=["[openguimenu] skills_perks"]))
E.append(dict(id="spellbook", group="me", order=20, order_bedrock=40, icon="WRITABLE_BOOK",
    label="&d&lSpellbook",
    lore=["&7Your Magic wand's spells:", "&7cost, cooldown, range and rank",
          "&7Works while you hold this compass"],
    action=["[close]", "[player] spellinfo"]))
E.append(dict(id="jobs", group="me", order=30, order_bedrock=80, icon="IRON_PICKAXE",
    label="&a&lJobs & Money", lore=["&7Browse the jobs that pay you", "&7and join one"],
    action=["[close]", "[player] jobs browse"]))
E.append(dict(id="leaderboard", group="me", order=40, order_bedrock=100, icon="BEACON",
    label="&a&lLeaderboard", lore=["&7Who is on top in the valley"],
    action=["[close]", "[player] leaderboard"]))
E.append(dict(id="lostfound", group="me", order=50, order_bedrock=110, icon="BUNDLE",
    label="&a&lLost & Found",
    lore=["&7Bring back something you lost", "&7to lava, fire, the void or despawning",
          "&7Shows the price before you pay"],
    action=["[close]", "[player] lostfound"]))
E.append(dict(id="utility_pouch", group="me", order=60, order_bedrock=30, icon="CHEST",
    label="&6Utility Pouch",
    lore=["&7Park your Server Menu compass", "&7and your Magic wand here."],
    action=["[player] pouch", "[close]"]))
E.append(dict(id="trash", group="me", order=70, order_bedrock=120, icon="CAULDRON",
    label="&a&lTrash Bin",
    lore=["&7Throw items away.", "&7Changed your mind? Recover within 5 min."],
    action=["[close]", "[player] trash"]))
E.append(dict(id="boss_rewards", group="me", order=80, order_bedrock=130, icon="CHEST",
    label="&6&lMy Boss Rewards",
    lore=["&7Collect your earned personal boss rewards", "&7Claims wait if your inventory is full",
          "&7Close your wand hotbar before collecting"],
    action=["[close]", "[player] bossrewards"]))
# ---- Shops (Java slots 19-20; Bedrock idx 5,6) ----
E.append(dict(id="shop", group="shops", order=10, order_bedrock=50, icon="EMERALD",
    label="&a&lExcel Shop", lore=["&7Spend your Jobs money", "&7on useful items"],
    action=["[player] shop"]))
E.append(dict(id="cosmetics", group="shops", order=20, order_bedrock=60, icon="AMETHYST_SHARD",
    label="&d&lMy Cosmetics", lore=["&7See and wear the cosmetics you own"],
    action=["[close]", "[player] cosmetics"]))
# ---- Help (Java slots 28-34; Bedrock idx 14,15,16,17,18 + halloween new) ----
E.append(dict(id="whatsnew", group="help", order=10, order_bedrock=170, icon="NETHER_STAR",
    label="&e&lWhat's New", lore=["&7The newest changes", "&7and new commands"],
    action=["[close]", "[player] whatsnew"]))
E.append(dict(id="ask", group="help", order=20, order_bedrock=180, icon="OAK_SIGN",
    label="&a&lAsk the Guide",
    lore=["&7Type /ask and your question in chat", "&7The Guide answers from the server's own pages"],
    action=["[message] &7Type &e/ask &7and your question in chat, or start a message with &eGuide,"]))
E.append(dict(id="commands", group="help", order=30, order_bedrock=150, icon="KNOWLEDGE_BOOK",
    label="&a&lServer Commands", lore=["&7Every command you can use"],
    action=["[openguimenu] commands_menu"]))
E.append(dict(id="info", group="help", order=40, order_bedrock=140, icon="BOOK",
    label="&a&lServer Info", lore=["&7What runs on this server"],
    action=["[player] motd"]))
E.append(dict(id="website", group="help", order=50, order_bedrock=160, icon="ENDER_EYE",
    label="&a&lServer Website", lore=["&7Guides, commands and news", "&7Tap for the address"],
    action=['[json] {"text":"» Server Website: elmikeexcel.github.io/winkreyesvalley","color":"aqua","underlined":true,"clickEvent":{"action":"open_url","value":"https://elmikeexcel.github.io/winkreyesvalley/"},"hoverEvent":{"action":"show_text","value":"Open the server website"}}',
            "[message] &bhttps://elmikeexcel.github.io/winkreyesvalley/",
            "[message] &7Short, easy to type: &btinyurl.com/winkreyesvalley"]))
E.append(dict(id="halloween", group="help", order=60, order_bedrock=185, icon="JACK_O_LANTERN",
    label="&6&lHalloween",
    lore=["&7The Halloween event is live through Nov 5", "&7Witching Hour: 7 PM - 2 AM Central",
          "&7Tap for your scare stats"],
    action=["[close]", "[player] halloween stats"],
    visible_from="2026-10-31", visible_until="2026-11-05"))
# Added in the Nov 1, 2026 re-base: the live server_menu.yml gained this
# item AFTER the original conversion (ExcelHalloweenToggles same-pass
# surface, slot 34 beside 'halloween'). Transcribed EXACTLY as live;
# dated with the same event window so it retires with 'halloween' in
# the post-Nov 5 cleanup. order-bedrock 186 slots it directly after
# 'halloween' (185) in the Bedrock flat order.
E.append(dict(id="halloween_scares", group="help", order=70, order_bedrock=186, icon="CARVED_PUMPKIN",
    label="&6&lHalloween Scares — your switches",
    lore=["&7Pick which Halloween scares can find you:",
          "&7pumpkin hat, Slenderman, Herobrine and more",
          "&7The event runs through Nov 5"],
    action=["[openguimenu] halloween_scares"],
    visible_from="2026-10-31", visible_until="2026-11-05"))
# ---- Settings (Java slots 37-43; Bedrock idx 19,20,21,22,23,24) ----
E.append(dict(id="autosort", group="settings", order=10, order_bedrock=200, icon="HOPPER",
    label="&e&lAuto-sort my chests: on/off",
    lore=["&7Tidies your chests, barrels, shulker", "&7boxes and ender chest when you open them",
          "&7Click to switch it on or off", "&7Chat tells you which it is now"],
    action=["[close]", "[player] autosort"]))
E.append(dict(id="makersmark_settings", group="settings", order=20, order_bedrock=210, icon="NAME_TAG",
    label="&7&lMaker's Mark settings",
    lore=["&7Crafted / found signatures and gear counters", "&7Choose your on/off settings"],
    action=["[openguimenu] makersmark_settings"]))
E.append(dict(id="autojoin", group="settings", order=30, order_bedrock=220, icon="LIME_DYE",
    label="&a&lAuto-open on join: ✔ ON",
    lore=["&7This menu opens when you join", "&7Click to turn it off"],
    action=["[player] menujoin", "[refresh]"],
    status_placeholder="%menujoin_status%",
    states={
      "ON": dict(icon="LIME_DYE", label="&a&lAuto-open on join: ✔ ON",
                 lore=["&7This menu opens when you join", "&7Click to turn it off"],
                 action=["[player] menujoin", "[refresh]"]),
      "OFF": dict(icon="GRAY_DYE", label="&7&lAuto-open on join: &c&l✘ OFF",
                 lore=["&7This menu stays closed when you join", "&7Click to turn it on"],
                 action=["[player] menujoin", "[refresh]"]),
      "default": dict(icon="GRAY_DYE", label="&7&lAuto-open on join",
                 lore=["&7This menu opens when you join"],
                 action=["[player] menujoin", "[refresh]"]),
    }))
E.append(dict(id="wandstyle", group="settings", order=40, order_bedrock=190, icon="BLAZE_ROD",
    label="&e&lWand controls",
    lore=["&7Pick how your Magic wand works:", "&7Classic, Sneak bars, Cycle,",
          "&7Tap to pick or Tap bar", "&7Click to choose, switch any time",
          "&7Same as typing &e/wandstyle"],
    action=["[close]", "[player] wandstyle"]))
E.append(dict(id="silkwarnings", group="settings", order=50, order_bedrock=230, icon="LIME_DYE",
    label="&a&lSilk Touch Warnings: ON",
    lore=["&7Warn when storing Silk Touch items", "&7or shulkers containing them in your ender chest",
          "&7Click to turn notifications OFF"],
    action=["[player] excelsilkwatch:silkwarnings toggle", "[refresh]"],
    requires_plugin="ExcelSilkWatch", requires_permission="excelsilkwatch.toggle",
    status_placeholder="%silkwarnings_status%",
    states={
      "ON": dict(icon="LIME_DYE", label="&a&lSilk Touch Warnings: ON",
                 lore=["&7Warn when storing Silk Touch items",
                       "&7or shulkers containing them in your ender chest",
                       "&7Click to turn notifications OFF"],
                 action=["[player] excelsilkwatch:silkwarnings toggle", "[refresh]"]),
      "OFF": dict(icon="GRAY_DYE", label="&7&lSilk Touch Warnings: OFF",
                 lore=["&7Notifications only; your items move normally",
                       "&7Click to turn notifications ON"],
                 action=["[player] excelsilkwatch:silkwarnings toggle", "[refresh]"]),
      "default": dict(icon="GRAY_DYE", label="&7&lSilk Touch Warnings: unavailable",
                 lore=["&7Warning plugin or status bridge is unavailable",
                       "&7Try again later or ask server staff"]),
    }))
E.append(dict(id="aimassist", group="settings", order=60, order_bedrock=240, icon="LIME_DYE",
    label="&a&lAim Assist: ON",
    lore=["&7Shots bend toward a hostile mob", "&7you are already aiming at",
          "&7Click to turn aim assist OFF"],
    action=["[player] excelaimassist:aimassist toggle", "[refresh]"],
    requires_plugin="ExcelAimAssist", requires_permission="excelaimassist.toggle",
    status_placeholder="%aimassist_status%",
    states={
      "ON": dict(icon="LIME_DYE", label="&a&lAim Assist: ON",
                 lore=["&7Shots bend toward a hostile mob", "&7you are already aiming at",
                       "&7Click to turn aim assist OFF"],
                 action=["[player] excelaimassist:aimassist toggle", "[refresh]"]),
      "OFF": dict(icon="GRAY_DYE", label="&7&lAim Assist: OFF",
                 lore=["&7Bows, crossbows and thrown tridents", "&7Click to turn aim assist ON"],
                 action=["[player] excelaimassist:aimassist toggle", "[refresh]"]),
      "default": dict(icon="GRAY_DYE", label="&7&lAim Assist: unavailable",
                 lore=["&7Aim assist plugin or status bridge is unavailable",
                       "&7Try again later or ask server staff"]),
    }))

for e in E:
    emit(e)
print(f"wrote {len(E)} entry files to {OUT}")
