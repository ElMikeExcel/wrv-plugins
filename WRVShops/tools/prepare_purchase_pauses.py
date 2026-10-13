#!/usr/bin/env python3
"""Prepare private, reversible offline Jobs/DeluxeMenus config copies; never install them."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import stat

TARGET_MENUS = ("advanced_menu", "requirements_menu")
MAX_INPUT_BYTES = 8 * 1024 * 1024
FILE_VALUE = re.compile(r'''[ \t]*(?:"(?P<double>[^"\\\r\n]+\.ya?ml)"|'(?P<single>[^'\r\n]+\.ya?ml)'|(?P<plain>[A-Za-z0-9_./-]+\.ya?ml))(?:[ \t]+(?:#[^\r\n]*)?)?''')
KEY = re.compile(r'''(?P<key>[A-Za-z0-9_.-]+|'[^'\\]*'|"[^"\\]*")(?P<space>[ \t]*):(?P<value>.*)$''')


def digest(data):
    return hashlib.sha256(data).hexdigest()


def decode_lines(data):
    text = data.decode("utf-8")
    if "\x00" in text or text.startswith("\ufeff"):
        raise ValueError("Unsupported NUL/BOM in config; export ordinary UTF-8 first")
    lines = text.splitlines(keepends=True)
    for line in lines:
        body = line.rstrip("\r\n")
        if "\t" in body[:len(body) - len(body.lstrip())]:
            raise ValueError("Tab indentation is ambiguous; no output was prepared")
        if body.lstrip().startswith(("---", "...", "%YAML", "%TAG")):
            raise ValueError("YAML document/directive syntax is not supported")
    return lines


def significant(line):
    return bool(line.strip()) and not line.lstrip().startswith("#")


def mapping(line):
    body = line.rstrip("\r\n")
    indent = len(body) - len(body.lstrip(" "))
    found = KEY.fullmatch(body[indent:])
    if found is None:
        raise ValueError("Unsupported mapping key syntax; no output was prepared")
    key = found.group("key")
    if key[:1] in ("'", '"'):
        key = key[1:-1]
    return indent, key, found.group("value")


def top_keys(lines):
    result = {}
    for index, line in enumerate(lines):
        if significant(line) and not line.startswith(" "):
            _, key, _ = mapping(line)
            if key in result:
                raise ValueError("Duplicate top-level config key: " + key)
            result[key] = index
    return result


def pause_jobs(data):
    lines = decode_lines(data)
    keys = top_keys(lines)
    if "jobs-shop-enabled" not in keys:
        raise ValueError("Missing top-level jobs-shop-enabled")
    index = keys["jobs-shop-enabled"]
    line = lines[index]
    pattern = re.compile(r'''^(?P<prefix>(?:jobs-shop-enabled|'jobs-shop-enabled'|"jobs-shop-enabled")[ \t]*:[ \t]*)true(?P<suffix>(?:[ \t]+(?:#[^\r\n]*)?)?)(?P<end>\r\n|\n|\r|)$''')
    found = pattern.fullmatch(line)
    if found is None:
        raise ValueError("Expected one ordinary jobs-shop-enabled: true flag")
    lines[index] = found.group("prefix") + "false" + found.group("suffix") + found.group("end")
    return "".join(lines).encode("utf-8")


def pause_examples(data):
    lines = decode_lines(data)
    keys = top_keys(lines)
    if "gui_menus" not in keys:
        raise ValueError("Missing top-level gui_menus mapping")
    start = keys["gui_menus"]
    _, _, value = mapping(lines[start])
    if value.strip() and not value.lstrip().startswith("#"):
        raise ValueError("gui_menus must be an ordinary block mapping without aliases/anchors")
    end = next((i for i in range(start + 1, len(lines))
                if significant(lines[i]) and not lines[i].startswith(" ")), len(lines))
    rows = [(i, len(line) - len(line.lstrip(" "))) for i, line in enumerate(lines[start + 1:end], start + 1)
            if significant(line)]
    if not rows:
        raise ValueError("gui_menus is empty")
    child_indent = min(indent for _, indent in rows)
    if child_indent < 1:
        raise ValueError("gui_menus children must be indented")
    children = []
    seen = set()
    for index, indent in rows:
        if indent != child_indent:
            continue
        _, key, value = mapping(lines[index])
        if key in seen:
            raise ValueError("Duplicate gui_menus registration: " + key)
        seen.add(key)
        children.append((index, key, value))
    if any(name not in seen for name in TARGET_MENUS):
        raise ValueError("Both advanced_menu and requirements_menu registrations must exist exactly once")
    remove = set()
    for position, (index, key, value) in enumerate(children):
        if value.strip() and not value.lstrip().startswith("#"):
            raise ValueError("Target menu registration must be an ordinary block mapping")
        limit = children[position + 1][0] if position + 1 < len(children) else end
        body_rows = [i for i in range(index + 1, limit) if significant(lines[i])]
        if not body_rows:
            raise ValueError("Target menu registration has no file mapping")
        field_indent = min(len(lines[i]) - len(lines[i].lstrip(" ")) for i in body_rows)
        fields = []
        file_name = None
        for i in body_rows:
            if re.search(r"(?:^|[\s:[{,])[*&][A-Za-z0-9_-]+", lines[i]):
                raise ValueError("Target menu aliases/anchors require manual review")
            indent, field, field_value = mapping(lines[i])
            parsed_file = FILE_VALUE.fullmatch(field_value)
            if indent != field_indent or field != "file" or parsed_file is None:
                raise ValueError("Unexpected registration contents; expected one ordinary YAML file mapping")
            file_name = next(value for value in parsed_file.groups() if value is not None)
            fields.append(field)
        if fields != ["file"]:
            raise ValueError("Target registration must have one unambiguous file mapping")
        if key in TARGET_MENUS:
            if file_name != key + ".yml":
                raise ValueError("Target registration does not name the expected reviewed example file")
            remove.update([index] + body_rows)
        elif Path(file_name).name in {name + ".yml" for name in TARGET_MENUS}:
            raise ValueError("An example file has another registered name; review all aliases before disabling")
    # Keep every comment, blank line, other registration and unrelated setting byte-for-byte.
    return "".join(line for index, line in enumerate(lines) if index not in remove).encode("utf-8")


def reject_linked_path(path, require_file=False):
    path = Path(os.path.abspath(path))
    for ancestor in [*reversed(path.parents), path]:
        if not os.path.lexists(ancestor):
            continue
        info = ancestor.lstat()
        if stat.S_ISLNK(info.st_mode):
            raise ValueError("Linked paths are not accepted")
        if ancestor == path and require_file and (not stat.S_ISREG(info.st_mode) or info.st_nlink != 1):
            raise ValueError("Inputs must be regular, non-hardlinked exported files")
    if require_file and not path.is_file():
        raise ValueError("Exported input file is missing")
    return path


def read_export(path):
    path = reject_linked_path(path, True)
    with os.fdopen(os.open(path, os.O_RDONLY | getattr(os, "O_NOFOLLOW", 0)), "rb") as file:
        before = os.fstat(file.fileno())
        if not stat.S_ISREG(before.st_mode) or before.st_nlink != 1:
            raise ValueError("Input changed to a linked or nonregular file")
        if before.st_size > MAX_INPUT_BYTES:
            raise ValueError("Exported config exceeds the 8 MiB safety limit")
        data = file.read(MAX_INPUT_BYTES + 1)
        if len(data) > MAX_INPUT_BYTES:
            raise ValueError("Exported config exceeds the 8 MiB safety limit")
        after = os.fstat(file.fileno())
    identity = lambda s: (s.st_dev, s.st_ino, s.st_size, s.st_mtime_ns, s.st_ctime_ns)
    if identity(before) != identity(after) or identity(after) != identity(path.lstat()):
        raise ValueError("Exported input changed while being read")
    return path, data, identity(after)


def write_private(path, data):
    path.parent.mkdir(mode=0o700, parents=True, exist_ok=True)
    with os.fdopen(os.open(path, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600), "wb") as file:
        file.write(data)
        file.flush()
        os.fsync(file.fileno())


def git_marker_exists(directory):
    marker = directory / ".git"
    if not os.path.lexists(marker):
        return False
    if marker.is_symlink() or not marker.is_dir():
        return True  # Includes worktree gitdir files, even malformed ones.
    # An empty placeholder directory contains no Git metadata or tracked worktree.
    # Any metadata at all is conservatively treated as a checkout.
    return next(marker.iterdir(), None) is not None


def prepare(jobs_config, deluxemenus_config, output_dir):
    jobs_path, jobs, jobs_identity = read_export(jobs_config)
    menus_path, menus, menus_identity = read_export(deluxemenus_config)
    output = reject_linked_path(output_dir)
    if os.path.lexists(output):
        raise ValueError("Output already exists; no file will be overwritten")
    if not output.parent.is_dir():
        raise ValueError("Output parent must be an existing private directory")
    for parent in (output, *output.parents):
        if git_marker_exists(parent):
            raise ValueError("Private generated configs must be outside every Git checkout")
    changed_jobs, changed_menus = pause_jobs(jobs), pause_examples(menus)
    for path, expected in ((jobs_path, jobs_identity), (menus_path, menus_identity)):
        reject_linked_path(path, True)
        info = path.lstat()
        if (info.st_dev, info.st_ino, info.st_size, info.st_mtime_ns, info.st_ctime_ns) != expected:
            raise ValueError("Exported source changed before output preparation")
    reject_linked_path(output)
    os.mkdir(output, 0o700)  # Exclusive; a raced or previously used output is never replaced.
    files = {
        "backups/Jobs/generalConfig.yml": jobs,
        "backups/DeluxeMenus/config.yml": menus,
        "staged/Jobs/generalConfig.yml": changed_jobs,
        "staged/DeluxeMenus/config.yml": changed_menus,
    }
    for name, content in files.items():
        write_private(output / name, content)
    manifest = {
        "format": 1, "installed": False, "source_modified": False,
        "requires_stopped_server_and_approved_install": True,
        "changes": ["jobs-shop-enabled: true -> false",
                    "remove gui_menus.advanced_menu registration", "remove gui_menus.requirements_menu registration"],
        "preserved": "All other bytes and actual menu files; no permissions changed",
        "files": {name: {"bytes": len(data), "sha256": digest(data)} for name, data in sorted(files.items())},
        "rollback": "Backups are exact exports. Restoring unsafe purchase routes requires a separate decision; retain guarded ChestShop or stop all trading.",
    }
    write_private(output / "MANIFEST.json", (json.dumps(manifest, indent=2, sort_keys=True) + "\n").encode())
    return manifest


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--jobs-config", required=True, type=Path)
    parser.add_argument("--deluxemenus-config", required=True, type=Path)
    parser.add_argument("--output-dir", required=True, type=Path)
    args = parser.parse_args()
    try:
        result = prepare(args.jobs_config, args.deluxemenus_config, args.output_dir)
    except (OSError, ValueError, UnicodeError) as error:
        parser.error(str(error))
    print(json.dumps({"output_dir": str(args.output_dir), "files": len(result["files"]), "installed": False}))


if __name__ == "__main__":
    main()
