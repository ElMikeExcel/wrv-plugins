#!/usr/bin/env python3
"""Package a verified ExcelShops candidate locally. Never contacts or changes a server."""
import argparse
import hashlib
import importlib.util
import json
from pathlib import Path
import re
import zipfile

MODULE = Path(__file__).resolve().parents[1]
ROOT = MODULE.parent
VERSION = "0.1.1"


def sha256(data):
    return hashlib.sha256(data).hexdigest()


def package(jar, expected_sha256, output, source_commit=None):
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
        for path in ("com/winkreyes/shops/WRVShops.class", "com/winkreyes/shops/LegacyDataMigration.class", "com/winkreyes/shops/store/ShopRegistry.class"):
            if path not in names:
                raise ValueError("Required production class is missing: " + path)
        registry = z.read("com/winkreyes/shops/store/ShopRegistry.class")
        if any(value not in registry for value in (b"wrvshops", b"shop_id", b"chest_identity")):
            raise ValueError("Stable storage namespace keys are missing")
        if any(n.startswith(("org/", "net/", "wrv/")) and n.endswith(".class") for n in names):
            raise ValueError("Unexpected bundled server/test dependency classes")
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
        "plugins/ExcelShops/config.yml": (MODULE / "config.yml").read_bytes(),
        "plugins/ExcelShops/spawn-catalog.yml": catalog,
        "plugins/DeluxeMenus/gui_menus/spawnshop.yml": menu,
        "PLAYER-CHECKLIST.md": (MODULE / "PLAYER-CHECKLIST.md").read_bytes(),
        "DEPLOYMENT.md": (MODULE / "DEPLOYMENT.md").read_bytes(),
        "LICENSE": (MODULE / "LICENSE").read_bytes(),
    }
    manifest = {
        "plugin": "ExcelShops", "version": VERSION, "source_commit": source_commit,
        "jar_sha256": expected_sha256, "deployment_authorized": False,
        "live_catalog_preflight_verified": False, "configuration": "repository baseline; do not overwrite live prices/settings without read-only preflight",
        "legacy_data": "copy whole WRVShops folder through migration; preserve source and dirty markers; do not prefill conflicting ExcelShops target",
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
            info.external_attr = 0o100644 << 16
            z.writestr(info, content)
    return manifest


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--jar", required=True, type=Path)
    p.add_argument("--expected-sha256", required=True)
    p.add_argument("--output", required=True, type=Path)
    p.add_argument("--source-commit")
    a = p.parse_args()
    try:
        result = package(a.jar, a.expected_sha256, a.output, a.source_commit)
    except (ValueError, OSError, KeyError, zipfile.BadZipFile) as exc:
        p.error(str(exc))
    print(json.dumps({"package": str(a.output), "sha256": sha256(a.output.read_bytes()), "files": len(result["files"]), "deployment_authorized": False}))


if __name__ == "__main__":
    main()
