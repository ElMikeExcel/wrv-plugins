# BUILD-RECORD — ExcelWeekendXP 1.2.1 (audit finding F-1 fix, decision D146)

- Worker: W7, board supervisor run 619, Thu Nov 19, 2026 ~22:24-22:32 CST (VM clock Nov 20 04:24-04:32 UTC).
- Scope (W5, `~/workspace/board-r619/W5.md` §4 + `~/workspace/plugin-audit-triage-20261119/TRIAGE.md` §1 F-1 / §3 option C, decision D146): WeekendXP 1.2.0's `VanillaXpBridge.java:69-71` reflectively requires `isVanillaXpBonusSupported` / `setVanillaXpEventMultiplier` / `getVanillaXpEventMultiplier` on `com.gmail.nossr50.mcMMO`, which the live patched mcMMO (WRV-XP-2) never exposes. Fix in WeekendXP 1.2.1: WeekendXP owns the vanilla multiplier via its own `PlayerExpChangeEvent` listener (1.2x Fri-Sun, 1.0x weekdays; keep the >=1.0 guard; the `mcmmo:xprate` leg untouched; mcMMO skill XP does not flow through that event, so no double-count). mcMMO was NOT rebuilt (option A rejected) and the leg was NOT retired (option B needs Mike).
- Claim (written first, before the candidate build): 1.2.1 deletes `VanillaXpBridge` and adds `VanillaXpMultiplier` (listener + multiplier state, guard >=1.0). Everything below was measured after the claim was written.
- Source: `src/` in this folder (1.2.1 tree). The 1.2.0 tree is untouched at `~/workspace/weekendxp-1.2.0/src/`; its top-level jar was rebuilt by the base gate and restored byte-identical afterwards (`b54b3c46…`, verified).
- NOT filed to the MANIFEST (supervisor files). NOT installed. No live placement by W7.
- Bedrock/Geyser parity: vanilla XP is Java-side XP mechanics — `PlayerExpChangeEvent` fires for server-side vanilla XP gains, and Bedrock players' XP gains arrive through Geyser as the same server-side events, so the listener covers both platforms with no Bedrock-specific code path. No Bedrock-specific consideration was found in the 1.2.0 source or the audit; no Bedrock-only behaviour is claimed. Player proof on live (Java + Bedrock) is owed after install.

## Base gate (1.2.0 rebuild) — PASS

- Command: `bash ~/workspace/weekendxp-1.2.0/build-and-test.sh` (Nov 19, 22:25 CST).
- Rebuild from current source: 19,260 B / SHA-256 `7f8bbf18…` — 8 B larger than the installed jar. Investigation: all 4 class files byte-identical; the ONLY delta is `plugin.yml` — the source tree (Nov 6) says `authors: [Iris, Mike]` while the Oct 17 jar embeds `author: Argus` (a post-build author-attribution edit, no code effect).
- Proof: repackaged the rebuilt classes with the jar-embedded (Oct 17) `plugin.yml` via `make-jar.py` → 19,252 B / SHA-256 `b54b3c465b5609f2b7a5df78e0498e1141d4aab6e0aed0bfd9371b0a6d609db8`, `cmp` clean against `~/workspace/weekendxp-1.2.0/build2/ExcelWeekendXP-1.2.0.jar`. Base gate PASS: code rebuild is byte-identical.
- 1.2.0 tests on rebuild: 31/31 pass (7 containers, 0 failed).

## Change summary (1.2.0 → 1.2.1)

