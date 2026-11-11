# INSTALL PACKET — ExcelBuildMode 1.3.4 (combined rebuild: reviewed + console admin)

An Excel plugin by Argus for Wink Reyes Valley. Prepared Nov 10, 2026.
Status: BUILT + STAGED ONLY — do not install from this packet without
a proper install window under the standing gates (the parent/failsafe
installs; this desk packet installs nothing). Commissioned by
board-supervisor decision D5 (Nov 10, 2026).

## Why

The live server runs the October ExcelBuildMode 1.3.3 "reviewed"
build (live since the Oct 23–25, 2026 batch): a reviewed builder
(`excelbuildmode.reviewed`) may keep standing WorldEdit permissions
and still use Build Mode. Its source was lost. The Nov 10, 2026
console-admin build was ALSO numbered 1.3.3, was built from 1.3.2,
and contains no reviewed code — the failsafe's live-jar gate stopped
its install on Nov 10, 2026 because installing it would have
silently regressed the reviewed-builder flow.

1.3.4 reunites both deltas: the reviewed delta, recovered from the
preserved live jar and proven bytecode-faithful by a control build
(REVIEWED-DELTA-RECOVERY.md in the build folder), plus the console
admin surface (`/buildmode admin status <player>` and
`/buildmode admin off <player>`, console explicitly allowed, gated
by `excelbuildmode.admin`, default op; admin off forces the clean
Survival restore through the EXISTING recovery path). Full designs:
docs/PILOT-1.3.3.md (admin) and docs/PILOT-1.3.4.md (combined) in the
source zip.

## The artifact

- ExcelBuildMode-1.3.4.jar — 99,073 bytes
  SHA-256 39d86f3bc76ee7f28ff5bd428e1c8add9d07d5f9bedda5d1a84551c36f7464d3
  MD5 f175678bbae3eb6ae55416ea260c1ed0
  Staged at ~/workspace/buildmode-134-build/ExcelBuildMode-1.3.4.jar
  (build folder root; build record + SOURCE record alongside).
- **Live-jar gate (what must be live before the swap):**
  ExcelBuildMode-1.3.3.jar — the October REVIEWED build — 90,044
  bytes — SHA-256
  3662dcb73ca08ac1677e4257f64eae489ab9486e484ecc447ea2779cedc3ee1c.
  If the live jar does not hash to this, STOP — this packet's gate
  is the reviewed build, NOT the genuine 1.3.2 jar
  (26d356b4…, which sits on the server only as the non-loadable
  `ExcelBuildMode-1.3.2.jar.bak-20261023-batch`) and NOT the Nov 10
  console-admin 1.3.3 jar (fa640544…, which must never be installed —
  it lacks the reviewed code).
- Delta vs live (reviewed 1.3.3): the console admin surface only.
  The reviewed exemption is preserved instruction-for-instruction
  (control-build proven); BuildPolicy's probe set, the recovery
  machinery, big paste, and config.yml are unchanged.
- Verification: eight suites green, 11,685 assertions (82 admin +
  25 new reviewed-regression pins incl. both truth tables); recovery
  control-build zero-diff proof; clean-room double-build
  byte-identical; source zip rebuilds the same hash; rig boot
  (Paper 26.2-123 + WorldEdit 7.4.5): clean enable, 1.3.4 banner,
  zero SEVERE, 7/7 console checks.
- Rig limitation, stated plainly: no live reviewed entry/exit or
  in-session force-restore was run on the rig (needs a real eligible
  player session). The player proofs below ARE the end-to-end tests.

## The swap (one jar; standard gates)

Preconditions: nobody in Build Mode (all builders offline or in Play
Normally); `list` = 0 twice per the standing gate; guildgate LOCKED
0 of 5 pre-flight; marker discipline
(~/workspace/wrv-install-queue/WINDOW-IN-PROGRESS.txt at start,
deleted at finish, foreign-marker re-checks before Shut Down and
before Start); BACKUP FIRST with cap-at-6 (name prunees; never the
Daily Backup's newest copy or the two newest manual restore points).

1. Stop the server fully (panel Shut Down — console `stop`
   auto-restarts on this host).
2. Rename the live jar: plugins/ExcelBuildMode-1.3.3.jar →
   ExcelBuildMode-1.3.3.jar.bak-20261110-reviewed (keep alongside;
   only one ExcelBuildMode jar loadable at a time). This .bak IS the
   reviewed build — the only other copy is the preserved jar in
   ~/workspace/requal/menujoin143/preserved/; do not delete either.
3. Upload ExcelBuildMode-1.3.4.jar as plugins/ExcelBuildMode-1.3.4.jar
   (new filename — no rename-over dance needed). Panel has no checksum
   field: verify by exact size, 99,073 bytes, in the file listing.
4. NO config edit (the live config is preserved; 1.3.4 adds no config
   knob), NO data change, NO permission changes — the existing
   `excelbuildmode.reviewed` grant(s) keep working under the same
   node name, the console works immediately, and any op holds
   excelbuildmode.admin by its default. (A LuckPerms grant is needed
   only if a non-op staff account should run the admin forms.)
5. Start the server; run the boot-verify + console smoke below.

## Boot-verify (all must hold; any failure → rollback)

- "Enabling ExcelBuildMode v1.3.4" present.
- The banner line reads "ExcelBuildMode 1.3.4 PILOT:" and includes
  BOTH "1.3.3: console admin - /buildmode admin status <player> and
  /buildmode admin off <player> (excelbuildmode.admin, console
  allowed)" AND "1.3.4: combined rebuild - the reviewed-builder
  exemption rides alongside the console admin surface".
- NO startup-problem line (no cohort/storage/journal complaints).
- Zero SEVERE attributable to the swap; guildgate LOCKED 0 of 5.
- Plugin count unchanged from pre-swap.

## Console smoke (from the Shockbyte console, immediately post-boot)

1. `buildmode admin status NobodyHereXYZ` → "Build Mode admin: no
   player named 'NobodyHereXYZ' is online and no Build Mode record
   exists for that name." (unknown-name handling, live)
2. `buildmode admin` → "Usage: /buildmode admin status <player> |
   /buildmode admin off <player>"
3. `buildmode status` → "Use /buildmode in game. It only changes
   your own mode." (the player path still rejects the console — the
   allowance is admin-branch only, by design)
