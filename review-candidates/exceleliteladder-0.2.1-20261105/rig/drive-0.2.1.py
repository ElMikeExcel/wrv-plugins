#!/usr/bin/env python3
"""ExcelEliteLadder 0.2.1 rig driver: boot the sandbox, run console commands, stop.
Usage: drive-0.2.1.py <mode A|B> <evidence-log-name>"""
import subprocess, sys, threading, time, os

mode = sys.argv[1]
evidence_name = sys.argv[2]
server_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), "server")
java = os.path.expanduser("~/workspace/.jdk25/bin/java")
proc = subprocess.Popen([java, "-Xmx1G", "-jar", "paper.jar", "nogui"],
                        cwd=server_dir, stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                        stderr=subprocess.STDOUT, text=True, bufsize=1)
lines, done, em_ready = [], threading.Event(), threading.Event()
def reader():
    for line in proc.stdout:
        lines.append(line.rstrip("\n"))
        print(lines[-1], flush=True)
        if "Done (" in line: done.set()
        if "EliteMobs fully initialized" in line: em_ready.set()
threading.Thread(target=reader, daemon=True).start()

if not done.wait(timeout=240):
    print("RIG: server did not reach Done in 240s"); proc.kill(); sys.exit(1)
time.sleep(3)  # let async housekeeping settle

def send(c, wait=4):
    print(f">>> CONSOLE: {c}", flush=True)
    proc.stdin.write(c + "\n"); proc.stdin.flush()
    time.sleep(wait)

if mode == "A":
    send("eliteladder status", 5)
    send("eliteladder reload", 5)
    # EM initializes ASYNC, well after Done; conversions only happen once it
    # is fully up (proven by boots A/C). Wait for its own ready line.
    print("RIG: waiting for EliteMobs full initialization...", flush=True)
    if not em_ready.wait(timeout=180):
        print("RIG: EM never reported full initialization", flush=True)
    time.sleep(2)
    # Summon persistent+invulnerable zombies until EM converts some into
    # elites (announcement names the mob). Then read the elite's effects NBT
    # with a DIRECT data get (feedback returns to console; execute-as would
    # swallow it into the mob).
    send("forceload add 0 0", 3)
    for _ in range(12):
        send("summon minecraft:zombie 0.5 68 0.5 {PersistenceRequired:1b,Invulnerable:1b}", 1)
    time.sleep(5)
    send('data get entity @e[name="Lvl 0 Elite Zombie",limit=1] ActiveEffects', 5)
    send('data get entity @e[name="Lvl 0 Elite Zombie",limit=1] CustomName', 3)
    send("kill @e[type=minecraft:zombie]", 3)
    send("forceload remove 0 0", 2)
elif mode == "C":
    # Diagnostic: mobs that cannot die (Invulnerable), wander (NoAI), despawn
    # (PersistenceRequired) or unload (forceload) — whatever the dump shows is
    # the truth about conversion + the buff.
    send("forceload add 0 0", 3)
    for _ in range(10):
        send("summon minecraft:zombie 0.5 68 0.5 {PersistenceRequired:1b,NoAI:1b,Invulnerable:1b}", 0.5)
    time.sleep(8)
    send("execute as @e[type=minecraft:zombie] run data get entity @s CustomName", 6)
    send("execute as @e[type=minecraft:zombie] run data get entity @s ActiveEffects", 8)
    send("kill @e[type=minecraft:zombie]", 3)
    send("forceload remove 0 0", 2)
elif mode == "B":
    send("eliteladder status", 5)
else:
    print("unknown mode"); proc.kill(); sys.exit(1)

send("stop", 2)
try: proc.wait(timeout=90)
except subprocess.TimeoutExpired: proc.kill()

ev_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), "evidence")
os.makedirs(ev_dir, exist_ok=True)
log_path = os.path.join(ev_dir, evidence_name)
with open(log_path, "w") as f: f.write("\n".join(lines) + "\n")
print(f"RIG: transcript -> {log_path} ({len(lines)} lines), exit={proc.returncode}")
