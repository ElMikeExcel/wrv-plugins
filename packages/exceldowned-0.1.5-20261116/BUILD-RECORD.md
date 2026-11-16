# ExcelDowned 0.1.5 — self-check timing fix: BUILD + RIG REPORT

- Worker: board supervisor run 427, worker W13, Mon Nov 16, 2026
  (America/Chicago). Desk build + mirror rig proof only: **no
  MANIFEST filing, no install, live server and panel never touched.**
- Scope of record: `~/workspace/board-r427/downed-template-diagnosis/DIAGNOSIS.md`
  §5, followed exactly. The optional stage5 template range rider
  (§5: stage5 resurrection `range: 16` → `20`) is **NOT included** —
  it is a separate template-side call, left out per dispatch.

## Candidate

- `ExcelDowned-0.1.5.jar` — **63,701 B**, SHA-256
  **`50d7514934c7f71eaa4840a3024d3cb0594436a6a0409c6c7b2cb21e048b9710`**
- Deterministic double-build byte-identical; fingerprint gate PASS
  (build `EXC-DOWNED-0.1.5-WRV-20261116-01-d0wn3d`). Build log:
  `work/build-0.1.5.log`.
- Hash verified a second time on the mirror at staging
  (50D75149…, 63,701 B — exact match before any boot).

## Base gate — PROVEN

The untouched installed-0.1.4 tree
(`board-r417/downed-014/work/build-0.1.4`, copied to
`work/base-gate-0.1.4/`) was rebuilt BEFORE any edit:

- Rebuilt jar **59,998 B**, SHA-256
  **`703d7f9a45b4c0d00f49bfddb1f404bd5990601ae37e9b60e5f1937599792339`**
  — byte-identical to the installed 0.1.4 candidate of record;
  double-build byte-identical; suite 39/39; fingerprint PASS.
  Log: `work/base-gate-build.log`.

## The fix (vs the 0.1.4 tree — `ExcelDowned-0.1.5.diff`)

Production changes: **one file modified** (`ExcelDowned.java`) plus
**one new pure policy class** (`TemplateCheckPolicy.java`, the
FixPolicy pattern DIAGNOSIS §5 item 5 prescribes). Everything else
in the diff is the version knobs (plugin.yml 0.1.5, BuildInfo
fragment, local-build.sh), tests, and `DESIGN-0.1.5.md`.

1. **Defer + bounded retry:** the self-check no longer runs
   synchronously in `onEnable`. It is scheduled at +200 ticks
   (10 s), with retries at +20 s and +40 s (3 attempts max), via
   `TemplateCheckPolicy.ATTEMPT_DELAY_TICKS = {200, 200, 400}`.
