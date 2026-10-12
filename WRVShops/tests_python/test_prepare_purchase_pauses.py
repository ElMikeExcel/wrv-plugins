"""Offline pause preparation uses synthetic configs only; never reads live exports."""
import importlib.util
import json
import os
from pathlib import Path
import tempfile
import unittest

MODULE = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location("pause", MODULE / "tools/prepare_purchase_pauses.py")
pause = importlib.util.module_from_spec(spec)
spec.loader.exec_module(pause)

JOBS = b"# jobs-shop-enabled: false is only a comment\nmysql:\n  password: 'fixture-secret-not-logged'\n  jobs-shop-enabled: false\njobs-shop-enabled: true  # pause this one\nother: keep\n"
MENUS = b"# exported menus\ncheck_updates: true\ngui_menus: # registrations\n  spawnshop:\n    file: spawnshop.yml\n  # keep this comment\n  advanced_menu: # example\n    file: advanced_menu.yml\n\n  requirements_menu:\n    file: requirements_menu.yml\n  personal:\n    file: personal.yml\nother:\n  advanced_menu: untouched\n  requirements_menu: untouched\n"
EXPECTED_MENUS = MENUS.replace(b"  advanced_menu: # example\n    file: advanced_menu.yml\n", b"").replace(b"  requirements_menu:\n    file: requirements_menu.yml\n", b"")


class PausePreparationTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.jobs = self.root / "generalConfig.yml"
        self.menus = self.root / "config.yml"
        self.jobs.write_bytes(JOBS)
        self.menus.write_bytes(MENUS)
        self.output = self.root / "prepared"

    def prepare(self):
        return pause.prepare(self.jobs, self.menus, self.output)

    def assert_rejected(self):
        jobs_before, menus_before = self.jobs.read_bytes(), self.menus.read_bytes()
        with self.assertRaises((ValueError, OSError)):
            self.prepare()
        self.assertFalse(self.output.exists())
        self.assertEqual(jobs_before, self.jobs.read_bytes())
        self.assertEqual(menus_before, self.menus.read_bytes())

    def test_only_exact_targets_change_with_lf_and_crlf(self):
        for newline in (b"\n", b"\r\n"):
            with self.subTest(newline=newline):
                jobs = JOBS.replace(b"\n", newline)
                menus = MENUS.replace(b"\n", newline)
                self.assertEqual(jobs.replace(b"jobs-shop-enabled: true", b"jobs-shop-enabled: false"), pause.pause_jobs(jobs))
                self.assertEqual(EXPECTED_MENUS.replace(b"\n", newline), pause.pause_examples(menus))

    def test_backups_sources_hashes_permissions_and_example_files_are_preserved(self):
        examples = self.root / "gui_menus"
        examples.mkdir()
        for name in pause.TARGET_MENUS:
            (examples / (name + ".yml")).write_bytes(b"untouched example fixture")
        before = [(p, p.read_bytes(), p.stat().st_mtime_ns) for p in (self.jobs, self.menus)]
        manifest = self.prepare()
        self.assertFalse(manifest["installed"])
        self.assertFalse(manifest["source_modified"])
        self.assertEqual(JOBS, (self.output / "backups/Jobs/generalConfig.yml").read_bytes())
        self.assertEqual(MENUS, (self.output / "backups/DeluxeMenus/config.yml").read_bytes())
        self.assertEqual(EXPECTED_MENUS, (self.output / "staged/DeluxeMenus/config.yml").read_bytes())
        self.assertEqual(JOBS.replace(b"jobs-shop-enabled: true", b"jobs-shop-enabled: false"),
                         (self.output / "staged/Jobs/generalConfig.yml").read_bytes())
        for path, content, timestamp in before:
            self.assertEqual(content, path.read_bytes())
            self.assertEqual(timestamp, path.stat().st_mtime_ns)
        for name in pause.TARGET_MENUS:
            self.assertEqual(b"untouched example fixture", (examples / (name + ".yml")).read_bytes())
        for path, record in manifest["files"].items():
            data = (self.output / path).read_bytes()
            self.assertEqual(pause.digest(data), record["sha256"])
            self.assertEqual(len(data), record["bytes"])
            self.assertEqual(0o600, (self.output / path).stat().st_mode & 0o777)
        self.assertEqual(0o700, self.output.stat().st_mode & 0o777)
        recorded = json.loads((self.output / "MANIFEST.json").read_text())
        self.assertEqual(manifest, recorded)
        self.assertNotIn("fixture-secret", json.dumps(recorded))

    def test_no_final_newline_or_uniform_child_indent_assumption(self):
        self.assertEqual(b"jobs-shop-enabled: false", pause.pause_jobs(b"jobs-shop-enabled: true"))
        menus = MENUS.replace(b"\n  ", b"\n    ")
        expected = EXPECTED_MENUS.replace(b"\n  ", b"\n    ")
        self.assertEqual(expected.rstrip(b"\n"), pause.pause_examples(menus.rstrip(b"\n")))

    def test_quoted_mapping_keys_are_recognized_not_duplicates_in_disguise(self):
        self.assertEqual(b"'jobs-shop-enabled': false\n", pause.pause_jobs(b"'jobs-shop-enabled': true\n"))
        quoted = MENUS.replace(b"  advanced_menu:", b"  'advanced_menu':").replace(b"  requirements_menu:", b'  "requirements_menu":')
        # The similarly named nested keys outside gui_menus remain exactly as supplied.
        expected = EXPECTED_MENUS.replace(b"  advanced_menu:", b"  'advanced_menu':").replace(b"  requirements_menu:", b'  "requirements_menu":')
        self.assertEqual(expected, pause.pause_examples(quoted))

    def test_expected_file_scalars_accept_ordinary_quotes_and_comments(self):
        quoted = MENUS.replace(b"file: advanced_menu.yml", b'file: "advanced_menu.yml" # reviewed')
        quoted = quoted.replace(b"file: requirements_menu.yml", b"file: 'requirements_menu.yml' # reviewed")
        self.assertEqual(EXPECTED_MENUS, pause.pause_examples(quoted))

    def test_missing_duplicate_or_ambiguous_jobs_flag_refuses_output(self):
        candidates = [b"other: true\n", JOBS + b"jobs-shop-enabled: true\n", JOBS + b"'jobs-shop-enabled': true\n",
                      JOBS.replace(b"jobs-shop-enabled: true", b"jobs-shop-enabled: false"),
                      JOBS.replace(b"jobs-shop-enabled: true", b"jobs-shop-enabled: 'true'"),
                      JOBS.replace(b"jobs-shop-enabled: true", b"jobs-shop-enabled: *flag"),
                      JOBS.replace(b"jobs-shop-enabled: true", b"jobs-shop-enabled: true#not-a-comment")]
        for candidate in candidates:
            with self.subTest(candidate=candidate):
                self.jobs.write_bytes(candidate)
                self.assert_rejected()

    def test_missing_duplicate_or_flow_menu_targets_refuse_output(self):
        candidates = [MENUS.replace(b"gui_menus:", b"not_gui_menus:"), MENUS + b"gui_menus: {}\n",
                      MENUS.replace(b"  advanced_menu:", b"  renamed:"),
                      MENUS.replace(b"  requirements_menu:", b"  renamed:"),
                      MENUS.replace(b"  personal:", b"  advanced_menu:"),
                      MENUS.replace(b"  personal:", b"  'advanced_menu':"),
                      MENUS.replace(b"gui_menus: # registrations", b"gui_menus: &menus"),
                      MENUS.replace(b"  advanced_menu: # example", b"  advanced_menu: {file: advanced_menu.yml}"),
                      MENUS.replace(b"    file: advanced_menu.yml", b"    file: advanced_menu.yml\n    file: other.yml"),
                      MENUS.replace(b"    file: advanced_menu.yml", b"    file: &example advanced_menu.yml"),
                      MENUS.replace(b"    file: advanced_menu.yml", b"    unexpected: advanced_menu.yml"),
                      MENUS.replace(b"    file: advanced_menu.yml", b"    file: # missing value"),
                      MENUS.replace(b"    file: advanced_menu.yml", b"    file: custom.yml"),
                      MENUS.replace(b"    file: personal.yml", b"    file: advanced_menu.yml") ]
        for candidate in candidates:
            with self.subTest(candidate=candidate):
                self.menus.write_bytes(candidate)
                self.assert_rejected()

    def test_nested_context_or_comment_does_not_supply_missing_registration(self):
        self.menus.write_bytes(MENUS.replace(b"  advanced_menu: # example\n    file: advanced_menu.yml\n", b"  # advanced_menu:\n"))
        self.assert_rejected()

    def test_malformed_unsupported_or_merged_yaml_is_refused(self):
        for candidate in (JOBS + b"<<: *defaults\n", b"---\n" + JOBS, JOBS.replace(b"  password", b"\tpassword"),
                          b"\xef\xbb\xbf" + JOBS, JOBS + b"\x00", JOBS + b"\xff"):
            with self.subTest(candidate=candidate):
                self.jobs.write_bytes(candidate)
                self.assert_rejected()

    def test_existing_output_is_never_overwritten(self):
        self.output.mkdir()
        marker = self.output / "keep"
        marker.write_bytes(b"existing evidence")
        with self.assertRaises(ValueError): self.prepare()
        self.assertEqual(b"existing evidence", marker.read_bytes())
        self.assertEqual(["keep"], [p.name for p in self.output.iterdir()])

    def test_input_symlink_or_hardlink_is_refused(self):
        original = self.root / "original.yml"
        self.jobs.rename(original)
        self.jobs.symlink_to(original)
        self.assert_rejected()
        self.jobs.unlink()
        os.link(original, self.jobs)
        self.assert_rejected()

    def test_linked_input_ancestor_is_refused(self):
        real = self.root / "real"
        real.mkdir()
        alias = self.root / "alias"
        alias.symlink_to(real, target_is_directory=True)
        self.jobs = alias / "jobs.yml"
        (real / "jobs.yml").write_bytes(JOBS)
        self.assert_rejected()

    def test_linked_output_or_output_ancestor_is_refused(self):
        real = self.root / "real"
        real.mkdir()
        self.output.symlink_to(real, target_is_directory=True)
        with self.assertRaises(ValueError): self.prepare()
        self.assertEqual([], list(real.iterdir()))
        self.output = self.output / "new-child"
        with self.assertRaises(ValueError): self.prepare()
        self.assertEqual([], list(real.iterdir()))

    def test_source_checkout_is_not_a_destination_for_private_exports(self):
        self.output = MODULE / "must-not-create-private-configs"
        self.assertFalse(self.output.exists())
        with self.assertRaises(ValueError): self.prepare()
        self.assertFalse(self.output.exists())

    def test_other_git_checkout_or_worktree_is_not_a_private_output_destination(self):
        for kind in ("directory", "worktree-file"):
            with self.subTest(kind=kind):
                checkout = self.root / kind
                checkout.mkdir()
                if kind == "directory":
                    (checkout / ".git").mkdir()
                    (checkout / ".git/HEAD").write_text("ref: refs/heads/main\n")
                else: (checkout / ".git").write_text("gitdir: /private/elsewhere")
                self.output = checkout / "private-configs"
                with self.assertRaises(ValueError): self.prepare()
                self.assertFalse(self.output.exists())
