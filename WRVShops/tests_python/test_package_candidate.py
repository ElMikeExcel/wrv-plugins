"""No server access. Fake JARs exercise package validation, never production evidence."""
import hashlib
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest
import zipfile

MODULE = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location("pack", MODULE / "tools/package_candidate.py")
pack = importlib.util.module_from_spec(spec)
spec.loader.exec_module(pack)


class PackageTests(unittest.TestCase):
    def fake_jar(self, path, changes=None):
        files = {n: (MODULE / n).read_bytes() for n in ("plugin.yml", "config.yml", "spawn-catalog.yml", "LICENSE")}
        files.update({"com/winkreyes/shops/WRVShops.class": b"fixture only", "com/winkreyes/shops/LegacyDataMigration.class": b"fixture only", "com/winkreyes/shops/store/ShopRegistry.class": b"fixture wrvshops shop_id chest_identity"})
        files.update(changes or {})
        with zipfile.ZipFile(path, "w") as z:
            for name, value in files.items():
                z.writestr(name, value)
        return hashlib.sha256(path.read_bytes()).hexdigest()

    def test_manifest_and_payload_are_exact_and_reproducible(self):
        with tempfile.TemporaryDirectory() as tmp:
            d = Path(tmp); jar = d / "test.jar"; h = self.fake_jar(jar)
            first = pack.package(jar, h, d / "one.zip", "a" * 40)
            pack.package(jar, h, d / "two.zip", "a" * 40)
            self.assertEqual((d / "one.zip").read_bytes(), (d / "two.zip").read_bytes())
            self.assertFalse(first["deployment_authorized"])
            self.assertFalse(first["live_catalog_preflight_verified"])
            with zipfile.ZipFile(d / "one.zip") as z:
                for name, entry in first["files"].items():
                    self.assertEqual(entry["sha256"], hashlib.sha256(z.read(name)).hexdigest())
                self.assertIn(b"[player] excelshop browse bread", z.read("plugins/DeluxeMenus/gui_menus/spawnshop.yml"))

    def test_wrong_runtime_hash_does_not_create_package(self):
        with tempfile.TemporaryDirectory() as tmp:
            d = Path(tmp); self.fake_jar(d / "test.jar")
            with self.assertRaises(ValueError):
                pack.package(d / "test.jar", "0" * 64, d / "result.zip")
            self.assertFalse((d / "result.zip").exists())

    def test_changed_descriptor_or_resource_is_refused(self):
        for path in ("plugin.yml", "spawn-catalog.yml", "config.yml"):
            with self.subTest(path=path), tempfile.TemporaryDirectory() as tmp:
                d = Path(tmp); h = self.fake_jar(d / "test.jar", {path: b"changed"})
                with self.assertRaises(ValueError):
                    pack.package(d / "test.jar", h, d / "result.zip")

    def test_bundled_test_dependency_is_refused(self):
        with tempfile.TemporaryDirectory() as tmp:
            d = Path(tmp); h = self.fake_jar(d / "test.jar", {"org/geysermc/Fake.class": b"fixture"})
            with self.assertRaises(ValueError):
                pack.package(d / "test.jar", h, d / "result.zip")

    def test_stable_storage_namespace_is_required(self):
        with tempfile.TemporaryDirectory() as tmp:
            d = Path(tmp); h = self.fake_jar(d / "test.jar", {"com/winkreyes/shops/store/ShopRegistry.class": b"excelshops shop_id chest_identity"})
            with self.assertRaises(ValueError):
                pack.package(d / "test.jar", h, d / "result.zip")

    def test_existing_package_is_never_overwritten(self):
        with tempfile.TemporaryDirectory() as tmp:
            d = Path(tmp); h = self.fake_jar(d / "test.jar"); out = d / "result.zip"; out.write_bytes(b"keep")
            with self.assertRaises(ValueError):
                pack.package(d / "test.jar", h, out)
            self.assertEqual(b"keep", out.read_bytes())

    def test_invalid_source_commit_is_refused(self):
        with tempfile.TemporaryDirectory() as tmp:
            d = Path(tmp); h = self.fake_jar(d / "test.jar")
            with self.assertRaises(ValueError):
                pack.package(d / "test.jar", h, d / "result.zip", "main")
