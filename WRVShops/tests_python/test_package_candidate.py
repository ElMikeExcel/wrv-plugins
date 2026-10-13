"""No server access. Fake JARs exercise package validation, never production evidence."""
import hashlib
import importlib.util
import json
import io
import tarfile
from unittest.mock import patch
from pathlib import Path
import tempfile
import unittest
import zipfile

MODULE = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location("pack", MODULE / "tools/package_candidate.py")
pack = importlib.util.module_from_spec(spec)
spec.loader.exec_module(pack)


class PackageTests(unittest.TestCase):
    def setUp(self):
        fixture = tempfile.TemporaryDirectory()
        self.addCleanup(fixture.cleanup)
        self.guard = Path(fixture.name)
        (self.guard / ".cache").mkdir()
        source_name = "src/com/Acrobot/ChestShop/Confirmation/ConfirmationGate.java"
        (self.guard / source_name).parent.mkdir(parents=True)
        (self.guard / source_name).write_bytes(b"fixture guard source")
        for name in ("NOTICE", "COPYING.LESSER", "bootstrap.sh", "package.py", "pom.xml", "README.md"):
            (self.guard / name).write_bytes(("fixture " + name).encode())
        original = {
            "plugin.yml": b"name: ChestShop\nmain: com.Acrobot.ChestShop.ChestShop\nversion: '3.13-pre-1 (build 474)'\n",
            "com/Acrobot/ChestShop/ChestShop.class": b"original main fixture",
            "com/Acrobot/ChestShop/UUIDs/NameManager.class": b"original account fixture",
            "languages/lang.en.yml": b"original translations fixture",
        }
        original_path = self.guard / ".cache/ChestShop-3.13-pre-1-build474.jar"
        self.write_zip(original_path, original)
        upstream_files = {"pom.xml": b"upstream pom", "LICENSE": b"upstream LGPL",
                          "src/main/java/com/Acrobot/ChestShop/ChestShop.java": b"upstream main source"}
        source_tar = self.guard / ".cache/upstream-source.tar.gz"
        with tarfile.open(source_tar, "w:gz") as tar:
            for name, content in upstream_files.items():
                info = tarfile.TarInfo("ChestShop-3-" + pack.UPSTREAM_REVISION + "/" + name)
                info.size = len(content)
                tar.addfile(info, io.BytesIO(content))
        for name, value in (("GUARD", self.guard), ("UPSTREAM_SHA", pack.sha256(original_path.read_bytes())),
                            ("UPSTREAM_SOURCE_SHA", pack.sha256(source_tar.read_bytes()))):
            replacement = patch.object(pack, name, value)
            replacement.start()
            self.addCleanup(replacement.stop)
        build = {"build_id": pack.GUARD_BUILD, "api_version": 1,
                 "upstream_sha256": pack.UPSTREAM_SHA, "upstream_source_sha256": pack.UPSTREAM_SOURCE_SHA,
                 "upstream_revision": pack.UPSTREAM_REVISION,
                 "overlay_sources": {source_name: pack.sha256((self.guard / source_name).read_bytes())}}
        self.guarded_files = dict(original)
        self.guarded_files["plugin.yml"] = original["plugin.yml"].replace(b"(build 474)'", b"(build 474)+confirmation.1'")
        self.guarded_files["com/Acrobot/ChestShop/Confirmation/ConfirmationGate.class"] = pack.GUARD_BUILD.encode()
        self.guarded_files["META-INF/confirmation-guard/build.json"] = json.dumps(build).encode()
        self.source_files = {"ChestShop-upstream/" + n: b for n, b in upstream_files.items()}
        for name in (source_name, "NOTICE", "COPYING.LESSER", "bootstrap.sh", "package.py", "pom.xml", "README.md"):
            self.source_files["ChestShop-ConfirmationGuard/" + name] = (self.guard / name).read_bytes()
        for name in ("NOTICE", "COPYING.LESSER"):
            self.guarded_files["META-INF/confirmation-guard/" + name] = (self.guard / name).read_bytes()
        for name in ("pom.xml", "mvnw", "BUILDING-GUARD.txt"):
            self.source_files[name] = ("standalone fixture " + name).encode()
        compiled = self.guard / "target/classes/com/Acrobot/ChestShop/Confirmation/ConfirmationGate.class"
        compiled.parent.mkdir(parents=True)
        compiled.write_bytes(pack.GUARD_BUILD.encode())
        self.guard_jar = self.guard / "guarded.jar"
        self.guard_sources = self.guard / "sources.zip"
        self.write_zip(self.guard_jar, self.guarded_files)
        self.write_zip(self.guard_sources, self.source_files)

    @staticmethod
    def write_zip(path, files):
        with zipfile.ZipFile(path, "w") as archive:
            for name, content in files.items():
                archive.writestr(name, content)

    def package(self, *args, **kwargs):
        pair = {"guarded_jar": self.guard_jar, "expected_guarded_sha256": pack.sha256(self.guard_jar.read_bytes()),
                "guard_sources": self.guard_sources, "expected_sources_sha256": pack.sha256(self.guard_sources.read_bytes())}
        pair.update(kwargs)
        return pack.package(*args, **pair)

    def fake_jar(self, path, changes=None):
        files = {n: (MODULE / n).read_bytes() for n in ("plugin.yml", "config.yml", "spawn-catalog.yml", "LICENSE")}
        files.update({"com/winkreyes/shops/WRVShops.class": b"fixture only", "com/winkreyes/shops/LegacyDataMigration.class": b"fixture only", "com/winkreyes/shops/store/ShopRegistry.class": b"fixture wrvshops shop_id chest_identity"})
        for name in ("LegacyShopBridge", "LegacyShopUi", "LegacyTransactionFence"):
            files["com/winkreyes/shops/" + name + ".class"] = b"fixture only"
        files.update(changes or {})
        with zipfile.ZipFile(path, "w") as z:
            for name, value in files.items():
                z.writestr(name, value)
        return hashlib.sha256(path.read_bytes()).hexdigest()

    def test_manifest_and_payload_are_exact_and_reproducible(self):
        with tempfile.TemporaryDirectory() as tmp:
            d = Path(tmp); jar = d / "test.jar"; h = self.fake_jar(jar)
            first = self.package(jar, h, d / "one.zip", "a" * 40)
            self.package(jar, h, d / "two.zip", "a" * 40)
            self.assertEqual((d / "one.zip").read_bytes(), (d / "two.zip").read_bytes())
            self.assertFalse(first["deployment_authorized"])
            self.assertFalse(first["live_catalog_preflight_verified"])
            self.assertTrue(first["trading_requires_pair"])
            self.assertFalse(first["whole_server_confirmation_verified"])
            self.assertEqual(["Jobs item shop", "DeluxeMenus advanced_menu and requirements_menu examples"], first["scope_exclusions"])
            self.assertEqual("ChestShop", first["guarded_chestshop"]["plugin"])
            self.assertEqual("LGPL-2.1", first["guarded_chestshop"]["license"])
            with zipfile.ZipFile(d / "one.zip") as z:
                for name, entry in first["files"].items():
                    self.assertEqual(entry["sha256"], hashlib.sha256(z.read(name)).hexdigest())
                self.assertIn(b"[player] excelshop browse bread", z.read("plugins/DeluxeMenus/gui_menus/spawnshop.yml"))
                self.assertEqual({"plugins/ExcelShops-0.1.5.jar", "plugins/" + pack.GUARD_NAME + ".jar"},
                                 {name for name in z.namelist() if name.endswith(".jar")})
                self.assertEqual(self.guard_sources.read_bytes(), z.read("THIRD-PARTY/ChestShop/" + pack.GUARD_NAME + "-sources.zip"))
                self.assertIn("THIRD-PARTY/ChestShop/COPYING.LESSER", z.namelist())
                self.assertEqual((MODULE / "tools/prepare_purchase_pauses.py").read_bytes(), z.read("tools/prepare_purchase_pauses.py"))
                self.assertIn("PURCHASE-PAUSE-SPEC.md", z.namelist())
                self.assertNotIn("plugins/Jobs/generalConfig.yml", z.namelist())
                self.assertNotIn("plugins/DeluxeMenus/config.yml", z.namelist())
                self.assertTrue(first["offline_pause_preparation_included"])
                self.assertFalse(first["offline_pause_installation_applied"])

    def test_wrong_runtime_hash_does_not_create_package(self):
        with tempfile.TemporaryDirectory() as tmp:
            d = Path(tmp); self.fake_jar(d / "test.jar")
            with self.assertRaises(ValueError):
                self.package(d / "test.jar", "0" * 64, d / "result.zip")
            self.assertFalse((d / "result.zip").exists())

    def test_changed_descriptor_or_resource_is_refused(self):
        for path in ("plugin.yml", "spawn-catalog.yml", "config.yml"):
            with self.subTest(path=path), tempfile.TemporaryDirectory() as tmp:
                d = Path(tmp); h = self.fake_jar(d / "test.jar", {path: b"changed"})
                with self.assertRaises(ValueError):
                    self.package(d / "test.jar", h, d / "result.zip")

    def test_bundled_test_dependency_is_refused(self):
        with tempfile.TemporaryDirectory() as tmp:
            d = Path(tmp); h = self.fake_jar(d / "test.jar", {"org/geysermc/Fake.class": b"fixture"})
            with self.assertRaises(ValueError):
                self.package(d / "test.jar", h, d / "result.zip")

    def test_stable_storage_namespace_is_required(self):
        with tempfile.TemporaryDirectory() as tmp:
            d = Path(tmp); h = self.fake_jar(d / "test.jar", {"com/winkreyes/shops/store/ShopRegistry.class": b"excelshops shop_id chest_identity"})
            with self.assertRaises(ValueError):
                self.package(d / "test.jar", h, d / "result.zip")

    def test_existing_package_is_never_overwritten(self):
        with tempfile.TemporaryDirectory() as tmp:
            d = Path(tmp); h = self.fake_jar(d / "test.jar"); out = d / "result.zip"; out.write_bytes(b"keep")
            with self.assertRaises(ValueError):
                self.package(d / "test.jar", h, out)
            self.assertEqual(b"keep", out.read_bytes())

    def test_invalid_source_commit_is_refused(self):
        with tempfile.TemporaryDirectory() as tmp:
            d = Path(tmp); h = self.fake_jar(d / "test.jar")
            with self.assertRaises(ValueError):
                self.package(d / "test.jar", h, d / "result.zip", "main")

    def test_pair_cannot_be_omitted(self):
        with tempfile.TemporaryDirectory() as tmp:
            d = Path(tmp); h = self.fake_jar(d / "test.jar")
            with self.assertRaises(ValueError):
                pack.package(d / "test.jar", h, d / "result.zip")
            self.assertFalse((d / "result.zip").exists())

    def test_unverified_guard_or_source_hash_is_refused(self):
        for argument in ("expected_guarded_sha256", "expected_sources_sha256"):
            with self.subTest(argument=argument), tempfile.TemporaryDirectory() as tmp:
                d = Path(tmp); h = self.fake_jar(d / "test.jar")
                with self.assertRaises(ValueError):
                    self.package(d / "test.jar", h, d / "result.zip", **{argument: "0" * 64})
                self.assertFalse((d / "result.zip").exists())

    def test_overlay_only_or_unguarded_jar_is_refused(self):
        for content in ({"com/Acrobot/ChestShop/Confirmation/ConfirmationGate.class": b"overlay only"},
                        pack.archive_files((self.guard / ".cache/ChestShop-3.13-pre-1-build474.jar").read_bytes())):
            with self.subTest(entries=len(content)), tempfile.TemporaryDirectory() as tmp:
                d = Path(tmp); h = self.fake_jar(d / "test.jar")
                self.write_zip(self.guard_jar, content)
                with self.assertRaises((ValueError, KeyError)):
                    self.package(d / "test.jar", h, d / "result.zip")
                self.assertFalse((d / "result.zip").exists())

    def test_preserved_upstream_resources_and_classes_cannot_be_removed_or_modified(self):
        for name in ("com/Acrobot/ChestShop/ChestShop.class", "languages/lang.en.yml"):
            for replacement in (None, b"unexpected change"):
                with self.subTest(name=name, replacement=replacement), tempfile.TemporaryDirectory() as tmp:
                    d = Path(tmp); h = self.fake_jar(d / "test.jar")
                    content = dict(self.guarded_files)
                    if replacement is None: del content[name]
                    else: content[name] = replacement
                    self.write_zip(self.guard_jar, content)
                    with self.assertRaises(ValueError): self.package(d / "test.jar", h, d / "result.zip")

    def test_corresponding_source_must_be_complete_and_match_guard(self):
        for name in ("ChestShop-upstream/LICENSE", "ChestShop-upstream/src/main/java/com/Acrobot/ChestShop/ChestShop.java",
                     "ChestShop-ConfirmationGuard/src/com/Acrobot/ChestShop/Confirmation/ConfirmationGate.java",
                     "ChestShop-ConfirmationGuard/COPYING.LESSER", "pom.xml", "BUILDING-GUARD.txt"):
            with self.subTest(name=name), tempfile.TemporaryDirectory() as tmp:
                d = Path(tmp); h = self.fake_jar(d / "test.jar")
                content = dict(self.source_files); del content[name]
                self.write_zip(self.guard_sources, content)
                with self.assertRaises((ValueError, KeyError)): self.package(d / "test.jar", h, d / "result.zip")

    def test_guard_identity_and_provenance_cannot_be_changed(self):
        for name, replacement in (("plugin.yml", b"name: ExcelShops\nversion: unofficial\n"),
                                  ("META-INF/confirmation-guard/build.json", b"{}"),
                                  ("com/Acrobot/ChestShop/Confirmation/ConfirmationGate.class", b"unrecognized guard")):
            with self.subTest(name=name), tempfile.TemporaryDirectory() as tmp:
                d = Path(tmp); h = self.fake_jar(d / "test.jar")
                content = dict(self.guarded_files); content[name] = replacement
                self.write_zip(self.guard_jar, content)
                with self.assertRaises(ValueError): self.package(d / "test.jar", h, d / "result.zip")

    def test_extra_guard_classes_and_unsafe_zip_members_are_refused(self):
        for name in ("com/unreviewed/Plugin.class", "../unsafe", "..\\unsafe", "C:/unsafe"):
            with self.subTest(name=name), tempfile.TemporaryDirectory() as tmp:
                d = Path(tmp); h = self.fake_jar(d / "test.jar")
                self.write_zip(self.guard_jar, {**self.guarded_files, name: b"unexpected"})
                with self.assertRaises(ValueError): self.package(d / "test.jar", h, d / "result.zip")

    def test_guarded_binary_must_match_current_compiled_overlay(self):
        with tempfile.TemporaryDirectory() as tmp:
            d = Path(tmp); h = self.fake_jar(d / "test.jar")
            (self.guard / "target/classes/com/Acrobot/ChestShop/Confirmation/ConfirmationGate.class").write_bytes(b"different compiled guard")
            with self.assertRaises(ValueError): self.package(d / "test.jar", h, d / "result.zip")

    def test_excelshops_cannot_bundle_third_party_chestshop_classes(self):
        with tempfile.TemporaryDirectory() as tmp:
            d = Path(tmp); h = self.fake_jar(d / "test.jar", {"com/Acrobot/ChestShop/ChestShop.class": b"wrong classloader"})
            with self.assertRaises(ValueError): self.package(d / "test.jar", h, d / "result.zip")
