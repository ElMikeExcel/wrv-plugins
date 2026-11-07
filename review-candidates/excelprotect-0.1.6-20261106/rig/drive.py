#!/usr/bin/env python3
"""ExcelProtect 0.1.6 "Recovery" acceptance rig: boots the sandbox
(Protect 0.1.6 + RigRecovery fake-player driver + an assembled WorldEdit
7.2.x plugin jar) and runs: every Nov 6 rollback case (A sign round-trip
+ preview + undo, B container round-trip both directions, C explosion
batch) PLUS B2 container undo, F1 export bounds + loud malformed errors,
D entity rollback (stand + tamed wolf, preview/once-only/force),
E inventory rollback (preview counts, REPLACE, once-only, force),
F WorldEdit session recording."""
import subprocess, sys, threading, time, os, shutil, glob, json

rig_dir = os.path.dirname(os.path.abspath(__file__))
root = os.path.dirname(rig_dir)
server_dir = os.path.join(rig_dir, "server")
data_dir = os.path.join(server_dir, "plugins", "ExcelProtect")
ev = os.path.join(root, "rig-evidence")
java = os.path.expanduser("~/workspace/.jdk25/bin/java")

proc = subprocess.Popen([java, "-Xmx1G", "-jar", "paper.jar", "nogui"],
                        cwd=server_dir, stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                        stderr=subprocess.STDOUT, text=True, bufsize=1)
lines, done, rigdone = [], threading.Event(), threading.Event()
def reader():
    for line in proc.stdout:
        lines.append(line.rstrip("\n"))
        print(lines[-1], flush=True)
        if "Done (" in line: done.set()
        if "RIGDONE" in line: rigdone.set()
threading.Thread(target=reader, daemon=True).start()
if not done.wait(timeout=300):
    print("RIG: server did not reach Done in 300s"); proc.kill(); sys.exit(1)
if not rigdone.wait(timeout=600):
    print("RIG: scenarios did not reach RIGDONE in 600s"); proc.kill(); sys.exit(1)
time.sleep(6)  # ledger flush is 1s; let the last records land
print(">>> CONSOLE: stop", flush=True)
try:
    proc.stdin.write("stop\n"); proc.stdin.flush()
    proc.wait(timeout=90)
except Exception:
    proc.kill()

os.makedirs(ev, exist_ok=True)
with open(os.path.join(ev, "boot-recovery.log"), "w") as f: f.write("\n".join(lines) + "\n")
for pat in ["blocks-*.jsonl", "containers-*.jsonl", "system-*.jsonl", "admin-*.jsonl",
            "entities-*.jsonl", "deaths-*.jsonl"]:
    for f in glob.glob(os.path.join(data_dir, pat)):
        shutil.copy(f, ev)
        print(f"RIG: {os.path.basename(f)} -> evidence ({sum(1 for _ in open(f))} lines)")
for f in glob.glob(os.path.join(data_dir, "undo-*.jsonl*")):
    shutil.copy(f, ev)
    print(f"RIG: {os.path.basename(f)} -> evidence (undo file)")
for f in glob.glob(os.path.join(data_dir, "exports", "*.jsonl")):
    shutil.copy(f, os.path.join(ev, "export-" + os.path.basename(f)))
    print(f"RIG: exports/{os.path.basename(f)} -> evidence")

# ---------------------------------------------------------------- analysis
def load(pat):
    out = []
    for f in sorted(glob.glob(os.path.join(ev, pat))):
        for line in open(f):
            try: out.append(json.loads(line))
            except Exception: pass
    return out

def verdict(name, ok, detail=""):
    print(f"ANALYSIS {name}: {'PASS' if ok else 'FAIL'} {detail}")

log_text = "\n".join(lines)
CASES = ["A_sign_roundtrip", "A_place_reverted", "A_oldblock_restored", "A_undo_roundtrip",
         "B_container_roundtrip", "B_container_latest", "B2_undo_file_captures_container",
         "B2_container_undo", "C_explosion_rollback",
         "D_entity_stand_restored", "D_entity_wolf_restored", "D_entity_onceonly", "D_entity_force",
         "E_inventory_replace", "E_inventory_onceonly", "E_inventory_force", "F_worldedit_applied"]
for case in CASES:
    verdict(f"rig {case}", f"RIGRESULT {case} PASS" in log_text,
            next((l.split('RIGRESULT ', 1)[1] for l in lines if f"RIGRESULT {case}" in l), "no result line"))

verdict("A preview line printed", "PREVIEW: would restore 3 block position(s) and 0 container(s)" in log_text,
        next((l for l in lines if "PREVIEW: would restore 3 block" in l), "no A PREVIEW line"))
verdict("D entity preview line", "PREVIEW: would restore minecraft:armor_stand 'RigStand'" in log_text,
        next((l for l in lines if "PREVIEW: would restore minecraft:" in l), "no entity PREVIEW line"))
