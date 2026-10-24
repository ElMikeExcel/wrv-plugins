#!/usr/bin/env python3
"""Display pins for ExcelAttunement 0.1.2 (source-level assertions).

The plain-JVM suites (run-tests.sh) cover only the Bukkit-free core;
the stance DISPLAY code lives in the Bukkit glue class and has no JVM
coverage. These pins lock the display deltas at the source level:

  (a) [0.1.1] tick() contains NO sendActionBar call (the per-second
      resend that Mike found distracting, removed in 0.1.1);
  (b) [0.1.1 + 0.1.2] the sendActionBar helper sends the bare stance
      label only: exactly Component.text(label), no remainingSeconds,
      no countdown suffix; [0.1.2] the label colors are DIMMED -
      martial section-7 gray, caster section-3 dark aqua - and the
      old bright forms (section-6 gold / section-b aqua) are gone
      from the helper. Action-bar text has no opacity control, so
      dimmer color is the only "more transparent" the game allows;
  (c) the cast-blocked / stance-set feedback is byte-identical to
      the 0.1.1 source (deposited ExcelAttunement-0.1.1-source.zip):
      the precise-seconds CHAT messages keep their bright colors and
      stay untouched - that warning must stay noticeable;
  (d) the sendActionBar call-site inventory is exactly the three
      event-driven sites (onCastBlocked, onStanceSet, onDamageTrigger)
      plus the definition - no site added, none lost silently;
  (e) [0.1.2] whole-file delta scope vs the 0.1.1 source: exactly ONE
      line differs in ExcelAttunement.java and it is the label line
      (old: section-6/section-b, new: section-7/section-3).

Usage: display_pins.py [path-to-ExcelAttunement.java] [path-to-0.1.1-source.zip]
Defaults resolve relative to this script (tests/ sits under the project
root; the 0.1.1 zip sits two levels up).
"""
import io
import sys
import zipfile
from pathlib import Path

HERE = Path(__file__).resolve().parent
JAVA_REL = Path("src/com/winkreyes/excelattunement/ExcelAttunement.java")

java_path = Path(sys.argv[1]) if len(sys.argv) > 1 else HERE.parent / JAVA_REL
zip_path = Path(sys.argv[2]) if len(sys.argv) > 2 else HERE.parents[2] / "ExcelAttunement-0.1.1-source.zip"

fails = []


def check(name, ok, detail=""):
    print(("PASS " if ok else "FAIL ") + name + ((" - " + detail) if detail else ""))
    if not ok:
        fails.append(name)


def method_body(src, sig):
    """Return the body text of the first method whose declaration contains sig."""
    i = src.index(sig)
    j = src.index("{", i)
    depth = 0
    for k in range(j, len(src)):
        if src[k] == "{":
            depth += 1
        elif src[k] == "}":
            depth -= 1
            if depth == 0:
                return src[j:k + 1]
    raise ValueError("unbalanced braces after " + sig)


def enclosing_method(src, pos):
    """Name of the method whose body contains offset pos (last 'void name(' before pos that opens a body spanning pos)."""
    import re
    best = None
    for m in re.finditer(r"(?:public|private|protected)\s+(?:static\s+)?[\w<>\[\]]+\s+(\w+)\s*\(", src):
        try:
            body = method_body(src[m.start():], src[m.start():src.index("(", m.start())])
        except Exception:
            continue
        start = src.index("{", m.start())
        if m.start() <= pos <= m.start() + (src.index("{", m.start()) - m.start()) + len(body):
            if best is None or m.start() > best[0]:
                best = (m.start(), m.group(1))
    return best[1] if best else "?"


new_src = io.open(java_path, encoding="utf-8").read()
with zipfile.ZipFile(zip_path) as z:
    old_src = z.read("ExcelAttunement/" + JAVA_REL.as_posix()).decode("utf-8")

# --- pin (a): tick() has no sendActionBar -------------------------------
tick = method_body(new_src, "private void tick(")
check("(a) tick() contains no sendActionBar call", "sendActionBar" not in tick)
check("(a) tick() still sends the stance-fades expiry message",
      "Your stance fades - wand and weapon are both free again." in tick)

# --- pin (b): bare label, no countdown, DIMMED colors -------------------
helper = method_body(new_src, "private void sendActionBar(")
check("(b) sendActionBar sends exactly Component.text(label)",
      "player.sendActionBar(Component.text(label));" in helper)
check("(b) sendActionBar has no remainingSeconds / countdown suffix",
      "remainingSeconds" not in helper and '"s"' not in helper and '" \u00a77- "' not in helper)
check("(b) stance labels DIMMED (martial \u00a77 gray, caster \u00a73 dark aqua)",
      '"\u00a77Martial stance"' in helper and '"\u00a73Caster stance"' in helper)
check("(b) bright label colors gone from the helper (\u00a76 gold / \u00a7b aqua)",
      '"\u00a76Martial stance"' not in helper and '"\u00a7bCaster stance"' not in helper)

# --- pin (c): blocked/set feedback byte-identical to 0.1.1 --------------
for sig, label in [("public void onCastBlocked(", "onCastBlocked"),
                   ("public void onStanceSet(", "onStanceSet")]:
    same = method_body(new_src, sig) == method_body(old_src, sig)
    check(f"(c) {label} byte-identical to 0.1.1", same)
check("(c) precise-seconds set message intact (bright chat colors kept)",
      '"\u00a76Martial stance \u00a77- your wand is stilled for " + windowSeconds + "s."' in new_src)
check("(c) precise-seconds blocked debug line intact",
      '" (martial stance, " + state(player.getUniqueId()).remainingSeconds(now()) + "s left)"' in new_src)

# --- pin (d): call-site inventory ----------------------------------------
import re
sites = set()
for m in re.finditer(r"sendActionBar\(", new_src):
    name = enclosing_method(new_src, m.start())
    sites.add(name)
check("(d) sendActionBar sites == {definition, onCastBlocked, onStanceSet, onDamageTrigger}",
      sites == {"sendActionBar", "onCastBlocked", "onStanceSet", "onDamageTrigger"},
      "found: " + ", ".join(sorted(sites)))

# --- pin (e): whole-file delta scope vs 0.1.1 ----------------------------
new_lines = new_src.splitlines()
old_lines = old_src.splitlines()
diff = [(o, n) for o, n in zip(old_lines, new_lines) if o != n] if len(old_lines) == len(new_lines) else None
check("(e) file length unchanged vs 0.1.1 (one-line delta)", diff is not None,
      f"old {len(old_lines)} lines, new {len(new_lines)} lines")
if diff is not None:
    check("(e) exactly ONE line differs vs 0.1.1", len(diff) == 1,
          f"{len(diff)} differing line(s)")
    if len(diff) == 1:
        o, n = diff[0]
        check("(e) the differing line is the stance label line (colors only)",
              "Martial stance" in o and "Caster stance" in o
              and "\u00a76Martial stance" in o and "\u00a7bCaster stance" in o
              and "\u00a77Martial stance" in n and "\u00a73Caster stance" in n
              and o.replace("\u00a76", "\u00a77").replace("\u00a7b", "\u00a73") == n,
              "old: " + o.strip() + " | new: " + n.strip())

print("TOTAL: %d failed" % len(fails))
sys.exit(1 if fails else 0)