- DELETED `src/com/winkreyes/excelweekendxp/VanillaXpBridge.java` (reflective mcMMO probe) and `tests/.../VanillaXpBridgeTest.java` (10 tests).
- ADDED `VanillaXpMultiplier.java`: `Listener` holding a volatile double multiplier (default 1.0); `applyMultiplier(double)` validates finite and >= 1.0 (throws `IllegalArgumentException` otherwise) and returns true; `@EventHandler(HIGHEST, ignoreCancelled)` on `PlayerExpChangeEvent` scales the amount by `Math.round(amount * multiplier)` only when the multiplier is > 1.0 and the amount is > 0, never below the original amount; at 1.0x the event is left exactly untouched.
- `ExcelWeekendXP.java`: drops the bridge, constructs `VanillaXpMultiplier`, registers it with `registerEvents`, passes `vanillaXp::applyMultiplier` to the unchanged `WeekendSchedule.apply`. Log lines unchanged (now truthful: the "verified vanilla XP event multiplier 1.2x/1.0x" line reflects state this plugin actually holds).
- `WeekendSchedule.java`, `LegacyDataMigration.java` and their tests: UNCHANGED. The `mcmmo:xprate` dispatch leg is untouched.
- `plugin.yml`: version 1.2.1; description notes the in-plugin multiplier and no mcMMO bridge API required.
- No-double-count evidence: `grep -rc PlayerExpChangeEvent` over `~/workspace/mcmmo-xpfix-build-20261115/repo/src/main/java/` returns zero files; skill XP flows through `McMMOPlayer.addXp(PrimarySkillType, float)` (line 1310) → `applyXpGain` (internal profile math). A skill-XP gain never passes through this listener.
- No remaining code references to the bridge API: `grep -rn VanillaXpBridge|isVanillaXpBonusSupported|setVanillaXpEventMultiplier|getVanillaXpEventMultiplier src/` returns only the explanatory comment in `VanillaXpMultiplier.java:12-13`.

## Candidate build (1.2.1) — deterministic double-build, byte-identical

- Build 1: 15,148 B / SHA-256 `b4725c555bbf218135d4c3b6eb40ddcdbecb05e46c48e2dec1e49cb1e1d791e6`
- Build 2: 15,148 B / SHA-256 `b4725c555bbf218135d4c3b6eb40ddcdbecb05e46c48e2dec1e49cb1e1d791e6`
- Byte-identical: YES (`cmp` clean). Script: `build-and-test.sh` in this folder (same toolchain as 1.2.0: JDK 25, `--release 21`, deterministic `make-jar.py`).
- Jar contents: `META-INF/MANIFEST.MF`, `ExcelWeekendXP.class`, `LegacyDataMigration.class`, `VanillaXpMultiplier.class`, `WeekendSchedule.class`, `WeekendSchedule$Result.class`, `plugin.yml`. No `VanillaXpBridge.class` remains (verified via `javap` on the jar: `VanillaXpMultiplier` implements `Listener` with `onPlayerExpChange(PlayerExpChangeEvent)`).
- Source zip: `ExcelWeekendXP-1.2.1-source.zip`, 33,236 B / SHA-256 `38cc2f3bd47a61120141f8b5cf4d9a09503ef0e2b2fa736b5f352cf56d406955` (fixed zip timestamps 2026-11-19 22:30 CST). See `SHA256SUMS` in this folder.

## Tests — 25/25 pass on BOTH builds (7 containers, 0 failed)

| Class | Tests | What it proves |
|---|---|---|
| `VanillaXpMultiplierTest` | 5 | 1.2x scales 10→12, 5→6, 3→4 (round); 1→1 never reduced; 0 stays 0; weekday 1.0x leaves 1/3/7/10/100 exactly untouched; 1.2→1.0 transition returns to no-op; guard rejects 0.0/0.99/-1.0/NaN/±Inf and leaves the live rate at 1.2; exactly one `@EventHandler`, taking `PlayerExpChangeEvent` (no-double-count shape) |
| `WeekendScheduleTest` | 11 | UNCHANGED from 1.2.0: Fri-Sun engagement, Chicago-midnight boundaries, DST, command-before-vanilla ordering, retry-on-failure, `mcmmo:xprate 1.2 true` / `mcmmo:xprate reset` dispatch |
| `ExcelWeekendXPTest` | 4 | Rewritten for 1.2.1: startup applies immediately with listener multiplier matching the dispatched command; 30-min timer + cancel-on-disable; F-1 regression — a plain mcMMO mock with NO bridge methods enables cleanly with zero bridge/greenbar/schedule-incomplete warnings and the verified-multiplier info line; failed dispatch retries without claiming success; throwing dispatch retries then applies |
| `LegacyDataMigrationTest` | 5 | UNCHANGED from 1.2.0 |

