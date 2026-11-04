# Magic spell packets #1–#3 + Familiar/Cat cooldown — config record

Plugin: Magic (upstream, by NathanWolf — NOT our code; this record covers only our configuration work)  
Record assembled: Nov 3, 2026 (records catch-up pass)

## What this is

The Nov 2–3, 2026 spell-tuning packets applied to the live Magic spells.yml files and the MagicGate stage templates (stages 2–5 in the packet folders; the live server runs the stage-3 template). Packet #1 (spell logic fixes), packet #2 (arrow-family full-draw fix), and packet #3 (Familiar entity_parameters removal, Boomerang cooldown, Workbench rank-4 restore) installed Nov 2, 2026; the Familiar/Cat cooldown packet (both cooldowns set to exactly 240,000 ms at Mike's decision) installed Nov 3, 2026, 10:11–10:37 AM CST.

## Final live state (verified hashes)

- live spells.yml + stage-3 template: SHA-256 5c340235a002134c9a9e051f1a3dc4580cc328bdba33432d9a6d5fb617022b69
- stage-4 template: SHA-256 14fc6112332c92d08b3bd54db8f21a6ca59a8836f5cf1ce672a49f4fde4725ea
- stage-5 template: SHA-256 bf30449cd4ecd1afbc9e7db0f7d70c9b7174b64e3760f648f29c33480005b03f

## Contents

- `packet-1/`, `packet-2/`, `packet-3/` — each packet's docs and its per-stage spells.yml outputs, exactly as filed in the local packet folders
- `familiar-cat-cd/` — the Nov 3 cooldown packet: final live + stage files and harness verdicts
- `TEST-RESULTS.txt` — spell logic-test harness outputs
- `FUNCTIONAL-CHANGE.diff` — the final delta (packet-3 stage files → Familiar/Cat packet outputs)
