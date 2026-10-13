import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

class ServerMenuTest(unittest.TestCase):
    def test_redundant_jobs_route_removed_and_existing_entries_preserved(self):
        text = (ROOT / 'server_menu.yml').read_text()
        self.assertEqual(re.findall(r'^  ([a-z]+):$', text, re.M),
                         ['spawn', 'shop', 'mcmmo', 'info', 'autojoin'])
        self.assertNotIn('Jobs & Money', text)
        self.assertNotIn('[player] jobs browse', text)
        self.assertIn("'[player] shop'", text)
        self.assertIn("'[player] menujoin'", text)
        self.assertEqual(re.findall(r'^    slot: (\d+)$', text, re.M),
                         ['10', '11', '14', '15', '16'])
        self.assertIn('size: 27', text)

if __name__ == '__main__':
    unittest.main()
