#!/usr/bin/env python3
"""ExcelBrainGames 0.3.1 — Letter Tiles generator.

ONE source of truth for every tile: a defined 5x7 pixel font (below) drawn
programmatically with PIL onto 64x64 tiles in each family's state colors.
No AI image generation anywhere near the glyphs — letters must be exact.

Emits, under packs/dist/:
  java/                      the Java server resource pack tree
  braingames-tiles-java.zip  ... zipped deterministically (sorted, fixed date)
  bedrock/                   the Geyser Bedrock resource pack tree
  braingames-tiles-bedrock.mcpack  ... same zip technique
  geyser/braingames-tiles.json     Geyser v2 custom-item mappings
  tiles-manifest.json        every tile's key/glyph/state/base item/colors —
                             the contract validate_packs.py and TileTest check
  java-pack.sha1             SHA-1 of the Java zip, for server.properties

Deterministic: fixed UUIDs, fixed zip timestamps, sorted entries — two runs
produce byte-identical zips (asserted by build.sh running it twice).
"""
import hashlib
import io
import json
import os
import sys
import zipfile

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
DIST = os.path.join(HERE, "dist")
TILE = 64
ZIP_DATE = (2026, 11, 5, 0, 0, 0)
PACK_FORMAT = 97  # Paper/Minecraft 26.3 version.json: pack_version resource 97.1

# Fixed pack identity — MUST stay stable across rebuilds so Bedrock clients
# treat the pack as the same pack (update = version bump, not a new pack).
HEADER_UUID = "3f8b2a51-9c4d-4e7a-b6d1-2c5e8f0a4311"
MODULE_UUID = "5a1c9e77-3b28-4f6d-9e02-7d4b6a8c2f55"
PACK_VERSION = [0, 3, 1]

# 5x7 pixel font, rows top-to-bottom, '#' = glyph pixel. Defined by hand so
# every letter is exactly the letter — the whole point of the pack.
FONT = {
    "A": [".###.", "#...#", "#...#", "#####", "#...#", "#...#", "#...#"],
    "B": ["####.", "#...#", "#...#", "####.", "#...#", "#...#", "####."],
    "C": [".####", "#....", "#....", "#....", "#....", "#....", ".####"],
    "D": ["####.", "#...#", "#...#", "#...#", "#...#", "#...#", "####."],
    "E": ["#####", "#....", "#....", "####.", "#....", "#....", "#####"],
    "F": ["#####", "#....", "#....", "####.", "#....", "#....", "#...."],
    "G": [".####", "#....", "#....", "#.###", "#...#", "#...#", ".###."],
    "H": ["#...#", "#...#", "#...#", "#####", "#...#", "#...#", "#...#"],
    "I": ["#####", "..#..", "..#..", "..#..", "..#..", "..#..", "#####"],
    "J": ["..###", "...#.", "...#.", "...#.", "...#.", "#..#.", ".##.."],
    "K": ["#...#", "#..#.", "#.#..", "##...", "#.#..", "#..#.", "#...#"],
    "L": ["#....", "#....", "#....", "#....", "#....", "#....", "#####"],
    "M": ["#...#", "##.##", "#.#.#", "#.#.#", "#...#", "#...#", "#...#"],
    "N": ["#...#", "#...#", "##..#", "#.#.#", "#..##", "#...#", "#...#"],
    "O": [".###.", "#...#", "#...#", "#...#", "#...#", "#...#", ".###."],
    "P": ["####.", "#...#", "#...#", "####.", "#....", "#....", "#...."],
    "Q": [".###.", "#...#", "#...#", "#...#", "#.#.#", "#..#.", ".##.#"],
    "R": ["####.", "#...#", "#...#", "####.", "#.#..", "#..#.", "#...#"],
    "S": [".####", "#....", "#....", ".###.", "....#", "....#", "####."],
    "T": ["#####", "..#..", "..#..", "..#..", "..#..", "..#..", "..#.."],
    "U": ["#...#", "#...#", "#...#", "#...#", "#...#", "#...#", ".###."],
    "V": ["#...#", "#...#", "#...#", "#...#", "#...#", ".#.#.", "..#.."],
    "W": ["#...#", "#...#", "#...#", "#.#.#", "#.#.#", "##.##", "#...#"],
    "X": ["#...#", ".#.#.", "..#..", "..#..", "..#..", ".#.#.", "#...#"],
    "Y": ["#...#", "#...#", ".#.#.", "..#..", "..#..", "..#..", "..#.."],
    "Z": ["#####", "....#", "...#.", "..#..", ".#...", "#....", "#####"],
    "0": [".###.", "#..##", "#.#.#", "##..#", "#...#", "#...#", ".###."],
    "1": ["..#..", ".##..", "..#..", "..#..", "..#..", "..#..", "#####"],
    "2": [".###.", "#...#", "....#", "..##.", ".#...", "#....", "#####"],
    "3": ["####.", "....#", "....#", ".###.", "....#", "....#", "####."],
    "4": ["...#.", "..##.", ".#.#.", "#..#.", "#####", "...#.", "...#."],
    "5": ["#####", "#....", "####.", "....#", "....#", "#...#", ".###."],
    "6": [".###.", "#....", "#....", "####.", "#...#", "#...#", ".###."],
    "7": ["#####", "....#", "...#.", "..#..", "..#..", "..#..", "..#.."],
    "8": [".###.", "#...#", "#...#", ".###.", "#...#", "#...#", ".###."],
    "9": [".###.", "#...#", "#...#", ".####", "....#", "....#", ".###."],
}