## Mirror-rig proof — boots clean, weekday 1.0x observed

- Rig: copy of `~/workspace/excelbuildmode-pilot/runtime-server/` (Paper 26.2-123, Java 25.0.4.1) at `rig/` in this folder; plugins: rig originals + live patched mcMMO `mcMMO-2.3.003-WRV-XP-2-ExcelShare-1.jar` (3,721,966 B / SHA-256 `404025cc63f5733fa135dd9303783dc9812248ce6ad9bb0d408643c8879c8501`, copied from `~/workspace/mcmmo-xpfix-build-20261115/mcMMO.jar`) + candidate `ExcelWeekendXP-1.2.1.jar`. Boot Nov 19, 22:28 CST; stopped cleanly via console `stop` 22:29 CST, exit 0. Full log: `rig/logs/latest.log` (SHA-256 `223e939757955e26efc9f27ba3412e538d27fc8b14a2c9bb258ca86d8c3cfc25`).
- Boot lines (verbatim, `rig/logs/latest.log:147-149`):
  - `[ExcelWeekendXP] Enabling ExcelWeekendXP v1.2.1`
  - `[ExcelWeekendXP] Weekday detected: dispatched mcMMO XP-rate reset command; verified vanilla XP event multiplier 1.0x.`
  - `[ExcelWeekendXP] ExcelWeekendXP scheduler enabled: America/Chicago, every 30 minutes.`
- `Done (36.756s)!` at log line 163. mcMMO enabled (log line 137, `Enabling mcMMO v2.3.003-SNAPSHOT`, treasures/potions loaded).
- SEVERE: 0. ERROR: 7 lines, NONE naming ExcelWeekendXP or mcMMO — 1x Yggdrasil key fetch (sandbox offline), 3x ExcelBuildMode storage (pre-existing rig config), 3x PaperVersionFetcher (sandbox offline).
- Zero occurrences of `schedule incomplete`, `greenbar`, `bridge`, or `VanillaXp` warnings in the rig log (grep, case-insensitive).
- What the rig did NOT prove (honest limits): today is Thursday Nov 19, so the rig exercised only the weekday 1.0x path — the weekend 1.2x engagement on a live server was NOT observed on the rig; it is proven at unit level (schedule Fri-Sun transitions + listener scaling) and is owed as player proof after install (see INSTALL-PACKET.md). The 30-minute re-check did not elapse during the ~1-minute rig run. The rig runs Paper 26.2; live runs Paper 26.3 — same API level for this plugin (api-version 1.21). No player was on the rig, so no live `PlayerExpChangeEvent` was fired there; scaling is proven by the listener unit tests.

## Files in this folder

- `BUILD-RECORD.md` (this file)
- `ExcelWeekendXP-1.2.1.jar` — candidate, 15,148 B / `b4725c55…`
- `ExcelWeekendXP-1.2.1-source.zip` — 33,236 B / `38cc2f3b…`
- `SHA256SUMS` — jar + source zip hashes
- `INSTALL-PACKET.md` — file-ready packet (supervisor files; W7 did not file)
- `RECEIPTS.md` — live-premise receipts + rig evidence index
- `build-and-test.sh` — adapted 1.2.1 build script
- `build/` — build intermediates (classes, test-classes, sources lists)
- `src/` — 1.2.1 source tree (main + tests + plugin.yml)
- `rig/` — mirror-rig copy used for the boot proof (Paper 26.2-123 + live mcMMO + candidate jar; `rig/logs/latest.log` is the boot evidence)
