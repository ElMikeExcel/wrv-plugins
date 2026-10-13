"""Offline rename/compatibility invariants. No production files are accessed."""
import hashlib
import importlib.util
import json
from pathlib import Path
import re
import tempfile
import unittest
from unittest.mock import patch
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
MODULES = ('BedrockMenu', 'MenuJoin', 'WeekendXP', 'NoPhantoms',
           'RestartCountdown', 'Leaderboard', 'WealthBoard', 'ShopVillager')
NS = {'m': 'http://maven.apache.org/POM/4.0.0'}
spec = importlib.util.spec_from_file_location('stage_excel_data', ROOT / 'tools/stage_excel_data.py')
stage = importlib.util.module_from_spec(spec)
spec.loader.exec_module(stage)


class SourceIdentityTests(unittest.TestCase):
    def test_only_descriptor_name_and_provides_changed_against_approved_sources(self):
        fixture = json.loads((ROOT / 'tests/excel_descriptor_baselines.json').read_text())
        for module in MODULES:
            with self.subTest(module=module):
                text = (ROOT / module / 'plugin.yml').read_text()
                self.assertEqual(1, len(re.findall(r'^name: Excel' + module + r'$', text, re.M)))
                self.assertEqual(1, len(re.findall(r'^provides: \[' + module + r'\]$', text, re.M)))
                unchanged = re.sub(r'^(?:name|provides):.*\n', '', text, flags=re.M)
                self.assertEqual(fixture['sha256_without_name_or_provides'][module],
                                 hashlib.sha256(unchanged.encode()).hexdigest())

    def test_maven_artifact_names_and_workflow_outputs_match_new_identities(self):
        workflow = (ROOT / '.github/workflows/verify.yml').read_text()
        self.assertIn('            EXCEL-PLUGIN-RENAMES.md', workflow)
        self.assertIn('not approved for rename-only deployment', workflow)
        for module in MODULES:
            pom = ET.parse(ROOT / module / 'pom.xml')
            self.assertEqual('Excel' + module, pom.find('m:artifactId', NS).text)
            self.assertIn(f'{module}/target/Excel{module}-*.jar', workflow)
            self.assertNotIn(f'{module}/target/{module}-*.jar', workflow)
        self.assertEqual('ExcelShops', ET.parse(ROOT / 'WRVShops/pom.xml').find('m:artifactId', NS).text)

    def test_menujoin_uses_literal_legacy_persistent_namespace(self):
        source = (ROOT / 'MenuJoin/src/com/winkreyesvalley/menujoin/MenuJoin.java').read_text()
        self.assertEqual(['autojoin_disabled', 'menu_compass'],
                         re.findall(r'new NamespacedKey\("menujoin", "([a-z_]+)"\)', source))
        self.assertNotIn('new NamespacedKey(this,', source)
        tests = (ROOT / 'MenuJoin/tests/com/winkreyesvalley/menujoin/MenuJoinTest.java').read_text()
        self.assertIn('doReturn("ExcelMenuJoin").when(plugin).getName()', tests)
        self.assertIn('new NamespacedKey("menujoin", "menu_compass")', tests)
        self.assertIn('new NamespacedKey("menujoin", "autojoin_disabled")', tests)

    def test_weekendxp_is_installed_102_source_not_downgraded(self):
        self.assertRegex((ROOT / 'WeekendXP/plugin.yml').read_text(), r'(?m)^version: 1\.0\.2$')
        records = json.loads((ROOT / 'tests/weekend_102_source_blobs.json').read_text())
        for relative, expected in records['git_blob_sha1'].items():
            data = (ROOT / 'WeekendXP' / relative).read_bytes()
            actual = hashlib.sha1(f'blob {len(data)}\0'.encode() + data).hexdigest()
            self.assertEqual(expected, actual, relative)

    def test_warning_distinguishes_source_from_live_rename(self):
        text = (ROOT / 'EXCEL-PLUGIN-RENAMES.md').read_text()
        self.assertIn('SOURCE BUILD IS NOT A RENAME-ONLY LIVE DEPLOYMENT', text)
        self.assertIn('menujoin:menu_compass', text)
        self.assertIn('menujoin:autojoin_disabled', text)
        self.assertIn('provides', text)