# family-state -> (background, glyph). Contrast-checked by validate_packs.py.
COLORS = {
    ("ws", "normal"):        ((243, 236, 217), (43, 36, 26)),
    ("ws", "selected"):      ((250, 213, 62),  (43, 36, 20)),
    ("ws", "found"):         ((146, 222, 94),  (23, 48, 14)),
    ("sudoku", "given"):     ((163, 208, 238), (15, 44, 92)),
    ("sudoku", "placed"):    ((243, 236, 217), (32, 32, 38)),
    ("sudoku", "wrong"):     ((200, 42, 42),   (255, 255, 255)),
    ("number", "value"):     ((18, 138, 80),   (255, 255, 255)),
}

# family-state -> the vanilla base item the view stamps (mirrors Views.java).
BASE_ITEMS = {
    ("ws", "normal"): "minecraft:paper",
    ("ws", "selected"): "minecraft:yellow_stained_glass_pane",
    ("ws", "found"): "minecraft:lime_stained_glass_pane",
    ("sudoku", "given"): "minecraft:light_blue_stained_glass_pane",
    ("sudoku", "placed"): "minecraft:paper",
    ("sudoku", "wrong"): "minecraft:red_stained_glass_pane",
    ("number", "value"): "minecraft:emerald",
}


def tiles():
    """The full tile list, mirroring core/TileKeys.java exactly (205 tiles)."""
    out = []
    for c in "abcdefghijklmnopqrstuvwxyz":
        for state in ("normal", "selected", "found"):
            out.append(dict(key=f"ws/{c}_{state}", glyph=c.upper(), family="ws", state=state))
    for d in range(1, 10):
        for state in ("given", "placed", "wrong"):
            out.append(dict(key=f"sudoku/{d}_{state}", glyph=str(d), family="sudoku", state=state))
    for v in range(0, 100):
        out.append(dict(key=f"number/{v}", glyph=str(v), family="number", state="value"))
    for t in out:
        flat = t["key"].replace("/", "_")
        t["bedrock_identifier"] = "braingames:" + flat
        t["icon"] = "braingames." + flat  # Geyser's derived shorthand (':'->'.', '/'->'_')
        t["base_item"] = BASE_ITEMS[(t["family"], t["state"])]
        t["bg"], t["fg"] = COLORS[(t["family"], t["state"])]
        t["texture_java"] = f"assets/braingames/textures/item/{t['key']}.png"
        t["texture_bedrock"] = f"textures/items/{flat}.png"
    assert len(out) == 205, len(out)
    assert len({t["bedrock_identifier"] for t in out}) == 205, "flattened ids collide"
    return out


def draw_tile(glyph, bg, fg):
    img = Image.new("RGB", (TILE, TILE), bg)
    d = ImageDraw.Draw(img)
    frame = tuple(int(c * 0.78) for c in bg)
    d.rectangle([1, 1, TILE - 2, TILE - 2], outline=frame, width=2)
    if len(glyph) == 1:
        block, gap = 7, 0
    else:
        block, gap = 5, 6
    gw = len(glyph) * 5 * block + (len(glyph) - 1) * gap
    gh = 7 * block
    x0, y0 = (TILE - gw) // 2, (TILE - gh) // 2
    for i, ch in enumerate(glyph):
        rows = FONT[ch]
        ox = x0 + i * (5 * block + gap)
        for y, row in enumerate(rows):
            for x, px in enumerate(row):
                if px == "#":
                    d.rectangle([ox + x * block, y0 + y * block,
                                 ox + x * block + block - 1, y0 + y * block + block - 1], fill=fg)
    buf = io.BytesIO()
    img.save(buf, format="PNG", optimize=False)
    return buf.getvalue()