4. With a builder online in Play Normally:
   `buildmode admin status <builder>` → "… is playing normally (no
   Build Mode session)." and `buildmode admin off <builder>` →
   "… is already playing normally; nothing to restore." (clean
   no-op — do NOT smoke a real restore on a live session outside
   the player proofs below)

## Player proofs (post-install)

### Proof A — reviewed regression pair (Marvin, the reviewed builder)

Precondition: Marvin holds `excelbuildmode.reviewed` AND standing
WorldEdit outside Build Mode (the live reviewed setup).

1. `/buildmode on` → Build Mode OPENS (no "You already have
   WorldEdit permissions outside Build Mode" refusal). This is the
   recovered exemption, live.
2. `/buildmode off` on solid ground → clean restore to Play
   Normally, Survival items/XP back exactly, NO fail-closed kick
   and no "WorldEdit permission remains" failure — the exit probe
   is skipped for a reviewed holder, whose standing WorldEdit is
   expected to remain.
3. Counter-check (only if a non-reviewed eligible account is
   available): the same standing-WorldEdit setup WITHOUT
   `excelbuildmode.reviewed` still gets the entry refusal message.
   If no such account exists, note it — the harness truth tables
   already pin this leg; do not manufacture grants to force it.

### Proof B — console admin rescue (one eligible builder)

1. Builder enters Build Mode normally (`/buildmode on`); console
   `buildmode admin status <builder>` → "… is in Build Mode." with
   the journal session id.
2. Builder stays in Build Mode (the stuck shape). From console:
   `buildmode admin off <builder>` → "… restored to Play Normally;
   the Survival snapshot was returned through the standard recovery
   path." The server log carries "BuildMode admin off by CONSOLE
   for <builder> (<uuid>): …". The builder confirms in game:
   Survival inventory, ender chest and XP are back exactly, mode is
   Survival, no recovery lock.
3. Regression pair (unchanged behavior): the builder's own
   `/buildmode on` → `/buildmode off` round trip still restores
   cleanly, and a relog while in Build Mode still returns to Build
   Mode (1.3 behavior).
4. Optional (only if a landing-hold case is wanted): builder enters
   Build Mode, flies up, console `admin off` → the reply names the
   landing hold; on landing, the restore completes on its own.

## Rollback

Stop; delete ExcelBuildMode-1.3.4.jar; rename
ExcelBuildMode-1.3.3.jar.bak-20261110-reviewed →
ExcelBuildMode-1.3.3.jar; start; verify the reviewed build is live
again (a reviewed builder can enter Build Mode with standing
WorldEdit; note the reviewed build prints the plain 1.3.2 banner
text — its identity is the jar hash 3662dcb7…, not the banner). The
excelbuildmode.admin node is simply unread under the reviewed
build; no data, config, or permission state was touched by this
swap. The console admin surface is lost on rollback — that is the
known, accepted cost; the reviewed flow is the one that must not
regress.

## Same-pass surfaces owed at install

Per the same-pass rule, closing this install also lands/verifies:
Guide knowledge + intents for the two admin forms (operator-facing,
not player Guide content — confirm with the Guide owner surface);
What's New BOTH surfaces (website section AND the in-game
ExcelWhatsNew changelog) noting the console rescue for stuck Build
Mode sessions in player terms (the reviewed flow is unchanged for
players — no separate What's New claim for it); rotation tips
checked (no tip change expected — Build Mode tips are player-facing
and the player surface is unchanged; state the check in the install
report); in-game menus checked (ExcelMenu + ExcelBedrockMenu — no
menu change: /buildmode's own two-button menu is unchanged and the
admin forms are console/operator surface; state the check in the
install report).

## Records owed at install

INSTALLED marker + daily log, Route Sheet row, Big Brain (local +
shared + Drive), records package already staged at
~/workspace/records-catchup-20261103/packages/excelbuildmode-1.3.4-20261110/,
a one-line note closing the audit's BuildMode FAIL row (console-admin
audit §2/§4) once Proof B passes, and a note in the HELD BuildMode
MANIFEST history that the version collision is resolved by 1.3.4
(the console-admin 1.3.3 jar fa640544… is superseded, never to be
installed).
