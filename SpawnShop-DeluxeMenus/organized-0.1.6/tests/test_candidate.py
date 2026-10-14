import copy
import hashlib
import importlib.util
from pathlib import Path
import re
import unittest
import yaml

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location('build_candidate', ROOT / 'build_candidate.py')
builder = importlib.util.module_from_spec(spec)
spec.loader.exec_module(builder)

class UniqueLoader(yaml.SafeLoader):
    pass
def unique_mapping(loader, node, deep=False):
    result = {}
    for key_node, value_node in node.value:
        key = loader.construct_object(key_node, deep=deep)
        if key in result:
            raise ValueError('Duplicate YAML key: ' + str(key))
        result[key] = loader.construct_object(value_node, deep=deep)
    return result
UniqueLoader.add_constructor(yaml.resolver.BaseResolver.DEFAULT_MAPPING_TAG, unique_mapping)

def read(path):
    return yaml.load(path.read_text(), Loader=UniqueLoader)

class ShopOrganizationTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.source = read(ROOT / 'baseline/spawnshop.yml')
        cls.pages = {p.stem: read(p) for p in (ROOT / 'candidate/gui_menus').glob('*.yml')}
        cls.offers = {k: v for k, v in cls.source['items'].items() if k not in ('balance', 'back')}

    def test_source_is_exact_verified_live_checkpoint(self):
        self.assertEqual(hashlib.sha256((ROOT / 'baseline/spawnshop.yml').read_bytes()).hexdigest(), builder.EXPECTED_SOURCE)

    def test_yaml_rejects_duplicates(self):
        with self.assertRaises(ValueError):
            yaml.load('a: 1\na: 2\n', Loader=UniqueLoader)
        for p in (ROOT / 'candidate').rglob('*.yml'):
            self.assertIsInstance(read(p), dict)

    def test_exact_six_menu_files_and_only_seven_install_files(self):
        self.assertEqual(set(self.pages), {'spawnshop'} | {'spawnshop_' + c[0] for c in builder.CATEGORIES})
        self.assertEqual(len(list((ROOT / 'candidate').rglob('*.*'))), 7)

    def test_all_21_products_exist_exactly_once(self):
        occurrences = [k for name, page in self.pages.items() if name != 'spawnshop' for k in page['items'] if k not in ('balance', 'back')]
        self.assertEqual(len(occurrences), 21)
        self.assertEqual(len(set(occurrences)), 21)
        self.assertEqual(set(occurrences), set(self.offers))

    def test_every_product_field_except_slot_is_unchanged(self):
        for key, _, _, products in builder.CATEGORIES:
            for product in products:
                before = copy.deepcopy(self.offers[product]); before.pop('slot')
                after = copy.deepcopy(self.pages['spawnshop_' + key]['items'][product]); after.pop('slot')
                self.assertEqual(after, before, product)

    def test_product_text_blocks_only_change_slot(self):
        pattern = r'^  ([a-zA-Z0-9_]+):\n(.*?)(?=^  [a-zA-Z0-9_]+:\n|\Z)'
        original = dict(re.findall(pattern, (ROOT / 'baseline/spawnshop.yml').read_text(), re.M | re.S))
        clean = lambda s: re.sub(r'^    slot: \d+$', '    slot: SLOT', s, flags=re.M)
        for key, _, _, products in builder.CATEGORIES:
            page = dict(re.findall(pattern, (ROOT / f'candidate/gui_menus/spawnshop_{key}.yml').read_text(), re.M | re.S))
            for product in products:
                self.assertEqual(clean(original[product]), clean(page[product]), product)

    def test_categories_match_intended_order(self):
        for key, _, _, products in builder.CATEGORIES:
            items = self.pages['spawnshop_' + key]['items']
            self.assertEqual([p for p in items if p not in ('balance','back')], products)
            self.assertEqual([items[p]['slot'] for p in products], list(range(len(products))))

    def test_minimum_size_contiguous_slots_no_overlaps(self):
        for name, page in self.pages.items():
            self.assertEqual(page['size'], 9, name)
            self.assertEqual(page['inventory_type'], 'DISPENSER', name)
            slots = [i['slot'] for i in page['items'].values()]
            self.assertEqual(sorted(slots), list(range(len(slots))), name)
            self.assertEqual(len(slots), len(set(slots)), name)
            self.assertLessEqual(max(slots), 6, name)
            self.assertTrue(all(isinstance(s, int) and 0 <= s < 9 for s in slots))

    def test_five_and_nine_column_order_has_no_leading_or_internal_gaps(self):
        for page in self.pages.values():
            slots = sorted(i['slot'] for i in page['items'].values())
            for columns in (3, 5, 9):
                positions = [(s // columns, s % columns) for s in slots]
                self.assertEqual(positions, [(i // columns, i % columns) for i in range(len(slots))])
                self.assertLessEqual(max(r for r, c in positions), 2 if columns == 3 else 1)

    def test_root_route_and_existing_global_settings_preserved(self):
        root = self.pages['spawnshop']
        for key in ('menu_title','open_command','register_command','update_interval'):
            self.assertEqual(root[key], self.source[key])
        self.assertEqual(root['open_command'], 'shop')
        self.assertIs(root['register_command'], True)

    def test_all_category_buttons_have_exact_target(self):
        items = self.pages['spawnshop']['items']
        for key, title, _, _ in builder.CATEGORIES:
            item = items['category_' + key]
            self.assertEqual(item['display_name'], '&a&l' + title)
            for click in ('left_click_commands', 'right_click_commands'):
                self.assertEqual(item[click], ['[openguimenu] spawnshop_' + key])

    def test_child_pages_add_no_public_command_or_permission_requirements(self):
        for name, page in self.pages.items():
            if name != 'spawnshop':
                self.assertEqual(page['open_command'], [])
                self.assertNotIn('register_command', page)
            self.assertNotIn('open_requirement', page)

    def test_back_routes_are_reachable(self):
        for name, page in self.pages.items():
            command = '[player] menu' if name == 'spawnshop' else '[openguimenu] spawnshop'
            for side in ('left_click_commands', 'right_click_commands'):
                self.assertEqual(page['items']['back'][side], [command])

    def test_balance_visible_and_updated_on_every_page(self):
        for page in self.pages.values():
            self.assertIn('%vault_eco_balance_2dp%', page['menu_title'])
            self.assertIn('%vault_eco_balance_2dp%', page['items']['balance']['display_name'])
            self.assertIs(page['items']['balance']['update'], True)
            self.assertEqual(page['update_interval'], 1)
            self.assertNotIn('left_click_commands', page['items']['balance'])

    def test_click_handlers_are_single_actions_no_double_dispatch(self):
        for page in self.pages.values():
            for item in page['items'].values():
                self.assertNotIn('click_commands', item)
                self.assertNotIn('shift_left_click_commands', item)
                self.assertNotIn('shift_right_click_commands', item)
                if 'left_click_commands' in item:
                    self.assertEqual(len(item['left_click_commands']), 1)
                    self.assertEqual(item['left_click_commands'], item['right_click_commands'])

    def test_no_new_purchase_give_or_charge_commands(self):
        allowed = {tuple(v['left_click_commands']) for v in self.offers.values()}
        allowed |= {('[player] menu',), ('[openguimenu] spawnshop',)}
        allowed |= {('[openguimenu] spawnshop_' + c[0],) for c in builder.CATEGORIES}
        for page in self.pages.values():
            for item in page['items'].values():
                for field, value in item.items():
                    if field.endswith('_commands'):
                        self.assertIn(tuple(value), allowed)

    def test_special_purchase_review_commands_remain_exact(self):
        page = self.pages['spawnshop_potions_protection']['items']
        self.assertEqual(page['deathprotection_basic']['left_click_commands'], ['[console] deathprotection buy basic %player_name%'])
        self.assertEqual(page['deathpotion']['left_click_commands'], ['[console] deathprotection buy premium %player_name%'])
        self.assertEqual(page['veil_potion']['left_click_commands'], ['[console] mobveil buy %player_name%'])

    def test_live_registration_only_appends_five_unique_entries(self):
        old = read(ROOT / 'baseline/deluxemenus-config.live.yml')
        new = read(ROOT / 'candidate/config.yml')
        for key, value in old.items():
            if key != 'gui_menus':
                self.assertEqual(new[key], value)
        self.assertTrue((ROOT/'candidate/config.yml').read_bytes().startswith((ROOT/'baseline/deluxemenus-config.live.yml').read_bytes()))
        for name, definition in old['gui_menus'].items():
            self.assertEqual(new['gui_menus'][name], definition)
        self.assertEqual(set(new['gui_menus']) - set(old['gui_menus']), set(self.pages) - {'spawnshop'})
        for name in self.pages:
            self.assertEqual(new['gui_menus'][name]['file'], name + '.yml')

    def test_generator_is_deterministic(self):
        before = {p: p.read_bytes() for p in (ROOT/'candidate').rglob('*.yml')}
        builder.build()
        for path, content in before.items():
            self.assertEqual(path.read_bytes(), content)

if __name__ == '__main__':
    unittest.main(verbosity=2)
