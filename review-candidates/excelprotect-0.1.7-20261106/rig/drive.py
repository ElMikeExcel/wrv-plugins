#!/usr/bin/env python3
"""ExcelProtect 0.1.7 "Parity" acceptance rig: boots the sandbox
(Protect 0.1.7 + RigElites fake-player driver + WorldEdit 7.4.6 + real
EliteMobs 10.9.12) and re-runs: every 0.1.6 rollback case (A sign
round-trip + preview + undo, B container round-trip both directions,
C explosion batch, B2 container undo, F1 export bounds + loud malformed
errors, D entity rollback, E inventory rollback, F WorldEdit session)
PLUS the 0.1.7 cases: G DeathRecord importer round-trip on a synthetic
Kailey-shaped fixture set (restored marks honored, LF state migrated,
second import all-skipped) and H kill-field parity (fall-softened sword
kill + a genuine EliteMobs elite kill carrying elite=true)."""
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
for name in ["restore-marks.json", "lostfound-players.json", "imported-lf-state.json", "imported-sources.json"]:
    f = os.path.join(data_dir, name)
    if os.path.exists(f):
        shutil.copy(f, ev)
        print(f"RIG: {name} -> evidence")

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
         "E_inventory_replace", "E_inventory_onceonly", "E_inventory_force", "F_worldedit_applied",
         "G_fixtures_written", "G_import_restored_marks", "G_import_lf_state",
         "H_elite_registered"]
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
verdict("READY 0.1.7", any("READY version=0.1.7" in l for l in lines))
verdict("importer boot line (0.1.7)", any("importer: deathrecord honors restored marks" in l for l in lines))
verdict("kill parity boot line (0.1.7)", any("kill parity: elite/weapon/origin-class/damage-shares" in l for l in lines))
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
verdict("E death snapshot recorded with payloads", len([s for s in snaps if not s.get("imported")]) == 1
        and any(i.get("bytes") for i in [s for s in snaps if not s.get("imported")][0].get("inventory", []) if i),
        f"snapshots={len(snaps)} (1 live + imported)")

# ---- G (0.1.7): importer round-trip, stream + system side
imp_snaps = [r for r in snaps if r.get("imported")]
imp_burns = [r for r in deaths if r.get("type") == "item_destroyed" and r.get("imported")]
verdict("G imported burns emitted (6)", len(imp_burns) == 6, f"imported item_destroyed={len(imp_burns)}")
verdict("G restored burns stamped (4)", len([r for r in imp_burns if r.get("source_restored")]) == 4,
        f"source_restored lines={len([r for r in imp_burns if r.get('source_restored')])}")
verdict("G imported deaths emitted (2)", len(imp_snaps) == 2, f"imported snapshots={len(imp_snaps)}")
mixed = next((r for r in imp_snaps if str(r.get("death_id", "")).endswith("g-death-mixed.yml")), None)
verdict("G mixed death: restored item excluded, xp kept",
        mixed is not None and len(mixed.get("inventory", [])) == 2
        and mixed.get("source_restored_items") == 1 and mixed.get("xp_level") == 27,
        f"mixed={None if mixed is None else (len(mixed.get('inventory', [])), mixed.get('source_restored_items'), mixed.get('xp_level'))}")
full = next((r for r in imp_snaps if str(r.get("death_id", "")).endswith("g-death-full.yml")), None)
verdict("G full death: no xp_level, xp_restored stamped, empty inventory",
        full is not None and "xp_level" not in full and full.get("xp_restored_in_source") is True
        and len(full.get("inventory", [])) == 0,
        f"full keys xp_level={'xp_level' in full if full else 'n/a'} xp_restored={full.get('xp_restored_in_source') if full else 'n/a'}")
imports = [r for r in system if r.get("type") == "import"]
verdict("G import #1 counts (2d/6b/0s/0e, 4 restored burns, lf 1)",
        len(imports) >= 1 and imports[0].get("deaths") == 2 and imports[0].get("burns") == 6
        and imports[0].get("skipped") == 0 and imports[0].get("errors") == 0
        and imports[0].get("restored_burns") == 4 and imports[0].get("restored_items") == 2
        and imports[0].get("lf_state") == 1,
        f"import lines={[(i.get('deaths'), i.get('burns'), i.get('skipped'), i.get('errors'), i.get('restored_burns'), i.get('restored_items'), i.get('lf_state')) for i in imports]}")
verdict("G import #2 all-skipped (idempotent)",
        len(imports) >= 2 and imports[1].get("deaths") == 0 and imports[1].get("burns") == 0
        and imports[1].get("skipped") == 8 and imports[1].get("errors") == 0,
        f"second import line={imports[1] if len(imports) >= 2 else None}")

# ---- H (0.1.7): kill parity on the entities stream
entities = load("entities-*.jsonl")
h1_uuid = next((l.split("uuid=", 1)[1].split(" ", 1)[0] for l in lines if "H1 zombie uuid=" in l), None)
h2_uuid = next((l.split("uuid=", 1)[1].split(" ", 1)[0] for l in lines if "H2 zombie uuid=" in l), None)
h1 = next((r for r in entities if r.get("type") == "entity_death" and r.get("entity_uuid") == h1_uuid), None)
verdict("H1 normal kill parity (elite=false, sword, origin_class, fall share)",
        h1 is not None and h1.get("elite") is False
        and (h1.get("weapon") or {}).get("id") == "minecraft:iron_sword"
        and h1.get("origin_class") == "other" and "rg_denied" not in h1
        and (h1.get("damage") or {}).get("fall_share", 0) > 0.5
        and (h1.get("damage") or {}).get("explosion_share") == 0.0
        and h1.get("fall_softened") is True
        and (h1.get("killer") or {}).get("kind") == "player",
        f"h1={None if h1 is None else {k: h1.get(k) for k in ('elite', 'origin_class', 'fall_softened')}}"
        f" weapon={(h1 or {}).get('weapon')} damage={(h1 or {}).get('damage')}")
h2 = next((r for r in entities if r.get("type") == "entity_death" and r.get("entity_uuid") == h2_uuid), None)
verdict("H2 ELITE kill parity (elite=true, level 5, weapon, shares present)",
        h2 is not None and h2.get("elite") is True and h2.get("elite_level") == 5
        and (h2.get("weapon") or {}).get("id") == "minecraft:iron_sword"
        and h2.get("origin_class") is not None
        and "fall_share" in (h2.get("damage") or {}) and "explosion_share" in (h2.get("damage") or {})
        and "fall_softened" in h2,
        f"h2={None if h2 is None else {k: h2.get(k) for k in ('elite', 'elite_level', 'origin_class', 'fall_softened')}}"
        f" weapon={(h2 or {}).get('weapon')} damage={(h2 or {}).get('damage')}")

severe = [l for l in lines if "SEVERE" in l]
protect_severe = [l for l in severe if "ExcelProtect" in l or "excelprotect" in l]
print(f"ANALYSIS severe-lines total={len(severe)} protect-attributable={len(protect_severe)}")
for l in severe[:5]: print("  SEVERE:", l[:200])
print("RIG: done")
