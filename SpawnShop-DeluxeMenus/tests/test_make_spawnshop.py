"""Standard-library tests for the preserved shop economy and generator."""
import importlib.util
from pathlib import Path
import re
import subprocess
import sys
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]
SCRIPT = ROOT / "make_spawnshop.py"
spec = importlib.util.spec_from_file_location("make_spawnshop", SCRIPT)
shop = importlib.util.module_from_spec(spec)
spec.loader.exec_module(shop)


class SpawnShopTest(unittest.TestCase):
    def test_default_output_matches_preserved_menu_byte_for_byte(self):
        self.assertEqual(shop.render().encode("utf-8"), (ROOT / "spawnshop.yml").read_bytes())

    def test_shop_open_command_is_registered_for_programmatic_menu_buttons(self):
        # BedrockMenu and DeluxeMenus [player] actions use performCommand,
        # which bypasses PlayerCommandPreprocessEvent-only menu aliases.
        for multiplier in (0.5, 1.0, 1.25, 2.0):
            with self.subTest(multiplier=multiplier):
                lines = shop.render(multiplier).splitlines()
                self.assertEqual(lines.count("open_command: shop"), 1)
                self.assertEqual(lines.count("register_command: true"), 1)
                self.assertEqual(lines.index("register_command: true"),
                                 lines.index("open_command: shop") + 1)

    def test_balance_is_visible_without_hover_and_updates_icon(self):
        menu = shop.render()
        self.assertIn("menu_title: '&eBalance $%vault_eco_balance_fixed% &8| &2Spawn Shop'", menu)
        self.assertIn("update_interval: 1", menu)
        self.assertIn("    update: true", menu)
        self.assertIn("Current balance: &f$%vault_eco_balance_fixed%", menu)

    def test_import_does_not_generate_a_menu(self):
        with tempfile.TemporaryDirectory() as directory:
            result = subprocess.run([sys.executable, "-c", f"import runpy; runpy.run_path({str(SCRIPT)!r})"], cwd=directory, capture_output=True, text=True)
            self.assertEqual(result.returncode, 0, result.stderr)
            self.assertEqual(result.stdout, "")
            self.assertEqual(list(Path(directory).iterdir()), [])

    def test_default_cli_preserves_menu(self):
        with tempfile.TemporaryDirectory() as directory:
            result = subprocess.run([sys.executable, str(SCRIPT)], cwd=directory, capture_output=True, text=True)
            self.assertEqual(result.returncode, 0, result.stderr)
            self.assertEqual((Path(directory) / "spawnshop.yml").read_bytes(), (ROOT / "spawnshop.yml").read_bytes())

    def test_invalid_cli_prices_do_not_overwrite_existing_file(self):
        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory) / "spawnshop.yml"
            for value in ("0", "-1", "nan", "inf", "-inf", "1e309", "1e308", "invalid"):
                with self.subTest(multiplier=value):
                    output.write_text("preserve me", encoding="utf-8")
                    result = subprocess.run([sys.executable, str(SCRIPT), value], cwd=directory, capture_output=True, text=True)
                    self.assertNotEqual(result.returncode, 0)
                    self.assertEqual(output.read_text(encoding="utf-8"), "preserve me")
                    self.assertNotIn("Traceback", result.stderr)

    def test_explicit_output_and_scaled_prices(self):
        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory) / "retuned.yml"
            result = subprocess.run([sys.executable, str(SCRIPT), "1.25", "--output", str(output)], cwd=directory, capture_output=True, text=True)
            self.assertEqual(result.returncode, 0, result.stderr)
            self.assertEqual(output.read_text(encoding="utf-8"), shop.render(1.25))
            self.assertFalse((Path(directory) / "spawnshop.yml").exists())

    def test_original_catalog_and_prices_are_preserved(self):
        self.assertEqual({item[0]: item[6] for item in shop.ITEMS}, {
            "bread": 40, "steak": 80, "gcarrot": 250, "gapple": 300,
            "torch": 40, "logs": 150, "glass": 180, "iron": 300,
            "water": 100, "saddle": 400, "nametag": 350, "lead": 150,
            "pearl": 400, "rocket": 200, "xp": 500, "mending": 600,
            "unbreaking": 450, "efficiency": 450,
        })
        slots = [4, 40] + [item[1] for item in shop.ITEMS]
        self.assertEqual(len(slots), len(set(slots)))
        self.assertTrue(all(0 <= slot < 45 for slot in slots))
        self.assertNotIn("netherite", shop.render())
        self.assertNotIn("elytra", shop.render())
        self.assertNotIn("[givemoney]", shop.render())

    def test_every_click_opens_server_side_confirmation_without_mutation(self):
        for multiplier in (0.5, 1.0, 1.25, 2.0):
            rendered = shop.render(multiplier)
            for key, slot, icon, name, give, amount, base, lore in shop.ITEMS:
                block = re.search(rf"^  {key}:\n(.*?)(?=^  \w+:|\Z)", rendered, re.M | re.S).group(1)
                self.assertEqual(block.count(f"[player] excelshop browse {key}"), 2)
                self.assertIn(f"&eBundle price: &f${shop.price(base, multiplier)}", block)
            for forbidden in ("[takemoney]", "minecraft:give", "[givemoney]", "[console]", "[refresh]"):
                self.assertNotIn(forbidden, rendered)

    def test_authoritative_catalog_matches_packaged_resource(self):
        self.assertEqual(shop.render_catalog(), (ROOT / "spawn-catalog.yml").read_text())
        self.assertEqual(shop.render_catalog(), (ROOT.parent / "WRVShops/spawn-catalog.yml").read_text())
        for item in shop.ITEMS:
            self.assertIn("bundle-price: '" + str(shop.price(item[6])) + ".00'", shop.render_catalog())

    def test_catalog_refuses_unsafe_prices(self):
        with self.assertRaises(ValueError):
            shop.render_catalog(1e10)

    def test_rounding_matches_existing_friendly_steps(self):
        for base, multiplier, expected in ((40, 1.25, 50), (80, 1.25, 100), (250, 1.25, 320), (600, 1.25, 750), (600, 2.0, 1200)):
            with self.subTest(base=base, multiplier=multiplier):
                self.assertEqual(shop.price(base, multiplier), expected)

    def test_yaml_single_quote_escaping(self):
        self.assertEqual(shop.q("Bottles o' Enchanting"), "'Bottles o'' Enchanting'")
        self.assertIn("display_name: '&a&l16 Bottles o'' Enchanting'", shop.render())


if __name__ == "__main__":
    unittest.main()