def write(path, data):
    full = os.path.join(DIST, path)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    mode = "wb" if isinstance(data, bytes) else "w"
    with open(full, mode) as f:
        f.write(data)


def dump(obj):
    return json.dumps(obj, indent=2, sort_keys=True) + "\n"


def zip_tree(tree_root, out_name):
    root = os.path.join(DIST, tree_root)
    entries = {}
    for dp, _, fn in os.walk(root):
        for f in fn:
            p = os.path.join(dp, f)
            entries[os.path.relpath(p, root)] = open(p, "rb").read()
    buf = io.BytesIO()
    with zipfile.ZipFile(buf, "w", zipfile.ZIP_DEFLATED) as z:
        for name in sorted(entries):
            zi = zipfile.ZipInfo(name.replace(os.sep, "/"), ZIP_DATE)
            zi.compress_type = zipfile.ZIP_DEFLATED
            z.writestr(zi, entries[name])
    data = buf.getvalue()
    write(out_name, data)
    return data


def main():
    ts = tiles()
    for t in ts:
        png = draw_tile(t["glyph"], t["bg"], t["fg"])
        write(f"java/assets/braingames/items/{t['key']}.json",
              dump({"model": {"type": "minecraft:model", "model": f"braingames:item/{t['key']}"}}))
        write(f"java/assets/braingames/models/item/{t['key']}.json",
              dump({"parent": "minecraft:item/generated",
                    "textures": {"layer0": f"braingames:item/{t['key']}"}}))
        write(f"java/{t['texture_java']}", png)
        write(f"bedrock/{t['texture_bedrock']}", png)  # Bedrock: the SAME PNG bytes
    write("java/pack.mcmeta", dump({"pack": {"pack_format": PACK_FORMAT,
                                              "description": "Brain Games letter tiles (ExcelBrainGames 0.3.1)"}}))
    write("bedrock/manifest.json", dump({
        "format_version": 2,
        "header": {"name": "Brain Games Letter Tiles",
                   "description": "Letter/digit tiles for ExcelBrainGames 0.3.1 (Geyser custom items)",
                   "uuid": HEADER_UUID, "version": PACK_VERSION,
                   "min_engine_version": [1, 21, 0]},
        "modules": [{"type": "resources", "uuid": MODULE_UUID, "version": PACK_VERSION}],
    }))
    texture_data = {}
    for t in ts:
        entry = {"textures": t["texture_bedrock"]}
        texture_data[t["bedrock_identifier"]] = entry  # full-identifier key form
        texture_data[t["icon"]] = entry                # Geyser-derived shorthand key form
    write("bedrock/textures/item_texture.json", dump({
        "resource_pack_name": "braingames-tiles", "texture_name": "atlas.items",
        "texture_data": texture_data}))
    by_item = {}
    for t in ts:
        by_item.setdefault(t["base_item"], []).append({
            "type": "definition",
            "model": "braingames:" + t["key"],
            "bedrock_identifier": t["bedrock_identifier"],
            "bedrock_options": {"icon": t["icon"]},
        })
    for item in by_item:
        by_item[item].sort(key=lambda e: e["model"])
    write("geyser/braingames-tiles.json", dump({"format_version": 2, "items": by_item}))
    manifest = {"pack": "ExcelBrainGames 0.3.1 letter tiles", "count": len(ts),
                "tiles": [{k: t[k] for k in ("key", "glyph", "family", "state", "base_item",
                                             "bedrock_identifier", "icon", "texture_java",
                                             "texture_bedrock", "bg", "fg")} for t in ts]}
    write("tiles-manifest.json", dump(manifest))
    java_zip = zip_tree("java", "braingames-tiles-java.zip")
    zip_tree("bedrock", "braingames-tiles-bedrock.mcpack")
    sha1 = hashlib.sha1(java_zip).hexdigest()
    write("java-pack.sha1", sha1 + "\n")
    print(f"tiles: {len(ts)}  java zip: {len(java_zip)} B  sha1 {sha1}")
    print(f"java zip sha256 {hashlib.sha256(java_zip).hexdigest()}")


if __name__ == "__main__":
    sys.exit(main())