2. **Control key:** every attempt probes `heal` (known-good
   builtin) alongside `resurrection` / `grouprenewal`. Control
   false → registry not yet loaded → one INFO line ("Magic spell
   registry not yet loaded — spell-template self-check deferred
   (attempt N of 3)") and a retry — never a template WARN.
3. **Wording corrected:** the stale "grants of this spell will NPE
   in Magic's wand build" claim is gone. The final WARN reads:
   "spell template '<key>' does NOT resolve in Magic's loaded
   spell registry (final self-check) — the key is absent from the
   loaded registry; check the Magic spells data for this key."
4. **Fail-loud kept:** a key absent from the POPULATED registry
   WARNs exactly once, naming that key only (proven by R2). The
   probe-unavailable branch stays distinct (UNPROVEN wording,
   retried, warned only at the final attempt); a registry that
   never loads ends in the distinct UNPROVEN warning, never a
   per-key template WARN.

## Suite

**44/44 passed, 0 failed** (39 carried + 5 new TemplateCheckPolicy
tests: schedule shape, empty-registry defers and never WARNs early,
deferred→loaded resolves sequence, missing-key-in-loaded-registry
WARNs once, probe-unavailable stays distinct). The 0.1.4 pin test
was updated to 0.1.5 (version + fingerprint lineage).

## Rig proof (Homebase mirror ONLY — `wrv-testserver`, full stack, real Magic 11.2.5)

Route: `ssh -F ~/.ssh/config mike-pc` on every call; Homebase probe
PASS on attempt 1 (DESKTOP-IL05DQR). Two SSH sessions dropped with
broken pipes during boot waits (known relay pattern); both boots
completed detached on the PC and their logs were recovered —
relay-path evidence only, PC never implicated. No fallback used.
Rig probe plugin: `DownedCheckRig427.jar` (3,041 B /
`7e38689419d703391e13b77b097f8bdd8d6a5cd7eba78c7c8ee4432a939ad6bd`;
source in `rig-src/`) — reflective `getSpellTemplate` probes at
+3 s / +25 s, a MagicGate gate attempt (R4), then shutdown.
Evidence: `rig-evidence/boot-R1.log`, `boot-R2.log`, `EXCERPTS.txt`.

- **R1 (end state) — PASS.** Cold mirror boot, candidate staged:
  Downed 0.1.5 enabled 08:35:26; Magic `Loaded 221 spells`
  08:35:31; the deferred check logged `resolves` for BOTH
  `resurrection` (08:35:42) and `grouprenewal` (08:35:42);
  **template WARN count = 0**; ExcelDowned WARN count = 0;
  **SEVERE = 0**. (All 38 WARN lines in the boot are unrelated
  stack noise — WhatsNew long-line notes etc.)
- **R2 (negative control) — PASS.** Mirror `spells.yml` copy with
  the `resurrection:` block removed (222→221 top-level keys, YAML
  re-parsed clean, `heal`/`grouprenewal` intact; edited file
  `rig-evidence/mirror-spells-nores.yml`, 187,250 B /
  `6e7f15e0…`). Boot: Magic `Loaded 220 spells`; the final-check
  WARN fired **exactly once**, naming `resurrection` only, with
  the corrected wording; `grouprenewal` logged `resolves`, no
  WARN; rig probes corroborate (heal=true, resurrection=false,
  grouprenewal=true); SEVERE = 0.
- **R3 (timing evidence) — PARTIAL (ordering proven; the
  deferred-INFO branch did not fire on the mirror, correctly).**
  The defective ordering reproduced: Downed enabled at 08:35:26,
  Magic's spell load completed at 08:35:31 — a synchronous
  onEnable probe (the pre-0.1.5 shape) would have read the empty
  registry in this very boot. On the mirror Magic's load finished
  only ~5 s after Downed's enable, so Downed's first deferred
  attempt (+10 s) already found the populated registry and no
  "deferred" INFO was emitted — the INFO fires only when an
  attempt actually finds the registry unloaded. That branch is
  pinned by the unit suite (empty-registry sequence → defer,
  retry, never WARN early; deferred→loaded sequence → resolves).
  On the live stack (load completes ~+7 s, plus startup tick lag)
  the first attempt may or may not need the retry; both paths end
  at the same verdict logic.
- **R4 (grant sanity) — PARTIAL.** With the populated registry,
  the rig's post-load probe of both keys — the identical
  reflective resolver shape MagicGate's hook uses
  (`getSpellTemplate(key) != null`) — resolves both keys
  (late+25s: heal/resurrection/grouprenewal all true), and the
  boot contains **no skipped-key "Wand inventory sync" WARNING**
  (the only such line is MagicGate's INFO "sync: ON" notice).
  Two limits, stated plainly: (a) the mirror's ExcelMagicGate is
  **0.3.1**, which predates `SpellTemplateGate` (added 0.3.2), so
  the gate class itself could not be invoked on the mirror
  (rig logged `R4 GATE unavailable: ClassNotFoundException`);
  live carries 0.3.3. (b) No player joined the mirror, so no
  actual join-time wand sync ran. The registry-resolution fact
  the WARN wording used to condemn is proven; a literal 0.3.3
  join-time sync proof remains available on the install boot /
  S7 session window.

**Mirror restoration — verified:** cleanup restored the pre-run
state exactly: plugin census IDENTICAL to the pre-run census,
Downed jar back to 0.1.4 (`703D7F9A…`), mirror `spells.yml`
restored (`68006831…`, matching the pre-run pull), ExcelDowned
data folder restored from backup, JAVA NONE. PC staging left at
`C:\Users\MikeExcel\muse-temp\downed015rig\` per house pattern.

## Remaining chain (exact)

1. **G1 filing (supervisor):** fresh same-day live-premise pull
   (live ExcelDowned jar re-pulled via Homebase rclone — expected
   0.1.4 `703d7f9a…`, 59,998 B — never cited from this report),
   then a pointer-stub PENDING entry in
   `~/workspace/wrv-install-queue/MANIFEST.md` per
   PACKET-TEMPLATE.md naming this candidate (63,701 B /
   `50d75149…`). Jar change = fully gated install.
2. **Failsafe install:** BACKUP-FIRST (cap-at-6), emptiness gate,
   swap, verification boot — the install boot log is the WARN
   leg's live proof (expected: 0 template WARNs, both keys logged
   `resolves`; possibly one deferred-INFO line first if Magic's
   load outruns the +10 s attempt). Same-pass surfaces owed at
   install (Guide, What's New BOTH surfaces, website pair, tips,
   menus check).
3. **S7 pairing:** the owed S7 live retest (real downed player,
   real Resurrection cast, named volunteer) is scheduled in the
   same player-session window as the install verification — it
   remains a separate, still-owed proof of the revive path
   itself; this fix neither needs nor replaces it, and a literal
   MagicGate 0.3.3 join-time sync observation (R4's remainder)
   can be read off that same session's log.

## Files

- `ExcelDowned-0.1.5.jar` — the candidate (also in
  `work/build-0.1.5/` with its repro jar).
- `ExcelDowned-0.1.5.diff` — full diff vs the 0.1.4 base tree.
- `work/base-gate-0.1.4/` + `work/base-gate-build.log` — base gate.
- `work/build-0.1.5/` — the 0.1.5 tree of record (src, tests,
  DESIGN-0.1.5.md, local-build.sh) + `work/build-0.1.5.log`.
- `rig-src/` + `DownedCheckRig427.jar` — rig probe source + jar.
- `rig-evidence/` — boot-R1.log, boot-R2.log, EXCERPTS.txt,
  mirror spells files (orig / no-resurrection / removed block).
- `SHA256SUMS` — hashes for the deliverables above.
