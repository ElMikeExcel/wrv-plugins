#!/usr/bin/env python3
# ExcelMenu 2.0 tooling: machine-extract ExcelBedrockMenu's BUTTONS /
# COMMANDS arrays into the golden harness fixture (no hand transcription).
#
# Usage: extract_bedrock_arrays.py <ExcelBedrockMenu.java> <out.json>
#
# The arrays are plain string-literal lists; this pulls every literal
# in order from each array body and writes {"source", "buttons",
# "commands"} JSON. (This tool existed in Phase 0 and was lost between
# phases; recreated in Phase 2 when the fixture was re-based from
# 1.5.27 to the live 1.5.28 predecessor.)
import json
import re
import sys


def extract_array(source: str, name: str) -> list[str]:
    m = re.search(name + r"\s*=\s*\{(.*?)\};", source, re.DOTALL)
    if not m:
        raise SystemExit(f"array {name} not found")
    body = m.group(1)
    literals = re.findall(r'"((?:[^"\\]|\\.)*)"', body)
    return [bytes(s, "utf-8").decode("unicode_escape") for s in literals]


def main() -> None:
    src_path, out_path = sys.argv[1], sys.argv[2]
    source = open(src_path, encoding="utf-8").read()
    buttons = extract_array(source, "BUTTONS")
    commands = extract_array(source, "COMMANDS")
    if len(buttons) != len(commands):
        raise SystemExit(f"array length mismatch: {len(buttons)} vs {len(commands)}")
    with open(out_path, "w", encoding="utf-8") as fh:
        json.dump({"source": src_path, "buttons": buttons, "commands": commands},
                  fh, ensure_ascii=False, indent=1)
    print(f"wrote {out_path}: {len(buttons)} buttons / {len(commands)} commands")


if __name__ == "__main__":
    main()
