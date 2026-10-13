#!/usr/bin/env python3
"""Copy the two known data directories from an offline snapshot; never merge.

This prepares an offline package only. It does not touch server JARs, deploy,
restart, update aliases, or certify that the input is a current stopped snapshot.
"""
import argparse
import hashlib
import json
from pathlib import Path
import shutil
import stat

DIRECTORIES = {'BedrockMenu': 'ExcelBedrockMenu', 'ShopVillager': 'ExcelShopVillager'}


def require(condition, message):
    if not condition:
        raise ValueError(message)


def inventory(root):
    """Record every relative path/type and each file's exact bytes, including hidden files."""
    root = Path(root)
    require(root.is_dir() and not root.is_symlink(), f'Not a real directory: {root}')
    result = {}
    casefold_paths = set()
    for path in sorted(root.rglob('*')):
        mode = path.lstat().st_mode
        relative = path.relative_to(root).as_posix()
        require(relative.casefold() not in casefold_paths, f'Case-colliding data paths: {relative}')
        casefold_paths.add(relative.casefold())
        if stat.S_ISDIR(mode):
            result[relative] = {'type': 'directory'}
        elif stat.S_ISREG(mode):
            data = path.read_bytes()
            result[relative] = {'type': 'file', 'bytes': len(data),
                                'sha256': hashlib.sha256(data).hexdigest()}
        else:
            raise ValueError(f'Symlink or special file requires manual review: {path}')
    return result


def prepare(offline_plugins, output):
    offline_plugins, output = Path(offline_plugins), Path(output)
    require(offline_plugins.is_dir() and not offline_plugins.is_symlink(), 'Invalid offline snapshot directory')
    require(not output.exists() and not output.is_symlink(), 'Output exists; refusing to merge or overwrite')
    require(output.parent.is_dir(), 'Create the output parent directory first')
    for parent in (output.parent, *output.parent.parents):
        require(not parent.is_symlink(), f'Output parent is a symlink: {parent}')
    require(not output.resolve().is_relative_to(offline_plugins.resolve()), 'Output must be outside the snapshot')
    for child in output.parent.iterdir():
        require(child.name.casefold() != output.name.casefold(), 'Case-conflicting output exists')
    expected_names = {name.casefold(): name for pair in DIRECTORIES.items() for name in pair}
    for child in offline_plugins.iterdir():
        expected = expected_names.get(child.name.casefold())
        require(expected is None or child.name == expected, f'Case-conflicting plugin directory: {child.name}')
    before = {}
    for old, new in DIRECTORIES.items():
        require(not (offline_plugins / new).exists() and not (offline_plugins / new).is_symlink(),
                f'Conflicting target directory in snapshot: {new}')
        before[old] = inventory(offline_plugins / old)
    # No output is created until every source/target precondition passes.
    output.mkdir()
    for old, new in DIRECTORIES.items():
        shutil.copytree(offline_plugins / old, output / new, copy_function=shutil.copy2)
    for old, new in DIRECTORIES.items():
        require(inventory(offline_plugins / old) == before[old], f'Source changed while copying: {old}')
        require(inventory(output / new) == before[old], f'Copied bytes/paths differ: {new}')
    report = {'status': 'offline-copy-verified', 'mapping': DIRECTORIES,
              'inventories': before, 'deployed': False}
    # This receipt is written only after all copies pass verification.
    (output / 'data-copy-manifest.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
    return report


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--offline-plugins', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    prepare(args.offline_plugins, args.output)
    print('Offline copies verified. Not deployed. Keep the original directories unchanged.')


if __name__ == '__main__':
    main()