inv_prev = next((l for l in lines if "PREVIEW: would REPLACE" in l), "")
verdict("E inventory preview counts", "4 inventory stacks, 1 ender-chest stacks" in inv_prev, inv_prev[:200])
verdict("once-only refusals spoken (entity + inventory)", log_text.count("already restored (staff: add force)") >= 2,
        f"occurrences={log_text.count('already restored (staff: add force)')}")
verdict("READY 0.1.6", any("READY version=0.1.6" in l for l in lines))
verdict("worldedit logging ON at boot", any("worldedit logging: ON" in l for l in lines),
        next((l for l in lines if "worldedit logging:" in l), "no worldedit boot line"))

blocks = load("blocks-*.jsonl")
tnt = [r for r in blocks if r.get("cause") == "minecraft:tnt"]
ids = {r.get("event_id") for r in tnt}
verdict("explosion batch one event id", len(tnt) >= 10 and len(ids) == 1 and None not in ids,
        f"tnt block_change lines={len(tnt)} distinct event_ids={ids}")
sign_rec = [r for r in blocks if r.get("sign_before") == ["WRV", "ROLLBACK", "PROOF", "2026"]]
verdict("sign break recorded with sign_before", len(sign_rec) == 1, f"matching lines={len(sign_rec)}")
we_lines = [r for r in blocks if r.get("action") == "worldedit"]
verdict("F worldedit blocks recorded w/ actor", len(we_lines) >= 3
        and all(r.get("player") == "RigRoll" for r in we_lines)
        and all(str(r.get("after", "")).startswith("minecraft:stone_bricks") for r in we_lines),
        f"worldedit lines={len(we_lines)} players={[r.get('player') for r in we_lines]}")

conts = load("containers-*.jsonl")
txns = [r for r in conts if r.get("type") == "container_transaction"]
verdict("two container transactions", len(txns) == 2, f"txns={len(txns)}")
if len(txns) == 2:
    lens = sorted(len(t.get("contents_after", [])) for t in txns)
    verdict("contents_after snapshots 1 then 2 items", lens == [1, 2], f"snapshot sizes={lens}")
    pl = [i for t in txns for i in t.get("contents_after", [])]
    verdict("snapshots carry byte payloads", bool(pl) and all(i.get("bytes") for i in pl),
            f"items={len(pl)} payloaded={sum(1 for i in pl if i.get('bytes'))}")

# F1: the bounded export file must contain only in-window block events
exp_files = sorted(glob.glob(os.path.join(ev, "export-export-*.jsonl")))
f1_ok, f1_detail = False, "no export file copied"
if exp_files:
    recs = []
    for line in open(exp_files[-1]):
        try: recs.append(json.loads(line))
        except Exception: pass
    events = [r for r in recs if r.get("type") != "export_meta"]
    f1_ok = len(events) >= 3 and all(r.get("type") == "block_change" for r in events)
    f1_detail = f"export file lines={len(recs)} events={len(events)} types={sorted({r.get('type') for r in events})}"
verdict("F1 export with from=/to= bounds returns in-window events", f1_ok, f1_detail)

admin = load("admin-*.jsonl")
acts = [r.get("action") for r in admin if r.get("type") == "admin"]
verdict("admin self-logging covers preview/rollback/undo",
        "rollback_preview" in acts and "rollback" in acts and "rollback_undo" in acts,
        f"admin actions={acts}")
verdict("F1 malformed filters fail loudly (export + rollback)",
        "export_error" in acts and "rollback_error" in acts
        and any("banana" in str(r.get("detail", "")) for r in admin if r.get("action") == "export_error"),
        f"error actions={[a for a in acts if 'error' in str(a)]}")
verdict("D/E restore admin lines", "restore_entity" in acts and "restore_inventory" in acts,
        f"restore actions={[a for a in acts if 'restore' in str(a)]}")

system = load("system-*.jsonl")
rb = [r for r in system if r.get("type") == "rollback"]
er = [r for r in rb if r.get("action") == "entity_restore"]
ir = [r for r in rb if r.get("action") == "inventory_restore"]
verdict("entity + inventory restores logged in system stream", len(er) >= 2 and len(ir) >= 2,
        f"entity_restore={len(er)} inventory_restore={len(ir)} (commit + force each)")
ops = [r for r in system if r.get("type") == "rollback_op"]
verdict("rollback_op lines carry undo_containers (0.1.6)", len(ops) >= 3
        and all("undo_containers" in r for r in ops), f"ops={len(ops)} undo_containers={[r.get('undo_containers') for r in ops]}")

deaths = load("deaths-*.jsonl")
snaps = [r for r in deaths if r.get("type") == "death_snapshot"]
verdict("E death snapshot recorded with payloads", len(snaps) == 1
        and any(i.get("bytes") for i in snaps[0].get("inventory", []) if i),
        f"snapshots={len(snaps)}")

severe = [l for l in lines if "SEVERE" in l]
protect_severe = [l for l in severe if "ExcelProtect" in l or "excelprotect" in l]
print(f"ANALYSIS severe-lines total={len(severe)} protect-attributable={len(protect_severe)}")
for l in severe[:5]: print("  SEVERE:", l[:200])
print("RIG: done")
