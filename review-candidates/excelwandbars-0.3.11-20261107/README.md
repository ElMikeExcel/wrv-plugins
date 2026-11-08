# ExcelWandBars 0.3.11 — record package

The compass-on-wand-bar fix, commissioned by Mike Nov 7, 2026 (5:28 PM CST) after his 5:18 PM Bedrock repro ("when I spawn in I still get a compass on my wand bar"). Design of record: `~/workspace/wandbars-ux-build/COMPASS-ON-WANDBAR-INVESTIGATION.md`. Build folder: `~/workspace/wandbars-ux-build/build-0.3.11/`.

Two changes, nothing else:
1. **Quit close** — an open spell bar is closed as the player leaves, from Paper's connection-close event (before Magic's quit handling reads the wand's open state for its restore decision), with a quit-handler belt for `logout_delay > 0`. No join ever starts on the wand bar again, so the join-time compass give checks the real inventory and never duplicates into the bar.
2. **Join sweep** — the join guard's final check runs the existing ExitSweep once when it leaves a bar open, evicting compasses baked into saved pages by pre-0.3.11 logouts into the player's real inventory.

Candidate only — NOT installed. Install packet: `INSTALL-PACKET-0.3.11.md`; MANIFEST entry filed PENDING (gated) Nov 7, 2026. Tests: 432 pure checks + rig 39 checks, 0 failed (TEST-RESULTS.txt). Remaining legs: PUSH-NOTES.md.
