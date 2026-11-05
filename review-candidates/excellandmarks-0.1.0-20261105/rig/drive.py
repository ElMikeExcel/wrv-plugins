#!/usr/bin/env python3
"""ExcelLandmarks 0.1.0 rig driver: two boots of the scratch Paper 26.3-235
server. Boot A (Vault + EssentialsX + GriefPrevention + ExcelProtect):
sign registration, sign-tap menu rent with confirm, GP subdivision,
view-only guest menu, unregistered sign, Protect-fed lapse finalize with
redemption box. Boot B (GriefPrevention + ExcelProtect REMOVED): the
fail-soft proof — naming-only rents and a clean finalize. Transcripts
land in rig/evidence/."""
import os
import shutil
import subprocess
import sys
import threading
import time

HERE = os.path.dirname(os.path.abspath(__file__))
SERVER = "/tmp/landmarks-rig/server"
JAVA = os.path.expanduser("~/workspace/.jdk25/bin/java")
EVIDENCE = os.path.join(HERE, "evidence")


def boot(phase, ready_marker, log_name, timeout=420):
    phase_dir = os.path.join(SERVER, "plugins", "LmRig")
    os.makedirs(phase_dir, exist_ok=True)
    with open(os.path.join(phase_dir, "phase.txt"), "w") as f:
        f.write(phase)
    proc = subprocess.Popen([JAVA, "-Xmx1G", "-jar", "paper.jar", "nogui"],
                            cwd=SERVER, stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                            stderr=subprocess.STDOUT, text=True, bufsize=1)
    lines, done, ready = [], threading.Event(), threading.Event()

    def reader():
        for line in proc.stdout:
            lines.append(line.rstrip("\n"))
            if "Done (" in line:
                done.set()
            if ready_marker in line:
                ready.set()

    threading.Thread(target=reader, daemon=True).start()
    ok = True
    if not done.wait(timeout=240):
        print(f"RIG {phase}: server did not reach Done in 240s")
        ok = False
    if ok and not ready.wait(timeout=timeout):
        print(f"RIG {phase}: driver never reached {ready_marker}")
        ok = False
    if ok:
        time.sleep(2)
    print(">>> CONSOLE: stop", flush=True)
    try:
        proc.stdin.write("stop\n")
        proc.stdin.flush()
    except BrokenPipeError:
        pass
    try:
        proc.wait(timeout=90)
    except subprocess.TimeoutExpired:
        proc.kill()
    os.makedirs(EVIDENCE, exist_ok=True)
    with open(os.path.join(EVIDENCE, log_name), "w") as f:
        f.write("\n".join(lines) + "\n")
    severe = sum(1 for l in lines if "SEVERE" in l)
    enable = [l for l in lines if "ExcelLandmarks 0.1.0 enabled" in l]
    for l in enable:
        print("  ENABLE: " + l.split("]: ", 1)[-1] if "]: " in l else "  ENABLE: " + l, flush=True)
    results = [l for l in lines if "RIGRESULT" in l]
    for l in results:
        print("  " + l.split("]: ", 1)[-1] if "]: " in l else "  " + l, flush=True)
    fails = [l for l in results if " FAIL " in l]
    print(f"RIG {phase}: {len(results)} result lines, {len(fails)} FAIL, SEVERE lines: {severe}")
    return ok and not fails and severe == 0


def wipe_state(extra=()):
    plugins = os.path.join(SERVER, "plugins")
    for d in ("ExcelLandmarks", "Essentials", "Vault", "LmRig") + tuple(extra):
        p = os.path.join(plugins, d)
        if os.path.isdir(p):
            shutil.rmtree(p)
    for w in ("world", "world_nether", "world_the_end"):
        p = os.path.join(SERVER, w)
        if os.path.isdir(p):
            shutil.rmtree(p)


def stage_boot_a():
    """(Re)stage the full Boot A plugin set + a clean state — makes
    repeated runs correct even after a previous run's Boot B swap."""
    plugins = os.path.join(SERVER, "plugins")
    home = os.path.expanduser("~")
    for src in (os.path.join(home, "workspace/excelprotect/src/ExcelProtect/libs/GriefPrevention-16.18.7.jar"),
                os.path.join(home, "workspace/excelprotect/src/ExcelProtect/ExcelProtect-0.1.1.jar")):
        shutil.copy(src, plugins)
    wipe_state(extra=("GriefPrevention", "GriefPreventionData", "ExcelProtect"))


def swap_to_boot_b():
    """Remove GP + Protect and wipe state so Boot B starts clean."""
    plugins = os.path.join(SERVER, "plugins")
    for jar in ("GriefPrevention-16.18.7.jar", "ExcelProtect-0.1.1.jar"):
        p = os.path.join(plugins, jar)
        if os.path.exists(p):
            os.remove(p)
    for d in ("GriefPrevention", "GriefPreventionData", "ExcelProtect"):
        p = os.path.join(plugins, d)
        if os.path.isdir(p):
            shutil.rmtree(p)
    wipe_state()


def main():
    stage_boot_a()
    ok_a = boot("A", "RIGREADY STOP-A", "boot-a.log")
    swap_to_boot_b()
    ok_b = boot("B", "RIGREADY STOP-B", "boot-b.log")
    print("RIG OVERALL: " + ("PASS" if (ok_a and ok_b) else "FAIL"))
    sys.exit(0 if (ok_a and ok_b) else 1)


if __name__ == "__main__":
    main()
