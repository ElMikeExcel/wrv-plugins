#!/usr/bin/env python3
"""ExcelBrainGames 0.3.1 — Letter Tiles pack validator.

Independently re-derives the expected tile space (mirroring
core/TileKeys.java) and checks the generated packs in packs/dist/ against
it: every JSON parses, every model/texture/mapping reference resolves, the
Java and Bedrock textures are byte-identical, glyph/background contrast is
>= 4.0 (WCAG) for every tile, and the zips match their trees. Exits 1 on
the first failed category (all failures are printed).
"""
import hashlib
import json
import os
import struct
import sys
import zipfile

HERE = os.path.dirname(os.path.abspath(__file__))
DIST = os.path.join(HERE, "dist")
HEADER_UUID = "3f8b2a51-9c4d-4e7a-b6d1-2c5e8f0a4311"
MODULE_UUID = "5a1c9e77-3b28-4f6d-9e02-7d4b6a8c2f55"
fails = []


def check(cond, msg):
    if not cond:
        fails.append(msg)


def load(path):
    with open(os.path.join(DIST, path)) as f:
        return json.load(f)


def expected_keys():
    keys = []
    for c in "abcdefghijklmnopqrstuvwxyz":
        for s in ("normal", "selected", "found"):
            keys.append(f"ws/{c}_{s}")
    for d in range(1, 10):
        for s in ("given", "placed", "wrong"):
            keys.append(f"sudoku/{d}_{s}")
    for v in range(100):
        keys.append(f"number/{v}")
    return sorted(keys)


def luminance(rgb):
    def lin(c):
        c = c / 255.0
        return c / 12.92 if c <= 0.03928 else ((c + 0.055) / 1.055) ** 2.4
    r, g, b = (lin(c) for c in rgb)
    return 0.2126 * r + 0.7152 * g + 0.0722 * b


def png_size(path):
    with open(path, "rb") as f:
        head = f.read(24)
    if head[:8] != b"\x89PNG\r\n\x1a\n":
        return None
    return struct.unpack(">II", head[16:24])


def main():
    exp = expected_keys()
    manifest = load("tiles-manifest.json")
    tiles = {t["key"]: t for t in manifest["tiles"]}
    check(manifest["count"] == 205, f"manifest count {manifest['count']} != 205")
    check(sorted(tiles) == exp, "manifest key set != TileKeys key space")

    # ---- Java pack ----
    mcmeta = load("java/pack.mcmeta")
    check(mcmeta["pack"]["pack_format"] == 97,
          f"pack_format {mcmeta['pack'].get('pack_format')} != 97 (Paper 26.3)")
    for key in exp:
        idef = load(f"java/assets/braingames/items/{key}.json")
        check(idef["model"]["type"] == "minecraft:model"
              and idef["model"]["model"] == f"braingames:item/{key}",
              f"java item def wrong for {key}")
        model = load(f"java/assets/braingames/models/item/{key}.json")
        check(model["parent"] == "minecraft:item/generated"
              and model["textures"]["layer0"] == f"braingames:item/{key}",
              f"java model wrong for {key}")
        jp = os.path.join(DIST, "java", tiles[key]["texture_java"])
        check(os.path.exists(jp), f"java texture missing {key}")
        if os.path.exists(jp):
            check(png_size(jp) == (64, 64), f"java texture not 64x64: {key}")

    # ---- Bedrock pack ----
    bman = load("bedrock/manifest.json")
    check(bman["format_version"] == 2, "bedrock manifest format_version != 2")
    check(bman["header"]["uuid"] == HEADER_UUID, "bedrock header uuid drifted")
    check(bman["modules"][0]["uuid"] == MODULE_UUID
          and bman["modules"][0]["type"] == "resources", "bedrock module wrong")
    itex = load("bedrock/textures/item_texture.json")["texture_data"]
    for key in exp:
        t = tiles[key]
        for form in (t["bedrock_identifier"], t["icon"]):
            check(form in itex and itex[form]["textures"] == t["texture_bedrock"],
                  f"item_texture missing/wrong for {form}")
        bp = os.path.join(DIST, "bedrock", t["texture_bedrock"])
        jp = os.path.join(DIST, "java", t["texture_java"])
        check(os.path.exists(bp), f"bedrock texture missing {key}")
        if os.path.exists(bp) and os.path.exists(jp):
            check(open(bp, "rb").read() == open(jp, "rb").read(),
                  f"bedrock/java texture bytes differ for {key}")

    # ---- Geyser mappings ----
    maps = load("geyser/braingames-tiles.json")
    check(maps["format_version"] == 2, "mappings format_version != 2")
    seen = {}
    for base_item, defs in maps["items"].items():
        for d in defs:
            check(d["type"] == "definition", f"mapping not a definition: {d}")
            key = d["model"].removeprefix("braingames:")
            check(key in tiles, f"mapping model unknown: {d['model']}")
            if key in tiles:
                t = tiles[key]
                check(base_item == t["base_item"], f"{key} grouped under {base_item}, view stamps {t['base_item']}")
                check(d["bedrock_identifier"] == t["bedrock_identifier"], f"{key} bedrock id wrong")
                check(d.get("bedrock_options", {}).get("icon") == t["icon"], f"{key} icon wrong")
            seen[key] = seen.get(key, 0) + 1
    check(sorted(seen) == exp, "mapping key set != TileKeys key space")
    check(all(v == 1 for v in seen.values()), "duplicate mapping definitions")

    # ---- Contrast (WCAG ratio of glyph on its state background) ----
    worst = 99.0
    for key in exp:
        t = tiles[key]
        l1, l2 = luminance(t["fg"]), luminance(t["bg"])
        ratio = (max(l1, l2) + 0.05) / (min(l1, l2) + 0.05)
        worst = min(worst, ratio)
        check(ratio >= 4.0, f"contrast {ratio:.2f} < 4.0 for {key}")

    # ---- Zips match their trees and the sha1 sidecar ----
    for tree, zname in (("java", "braingames-tiles-java.zip"),
                        ("bedrock", "braingames-tiles-bedrock.mcpack")):
        root = os.path.join(DIST, tree)
        on_disk = sorted(os.path.relpath(os.path.join(dp, f), root)
                         for dp, _, fn in os.walk(root) for f in fn)
        with zipfile.ZipFile(os.path.join(DIST, zname)) as z:
            check(sorted(z.namelist()) == on_disk, f"{zname} entries != {tree} tree")
    with open(os.path.join(DIST, "java-pack.sha1")) as f:
        recorded = f.read().strip()
    actual = hashlib.sha1(open(os.path.join(DIST, "braingames-tiles-java.zip"), "rb").read()).hexdigest()
    check(recorded == actual, "java-pack.sha1 does not match the java zip")

    if fails:
        print(f"PACK VALIDATION FAILED ({len(fails)}):")
        for f in fails[:25]:
            print("  -", f)
        return 1
    print(f"PACK VALIDATION OK: 205 tiles x2 packs, mappings 205, worst contrast {worst:.2f} (>= 4.0)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
