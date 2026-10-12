"""Repository and packaged-artifact invariants; no live server is contacted."""
import hashlib
from pathlib import Path
import re
import struct
import unittest
import xml.etree.ElementTree as ET
import zipfile

ROOT = Path(__file__).resolve().parents[1]
MODULES = ('BedrockMenu', 'Leaderboard', 'MenuJoin', 'NoPhantoms',
           'RestartCountdown', 'ShopVillager', 'WealthBoard', 'WeekendXP', 'WRVPlayerMap')
NS = {'m': 'http://maven.apache.org/POM/4.0.0'}


def descriptor(module):
    text = (ROOT / module / 'plugin.yml').read_text(encoding='utf-8')
    return {key: re.search(r'^' + key + r':\s*[\'\"]?([^\s\'\"]+)', text, re.M).group(1)
            for key in ('name', 'version', 'main', 'api-version')}


class RepositoryTests(unittest.TestCase):
    def test_no_case_colliding_paths(self):
        seen = {}
        for path in ROOT.rglob('*'):
            rel = path.relative_to(ROOT)
            if set(rel.parts) & {'.git', 'target', 'build', '__pycache__'}:
                continue
            self.assertNotIn(str(rel).casefold(), seen, str(rel))
            seen[str(rel).casefold()] = str(rel)

    def test_all_modules_have_source_and_matching_versions(self):
        root = ET.parse(ROOT / 'pom.xml')
        self.assertEqual(set(MODULES), {m.text for m in root.findall('m:modules/m:module', NS)})
        for module in MODULES:
            with self.subTest(module=module):
                d = descriptor(module)
                pom = ET.parse(ROOT / module / 'pom.xml')
                self.assertEqual(module, d['name'])
                self.assertEqual(pom.find('m:version', NS).text, d['version'])
                self.assertTrue((ROOT / module / 'src' / (d['main'].replace('.', '/') + '.java')).is_file())
                self.assertEqual('1.21', d['api-version'])
                self.assertTrue((ROOT / module / 'README.md').is_file())

    def test_jar_main_version_and_java21_bytecode(self):
        for module in MODULES:
            with self.subTest(module=module):
                d = descriptor(module)
                jar = ROOT / module / 'target' / f"{module}-{d['version']}.jar"
                self.assertTrue(jar.is_file(), f'Run ./mvnw clean verify first: {jar}')
                with zipfile.ZipFile(jar) as z:
                    self.assertIn(d['main'].replace('.', '/') + '.class', z.namelist())
                    self.assertEqual((ROOT / module / 'plugin.yml').read_bytes(), z.read('plugin.yml'))
                    if (ROOT / module / 'config.yml').exists():
                        self.assertEqual((ROOT / module / 'config.yml').read_bytes(), z.read('config.yml'))
                    for entry in z.namelist():
                        if entry.endswith('.class'):
                            data = z.read(entry)
                            self.assertEqual(b'\xca\xfe\xba\xbe', data[:4])
                            self.assertEqual(65, struct.unpack('>H', data[6:8])[0], entry)
                            self.assertTrue(entry.startswith('com/winkreyes'), entry)
                            self.assertFalse(entry.endswith('Test.class'), entry)

    def test_module_build_wrappers_use_repository_maven(self):
        for module in MODULES:
            script = (ROOT / module / "build.sh").read_text()
            self.assertIn("$ROOT/mvnw", script, module)
            self.assertIn(" verify ", script, module)

    def test_original_recovery_artifacts_are_unchanged(self):
        # Git blob hashes from the verified current-main snapshot, before repair.
        expected = {
            'BedrockMenu/classes-1.4.1/META-INF/MANIFEST.MF': 'cf71e50b081d997861f80d837728835f25945906',
            'BedrockMenu/classes-1.4.1/com/winkreyes/bedrockmenu/BedrockMenu.class': '56bb1da1a9ffd2f5fd8f15c999d52c5a8e026e7f',
            'BedrockMenu/classes-1.4.1/com/winkreyes/bedrockmenu/GeyserForms.class': '1c6bfd3315eaf5328e9222bc7a43b71dc35443c0',
            'BedrockMenu/classes-1.4.1/com/winkreyes/bedrockmenu/TeleportMenu$PickHolder.class': '4f3388d196af0fd540e8b3ae9076bbd8efab82eb',
            'BedrockMenu/classes-1.4.1/com/winkreyes/bedrockmenu/TeleportMenu.class': '44494bb454f2f20521f56b5bdbde701497bd78b7',
            'BedrockMenu/classes-1.4.1/plugin.yml': '36dbe7e21fc1df58cdeba9236565f54e7ce62071',
            'BedrockMenu/decompiled-1.4.1/com/winkreyes/bedrockmenu/BedrockMenu.java': '8edfb87522db134124b2efcdce13ebb1632cff3e',
            'BedrockMenu/decompiled-1.4.1/com/winkreyes/bedrockmenu/GeyserForms.java': 'a2f713dfc607736f207aa52615cc8dca4cd4397e',
            'BedrockMenu/decompiled-1.4.1/com/winkreyes/bedrockmenu/TeleportMenu.java': '6c4f99d1e16771b98531478bd0cf74bbd27b6e6d',
            'BedrockMenu/decompiled-1.4.1/summary.txt': 'f22fc307637b032bab344a1f7c05fcd6a78d1c89',
            'BedrockMenu/history/1.3.0-teleport-source/pom.xml': '1ae9ec9990d86098aef39b8b392a6d5f3f696274',
            'BedrockMenu/history/1.3.0-teleport-source/src/main/java/com/winkreyes/bedrockmenu/BedrockMenu.java': '9f2dad97b3ddf023567b702b65ef625565e4d9e6',
            'BedrockMenu/history/1.3.0-teleport-source/src/main/java/com/winkreyes/bedrockmenu/TeleportMenu.java': '6d26c12df9f504026d6b1f303ea942f60c7ccfee',
            'BedrockMenu/history/1.3.0-teleport-source/src/main/resources/config.yml': 'e082ca437c70133f0bddbf2b4c761b6efba9a92c',
            'BedrockMenu/history/1.3.0-teleport-source/src/main/resources/plugin.yml': 'aca00481029e961e0c0fb4d838ffdbf352e016e9',
            'BedrockMenu/tools/Patch.java': 'aee9dbfe1f1634ccd2bcc51c8ccfc0ba8a212f9e',
            'BedrockMenu/tools/plugin-1.4.2.yml': '2a5fb686bb941fadbb3bd2594f09e832b5099dae',
            'mcMMO-TogetherBonus/patches/0001-Add-party-Together-XP-bonus.patch': '01ef56512281cb2a0cd416e79381b56585b2fd7f',
            'mcMMO-TogetherBonus/patches/0002-Together-bonus-own-class-skill-XP-only-gameplay-sour.patch': 'e70d28c5d5850220c505db3525795124f24db732',
        }
        for relative, expected_sha in expected.items():
            with self.subTest(path=relative):
                content = (ROOT / relative).read_bytes()
                actual = hashlib.sha1(f"blob {len(content)}\0".encode() + content).hexdigest()
                self.assertEqual(expected_sha, actual)


if __name__ == '__main__':
    unittest.main()