class OfflineDataCopyTests(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.root = Path(self.temporary.name)
        self.snapshot = self.root / 'snapshot'
        self.snapshot.mkdir()
        for old in stage.DIRECTORIES:
            folder = self.snapshot / old
            (folder / 'empty').mkdir(parents=True)
            (folder / 'nested').mkdir()
            (folder / 'config.yml').write_bytes(b'preserve: exact\r\n# \xc3\xa9\n')
            (folder / '.hidden').write_bytes(b'\0\xffuntouched')
            (folder / 'nested/data.bin').write_bytes(bytes(range(256)))
        self.output = self.root / 'new-output'

    def tearDown(self):
        self.temporary.cleanup()

    def test_copy_preserves_exact_bytes_hidden_files_empty_dirs_and_originals(self):
        original = {old: stage.inventory(self.snapshot / old) for old in stage.DIRECTORIES}
        report = stage.prepare(self.snapshot, self.output)
        self.assertFalse(report['deployed'])
        self.assertEqual('offline-copy-verified', report['status'])
        for old, new in stage.DIRECTORIES.items():
            self.assertEqual(original[old], stage.inventory(self.snapshot / old))
            self.assertEqual(original[old], stage.inventory(self.output / new))
        self.assertTrue((self.output / 'data-copy-manifest.json').is_file())

    def test_existing_output_fails_without_overwrite(self):
        self.output.mkdir()
        (self.output / 'keep').write_text('retained')
        with self.assertRaisesRegex(ValueError, 'Output exists'):
            stage.prepare(self.snapshot, self.output)
        self.assertEqual('retained', (self.output / 'keep').read_text())

    def test_new_name_in_snapshot_fails_before_any_copy(self):
        (self.snapshot / 'ExcelShopVillager').mkdir()
        with self.assertRaisesRegex(ValueError, 'Conflicting target'):
            stage.prepare(self.snapshot, self.output)
        self.assertFalse(self.output.exists())

    def test_casefold_new_name_conflict_fails_before_copy(self):
        (self.snapshot / 'excelshopvillager').mkdir()
        with self.assertRaisesRegex(ValueError, 'Case-conflicting plugin directory'):
            stage.prepare(self.snapshot, self.output)
        self.assertFalse(self.output.exists())

    def test_casefold_old_name_conflict_fails_before_copy(self):
        (self.snapshot / 'bedrockmenu').mkdir()
        with self.assertRaisesRegex(ValueError, 'Case-conflicting plugin directory'):
            stage.prepare(self.snapshot, self.output)
        self.assertFalse(self.output.exists())

    def test_casefold_relative_data_paths_fail_before_copy(self):
        (self.snapshot / 'BedrockMenu/CONFIG.yml').write_text('conflict')
        with self.assertRaisesRegex(ValueError, 'Case-colliding data paths'):
            stage.prepare(self.snapshot, self.output)
        self.assertFalse(self.output.exists())

    def test_casefold_output_conflict_fails_before_copy(self):
        (self.root / self.output.name.upper()).mkdir()
        with self.assertRaisesRegex(ValueError, 'Case-conflicting output'):
            stage.prepare(self.snapshot, self.output)
        self.assertFalse(self.output.exists())

    def test_source_mutation_during_copy_has_no_success_receipt(self):
        real = stage.shutil.copytree
        def mutate(source, destination, *args, **kwargs):
            result = real(source, destination, *args, **kwargs)
            if Path(source).name == 'BedrockMenu':
                with (Path(source) / 'config.yml').open('ab') as stream:
                    stream.write(b'changed after copy')
            return result
        with patch.object(stage.shutil, 'copytree', side_effect=mutate):
            with self.assertRaisesRegex(ValueError, 'Source changed while copying'):
                stage.prepare(self.snapshot, self.output)
        self.assertFalse((self.output / 'data-copy-manifest.json').exists())

    def test_missing_source_fails_before_any_copy(self):
        (self.snapshot / 'ShopVillager').rename(self.snapshot / 'old-shop-data')
        with self.assertRaisesRegex(ValueError, 'Not a real directory'):
            stage.prepare(self.snapshot, self.output)
        self.assertFalse(self.output.exists())

    def test_symlink_fails_before_copy(self):
        (self.snapshot / 'BedrockMenu/link').symlink_to(self.snapshot / 'ShopVillager/config.yml')
        with self.assertRaisesRegex(ValueError, 'Symlink or special file'):
            stage.prepare(self.snapshot, self.output)
        self.assertFalse(self.output.exists())

    def test_copy_must_be_outside_snapshot(self):
        with self.assertRaisesRegex(ValueError, 'outside the snapshot'):
            stage.prepare(self.snapshot, self.snapshot / 'copy')

    def test_corrupt_copy_has_no_success_receipt(self):
        real = stage.shutil.copytree
        def corrupt(source, destination, *args, **kwargs):
            result = real(source, destination, *args, **kwargs)
            (Path(destination) / 'config.yml').write_text('changed')
            return result
        with patch.object(stage.shutil, 'copytree', side_effect=corrupt):
            with self.assertRaisesRegex(ValueError, 'Copied bytes/paths differ'):
                stage.prepare(self.snapshot, self.output)
        self.assertFalse((self.output / 'data-copy-manifest.json').exists())


if __name__ == '__main__':
    unittest.main()
