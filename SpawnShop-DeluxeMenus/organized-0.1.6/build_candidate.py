#!/usr/bin/env python3
"""Build an offline, menu-only organization candidate from the pinned live menu."""
from pathlib import Path
import copy
import hashlib
import json
import re
import yaml

ROOT = Path(__file__).resolve().parent
EXPECTED_SOURCE = '44b714b9a46d0f18fadaa787b3b0019746cecb7b492fa39e851f59dc3918f52a'
CATEGORIES = [
    ('food', 'Food', 'BREAD', ['bread', 'steak', 'gcarrot', 'gapple']),
    ('building', 'Building', 'OAK_LOG', ['torch', 'logs', 'glass', 'iron', 'water']),
    ('travel_animals', 'Travel & Animals', 'SADDLE', ['saddle', 'nametag', 'lead', 'pearl', 'rocket']),
    ('enchanting', 'Enchanting', 'ENCHANTED_BOOK', ['xp', 'mending', 'unbreaking', 'efficiency']),
    ('potions_protection', 'Potions & Protection', 'POTION', ['veil_potion', 'deathprotection_basic', 'deathpotion']),
]
DESCRIPTIONS = {
    'food': ['&7Bread, steak, golden carrots', '&7and golden apples.'],
    'building': ['&7Torches, oak logs, glass,', '&7iron ingots and water buckets.'],
    'travel_animals': ['&7Saddles, name tags, leads,', '&7ender pearls and fireworks.'],
    'enchanting': ['&7XP bottles, Mending,', '&7Unbreaking III and Efficiency IV.'],
    'potions_protection': ['&7Veil Potions, Death Protection', '&7and +Death Protection Potions.'],
}

def dump(data):
    return yaml.safe_dump(data, sort_keys=False, allow_unicode=True, width=1000)

def controls(slot, root=False):
    return {
        'balance': {
            'material': 'GOLD_NUGGET', 'slot': slot, 'update': True,
            'display_name': '&e&lCurrent balance: &f$%vault_eco_balance_2dp%',
            'lore': ['&7Choose a category to browse.' if root else '&7Tap an item to review your purchase.',
                     '&7Browsing does not spend money.'],
        },
        'back': {
            'material': 'ARROW', 'slot': slot + 1,
            'display_name': '&c&lBack to Menu' if root else '&c&lBack to Categories',
            'left_click_commands': ['[player] menu' if root else '[openguimenu] spawnshop'],
            'right_click_commands': ['[player] menu' if root else '[openguimenu] spawnshop'],
        },
    }

def build():
    raw = (ROOT / 'baseline/spawnshop.yml').read_bytes()
    if hashlib.sha256(raw).hexdigest() != EXPECTED_SOURCE:
        raise ValueError('Pinned live menu changed; inspect and repin before generation.')
    source = yaml.safe_load(raw)
    text = raw.decode('utf-8')
    blocks = dict(re.findall(r'^  ([a-zA-Z0-9_]+):\n(.*?)(?=^  [a-zA-Z0-9_]+:\n|\Z)', text, re.M | re.S))
    destination = ROOT / 'candidate'
    menus = destination / 'gui_menus'
    menus.mkdir(parents=True, exist_ok=True)
    root = {k: copy.deepcopy(v) for k, v in source.items() if k != 'items'}
    root['size'] = 9
    root['inventory_type'] = 'DISPENSER'
    root['items'] = {}
    for slot, (key, title, material, products) in enumerate(CATEGORIES):
        root['items']['category_' + key] = {
            'material': material, 'slot': slot, 'display_name': '&a&l' + title,
            'lore': DESCRIPTIONS[key] + [f'&e{len(products)} products', '&8Tap to browse. No charge yet.'],
            'left_click_commands': ['[openguimenu] spawnshop_' + key],
            'right_click_commands': ['[openguimenu] spawnshop_' + key],
        }
    root['items'].update(controls(5, root=True))
    header = '# ORGANIZATION CANDIDATE: 21 unchanged offers, grouped into five categories.\n'
    (menus / 'spawnshop.yml').write_text(header + dump(root))
    for key, title, material, products in CATEGORIES:
        page = {'menu_title': '&e$%vault_eco_balance_2dp% &8| &2' + title,
                'open_command': [], 'inventory_type': 'DISPENSER', 'size': 9, 'update_interval': 1}
        content = '# Product blocks are copied from the pinned live menu; only slot changes.\n' + dump(page) + 'items:\n'
        for slot, product in enumerate(products):
            block, changes = re.subn(r'^    slot: \d+$', f'    slot: {slot}', blocks[product], flags=re.M)
            if changes != 1:
                raise ValueError('Unexpected slot in ' + product)
            content += '  ' + product + ':\n' + block
        control_text = dump({'items': controls(len(products))}).split('items:\n', 1)[1]
        content += control_text
        (menus / f'spawnshop_{key}.yml').write_text(content)
    live_config = (ROOT / 'baseline/deluxemenus-config.live.yml').read_text()
    registration = {f'spawnshop_{key}': {'file': f'spawnshop_{key}.yml'} for key, *_ in CATEGORIES}
    parsed = yaml.safe_load(live_config)
    assert not (set(registration) & set(parsed['gui_menus'])), 'Existing registration collision'
    # All current live registrations and comments stay byte-for-byte; append five siblings.
    (destination / 'config.yml').write_text(live_config + ''.join(
        f'  {key}:\n    file: {value["file"]}\n' for key, value in registration.items()))
    (ROOT / 'registration-additions.yml').write_text('# Merge under the existing gui_menus key; do not replace the whole live config with this fragment.\n' + dump({'gui_menus': registration}))
    manifest = {'status': 'OFF-SERVER CANDIDATE; NOT INSTALLED', 'source_menu_sha256': EXPECTED_SOURCE,
                'source_registration_sha256': hashlib.sha256(live_config.encode()).hexdigest(),
                'categories': {key: {'title': title, 'product_ids': products} for key, title, _, products in CATEGORIES},
                'files': {str(p.relative_to(destination)): hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted(destination.rglob('*.yml'))}}
    (ROOT / 'evidence/candidate-manifest.json').write_text(json.dumps(manifest, indent=2) + '\n')
    return manifest

if __name__ == '__main__':
    print(json.dumps(build(), indent=2))
