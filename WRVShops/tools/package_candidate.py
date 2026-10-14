#!/usr/bin/env python3
"""Package a verified ExcelShops candidate locally. Never contacts or changes a server."""
import argparse
import hashlib
import importlib.util
import json
import io
import tarfile
from pathlib import Path
import re
import zipfile

MODULE = Path(__file__).resolve().parents[1]
ROOT = MODULE.parent
VERSION = "0.1.6"
GUARD = ROOT / "ChestShop-ConfirmationGuard"
GUARD_NAME = "ChestShop-3.13-pre-1-build474-confirmation.1"
GUARD_BUILD = "chestshop474-confirmation.1"
UPSTREAM_SHA = "b31a40e9561d399d8b04fada4e863ae28c523618976ff3a3b1a6f16fe3aa8ab2"
UPSTREAM_SOURCE_SHA = "00f15e49bedd11c8300dd4ffaee6a783b92b26af39f5ba00a7683814c7564501"
UPSTREAM_REVISION = "dde7ee2155303714c2d5054deeccef4618e3939c"


def sha256(data):
    return hashlib.sha256(data).hexdigest()


def verified_file(path, expected):
    if not isinstance(expected, str) or not re.fullmatch(r"[a-f0-9]{64}", expected):
        raise ValueError("Each artifact requires its verified SHA-256")
    data = Path(path).read_bytes()
    if sha256(data) != expected:
        raise ValueError("Artifact does not match its expected SHA-256: " + str(path))
    return data


def archive_files(payload):
    with zipfile.ZipFile(io.BytesIO(payload)) as archive:
        names = archive.namelist()
        if len(names) != len(set(names)) or any(n.startswith("/") or "\\" in n or re.match(r"^[A-Za-z]:", n)
                                             or ".." in Path(n).parts for n in names):
            raise ValueError("Duplicate or unsafe archive entry")
        return {entry.filename: archive.read(entry) for entry in archive.infolist() if not entry.is_dir()}


def validate_guard(guarded_jar, expected_guarded_sha256, guard_sources, expected_sources_sha256):
    if guarded_jar is None or guard_sources is None:
        raise ValueError("The full guarded ChestShop JAR and corresponding-source ZIP are required")
    binary = verified_file(guarded_jar, expected_guarded_sha256)
    sources = verified_file(guard_sources, expected_sources_sha256)
    guarded, source_files = archive_files(binary), archive_files(sources)
    original = archive_files(verified_file(GUARD / ".cache/ChestShop-3.13-pre-1-build474.jar", UPSTREAM_SHA))
    source_tar = verified_file(GUARD / ".cache/upstream-source.tar.gz", UPSTREAM_SOURCE_SHA)
    build = json.loads(guarded["META-INF/confirmation-guard/build.json"])
    if (build.get("build_id") != GUARD_BUILD or build.get("api_version") != 1
            or build.get("upstream_sha256") != UPSTREAM_SHA
            or build.get("upstream_revision") != UPSTREAM_REVISION
            or build.get("upstream_source_sha256") != UPSTREAM_SOURCE_SHA):
        raise ValueError("Unsupported guarded ChestShop provenance or API")
    expected_sources = {p.relative_to(GUARD).as_posix(): sha256(p.read_bytes())
                        for p in sorted((GUARD / "src").rglob("*.java"))}
    if not expected_sources or build.get("overlay_sources") != expected_sources:
        raise ValueError("Guarded JAR does not match the reviewed overlay source")
    class_roots = [p.removeprefix("src/").removesuffix(".java") for p in expected_sources]
    def overlay_class(name):
        return any(name == root + ".class" or (name.startswith(root + "$") and name.endswith(".class"))
                   for root in class_roots)
    classes = GUARD / "target/classes"
    for root in class_roots:
        if not (classes / (root + ".class")).is_file():
            raise ValueError("Compile the complete reviewed guard overlay before packaging")
    for path in classes.rglob("*.class"):
        name = path.relative_to(classes).as_posix()
        if not overlay_class(name) or guarded.get(name) != path.read_bytes():
            raise ValueError("Guarded JAR differs from the reviewed compiled overlay: " + name)
    expected_descriptor = original["plugin.yml"].replace(
        b"version: '3.13-pre-1 (build 474)'", b"version: '3.13-pre-1 (build 474)+confirmation.1'")
    if guarded["plugin.yml"] != expected_descriptor or b"name: ChestShop\n" not in expected_descriptor:
        raise ValueError("The guarded artifact must retain ChestShop's actual plugin identity")
    for name, content in original.items():
        if name not in guarded:
            raise ValueError("Incomplete ChestShop artifact; upstream entry missing: " + name)
        if name != "plugin.yml" and not overlay_class(name) and guarded[name] != content:
            raise ValueError("Unreviewed change to upstream ChestShop resource: " + name)
    for name in guarded:
        if name.endswith(".class") and name not in original and not overlay_class(name):
            raise ValueError("Unexpected class in guarded ChestShop artifact: " + name)
    gate = guarded["com/Acrobot/ChestShop/Confirmation/ConfirmationGate.class"]
    if GUARD_BUILD.encode() not in gate:
        raise ValueError("Missing guarded runtime identity")
    for name in ("NOTICE", "COPYING.LESSER"):
        content = (GUARD / name).read_bytes()
        if guarded["META-INF/confirmation-guard/" + name] != content:
            raise ValueError("Guarded ChestShop legal notice mismatch")
        if source_files["ChestShop-ConfirmationGuard/" + name] != content:
            raise ValueError("Corresponding-source legal notice mismatch")
    with tarfile.open(fileobj=io.BytesIO(source_tar), mode="r:gz") as archive:
        for member in archive.getmembers():
            if member.isfile():
                relative = Path(member.name).relative_to("ChestShop-3-" + UPSTREAM_REVISION)
                if source_files.get("ChestShop-upstream/" + relative.as_posix()) != archive.extractfile(member).read():
                    raise ValueError("Incomplete or changed corresponding upstream source: " + str(relative))
    for name in expected_sources:
        if sha256(source_files["ChestShop-ConfirmationGuard/" + name]) != expected_sources[name]:
            raise ValueError("Corresponding guard source differs from the binary provenance")
    for name in ("bootstrap.sh", "package.py", "pom.xml", "README.md"):
        if source_files["ChestShop-ConfirmationGuard/" + name] != (GUARD / name).read_bytes():
            raise ValueError("Corresponding guard build instructions differ from this checkout")
    for name in ("pom.xml", "mvnw", "BUILDING-GUARD.txt"):
        if not source_files.get(name):
            raise ValueError("Missing standalone source rebuild file: " + name)
    return binary, sources


