#!/usr/bin/env python3
"""Generates spawnshop.yml for DeluxeMenus.
Retune ALL prices at once: pass a multiplier (1.0 = base list, 1.5 = 50% pricier),
install spawnshop.yml into DeluxeMenus and the matching spawn-catalog.yml into WRVShops.
Restart the staged server to load the authoritative catalog; never deploy only one file.
Base prices assume ~$600/hr for an all-jobs player ($10 per minute of play)."""
import argparse
import math
from pathlib import Path


def validate_multiplier(value):
    """Reject invalid prices before an existing menu can be overwritten."""
    multiplier = float(value)
    if not math.isfinite(multiplier) or multiplier <= 0:
        raise ValueError("price multiplier must be finite and greater than zero")
    return multiplier


def price(p, multiplier=1.0):  # round to a friendly number
    v = p * validate_multiplier(multiplier)
    if not math.isfinite(v) or v <= 0:
        raise ValueError("price multiplier produces an invalid price")
    step = 5 if v < 100 else 10 if v < 1000 else 50
    return int(math.ceil(v / step) * step)

# (key, slot, icon, display name, give-arg, amount, base price, lore line)
ITEMS = [
 # Row 2: food
 ("bread",        10, "BREAD",            "Bread",                 "bread", 16, 40,  "Fill up on a trip"),
 ("steak",        11, "COOKED_BEEF",      "Steak",                 "cooked_beef", 16, 80, "Great food"),
 ("gcarrot",      12, "GOLDEN_CARROT",    "Golden Carrots",        "golden_carrot", 16, 250, "Best normal food"),
 ("gapple",       13, "GOLDEN_APPLE",     "Golden Apple",          "golden_apple", 1, 300, "Heals you fast"),
 # Row 2 right: building
 ("torch",        14, "TORCH",            "Torches",               "torch", 64, 40,  "Light up your base"),
 ("logs",         15, "OAK_LOG",          "Oak Logs",              "oak_log", 64, 150, "Wood for building"),
 ("glass",        16, "GLASS",            "Glass",                 "glass", 64, 180, "See-through windows"),
 # Row 3: useful stuff
 ("iron",         19, "IRON_INGOT",       "Iron Ingots",           "iron_ingot", 16, 300, "Tools and armor"),
 ("water",        20, "WATER_BUCKET",     "Water Bucket",          "water_bucket", 1, 100, "Start a farm"),
 ("saddle",       21, "SADDLE",           "Saddle",                "saddle", 1, 400, "Ride horses and pigs"),
 ("nametag",      22, "NAME_TAG",         "Name Tag",              "name_tag", 1, 350, "Name your pet"),
 ("lead",         23, "LEAD",             "Leads",                 "lead", 2, 150, "Walk your animals"),
 ("pearl",        24, "ENDER_PEARL",      "Ender Pearls",          "ender_pearl", 4, 400, "Throw to teleport"),
 ("rocket",       25, "FIREWORK_ROCKET",  "Fireworks",             "firework_rocket", 16, 200, "Party time"),
 # Row 4: enchanting
 ("xp",           29, "EXPERIENCE_BOTTLE","Bottles o' Enchanting", "experience_bottle", 16, 500, "Free XP"),
 ("mending",      30, "ENCHANTED_BOOK",   "Mending Book",          'enchanted_book[stored_enchantments={"minecraft:mending":1}]', 1, 600, "Fixes gear with XP"),
 ("unbreaking",   32, "ENCHANTED_BOOK",   "Unbreaking III Book",   'enchanted_book[stored_enchantments={"minecraft:unbreaking":3}]', 1, 450, "Gear lasts longer"),
 ("efficiency",   33, "ENCHANTED_BOOK",   "Efficiency IV Book",    'enchanted_book[stored_enchantments={"minecraft:efficiency":4}]', 1, 450, "Dig faster"),
]

def q(s): return "'" + s.replace("'", "''") + "'"
def render(multiplier=1.0):
    """Display-only entry menu: WRVShops owns quotes, validation and final confirmation."""
    multiplier = validate_multiplier(multiplier)
    out = ["# AUTO-GENERATED. Install matching WRVShops spawn-catalog.yml with this menu.",
           "menu_title: '&2&lSpawn Shop'", "open_command: shop", "size: 45", "items:",
           "  balance:", "    material: GOLD_NUGGET", "    slot: 4",
           "    display_name: '&e&lYour money: &f$%vault_eco_balance_formatted%'",
           "    lore:", "      - '&7Tap an item, choose quantity, then confirm.'",
           "  back:", "    material: ARROW", "    slot: 40", "    display_name: '&c&lBack to Menu'",
           "    left_click_commands:", "      - '[player] menu'",
           "    right_click_commands:", "      - '[player] menu'"]
    for key, slot, icon, name, give, amt, base, lore in ITEMS:
        p = price(base, multiplier)
        if p > 100_000_000:
            raise ValueError("bundle price exceeds WRVShops safety limit")
        label = name if amt == 1 else f"{amt} {name}"
        out += [f"  {key}:", f"    material: {icon}", f"    slot: {slot}", f"    amount: {min(amt,64)}",
                f"    display_name: {q('&a&l' + label)}", "    lore:", f"      - {q('&7' + lore)}",
                f"      - {q('&eBundle price: &f$' + str(p))}", "      - '&8Tap to choose quantity. No charge yet.'"]
        for side in ("left", "right"):
            out += [f"    {side}_click_commands:", f"      - {q('[player] wrvshop browse ' + key)}"]
    return "\n".join(out) + "\n"


def render_catalog(multiplier=1.0):
    """Authoritative server-side bundle catalog; prices never come from a player command."""
    multiplier = validate_multiplier(multiplier)
    out = ["# Generated base catalog: prices are PER BUNDLE, retaining the original economy.", "offers:"]
    for key, slot, icon, name, give, amt, base, lore in ITEMS:
        p = price(base, multiplier)
        if p > 100_000_000:
            raise ValueError("bundle price exceeds WRVShops safety limit")
        out += [f"  {key}:", f"    material: {icon}", f"    name: {q(name)}", f"    bundle-size: {amt}", f"    bundle-price: {q(str(p) + '.00')}"]
        if key in ("mending", "unbreaking", "efficiency"):
            level = {"mending": 1, "unbreaking": 3, "efficiency": 4}[key]
            out += [f"    enchantment: {key}", f"    enchantment-level: {level}"]
    return "\n".join(out) + "\n"


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("multiplier", nargs="?", default="1.0", help="positive price multiplier (default: 1.0)")
    parser.add_argument("--output", type=Path, default=Path("spawnshop.yml"), help="output file (default: spawnshop.yml in the current directory)")
    parser.add_argument("--catalog-output", type=Path, default=Path("spawn-catalog.yml"), help="matching WRVShops catalog output")
    args = parser.parse_args(argv)
    try:
        multiplier = validate_multiplier(args.multiplier)
        menu = render(multiplier)
        catalog = render_catalog(multiplier)
        if args.output.resolve() == args.catalog_output.resolve():
            raise ValueError("menu and catalog outputs must be separate files")
    except (ValueError, OverflowError) as exc:
        parser.error(str(exc))
    args.output.write_text(menu, encoding="utf-8")
    args.catalog_output.write_text(catalog, encoding="utf-8")
    print(f"wrote {args.output} and {args.catalog_output} MULT={multiplier}")
    for key, slot, icon, name, give, amt, base, lore in ITEMS:
        print(f"{amt:>3} x {name:<24} ${price(base, multiplier)}")


if __name__ == "__main__":
    main()
