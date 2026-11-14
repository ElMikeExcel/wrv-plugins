# RIG REPORT — ExcelExpansions 0.1.1 MIRROR RIG

**Date:** Sat Nov 14, 2026, 03:44–03:47 CST (America/Chicago) — desk verification + PC-route probe window
**Worker:** board supervisor run 331 rig lane (front-of-line, expansion programme, Mike's Nov 14 priority law)
**Scope:** PC test-server mirror ONLY — `C:\Users\MikeExcel\wrv-testserver\server\` on DESKTOP-IL05DQR via the PC route. NEVER the live server. No MANIFEST edits, no installs to live. None were made.
**Outcome:** **GATED ON THE PC ROUTE** — mirror placement, boot, and all release-path steps are NOT RUN. This is a gated report, per task instruction; it is not a rig pass or a rig failure of the plugin.

---

## 1. Candidate of record — desk-side verification (PASS)

Verified desk-side BEFORE any PC contact, per task STOP rule.

| Item | Expected (task / D51 + work-advance run 251 reconcile) | Actual (desk, this run) | Verdict |
|---|---|---|---|
| File | `~/workspace/excelexpansions-011-build-20261114/ExcelExpansions-0.1.1.jar` | present | — |
| Size | 61,736 B | 61,736 B (`stat`: 61736, mtime 2026-11-14 08:54:22 UTC) | ✅ MATCH |
| SHA-256 (full) | `b9d26233…` (full value in BUILD-RECORD-011.md reconciliation) | `b9d26233fbef56520b4d9bd8d200093d85fa103e7ae51d17b50b729f9749ba91` | ✅ MATCH — full hash, not prefix-only |
| Barred jar | 203464a6… preserved at `work/pkg011-A.jar`, BARRED from this rig | `work/pkg011-A.jar` = 61,081 B / `203464a63d39c6c96fde7cac21ce0fcaeadf57c9b7db457056bbaca0ac7f3e7c`; `work/pkg011-B.jar` = same hash. Neither copied, placed, nor used in any step of this run. | ✅ BARRED JAR NOT USED |
| Corroborating build copies | continuation-lane deterministic copies at `work/pkgA.jar` / `work/pkgB.jar` | `work/pkgA.jar` re-hashed this run: `b9d26233fbef56520b4d9bd8d200093d85fa103e7ae51d17b50b729f9749ba91` (identical to candidate) | ✅ CONSISTENT |
| Build record | `BUILD-RECORD-011.md` in same folder; 91/91 checks (51 Watcher + 40 Release) | Read in full this run. Reconciliation section confirms: FINAL candidate is the current-src rebuild (61,736 B / b9d26233…), superseding 203464a6… at the root path; `SHA256SUMS-011.txt` regenerated after; embedded config.yml = src = `3a2de1b3…` (2,991 B), embedded plugin.yml = src = `4be4101f…` (740 B), embedded releases.yml = `9fa280f3…` (55,651 B), 36 jar entries | ✅ READ |
| Desk test record | `TEST-RESULTS-011.txt`: WatcherTest 51/0 + ReleaseTest 40/0 = 91/0 | File present, contents match that count (51 PASS lines + `RESULT pass=51 fail=0`; 40 PASS lines + `RESULT pass=40 fail=0`) | ✅ DESK SUITE ON RECORD (desk evidence only — not a substitute for mirror proof) |
| Config of record | Mike-only tap cards; Mike's Java + Bedrock/Floodgate UUIDs in config; rundown command; FINAL manifest data `releases.yml` | `config.yml` (src, 2,991 B): `proposal.mike-uuids` = `7401bc22-d42a-4492-a451-374bd12d9e2c` (Java) + `00000000-0000-0000-0009-01feb74ea7be` (Bedrock/Floodgate), `proposal.allow-op: true`; host code (`ExcelExpansions.java`) authenticates BEFORE ReleaseService: console always, listed UUID, or op when allow-op; wrong token rejected from anyone. `plugin.yml` (740 B): version 0.1.1, usage `[status|list|store|rundown|tap <row id> <token>]`. `releases.yml`: FINAL manifest data, 57 rows. | ✅ AS DESCRIBED |

**STOP-rule outcome:** no hash mismatch — the probe phase was authorised to proceed. Had the full hash differed, this report would have stopped at this section.

---

## 2. PC route — probe attempts (GATED)

Retry pattern per task: up to 3 attempts over ~2 minutes, `ssh mike-pc hostname` (alias: HostName 100.72.156.1, User mikeexcel, ProxyCommand via `~/workspace/pc-access/ts-connect.py`). No further attempts were made after attempt 3 — no hammering.

| # | Time (CST, Nov 14, 2026) | Command | Result | stderr (verbatim) | Exit |
|---|---|---|---|---|---|
| 1 | 03:44:24 | `timeout 30 ssh -o ConnectTimeout=20 mike-pc hostname` | FAIL — no hostname returned | `kex_exchange_identification: read: Connection reset by peer` / `Connection reset by 198.19.0.1 port 3128` | 255 |
| 2 | 03:45:23 (~59 s after #1) | same | FAIL — no hostname returned | same as #1 | 255 |
| 3 | 03:46:28 (~65 s after #2) | same | FAIL — no hostname returned | same as #1 | 255 |

**Counts:** attempts 3 / successes 0 / failures 3. All 3 failures are identical: connection reset by the relay at `198.19.0.1:3128`, during `kex_exchange_identification` — i.e. **before SSH authentication**. No auth rejection, no host-key error, and no response from the PC itself was received in any attempt.

**Interpretation (PC-route alert clarity rule, Nov 11, 2026 — binding):**
- This is evidence about **my connection / relay path to the PC only**. It is NOT evidence that the PC is down, off, or misbehaving; the PC's own state is **unknowable** while these probes fail.
- The failure signature matches run 330's rig lane record (~31 failed probes 02:42–03:39 CST, relay reset `198.19.0.1:3128`, before auth) — the same relay, the same pre-auth reset.
- **Last successful probe on record: 02:43:48 CST, Nov 14, 2026** (per task context; other lanes succeeded at 02:42–02:43 CST while run 330's lane was failing — lane-specific relay behaviour, further reason not to generalise to the PC).
- No fallback was used: the task scopes this rig to the PC test-server mirror, which has no panel fallback (the mirror exists only on the PC). No live-server contact was made as a substitute.

**Gate decision:** 3-attempt pattern exhausted → **STOP**, per task instruction. Mirror steps below are NOT RUN, not failed.

---

## 3. Hash pairs — desk ↔ mirror

| Pair | Desk (size / SHA-256) | Mirror placed (size / SHA-256) | Verdict |
|---|---|---|---|
| ExcelExpansions-0.1.1.jar | 61,736 B / `b9d26233fbef56520b4d9bd8d200093d85fa103e7ae51d17b50b729f9749ba91` | NOT PLACED — PC route gated (§2); no transfer attempted | ⏸ NOT RUN (gated) |

No partial transfer exists to clean up: the first transfer command was never issued.

---

## 4. Boot

| Check | Expected | Actual | Verdict |
|---|---|---|---|
| Boot clean on mirror | `Done (...)` line in boot log | No boot attempted — server never contacted | ⏸ NOT RUN (gated) |
| SEVERE attributable to ExcelExpansions | 0 | No boot log exists for this run | ⏸ NOT RUN (gated) |
| ERROR total | reported with benign-line attribution (cf. 0.1.0 rig: 1 = Essentials unsupported-version line) | No boot log exists for this run | ⏸ NOT RUN (gated) |

Prior-rig baseline for the re-run (from `~/workspace/expansion-rig-20261114/RIG-REPORT.md`, 0.1.0 rig, Nov 14 ~02:09–02:31 CST): boots via WMI `Win32_Process.Create` of `start-testserver.cmd`, shutdowns via mirror RCON `stop` (port 25511), `Done (~21s)` per boot, SEVERE 0, ERROR 1 benign. That method remains the re-run method; it was not re-executed here.

---

## 5. Release-path proof steps — per-step verdicts

All steps are on fixture state on the mirror only. None were executed; each is listed with its planned proof so the re-run is a checklist, not a redesign.

| # | Step | Planned proof (from build record / host source) | Verdict |
|---|---|---|---|
| 5.1 | Watcher tick → proposal generated | Boot with candidate; first proposer tick ~1 min after enable (host schedules `proposerTick` at 1200 ticks ≈ 60 s, then cadence-hours); fixture level store seeded so a fixed-band row is DUE (cf. 0.1.0 rig B-2: M-02 Primis DUE, 6 of 7 active in band, on pre-wipe signals); log line `PROPOSAL RELEASE M-02 … \| tap: /excelexpansions tap M-02 <token>` + `release-state.txt` entry `proposed` with the same 10-char token | ⏸ NOT RUN (gated) |
| 5.2 | Mike-only tap card appears; non-Mike UUID cannot release | Card is delivered only to Mike-UUID / op senders (`deliverCard` iterates online players, `isMike` only); a tap from a non-listed, non-op player returns `Only Mike can tap a release.`; a wrong-token tap is rejected from anyone, console included (stale-card guard §7.12). **Client-less limit (standing, from 0.1.0 rig):** no Minecraft client can join the headless mirror and bot-player testing is declined by Mike — the in-game card rendering and a real non-Mike player tap are player-proof class; the mirror-provable leg is console/RCON: tokenless console tap honoured, wrong-token console tap rejected, plus the host auth code path as shipped. That split must be stated in the re-run report, not smoothed over. | ⏸ NOT RUN (gated) |
| 5.3 | Tap → instant flip of a parked fixture pack (enable + open, .bak discipline) | Fixture: a parked content file with anchored column-0 `isEnabled: false` (0.1.0 rig used M-03 `catacombs_lair.yml`); tap with the live token → anchored flag → `true`, sibling backup `<file>.bak-excelexpansions` holding the original, decoy `# isEnabled:` comment untouched, temp-file + atomic move; state advances to `released` with `Mike tap <date>` audit line; double-tap returns `ALREADY_RELEASED` and flips nothing. "Open" beyond the enable flag is the row's release work per the FINAL manifest, not part of the flip (0.1.0 rig B-4 note). | ⏸ NOT RUN (gated) |
| 5.4 | Untapped proposal stays due silently (no spam on re-tick) | Force/await a second tick with the proposal untapped: no second `PROPOSAL` log line / card for the same episode, `proposed_at` unchanged in `release-state.txt`; re-surfacing only via `rundown` / Mike-join redelivery of the SAME token | ⏸ NOT RUN (gated) |
| 5.5 | Seasonal window behaviour — November | In November: M-30 north-pole (December) NOT_DUE, M-31 fireworks (New Year turn = Dec 31 + Jan 1 only) NOT_DUE, M-32 the-hallowed-haunt (October) NOT_DUE, M-33 harvester UNASSIGNED_NEVER (no content files — can never flip). In-window control DUE: seasonal evaluation with month=December for M-30 → SEASONAL_DUE (harness-level control, as in 0.1.0 rig B-5, unless the re-run sets the mirror clock — it should not) | ⏸ NOT RUN (gated) |
| 5.6 | <3-active floor holds | Fixture roster cut to 2 active: verdict NOT_DUE — `ACTIVE floor of 3 not met: 2`; no proposal, row untouched (config `watcher.active-floor: 3`, `active-window-hours: 336`) | ⏸ NOT RUN (gated) |
| 5.7 | Rundown command lists the due proposal | RCON/console `excelexpansions rundown` output includes the proposed row with state + `due since` + note; `excelexpansions status` counts proposed ≥ 1; `list` shows 57 rows | ⏸ NOT RUN (gated) |
| 5.8 | Level store reads its own seeded store (post-wipe zeros baseline) | Mirror EM DB is the post-wipe state (0.1.0 rig Part C restored the mirror's own DB after its wipe rehearsal — re-verify on re-run, do not assume); on boot, refresh reads `EliteMobs/data/player_data.db` and the store seeds/retains post-wipe zeros (HLK 0 / Score 0) — `excelexpansions store` shows the store size + refresh note `EM PlayerData read OK: N rows applied…`; no pre-wipe signal (HLK 22/23 etc.) may appear as a current signal except where a fixture deliberately seeds it for §5.1, and any such fixture seeding must be labelled fixture, not baseline | ⏸ NOT RUN (gated) |

**Step counts:** planned 8 (5.1–5.8) / PASS 0 / FAIL 0 / NOT RUN 8 — all NOT RUN for the single gated reason in §2. No step was partially executed.

---

## 6. §7.8 live-apply question — note with evidence

**Question (DESIGN §7.8, carried in BUILD-RECORD-011.md decision D1):** does EliteMobs apply a flipped `isEnabled` flag live (on EM reload / immediately), or only at next restart?

**Evidence on record:**
- Build record D1: the releaser is built against the rig-proven anchored-flag flip (W7 / 0.1.0 rig Part B-4 manual proof: `catacombs_lair.yml` false→true, **next boot** clean with the pack back in the enabled set). Nothing on record proves live apply.
- Shipped behaviour is honest about this: the tap announcement does **not** promise instant in-world access, and `config.yml`'s release note states the reload-vs-restart question is rig-owed, not assumed.
- This run adds **no new evidence**: the mirror was never reached, so neither a plugin-driven flip nor a post-flip boot / EM-reload observation exists for 0.1.1.

**Status: §7.8 remains OPEN / NOT SETTLED by this rig.** The re-run must settle it explicitly: after the §5.3 plugin-driven flip, (a) issue an EM reload if one exists and check whether the pack is live without a restart, then (b) boot once more and confirm the flipped flag survives with a clean boot (Done, SEVERE 0 attributable) — and report (a) and (b) as separate lines.

A second carried question the re-run inherits (config, not §7.8): `watcher.dynamic-floor` ships unset — dynamic rows (e.g. M-03) stay NOT_EVALUABLE until a floor is set; no floor was or may be invented by the rig.

---

## 7. Mirror state left behind

- **Unchanged by this run.** No file was transferred to, modified on, or deleted from the PC or the mirror; no boot was started; no process was left running by this lane. The mirror remains exactly as the 0.1.0 rig left it (per that report: 0.1.0 jars placed, server stopped, java count 0 at that time — not re-verified this run because the route is gated; stated as last-known, not current).
- Desk side: candidate jar and build folder untouched (read-only access only). Barred jar untouched at `work/pkg011-A.jar` / `work/pkg011-B.jar`.
- No MANIFEST, backlog, state, live-server, or panel contact of any kind.

---

## 8. Re-run trigger

Re-dispatch this rig when the PC route next probes clean (`ssh mike-pc hostname` returns `DESKTOP-IL05DQR`). The desk verification in §1 does not need repeating unless the candidate file's size/mtime changes (VERIFIED STAMPS rule); the re-run starts at §2 probe, then §3 placement with a fresh desk↔mirror hash pair, then §§4–5 in order, closing §6's §7.8 question with the two-line (reload / reboot) evidence.

**Fallback notification (for the supervisor's chat line):** ExcelExpansions 0.1.1 mirror rig — PC route gated after 3 probes (relay reset 198.19.0.1:3128, pre-auth); no fallback exists for the PC-only mirror, work did not continue on another path, item stopped at the gate. PC's own state unknowable from these probes; last successful probe on record 02:43:48 CST Nov 14, 2026.

---

# RUN 334 ADDENDUM — ExcelExpansions 0.1.1 MIRROR RIG RE-ATTEMPT (GATED)

**Date:** Sat Nov 14, 2026, 04:33:57–04:35:58 CST (America/Chicago) — candidate check + PC-route probe window
**Worker:** board supervisor run 334 rig lane (front-of-line, expansion programme, Mike's Nov 14 priority law)
**Scope:** PC test-server mirror ONLY — `C:\Users\MikeExcel\wrv-testserver\server\` on DESKTOP-IL05DQR via the PC route. NEVER the live server. No MANIFEST edits, no backlog edits, no installs to live. None were made. `WINDOW-IN-PROGRESS.txt` was ABSENT at run start (checked 04:33 CST) — no panel window was opened by this lane regardless.
**Outcome:** **GATED ON THE PC ROUTE** — mirror placement, boot, and all release-path steps are NOT RUN. This is a gated report, per task instruction; it is not a rig pass or a rig failure of the plugin. This is the fourth consecutive gated attempt (runs 330 W4, 331 W2, 332 W1, now 334), all with the identical relay signature below.

---

## R334-1. Candidate of record — size + mtime check (PASS, F2 verified-stamp rule)

Desk verification per task: size + mtime check only unless changed. A full re-hash was also taken this run as corroboration (read-only, does not change the verdict basis).

| Item | Expected (run 331 record / SHA256SUMS-011.txt) | Actual (desk, this run, 04:33 CST) | Verdict |
|---|---|---|---|
| File | `~/workspace/excelexpansions-011-build-20261114/ExcelExpansions-0.1.1.jar` | present | — |
| Size | 61,736 B | 61,736 B (`stat`: size=61736) | MATCH — unchanged |
| mtime | 2026-11-14 08:54:22 UTC (run 331: 2026-11-14 08:54:22.687517129 +0000) | 2026-11-14 08:54:22.687517129 +0000 | MATCH — unchanged |
| Full SHA-256 | `b9d26233fbef56520b4d9bd8d200093d85fa103e7ae51d17b50b729f9749ba91` | `b9d26233fbef56520b4d9bd8d200093d85fa103e7ae51d17b50b729f9749ba91` (`sha256sum`, this run) | MATCH — full hash, not prefix-only |
| Barred jar | 203464a6… at `work/pkg011-A.jar` — BARRED, never place or run | `work/pkg011-A.jar` = 61,081 B, mtime 2026-11-14 08:45:49 UTC; not copied, placed, or used in any step of this run | BARRED JAR NOT USED |
| Prior report | `RIG-REPORT.md` 14,217 B / `bb9924f7…` before this addendum | 14,217 B / `bb9924f77e99b44fa51b7beca244628eb126dfc2adfc84ff8a67ce53e18f5dbb` (pre-append, this run) | MATCH — addendum appends to the record of runs 331/332 |

**Check counts:** size check 1/1 MATCH; mtime check 1/1 MATCH; changed? NO. Probe phase was authorised to proceed.

---

## R334-2. PC route — probe attempts (GATED, 3/3 FAIL)

Retry pattern per task: up to 3 attempts over ~2 minutes, `timeout 30 ssh -o ConnectTimeout=20 mike-pc hostname` (alias: HostName 100.72.156.1, User mikeexcel, ProxyCommand via `~/workspace/pc-access/ts-connect.py`). No further attempts were made after attempt 3 — no hammering, no repeat of run 330's 31-probe pattern.

| # | Time (CST, Nov 14, 2026) | Command | Result | stderr (verbatim) | Exit |
|---|---|---|---|---|---|
| 1 | 04:33:57 | `timeout 30 ssh -o ConnectTimeout=20 mike-pc hostname` | FAIL — no hostname returned | `kex_exchange_identification: read: Connection reset by peer` / `Connection reset by 198.19.0.1 port 3128` | 255 |
| 2 | 04:34:56 (~59 s after #1) | same | FAIL — no hostname returned | same as #1 | 255 |
| 3 | 04:35:58 (~62 s after #2) | same | FAIL — no hostname returned | same as #1 | 255 |

**Counts:** attempts 3 / successes 0 / failures 3. Probe window span: 04:33:57–04:35:58 CST (2 min 01 s). All 3 failures are identical: connection reset by the relay at `198.19.0.1:3128`, during `kex_exchange_identification` — i.e. **before SSH authentication**. No auth rejection, no host-key error, and no response from the PC itself was received in any attempt. The signature is identical to runs 331 (03:44–03:46 CST) and 332 (03:53–03:55 CST).

**Notable discrepancy (recorded, not smoothed over):** per the run 334 task context, the install failsafe used this same PC lane successfully at 04:03–04:28 CST today (probe PASS on attempt 1, hostname DESKTOP-IL05DQR) — minutes before this lane's window. This lane's 3 probes at 04:33–04:35 CST all failed pre-auth at the relay. That pattern (one lane succeeding while another fails, as also seen at 02:42–02:43 CST during run 330) is consistent with lane/session-specific relay behaviour, and is a further reason the failures below must not be generalised to the PC.

**Interpretation (PC-route alert clarity rule, Nov 11, 2026 — binding):**
- This is evidence about **my connection / relay path to the PC only**. It is NOT evidence that the PC is down, off, or misbehaving; the PC's own state is **unknowable** while these probes fail.
- **Last successful PC probe on record: 04:03–04:28 CST window, Nov 14, 2026** (install failsafe lane, probe PASS attempt 1, DESKTOP-IL05DQR — per run 334 task context; supersedes the 02:43:48 CST record carried in runs 331/332).
- No fallback was used: the task scopes this rig to the PC test-server mirror, which has no panel fallback (the mirror exists only on the PC). No live-server or panel contact was made as a substitute.

**Gate decision:** 3-attempt pattern exhausted → **STOP**, per task instruction. Mirror steps below are NOT RUN, not failed.

---

## R334-3. Rig steps — NOT RUN count

Fixture-expectation note for the re-run (carried, not executed): the live EM wipe landed today at 04:28 CST, so the post-wipe zeros baseline in §5.8 of the run 331 report is now the live-era baseline; on re-run, re-verify the mirror's own EM DB state fresh (the 0.1.0 rig restored the mirror's own DB after its rehearsal) and label any deliberately seeded pre-wipe signal (e.g. for the §5.1 due-detection fixture) as fixture, not baseline.

| Group | Planned | PASS | FAIL | NOT RUN |
|---|---|---|---|---|
| Desk candidate check (§R334-1) | 1 | 1 | 0 | 0 |
| PC-route probes (§R334-2) | 3 attempts | 0 | 3 (all gated, same relay signature) | 0 |
| Mirror placement + desk↔mirror hash pair (61,736 B / b9d26233…) | 1 | 0 | 0 | 1 (gated) |
| Mirror boot (Done line / SEVERE count / ERROR count) | 1 | 0 | 0 | 1 (gated) |
| Release-path proof steps 5.1–5.8 (per §5 above: tick→proposal; Mike-only tap card; tap→flip with .bak discipline; untapped stays silent; seasonal November behaviour; <3-active floor; rundown lists proposal; level store post-wipe zeros) | 8 | 0 | 0 | 8 (gated) |
| **Rig total (placement + boot + release-path)** | **10** | **0** | **0** | **10** |

**Boot lines:** NOT RUN — no boot attempted, no boot log exists for this run; no Done line, no SEVERE count, and no ERROR count is claimed. (Prior-rig baseline for the re-run stands as recorded in §4 above: WMI `Win32_Process.Create` of `start-testserver.cmd`, RCON `stop` on port 25511, `Done (~21s)`, SEVERE 0, ERROR 1 benign.)

No partial transfer exists to clean up: the first transfer command was never issued. No PC temp files were created by this lane, so there is nothing to clean up on the PC; the debug Chrome and the mirror were never contacted and remain as found.

---

## R334-4. §7.8 live-apply question

**Status: OPEN / NOT SETTLED by this rig** (unchanged from runs 331/332). The mirror was never reached, so neither a plugin-driven flip nor a post-flip EM-reload / reboot observation exists for 0.1.1. The re-run must still settle it with two separate evidence lines after the tap→flip step: (a) EM reload observation without a restart, then (b) one more boot confirming the flipped flag survives with a clean boot (Done, SEVERE 0 attributable).

---

## R334-5. Mirror / desk state left behind

- **Unchanged by this run.** No file was transferred to, modified on, or deleted from the PC or the mirror; no boot was started; no process was left running by this lane.
- Desk side: candidate jar read by `stat` + `sha256sum` only; contents untouched. Barred jar untouched.
- No MANIFEST, backlog, state, live-server, or panel contact of any kind.

---

## R334-6. Re-run trigger

Re-dispatch this rig when the PC route next probes clean from the dispatching lane (`ssh mike-pc hostname` returns `DESKTOP-IL05DQR`). Note the lane-specific pattern in §R334-2: a failsafe-lane success does not prove this lane's relay path is clear — the re-run must still open with its own 3-attempt probe pattern. The candidate size + mtime check in §R334-1 does not need repeating unless the candidate file's size/mtime changes (VERIFIED STAMPS rule); the re-run then proceeds: placement with a fresh desk↔mirror hash pair, boot, release-path steps 5.1–5.8 in order, closing §7.8 with the two-line (reload / reboot) evidence.

**Fallback notification (for the supervisor's chat line):** ExcelExpansions 0.1.1 mirror rig (run 334) — PC route gated after 3 probes (relay reset 198.19.0.1:3128, pre-auth, 04:33:57–04:35:58 CST); no fallback exists for the PC-only mirror, work did not continue on another path, item stopped at the gate. This is relay-path evidence only; the PC's own state is unknowable from these probes. Last successful probe on record: failsafe lane, 04:03–04:28 CST Nov 14, 2026 (DESKTOP-IL05DQR).

---

# RUN 335 — ExcelExpansions 0.1.1 MIRROR RIG (FULL RUN — PASS)

**Date:** Sat Nov 14, 2026, 05:00–05:18 CST (America/Chicago) — probe, staging, 3 mirror boots, harness, restore
**Worker:** board supervisor run 335 rig lane (front-of-line, expansion programme, Mike's Nov 14 priority law)
**Scope:** PC test-server mirror ONLY — `C:\Users\MikeExcel\wrv-testserver\server\` on DESKTOP-IL05DQR via the PC route. NEVER the live server. No MANIFEST edits, no installs to live. None were made. `WINDOW-IN-PROGRESS.txt` was ABSENT at run start — no panel window was opened by this lane.
**Outcome:** **RIG PASS** — the full plugin-driven path is proven on the mirror: watcher tick → Mike-only proposal (token) → tap → instant anchored-flag flip of a parked pack, with the §7.8 live-apply question SETTLED (reload does not apply; restart does — see R335-6). Candidate of record only (61,736 B / b9d26233…); barred jar 203464a6… was never copied, placed, or used.

---

## R335-1. PC route — probe attempts + the invocation root cause (RESOLVED)

Per task: use the G1 lanes' EXACT invocation from run 334 (`ssh -F ~/.ssh/config`), up to 3 attempts over ~2 minutes.

| # | Time (CST, Nov 14, 2026) | Invocation | Result | Exit |
|---|---|---|---|---|
| 1 | 05:00:27 | `timeout 30 ssh -F ~/.ssh/config -o ConnectTimeout=20 mike-pc "hostname"` | `DESKTOP-IL05DQR` returned | 0 — **PASS on attempt 1** |

No further attempts were needed. **Root cause of the runs 330/331/332/334 gating, now established:** those rig lanes probed with `ssh -o ConnectTimeout=20 mike-pc hostname` — WITHOUT `-F`. The run-334 G1 re-dispatch appendix (in `~/workspace/g1-pulls-bm334/RECEIPTS.md`) had already isolated it: in this exec environment, ssh without `-F` does not load `~/.ssh/config`, resolves a wrong address, and the failure surfaces as the relay reset at `198.19.0.1:3128` / a direct-connect intercept — a LOCAL INVOCATION ERROR, not (only) relay flapping. With `-F ~/.ssh/config` (alias → HostName 100.72.156.1, ProxyCommand via `~/workspace/pc-access/ts-connect.py`), this run executed 20+ ssh/scp operations between 05:00–05:18 CST, all first-try except ONE idle-drop: an SSH session carrying a 110 s server-side sleep was dropped at ~62 s (`client_loop: send disconnect: Broken pipe`, 05:10 CST window); the check was re-run immediately and returned clean — transport behaviour, no rig impact. **Last successful probe: this run, 05:00:27 CST (and continuously thereafter).** No fallback was used or needed; the mirror has no panel fallback in any case.

## R335-2. Candidate of record — size + mtime check (PASS, F2 verified-stamp rule)

| Item | Expected (runs 331/334 record) | Actual (desk, this run, 05:00 CST) | Verdict |
|---|---|---|---|
| File | `~/workspace/excelexpansions-011-build-20261114/ExcelExpansions-0.1.1.jar` | present | — |
| Size | 61,736 B | 61,736 B | MATCH — unchanged |
| mtime | 2026-11-14 08:54:22.687517129 +0000 | 2026-11-14 08:54:22.687517129 +0000 | MATCH — unchanged |
| Full SHA-256 | `b9d26233fbef56520b4d9bd8d200093d85fa103e7ae51d17b50b729f9749ba91` | same (re-hashed this run as corroboration) | MATCH |
| Barred jar | 203464a6… at `work/pkg011-A.jar` — BARRED | untouched desk-side; never copied to the PC in any step | BARRED JAR NOT USED |

Desk verification from runs 331/334 was NOT repeated beyond this check (per F2). Mirror placement hash pair (staging, 05:04 CST): staged jar copied via `scp -F ~/.ssh/config` → placed `plugins\ExcelExpansions-0.1.1.jar` = **61,736 B / `b9d26233fbef56520b4d9bd8d200093d85fa103e7ae51d17b50b729f9749ba91`** — EXACT desk↔mirror match. The 0.1.0 jar was retired for the rig to `ExcelExpansions-0.1.0.jar.bak-r335` (non-loadable) and restored after (R335-8).

**Rig fixture (named, all restored after):** (a) mirror config = the SHIPPED 0.1.1 `config.yml` (Mike's Java + Bedrock/Floodgate UUIDs populated, `allow-op: true`) with ONE rig change: `level-store.refresh-minutes: 15 → 1` (placed: 2,990 B / `6d30df47935981798c4740a3844563959a6aa59bb05af52e95ba76e8c8c2071d`); (b) the 4 M-02 Primis content files were PARKED as the flip fixture — anchored first-line `isEnabled: true → false`, one `isEnabled` occurrence per file (no decoy comment exists in these files), pristine copies backed up first (parked hashes: meta `e89a64ec…`, adventure `d750ef19…`, blood-temple `e370e6d6…`, gladius `ada9c540…`; +1 B each, `false` vs `true`); (c) no `release-state.txt` existed before Boot 1 (fresh overlay, as the 0.1.0 rig left it); the mirror's existing 12-player level store was used as found.

## R335-3. Boots (all on the mirror; WMI `Win32_Process.Create` of `start-testserver.cmd`; stops via mirror RCON `stop`, port 25511)

| Boot | Purpose | Preserved log (on PC, `wrv-testserver\`; copies also in this folder's `work335\`) | Size / SHA-256 | Paper Done line | ERROR | SEVERE | Shutdown |
|---|---|---|---|---|---|---|---|
| 1 | Tick → proposal (§5.1, §5.7, §5.8) | `boot-expansions011-r335-boot1-20261114.log` | 90,129 B / `36cbd80847899b674caca114db0ee2d1efd94d9d40a1c487912be386fd0f1291` | `Done (21.286s)!` 05:05:25 | 1 | 0 | RCON stop → `SERVER-EXIT 0`, java 0 |
| 2 | Untapped re-tick silence (§5.4) → tap sequence (§5.2/§5.3) → EM reload (§7.8a) | `boot-expansions011-r335-boot2-20261114.log` | 95,616 B / `e270f5f419678c5bf6bd0512c7e65f18c7c481e0ae2aa9c9ed25658bcf9c3716` | `Done (21.237s)!` 05:09:06 | 1 | 0 | RCON stop → `SERVER-EXIT 0`, java 0 |
| 3 | Reboot leg (§7.8b): flip persistence + released-state persistence | `boot-expansions011-r335-boot3-20261114.log` | 89,329 B / `b3beda937a679b331a0847778946f3f150e9ad7a42f44c1b2f87c6513692301b` | `Done (20.978s)!` 05:14:02 | 1 | 0 | RCON stop → `SERVER-EXIT 0`, java 0 |

The single ERROR in every boot is the benign `[Essentials] You are running an unsupported server version!` line of record. SEVERE attributable to ExcelExpansions: **0** in all boots. ExcelExpansions enable line (all boots): `ExcelExpansions 0.1.1: 57 manifest rows loaded; IN-BAND source: ExcelExpansions level store (band signal: EliteMobs PlayerData.HighestLevelKilled; canonical-source decision rig-owed, #16); level store: 12 players; proposer cadence: 6h; dynamic floor: unset (TBD at build)` — Boot 1 `release state: 0 rows off initial`, Boots 2–3 `1 rows off initial` (the M-02 episode persisting).

## R335-4. Release-path proof steps — per-step verdicts

| # | Step | Evidence (structured) | Verdict |
|---|---|---|---|
| 5.1 | Watcher tick → proposal generated | Boot 1 log 05:06:25 CST (~64 s after the 05:05:21 enable — the shipped 1,200-tick first tick): `[ExcelExpansions] PROPOSAL RELEASE M-02 Primis - 7 of 7 active in band \| tap: /excelexpansions tap M-02 f378e1a854` + `proposal M-02 posted; Mike not online - the same card delivers on his join (no re-post, DESIGN 3.3)`. `release-state.txt`: `M-02\|proposed\|1794654385267\|1794654385267\|\|\|f378e1a854\|proposed: 7 of 7 active in band` (dueSince = proposedAt = the tick instant; token in state = token in log). Harness (R335-5) independently produced exactly 1 proposal (M-02) from the same store. | ✅ PASS |
| 5.2 | Mike-only tap card; non-Mike cannot release | Console/RCON legs (the mirror-provable layer): wrong-token tap → `tap token does not match the live proposal for M-02 - stale card rejected, nothing flipped`; tap on never-proposed M-03 → `no live proposal for M-03 (state: unreleased) - tap rejected`; state, flags, and backup count (0) verified UNCHANGED after both rejections; correct-token console tap honoured (see 5.3); double-tap → already-released no-op. Card audience is Mike-only in the shipped host (`deliverCard` iterates online players, `isMike` only; config carries Mike's two UUIDs + `allow-op: true`). **Client-less split (stated, not smoothed):** the in-game card RENDERING, a real non-Mike PLAYER tap, and Mike-join card re-delivery cannot be exercised on the headless mirror (no client can join; bot-player testing is declined by Mike) — those three sub-legs are PLAYER-PROOF class, NOT RUN here. | ✅ PASS (all mirror-provable legs) — 3 sub-legs player-proof / NOT RUN |
| 5.3 | Tap → instant flip of parked fixture pack (.bak discipline) | RCON `excelexpansions tap M-02 f378e1a854` → `Primis released by Mike tap 2026-11-14 (console)` + 4 per-file lines `FLIPPED (isEnabled -> true)`; log 05:10:45 `TAP RELEASED M-02 Primis by Rcon`. On disk after: all 4 LIVE files `isEnabled: true` and BYTE-IDENTICAL TO PRISTINE (meta 567 B / `4162e2f1…`, adventure 708 B / `cc1141bd…`, blood-temple 866 B / `3e08b9a9…`, gladius 986 B / `f4813332…` — the exact pre-rig hashes); all 4 sibling `<file>.bak-excelexpansions` hold the PARKED bytes (hashes = the parked hashes of R335-2). State: `M-02\|released\|…\|releasedAt 1794654645448\|Mike tap 2026-11-14 (console)\|token cleared\|surfaces-owed note`. Double-tap: `M-02 Primis is already released - double-tap ignored, nothing re-flipped`. Status after: `0 proposed, 0 due, 2 released`. | ✅ PASS |
| 5.4 | Untapped proposal stays due silently on re-tick | Boot 2 (fresh boot, episode persisted untapped): proposer tick fired on the same 1,200-tick schedule; Boot 2 log PROPOSAL-line count = **0**; `release-state.txt` after the tick BYTE-IDENTICAL episode (dueSince/proposedAt 1794654385267, token `f378e1a854` unchanged); flags still parked, 0 backups. Harness corroboration: second `Proposer.tick` on the same state returned 0 proposals. | ✅ PASS |
| 5.5 | Seasonal window behaviour — November | Harness vs the placed jar's own classes (live month = November): M-30 north-pole NOT_DUE (outside season window), M-31 fireworks NOT_DUE, M-32 the-hallowed-haunt NOT_DUE, M-33 harvester UNASSIGNED_NEVER (no content captured — can never flip). In-window control: M-30 evaluated with month=December → SEASONAL_DUE. | ✅ PASS |
| 5.6 | <3-active floor holds | Harness: M-02 evaluated against a 2-active subset → NOT_DUE `ACTIVE floor of 3 not met: 2`, no proposal. (Live boots ran with 7 active ≥ floor 3.) Dynamic companion check: M-03 with phase order unblocked → NOT_EVALUABLE `dynamic row: watcher floor TBD at build (config watcher.dynamic-floor unset) - no floor invented`; with a harness-supplied floor of 10 it evaluates normally (DUE, 4 of 7) — the shipped config correctly leaves the floor unset. | ✅ PASS |
| 5.7 | Rundown lists the due proposal | Boot 1 RCON `excelexpansions rundown` → `M-01 Adventurers Guild Hub - released` + `M-02 Primis - proposed (due since 2026-11-14T11:06:25.267618100Z) - proposed: 7 of 7 active in band`. `status` → `1 proposed, 0 due, 1 released`, 57 rows; `list` shows all 57 rows with M-02 `proposed`. | ✅ PASS |
| 5.8 | Level store reads its own store (named EM signals) | Boot 1 RCON `store`: `Last refresh: EM PlayerData read OK: 12 rows applied from plugins\EliteMobs\data\player_data.db`; store size 12. Read-only sqlite cross-check of the mirror DB returned, to the digit, the values the refresh applied (Jester HLK 22 / Score 3644; Marvin 23 / 828; Kailey 16 / −988; Mike-Bedrock 12 / 86; Mike-Java 0 / 0; 12 rows total) — the band signal is the NAMED EM field `HighestLevelKilled` (+ `Score`), never an invented or XP-converted number. **Baseline clarification (per the run-334 note):** the mirror's EM DB is the mirror's OWN restored DB (28,672 B / `7841b19e…`, unchanged all run — the rig never wrote it), carrying real pre-wipe-era values; it is NOT the live server's post-wipe DB. The post-wipe-zeros seeding behaviour was proven on the 0.1.0 rig (Part B-1: refresh overwrote fixture signals with the wiped DB's zeros) and the live wipe itself landed on the live server 04:28 CST today — the mirror leg owed here was the read/apply path, and it is exact. | ✅ PASS |

**Step counts:** release-path steps planned 8 (5.1–5.8) / **PASS 8** (5.2 with 3 named player-proof sub-legs NOT RUN, client-less class) / FAIL 0 / NOT RUN 0 at every mirror-provable layer. Placement + hash pair 1/1 PASS; boots 3/3 clean (Done ×3, SEVERE 0 ×3, ERROR 1 benign ×3, SERVER-EXIT 0 ×3); harness checks (rows/active/DUE/floor/dynamic ×2/seasonal ×5/tick ×2/tap ×3) 15/15 as expected. **Tap-to-flip verdict: PASS — the complete tick → proposal → Mike-token tap → flip path is plugin-proven end to end on the mirror.**

## R335-5. Harness (engine-level corroboration, on the PC against the placed 0.1.1 jar's own classes)

`Rig335.java` / `Rig335b.java` (kept in this folder's `work335\`), compiled with the mirror JDK 25 against `plugins\ExcelExpansions-0.1.1.jar`, run over a copy of the mirror's live level store: `ROWS 57`, `STORE-SIZE 12`, `ACTIVE-COUNT 7`, `DUE M-02 … 7 of 7 active in band`, `FLOOR 2-active … ACTIVE floor of 3 not met: 2`, seasonal lines as in §5.5, `TICK1 proposals=1 (M-02)`, `TICK2 proposals=0`, `TAP-WRONGTOKEN outcome=REJECTED_TOKEN`, `TAP-CORRECT outcome=RELEASED` with 4 × `FLIPPED`, fixture backups created, `TAP-DOUBLE outcome=ALREADY_RELEASED`. Every harness line agrees with the live-plugin evidence in R335-4; the harness is corroboration, not a substitute — the plugin/RCON legs above are the primary proof.

## R335-6. §7.8 live-apply question — SETTLED

**Question (DESIGN §7.8 / BUILD-RECORD D1):** does EliteMobs apply a flipped `isEnabled` flag live (on EM reload / immediately), or only at next restart?

**(a) Reload leg — NO live apply (Boot 2, after the 05:10:45 tap-flip):** RCON `em reload` answered `[EliteMobs] Plugin reloading...` and the log shows EM's FULL shutdown/re-initialisation in place (05:11:32 `Starting EliteMobs shutdown sequence...` → 05:11:34 `EliteMobs fully initialized!`, class-loot profiles, skill bonuses, database, and content-version parse all re-run). Despite that complete re-init, the reload window contains **zero** `Loading world` lines — EM does not (re)register content-package worlds on reload — and no `em_primis` load followed. (The probe `elitemobs reload` returned `Unknown command!`; `em reload` is the command.)

**(b) Reboot leg — flip persists; clean boot (Boot 3):** all 4 flipped flags survived the restart byte-identical to pristine, the release state persisted (`M-02 released`, 0 new proposals), boot clean (`Done (20.978s)`, ERROR 1 benign, SEVERE 0). EM-side world instantiation for M-02 was **not observable on this mirror in either state**: the mirror holds no Primis world content at all (only resource-pack map assets; the package is `registered-not-set-up` per the FINAL manifest — its world setup is the row's separate S-7 release work, and EM creates only `em_adventurers_guild` here). The anchored-flag enable mechanism at the EM layer for a SET-UP pack was already proven by the 0.1.0 rig (catacombs_lair flip → next boot clean with the pack back in the enabled set).

**Verdict: §7.8 is SETTLED — a tap flip is a RESTART-time input. EliteMobs reload does NOT apply it; the pack enables when the server next boots (plus, for world-bearing packs, their setup state must exist — release work, not flip mechanics).** The shipped behaviour is therefore correct as designed: the tap announcement does not promise instant in-world access. The `config.yml` releaser note ("rig-owed, not assumed") can now be updated to this settled answer by the build/supervisor lane — a wording change only, no code change owed.

## R335-7. Fallback / incident notes

- No fallback path was used; no live-server, panel, MANIFEST, backlog, or state contact of any kind.
- One SSH idle-drop during a long server-side sleep (R335-1) — re-run immediately, no impact.
- Two upload fumbles, both self-inflicted and recovered in-run: multi-source `scp` cannot rename per-file (fails with remote "No such file or directory") and, worse, a failed multi-source attempt left DIRECTORIES named `Rig335.java` / `r335-dbcheck.py` in `muse-temp` that shadowed the real files — removed and re-uploaded singly; harness then ran clean. Future lanes: one `scp` per file when renaming.

## R335-8. Mirror state left behind — RESTORED TO AS-FOUND (verified)

- ExcelExpansions 0.1.1 jar REMOVED; 0.1.0 jar restored in place: 32,877 B / `3a36001dde845164a5619f4e637ca3b85e8839a510d98a14475962fe3e7f0b55`.
- `plugins\ExcelExpansions\config.yml` restored: 628 B / `a4410d1bb07479d8127e526970c0f9232a27338a7647219b4c1f63276957c031` (the 0.1.0-rig fixture); `level-store.txt` restored: 1,538 B / `67f77340c1b4771a54cce8279245d5ec5833bd64d3ffaf2c6251f284590b8491`; rig `release-state.txt` DELETED (absent, as found); `EXCELEXPANSIONS-MANIFEST-FINAL.md` untouched.
- Primis files restored byte-exact from pristine backups (meta `4162e2f1…`, adventure `cc1141bd…`, blood-temple `3e08b9a9…`, gladius `f4813332…`, all `isEnabled: true`); all 4 `.bak-excelexpansions` flip backups REMOVED (0 remaining).
- Mirror EM DB never written by this rig and hash-stable all run: 28,672 B / `7841b19e619cab9a7cc8b0e283ff47bd9732bdfddf1af973e77f3ba23279ea91` before staging and after restore.
- Server left STOPPED, java count 0, last exit `SERVER-EXIT 0`. All three boot logs preserved on the PC (table in R335-3); copies + all rig scripts in this folder's `work335\`. This lane's PC `muse-temp` files (r335-* scripts, staged jar copy, harness sources/classes, flip fixture, backups dir) DELETED and verified 0 remaining (PC cleanup rule). Desk side: candidate jar and build folder untouched (read-only); barred jar untouched.

**Fallback notification (for the supervisor's chat line):** ExcelExpansions 0.1.1 mirror rig (run 335) — PC route PASS on attempt 1 with the G1 lanes' `ssh -F ~/.ssh/config` invocation (the runs 331/332/334 gating was the missing `-F`: a local invocation error, documented in R335-1); no fallback used. RIG PASS: tick → Mike-only proposal → tap → flip proven end to end; §7.8 settled (restart applies the flip, EM reload does not). Mirror restored byte-exact to as-found.