def package(jar, expected_sha256, output, source_commit=None, *, guarded_jar=None,
            expected_guarded_sha256=None, guard_sources=None, expected_sources_sha256=None):
    jar, output = Path(jar), Path(output)
    if not re.fullmatch(r"[a-f0-9]{64}", expected_sha256):
        raise ValueError("Expected SHA-256 must be the verified runtime/CI JAR hash")
    if source_commit is not None and not re.fullmatch(r"[a-f0-9]{40}", source_commit):
        raise ValueError("Source commit must be a full Git commit SHA")
    if output.exists():
        raise ValueError("Output already exists; choose a new file rather than overwrite it")
    payload = jar.read_bytes()
    if sha256(payload) != expected_sha256:
        raise ValueError("JAR does not match the expected verified SHA-256")
    with zipfile.ZipFile(jar) as z:
        names = z.namelist()
        if len(names) != len(set(names)):
            raise ValueError("Duplicate ZIP entries in JAR")
        descriptor = (MODULE / "plugin.yml").read_bytes()
        if z.read("plugin.yml") != descriptor:
            raise ValueError("JAR descriptor does not match this source checkout")
        text = descriptor.decode()
        for value in ("name: ExcelShops\n", "version: " + VERSION + "\n", "  excelshop:\n", "aliases: [wrvshop]", "wrvshops.use", "wrvshops.create"):
            if value not in text:
                raise ValueError("Unexpected plugin identity, alias or permissions")
        for path in ("config.yml", "spawn-catalog.yml", "LICENSE"):
            if z.read(path) != (MODULE / path).read_bytes():
                raise ValueError("JAR resource mismatch: " + path)
        for path in ("com/winkreyes/shops/WRVShops.class", "com/winkreyes/shops/LegacyDataMigration.class", "com/winkreyes/shops/LegacyShopBridge.class", "com/winkreyes/shops/LegacyShopUi.class", "com/winkreyes/shops/LegacyTransactionFence.class", "com/winkreyes/shops/store/ShopRegistry.class"):
            if path not in names:
                raise ValueError("Required production class is missing: " + path)
        registry = z.read("com/winkreyes/shops/store/ShopRegistry.class")
        if any(value not in registry for value in (b"wrvshops", b"shop_id", b"chest_identity")):
            raise ValueError("Stable storage namespace keys are missing")
        if any(n.endswith(".class") and not n.startswith("com/winkreyes/") for n in names):
            raise ValueError("Unexpected bundled server/test dependency classes")
    guarded_payload, source_payload = validate_guard(guarded_jar, expected_guarded_sha256,
                                                     guard_sources, expected_sources_sha256)
    script = ROOT / "SpawnShop-DeluxeMenus/make_spawnshop.py"
    spec = importlib.util.spec_from_file_location("excel_spawnshop", script)
    generator = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(generator)
    menu = (ROOT / "SpawnShop-DeluxeMenus/spawnshop.yml").read_bytes()
    catalog = (ROOT / "SpawnShop-DeluxeMenus/spawn-catalog.yml").read_bytes()
    if menu != generator.render().encode() or catalog != generator.render_catalog().encode():
        raise ValueError("Menu/catalog do not match the reviewed base generator")
    if catalog != (MODULE / "spawn-catalog.yml").read_bytes():
        raise ValueError("Menu-side and plugin-side catalogs differ")
    for forbidden in (b"[takemoney]", b"[givemoney]", b"minecraft:give"):
        if forbidden in menu:
            raise ValueError("Immediate transaction command remains in menu")
    files = {
        "plugins/ExcelShops-" + VERSION + ".jar": payload,
        "plugins/" + GUARD_NAME + ".jar": guarded_payload,
        "THIRD-PARTY/ChestShop/" + GUARD_NAME + "-sources.zip": source_payload,
        "THIRD-PARTY/ChestShop/NOTICE": (GUARD / "NOTICE").read_bytes(),
        "THIRD-PARTY/ChestShop/COPYING.LESSER": (GUARD / "COPYING.LESSER").read_bytes(),
        "plugins/ExcelShops/config.yml": (MODULE / "config.yml").read_bytes(),
        "plugins/ExcelShops/spawn-catalog.yml": catalog,
        "plugins/DeluxeMenus/gui_menus/spawnshop.yml": menu,
        "tools/prepare_purchase_pauses.py": (MODULE / "tools/prepare_purchase_pauses.py").read_bytes(),
        "PURCHASE-PAUSE-SPEC.md": (MODULE / "PURCHASE-PAUSE-SPEC.md").read_bytes(),
        "PLAYER-CHECKLIST.md": (MODULE / "PLAYER-CHECKLIST.md").read_bytes(),
        "BALANCE-DISPLAY-0.1.5.md": (MODULE / "BALANCE-DISPLAY-0.1.5.md").read_bytes(),
        "AMOUNT-ENTRY-0.1.4.md": (MODULE / "AMOUNT-ENTRY-0.1.4.md").read_bytes(),
        "DEPLOYMENT.md": (MODULE / "DEPLOYMENT.md").read_bytes(),
        "LICENSE": (MODULE / "LICENSE").read_bytes(),
    }
    manifest = {
        "plugin": "ExcelShops", "version": VERSION, "source_commit": source_commit,
        "jar_sha256": expected_sha256, "deployment_authorized": False,
        "guarded_chestshop": {"plugin": "ChestShop", "build_id": GUARD_BUILD,
                              "jar_sha256": expected_guarded_sha256, "sources_sha256": expected_sources_sha256,
                              "upstream_sha256": UPSTREAM_SHA, "license": "LGPL-2.1"},
        "trading_requires_pair": True,
        "whole_server_confirmation_verified": False,
        "scope_exclusions": ["Jobs item shop", "DeluxeMenus advanced_menu and requirements_menu examples"],
        "offline_pause_preparation_included": True,
        "offline_pause_installation_applied": False,
        "rollback": "Retain guarded ChestShop or stop the server and disable all trading; never restore instant purchases silently",
        "live_catalog_preflight_verified": False, "configuration": "repository baseline; do not overwrite live prices/settings without read-only preflight",
        "legacy_data": "copy whole WRVShops folder through migration; preserve checkout and legacy-checkout journals/markers; retain ChestShop data/signs/prices/protection; do not prefill conflicting ExcelShops target",
        "files": {p: {"bytes": len(b), "sha256": sha256(b)} for p, b in sorted(files.items())},
    }
    files["MANIFEST.json"] = (json.dumps(manifest, indent=2, sort_keys=True) + "\n").encode()
    files["SHA256SUMS"] = "".join(sha256(b) + "  " + p + "\n" for p, b in sorted(files.items())).encode()
    # Exclusive creation and fixed ZIP metadata make the package reproducible.
    output.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(output, "x", compression=zipfile.ZIP_DEFLATED) as z:
        for path, content in sorted(files.items()):
            info = zipfile.ZipInfo(path, date_time=(2026, 10, 12, 0, 0, 0))
            info.compress_type = zipfile.ZIP_DEFLATED
            info.create_system = 3
            info.external_attr = 0o100644 << 16
            z.writestr(info, content)
    return manifest


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--jar", required=True, type=Path)
    p.add_argument("--expected-sha256", required=True)
    p.add_argument("--output", required=True, type=Path)
    p.add_argument("--source-commit")
    p.add_argument("--guarded-jar", required=True, type=Path)
    p.add_argument("--expected-guarded-sha256", required=True)
    p.add_argument("--guard-sources", required=True, type=Path)
    p.add_argument("--expected-sources-sha256", required=True)
    a = p.parse_args()
    try:
        result = package(a.jar, a.expected_sha256, a.output, a.source_commit,
                         guarded_jar=a.guarded_jar, expected_guarded_sha256=a.expected_guarded_sha256,
                         guard_sources=a.guard_sources, expected_sources_sha256=a.expected_sources_sha256)
    except (ValueError, OSError, KeyError, zipfile.BadZipFile, tarfile.TarError) as exc:
        p.error(str(exc))
    print(json.dumps({"package": str(a.output), "sha256": sha256(a.output.read_bytes()), "files": len(result["files"]), "deployment_authorized": False}))


if __name__ == "__main__":
    main()
