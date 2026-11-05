#!/usr/bin/env python3
"""ExcelBrainGames Phase 3 rig driver: two boots of the scratch Paper 26.3
server. Boot A plays full rounds (kid-tier half payout, standard Word
Search, Memory Match, pays-once), seeds a Challenge round with one word
found, then exercises the console admin surface. Boot B proves the seeded
round resumes intact across the restart and pays out correctly.
Transcripts land in rig/evidence/."""
import os
import subprocess
import sys
import threading
import time

HERE = os.path.dirname(os.path.abspath(__file__))
SERVER = "/tmp/braingames-rig2/server"  # Phase 3 reuses the Phase 2 scratch server in place (/tmp is a 512 MB tmpfs)
JAVA = os.path.expanduser("~/workspace/.jdk25/bin/java")
EVIDENCE = os.path.join(HERE, "evidence")


def boot(phase, admin_commands, ready_marker, log_name, timeout=420):
    phase_dir = os.path.join(SERVER, "plugins", "BrainRig")
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
        for c in admin_commands:
            print(f">>> CONSOLE: {c}", flush=True)
            proc.stdin.write(c + "\n")
            proc.stdin.flush()
            time.sleep(3)
    print(f">>> CONSOLE: stop", flush=True)
    try:
        proc.stdin.write("stop\n")
        proc.stdin.flush()
    except BrokenPipeError:
        pass
    try:
        proc.wait(timeout=150)
    except subprocess.TimeoutExpired:
        proc.kill()
    os.makedirs(EVIDENCE, exist_ok=True)
    with open(os.path.join(EVIDENCE, log_name), "w") as f:
        f.write("\n".join(lines) + "\n")
    severe = sum(1 for l in lines if "SEVERE" in l)
    results = [l for l in lines if "RIGRESULT" in l]
    for l in results:
        print("  " + l.split("]: ", 1)[-1] if "]: " in l else "  " + l, flush=True)
    fails = [l for l in results if " FAIL " in l]
    print(f"RIG {phase}: {len(results)} result lines, {len(fails)} FAIL, SEVERE lines: {severe}")
    return ok and not fails and severe == 0


def main():
    admin_a = [
        "games",
        "games admin status RigPlayer",
        "games admin riddle",
        "games admin setriddle 3",
        "games admin riddle",
        "games admin reset RigPlayer riddle",
        "games admin reload",
    ]
    admin_b = [
        "games admin status RigPlayer",
        "games admin reset RigPlayer scramble",
    ]
    ok_a = boot("A", admin_a, "RIGREADY STOP-A", "boot-a.log")
    ok_b = boot("B", admin_b, "RIGREADY STOP-B", "boot-b.log")
    print("RIG OVERALL: " + ("PASS" if (ok_a and ok_b) else "FAIL"))
    sys.exit(0 if (ok_a and ok_b) else 1)


if __name__ == "__main__":
    main()
