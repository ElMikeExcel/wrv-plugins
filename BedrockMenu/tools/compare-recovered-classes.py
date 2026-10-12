#!/usr/bin/env python3
"""Compare original 1.4.1 classes with separately compiled recovered 1.4.1 source.

Compile the unmodified historical source against the project dependency classpath
first; pass that output directory as --baseline-classes. Do NOT pass the repaired
1.4.3 classes: this check establishes the recovered starting point only.
"""

import argparse
import difflib
import pathlib
import re
import subprocess


def javap(path, *options):
    return subprocess.check_output(
        ["java", "-m", "jdk.jdeps/com.sun.tools.javap.Main", "-p", *options, str(path)],
        text=True,
    )


def normalize(text):
    # Constant-pool positions and javac's synthetic lambda suffixes vary across
    # compilers. Method descriptors still identify every lambda target here.
    text = re.sub(r"#\d+", "#CP", text)
    text = re.sub(r"(lambda\$[\w]+\$)\d+", r"\1N", text)
    return "\n".join(line.rstrip() for line in text.splitlines()) + "\n"


def comparable(path):
    code = javap(path, "-s", "-c", "-constants")
    # Compare bootstrap arguments too: bytecode alone does not identify the
    # lambda method targets or the recipes used by StringConcatFactory.
    verbose = javap(path, "-v")
    bootstrap = verbose.partition("BootstrapMethods:\n")[2]
    if bootstrap:
        bootstrap = "BootstrapMethods:\n" + bootstrap.split("InnerClasses:\n")[0]
    result = normalize(code + bootstrap)
    # Refuse normalization if it would merge distinct lambda signatures.
    declarations = [line for line in normalize(code).splitlines()
                    if "lambda$" in line and line.strip().endswith(";")]
    if len(declarations) != len(set(declarations)):
        raise ValueError(f"Ambiguous normalized lambda signatures in {path}")
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--baseline-classes", required=True, type=pathlib.Path)
    parser.add_argument("--original-classes", type=pathlib.Path,
                        default=pathlib.Path(__file__).resolve().parents[1] / "classes-1.4.1")
    args = parser.parse_args()
    original_files = {p.relative_to(args.original_classes)
                      for p in args.original_classes.rglob("*.class")}
    baseline_files = {p.relative_to(args.baseline_classes)
                      for p in args.baseline_classes.rglob("*.class")}
    if original_files != baseline_files or not original_files:
        raise SystemExit(f"Class sets differ: original={original_files}, baseline={baseline_files}")
    failures = 0
    for relative in sorted(original_files):
        original = comparable(args.original_classes / relative)
        baseline = comparable(args.baseline_classes / relative)
        if original != baseline:
            failures += 1
            print("".join(difflib.unified_diff(original.splitlines(True), baseline.splitlines(True),
                  fromfile=f"original/{relative}", tofile=f"baseline/{relative}")))
        else:
            print(f"MATCH {relative}")
    print(f"{len(original_files) - failures}/{len(original_files)} normalized disassemblies match.")
    raise SystemExit(bool(failures))


if __name__ == "__main__":
    main()
